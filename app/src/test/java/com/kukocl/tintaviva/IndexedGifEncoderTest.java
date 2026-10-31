package com.kukocl.tintaviva;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.util.Random;

import static org.junit.Assert.*;

public class IndexedGifEncoderTest {
    @Test public void independentDecoderReadsEveryPixelAcrossFullDictionaryResetsAndFrameBoundaries() throws Exception {
        int[] palette = palette(256);
        byte[][] frames = new byte[3][257 * 263];
        new Random(37).nextBytes(frames[0]);
        new Random(81).nextBytes(frames[2]);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        IndexedGifEncoder encoder = new IndexedGifEncoder(output, 257, 263, palette);
        for (byte[] frame : frames) encoder.addFrame(frame, 12);
        encoder.finish();
        try (GifExporterTest.GifReader reader = new GifExporterTest.GifReader(output.toByteArray())) {
            assertEquals(frames.length, reader.count());
            for (int frame = 0; frame < frames.length; frame++) {
                assertPixels(palette, frames[frame], reader.pixels(frame));
            }
        }
    }

    @Test public void smallPalettesAndCodeSizeBoundariesDecodeIncludingTheLastPixel() throws Exception {
        Random random = new Random(25);
        for (int count : new int[]{1, 2, 3, 4, 16, 255, 256}) {
            int[] palette = palette(count);
            for (int length : new int[]{1, 2, 7, 8, 31, 128, 255, 256, 257, 511, 512, 513, 4096, 16384}) {
                byte[] indices = new byte[length];
                for (int i = 0; i < length; i++) indices[i] = (byte) random.nextInt(count);
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                IndexedGifEncoder encoder = new IndexedGifEncoder(output, length, 1, palette);
                encoder.addFrame(indices, 13);
                encoder.finish();
                try (GifExporterTest.GifReader reader = new GifExporterTest.GifReader(output.toByteArray())) {
                    assertEquals(1, reader.count());
                    assertPixels(palette, indices, reader.pixels(0));
                }
            }
        }
    }

    private int[] palette(int count) {
        int[] colors = new int[count];
        for (int i = 0; i < count; i++) colors[i] = (i << 16) | (i << 8) | i;
        return colors;
    }

    private void assertPixels(int[] palette, byte[] indices, int[] pixels) {
        assertEquals(indices.length, pixels.length);
        for (int i = 0; i < indices.length; i++) {
            assertEquals("Pixel " + i, 0xff000000 | palette[indices[i] & 255], pixels[i]);
        }
    }
}
