package com.kukocl.tintaviva;

import android.graphics.Path;

/** A tapered filled ribbon with round caps and joins, cached once per animation frame. */
final class AnimatedStrokePath {
    private AnimatedStrokePath() { }

    static Path create(float[] source, float[] widths) {
        if (source.length < 2 || source.length % 2 != 0 || widths.length * 2 != source.length) {
            throw new IllegalArgumentException("Missing stroke geometry");
        }
        // Repeated coordinates must not produce undefined normals or holes in the outline.
        float[] points = new float[source.length];
        float[] radii = new float[widths.length];
        int count = 0;
        for (int i = 0; i < widths.length; i++) {
            float x = source[i * 2], y = source[i * 2 + 1], radius = widths[i] / 2;
            if (count > 0 && Math.hypot(x - points[count * 2 - 2], y - points[count * 2 - 1]) < .001) {
                radii[count - 1] = Math.max(radii[count - 1], radius);
                continue;
            }
            points[count * 2] = x;
            points[count * 2 + 1] = y;
            radii[count++] = radius;
        }
        Path path = new Path();
        if (count == 1) {
            path.addCircle(points[0], points[1], radii[0], Path.Direction.CCW);
            return path;
        }
        float[] nx = new float[count - 1], ny = new float[count - 1];
        for (int i = 0; i < count - 1; i++) {
            float dx = points[i * 2 + 2] - points[i * 2];
            float dy = points[i * 2 + 3] - points[i * 2 + 1];
            float length = (float) Math.hypot(dx, dy);
            nx[i] = -dy / length;
            ny[i] = dx / length;
        }
        path.moveTo(points[0] + nx[0] * radii[0], points[1] + ny[0] * radii[0]);
        for (int i = 1; i < count - 1; i++) join(path, points, radii, nx, ny, i, 1, false);
        int last = count - 1;
        path.lineTo(points[last * 2] + nx[last - 1] * radii[last],
                points[last * 2 + 1] + ny[last - 1] * radii[last]);
        arc(path, points[last * 2], points[last * 2 + 1], radii[last],
                angle(nx[last - 1], ny[last - 1]), -180);
        for (int i = count - 2; i > 0; i--) join(path, points, radii, nx, ny, i, -1, true);
        path.lineTo(points[0] - nx[0] * radii[0], points[1] - ny[0] * radii[0]);
        arc(path, points[0], points[1], radii[0], angle(-nx[0], -ny[0]), -180);
        path.close();
        // A near-180-degree reversal is a round cusp, rather than a very long miter.
        for (int i = 1; i < count - 1; i++) {
            if (nx[i - 1] * nx[i] + ny[i - 1] * ny[i] < -.95f) {
                path.addCircle(points[i * 2], points[i * 2 + 1], radii[i], Path.Direction.CCW);
            }
        }
        return path;
    }

    private static void join(Path path, float[] points, float[] radii, float[] nx, float[] ny,
                             int i, int side, boolean reverse) {
        float x = points[i * 2], y = points[i * 2 + 1], radius = radii[i];
        float cross = nx[i - 1] * ny[i] - ny[i - 1] * nx[i];
        float dot = Math.max(-1, Math.min(1, nx[i - 1] * nx[i] + ny[i - 1] * ny[i]));
        if (cross * side < -.00001f) {
            int incoming = reverse ? i : i - 1;
            path.lineTo(x + side * nx[incoming] * radius, y + side * ny[incoming] * radius);
            float sweep = (float) Math.toDegrees(Math.atan2(cross, dot));
            arc(path, x, y, radius, angle(side * nx[incoming], side * ny[incoming]),
                    reverse ? -sweep : sweep);
        } else {
            float mx = nx[i - 1] + nx[i], my = ny[i - 1] + ny[i];
            float length = (float) Math.hypot(mx, my);
            if (length < .0001f) {
                path.lineTo(x + side * nx[i] * radius, y + side * ny[i] * radius);
                return;
            }
            mx /= length;
            my /= length;
            float distance = radius / Math.max(.5f, mx * nx[i] + my * ny[i]);
            path.lineTo(x + side * mx * distance, y + side * my * distance);
        }
    }

    private static float angle(float x, float y) { return (float) Math.toDegrees(Math.atan2(y, x)); }
    private static void arc(Path path, float x, float y, float radius, float start, float sweep) {
        path.arcTo(x - radius, y - radius, x + radius, y + radius, start, sweep, false);
    }
}
