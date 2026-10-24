package com.kukocl.tintaviva.core;

public final class HitTest {
    private HitTest() { }

    public static boolean touches(Stroke stroke, float x, float y, float tolerance, int frame) {
        float[] points = LineAnimation.pointsAt(stroke, frame);
        float radius = stroke.width / 2f + Math.max(0, tolerance);
        float radiusSquared = radius * radius;
        if (points.length == 2) {
            return squared(x - points[0], y - points[1]) <= radiusSquared;
        }
        for (int i = 2; i < points.length; i += 2) {
            float ax = points[i - 2], ay = points[i - 1];
            float dx = points[i] - ax, dy = points[i + 1] - ay;
            float lengthSquared = squared(dx, dy);
            float t = lengthSquared == 0 ? 0 :
                    Math.max(0, Math.min(1, ((x - ax) * dx + (y - ay) * dy) / lengthSquared));
            if (squared(x - ax - t * dx, y - ay - t * dy) <= radiusSquared) return true;
        }
        return false;
    }

    private static float squared(float x, float y) { return x * x + y * y; }
}
