package com.kukocl.tintaviva;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.core.view.ViewCompat;

import com.kukocl.tintaviva.core.CanvasViewport;

/** A long slider with a sample at the paper's fitted scale, independent of zoom. */
final class BrushSizeControl extends LinearLayout {
    interface Listener { void onWidthSelected(int width); }

    private final SeekBar slider;
    private final TextView value;
    private final View preview;
    private int color, width;

    BrushSizeControl(Context context, CanvasViewport viewport, int color, int width, Listener listener) {
        super(context);
        setId(R.id.brush_size_control);
        setOrientation(VERTICAL);
        ViewCompat.setAccessibilityPaneTitle(this, "Grosor del pincel");
        LinearLayout sample = new LinearLayout(context);
        sample.setGravity(Gravity.CENTER_VERTICAL);
        preview = new View(context) {
            private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

            @Override protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                paint.setColor(BrushSizeControl.this.color);
                paint.setStrokeCap(Paint.Cap.ROUND);
                paint.setStrokeWidth(BrushSizeControl.this.width * viewport.getScale() / viewport.getZoom());
                float inset = Math.max(dp(12), paint.getStrokeWidth() / 2 + dp(2));
                canvas.drawLine(inset, getHeight() / 2f, Math.max(inset, getWidth() - inset),
                        getHeight() / 2f, paint);
            }
        };
        preview.setId(R.id.brush_size_preview);
        preview.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        sample.addView(preview, new LayoutParams(0, dp(32), 1));
        value = new TextView(context);
        value.setTextColor(0xff20382f);
        value.setTextSize(12);
        value.setGravity(Gravity.CENTER);
        value.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        sample.addView(value, new LayoutParams(dp(32), dp(32)));
        addView(sample, new LayoutParams(-1, -2));
        slider = new SeekBar(context);
        slider.setId(R.id.brush_size_slider);
        slider.setMax(30);
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (fromUser) listener.onWidthSelected(progress + 2);
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { }
        });
        addView(slider, new LayoutParams(-1, dp(48)));
        setBrush(color, width);
    }

    void setBrush(int color, int width) {
        this.color = color;
        this.width = width;
        value.setText(String.valueOf(width));
        slider.setProgress(width - 2);
        slider.setContentDescription("Grosor del pincel: " + width + ", de 2 a 32");
        preview.invalidate();
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
