package com.kukocl.tintaviva.core;

/** Screen/document transform. Navigation never changes saved stroke coordinates. */
public final class CanvasViewport {
    public static final float MIN_ZOOM = 1f;
    public static final float MAX_ZOOM = 8f;
    private float viewWidth, viewHeight, margin;
    private float fitScale = 1, zoom = 1, left, top;
    private boolean ready;

    public void resize(float width, float height, float padding) {
        if (width <= 0 || height <= 0) return;
        float centerX = ready ? toDocumentX(viewWidth / 2) : DrawingDocument.WIDTH / 2f;
        float centerY = ready ? toDocumentY(viewHeight / 2) : DrawingDocument.HEIGHT / 2f;
        viewWidth = width;
        viewHeight = height;
        margin = Math.max(0, Math.min(padding, Math.min(width, height) / 4));
        fitScale = Math.max(.001f, Math.min((width - 2 * margin) / DrawingDocument.WIDTH,
                (height - 2 * margin) / DrawingDocument.HEIGHT));
        left = width / 2 - centerX * getScale();
        top = height / 2 - centerY * getScale();
        ready = true;
        constrain();
    }

    /** Keep the document point beneath the previous focus beneath the new focus. */
    public void transform(float factor, float previousFocusX, float previousFocusY,
                          float focusX, float focusY) {
        if (!ready || !Float.isFinite(factor) || factor <= 0
                || !Float.isFinite(previousFocusX) || !Float.isFinite(previousFocusY)
                || !Float.isFinite(focusX) || !Float.isFinite(focusY)) return;
        float documentX = toDocumentX(previousFocusX);
        float documentY = toDocumentY(previousFocusY);
        zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom * factor));
        left = focusX - documentX * getScale();
        top = focusY - documentY * getScale();
        constrain();
    }

    public float getScale() { return fitScale * zoom; }

    /** Show the complete page again when starting a new drawing. */
    public void reset() {
        zoom = MIN_ZOOM;
        if (!ready) return;
        left = (viewWidth - DrawingDocument.WIDTH * getScale()) / 2;
        top = (viewHeight - DrawingDocument.HEIGHT * getScale()) / 2;
        constrain();
    }

    public float getZoom() { return zoom; }
    public float getLeft() { return left; }
    public float getTop() { return top; }
    public float getRight() { return left + DrawingDocument.WIDTH * getScale(); }
    public float getBottom() { return top + DrawingDocument.HEIGHT * getScale(); }
    public float toDocumentX(float screenX) { return (screenX - left) / getScale(); }
    public float toDocumentY(float screenY) { return (screenY - top) / getScale(); }
    public float toScreenX(float documentX) { return left + documentX * getScale(); }
    public float toScreenY(float documentY) { return top + documentY * getScale(); }

    public boolean contains(float x, float y) {
        return ready && x >= getLeft() && x <= getRight() && y >= getTop() && y <= getBottom();
    }

    private void constrain() {
        left = constrainAxis(left, DrawingDocument.WIDTH * getScale(), viewWidth);
        top = constrainAxis(top, DrawingDocument.HEIGHT * getScale(), viewHeight);
    }

    private float constrainAxis(float origin, float documentSize, float viewSize) {
        if (documentSize <= viewSize - 2 * margin) return (viewSize - documentSize) / 2;
        return Math.max(viewSize - margin - documentSize, Math.min(margin, origin));
    }
}
