package com.kukocl.tintaviva;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.GradientDrawable;
import android.view.View;

import com.kukocl.tintaviva.core.CanvasViewport;

/** A rounded freehand sample, shown at the paper's fitted scale regardless of zoom. */
final class BrushStrokePreview extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final CanvasViewport viewport;
    private int color, width;

    BrushStrokePreview(Context context, CanvasViewport viewport) {
        super(context);
        this.viewport = viewport;
        setId(R.id.brush_size_preview);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        GradientDrawable background = new GradientDrawable();
        background.setColor(0xffffffff);
        background.setCornerRadius(dp(12));
        background.setStroke(Math.round(dp(1)), 0xffe1e3d9);
        setBackground(background);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        // A tall arch that turns into two smaller loops, like a quick handwritten flourish.
        path.moveTo(.06f, .70f);
        path.cubicTo(.23f, .40f, .55f, -.02f, .61f, .06f);
        path.cubicTo(.72f, .15f, .28f, .94f, .39f, .93f);
        path.cubicTo(.47f, .91f, .77f, .51f, .79f, .54f);
        path.cubicTo(.85f, .58f, .70f, .87f, .76f, .84f);
        path.cubicTo(.83f, .80f, .93f, .65f, .94f, .67f);
        path.cubicTo(.96f, .70f, .89f, .89f, .98f, .83f);
    }

    void setBrush(int color, int width) {
        this.color = color;
        this.width = width;
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float side = Math.min(getWidth(), getHeight()) - dp(24);
        if (side <= 0) return;
        int saved = canvas.save();
        canvas.translate((getWidth() - side) / 2, (getHeight() - side) / 2);
        canvas.scale(side, side);
        paint.setColor(color);
        paint.setStrokeWidth(width * viewport.getScale() / viewport.getZoom() / side);
        canvas.drawPath(path, paint);
        canvas.restoreToCount(saved);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
