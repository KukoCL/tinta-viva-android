package com.kukocl.tintaviva;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;

import com.kukocl.tintaviva.core.DrawingDocument;
import com.kukocl.tintaviva.core.Layer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = Application.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class DrawingRendererTest {
    private Bitmap reference() {
        Bitmap image = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888);
        image.eraseColor(Color.RED);
        return image;
    }

    private DrawingDocument document() {
        DrawingDocument document = new DrawingDocument();
        document.addStroke(Color.BLUE, 100, new float[]{300, 675, 780, 675});
        return document;
    }

    @Test public void sketchStrokesDrawAboveTheImportedImageAndTheImageKeepsItsAspectRatio() {
        Bitmap result = render(document(), reference());
        assertEquals(Color.BLUE, result.getPixel(54, 67));
        assertEquals(Color.RED, result.getPixel(54, 35));
        assertEquals(Color.WHITE, result.getPixel(54, 5));
    }

    @Test public void imageAndOverlappingSketchFadeTogetherToThirtyPercent() {
        DrawingDocument document = document();
        document.setActiveLayer(Layer.LINE_ART);
        Bitmap result = render(document, reference());
        assertBlendedColor(Color.rgb(178, 178, 255), result.getPixel(54, 67));
        assertBlendedColor(Color.rgb(255, 178, 178), result.getPixel(54, 35));
    }

    @Test public void lineArtOnlyHidesBothTheImageAndSketchStrokes() {
        DrawingDocument document = document();
        document.showOnlyLineArt();
        Bitmap result = render(document, reference());
        assertEquals(Color.WHITE, result.getPixel(54, 67));
        assertEquals(Color.WHITE, result.getPixel(54, 35));
        document.addStroke(Color.GREEN, 100, new float[]{300, 675, 780, 675});
        assertEquals(Color.GREEN, render(document, reference()).getPixel(54, 67));
    }

    @Test public void removingTheImagePreservesTheSketchDrawnAboveIt() {
        Bitmap result = render(document(), null);
        assertEquals(Color.BLUE, result.getPixel(54, 67));
        assertEquals(Color.WHITE, result.getPixel(54, 35));
    }

    private Bitmap render(DrawingDocument document, Bitmap reference) {
        Bitmap bitmap = Bitmap.createBitmap(108, 135, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(Color.WHITE);
        Canvas canvas = new Canvas(bitmap);
        canvas.scale(.1f, .1f);
        new DrawingRenderer().draw(canvas, document.getStrokes(), reference, document.sketchOpacity(),
                document.isStrongMotion(), 0);
        return bitmap;
    }

    private void assertBlendedColor(int expected, int actual) {
        // Skia's layer compositing can round a channel by one 8-bit step.
        assertEquals(Color.red(expected), Color.red(actual), 1);
        assertEquals(Color.green(expected), Color.green(actual), 1);
        assertEquals(Color.blue(expected), Color.blue(actual), 1);
        assertEquals(255, Color.alpha(actual));
    }
}
