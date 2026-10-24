package com.kukocl.tintaviva;

import android.app.AlertDialog;
import android.graphics.Rect;
import android.os.Looper;
import android.view.View;

import com.kukocl.tintaviva.core.Layer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;

import java.util.Collections;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, qualifiers = "w360dp-h640dp-mdpi")
public class MainActivityTest {
    private ActivityController<MainActivity> controller;
    private MainActivity activity;
    private TintaVivaApp session;

    @Before public void setUp() {
        session = (TintaVivaApp) RuntimeEnvironment.getApplication();
        session.document.restore(Collections.emptyList());
        session.getSharedPreferences("studio", 0).edit().clear().commit();
        controller = Robolectric.buildActivity(MainActivity.class).setup();
        activity = controller.get();
    }

    @After public void tearDown() {
        controller.pause().stop().destroy();
        session.store.close();
    }

    @Test public void theThreeLayerButtonsChangeVisibilityWithoutDeletingSketch() {
        session.document.addStroke(0xffff0000, 10, new float[]{100, 100});
        activity.findViewById(R.id.layer_line_art_only).performClick();
        assertEquals(0, session.document.sketchOpacity(), 0);
        assertEquals(Layer.LINE_ART, session.document.getActiveLayer());
        assertTrue(activity.findViewById(R.id.layer_line_art_only).isSelected());
        activity.findViewById(R.id.layer_line_art).performClick();
        assertEquals(.4f, session.document.sketchOpacity(), 0);
        activity.findViewById(R.id.layer_sketch).performClick();
        assertEquals(1, session.document.sketchOpacity(), 0);
        assertEquals(1, session.document.getStrokes().size());
    }

    @Test public void compactControlsLeaveMostOfASmallPortraitScreenForTheCanvas() {
        View decor = activity.getWindow().getDecorView();
        decor.measure(View.MeasureSpec.makeMeasureSpec(360, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(640, View.MeasureSpec.EXACTLY));
        decor.layout(0, 0, 360, 640);
        View canvas = activity.findViewById(R.id.drawing_canvas);
        assertTrue("El lienzo debe ocupar al menos el 60 % de la altura", canvas.getHeight() >= 384);
        assertTrue(activity.findViewById(R.id.tool_color).hasOnClickListeners());
        assertTrue(activity.findViewById(R.id.tool_brush).hasOnClickListeners());
        assertTrue(activity.findViewById(R.id.tool_eraser).hasOnClickListeners());
    }

    @Test public void phoneKeepsLayersAboveAndToolsBelowTheCanvas() {
        assertPortraitLayout(360, 640);
    }

    @Test @Config(qualifiers = "w600dp-h960dp-port-mdpi")
    public void smallPortraitTabletUsesTheFullCanvasWidth() {
        assertPortraitLayout(600, 960);
    }

    @Test @Config(qualifiers = "w800dp-h1280dp-port-mdpi")
    public void widePortraitTabletKeepsThePhoneLayoutInsteadOfASidePanel() {
        assertPortraitLayout(800, 1280);
    }

    @Test @Config(qualifiers = "w1280dp-h800dp-land-mdpi")
    public void landscapeTabletKeepsControlsBesideTheCanvasWithoutOverlapping() {
        layoutAt(1280, 800);
        Rect canvas = bounds(activity.findViewById(R.id.drawing_canvas));
        assertTrue(canvas.width() >= 900);
        assertTrue(canvas.height() >= 640);
        for (int id : new int[]{R.id.layer_sketch, R.id.layer_line_art, R.id.layer_line_art_only,
                R.id.tool_brush, R.id.tool_eraser, R.id.tool_color}) {
            assertTrue("Los controles deben quedar a la derecha del lienzo",
                    bounds(activity.findViewById(id)).left >= canvas.right);
        }
    }

    @Test public void newDrawingAndUndoHaveSeparateTouchTargets() {
        layoutAt(360, 640);
        Rect undo = bounds(activity.findViewById(R.id.action_undo));
        Rect fresh = bounds(activity.findViewById(R.id.action_new_drawing));
        assertTrue("Debe haber espacio entre Deshacer y Nuevo dibujo", fresh.left - undo.right >= 20);
        assertEquals(48, fresh.width());
        assertEquals(48, fresh.height());
    }

    @Test public void cancellingNewDrawingPreservesBothLayersHistoryAndZoom() {
        prepareDrawing();
        float zoom = session.viewport.getZoom();
        activity.findViewById(R.id.action_new_drawing).performClick();
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertTrue(dialog.isShowing());
        assertEquals(2, session.document.getStrokes().size());
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();
        assertFalse(dialog.isShowing());
        assertEquals(2, session.document.getStrokes().size());
        assertTrue(session.document.canUndo());
        assertTrue(session.document.isLineArtOnly());
        assertEquals(zoom, session.viewport.getZoom(), 0);
        assertTrue(activity.findViewById(R.id.tool_eraser).isSelected());
    }

    @Test public void confirmingNewDrawingClearsBothLayersHistoryAndZoomAndSelectsSketchBrush() {
        prepareDrawing();
        activity.findViewById(R.id.action_new_drawing).performClick();
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();
        assertFalse(dialog.isShowing());
        assertTrue(session.document.getStrokes().isEmpty());
        assertFalse(session.document.canUndo());
        assertFalse(session.document.undo());
        assertFalse(session.document.isLineArtOnly());
        assertEquals(Layer.SKETCH, session.document.getActiveLayer());
        assertEquals(1, session.viewport.getZoom(), 0);
        assertTrue(activity.findViewById(R.id.layer_sketch).isSelected());
        assertTrue(activity.findViewById(R.id.tool_brush).isSelected());
        assertFalse(activity.findViewById(R.id.action_undo).isEnabled());
        assertFalse(session.getSharedPreferences("studio", 0).getBoolean("lineArtOnly", true));
        assertFalse(session.getSharedPreferences("studio", 0).getBoolean("eraser", true));
    }

    @Test public void dismissingConfirmationAlsoPreservesTheDrawing() {
        prepareDrawing();
        activity.findViewById(R.id.action_new_drawing).performClick();
        ShadowAlertDialog.getLatestAlertDialog().cancel();
        assertEquals(2, session.document.getStrokes().size());
        assertTrue(session.document.canUndo());
    }

    private void prepareDrawing() {
        session.document.addStroke(0xffff0000, 10, new float[]{100, 100});
        activity.findViewById(R.id.layer_line_art_only).performClick();
        session.document.addStroke(0xff0000ff, 12, new float[]{200, 200});
        activity.findViewById(R.id.tool_eraser).performClick();
        session.viewport.resize(360, 480, 2);
        session.viewport.transform(3, 180, 240, 210, 260);
    }

    private void assertPortraitLayout(int width, int height) {
        layoutAt(width, height);
        Rect canvas = bounds(activity.findViewById(R.id.drawing_canvas));
        assertTrue("El lienzo debe aprovechar todo el ancho", canvas.width() >= width - 24);
        assertTrue("El lienzo debe ocupar la mayor parte de la altura", canvas.height() >= height * .6f);
        for (int id : new int[]{R.id.layer_sketch, R.id.layer_line_art, R.id.layer_line_art_only}) {
            assertTrue(bounds(activity.findViewById(id)).bottom <= canvas.top);
        }
        for (int id : new int[]{R.id.tool_brush, R.id.tool_eraser, R.id.tool_color}) {
            Rect tool = bounds(activity.findViewById(id));
            assertTrue(canvas.bottom <= tool.top);
            assertEquals("Los botones conservan su tamaño compacto", 48, tool.width());
        }
    }

    private void layoutAt(int width, int height) {
        View decor = activity.getWindow().getDecorView();
        decor.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        decor.layout(0, 0, width, height);
    }

    private Rect bounds(View view) {
        int[] location = new int[2];
        view.getLocationInWindow(location);
        return new Rect(location[0], location[1], location[0] + view.getWidth(), location[1] + view.getHeight());
    }
}
