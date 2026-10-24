package com.kukocl.tintaviva.core;

import org.junit.Test;

import static org.junit.Assert.*;

public class CanvasViewportTest {
    private CanvasViewport viewport() {
        CanvasViewport view = new CanvasViewport();
        view.resize(1080, 1350, 0);
        return view;
    }

    @Test public void initialFitShowsTheWholeDocumentCentered() {
        CanvasViewport view = new CanvasViewport();
        view.resize(600, 800, 4);
        assertEquals(4, view.getLeft(), .001f);
        assertEquals(596, view.getRight(), .001f);
        assertEquals(400, (view.getTop() + view.getBottom()) / 2, .001f);
        assertEquals(1, view.getZoom(), 0);
        assertTrue(view.getTop() >= 4);
        assertTrue(view.getBottom() <= 796);
    }

    @Test public void zoomKeepsThePointUnderTheFingersInPlace() {
        CanvasViewport view = viewport();
        float x = view.toDocumentX(400), y = view.toDocumentY(550);
        view.transform(2, 400, 550, 400, 550);
        assertEquals(x, view.toDocumentX(400), .001f);
        assertEquals(y, view.toDocumentY(550), .001f);
        assertEquals(2, view.getZoom(), 0);
    }

    @Test public void simultaneousPanAndZoomFollowTheMovingFocus() {
        CanvasViewport view = viewport();
        float x = view.toDocumentX(500), y = view.toDocumentY(600);
        view.transform(2, 500, 600, 540, 660);
        assertEquals(x, view.toDocumentX(540), .001f);
        assertEquals(y, view.toDocumentY(660), .001f);
    }

    @Test public void drawingCoordinatesRoundTripAfterPanAndZoom() {
        CanvasViewport view = viewport();
        view.transform(3, 540, 675, 580, 720);
        for (float x : new float[]{0, 250, 540, 1080}) {
            assertEquals(x, view.toDocumentX(view.toScreenX(x)), .001f);
        }
        for (float y : new float[]{0, 250, 675, 1350}) {
            assertEquals(y, view.toDocumentY(view.toScreenY(y)), .001f);
        }
    }

    @Test public void zoomIsLimitedAndPinchingBackOutRestoresTheFullPage() {
        CanvasViewport view = viewport();
        view.transform(1000, 540, 675, 540, 675);
        assertEquals(8, view.getZoom(), 0);
        view.transform(.00001f, 200, 300, 500, 800);
        assertEquals(1, view.getZoom(), 0);
        assertEquals(0, view.getLeft(), .001f);
        assertEquals(0, view.getTop(), .001f);
    }

    @Test public void panningCannotLoseThePageOffScreen() {
        CanvasViewport view = viewport();
        view.transform(2, 540, 675, 540, 675);
        view.transform(1, 0, 0, 100000, 100000);
        assertEquals(0, view.getLeft(), .001f);
        assertEquals(0, view.getTop(), .001f);
        view.transform(1, 0, 0, -100000, -100000);
        assertEquals(1080, view.getRight(), .001f);
        assertEquals(1350, view.getBottom(), .001f);
    }

    @Test public void resizingKeepsZoomAndTheVisibleDocumentCenter() {
        CanvasViewport view = viewport();
        view.transform(3, 440, 575, 500, 600);
        float x = view.toDocumentX(540), y = view.toDocumentY(675);
        view.resize(600, 800, 2);
        assertEquals(3, view.getZoom(), 0);
        assertEquals(x, view.toDocumentX(300), .001f);
        assertEquals(y, view.toDocumentY(400), .001f);
    }

    @Test public void invalidScaleEventsCannotCorruptTheViewport() {
        CanvasViewport view = viewport();
        for (float factor : new float[]{0, -1, Float.NaN, Float.POSITIVE_INFINITY}) {
            view.transform(factor, 200, 300, 400, 500);
        }
        assertEquals(1, view.getZoom(), 0);
        assertEquals(0, view.getLeft(), 0);
        view.transform(2, Float.NaN, 300, 400, 500);
        assertEquals(1, view.getZoom(), 0);
    }
}
