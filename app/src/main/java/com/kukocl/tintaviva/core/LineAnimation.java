package com.kukocl.tintaviva.core;

/** Eight repeating drawings at 8 fps: a gentle hand-drawn line boil, without drift. */
public final class LineAnimation {
    public static final int FRAME_COUNT = 8;
    public static final long FRAME_MS = 125;

    private LineAnimation() { }

    public static int frameAt(long elapsedMillis) {
        return (int) Math.floorMod(elapsedMillis / FRAME_MS, (long) FRAME_COUNT);
    }

    public static float[] pointsAt(Stroke stroke, int frame) {
        float[] points = stroke.copyPoints();
        if (stroke.layer == Layer.SKETCH) return points;
        int wrappedFrame = Math.floorMod(frame, FRAME_COUNT);
        double seed = (stroke.id * 0.754877666) % (Math.PI * 2);
        double phase = wrappedFrame * Math.PI * 2 / FRAME_COUNT;
        float amplitude = Math.min(3.2f, 1.9f + stroke.width * .035f);
        float arcLength = 0;
        float previousX = points[0], previousY = points[1];
        for (int i = 0; i < points.length; i += 2) {
            float x = points[i], y = points[i + 1];
            if (i > 0) arcLength += (float) Math.hypot(x - previousX, y - previousY);
            points[i] = x + amplitude * (float) (
                    .7 * Math.sin(arcLength * .026 + seed + phase)
                    + .3 * Math.sin(arcLength * .061 + seed * 2 - phase * 2));
            points[i + 1] = y + amplitude * (float) (
                    .7 * Math.cos(arcLength * .029 + seed * 1.7 - phase)
                    + .3 * Math.sin(arcLength * .057 + seed + phase * 2));
            previousX = x;
            previousY = y;
        }
        return points;
    }
}
