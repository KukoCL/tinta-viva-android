package com.kukocl.tintaviva.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Main-thread model, independent of Android so geometry and editing can be tested. */
public final class DrawingDocument {
    public static final int WIDTH = 1080;
    public static final int HEIGHT = 1350;
    private static final int MAX_UNDO = 128;
    private final ArrayList<Stroke> strokes = new ArrayList<>();
    private final ArrayDeque<Edit> history = new ArrayDeque<>();
    private long nextId = 1;
    private Layer activeLayer = Layer.SKETCH;
    private boolean lineArtOnly;

    public Layer getActiveLayer() { return activeLayer; }
    public void setActiveLayer(Layer layer) {
        if (layer == null) throw new IllegalArgumentException("Missing layer");
        activeLayer = layer;
        lineArtOnly = false;
    }
    public void showOnlyLineArt() { activeLayer = Layer.LINE_ART; lineArtOnly = true; }
    public boolean isLineArtOnly() { return lineArtOnly; }
    public float sketchOpacity() { return lineArtOnly ? 0 : activeLayer == Layer.LINE_ART ? .4f : 1f; }
    public List<Stroke> getStrokes() { return Collections.unmodifiableList(strokes); }
    public List<Stroke> snapshot() { return new ArrayList<>(strokes); }
    public boolean canUndo() { return !history.isEmpty(); }
    public boolean hasAnimatedStrokes() {
        for (Stroke stroke : strokes) if (stroke.layer == Layer.LINE_ART) return true;
        return false;
    }

    public Stroke addStroke(int color, float width, float[] points) {
        Stroke stroke = new Stroke(nextId++, activeLayer, color, width, points);
        strokes.add(stroke);
        remember(new Edit(true, stroke, strokes.size() - 1));
        return stroke;
    }

    /** One tap removes only the topmost complete stroke on the selected layer. */
    public boolean eraseAt(float x, float y, float tolerance, int displayedFrame) {
        for (int i = strokes.size() - 1; i >= 0; i--) {
            Stroke stroke = strokes.get(i);
            if (stroke.layer == activeLayer && HitTest.touches(stroke, x, y, tolerance, displayedFrame)) {
                strokes.remove(i);
                remember(new Edit(false, stroke, i));
                return true;
            }
        }
        return false;
    }

    public boolean undo() {
        Edit edit = history.pollLast();
        if (edit == null) return false;
        if (edit.wasAdded) strokes.remove(edit.stroke);
        else strokes.add(edit.index, edit.stroke);
        return true;
    }

    /** Start an empty document without allowing undo to bring back the previous drawing. */
    public void clear() {
        strokes.clear();
        history.clear();
        nextId = 1;
    }

    /** History belongs to this editing session; drawing content survives app restarts. */
    public void restore(List<Stroke> savedStrokes) {
        Set<Long> ids = new HashSet<>();
        long highestId = 0;
        for (Stroke stroke : savedStrokes) {
            if (!ids.add(stroke.id) || stroke.id == Long.MAX_VALUE) {
                throw new IllegalArgumentException("Invalid stroke IDs");
            }
            highestId = Math.max(highestId, stroke.id);
        }
        strokes.clear();
        strokes.addAll(savedStrokes);
        nextId = highestId + 1;
        history.clear();
    }

    private void remember(Edit edit) {
        history.addLast(edit);
        if (history.size() > MAX_UNDO) history.removeFirst();
    }

    private static final class Edit {
        final boolean wasAdded;
        final Stroke stroke;
        final int index;
        Edit(boolean wasAdded, Stroke stroke, int index) {
            this.wasAdded = wasAdded;
            this.stroke = stroke;
            this.index = index;
        }
    }
}
