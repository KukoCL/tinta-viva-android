package com.kukocl.tintaviva.core;

import org.junit.Test;

import static org.junit.Assert.*;

public class StrokeBuilderTest {
    @Test public void singleTapIsKeptAsOnePoint() {
        StrokeBuilder builder = new StrokeBuilder();
        builder.add(40, 50);
        builder.add(40.1f, 50.1f);
        assertArrayEquals(new float[]{40, 50}, builder.finish(), 0);
    }

    @Test public void sparseAndDenseStraightInputProduceTheSameSamples() {
        StrokeBuilder sparse = new StrokeBuilder();
        sparse.add(0, 0); sparse.add(100, 0);
        StrokeBuilder dense = new StrokeBuilder();
        for (int x = 0; x <= 100; x += 2) dense.add(x, 0);
        assertArrayEquals(sparse.finish(), dense.finish(), .001f);
    }

    @Test public void resamplingKeepsTheLastPointOnAnUnevenLength() {
        StrokeBuilder builder = new StrokeBuilder();
        builder.add(0, 0); builder.add(11, 0);
        assertArrayEquals(new float[]{0, 0, 4, 0, 8, 0, 11, 0}, builder.finish(), .001f);
    }

    @Test public void buildingOrCancellingInputDoesNotCommitItToTheDocument() {
        DrawingDocument document = new DrawingDocument();
        StrokeBuilder builder = new StrokeBuilder();
        builder.add(0, 0); builder.add(20, 10);
        builder.finish();
        assertFalse(document.canUndo());
        assertTrue(document.getStrokes().isEmpty());
    }

    @Test public void invalidTouchCoordinatesAreIgnored() {
        StrokeBuilder builder = new StrokeBuilder();
        builder.add(Float.NaN, 50);
        builder.add(50, Float.POSITIVE_INFINITY);
        assertTrue(builder.isEmpty());
    }
}
