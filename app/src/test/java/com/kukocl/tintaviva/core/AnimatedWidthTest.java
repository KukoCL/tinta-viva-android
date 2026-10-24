package com.kukocl.tintaviva.core;

import org.junit.Test;

import static org.junit.Assert.*;

public class AnimatedWidthTest {
    private Stroke line() {
        float[] points = new float[402];
        for (int i = 0; i < 201; i++) { points[i * 2] = 100 + i * 4; points[i * 2 + 1] = 100; }
        return new Stroke(7, Layer.LINE_ART, 0xff000000, 40, points);
    }

    @Test public void eachFrameHasDifferentWidthsAlongTheSameStroke() {
        Stroke stroke = line();
        float[] widths = LineAnimation.widthsAt(stroke, 0);
        float min = Float.MAX_VALUE, max = 0;
        for (float width : widths) { min = Math.min(min, width); max = Math.max(max, width); }
        assertTrue("El mismo trazo debe tener zonas finas y gruesas", max - min > stroke.width * .2f);
    }

    @Test public void widthAnimationChangesLocallyRatherThanScalingTheWholeStroke() {
        Stroke stroke = line();
        float[] first = LineAnimation.widthsAt(stroke, 0);
        float[] next = LineAnimation.widthsAt(stroke, 1);
        assertNotEquals(first[10], next[10], .001f);
        assertNotEquals(first[90], next[90], .001f);
        assertNotEquals(next[10] / first[10], next[90] / first[90], .01f);
    }

    @Test public void allWidthsStayWithinTwentyTwoPercentAndRepeatWithoutDrift() {
        Stroke stroke = line();
        for (int frame = 0; frame < 1000; frame++) {
            for (float width : LineAnimation.widthsAt(stroke, frame)) {
                assertTrue(width >= stroke.width * .78f - .001f);
                assertTrue(width <= stroke.width * 1.22f + .001f);
            }
        }
        assertArrayEquals(LineAnimation.widthsAt(stroke, 0),
                LineAnimation.widthsAt(stroke, LineAnimation.FRAME_COUNT), 0);
        assertEquals(40, stroke.width, 0);
    }

    @Test public void sketchKeepsItsOriginalWidthInEveryFrame() {
        Stroke sketch = new Stroke(1, Layer.SKETCH, 0xff000000, 12, new float[]{10, 10, 50, 50});
        for (int frame = 0; frame < LineAnimation.FRAME_COUNT; frame++) {
            assertArrayEquals(new float[]{12, 12}, LineAnimation.widthsAt(sketch, frame), 0);
        }
    }

    @Test public void widthWavesAreStableForSavedIdsAndDifferentBetweenStrokes() {
        Stroke a = line();
        Stroke restored = new Stroke(a.id, a.layer, a.color, a.width, a.copyPoints());
        Stroke other = new Stroke(a.id + 1, a.layer, a.color, a.width, a.copyPoints());
        assertArrayEquals(LineAnimation.widthsAt(a, 5), LineAnimation.widthsAt(restored, 5), 0);
        assertFalse(java.util.Arrays.equals(LineAnimation.widthsAt(a, 5), LineAnimation.widthsAt(other, 5)));
    }

    @Test public void eraserIncludesAnExpandedPartOfTheVisibleStroke() {
        Stroke stroke = line();
        int frame = 3;
        float[] widths = LineAnimation.widthsAt(stroke, frame);
        int widest = 1;
        for (int i = 2; i < widths.length - 1; i++) if (widths[i] > widths[widest]) widest = i;
        assertTrue(widths[widest] > stroke.width);
        float[] point = offsetNormal(stroke, frame, widest, (widths[widest] + stroke.width) / 4);
        assertTrue(HitTest.touches(stroke, point[0], point[1], 0, frame));
    }

    @Test public void eraserDoesNotUseTheOldFullWidthWhereTheVisibleStrokeIsThinner() {
        Stroke stroke = line();
        int frame = 3;
        float[] widths = LineAnimation.widthsAt(stroke, frame);
        int thinnest = 1;
        for (int i = 2; i < widths.length - 1; i++) if (widths[i] < widths[thinnest]) thinnest = i;
        assertTrue(widths[thinnest] < stroke.width);
        float[] point = offsetNormal(stroke, frame, thinnest, (widths[thinnest] + stroke.width) / 4);
        assertFalse(HitTest.touches(stroke, point[0], point[1], 0, frame));
    }

    @Test public void aSinglePointAlsoGetsAnAnimatedDiameterAndCorrectEraserRadius() {
        Stroke dot = new Stroke(9, Layer.LINE_ART, 0xff000000, 20, new float[]{100, 100});
        for (int frame = 0; frame < LineAnimation.FRAME_COUNT; frame++) {
            float[] point = LineAnimation.pointsAt(dot, frame);
            float radius = LineAnimation.widthsAt(dot, frame)[0] / 2;
            assertTrue(HitTest.touches(dot, point[0] + radius - .1f, point[1], 0, frame));
            assertFalse(HitTest.touches(dot, point[0] + radius + .1f, point[1], 0, frame));
        }
    }

    private float[] offsetNormal(Stroke stroke, int frame, int sample, float distance) {
        float[] points = LineAnimation.pointsAt(stroke, frame);
        float dx = points[sample * 2 + 2] - points[sample * 2 - 2];
        float dy = points[sample * 2 + 3] - points[sample * 2 - 1];
        float length = (float) Math.hypot(dx, dy);
        return new float[]{points[sample * 2] - dy / length * distance,
                points[sample * 2 + 1] + dx / length * distance};
    }
}
