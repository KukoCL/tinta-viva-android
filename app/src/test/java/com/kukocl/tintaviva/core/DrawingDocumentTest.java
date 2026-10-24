package com.kukocl.tintaviva.core;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class DrawingDocumentTest {
    private final DrawingDocument document = new DrawingDocument();
    private static final float[] LINE = {100, 100, 200, 100, 300, 100};

    @Test public void selectingLineArtMakesSketchExactlyFortyPercentOpaque() {
        assertEquals(1f, document.sketchOpacity(), 0);
        document.setActiveLayer(Layer.LINE_ART);
        assertEquals(.4f, document.sketchOpacity(), 0);
        document.setActiveLayer(Layer.SKETCH);
        assertEquals(1f, document.sketchOpacity(), 0);
    }

    @Test public void lineArtOnlyHidesSketchAndKeepsEditingOnTheTopLayer() {
        Stroke sketch = document.addStroke(0xffff0000, 10, LINE);
        document.showOnlyLineArt();
        assertTrue(document.isLineArtOnly());
        assertEquals(0, document.sketchOpacity(), 0);
        assertEquals(Layer.LINE_ART, document.getActiveLayer());
        Stroke ink = document.addStroke(0xff0000ff, 10, LINE);
        assertEquals(Layer.LINE_ART, ink.layer);
        assertTrue(document.undo());
        assertEquals(Arrays.asList(sketch), document.getStrokes());
        assertTrue(document.isLineArtOnly());
    }

    @Test public void returningFromLineArtOnlyRestoresTheCorrectSketchOpacity() {
        document.showOnlyLineArt();
        document.setActiveLayer(Layer.LINE_ART);
        assertFalse(document.isLineArtOnly());
        assertEquals(.4f, document.sketchOpacity(), 0);
        document.showOnlyLineArt();
        document.setActiveLayer(Layer.SKETCH);
        assertFalse(document.isLineArtOnly());
        assertEquals(1, document.sketchOpacity(), 0);
    }

    @Test public void changingTheViewNeverCreatesAnUndoStep() {
        Stroke stroke = document.addStroke(0xff000000, 10, LINE);
        document.showOnlyLineArt();
        document.setActiveLayer(Layer.LINE_ART);
        assertTrue(document.undo());
        assertFalse(document.getStrokes().contains(stroke));
        assertFalse(document.canUndo());
    }

    @Test public void colorsAndLayersAreRetainedIndependentlyForEveryStroke() {
        Stroke sketch = document.addStroke(0xffed6767, 10, LINE);
        document.setActiveLayer(Layer.LINE_ART);
        Stroke ink = document.addStroke(0xff539dd3, 20, LINE);
        assertEquals(Layer.SKETCH, sketch.layer);
        assertEquals(Layer.LINE_ART, ink.layer);
        assertEquals(0xffed6767, sketch.color);
        assertEquals(0xff539dd3, ink.color);
    }

    @Test public void eraserOnlyRemovesTopmostStrokeOnSelectedLayer() {
        Stroke bottom = document.addStroke(0xff000000, 8, LINE);
        Stroke top = document.addStroke(0xff00ff00, 8, LINE);
        document.setActiveLayer(Layer.LINE_ART);
        Stroke ink = document.addStroke(0xff0000ff, 8, LINE);
        document.setActiveLayer(Layer.SKETCH);
        assertTrue(document.eraseAt(200, 100, 0, 3));
        assertEquals(Arrays.asList(bottom, ink), document.getStrokes());
        assertTrue(document.undo());
        assertEquals(Arrays.asList(bottom, top, ink), document.getStrokes());
    }

    @Test public void undoRestoresWholeErasedStrokeWithItsOriginalProperties() {
        document.setActiveLayer(Layer.LINE_ART);
        Stroke stroke = document.addStroke(0xff7744cc, 13, LINE);
        float[] visible = LineAnimation.pointsAt(stroke, 5);
        assertTrue(document.eraseAt(visible[2], visible[3], 0, 5));
        assertTrue(document.getStrokes().isEmpty());
        assertTrue(document.undo());
        assertSame(stroke, document.getStrokes().get(0));
        assertTrue(document.undo());
        assertTrue(document.getStrokes().isEmpty());
        assertFalse(document.undo());
    }

    @Test public void missedEraserTapDoesNotCreateAnUndoStep() {
        Stroke stroke = document.addStroke(0xff123456, 10, LINE);
        assertFalse(document.eraseAt(900, 900, 3, 0));
        assertTrue(document.undo());
        assertFalse(document.getStrokes().contains(stroke));
    }

    @Test public void undoWorksAcrossLayerChanges() {
        Stroke sketch = document.addStroke(0xff000000, 10, LINE);
        document.setActiveLayer(Layer.LINE_ART);
        document.addStroke(0xff00ff00, 10, LINE);
        document.setActiveLayer(Layer.SKETCH);
        assertTrue(document.undo());
        assertEquals(Arrays.asList(sketch), document.getStrokes());
    }

    @Test public void restorePreservesIdsAndMakesFutureIdsUnique() {
        Stroke saved = new Stroke(901, Layer.LINE_ART, 0xffabcdef, 9, LINE);
        document.restore(Arrays.asList(saved));
        assertFalse(document.canUndo());
        Stroke next = document.addStroke(0xff123456, 4, LINE);
        assertEquals(902, next.id);
        assertSame(saved, document.getStrokes().get(0));
        assertTrue(document.hasAnimatedStrokes());
    }

    @Test(expected = IllegalArgumentException.class)
    public void duplicateSavedIdsAreRejected() {
        Stroke saved = new Stroke(5, Layer.SKETCH, 0xff000000, 9, LINE);
        document.restore(Arrays.asList(saved, saved));
    }

    @Test public void undoHistoryIsBoundedButOlderDrawingContentIsKept() {
        for (int i = 0; i < 150; i++) document.addStroke(0xff000000, 5, new float[]{i, i});
        int undoCount = 0;
        while (document.undo()) undoCount++;
        assertEquals(128, undoCount);
        assertEquals(22, document.getStrokes().size());
        int redoCount = 0;
        while (document.redo()) redoCount++;
        assertEquals(128, redoCount);
        assertEquals(150, document.getStrokes().size());
        assertFalse(document.canRedo());
    }

    @Test public void editingSnapshotCannotChangeTheLiveDocument() {
        document.addStroke(0xff000000, 5, LINE);
        document.snapshot().clear();
        assertEquals(1, document.getStrokes().size());
    }

    @Test public void clearingRemovesBothLayersAndTheUndoHistoryIncludingErasedStrokes() {
        document.addStroke(0xff123456, 10, LINE);
        document.setActiveLayer(Layer.LINE_ART);
        document.addStroke(0xffabcdef, 12, LINE);
        document.setActiveLayer(Layer.SKETCH);
        assertTrue(document.eraseAt(200, 100, 0, 0));
        document.clear();
        assertTrue(document.getStrokes().isEmpty());
        assertFalse(document.hasAnimatedStrokes());
        assertFalse(document.canUndo());
        assertFalse(document.undo());
        Stroke fresh = document.addStroke(0xff000000, 5, LINE);
        assertEquals(1, fresh.id);
        assertTrue(document.undo());
        assertTrue(document.getStrokes().isEmpty());
        assertFalse(document.undo());
    }

    @Test public void redoReplaysAddedAndErasedStrokesInOrderAcrossLayers() {
        Stroke sketch = document.addStroke(0xffff0000, 10, LINE);
        document.setActiveLayer(Layer.LINE_ART);
        Stroke ink = document.addStroke(0xff0000ff, 20, LINE);
        document.setActiveLayer(Layer.SKETCH);
        assertTrue(document.eraseAt(200, 100, 0, 0));
        assertTrue(document.undo());
        assertTrue(document.undo());
        assertTrue(document.undo());
        assertTrue(document.getStrokes().isEmpty());
        assertFalse(document.canUndo());
        assertTrue(document.canRedo());
        assertTrue(document.redo());
        assertEquals(Arrays.asList(sketch), document.getStrokes());
        assertTrue(document.redo());
        assertEquals(Arrays.asList(sketch, ink), document.getStrokes());
        assertTrue(document.redo());
        assertEquals(Arrays.asList(ink), document.getStrokes());
        assertSame(ink, document.getStrokes().get(0));
        assertFalse(document.canRedo());
        assertFalse(document.redo());
        assertTrue(document.undo());
        assertEquals(Arrays.asList(sketch, ink), document.getStrokes());
    }

    @Test public void aNewStrokeAfterUndoDiscardsTheAbandonedRedoBranch() {
        Stroke first = document.addStroke(0xff123456, 5, LINE);
        Stroke abandoned = document.addStroke(0xff654321, 6, LINE);
        assertTrue(document.undo());
        assertTrue(document.canRedo());
        Stroke fresh = document.addStroke(0xffabcdef, 7, LINE);
        assertFalse(document.canRedo());
        assertFalse(document.redo());
        assertEquals(Arrays.asList(first, fresh), document.getStrokes());
        assertTrue(fresh.id > abandoned.id);
    }

    @Test public void onlyASuccessfulEraserTapDiscardsRedo() {
        document.addStroke(0xff000000, 10, LINE);
        document.addStroke(0xff123456, 10, LINE);
        assertTrue(document.undo());
        assertFalse(document.eraseAt(900, 900, 0, 0));
        assertTrue(document.canRedo());
        assertTrue(document.eraseAt(200, 100, 0, 0));
        assertFalse(document.canRedo());
        assertFalse(document.redo());
    }

    @Test public void clearAndRestoreBothDiscardRedoHistory() {
        Stroke saved = document.addStroke(0xff000000, 10, LINE);
        assertTrue(document.undo());
        document.clear();
        assertFalse(document.canRedo());
        assertFalse(document.redo());
        document.addStroke(0xff123456, 12, LINE);
        assertTrue(document.undo());
        document.restore(Arrays.asList(saved));
        assertFalse(document.canUndo());
        assertFalse(document.canRedo());
        assertEquals(Arrays.asList(saved), document.getStrokes());
    }

    @Test public void changingLayerAndMotionPreservesRedoAndTheOriginalStroke() {
        Stroke sketch = document.addStroke(0xff123456, 10, LINE);
        assertTrue(document.undo());
        document.showOnlyLineArt();
        document.setStrongMotion(false);
        assertTrue(document.canRedo());
        assertTrue(document.redo());
        assertSame(sketch, document.getStrokes().get(0));
        assertEquals(Layer.SKETCH, sketch.layer);
        assertTrue(document.isLineArtOnly());
        assertFalse(document.isStrongMotion());
    }

    @Test public void eraserUsesTheGeometryOfTheSelectedSoftMotionInEveryFrame() {
        for (int frame = 0; frame < LineAnimation.FRAME_COUNT; frame++) {
            DrawingDocument soft = new DrawingDocument();
            soft.setActiveLayer(Layer.LINE_ART);
            soft.setStrongMotion(false);
            Stroke dot = soft.addStroke(0xff000000, .5f, new float[]{100, 100});
            float[] visible = LineAnimation.pointsAt(dot, frame, false);
            assertTrue(soft.eraseAt(visible[0], visible[1], 0, frame));
            assertTrue(soft.getStrokes().isEmpty());
            assertTrue(soft.undo());
            assertTrue(soft.redo());
            assertTrue(soft.getStrokes().isEmpty());
        }
    }
}
