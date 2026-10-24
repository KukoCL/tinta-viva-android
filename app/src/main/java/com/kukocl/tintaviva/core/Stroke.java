package com.kukocl.tintaviva.core;

/** An immutable stroke. Coordinates belong to the document, never to the screen. */
public final class Stroke {
    public final long id;
    public final Layer layer;
    public final int color;
    public final float width;
    private final float[] points;

    public Stroke(long id, Layer layer, int color, float width, float[] points) {
        if (id < 1 || layer == null || !Float.isFinite(width) || width <= 0 || width > 100
                || points == null || points.length < 2 || points.length % 2 != 0) {
            throw new IllegalArgumentException("Invalid stroke");
        }
        for (float coordinate : points) {
            if (!Float.isFinite(coordinate)) throw new IllegalArgumentException("Invalid point");
        }
        this.id = id;
        this.layer = layer;
        this.color = color | 0xff000000;
        this.width = width;
        this.points = points.clone();
    }

    public float[] copyPoints() { return points.clone(); }
    public int pointCount() { return points.length / 2; }
}
