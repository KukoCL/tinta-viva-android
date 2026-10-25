package com.kukocl.tintaviva;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.RuntimeEnvironment;

import com.kukocl.tintaviva.core.DrawingDocument;
import com.kukocl.tintaviva.core.Layer;

import static org.junit.Assert.*;

/** Real Skia pixels check that the filled contour has taper, rounded joins and no cusp holes. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = Application.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class AnimatedStrokePathTest {
    @Test public void changingMotionStrengthImmediatelyRebuildsCachedDrawingGeometry() {
        DrawingDocument document = new DrawingDocument();
        document.setActiveLayer(Layer.LINE_ART);
        document.addStroke(Color.BLACK, 12, new float[]{100, 100, 200, 140, 300, 100});
        DrawingView view = new DrawingView(RuntimeEnvironment.getApplication(), document);
        view.layout(0, 0, 600, 800);
        Bitmap strong = renderView(view);
        view.setStrongMotion(false);
        Bitmap soft = renderView(view);
        assertFalse("El cambio de fuerza debe actualizar los trazos que ya estaban dibujados", strong.sameAs(soft));
        view.setStrongMotion(true);
        assertTrue(strong.sameAs(renderView(view)));
    }

    @Test public void oneStrokeCanBeThinAtOneEndAndThickAtTheOther() {
        Bitmap image = render(new float[]{100, 100, 300, 100}, new float[]{10, 30});
        assertEquals(Color.WHITE, image.getPixel(110, 110));
        assertEquals(Color.BLACK, image.getPixel(290, 110));
        assertEquals(Color.WHITE, image.getPixel(290, 118));
        assertEquals(Color.BLACK, image.getPixel(96, 100));
        assertEquals(Color.BLACK, image.getPixel(313, 100));
    }

    @Test public void rightAngleCornersHaveRoundOutsideJoins() {
        Bitmap image = render(new float[]{100, 100, 200, 100, 200, 200}, new float[]{20, 20, 20});
        assertEquals(Color.BLACK, image.getPixel(205, 95));
        assertEquals(Color.WHITE, image.getPixel(209, 90));
        assertEquals(Color.BLACK, image.getPixel(195, 105));
    }

    @Test public void reversingDirectionKeepsARoundCapWithoutHoles() {
        Bitmap image = render(new float[]{100, 100, 200, 100, 100, 100}, new float[]{12, 12, 12});
        assertEquals(Color.BLACK, image.getPixel(150, 100));
        assertEquals(Color.BLACK, image.getPixel(190, 100));
        assertEquals(Color.BLACK, image.getPixel(204, 100));
        assertEquals(Color.WHITE, image.getPixel(208, 100));
    }

    @Test public void repeatedPointsAndDotsStillDrawAFilledCircle() {
        Bitmap image = render(new float[]{100, 100, 100, 100}, new float[]{16, 16});
        assertEquals(Color.BLACK, image.getPixel(100, 100));
        assertEquals(Color.BLACK, image.getPixel(106, 100));
        assertEquals(Color.WHITE, image.getPixel(110, 100));
    }

    @Test public void aBrushWiderThanATinyLoopFillsItsCenter() {
        float[] points = new float[34], widths = new float[17];
        for (int i = 0; i < 17; i++) {
            double angle = i * Math.PI * 2 / 16;
            points[i * 2] = 100 + 8 * (float) Math.cos(angle);
            points[i * 2 + 1] = 100 + 8 * (float) Math.sin(angle);
            widths[i] = 24;
        }
        Bitmap image = render(points, widths);
        assertEquals(Color.BLACK, image.getPixel(100, 100));
        assertEquals(Color.BLACK, image.getPixel(118, 100));
        assertEquals(Color.WHITE, image.getPixel(124, 100));
    }

    private Bitmap render(float[] points, float[] widths) {
        Bitmap image = Bitmap.createBitmap(400, 250, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        canvas.drawColor(Color.WHITE);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.BLACK);
        Path path = AnimatedStrokePath.create(points, widths);
        canvas.drawPath(path, paint);
        return image;
    }

    private Bitmap renderView(DrawingView view) {
        Bitmap image = Bitmap.createBitmap(600, 800, Bitmap.Config.ARGB_8888);
        view.draw(new Canvas(image));
        return image;
    }
}
