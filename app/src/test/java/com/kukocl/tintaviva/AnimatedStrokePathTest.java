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

import static org.junit.Assert.*;

/** Real Skia pixels check that the filled contour has taper, rounded joins and no cusp holes. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = Application.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class AnimatedStrokePathTest {
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
}
