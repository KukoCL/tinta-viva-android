package com.kukocl.tintaviva.core;

import org.junit.Test;

import static org.junit.Assert.*;

public class LineAnimationTest {
    private static final float[] POINTS = {100, 100, 104, 100, 108, 102, 112, 104};

    @Test public void sketchStaysStillInEveryFrame() {
        Stroke stroke = new Stroke(1, Layer.SKETCH, 0xff000000, 8, POINTS);
        for (int frame = 0; frame < LineAnimation.FRAME_COUNT; frame++) {
            assertArrayEquals(POINTS, LineAnimation.pointsAt(stroke, frame), 0);
        }
    }

    @Test public void lineArtMovesAndLoopsWithoutAccumulatingDrift() {
        Stroke stroke = new Stroke(2, Layer.LINE_ART, 0xff000000, 8, POINTS);
        float[] first = LineAnimation.pointsAt(stroke, 0);
        float[] second = LineAnimation.pointsAt(stroke, 1);
        assertFalse(java.util.Arrays.equals(first, second));
        assertArrayEquals(first, LineAnimation.pointsAt(stroke, LineAnimation.FRAME_COUNT), 0);
        assertArrayEquals(POINTS, stroke.copyPoints(), 0);
        for (int frame = 0; frame < 1000; frame++) {
            float[] animated = LineAnimation.pointsAt(stroke, frame);
            for (int coordinate = 0; coordinate < animated.length; coordinate++) {
                assertTrue(Math.abs(animated[coordinate] - POINTS[coordinate]) <= 8f + .0001f);
            }
        }
    }

    @Test public void movementIsMorePronouncedThanThePreviousDisplacementLimit() {
        Stroke stroke = new Stroke(2, Layer.LINE_ART, 0xff000000, 12, POINTS);
        float peak = 0;
        for (int frame = 0; frame < LineAnimation.FRAME_COUNT; frame++) {
            float[] animated = LineAnimation.pointsAt(stroke, frame);
            for (int i = 0; i < animated.length; i++) {
                peak = Math.max(peak, Math.abs(animated[i] - POINTS[i]));
            }
        }
        assertTrue("El movimiento debe superar claramente el límite anterior de 3,2 unidades", peak > 4);
    }

    @Test public void strokesHaveDistinctDeterministicMotion() {
        Stroke a = new Stroke(1, Layer.LINE_ART, 0xff000000, 8, POINTS);
        Stroke b = new Stroke(2, Layer.LINE_ART, 0xff000000, 8, POINTS);
        assertFalse(java.util.Arrays.equals(LineAnimation.pointsAt(a, 3), LineAnimation.pointsAt(b, 3)));
        assertArrayEquals(LineAnimation.pointsAt(a, 3), LineAnimation.pointsAt(a, 3), 0);
    }

    @Test public void stationaryTapAlsoBecomesAnAnimatedDot() {
        Stroke dot = new Stroke(9, Layer.LINE_ART, 0xff7744cc, 5, new float[]{40, 40});
        float[] visible = LineAnimation.pointsAt(dot, 4);
        assertTrue(HitTest.touches(dot, visible[0], visible[1], 0, 4));
        assertFalse(HitTest.touches(dot, 500, 500, 5, 4));
    }

    @Test public void frameTimingIsEightFramesPerSecond() {
        assertEquals(0, LineAnimation.frameAt(0));
        assertEquals(0, LineAnimation.frameAt(124));
        assertEquals(1, LineAnimation.frameAt(125));
        assertEquals(7, LineAnimation.frameAt(999));
        assertEquals(0, LineAnimation.frameAt(1000));
    }

    @Test public void inputAndReturnedCoordinatesCannotMutateAStroke() {
        float[] input = {5, 10};
        Stroke stroke = new Stroke(1, Layer.SKETCH, 0xff123456, 5, input);
        input[0] = 999;
        float[] output = stroke.copyPoints();
        output[1] = 999;
        assertArrayEquals(new float[]{5, 10}, stroke.copyPoints(), 0);
    }
}
