package com.kukocl.tintaviva;

import android.app.Application;

import com.kukocl.tintaviva.core.DrawingDocument;
import com.kukocl.tintaviva.core.CanvasViewport;

/** One document and one disk writer per process, including across screen rotations. */
public final class TintaVivaApp extends Application {
    public final DrawingDocument document = new DrawingDocument();
    public final CanvasViewport viewport = new CanvasViewport();
    public DocumentStore store;
    public boolean showLoadError;

    @Override public void onCreate() {
        super.onCreate();
        store = new DocumentStore(this);
        showLoadError = !store.loadInto(document);
    }
}
