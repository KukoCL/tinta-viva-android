package com.kukocl.tintaviva;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;

import com.kukocl.tintaviva.core.CanvasViewport;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = Application.class, qualifiers = "mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class BrushStrokePreviewTest {
    @Test public void sampleIsACurvedMarkThatUsesTheSelectedColorAndThickness() {
        CanvasViewport viewport = new CanvasViewport();
        viewport.resize(600, 800, 2);
        BrushStrokePreview preview = new BrushStrokePreview(RuntimeEnvironment.getApplication(), viewport);
        preview.layout(0, 0, 200, 160);
        preview.setBrush(Color.RED, 2);
        int thinPixels = coloredPixels(render(preview), Color.RED);
        preview.setBrush(Color.BLUE, 32);
        Bitmap thick = render(preview);
        assertTrue(coloredPixels(thick, Color.BLUE) > thinPixels * 5);
        int top = 160, bottom = 0, left = 200, right = 0;
        for (int y = 0; y < 160; y++) for (int x = 0; x < 200; x++) {
            if (thick.getPixel(x, y) != Color.BLUE) continue;
            top = Math.min(top, y); bottom = Math.max(bottom, y);
            left = Math.min(left, x); right = Math.max(right, x);
        }
        assertTrue("El trazo ocupa altura, además de ancho", bottom - top > 100);
        assertTrue(right - left > 100);
        assertEquals(Color.WHITE, thick.getPixel(100, 150));
    }

    @Test public void zoomDoesNotChangeTheSampleOfTheSelectedWidth() {
        CanvasViewport viewport = new CanvasViewport();
        viewport.resize(600, 800, 2);
        BrushStrokePreview preview = new BrushStrokePreview(RuntimeEnvironment.getApplication(), viewport);
        preview.layout(0, 0, 200, 160);
        preview.setBrush(Color.BLUE, 24);
        Bitmap original = render(preview);
        viewport.transform(4, 300, 400, 300, 400);
        preview.setBrush(Color.BLUE, 24);
        assertTrue(original.sameAs(render(preview)));
    }

    private Bitmap render(BrushStrokePreview preview) {
        Bitmap bitmap = Bitmap.createBitmap(200, 160, Bitmap.Config.ARGB_8888);
        preview.draw(new Canvas(bitmap));
        return bitmap;
    }

    private int coloredPixels(Bitmap bitmap, int color) {
        int count = 0;
        for (int y = 0; y < bitmap.getHeight(); y++) for (int x = 0; x < bitmap.getWidth(); x++) {
            int pixel = bitmap.getPixel(x, y);
            if (Math.abs(Color.red(pixel) - Color.red(color)) < 100
                    && Math.abs(Color.green(pixel) - Color.green(color)) < 100
                    && Math.abs(Color.blue(pixel) - Color.blue(color)) < 100) count++;
        }
        return count;
    }
}
