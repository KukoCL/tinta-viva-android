package com.kukocl.tintaviva;

import android.app.Application;
import android.view.InputDevice;
import android.view.MotionEvent;

import com.kukocl.tintaviva.core.CanvasViewport;
import com.kukocl.tintaviva.core.DrawingDocument;
import com.kukocl.tintaviva.core.Layer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = Application.class)
public class DrawingViewGestureTest {
    private DrawingDocument document;
    private CanvasViewport viewport;
    private DrawingView view;
    private long time = 1000, downTime;

    @Before public void setUp() {
        document = new DrawingDocument();
        viewport = new CanvasViewport();
        view = new DrawingView(RuntimeEnvironment.getApplication(), document, viewport);
        view.layout(0, 0, 600, 800);
    }

    @Test public void pinchZoomNeverCommitsADraftOrContinuesItWithTheRemainingFinger() {
        pinch();
        assertTrue(viewport.getZoom() > 1);
        assertTrue(document.getStrokes().isEmpty());
        assertFalse(document.canUndo());
    }

    @Test public void pinchWithTheEraserSelectedNeverDeletesTheTouchedStroke() {
        document.addStroke(0xff000000, 12, new float[]{540, 675});
        view.setErasing(true);
        send(MotionEvent.ACTION_DOWN, new int[]{0}, 300, 400);
        assertEquals(1, document.getStrokes().size());
        send(MotionEvent.ACTION_POINTER_DOWN | 1 << 8, new int[]{0, 1}, 300, 400, 460, 400);
        send(MotionEvent.ACTION_MOVE, new int[]{0, 1}, 250, 400, 510, 400);
        send(MotionEvent.ACTION_MOVE, new int[]{0, 1}, 200, 400, 560, 400);
        send(MotionEvent.ACTION_POINTER_UP | 1 << 8, new int[]{0, 1}, 200, 400, 560, 400);
        send(MotionEvent.ACTION_UP, new int[]{0}, 200, 400);
        assertEquals(1, document.getStrokes().size());
        assertTrue(document.undo());
        assertTrue(document.getStrokes().isEmpty());
    }

    @Test public void drawingAfterZoomUsesDocumentCoordinatesAndStartsANewStroke() {
        pinch();
        float x = viewport.toScreenX(500), y = viewport.toScreenY(650);
        send(MotionEvent.ACTION_DOWN, new int[]{5}, x, y);
        send(MotionEvent.ACTION_UP, new int[]{5}, x, y);
        assertEquals(1, document.getStrokes().size());
        assertArrayEquals(new float[]{500, 650}, document.getStrokes().get(0).copyPoints(), .01f);
    }

    @Test public void eraserStillHitsAWholeStrokeAfterZoomAndPan() {
        document.addStroke(0xff000000, 12, new float[]{400, 675, 680, 675});
        viewport.transform(3, 300, 400, 320, 420);
        view.setErasing(true);
        float x = viewport.toScreenX(540), y = viewport.toScreenY(675);
        send(MotionEvent.ACTION_DOWN, new int[]{0}, x, y);
        send(MotionEvent.ACTION_UP, new int[]{0}, x, y);
        assertTrue(document.getStrokes().isEmpty());
        assertTrue(document.undo());
        assertEquals(1, document.getStrokes().size());
    }

    @Test public void cancellingAGestureLeavesNoStrokeAndNoUndoStep() {
        send(MotionEvent.ACTION_DOWN, new int[]{0}, 280, 380);
        send(MotionEvent.ACTION_MOVE, new int[]{0}, 320, 420);
        send(MotionEvent.ACTION_CANCEL, new int[]{0}, 320, 420);
        assertTrue(document.getStrokes().isEmpty());
        assertFalse(document.canUndo());
    }

    @Test public void drawingInLineArtOnlyKeepsTheSketchAndEditsTheTopLayer() {
        document.addStroke(0xffff0000, 10, new float[]{100, 100});
        document.showOnlyLineArt();
        send(MotionEvent.ACTION_DOWN, new int[]{0}, 300, 400);
        send(MotionEvent.ACTION_UP, new int[]{0}, 300, 400);
        assertEquals(2, document.getStrokes().size());
        assertEquals(Layer.SKETCH, document.getStrokes().get(0).layer);
        assertEquals(Layer.LINE_ART, document.getStrokes().get(1).layer);
        assertEquals(0, document.sketchOpacity(), 0);
    }

    @Test public void liftingAThirdFingerDoesNotJumpTheCanvas() {
        send(MotionEvent.ACTION_DOWN, new int[]{0}, 220, 400);
        send(MotionEvent.ACTION_POINTER_DOWN | 1 << 8, new int[]{0, 1}, 220, 400, 380, 400);
        send(MotionEvent.ACTION_MOVE, new int[]{0, 1}, 180, 400, 420, 400);
        send(MotionEvent.ACTION_MOVE, new int[]{0, 1}, 140, 400, 460, 400);
        float left = viewport.getLeft(), top = viewport.getTop();
        send(MotionEvent.ACTION_POINTER_DOWN | 2 << 8, new int[]{0, 1, 2}, 140, 400, 460, 400, 300, 650);
        send(MotionEvent.ACTION_POINTER_UP | 2 << 8, new int[]{0, 1, 2}, 140, 400, 460, 400, 300, 650);
        send(MotionEvent.ACTION_MOVE, new int[]{0, 1}, 140, 400, 460, 400);
        assertEquals(left, viewport.getLeft(), .01f);
        assertEquals(top, viewport.getTop(), .01f);
        assertTrue(document.getStrokes().isEmpty());
    }

    private void pinch() {
        send(MotionEvent.ACTION_DOWN, new int[]{0}, 220, 400);
        send(MotionEvent.ACTION_POINTER_DOWN | 1 << 8, new int[]{0, 1}, 220, 400, 380, 400);
        send(MotionEvent.ACTION_MOVE, new int[]{0, 1}, 180, 400, 420, 400);
        send(MotionEvent.ACTION_MOVE, new int[]{0, 1}, 140, 400, 460, 400);
        send(MotionEvent.ACTION_MOVE, new int[]{0, 1}, 100, 400, 500, 400);
        send(MotionEvent.ACTION_POINTER_UP | 1 << 8, new int[]{0, 1}, 100, 400, 500, 400);
        send(MotionEvent.ACTION_MOVE, new int[]{0}, 110, 410);
        send(MotionEvent.ACTION_UP, new int[]{0}, 110, 410);
    }

    private void send(int action, int[] ids, float... positions) {
        time += 20;
        if (action == MotionEvent.ACTION_DOWN) downTime = time;
        MotionEvent.PointerProperties[] properties = new MotionEvent.PointerProperties[ids.length];
        MotionEvent.PointerCoords[] coords = new MotionEvent.PointerCoords[ids.length];
        for (int i = 0; i < ids.length; i++) {
            properties[i] = new MotionEvent.PointerProperties();
            properties[i].id = ids[i];
            properties[i].toolType = MotionEvent.TOOL_TYPE_FINGER;
            coords[i] = new MotionEvent.PointerCoords();
            coords[i].x = positions[i * 2];
            coords[i].y = positions[i * 2 + 1];
            coords[i].pressure = 1;
            coords[i].size = 1;
        }
        MotionEvent event = MotionEvent.obtain(downTime, time, action, ids.length, properties,
                coords, 0, 0, 1, 1, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0);
        try { assertTrue(view.onTouchEvent(event)); }
        finally { event.recycle(); }
    }
}
