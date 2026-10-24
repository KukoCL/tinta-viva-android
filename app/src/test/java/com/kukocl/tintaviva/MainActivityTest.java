package com.kukocl.tintaviva;

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

import java.util.Collections;

import static org.junit.Assert.*;

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
}
