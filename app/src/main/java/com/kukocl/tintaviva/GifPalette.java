package com.kukocl.tintaviva;

import com.squareup.gifencoder.Color;
import com.squareup.gifencoder.HashMultiset;
import com.squareup.gifencoder.MedianCutQuantizer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** One palette per animation, with bounded sampling and cached color matching. */
final class GifPalette {
    final int[] colors;
    private final short[] nearest = new short[32 * 32 * 32];

    GifPalette(int[] pixels) {
        Set<Integer> exact = new HashSet<>();
        for (int pixel : pixels) {
            exact.add(pixel & 0xffffff);
            if (exact.size() > 256) break;
        }
        if (exact.size() <= 256) {
            colors = new int[exact.size()];
            int index = 0;
            for (int color : exact) colors[index++] = color;
        } else {
            // Sorting millions of colors and diffusing error for every frame stalls phones.
            HashMultiset<Color> sample = new HashMultiset<>();
            int step = Math.max(1, (pixels.length + 4095) / 4096);
            for (int i = 0; i < pixels.length; i += step) sample.add(Color.fromRgbInt(pixels[i]));
            Set<Color> reduced = sample.getDistinctElements().size() > 254
                    ? MedianCutQuantizer.INSTANCE.quantize(sample, 254) : sample.getDistinctElements();
            Set<Integer> palette = new HashSet<>();
            for (Color color : reduced) palette.add(color.getRgbInt());
            palette.add(0xffffff);
            palette.add(0);
            colors = new int[palette.size()];
            int index = 0;
            for (int color : palette) colors[index++] = color;
        }
        Arrays.sort(colors);
        Arrays.fill(nearest, (short) -1);
    }

    void index(int[] pixels, byte[] indices) {
        for (int i = 0; i < pixels.length; i++) indices[i] = (byte) index(pixels[i]);
    }

    private int index(int pixel) {
        int rgb = pixel & 0xffffff;
        int exact = Arrays.binarySearch(colors, rgb);
        if (exact >= 0) return exact;
        int bucket = ((rgb >>> 9) & 0x7c00) | ((rgb >>> 6) & 0x3e0) | ((rgb >>> 3) & 0x1f);
        if (nearest[bucket] >= 0) return nearest[bucket];
        int red = (rgb >>> 16) & 255, green = (rgb >>> 8) & 255, blue = rgb & 255;
        int best = 0, distance = Integer.MAX_VALUE;
        for (int i = 0; i < colors.length; i++) {
            int dr = red - ((colors[i] >>> 16) & 255);
            int dg = green - ((colors[i] >>> 8) & 255);
            int db = blue - (colors[i] & 255);
            int candidate = dr * dr + dg * dg + db * db;
            if (candidate < distance) { distance = candidate; best = i; }
        }
        nearest[bucket] = (short) best;
        return best;
    }
}
