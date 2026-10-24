package com.kukocl.tintaviva;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.kukocl.tintaviva.core.DrawingDocument;
import com.kukocl.tintaviva.core.Layer;

import java.util.Locale;

public final class MainActivity extends Activity {
    private static final int INK = 0xff20382f;
    private static final int MUTED = 0xff667366;
    private static final int BACKGROUND = 0xfff5f3ed;
    private static final int ACTIVE = 0xffdcebcf;
    private static final int DEFAULT_COLOR = 0xff29302b;
    private TintaVivaApp session;
    private DrawingDocument document;
    private DrawingView drawingView;
    private SharedPreferences preferences;
    private LinearLayout sketchCard, lineCard, onlyCard, brushButton, eraserButton, colorButton, undoButton;
    private TextView sizeValue;
    private StudioIcon colorIcon;
    private int selectedColor;
    private int brushWidth;
    private boolean erasing;
    private boolean wasSaveErrorShown;
    private AlertDialog newDrawingDialog;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if (Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(false);
        else getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        session = (TintaVivaApp) getApplication();
        document = session.document;
        preferences = getSharedPreferences("studio", MODE_PRIVATE);
        selectedColor = preferences.getInt("color", DEFAULT_COLOR);
        brushWidth = Math.max(2, Math.min(32, preferences.getInt("width", 12)));
        erasing = preferences.getBoolean("eraser", false);
        document.setActiveLayer(preferences.getBoolean("lineArt", false) ? Layer.LINE_ART : Layer.SKETCH);
        if (preferences.getBoolean("lineArtOnly", false)) document.showOnlyLineArt();
        LinearLayout root = vertical();
        root.setBackgroundColor(BACKGROUND);
        root.setPadding(dp(8), 0, dp(8), 0);
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
            view.setPadding(dp(8) + left, top, dp(8) + right, bottom);
            return insets;
        });
        root.addView(makeHeader(), params(-1, 50));
        drawingView = new DrawingView(this, document, session.viewport);
        drawingView.setId(R.id.drawing_canvas);
        drawingView.setBrushColor(selectedColor);
        drawingView.setBrushWidth(brushWidth);
        drawingView.setErasing(erasing);
        drawingView.setListener(() -> {
            updateControls();
            session.store.save(document.snapshot());
        });
        Configuration configuration = getResources().getConfiguration();
        // Portrait tablets need the full width for the paper, just like phones.
        boolean sidePanel = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                && configuration.screenWidthDp >= 600;
        if (sidePanel) {
            LinearLayout body = horizontal();
            body.addView(drawingView, new LinearLayout.LayoutParams(0, -1, 1));
            LinearLayout panel = vertical();
            panel.setPadding(dp(8), dp(4), 0, dp(8));
            addLayers(panel);
            addTools(panel);
            ScrollView scroll = new ScrollView(this);
            scroll.setFillViewport(true);
            scroll.addView(panel);
            body.addView(scroll, params(270, -1));
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
        header.addView(logo, params(28, 28));
        LinearLayout title = vertical();
        title.setPadding(dp(10), 0, 0, 0);
        TextView name = text("Tinta Viva", 22, INK, true);
        name.setTypeface(Typeface.create("sans-serif-rounded", Typeface.BOLD));
        title.addView(name);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        undoButton = iconButton("undo", "Deshacer");
        undoButton.setId(R.id.action_undo);
        undoButton.setOnClickListener(view -> {
            drawingView.cancelPending();
            if (document.undo()) {
                drawingView.documentChanged();
                updateControls();
                session.store.save(document.snapshot());
            }
        });
        undoButton.setContentDescription("Deshacer la última acción, también si fue borrar un trazo");
        header.addView(undoButton, params(48, 48));
        LinearLayout newDrawingButton = iconButton("new", "Nuevo dibujo");
        newDrawingButton.setId(R.id.action_new_drawing);
        newDrawingButton.setOnClickListener(view -> confirmNewDrawing());
        LinearLayout.LayoutParams newDrawingParams = params(48, 48);
        newDrawingParams.leftMargin = dp(20);
        header.addView(newDrawingButton, newDrawingParams);
        return header;
    }

    private void confirmNewDrawing() {
        if (newDrawingDialog != null && newDrawingDialog.isShowing()) return;
        drawingView.cancelPending();
        newDrawingDialog = new AlertDialog.Builder(this)
                .setTitle("¿Empezar un dibujo nuevo?")
                .setMessage("Se borrarán todos los trazos de Sketch y Line art. Esta acción no se puede deshacer.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Nuevo dibujo", (dialog, which) -> {
                    drawingView.cancelPending();
                    document.clear();
                    session.viewport.reset();
                    drawingView.documentChanged();
                    selectLayer(Layer.SKETCH);
                    selectTool(false);
                    session.store.save(document.snapshot());
                })
                .create();
        newDrawingDialog.show();
        newDrawingDialog.getButton(AlertDialog.BUTTON_NEGATIVE).requestFocus();
    }

    private void addLayers(LinearLayout parent) {
        LinearLayout row = horizontal();
        sketchCard = layerCard("Sketch");
        sketchCard.setId(R.id.layer_sketch);
        lineCard = layerCard("Line art");
        lineCard.setId(R.id.layer_line_art);
        onlyCard = layerCard("Solo line art");
        onlyCard.setId(R.id.layer_line_art_only);
        for (LinearLayout card : new LinearLayout[]{sketchCard, lineCard, onlyCard}) {
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(0, dp(48), 1);
            if (card != onlyCard) cardParams.rightMargin = dp(4);
            row.addView(card, cardParams);
        }
        LinearLayout.LayoutParams layerParams = params(-1, 48);
        layerParams.bottomMargin = dp(4);
        parent.addView(row, layerParams);
        sketchCard.setOnClickListener(view -> selectLayer(Layer.SKETCH));
        lineCard.setOnClickListener(view -> selectLayer(Layer.LINE_ART));
        onlyCard.setOnClickListener(view -> {
            document.showOnlyLineArt();
            drawingView.layerChanged();
            preferences.edit().putBoolean("lineArt", true).putBoolean("lineArtOnly", true).apply();
            updateControls();
        });
    }

    private LinearLayout layerCard(String name) {
        LinearLayout card = vertical();
        card.setPadding(dp(4), 0, dp(4), 0);
        card.setGravity(Gravity.CENTER);
        TextView title = text(name, 12, INK, true);
        title.setGravity(Gravity.CENTER);
        title.setMaxLines(2);
        card.addView(title);
        card.setFocusable(true);
        card.setClickable(true);
        return card;
    }

    private void addTools(LinearLayout parent) {
        LinearLayout tools = horizontal();
        tools.setGravity(Gravity.CENTER_VERTICAL);
        tools.setPadding(0, dp(4), 0, dp(4));
        brushButton = iconButton("brush", "Pincel");
        brushButton.setId(R.id.tool_brush);
        eraserButton = iconButton("eraser", "Goma de trazos");
        eraserButton.setId(R.id.tool_eraser);
        colorButton = iconButton("palette", "Elegir color");
        colorButton.setId(R.id.tool_color);
        colorIcon = (StudioIcon) colorButton.getChildAt(0);
        brushButton.setOnClickListener(view -> selectTool(false));
        eraserButton.setOnClickListener(view -> selectTool(true));
        colorButton.setOnClickListener(view -> ColorPickerDialog.show(this, selectedColor, this::selectColor));
        for (LinearLayout button : new LinearLayout[]{brushButton, eraserButton, colorButton}) {
            LinearLayout.LayoutParams buttonParams = params(48, 48);
            buttonParams.rightMargin = dp(4);
            tools.addView(button, buttonParams);
        }
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
        tools.addView(size, new LinearLayout.LayoutParams(0, dp(48), 1));
        sizeValue = text(String.valueOf(brushWidth), 12, INK, true);
        sizeValue.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        tools.addView(sizeValue, params(24, 48));
        parent.addView(tools, params(-1, -2));
    }

    private LinearLayout iconButton(String icon, String label) {
        LinearLayout button = horizontal();
        button.setGravity(Gravity.CENTER);
        button.addView(new StudioIcon(this, icon, INK), params(25, 25));
        button.setFocusable(true);
        button.setClickable(true);
        button.setContentDescription(label);
        button.setTooltipText(label);
        button.setBackground(background(Color.WHITE, 0xffe1e3d9));
        return button;
    }

    private void selectLayer(Layer layer) {
        document.setActiveLayer(layer);
        drawingView.layerChanged();
        preferences.edit().putBoolean("lineArt", layer == Layer.LINE_ART).putBoolean("lineArtOnly", false).apply();
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
        styleSelection(lineCard, lineArt && !document.isLineArtOnly());
        styleSelection(onlyCard, document.isLineArtOnly());
        styleSelection(brushButton, !erasing);
        styleSelection(eraserButton, erasing);
        sketchCard.setContentDescription("Capa sketch" + (!lineArt ? ", seleccionada" : ""));
        lineCard.setContentDescription("Capa line art, sketch al 40 por ciento" + (lineCard.isSelected() ? ", seleccionada" : ""));
        onlyCard.setContentDescription("Solo line art, sketch oculto" + (document.isLineArtOnly() ? ", seleccionado" : ""));
        colorIcon.setAccentColor(selectedColor);
        colorButton.setContentDescription("Elegir color. Color actual " + hex(selectedColor));
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

    @Override protected void onDestroy() {
        if (newDrawingDialog != null) newDrawingDialog.dismiss();
        super.onDestroy();
    }
}
