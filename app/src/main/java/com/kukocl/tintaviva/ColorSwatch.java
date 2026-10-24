package com.kukocl.tintaviva;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.SweepGradient;
import android.view.View;

final class ColorSwatch extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    final int color;
    private final boolean custom;

    ColorSwatch(Context context, int color, boolean custom) {
        super(context);
        this.color = color;
        this.custom = custom;
        setFocusable(true);
        setClickable(true);
    }

    @Override public void setSelected(boolean selected) { super.setSelected(selected); invalidate(); }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float x = getWidth() / 2f, y = getHeight() / 2f;
        float radius = Math.min(getWidth(), getHeight()) * .29f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        if (custom) paint.setShader(new SweepGradient(x, y,
                new int[]{0xffff6464, 0xffffd75d, 0xff8ed788, 0xff53bfc8, 0xff9f86ec, 0xffff6464}, null));
        canvas.drawCircle(x, y, radius, paint);
        paint.setShader(null);
        if (custom) {
            paint.setColor(Color.WHITE);
            paint.setStrokeWidth(radius * .14f);
            paint.setStrokeCap(Paint.Cap.ROUND);
            canvas.drawLine(x - radius * .38f, y, x + radius * .38f, y, paint);
            canvas.drawLine(x, y - radius * .38f, x, y + radius * .38f, paint);
        }
        if (isSelected()) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(0xff20382f);
            paint.setStrokeWidth(getResources().getDisplayMetrics().density * 1.6f);
            canvas.drawCircle(x, y, radius + getResources().getDisplayMetrics().density * 4, paint);
        }
    }
}
