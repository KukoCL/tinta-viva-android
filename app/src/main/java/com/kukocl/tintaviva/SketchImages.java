package com.kukocl.tintaviva;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.util.AtomicFile;

import com.kukocl.tintaviva.core.DrawingDocument;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/** Bounded image decoding and private copies, so source file permissions are not needed later. */
final class SketchImages {
    static final class Imported {
        final String name;
        final Bitmap bitmap;
        Imported(String name, Bitmap bitmap) { this.name = name; this.bitmap = bitmap; }
    }

    static Imported importImage(Context context, Uri uri) throws IOException {
        Bitmap bitmap = decode(context, uri);
        String name = "sketch-" + UUID.randomUUID() + ".png";
        File directory = new File(context.getFilesDir(), "sketch-images");
        if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Could not create image directory");
        AtomicFile file = new AtomicFile(file(context, name));
        FileOutputStream output = null;
        try {
            output = file.startWrite();
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) throw new IOException("Could not copy image");
            file.finishWrite(output);
            return new Imported(name, bitmap);
        } catch (IOException | RuntimeException error) {
            if (output != null) file.failWrite(output);
            bitmap.recycle();
            throw error;
        }
    }

    static Bitmap load(Context context, String name) throws IOException {
        Bitmap image = BitmapFactory.decodeFile(file(context, name).getPath());
        if (image == null) throw new IOException("Could not open sketch image");
        return image;
    }

    static File file(Context context, String name) throws IOException {
        if (name == null || !name.matches("sketch-[a-f0-9-]+\\.png")) throw new IOException("Invalid sketch image name");
        return new File(new File(context.getFilesDir(), "sketch-images"), name);
    }

    static void cleanUnused(Context context, String keep) {
        String[] originals = context.getFilesDir().list((directory, name) -> name.startsWith("drawing-unreadable-"));
        if (originals != null && originals.length > 0) return;
        File[] files = new File(context.getFilesDir(), "sketch-images").listFiles();
        if (files == null) return;
        for (File file : files) if (!file.getName().equals(keep)) file.delete();
    }

    private static Bitmap decode(Context context, Uri uri) throws IOException {
        if (Build.VERSION.SDK_INT >= 28) {
            return ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.getContentResolver(), uri),
                    (decoder, info, source) -> {
                        float scale = Math.min(1f, Math.min((float) DrawingDocument.WIDTH / info.getSize().getWidth(),
                                (float) DrawingDocument.HEIGHT / info.getSize().getHeight()));
                        decoder.setTargetSize(Math.max(1, Math.round(info.getSize().getWidth() * scale)),
                                Math.max(1, Math.round(info.getSize().getHeight() * scale)));
                        decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                    });
        }
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(input, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw new IOException("Unsupported image");
        float scale = Math.min(1f, Math.min((float) DrawingDocument.WIDTH / bounds.outWidth,
                (float) DrawingDocument.HEIGHT / bounds.outHeight));
        int width = Math.max(1, Math.round(bounds.outWidth * scale));
        int height = Math.max(1, Math.round(bounds.outHeight * scale));
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inSampleSize = 1;
        while (bounds.outWidth / (options.inSampleSize * 2) >= width
                && bounds.outHeight / (options.inSampleSize * 2) >= height) options.inSampleSize *= 2;
        Bitmap decoded;
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            decoded = BitmapFactory.decodeStream(input, null, options);
        }
        if (decoded == null) throw new IOException("Unsupported image");
        int orientation = ExifInterface.ORIENTATION_NORMAL;
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            if (input != null) orientation = new ExifInterface(input).getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
        } catch (IOException ignored) { /* Images without EXIF keep their decoded orientation. */ }
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL: matrix.setScale(-1, 1); break;
            case ExifInterface.ORIENTATION_ROTATE_180: matrix.setRotate(180); break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL: matrix.setScale(1, -1); break;
            case ExifInterface.ORIENTATION_TRANSPOSE: matrix.setRotate(90); matrix.postScale(-1, 1); break;
            case ExifInterface.ORIENTATION_ROTATE_90: matrix.setRotate(90); break;
            case ExifInterface.ORIENTATION_TRANSVERSE: matrix.setRotate(270); matrix.postScale(-1, 1); break;
            case ExifInterface.ORIENTATION_ROTATE_270: matrix.setRotate(270); break;
            default: break;
        }
        Bitmap oriented = Bitmap.createBitmap(decoded, 0, 0, decoded.getWidth(), decoded.getHeight(), matrix, true);
        if (oriented != decoded) decoded.recycle();
        scale = Math.min(1f, Math.min((float) DrawingDocument.WIDTH / oriented.getWidth(),
                (float) DrawingDocument.HEIGHT / oriented.getHeight()));
        Bitmap fitted = Bitmap.createScaledBitmap(oriented, Math.max(1, Math.round(oriented.getWidth() * scale)),
                Math.max(1, Math.round(oriented.getHeight() * scale)), true);
        if (fitted != oriented) oriented.recycle();
        return fitted;
    }
}
