package com.kukocl.tintaviva.core;

import java.util.Arrays;

/** Eight repeating drawings at 8 fps, with selectable movement and irregular local widths. */
public final class LineAnimation {
    public static final int FRAME_COUNT = 8;
    public static final long FRAME_MS = 125;
    public static final float MAX_DISPLACEMENT = 8f;
    public static final float MAX_WIDTH_VARIATION = .55f;

    private LineAnimation() { }

    public static int frameAt(long elapsedMillis) {
        return (int) Math.floorMod(elapsedMillis / FRAME_MS, (long) FRAME_COUNT);
    }

    public static float[] pointsAt(Stroke stroke, int frame) {
        return pointsAt(stroke, frame, true);
    }

    public static float[] pointsAt(Stroke stroke, int frame, boolean strongMotion) {
        float[] points = stroke.copyPoints();
        if (stroke.layer == Layer.SKETCH) return points;
        int wrappedFrame = Math.floorMod(frame, FRAME_COUNT);
        double seed = (stroke.id * 0.754877666) % (Math.PI * 2);
        double phase = wrappedFrame * Math.PI * 2 / FRAME_COUNT;
        float amplitude = strongMotion ? Math.min(MAX_DISPLACEMENT, 4.8f + stroke.width * .1f)
                : Math.min(3.2f, 1.9f + stroke.width * .035f);
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

    /** Each frame redraws fixed regions independently, so wide areas do not travel along the stroke. */
    public static float[] widthsAt(Stroke stroke, int frame) {
        float[] points = stroke.copyPoints();
        float[] widths = new float[points.length / 2];
        if (stroke.layer == Layer.SKETCH) {
            Arrays.fill(widths, stroke.width);
            return widths;
        }
        int wrappedFrame = Math.floorMod(frame, FRAME_COUNT);
        float arcLength = 0;
        for (int i = 0; i < widths.length; i++) {
            if (i > 0) arcLength += (float) Math.hypot(points[i * 2] - points[i * 2 - 2],
                    points[i * 2 + 1] - points[i * 2 - 1]);
            float variation = MAX_WIDTH_VARIATION * (
                    .75f * widthNoise(stroke.id, arcLength / 96f, wrappedFrame, 0x632be59bd9b4e019L)
                    + .25f * widthNoise(stroke.id, arcLength / 40f, wrappedFrame, 0x8cb92ba72f3d8dd7L));
            widths[i] = stroke.width * (1 + variation);
        }
        return widths;
    }

    private static float widthNoise(long strokeId, float position, int frame, long salt) {
        int region = (int) Math.floor(position);
        float blend = position - region;
        // Smooth neighboring regions spatially while giving each region its own frame sequence.
        blend = blend * blend * (3 - 2 * blend);
        float left = regionWidth(strokeId, region, frame, salt);
        float right = regionWidth(strokeId, region + 1, frame, salt);
        return left + (right - left) * blend;
    }

    private static float regionWidth(long strokeId, int region, int frame, long salt) {
        // Stable hashing preserves the same animation after reopening a saved drawing.
        long value = strokeId ^ region * 0x9e3779b97f4a7c15L ^ frame * 0xd1b54a32d192ed03L ^ salt;
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        value ^= value >>> 31;
        return (value >>> 40) / 8388607.5f - 1;
    }
}
