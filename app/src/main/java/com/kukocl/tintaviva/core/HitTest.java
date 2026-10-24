package com.kukocl.tintaviva.core;

public final class HitTest {
    private HitTest() { }

    public static boolean touches(Stroke stroke, float x, float y, float tolerance, int frame) {
        return touches(stroke, x, y, tolerance, frame, true);
    }

    public static boolean touches(Stroke stroke, float x, float y, float tolerance, int frame, boolean strongMotion) {
        float[] points = LineAnimation.pointsAt(stroke, frame, strongMotion);
        float[] widths = LineAnimation.widthsAt(stroke, frame);
        float padding = Math.max(0, tolerance);
        if (points.length == 2) {
            float radius = widths[0] / 2 + padding;
            return squared(x - points[0], y - points[1]) <= radius * radius;
        }
        for (int i = 2; i < points.length; i += 2) {
            float ax = points[i - 2], ay = points[i - 1];
            float dx = points[i] - ax, dy = points[i + 1] - ay;
            float lengthSquared = squared(dx, dy);
            float t = lengthSquared == 0 ? 0 :
                    Math.max(0, Math.min(1, ((x - ax) * dx + (y - ay) * dy) / lengthSquared));
            int sample = i / 2;
            float radius = (widths[sample - 1] + (widths[sample] - widths[sample - 1]) * t) / 2 + padding;
            if (squared(x - ax - t * dx, y - ay - t * dy) <= radius * radius) return true;
        }
        return false;
    }

    private static float squared(float x, float y) { return x * x + y * y; }
}
