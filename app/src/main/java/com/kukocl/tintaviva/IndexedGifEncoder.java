package com.kukocl.tintaviva;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Streaming GIF89a writer; the LZW dictionary stores integer prefix/suffix pairs. */
final class IndexedGifEncoder {
    private final OutputStream output;
    private final int width, height, minimumCodeSize;
    private final int[] keys = new int[8192], codes = new int[8192];
    private final byte[] block = new byte[255];
    private int blockSize, bits, bitCount;

    IndexedGifEncoder(OutputStream output, int width, int height, int[] palette) throws IOException {
        this.output = output;
        this.width = width;
        this.height = height;
        int tableBits = 1;
        while ((1 << tableBits) < palette.length) tableBits++;
        minimumCodeSize = Math.max(2, tableBits);
        output.write("GIF89a".getBytes(StandardCharsets.US_ASCII));
        word(width);
        word(height);
        output.write(0xf0 | (tableBits - 1)); // Global palette, eight-bit color resolution.
        output.write(0);
        output.write(0);
        for (int i = 0; i < (1 << tableBits); i++) {
            int color = i < palette.length ? palette[i] : 0;
            output.write(color >>> 16);
            output.write(color >>> 8);
            output.write(color);
        }
        output.write(new byte[]{0x21, (byte) 0xff, 11});
        output.write("NETSCAPE2.0".getBytes(StandardCharsets.US_ASCII));
        output.write(new byte[]{3, 1, 0, 0, 0}); // Infinite loop.
    }

    void addFrame(byte[] indices, int delayCentiseconds) throws IOException {
        if (indices.length != width * height) throw new IllegalArgumentException("Invalid frame size");
        output.write(new byte[]{0x21, (byte) 0xf9, 4, 4}); // Opaque frame, do not dispose.
        word(delayCentiseconds);
        output.write(0);
        output.write(0);
        output.write(0x2c);
        word(0);
        word(0);
        word(width);
        word(height);
        output.write(0); // Use the global palette.
        writeLzw(indices);
    }

    void finish() throws IOException { output.write(0x3b); }

    private void writeLzw(byte[] indices) throws IOException {
        output.write(minimumCodeSize);
        Arrays.fill(keys, -1);
        blockSize = bits = bitCount = 0;
        int clear = 1 << minimumCodeSize, end = clear + 1;
        int next = end + 1, codeSize = minimumCodeSize + 1;
        code(clear, codeSize);
        int prefix = indices[0] & 255;
        for (int i = 1; i < indices.length; i++) {
            int suffix = indices[i] & 255;
            int key = (prefix << 8) | suffix;
            int slot = (key * 0x9e3779b9) >>> 19;
            while (keys[slot] != -1 && keys[slot] != key) slot = (slot + 1) & (keys.length - 1);
            if (keys[slot] == key) {
                prefix = codes[slot];
                continue;
            }
            code(prefix, codeSize);
            if (next < 4096) {
                keys[slot] = key;
                codes[slot] = next;
                if (next == (1 << codeSize) && codeSize < 12) codeSize++;
                next++;
            } else {
                code(clear, codeSize);
                Arrays.fill(keys, -1);
                next = end + 1;
                codeSize = minimumCodeSize + 1;
            }
            prefix = suffix;
        }
        code(prefix, codeSize);
        // The decoder adds its last dictionary entry when reading the final pixel code.
        if (next == (1 << codeSize) && codeSize < 12) codeSize++;
        code(end, codeSize);
        if (bitCount > 0) dataByte(bits & 255);
        flushBlock();
        output.write(0);
    }

    private void code(int code, int size) throws IOException {
        bits |= code << bitCount;
        bitCount += size;
        while (bitCount >= 8) {
            dataByte(bits & 255);
            bits >>>= 8;
            bitCount -= 8;
        }
    }

    private void dataByte(int value) throws IOException {
        block[blockSize++] = (byte) value;
        if (blockSize == block.length) flushBlock();
    }

    private void flushBlock() throws IOException {
        if (blockSize == 0) return;
        output.write(blockSize);
        output.write(block, 0, blockSize);
        blockSize = 0;
    }

    private void word(int value) throws IOException {
        output.write(value);
        output.write(value >>> 8);
    }
}
