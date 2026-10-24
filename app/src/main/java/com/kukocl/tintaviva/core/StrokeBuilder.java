package com.kukocl.tintaviva.core;

import java.util.ArrayList;

/** Collects input without changing the document until the finger or pen is lifted. */
public final class StrokeBuilder {
    private final ArrayList<Float> coordinates = new ArrayList<>();

    public void add(float x, float y) {
        if (!Float.isFinite(x) || !Float.isFinite(y)) return;
        int size = coordinates.size();
        if (size >= 2 && Math.hypot(x - coordinates.get(size - 2),
                y - coordinates.get(size - 1)) < 1.2) return;
        coordinates.add(x);
        coordinates.add(y);
    }

    public boolean isEmpty() { return coordinates.isEmpty(); }

    public float[] preview() {
        float[] result = new float[coordinates.size()];
        for (int i = 0; i < result.length; i++) result[i] = coordinates.get(i);
        return result;
    }

    /** Equal-distance samples keep the animation independent of input event frequency. */
    public float[] finish() {
        float[] raw = preview();
        if (raw.length <= 2) return raw;
        ArrayList<Float> samples = new ArrayList<>();
        samples.add(raw[0]);
        samples.add(raw[1]);
        float carried = 0;
        for (int i = 2; i < raw.length; i += 2) {
            float startX = raw[i - 2], startY = raw[i - 1];
            float dx = raw[i] - startX, dy = raw[i + 1] - startY;
            float length = (float) Math.hypot(dx, dy);
            if (length == 0) continue;
            float distance = 4f - carried;
            while (distance <= length) {
                float fraction = distance / length;
                samples.add(startX + dx * fraction);
                samples.add(startY + dy * fraction);
                distance += 4f;
            }
            carried = length - (distance - 4f);
        }
        int end = raw.length - 2;
        int last = samples.size() - 2;
        if (Math.hypot(raw[end] - samples.get(last), raw[end + 1] - samples.get(last + 1)) > .01) {
            samples.add(raw[end]);
            samples.add(raw[end + 1]);
        }
        float[] result = new float[samples.size()];
        for (int i = 0; i < result.length; i++) result[i] = samples.get(i);
        return result;
    }
}
