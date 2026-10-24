package com.kukocl.tintaviva;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;

import com.kukocl.tintaviva.core.DrawingDocument;
import com.kukocl.tintaviva.core.CanvasViewport;
import com.kukocl.tintaviva.core.Layer;
import com.kukocl.tintaviva.core.LineAnimation;
import com.kukocl.tintaviva.core.Stroke;
import com.kukocl.tintaviva.core.StrokeBuilder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class DrawingView extends View {
    public interface Listener { void onDrawingChanged(); }
    private final DrawingDocument document;
    private final CanvasViewport viewport;
    private final ScaleGestureDetector scaleDetector;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF paper = new RectF();
    private final Map<Long, Path[]> pathCache = new HashMap<>();
    private final Runnable animate = this::invalidate;
    private Listener listener;
    private StrokeBuilder pending;
    private int activePointer = -1;
    private boolean erasing;
    private boolean animationAllowed;
    private int brushColor = Color.rgb(41, 48, 43);
    private float brushWidth = 12;
    private int displayedFrame;
    private int pendingColor;
    private float pendingWidth;
    private boolean gestureActive, navigating, gestureErasing, startedOnPaper, stylusGesture;
    private float previousFocusX, previousFocusY, gestureScaleFactor = 1;

    public DrawingView(Context context, DrawingDocument document) {
        this(context, document, new CanvasViewport());
    }

    public DrawingView(Context context, DrawingDocument document, CanvasViewport viewport) {
        super(context);
        this.document = document;
        this.viewport = viewport;
        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override public boolean onScaleBegin(ScaleGestureDetector detector) { return true; }
            @Override public boolean onScale(ScaleGestureDetector detector) {
                gestureScaleFactor = detector.getScaleFactor();
                return true;
            }
        });
        scaleDetector.setQuickScaleEnabled(false);
        scaleDetector.setStylusScaleEnabled(false);
        setFocusable(true);
        setContentDescription("Lienzo de dibujo. Dibuja con el dedo o con un lápiz. "
                + "Pellizca con dos dedos para ampliar y desplázalos para mover el lienzo. "
                + "En modo goma, toca y suelta un trazo para borrarlo entero de la capa seleccionada.");
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        setLayerType(View.LAYER_TYPE_HARDWARE, null);
    }

    public void setListener(Listener listener) { this.listener = listener; }
    public void setBrushColor(int color) { brushColor = color; }
    public void setBrushWidth(float width) { brushWidth = width; }
    public void setErasing(boolean erasing) { cancelPending(); this.erasing = erasing; }
    public void layerChanged() { cancelPending(); invalidate(); }
    public void setAnimationAllowed(boolean allowed) {
        animationAllowed = allowed;
        removeCallbacks(animate);
        if (!allowed) cancelPending();
        else invalidate();
    }

    public void documentChanged() {
        Set<Long> current = new HashSet<>();
        for (Stroke stroke : document.getStrokes()) current.add(stroke.id);
        pathCache.keySet().retainAll(current);
        invalidate();
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        viewport.resize(w, h, dp(2));
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paper.set(viewport.getLeft(), viewport.getTop(), viewport.getRight(), viewport.getBottom());
        displayedFrame = LineAnimation.frameAt(SystemClock.uptimeMillis());
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setAlpha(255);
        canvas.drawRoundRect(paper, dp(3), dp(3), paint);
        int saved = canvas.save();
        canvas.clipRect(paper);
        canvas.translate(paper.left, paper.top);
        canvas.scale(viewport.getScale(), viewport.getScale());
        if (document.sketchOpacity() > 0 && document.sketchOpacity() < 1) {
            // Apply opacity to the composite layer, so overlapping sketch strokes stay at 40%.
            int sketchSave = canvas.saveLayerAlpha(0, 0, DrawingDocument.WIDTH,
                    DrawingDocument.HEIGHT, Math.round(document.sketchOpacity() * 255));
            drawLayer(canvas, Layer.SKETCH, 1);
            canvas.restoreToCount(sketchSave);
        } else if (document.sketchOpacity() == 1) drawLayer(canvas, Layer.SKETCH, 1);
        drawLayer(canvas, Layer.LINE_ART, 1);
        if (document.getStrokes().isEmpty() && pending == null) {
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setColor(Color.rgb(161, 168, 158));
            textPaint.setTextSize(38);
            canvas.drawText("Aquí empieza tu próxima idea", DrawingDocument.WIDTH / 2f,
                    DrawingDocument.HEIGHT / 2f - 12, textPaint);
            textPaint.setTextSize(27);
            textPaint.setColor(Color.rgb(177, 182, 173));
            canvas.drawText("Haz un sketch. Dale vida con line art.", DrawingDocument.WIDTH / 2f,
                    DrawingDocument.HEIGHT / 2f + 43, textPaint);
        }
        canvas.restoreToCount(saved);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1));
        paint.setColor(Color.rgb(224, 226, 217));
        paint.setAlpha(255);
        canvas.drawRoundRect(paper, dp(3), dp(3), paint);
        removeCallbacks(animate);
        if (animationAllowed && isShown() && document.hasAnimatedStrokes()) {
            long delay = LineAnimation.FRAME_MS - SystemClock.uptimeMillis() % LineAnimation.FRAME_MS;
            postDelayed(animate, delay);
        }
    }

    private void drawLayer(Canvas canvas, Layer layer, float opacity) {
        for (Stroke stroke : document.getStrokes()) {
            if (stroke.layer != layer) continue;
            Path[] frames = pathCache.get(stroke.id);
            if (frames == null) {
                frames = new Path[layer == Layer.LINE_ART ? LineAnimation.FRAME_COUNT : 1];
                pathCache.put(stroke.id, frames);
            }
            int index = layer == Layer.LINE_ART ? displayedFrame : 0;
            if (frames[index] == null) frames[index] = makePath(LineAnimation.pointsAt(stroke, index));
            configurePaint(stroke.color, stroke.width, opacity);
            if (stroke.pointCount() == 1) {
                float[] point = LineAnimation.pointsAt(stroke, index);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(point[0], point[1], stroke.width / 2, paint);
            } else canvas.drawPath(frames[index], paint);
        }
        if (pending != null && layer == document.getActiveLayer()) {
            float[] points = pending.preview();
            configurePaint(pendingColor, pendingWidth, opacity);
            if (points.length == 2) {
                paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(points[0], points[1], pendingWidth / 2, paint);
            } else if (points.length > 2) canvas.drawPath(makePath(points), paint);
        }
    }

    private void configurePaint(int color, float width, float opacity) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(color);
        paint.setAlpha(Math.round(opacity * 255));
        paint.setStrokeWidth(width);
    }

    private static Path makePath(float[] points) {
        Path path = new Path();
        if (points.length == 0) return path;
        path.moveTo(points[0], points[1]);
        // Dense equal-distance samples with rounded joins also match the eraser geometry.
        for (int i = 2; i < points.length; i += 2) path.lineTo(points[i], points[i + 1]);
        return path;
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            cancelPending();
            gestureActive = true;
            disallowParentInterception(true);
            activePointer = event.getPointerId(0);
            startedOnPaper = viewport.contains(event.getX(), event.getY());
            boolean physicalEraser = event.getToolType(0) == MotionEvent.TOOL_TYPE_ERASER;
            stylusGesture = physicalEraser || event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS;
            gestureErasing = erasing || physicalEraser;
            if (startedOnPaper && !gestureErasing) {
                pending = new StrokeBuilder();
                pendingColor = brushColor;
                pendingWidth = brushWidth;
                pending.add(toX(event.getX()), toY(event.getY()));
            }
            if (!stylusGesture) scaleDetector.onTouchEvent(event);
            invalidate();
            return true;
        }
        if (!gestureActive) return false;
        gestureScaleFactor = 1;
        if (action == MotionEvent.ACTION_POINTER_DOWN && !stylusGesture) {
            // A pinch cancels the unfinished draft and suppresses editing until every finger is up.
            pending = null;
            navigating = true;
            previousFocusX = focus(event, true, -1);
            previousFocusY = focus(event, false, -1);
            invalidate();
        }
        if (!stylusGesture) scaleDetector.onTouchEvent(event);
        if (navigating) {
            if (action == MotionEvent.ACTION_MOVE && event.getPointerCount() >= 2) {
                float focusX = focus(event, true, -1), focusY = focus(event, false, -1);
                viewport.transform(gestureScaleFactor, previousFocusX, previousFocusY, focusX, focusY);
                previousFocusX = focusX;
                previousFocusY = focusY;
                invalidate();
            } else if (action == MotionEvent.ACTION_POINTER_UP) {
                // Rebase the focus when a third finger joins or leaves; never jump to its position.
                previousFocusX = focus(event, true, event.getActionIndex());
                previousFocusY = focus(event, false, event.getActionIndex());
            }
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) cancelPending();
            return true;
        }
        if (action == MotionEvent.ACTION_CANCEL) {
            cancelPending();
            return true;
        }
        // Additional fingers are ignored while an actual pen is drawing (palm rejection).
        if (action == MotionEvent.ACTION_POINTER_DOWN) return true;
        int index = event.findPointerIndex(activePointer);
        if (index < 0) { cancelPending(); return true; }
        if (pending != null && (action == MotionEvent.ACTION_MOVE || action == MotionEvent.ACTION_UP
                || action == MotionEvent.ACTION_POINTER_UP && event.getActionIndex() == index)) {
            for (int history = 0; history < event.getHistorySize(); history++) {
                addInput(event.getHistoricalX(index, history), event.getHistoricalY(index, history));
            }
            addInput(event.getX(index), event.getY(index));
            invalidate();
        }
        if (action == MotionEvent.ACTION_UP
                || action == MotionEvent.ACTION_POINTER_UP && event.getActionIndex() == index) {
            if (pending != null && !pending.isEmpty()) {
                document.addStroke(pendingColor, pendingWidth, pending.finish());
                pending = null;
                changed();
            } else if (gestureErasing && startedOnPaper && viewport.contains(event.getX(index), event.getY(index))) {
                // Wait for release: placing a second finger to zoom must never erase a stroke.
                if (document.eraseAt(toX(event.getX(index)), toY(event.getY(index)),
                        dp(9) / viewport.getScale(), displayedFrame)) changed();
            }
            cancelPending();
            performClick();
        }
        return true;
    }

    private void addInput(float x, float y) {
        // Clamp a gesture crossing the paper edge so it can return without corrupting the document.
        pending.add(Math.max(0, Math.min(DrawingDocument.WIDTH, toX(x))),
                Math.max(0, Math.min(DrawingDocument.HEIGHT, toY(y))));
    }

    private static float focus(MotionEvent event, boolean horizontal, int excludedIndex) {
        float sum = 0;
        int count = 0;
        for (int i = 0; i < event.getPointerCount(); i++) {
            if (i == excludedIndex) continue;
            sum += horizontal ? event.getX(i) : event.getY(i);
            count++;
        }
        return count == 0 ? 0 : sum / count;
    }

    private float toX(float x) { return viewport.toDocumentX(x); }
    private float toY(float y) { return viewport.toDocumentY(y); }
    private float dp(float value) { return value * getResources().getDisplayMetrics().density; }

    private void changed() { documentChanged(); if (listener != null) listener.onDrawingChanged(); }
    public void cancelPending() {
        pending = null;
        activePointer = -1;
        gestureActive = false;
        navigating = false;
        gestureErasing = false;
        disallowParentInterception(false);
        invalidate();
    }

    private void disallowParentInterception(boolean disallow) {
        if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(disallow);
    }

    @Override public boolean performClick() { super.performClick(); return true; }
    @Override protected void onDetachedFromWindow() {
        removeCallbacks(animate);
        super.onDetachedFromWindow();
    }

    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(View.class.getName());
    }
}
