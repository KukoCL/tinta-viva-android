package com.kukocl.tintaviva;

import android.app.AlertDialog;
import android.graphics.Rect;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.SeekBar;

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
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowPopupMenu;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

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
        assertEquals(.3f, session.document.sketchOpacity(), 0);
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

    @Test @Config(sdk = {26, 28, 35})
    public void phoneOpensAWideBrushPanelWithTheSampleAboveTheSlider() {
        layoutAt(360, 640);
        assertNull(activity.findViewById(R.id.brush_size_slider));
        PopupWindow popup = openBrushSizePanel();
        assertEquals(344, popup.getWidth());
        View panel = popup.getContentView();
        panel.measure(View.MeasureSpec.makeMeasureSpec(popup.getWidth(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        panel.layout(0, 0, panel.getMeasuredWidth(), panel.getMeasuredHeight());
        View slider = panel.findViewById(R.id.brush_size_slider);
        View preview = panel.findViewById(R.id.brush_size_preview);
        assertTrue(slider.getWidth() >= 320);
        assertTrue(preview.getBottom() <= ((View) preview.getParent()).getHeight());
        Rect sampleBounds = new Rect(0, 0, preview.getWidth(), preview.getHeight());
        Rect sliderBounds = new Rect(0, 0, slider.getWidth(), slider.getHeight());
        ((ViewGroup) panel).offsetDescendantRectToMyCoords(preview, sampleBounds);
        ((ViewGroup) panel).offsetDescendantRectToMyCoords(slider, sliderBounds);
        assertTrue(sampleBounds.bottom <= sliderBounds.top);
        assertTrue(preview.getHeight() >= 100);
        assertTrue(activity.findViewById(R.id.tool_brush_size).isSelected());
        popup.dismiss();
        assertFalse(activity.findViewById(R.id.tool_brush_size).isSelected());
        assertNull(activity.findViewById(R.id.brush_size_slider));
    }

    @Test public void brushPanelChangesTheNextStrokeAndRemembersTheWidthAfterRecreation() {
        layoutAt(360, 640);
        PopupWindow popup = openBrushSizePanel();
        SeekBar slider = popup.getContentView().findViewById(R.id.brush_size_slider);
        for (int i = 0; i < 30; i++) {
            slider.onKeyDown(KeyEvent.KEYCODE_DPAD_RIGHT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT));
        }
        assertEquals(32, session.getSharedPreferences("studio", 0).getInt("width", -1));
        popup.dismiss();
        controller.recreate();
        activity = controller.get();
        layoutAt(360, 640);
        popup = openBrushSizePanel();
        slider = popup.getContentView().findViewById(R.id.brush_size_slider);
        assertEquals(30, slider.getProgress());
        popup.dismiss();
        DrawingView canvas = activity.findViewById(R.id.drawing_canvas);
        touch(canvas, MotionEvent.ACTION_DOWN, canvas.getWidth() / 2f, canvas.getHeight() / 2f);
        touch(canvas, MotionEvent.ACTION_UP, canvas.getWidth() / 2f, canvas.getHeight() / 2f);
        assertEquals(32, session.document.getStrokes().get(0).width, 0);
    }

    @Test @Config(qualifiers = "w320dp-h568dp-port-mdpi")
    public void theFiveBottomButtonsFitOnANarrowPhoneWithoutShrinkingTheirTouchTargets() {
        layoutAt(320, 568);
        int previousRight = 8;
        for (int id : new int[]{R.id.tool_brush, R.id.tool_eraser, R.id.tool_color,
                R.id.tool_eyedropper, R.id.tool_brush_size}) {
            Rect button = bounds(activity.findViewById(id));
            assertTrue(button.left >= previousRight);
            assertTrue(button.right <= 312);
            assertEquals(48, button.width());
            assertEquals(48, button.height());
            previousRight = button.right;
        }
        PopupWindow popup = openBrushSizePanel();
        assertEquals(304, popup.getWidth());
        popup.dismiss();
    }

    @Test @Config(qualifiers = "w600dp-h960dp-port-mdpi")
    public void tabletKeepsACompactSliderAndShowsTheSampleOnlyWhileAdjusting() {
        layoutAt(600, 960);
        assertNull(activity.findViewById(R.id.tool_brush_size));
        View slider = activity.findViewById(R.id.brush_size_slider);
        assertTrue(slider.isShown());
        assertNull(activity.findViewById(R.id.brush_size_preview));
        assertTrue(slider.getWidth() >= 320);
        assertEquals(48, activity.findViewById(R.id.brush_size_control).getHeight());
        Rect canvas = bounds(activity.findViewById(R.id.drawing_canvas));
        assertTrue(canvas.bottom <= bounds(slider).top);
        PopupWindow popup = beginTabletSizeSelection();
        assertFalse(popup.isFocusable());
        assertFalse(popup.isTouchable());
        assertEquals(R.id.brush_size_preview, popup.getContentView().getId());
        shadowOf(Looper.getMainLooper()).idleFor(3, TimeUnit.SECONDS);
        assertTrue("La muestra permanece mientras se sostiene el dedo", popup.isShowing());
        touch(slider, MotionEvent.ACTION_UP, slider.getWidth() * .7f, 24);
        shadowOf(Looper.getMainLooper()).idleFor(1000, TimeUnit.MILLISECONDS);
        assertTrue(popup.isShowing());
        shadowOf(Looper.getMainLooper()).idleFor(600, TimeUnit.MILLISECONDS);
        assertFalse(popup.isShowing());
        assertEquals(canvas, bounds(activity.findViewById(R.id.drawing_canvas)));
        assertTrue(session.document.getStrokes().isEmpty());
    }

    @Test @Config(qualifiers = "w600dp-h960dp-port-mdpi")
    public void touchingTheTabletSliderAgainRestartsTheSampleTimeout() {
        layoutAt(600, 960);
        View slider = activity.findViewById(R.id.brush_size_slider);
        PopupWindow popup = beginTabletSizeSelection();
        touch(slider, MotionEvent.ACTION_UP, slider.getWidth() * .7f, 24);
        shadowOf(Looper.getMainLooper()).idleFor(1, TimeUnit.SECONDS);
        beginTabletSizeSelection();
        shadowOf(Looper.getMainLooper()).idleFor(2, TimeUnit.SECONDS);
        assertTrue(popup.isShowing());
        touch(slider, MotionEvent.ACTION_UP, slider.getWidth() * .7f, 24);
        shadowOf(Looper.getMainLooper()).idleFor(1500, TimeUnit.MILLISECONDS);
        assertFalse(popup.isShowing());
    }

    @Test @Config(qualifiers = "w600dp-h960dp-port-mdpi")
    public void cancellingTabletAdjustmentHidesTheSampleAndLeavingTheActivityClosesItImmediately() {
        layoutAt(600, 960);
        View slider = activity.findViewById(R.id.brush_size_slider);
        PopupWindow popup = beginTabletSizeSelection();
        touch(slider, MotionEvent.ACTION_CANCEL, slider.getWidth() * .7f, 24);
        shadowOf(Looper.getMainLooper()).idleFor(1500, TimeUnit.MILLISECONDS);
        assertFalse(popup.isShowing());
        popup = beginTabletSizeSelection();
        controller.pause();
        assertFalse(popup.isShowing());
        controller.resume();
    }

    @Test @Config(qualifiers = "w1280dp-h800dp-land-mdpi")
    public void aStationaryPressInTheScrollableTabletPanelAlsoShowsTheSampleImmediately() {
        layoutAt(1280, 800);
        View control = activity.findViewById(R.id.brush_size_control);
        View slider = activity.findViewById(R.id.brush_size_slider);
        MotionEvent down = MotionEvent.obtain(1000, 1020, MotionEvent.ACTION_DOWN, slider.getWidth() / 2f, 24, 0);
        try { assertTrue(control.dispatchTouchEvent(down)); }
        finally { down.recycle(); }
        PopupWindow popup = shadowOf(session).getLatestPopupWindow();
        assertNotNull(popup);
        assertTrue(popup.isShowing());
        shadowOf(Looper.getMainLooper()).idleFor(3, TimeUnit.SECONDS);
        assertTrue(popup.isShowing());
        MotionEvent up = MotionEvent.obtain(1000, 1040, MotionEvent.ACTION_UP, slider.getWidth() / 2f, 24, 0);
        try { assertTrue(control.dispatchTouchEvent(up)); }
        finally { up.recycle(); }
        shadowOf(Looper.getMainLooper()).idleFor(1600, TimeUnit.MILLISECONDS);
        assertFalse(popup.isShowing());
    }

    @Test @Config(qualifiers = "w1280dp-h800dp-land-mdpi")
    public void landscapeTabletGivesTheSliderTheFullSidePanelWidth() {
        layoutAt(1280, 800);
        View slider = activity.findViewById(R.id.brush_size_slider);
        assertTrue(slider.isShown());
        assertTrue(slider.getWidth() >= 220);
        assertTrue(bounds(activity.findViewById(R.id.tool_eyedropper)).bottom <= bounds(slider).top);
        PopupWindow popup = beginTabletSizeSelection();
        touch(slider, MotionEvent.ACTION_UP, slider.getWidth() * .7f, 24);
        shadowOf(Looper.getMainLooper()).idleFor(1500, TimeUnit.MILLISECONDS);
        assertFalse(popup.isShowing());
    }

    @Test @Config(qualifiers = "w640dp-h360dp-land-mdpi")
    public void aLandscapePhoneStillUsesAWidePopupForBrushSize() {
        layoutAt(640, 360);
        assertNull(activity.findViewById(R.id.brush_size_slider));
        PopupWindow popup = openBrushSizePanel();
        assertEquals(624, popup.getWidth());
        popup.dismiss();
    }

    @Test @GraphicsMode(GraphicsMode.Mode.NATIVE)
    public void eyedropperOnlyAppliesAndPersistsTheColorOnReleaseThenReturnsToTheBrush() {
        layoutAt(360, 640);
        session.document.addStroke(0xffff0000, 100, new float[]{200, 675, 880, 675});
        activity.findViewById(R.id.tool_eraser).performClick();
        activity.findViewById(R.id.tool_eyedropper).performClick();
        assertTrue(activity.findViewById(R.id.tool_eyedropper).isSelected());
        assertFalse(activity.findViewById(R.id.tool_eraser).isSelected());
        DrawingView canvas = activity.findViewById(R.id.drawing_canvas);
        float x = session.viewport.toScreenX(540), y = session.viewport.toScreenY(675);
        touch(canvas, MotionEvent.ACTION_DOWN, x, y);
        assertEquals(0xff29302b, session.getSharedPreferences("studio", 0).getInt("color", 0xff29302b));
        touch(canvas, MotionEvent.ACTION_UP, x, y);
        assertEquals(0xffff0000, session.getSharedPreferences("studio", 0).getInt("color", 0));
        assertTrue(activity.findViewById(R.id.tool_brush).isSelected());
        assertFalse(activity.findViewById(R.id.tool_eyedropper).isSelected());
        assertEquals(1, session.document.getStrokes().size());
        touch(canvas, MotionEvent.ACTION_DOWN, x, y + 50);
        touch(canvas, MotionEvent.ACTION_UP, x, y + 50);
        assertEquals(0xffff0000, session.document.getStrokes().get(1).color);
        controller.recreate();
        activity = controller.get();
        assertTrue(activity.findViewById(R.id.tool_color).getContentDescription().toString().contains("#FF0000"));
    }

    @Test public void togglingTheEyedropperRestoresThePreviousEraserWithoutEditingTheDrawing() {
        activity.findViewById(R.id.tool_eraser).performClick();
        activity.findViewById(R.id.tool_eyedropper).performClick();
        activity.findViewById(R.id.tool_eyedropper).performClick();
        assertTrue(activity.findViewById(R.id.tool_eraser).isSelected());
        assertFalse(activity.findViewById(R.id.tool_eyedropper).isSelected());
        assertTrue(session.document.getStrokes().isEmpty());
        assertFalse(session.document.canUndo());
    }

    private PopupWindow openBrushSizePanel() {
        activity.findViewById(R.id.tool_brush_size).performClick();
        PopupWindow popup = shadowOf(session).getLatestPopupWindow();
        assertNotNull(popup);
        assertTrue(popup.isShowing());
        return popup;
    }

    private PopupWindow beginTabletSizeSelection() {
        View slider = activity.findViewById(R.id.brush_size_slider);
        touch(slider, MotionEvent.ACTION_DOWN, slider.getWidth() * .5f, 24);
        touch(slider, MotionEvent.ACTION_MOVE, slider.getWidth() * .7f, 24);
        PopupWindow popup = shadowOf(session).getLatestPopupWindow();
        assertNotNull(popup);
        assertTrue(popup.isShowing());
        WindowManager.LayoutParams position = (WindowManager.LayoutParams) popup.getContentView().getRootView().getLayoutParams();
        assertTrue("El recuadro debe quedar por encima de la barra",
                position.y + position.height <= bounds(slider).top);
        return popup;
    }

    private void touch(View view, int action, float x, float y) {
        MotionEvent event = MotionEvent.obtain(1000, 1020, action, x, y, 0);
        try { assertTrue(view.onTouchEvent(event)); }
        finally { event.recycle(); }
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
                R.id.action_motion, R.id.tool_brush, R.id.tool_eraser, R.id.tool_color}) {
            assertTrue("Los controles deben quedar a la derecha del lienzo",
                    bounds(activity.findViewById(id)).left >= canvas.right);
        }
    }

    @Test public void fileMenuAndUndoHaveSeparateTouchTargets() {
        layoutAt(360, 640);
        Rect undo = bounds(activity.findViewById(R.id.action_undo));
        Rect redo = bounds(activity.findViewById(R.id.action_redo));
        Rect fresh = bounds(activity.findViewById(R.id.action_file_menu));
        assertEquals(4, redo.left - undo.right);
        assertEquals(48, redo.width());
        assertTrue("El menú debe quedar separado de Rehacer", fresh.left - redo.right >= 20);
        assertTrue("Debe haber espacio entre Deshacer y el menú", fresh.left - undo.right >= 20);
        assertEquals(48, fresh.width());
        assertEquals(48, fresh.height());
    }

    @Test public void cancellingNewDrawingPreservesBothLayersHistoryAndZoom() {
        prepareDrawing();
        float zoom = session.viewport.getZoom();
        chooseFileAction(R.id.action_new_drawing);
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
        chooseFileAction(R.id.action_new_drawing);
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();
        assertFalse(dialog.isShowing());
        assertTrue(session.document.getStrokes().isEmpty());
        assertFalse(session.document.canUndo());
        assertFalse(session.document.canRedo());
        assertFalse(session.document.undo());
        assertFalse(session.document.isLineArtOnly());
        assertEquals(Layer.SKETCH, session.document.getActiveLayer());
        assertEquals(1, session.viewport.getZoom(), 0);
        assertTrue(activity.findViewById(R.id.layer_sketch).isSelected());
        assertTrue(activity.findViewById(R.id.tool_brush).isSelected());
        assertFalse(activity.findViewById(R.id.action_undo).isEnabled());
        assertFalse(activity.findViewById(R.id.action_redo).isEnabled());
        assertFalse(session.getSharedPreferences("studio", 0).getBoolean("lineArtOnly", true));
        assertFalse(session.getSharedPreferences("studio", 0).getBoolean("eraser", true));
    }

    @Test public void dismissingConfirmationAlsoPreservesTheDrawing() {
        prepareDrawing();
        chooseFileAction(R.id.action_new_drawing);
        ShadowAlertDialog.getLatestAlertDialog().cancel();
        assertEquals(2, session.document.getStrokes().size());
        assertTrue(session.document.canUndo());
    }

    @Test @Config(qualifiers = "w320dp-h568dp-port-mdpi")
    public void allTopControlsStillFitOnANarrowPhone() {
        assertPortraitLayout(320, 568);
        for (int id : new int[]{R.id.action_undo, R.id.action_redo, R.id.action_file_menu, R.id.action_motion}) {
            Rect button = bounds(activity.findViewById(id));
            assertTrue(button.left >= 8);
            assertTrue(button.right <= 312);
            assertEquals(48, button.width());
            assertEquals(48, button.height());
        }
    }

    @Test public void redoButtonRestoresTheUndoneStrokeAndUpdatesBothButtons() {
        session.document.addStroke(0xff123456, 12, new float[]{100, 100});
        activity.findViewById(R.id.layer_sketch).performClick();
        View undo = activity.findViewById(R.id.action_undo), redo = activity.findViewById(R.id.action_redo);
        assertTrue(undo.isEnabled());
        assertFalse(redo.isEnabled());
        undo.performClick();
        assertTrue(session.document.getStrokes().isEmpty());
        assertFalse(undo.isEnabled());
        assertTrue(redo.isEnabled());
        redo.performClick();
        assertEquals(1, session.document.getStrokes().size());
        assertEquals(0xff123456, session.document.getStrokes().get(0).color);
        assertTrue(undo.isEnabled());
        assertFalse(redo.isEnabled());
    }

    @Test public void motionPanelChangesOnlyMotionAndPreservesRedo() {
        prepareDrawing();
        activity.findViewById(R.id.action_undo).performClick();
        assertTrue(session.document.canRedo());
        PopupMenu menu = openMotionPanel();
        assertEquals(2, menu.getMenu().size());
        assertTrue(menu.getMenu().findItem(R.id.motion_strong).isChecked());
        assertTrue(menu.getMenu().performIdentifierAction(R.id.motion_soft, 0));
        menu.dismiss();
        assertFalse(session.document.isStrongMotion());
        assertFalse(session.getSharedPreferences("studio", 0).getBoolean("strongMotion", true));
        assertEquals(1, session.document.getStrokes().size());
        assertTrue(session.document.canRedo());
        assertTrue(session.document.isLineArtOnly());
        assertTrue(activity.findViewById(R.id.tool_eraser).isSelected());
        assertEquals(3, session.viewport.getZoom(), 0);
        activity.findViewById(R.id.action_redo).performClick();
        assertEquals(2, session.document.getStrokes().size());
        menu = openMotionPanel();
        assertTrue(menu.getMenu().findItem(R.id.motion_soft).isChecked());
        assertFalse(menu.getMenu().findItem(R.id.motion_strong).isChecked());
        assertTrue(menu.getMenu().performIdentifierAction(R.id.motion_strong, 0));
        menu.dismiss();
        assertTrue(session.document.isStrongMotion());
    }

    @Test public void motionSelectionSurvivesActivityRecreation() {
        PopupMenu menu = openMotionPanel();
        assertTrue(menu.getMenu().performIdentifierAction(R.id.motion_soft, 0));
        menu.dismiss();
        controller.recreate();
        activity = controller.get();
        assertFalse(session.document.isStrongMotion());
        menu = openMotionPanel();
        assertTrue(menu.getMenu().findItem(R.id.motion_soft).isChecked());
        menu.dismiss();
    }

    private PopupMenu openMotionPanel() {
        layoutAt(360, 640);
        activity.findViewById(R.id.action_motion).performClick();
        PopupMenu menu = ShadowPopupMenu.getLatestPopupMenu();
        assertNotNull(menu);
        return menu;
    }

    private void chooseFileAction(int id) {
        layoutAt(360, 640);
        activity.findViewById(R.id.action_file_menu).performClick();
        PopupMenu menu = ShadowPopupMenu.getLatestPopupMenu();
        assertTrue(menu.getMenu().performIdentifierAction(id, 0));
        menu.dismiss();
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
        for (int id : new int[]{R.id.layer_sketch, R.id.layer_line_art, R.id.layer_line_art_only, R.id.action_motion}) {
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
