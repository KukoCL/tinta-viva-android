package com.kukocl.tintaviva;

import android.app.Application;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.kukocl.tintaviva.core.DrawingDocument;
import com.kukocl.tintaviva.core.CanvasViewport;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** One document and one disk writer per process, including across screen rotations. */
public final class TintaVivaApp extends Application {
    public final DrawingDocument document = new DrawingDocument();
    public final CanvasViewport viewport = new CanvasViewport();
    public DocumentStore store;
    public boolean showLoadError;
    public Bitmap sketchImage;
    private String sketchImageName;
    private long imageRequest;
    private boolean importing, exporting;
    private Runnable mediaListener;
    private String mediaError;
    private File pendingShare;
    private final Handler mainThread = new Handler(Looper.getMainLooper());
    private final ExecutorService mediaWorker = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "TintaViva-media");
        thread.setDaemon(true);
        return thread;
    });

    @Override public void onCreate() {
        super.onCreate();
        store = new DocumentStore(this);
        showLoadError = !store.loadInto(document);
        sketchImage = store.takeLoadedImage();
        sketchImageName = store.getLoadedImageName();
        if (!showLoadError) SketchImages.cleanUnused(this, sketchImageName);
    }

    public void saveDocument() { store.save(document.snapshot(), sketchImageName); }
    public boolean isImporting() { return importing; }
    public boolean isExporting() { return exporting; }
    public void setMediaListener(Runnable listener) { mediaListener = listener; }

    public String takeMediaError() {
        String error = mediaError;
        mediaError = null;
        return error;
    }

    public File takePendingShare() {
        File file = pendingShare;
        pendingShare = null;
        return file;
    }

    public void importSketchImage(Uri uri) {
        long request = ++imageRequest;
        importing = true;
        notifyMediaChanged();
        mediaWorker.execute(() -> {
            try {
                SketchImages.Imported image = SketchImages.importImage(this, uri);
                mainThread.post(() -> {
                    if (request != imageRequest) {
                        try { SketchImages.file(this, image.name).delete(); } catch (IOException ignored) { }
                        image.bitmap.recycle();
                        return;
                    }
                    sketchImage = image.bitmap;
                    sketchImageName = image.name;
                    importing = false;
                    saveDocument();
                    notifyMediaChanged();
                });
            } catch (IOException | RuntimeException | OutOfMemoryError error) {
                mainThread.post(() -> {
                    if (request != imageRequest) return;
                    importing = false;
                    mediaError = "No se pudo cargar esa imagen. Prueba con otro archivo de imagen.";
                    notifyMediaChanged();
                });
            }
        });
    }

    public void removeSketchImage() {
        imageRequest++;
        importing = false;
        sketchImage = null;
        sketchImageName = null;
        saveDocument();
        notifyMediaChanged();
    }

    public void exportGif() {
        if (exporting || importing) return;
        GifExporter.Snapshot snapshot = new GifExporter.Snapshot(document, sketchImage);
        exporting = true;
        notifyMediaChanged();
        mediaWorker.execute(() -> {
            File file = null;
            try {
                File directory = new File(getCacheDir(), "exports");
                if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Could not create export directory");
                File[] old = directory.listFiles();
                if (old != null) for (File item : old) {
                    if (item.lastModified() < System.currentTimeMillis() - 24 * 60 * 60 * 1000L) item.delete();
                }
                file = File.createTempFile("tinta-viva-", ".gif", directory);
                try (BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(file))) {
                    GifExporter.write(output, snapshot);
                }
                File complete = file;
                mainThread.post(() -> {
                    exporting = false;
                    pendingShare = complete;
                    notifyMediaChanged();
                });
            } catch (IOException | RuntimeException | OutOfMemoryError error) {
                if (file != null) file.delete();
                mainThread.post(() -> {
                    exporting = false;
                    mediaError = "No se pudo crear el GIF. Comprueba el espacio del dispositivo e inténtalo otra vez.";
                    notifyMediaChanged();
                });
            }
        });
    }

    private void notifyMediaChanged() { if (mediaListener != null) mediaListener.run(); }
}
