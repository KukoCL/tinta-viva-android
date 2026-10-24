package com.kukocl.tintaviva;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.text.InputFilter;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Locale;

/** HSV picker plus a hex field for any opaque brush color. */
final class ColorPickerDialog {
    interface Listener { void onColorSelected(int color); }

    static void show(Context context, int initialColor, Listener listener) {
        int padding = dp(context, 20);
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(padding, dp(context, 8), padding, padding);
        float[] hsv = new float[3];
        Color.colorToHSV(initialColor, hsv);
        EditText hex = new EditText(context);
        hex.setSingleLine(true);
        hex.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        hex.setImeOptions(EditorInfo.IME_ACTION_DONE);
        hex.setFilters(new InputFilter[]{new InputFilter.LengthFilter(7)});
        hex.setContentDescription("Color hexadecimal, seis cifras, por ejemplo #38A87A");
        hex.setHint("#38A87A");
        hex.setText(String.format(Locale.ROOT, "#%06X", initialColor & 0xffffff));
        TextView preview = new TextView(context);
        preview.setText("Vista previa");
        preview.setGravity(android.view.Gravity.CENTER);
        preview.setTextSize(14);
        Runnable update = () -> {
            int color = Color.HSVToColor(hsv);
            hex.setText(String.format(Locale.ROOT, "#%06X", color & 0xffffff));
            GradientDrawable background = new GradientDrawable();
            background.setColor(color);
            background.setCornerRadius(dp(context, 10));
            preview.setBackground(background);
            preview.setTextColor(hsv[2] > .7f && hsv[1] < .55f ? Color.BLACK : Color.WHITE);
        };
        Spectrum spectrum = new Spectrum(context, hsv, update);
        layout.addView(spectrum, new LinearLayout.LayoutParams(-1, dp(context, 180)));
        SeekBar hue = new SeekBar(context);
        hue.setId(View.generateViewId());
        hue.setMax(359);
        hue.setProgress(Math.round(hsv[0]));
        hue.setContentDescription("Tono del color");
        hue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (fromUser) { hsv[0] = progress; spectrum.invalidate(); update.run(); }
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { }
        });
        HorizontalScrollView paletteScroll = new HorizontalScrollView(context);
        paletteScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout palette = new LinearLayout(context);
        for (int color : new int[]{0xff29302b, 0xff8c69c6, 0xff4b84cd, 0xff37a58a,
                0xffe7b83f, 0xffe88c46, 0xffdc6270, 0xffd489b1}) {
            ColorSwatch swatch = new ColorSwatch(context, color, false);
            swatch.setContentDescription(String.format(Locale.ROOT, "Color #%06X", color & 0xffffff));
            swatch.setOnClickListener(view -> {
                Color.colorToHSV(color, hsv);
                hue.setProgress(Math.round(hsv[0]));
                spectrum.invalidate();
                update.run();
            });
            palette.addView(swatch, new LinearLayout.LayoutParams(dp(context, 42), dp(context, 48)));
        }
        paletteScroll.addView(palette);
        layout.addView(paletteScroll, new LinearLayout.LayoutParams(-1, dp(context, 48)));
        TextView hueLabel = new TextView(context);
        hueLabel.setText("Tono");
        hueLabel.setLabelFor(hue.getId());
        layout.addView(hueLabel);
        layout.addView(hue, new LinearLayout.LayoutParams(-1, dp(context, 48)));
        layout.addView(hex, new LinearLayout.LayoutParams(-1, dp(context, 52)));
        layout.addView(preview, new LinearLayout.LayoutParams(-1, dp(context, 44)));
        update.run();
        ScrollView scroll = new ScrollView(context);
        scroll.addView(layout);
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Tu color")
                .setView(scroll)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Usar color", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(button -> {
                    String input = hex.getText().toString().trim();
                    if (!input.matches("#?[0-9a-fA-F]{6}")) {
                        hex.setError("Escribe seis cifras, por ejemplo #38A87A");
                        return;
                    }
                    int color = Color.parseColor(input.startsWith("#") ? input : "#" + input);
                    listener.onColorSelected(color);
                    dialog.dismiss();
                }));
        dialog.show();
    }

    private static int dp(Context context, float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static final class Spectrum extends View {
        private final float[] hsv;
        private final Runnable changed;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Spectrum(Context context, float[] hsv, Runnable changed) {
            super(context);
            this.hsv = hsv;
            this.changed = changed;
            setContentDescription("Saturación y luminosidad. También puedes escribir el color hexadecimal.");
        }
        @Override protected void onDraw(Canvas canvas) {
            float width = getWidth(), height = getHeight();
            if (width <= 0 || height <= 0) return;
            paint.setStyle(Paint.Style.FILL);
            paint.setShader(new LinearGradient(0, 0, width, 0, Color.WHITE,
                    Color.HSVToColor(new float[]{hsv[0], 1, 1}), Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, width, height, paint);
            paint.setShader(new LinearGradient(0, 0, 0, height, Color.TRANSPARENT, Color.BLACK,
                    Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, width, height, paint);
            paint.setShader(null);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(getContext(), 2));
            paint.setColor(Color.WHITE);
            canvas.drawCircle(hsv[1] * width, (1 - hsv[2]) * height, dp(getContext(), 7), paint);
        }
        @Override public boolean onTouchEvent(MotionEvent event) {
            if (event.getActionMasked() == MotionEvent.ACTION_CANCEL) return true;
            if (getWidth() <= 0 || getHeight() <= 0) return false;
            hsv[1] = Math.max(0, Math.min(1, event.getX() / getWidth()));
            hsv[2] = 1 - Math.max(0, Math.min(1, event.getY() / getHeight()));
            changed.run();
            invalidate();
            if (event.getActionMasked() == MotionEvent.ACTION_UP) performClick();
            return true;
        }
        @Override public boolean performClick() { super.performClick(); return true; }
    }
}
