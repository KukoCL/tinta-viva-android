package com.kukocl.tintaviva;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
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
import org.robolectric.annotation.GraphicsMode;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = Application.class, qualifiers = "mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class DrawingViewColorPickerTest {
    private DrawingDocument document;
    private CanvasViewport viewport;
    private DrawingView view;
    private final List<Integer> selections = new ArrayList<>();
    private long time = 1000;

    @Before public void setUp() {
        document = new DrawingDocument();
        viewport = new CanvasViewport();
        view = new DrawingView(RuntimeEnvironment.getApplication(), document, viewport);
        view.layout(0, 0, 600, 800);
        view.setColorPickListener(selections::add);
        view.setColorPicking(true);
    }

    @Test public void draggingUpdatesTheCircleAboveTheFingerAndOnlyReleaseSelectsTheFinalColor() {
        Bitmap image = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888);
        image.eraseColor(Color.RED);
        for (int x = 10; x < 20; x++) for (int y = 0; y < 20; y++) image.setPixel(x, y, Color.BLUE);
        view.setSketchImage(image);
        touch(MotionEvent.ACTION_DOWN, 200, 400);
        assertTrue(selections.isEmpty());
        assertEquals(Color.RED, render().getPixel(200, 336));
        touch(MotionEvent.ACTION_MOVE, 400, 400);
        assertTrue(selections.isEmpty());
        assertEquals(Color.BLUE, render().getPixel(400, 336));
        touch(MotionEvent.ACTION_UP, 400, 400);
        assertEquals(List.of(Color.BLUE), selections);
        assertTrue(document.getStrokes().isEmpty());
        assertFalse(document.canUndo());
    }

    @Test public void releaseSamplesItsOwnPositionEvenWithoutAMoveEvent() {
        document.addStroke(Color.RED, 100, new float[]{300, 675});
        touchDocument(MotionEvent.ACTION_DOWN, 300, 675);
        touchDocument(MotionEvent.ACTION_UP, 800, 675);
        assertEquals(List.of(Color.WHITE), selections);
        assertEquals(1, document.getStrokes().size());
    }

    @Test public void visibleLayerOpacityAndImportedImageAreCompositedBeforeSampling() {
        Bitmap image = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888);
        image.eraseColor(Color.RED);
        view.setSketchImage(image);
        document.addStroke(Color.BLUE, 100, new float[]{300, 675, 780, 675});
        document.setActiveLayer(Layer.LINE_ART);
        view.layerChanged();
        Bitmap before = render();
        pickDocument(540, 675);
        assertColorNear(Color.rgb(178, 178, 255), selections.get(0));
        assertColorNear(before.getPixel(300, 400), selections.get(0));
        pickDocument(540, 350);
        assertColorNear(Color.rgb(255, 178, 178), selections.get(1));
        document.showOnlyLineArt();
        view.layerChanged();
        pickDocument(540, 675);
        assertEquals(Color.WHITE, (int) selections.get(2));
    }

    @Test public void theTopVisibleStrokeWinsEvenWhenSketchIsTheActiveLayer() {
        document.addStroke(Color.RED, 100, new float[]{200, 675, 880, 675});
        document.setActiveLayer(Layer.LINE_ART);
        document.addStroke(Color.BLUE, 100, new float[]{200, 675, 880, 675});
        document.setActiveLayer(Layer.SKETCH);
        render();
        pickDocument(540, 675);
        assertEquals(List.of(Color.BLUE), selections);
        assertEquals(2, document.getStrokes().size());
    }

    @Test public void samplingAfterZoomAndPanUsesThePixelBeneathTheFinger() {
        document.addStroke(Color.GREEN, 30, new float[]{500, 650});
        viewport.transform(3, 300, 400, 320, 420);
        pickDocument(500, 650);
        assertEquals(List.of(Color.GREEN), selections);
        assertEquals(3, viewport.getZoom(), 0);
    }

    @Test public void pickingFromEraserModePreservesTheStrokeAndRedoHistory() {
        document.addStroke(Color.RED, 100, new float[]{540, 675});
        document.addStroke(Color.BLUE, 100, new float[]{800, 675});
        document.undo();
        view.setErasing(true);
        pickDocument(540, 675);
        assertEquals(List.of(Color.RED), selections);
        assertEquals(1, document.getStrokes().size());
        assertTrue(document.canRedo());
    }

    @Test public void cancelAndReleaseOutsideThePaperNeverChooseAColor() {
        touch(MotionEvent.ACTION_DOWN, 300, 400);
        touch(MotionEvent.ACTION_CANCEL, 300, 400);
        touch(MotionEvent.ACTION_DOWN, 300, 400);
        touch(MotionEvent.ACTION_MOVE, 0, 0);
        touch(MotionEvent.ACTION_UP, 0, 0);
        assertTrue(selections.isEmpty());
        assertTrue(view.isColorPicking());
        pickDocument(540, 675);
        assertEquals(List.of(Color.WHITE), selections);
    }

    @Test public void aSecondFingerCancelsSelectionAndTheNextGestureCanPickNormally() {
        touch(MotionEvent.ACTION_DOWN, 300, 400);
        multitouch(MotionEvent.ACTION_POINTER_DOWN | 1 << 8);
        multitouch(MotionEvent.ACTION_POINTER_UP | 1 << 8);
        touch(MotionEvent.ACTION_UP, 300, 400);
        assertTrue(selections.isEmpty());
        assertEquals(1, viewport.getZoom(), 0);
        pickDocument(540, 675);
        assertEquals(List.of(Color.WHITE), selections);
    }

    @Test public void returningToTheBrushAfterPickingDoesNotLeaveAPartialStroke() {
        touch(MotionEvent.ACTION_DOWN, 200, 400);
        view.setColorPicking(false);
        assertTrue(document.getStrokes().isEmpty());
        touch(MotionEvent.ACTION_DOWN, 300, 400);
        touch(MotionEvent.ACTION_UP, 300, 400);
        assertEquals(1, document.getStrokes().size());
        assertTrue(selections.isEmpty());
    }

    private void pickDocument(float x, float y) {
        touchDocument(MotionEvent.ACTION_DOWN, x, y);
        touchDocument(MotionEvent.ACTION_UP, x, y);
    }

    private void touchDocument(int action, float x, float y) {
        touch(action, viewport.toScreenX(x), viewport.toScreenY(y));
    }

    private void touch(int action, float x, float y) {
        MotionEvent event = MotionEvent.obtain(1000, time += 20, action, x, y, 0);
        try { assertTrue(view.onTouchEvent(event)); }
        finally { event.recycle(); }
    }

    private void multitouch(int action) {
        MotionEvent.PointerProperties[] properties = new MotionEvent.PointerProperties[2];
        MotionEvent.PointerCoords[] coords = new MotionEvent.PointerCoords[2];
        for (int i = 0; i < 2; i++) {
            properties[i] = new MotionEvent.PointerProperties();
            properties[i].id = i;
            properties[i].toolType = MotionEvent.TOOL_TYPE_FINGER;
            coords[i] = new MotionEvent.PointerCoords();
            coords[i].x = 300 + i * 100;
            coords[i].y = 400;
        }
        MotionEvent event = MotionEvent.obtain(1000, time += 20, action, 2, properties, coords,
                0, 0, 1, 1, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0);
        try { assertTrue(view.onTouchEvent(event)); }
        finally { event.recycle(); }
    }

    private Bitmap render() {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        view.draw(new Canvas(bitmap));
        return bitmap;
    }

    private void assertColorNear(int expected, int actual) {
        assertEquals(Color.red(expected), Color.red(actual), 1);
        assertEquals(Color.green(expected), Color.green(actual), 1);
        assertEquals(Color.blue(expected), Color.blue(actual), 1);
        assertEquals(255, Color.alpha(actual));
    }
}
