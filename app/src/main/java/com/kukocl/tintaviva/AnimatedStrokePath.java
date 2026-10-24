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
        // Split hairpin turns into overlapping round-capped contours. This prevents
        // their two offset edges crossing and leaving a triangular hole near the turn.
        int start = 0;
        for (int i = 1; i < count - 1; i++) {
            if (nx[i - 1] * nx[i] + ny[i - 1] * ny[i] < -.95f) {
                ribbon(path, points, radii, nx, ny, start, i);
                start = i;
            }
        }
        ribbon(path, points, radii, nx, ny, start, count - 1);
        return path;
    }

    private static void ribbon(Path path, float[] points, float[] radii, float[] nx, float[] ny,
                               int start, int end) {
        path.moveTo(points[start * 2] + nx[start] * radii[start],
                points[start * 2 + 1] + ny[start] * radii[start]);
        for (int i = start + 1; i < end; i++) join(path, points, radii, nx, ny, i, 1, false);
        path.lineTo(points[end * 2] + nx[end - 1] * radii[end],
                points[end * 2 + 1] + ny[end - 1] * radii[end]);
        arc(path, points[end * 2], points[end * 2 + 1], radii[end],
                angle(nx[end - 1], ny[end - 1]), -180);
        for (int i = end - 1; i > start; i--) join(path, points, radii, nx, ny, i, -1, true);
        path.lineTo(points[start * 2] - nx[start] * radii[start],
                points[start * 2 + 1] - ny[start] * radii[start]);
        arc(path, points[start * 2], points[start * 2 + 1], radii[start],
                angle(-nx[start], -ny[start]), -180);
        path.close();
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
