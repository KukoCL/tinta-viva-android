package com.kukocl.tintaviva;

import android.content.Context;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.core.view.ViewCompat;

import com.kukocl.tintaviva.core.CanvasViewport;

/** A long slider with an inline phone sample or a temporary floating tablet sample. */
final class BrushSizeControl extends LinearLayout {
    interface Listener { void onWidthSelected(int width); }

    private final SeekBar slider;
    private final TextView value;
    private final BrushStrokePreview preview;
    private final boolean floatingPreview;
    private final Runnable hidePreview = this::dismissPreview;
    private PopupWindow previewPopup;
    private boolean tracking;

    BrushSizeControl(Context context, CanvasViewport viewport, int color, int width,
                     boolean floatingPreview, Listener listener) {
        super(context);
        this.floatingPreview = floatingPreview;
        setId(R.id.brush_size_control);
        setOrientation(VERTICAL);
        ViewCompat.setAccessibilityPaneTitle(this, "Grosor del pincel");
        preview = new BrushStrokePreview(context, viewport);
        LinearLayout row = new LinearLayout(context);
        row.setGravity(Gravity.CENTER_VERTICAL);
        value = new TextView(context);
        value.setTextColor(0xff20382f);
        value.setTextSize(12);
        value.setGravity(Gravity.CENTER);
        value.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        if (!floatingPreview) {
            FrameLayout sample = new FrameLayout(context);
            sample.addView(preview, new FrameLayout.LayoutParams(-1, -1));
            FrameLayout.LayoutParams label = new FrameLayout.LayoutParams(dp(32), dp(32), Gravity.TOP | Gravity.END);
            label.topMargin = dp(4);
            label.rightMargin = dp(4);
            sample.addView(value, label);
            addView(sample, new LayoutParams(-1, dp(120)));
        }
        slider = new SeekBar(context);
        slider.setId(R.id.brush_size_slider);
        slider.setMax(30);
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (!fromUser) return;
                listener.onWidthSelected(progress + 2);
                showPreview();
                if (!tracking) scheduleHidePreview();
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {
                tracking = true;
                showPreview();
            }
            @Override public void onStopTrackingTouch(SeekBar bar) {
                tracking = false;
                scheduleHidePreview();
            }
        });
        row.addView(slider, new LayoutParams(0, dp(48), 1));
        if (floatingPreview) row.addView(value, new LayoutParams(dp(32), dp(48)));
        addView(row, new LayoutParams(-1, -2));
        setBrush(color, width);
    }

    void setBrush(int color, int width) {
        value.setText(String.valueOf(width));
        slider.setProgress(width - 2);
        slider.setContentDescription("Grosor del pincel: " + width + ", de 2 a 32");
        preview.setBrush(color, width);
    }

    @Override public boolean dispatchTouchEvent(MotionEvent event) {
        boolean handled = super.dispatchTouchEvent(event);
        if (floatingPreview) {
            // A ScrollView can delay SeekBar's tracking callback until the finger moves.
            // Observe the gesture without intercepting it so a stationary press also previews.
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN && handled) {
                tracking = true;
                showPreview();
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                tracking = false;
                scheduleHidePreview();
            }
        }
        return handled;
    }

    private void showPreview() {
        if (!floatingPreview) return;
        removeCallbacks(hidePreview);
        if (previewPopup != null && previewPopup.isShowing()) return;
        if (!isShown() || getWindowToken() == null) return;
        if (previewPopup == null) {
            previewPopup = new PopupWindow(preview, dp(200), dp(144), false);
            previewPopup.setTouchable(false);
            previewPopup.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
            previewPopup.setBackgroundDrawable(preview.getBackground().getConstantState().newDrawable());
            previewPopup.setElevation(dp(8));
        }
        previewPopup.showAsDropDown(slider, (slider.getWidth() - previewPopup.getWidth()) / 2,
                -slider.getHeight() - previewPopup.getHeight() - dp(8), Gravity.START);
    }

    private void scheduleHidePreview() {
        if (!floatingPreview) return;
        removeCallbacks(hidePreview);
        postDelayed(hidePreview, 1500);
    }

    void dismissPreview() {
        removeCallbacks(hidePreview);
        tracking = false;
        if (previewPopup != null) previewPopup.dismiss();
    }

    @Override protected void onDetachedFromWindow() {
        dismissPreview();
        super.onDetachedFromWindow();
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
