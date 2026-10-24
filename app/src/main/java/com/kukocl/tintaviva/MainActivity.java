package com.kukocl.tintaviva;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.kukocl.tintaviva.core.DrawingDocument;
import com.kukocl.tintaviva.core.Layer;

import java.util.ArrayList;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final int INK = 0xff20382f;
    private static final int MUTED = 0xff667366;
    private static final int BACKGROUND = 0xfff5f3ed;
    private static final int ACTIVE = 0xffdcebcf;
    private static final int[] COLORS = {
            0xff29302b, 0xff8c69c6, 0xff4b84cd, 0xff37a58a,
            0xffe7b83f, 0xffe88c46, 0xffdc6270, 0xffd489b1
    };
    private final ArrayList<ColorSwatch> swatches = new ArrayList<>();
    private TintaVivaApp session;
    private DrawingDocument document;
    private DrawingView drawingView;
    private SharedPreferences preferences;
    private LinearLayout sketchCard, lineCard, brushButton, eraserButton, undoButton;
    private TextView hint, colorValue, sizeValue;
    private int selectedColor;
    private int brushWidth;
    private boolean erasing;
    private boolean wasSaveErrorShown;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if (Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(false);
        else getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        session = (TintaVivaApp) getApplication();
        document = session.document;
        preferences = getSharedPreferences("studio", MODE_PRIVATE);
        selectedColor = preferences.getInt("color", COLORS[0]);
        brushWidth = Math.max(2, Math.min(32, preferences.getInt("width", 12)));
        erasing = preferences.getBoolean("eraser", false);
        document.setActiveLayer(preferences.getBoolean("lineArt", false) ? Layer.LINE_ART : Layer.SKETCH);
        LinearLayout root = vertical();
        root.setBackgroundColor(BACKGROUND);
        root.setPadding(dp(16), 0, dp(16), 0);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            int left, top, right, bottom;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars()
                        | WindowInsets.Type.displayCutout());
                left = bars.left; top = bars.top; right = bars.right; bottom = bars.bottom;
            } else {
                left = insets.getSystemWindowInsetLeft(); top = insets.getSystemWindowInsetTop();
                right = insets.getSystemWindowInsetRight(); bottom = insets.getSystemWindowInsetBottom();
            }
            view.setPadding(dp(16) + left, top, dp(16) + right, bottom);
            return insets;
        });
        root.addView(makeHeader(), params(-1, 68));
        drawingView = new DrawingView(this, document);
        drawingView.setBrushColor(selectedColor);
        drawingView.setBrushWidth(brushWidth);
        drawingView.setErasing(erasing);
        drawingView.setListener(() -> {
            updateControls();
            session.store.save(document.snapshot());
        });
        boolean sidePanel = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE
                || getResources().getConfiguration().screenWidthDp >= 720;
        if (sidePanel) {
            LinearLayout body = horizontal();
            body.addView(drawingView, new LinearLayout.LayoutParams(0, -1, 1));
            LinearLayout panel = vertical();
            panel.setPadding(dp(12), dp(8), 0, dp(12));
            addLayers(panel);
            addTools(panel);
            ScrollView scroll = new ScrollView(this);
            scroll.setFillViewport(true);
            scroll.addView(panel);
            body.addView(scroll, params(290, -1));
            root.addView(body, new LinearLayout.LayoutParams(-1, 0, 1));
        } else {
            addLayers(root);
            root.addView(drawingView, new LinearLayout.LayoutParams(-1, 0, 1));
            addTools(root);
        }
        setContentView(root);
        root.requestApplyInsets();
        updateControls();
        if (session.showLoadError) {
            Toast.makeText(this, "No se pudo abrir el dibujo guardado. Conservaremos una copia del archivo original.",
                    Toast.LENGTH_LONG).show();
            session.showLoadError = false;
        }
    }

    private LinearLayout makeHeader() {
        LinearLayout header = horizontal();
        header.setGravity(Gravity.CENTER_VERTICAL);
        StudioIcon logo = new StudioIcon(this, "logo", INK);
        header.addView(logo, params(34, 34));
        LinearLayout title = vertical();
        title.setPadding(dp(10), 0, 0, 0);
        TextView name = text("Tinta Viva", 24, INK, true);
        name.setTypeface(Typeface.create("sans-serif-rounded", Typeface.BOLD));
        title.addView(name);
        TextView sub = text("PEQUEÑAS LÍNEAS, MUCHA VIDA", 9, MUTED, true);
        sub.setLetterSpacing(.08f);
        title.addView(sub);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        undoButton = toolButton("undo", "Deshacer");
        undoButton.setOnClickListener(view -> {
            drawingView.cancelPending();
            if (document.undo()) {
                drawingView.documentChanged();
                updateControls();
                session.store.save(document.snapshot());
            }
        });
        undoButton.setContentDescription("Deshacer la última acción, también si fue borrar un trazo");
        header.addView(undoButton, params(108, 48));
        return header;
    }

    private void addLayers(LinearLayout parent) {
        LinearLayout row = horizontal();
        sketchCard = layerCard("Sketch", "01 · TU BASE");
        lineCard = layerCard("Line art", "02 · COBRA VIDA");
        LinearLayout.LayoutParams first = new LinearLayout.LayoutParams(0, dp(58), 1);
        first.rightMargin = dp(8);
        row.addView(sketchCard, first);
        row.addView(lineCard, new LinearLayout.LayoutParams(0, dp(58), 1));
        parent.addView(row, params(-1, 58));
        sketchCard.setOnClickListener(view -> selectLayer(Layer.SKETCH));
        lineCard.setOnClickListener(view -> selectLayer(Layer.LINE_ART));
        hint = text("", 11, MUTED, false);
        hint.setGravity(Gravity.CENTER_VERTICAL);
        hint.setMaxLines(2);
        parent.addView(hint, params(-1, 34));
    }

    private LinearLayout layerCard(String name, String description) {
        LinearLayout card = vertical();
        card.setPadding(dp(14), dp(6), dp(10), dp(6));
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(text(name, 16, INK, true));
        TextView subtitle = text(description, 9, MUTED, true);
        subtitle.setLetterSpacing(.07f);
        card.addView(subtitle);
        card.setFocusable(true);
        card.setClickable(true);
        return card;
    }

    private void addTools(LinearLayout parent) {
        LinearLayout tools = vertical();
        tools.setPadding(0, dp(4), 0, dp(10));
        LinearLayout buttons = horizontal();
        brushButton = toolButton("brush", "Pincel");
        eraserButton = toolButton("eraser", "Goma de trazos");
        brushButton.setOnClickListener(view -> selectTool(false));
        eraserButton.setOnClickListener(view -> selectTool(true));
        LinearLayout.LayoutParams brushParams = new LinearLayout.LayoutParams(0, dp(48), 1);
        brushParams.rightMargin = dp(8);
        buttons.addView(brushButton, brushParams);
        buttons.addView(eraserButton, new LinearLayout.LayoutParams(0, dp(48), 1));
        tools.addView(buttons);
        LinearLayout colorHeading = horizontal();
        colorHeading.setGravity(Gravity.CENTER_VERTICAL);
        TextView label = text("COLOR", 10, MUTED, true);
        label.setLetterSpacing(.1f);
        colorHeading.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
        colorValue = text("", 11, MUTED, false);
        colorHeading.addView(colorValue);
        tools.addView(colorHeading, params(-1, 26));
        HorizontalScrollView paletteScroll = new HorizontalScrollView(this);
        paletteScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout palette = horizontal();
        for (int color : COLORS) {
            ColorSwatch swatch = new ColorSwatch(this, color, false);
            swatch.setContentDescription("Color " + hex(color));
            swatch.setOnClickListener(view -> selectColor(color));
            swatches.add(swatch);
            palette.addView(swatch, params(42, 48));
        }
        ColorSwatch custom = new ColorSwatch(this, Color.WHITE, true);
        custom.setContentDescription("Elegir cualquier color");
        custom.setOnClickListener(view -> ColorPickerDialog.show(this, selectedColor, this::selectColor));
        palette.addView(custom, params(48, 48));
        paletteScroll.addView(palette);
        tools.addView(paletteScroll, params(-1, 48));
        LinearLayout widthRow = horizontal();
        widthRow.setGravity(Gravity.CENTER_VERTICAL);
        widthRow.addView(text("Grosor", 12, MUTED, false), params(48, -2));
        SeekBar size = new SeekBar(this);
        size.setMax(30);
        size.setProgress(brushWidth - 2);
        size.setContentDescription("Grosor del pincel, de 2 a 32");
        size.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int value, boolean fromUser) {
                if (!fromUser) return;
                brushWidth = value + 2;
                drawingView.setBrushWidth(brushWidth);
                preferences.edit().putInt("width", brushWidth).apply();
                sizeValue.setText(String.valueOf(brushWidth));
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { }
        });
        widthRow.addView(size, new LinearLayout.LayoutParams(0, dp(48), 1));
        sizeValue = text(String.valueOf(brushWidth), 12, INK, true);
        sizeValue.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        widthRow.addView(sizeValue, params(28, 48));
        tools.addView(widthRow, params(-1, 48));
        parent.addView(tools, params(-1, -2));
    }

    private LinearLayout toolButton(String icon, String label) {
        LinearLayout button = horizontal();
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(8), 0, dp(8), 0);
        button.addView(new StudioIcon(this, icon, INK), params(20, 20));
        TextView title = text(label, 12, INK, true);
        title.setPadding(dp(7), 0, 0, 0);
        button.addView(title);
        button.setFocusable(true);
        button.setClickable(true);
        button.setContentDescription(label);
        button.setBackground(background(Color.WHITE, 0xffe1e3d9));
        return button;
    }

    private void selectLayer(Layer layer) {
        document.setActiveLayer(layer);
        drawingView.layerChanged();
        preferences.edit().putBoolean("lineArt", layer == Layer.LINE_ART).apply();
        updateControls();
    }

    private void selectTool(boolean eraser) {
        erasing = eraser;
        drawingView.setErasing(eraser);
        preferences.edit().putBoolean("eraser", eraser).apply();
        updateControls();
    }

    private void selectColor(int color) {
        selectedColor = color;
        drawingView.setBrushColor(color);
        preferences.edit().putInt("color", color).apply();
        selectTool(false);
    }

    private void updateControls() {
        boolean lineArt = document.getActiveLayer() == Layer.LINE_ART;
        styleSelection(sketchCard, !lineArt);
        styleSelection(lineCard, lineArt);
        styleSelection(brushButton, !erasing);
        styleSelection(eraserButton, erasing);
        sketchCard.setContentDescription("Capa sketch, " + (lineArt ? "opacidad 40 por ciento" : "seleccionada, opacidad 100 por ciento"));
        lineCard.setContentDescription("Capa line art, encima del sketch, líneas animadas" + (lineArt ? ", seleccionada" : ""));
        hint.setText(erasing ? "Goma · toca un trazo de esta capa para borrarlo entero"
                : lineArt ? "Sketch al 40 % · tus líneas cobran vida al soltar"
                : "Sketch al 100 % · dibuja la base de tu idea");
        colorValue.setText(hex(selectedColor));
        for (ColorSwatch swatch : swatches) swatch.setSelected(swatch.color == selectedColor);
        undoButton.setEnabled(document.canUndo());
        undoButton.setAlpha(document.canUndo() ? 1 : .35f);
    }

    private void styleSelection(LinearLayout view, boolean selected) {
        view.setSelected(selected);
        view.setBackground(background(selected ? ACTIVE : Color.WHITE, selected ? 0xffb7cba7 : 0xffe1e3d9));
    }

    private GradientDrawable background(int color, int border) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(14));
        drawable.setStroke(dp(1), border);
        return drawable;
    }

    private LinearLayout vertical() {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        return view;
    }
    private LinearLayout horizontal() {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.HORIZONTAL);
        return view;
    }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }
    private LinearLayout.LayoutParams params(int width, int height) {
        return new LinearLayout.LayoutParams(width < 0 ? width : dp(width), height < 0 ? height : dp(height));
    }
    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private String hex(int color) { return String.format(Locale.ROOT, "#%06X", color & 0xffffff); }

    @Override protected void onResume() {
        super.onResume();
        session.store.setErrorListener(() -> {
            if (!wasSaveErrorShown) {
                Toast.makeText(this, "No se pudo guardar el dibujo. Comprueba el espacio del dispositivo.", Toast.LENGTH_LONG).show();
                wasSaveErrorShown = true;
            }
        });
        drawingView.setAnimationAllowed(true);
    }

    @Override protected void onPause() {
        drawingView.setAnimationAllowed(false);
        session.store.save(document.snapshot());
        session.store.setErrorListener(null);
        super.onPause();
    }
}
