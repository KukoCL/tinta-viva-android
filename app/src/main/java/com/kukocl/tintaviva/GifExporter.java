package com.kukocl.tintaviva;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;

import com.kukocl.tintaviva.core.DrawingDocument;
import com.kukocl.tintaviva.core.LineAnimation;
import com.kukocl.tintaviva.core.Stroke;
import com.squareup.gifencoder.GifEncoder;
import com.squareup.gifencoder.ImageOptions;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Exports a frozen document, independent of the editor's zoom, controls and later edits. */
final class GifExporter {
    static final int WIDTH = 540, HEIGHT = 675;

    static final class Snapshot {
        final List<Stroke> strokes;
        final Bitmap sketchImage;
        final float sketchOpacity;
        final boolean strongMotion;

        Snapshot(DrawingDocument document, Bitmap sketchImage) {
            strokes = Collections.unmodifiableList(new ArrayList<>(document.getStrokes()));
            this.sketchImage = sketchImage;
            sketchOpacity = document.sketchOpacity();
            strongMotion = document.isStrongMotion();
        }
    }

    static void write(OutputStream output, Snapshot snapshot) throws IOException {
        write(output, snapshot, WIDTH, HEIGHT);
    }

    static void write(OutputStream output, Snapshot snapshot, int width, int height) throws IOException {
        GifEncoder encoder = new GifEncoder(output, width, height, 0);
        Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int[] pixels = new int[width * height];
        DrawingRenderer renderer = new DrawingRenderer();
        try {
            for (int frame = 0; frame < LineAnimation.FRAME_COUNT; frame++) {
                image.eraseColor(Color.WHITE);
                Canvas canvas = new Canvas(image);
                canvas.scale((float) width / DrawingDocument.WIDTH, (float) height / DrawingDocument.HEIGHT);
                renderer.draw(canvas, snapshot.strokes, snapshot.sketchImage, snapshot.sketchOpacity,
                        snapshot.strongMotion, frame);
                image.getPixels(pixels, 0, width, 0, 0, width, height);
                // GIF stores hundredths of a second: alternate 12/13 to preserve a one-second loop.
                ImageOptions options = new ImageOptions().setDelay(frame % 2 == 0 ? 120 : 130,
                        TimeUnit.MILLISECONDS);
                encoder.addImage(pixels, width, options);
                renderer.clear();
            }
            encoder.finishEncoding();
        } finally {
            image.recycle();
        }
    }
}
