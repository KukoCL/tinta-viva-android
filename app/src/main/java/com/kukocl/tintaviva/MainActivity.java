package com.kukocl.tintaviva;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.net.Uri;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.WindowInsets;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.kukocl.tintaviva.core.DrawingDocument;
import com.kukocl.tintaviva.core.Layer;
import androidx.core.content.FileProvider;

import java.io.File;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final int INK = 0xff20382f;
    private static final int MUTED = 0xff667366;
    private static final int BACKGROUND = 0xfff5f3ed;
    private static final int ACTIVE = 0xffdcebcf;
    private static final int DEFAULT_COLOR = 0xff29302b;
    private static final int PICK_SKETCH_IMAGE = 1001;
    private TintaVivaApp session;
    private DrawingDocument document;
    private DrawingView drawingView;
    private SharedPreferences preferences;
    private LinearLayout sketchCard, lineCard, onlyCard, brushButton, eraserButton, colorButton, undoButton;
    private LinearLayout redoButton, motionButton, fileButton, sizeButton, eyedropperButton;
    private LinearLayout root;
    private BrushSizeControl sizeControl;
    private PopupWindow sizePopup;
    private StudioIcon colorIcon;
    private int selectedColor;
    private int brushWidth;
    private boolean erasing;
    private boolean wasSaveErrorShown;
    private AlertDialog newDrawingDialog;
    private PopupMenu motionMenu;
    private PopupMenu fileMenu;

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
        root = vertical();
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
        drawingView.setColorPickListener(this::selectColor);
        drawingView.addOnLayoutChangeListener((view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (sizeControl != null) sizeControl.setBrush(selectedColor, brushWidth);
        });
        drawingView.setStrongMotion(preferences.getBoolean("strongMotion", true));
        drawingView.setSketchImage(session.sketchImage);
        drawingView.setListener(() -> {
            updateControls();
            session.saveDocument();
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
            addTools(panel, true);
            ScrollView scroll = new ScrollView(this);
            scroll.setFillViewport(true);
            scroll.addView(panel);
            body.addView(scroll, params(270, -1));
            root.addView(body, new LinearLayout.LayoutParams(-1, 0, 1));
        } else {
            addLayers(root);
            root.addView(drawingView, new LinearLayout.LayoutParams(-1, 0, 1));
            addTools(root, false);
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
        name.setSingleLine(true);
        name.setAutoSizeTextTypeUniformWithConfiguration(16, 22, 1, TypedValue.COMPLEX_UNIT_SP);
        title.addView(name);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        undoButton = iconButton("undo", "Deshacer");
        undoButton.setId(R.id.action_undo);
        undoButton.setOnClickListener(view -> {
            drawingView.cancelPending();
            if (document.undo()) {
                drawingView.documentChanged();
                updateControls();
                session.saveDocument();
            }
        });
        undoButton.setContentDescription("Deshacer la última acción, también si fue borrar un trazo");
        header.addView(undoButton, params(48, 48));
        redoButton = iconButton("redo", "Rehacer");
        redoButton.setId(R.id.action_redo);
        redoButton.setOnClickListener(view -> {
            drawingView.cancelPending();
            if (document.redo()) {
                drawingView.documentChanged();
                updateControls();
                session.saveDocument();
            }
        });
        redoButton.setContentDescription("Rehacer la última acción deshecha, también si fue borrar un trazo");
        LinearLayout.LayoutParams redoParams = params(48, 48);
        redoParams.leftMargin = dp(4);
        header.addView(redoButton, redoParams);
        fileButton = iconButton("menu", "Opciones del lienzo");
        fileButton.setId(R.id.action_file_menu);
        fileButton.setOnClickListener(this::showFileMenu);
        LinearLayout.LayoutParams fileParams = params(48, 48);
        fileParams.leftMargin = dp(20);
        header.addView(fileButton, fileParams);
        return header;
    }

    private void showFileMenu(View anchor) {
        drawingView.cancelPending();
        if (fileMenu != null) fileMenu.dismiss();
        fileMenu = new PopupMenu(this, anchor, Gravity.END);
        Menu menu = fileMenu.getMenu();
        menu.add(Menu.NONE, R.id.action_export_gif, 0, "Guardar GIF / compartir")
                .setEnabled(!session.isImporting() && !session.isExporting());
        menu.add(Menu.NONE, R.id.action_import_sketch, 1, "Importar imagen de sketch")
                .setEnabled(!session.isImporting());
        menu.add(Menu.NONE, R.id.action_remove_sketch, 2, "Quitar imagen de sketch")
                .setEnabled(session.sketchImage != null || session.isImporting());
        menu.add(Menu.NONE, R.id.action_new_drawing, 3, "Nuevo lienzo");
        fileMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_export_gif) {
                Toast.makeText(this, "Preparando GIF…", Toast.LENGTH_SHORT).show();
                session.exportGif();
            } else if (id == R.id.action_import_sketch) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*")
                        .addCategory(Intent.CATEGORY_OPENABLE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                try { startActivityForResult(intent, PICK_SKETCH_IMAGE); }
                catch (ActivityNotFoundException error) {
                    Toast.makeText(this, "No se encontró una aplicación para seleccionar imágenes.", Toast.LENGTH_LONG).show();
                }
            } else if (id == R.id.action_remove_sketch) session.removeSketchImage();
            else if (id == R.id.action_new_drawing) confirmNewDrawing();
            else return false;
            return true;
        });
        fileMenu.show();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_SKETCH_IMAGE || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        selectLayer(Layer.SKETCH);
        selectTool(false);
        Toast.makeText(this, "Cargando imagen…", Toast.LENGTH_SHORT).show();
        session.importSketchImage(data.getData());
    }

    private void refreshMedia() {
        drawingView.setSketchImage(session.sketchImage);
        updateControls();
        String error = session.takeMediaError();
        if (error != null) Toast.makeText(this, error, Toast.LENGTH_LONG).show();
        File gif = session.takePendingShare();
        if (gif == null) return;
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".exports", gif);
        // Telegram routes image/* through its photo editor, which flattens shared GIFs.
        // Send the original file; FileProvider still reports its actual image/gif type.
        Intent share = new Intent(Intent.ACTION_SEND).setType("application/octet-stream")
                .putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        share.setClipData(ClipData.newUri(getContentResolver(), "Tinta Viva", uri));
        try { startActivity(Intent.createChooser(share, "Compartir GIF")); }
        catch (ActivityNotFoundException missing) {
            Toast.makeText(this, "No se encontró una aplicación para compartir el GIF.", Toast.LENGTH_LONG).show();
        }
    }

    private void showMotionPanel(View anchor) {
        drawingView.cancelPending();
        if (motionMenu != null) motionMenu.dismiss();
        motionMenu = new PopupMenu(this, anchor, Gravity.END);
        Menu menu = motionMenu.getMenu();
        menu.add(1, R.id.motion_soft, 0, "Movimiento suave").setChecked(!document.isStrongMotion());
        menu.add(1, R.id.motion_strong, 1, "Movimiento intenso").setChecked(document.isStrongMotion());
        menu.setGroupCheckable(1, true, true);
        motionMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() != R.id.motion_soft && item.getItemId() != R.id.motion_strong) return false;
            boolean strong = item.getItemId() == R.id.motion_strong;
            drawingView.setStrongMotion(strong);
            preferences.edit().putBoolean("strongMotion", strong).apply();
            updateControls();
            return true;
        });
        motionMenu.show();
    }

    private void confirmNewDrawing() {
        if (newDrawingDialog != null && newDrawingDialog.isShowing()) return;
        drawingView.cancelPending();
        newDrawingDialog = new AlertDialog.Builder(this)
                .setTitle("¿Crear un lienzo nuevo?")
                .setMessage("Se borrarán la imagen de sketch y todos los trazos de Sketch y Line art. Esta acción no se puede deshacer.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Nuevo lienzo", (dialog, which) -> {
                    drawingView.cancelPending();
                    document.clear();
                    session.removeSketchImage();
                    session.viewport.reset();
                    drawingView.documentChanged();
                    selectLayer(Layer.SKETCH);
                    selectTool(false);
                    session.saveDocument();
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
        motionButton = iconButton("motion", "Movimiento de las líneas");
        motionButton.setId(R.id.action_motion);
        motionButton.setOnClickListener(this::showMotionPanel);
        LinearLayout.LayoutParams motionParams = params(48, 48);
        motionParams.leftMargin = dp(8);
        row.addView(motionButton, motionParams);
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

    private void addTools(LinearLayout parent, boolean sidePanel) {
        LinearLayout tools = horizontal();
        tools.setGravity(Gravity.CENTER);
        tools.setPadding(0, dp(4), 0, dp(4));
        brushButton = iconButton("brush", "Pincel");
        brushButton.setId(R.id.tool_brush);
        eraserButton = iconButton("eraser", "Goma de trazos");
        eraserButton.setId(R.id.tool_eraser);
        colorButton = iconButton("palette", "Elegir color");
        colorButton.setId(R.id.tool_color);
        colorIcon = (StudioIcon) colorButton.getChildAt(0);
        eyedropperButton = iconButton("eyedropper", "Tomar color del lienzo");
        eyedropperButton.setId(R.id.tool_eyedropper);
        brushButton.setOnClickListener(view -> selectTool(false));
        eraserButton.setOnClickListener(view -> selectTool(true));
        colorButton.setOnClickListener(view -> {
            drawingView.cancelPending();
            ColorPickerDialog.show(this, selectedColor, this::selectColor);
        });
        eyedropperButton.setOnClickListener(view -> {
            drawingView.setColorPicking(!drawingView.isColorPicking());
            updateControls();
        });
        for (LinearLayout button : new LinearLayout[]{brushButton, eraserButton, colorButton, eyedropperButton}) {
            LinearLayout.LayoutParams buttonParams = params(48, 48);
            if (button != brushButton) buttonParams.leftMargin = dp(4);
            tools.addView(button, buttonParams);
        }
        Configuration config = getResources().getConfiguration();
        boolean phone = Math.min(config.screenWidthDp, config.screenHeightDp) < 600;
        if (phone) {
            sizeButton = iconButton("size", "Grosor del pincel");
            sizeButton.setId(R.id.tool_brush_size);
            sizeButton.setOnClickListener(view -> showBrushSizePanel(tools));
            LinearLayout.LayoutParams sizeParams = params(48, 48);
            sizeParams.leftMargin = dp(4);
            tools.addView(sizeButton, sizeParams);
        } else {
            sizeControl = makeBrushSizeControl(true);
            if (!sidePanel) {
                LinearLayout.LayoutParams sizeParams = new LinearLayout.LayoutParams(0, -2, 1);
                sizeParams.leftMargin = dp(4);
                tools.addView(sizeControl, sizeParams);
            }
        }
        parent.addView(tools, params(-1, -2));
        if (!phone && sidePanel) parent.addView(sizeControl, params(-1, -2));
    }

    private BrushSizeControl makeBrushSizeControl(boolean floatingPreview) {
        return new BrushSizeControl(this, session.viewport, selectedColor, brushWidth, floatingPreview, width -> {
            brushWidth = width;
            drawingView.setBrushWidth(width);
            preferences.edit().putInt("width", width).apply();
            updateBrushSize();
        });
    }

    private void showBrushSizePanel(View anchor) {
        if (sizePopup != null && sizePopup.isShowing()) return;
        drawingView.cancelPending();
        sizeControl = makeBrushSizeControl(false);
        sizeControl.setPadding(dp(8), dp(8), dp(8), dp(8));
        int width = root.getWidth() - root.getPaddingLeft() - root.getPaddingRight();
        sizeControl.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        sizePopup = new PopupWindow(sizeControl, width, -2, true);
        sizePopup.setBackgroundDrawable(background(BACKGROUND, 0xffb7cba7));
        sizePopup.setElevation(dp(8));
        sizePopup.setOutsideTouchable(true);
        sizePopup.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
        sizePopup.setOnDismissListener(() -> {
            sizeControl = null;
            styleSelection(sizeButton, false);
        });
        int[] rootPosition = new int[2], anchorPosition = new int[2];
        root.getLocationOnScreen(rootPosition);
        anchor.getLocationOnScreen(anchorPosition);
        sizePopup.showAsDropDown(anchor, rootPosition[0] + root.getPaddingLeft() - anchorPosition[0],
                -anchor.getHeight() - sizeControl.getMeasuredHeight() - dp(8), Gravity.START);
        styleSelection(sizeButton, true);
    }

    private void updateBrushSize() {
        if (sizeControl != null) sizeControl.setBrush(selectedColor, brushWidth);
        if (sizeButton != null) {
            sizeButton.setContentDescription("Grosor del pincel: " + brushWidth + ". Abrir ajuste de grosor");
        }
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
        drawingView.setColorPicking(false);
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
        boolean picking = drawingView.isColorPicking();
        styleSelection(brushButton, !erasing && !picking);
        styleSelection(eraserButton, erasing && !picking);
        styleSelection(eyedropperButton, picking);
        eyedropperButton.setContentDescription("Tomar color del lienzo" + (picking
                ? ", seleccionado. Mantén pulsado y desliza sobre el lienzo; suelta para usar el color." : ""));
        sketchCard.setContentDescription("Capa sketch" + (!lineArt ? ", seleccionada" : ""));
        lineCard.setContentDescription("Capa line art, sketch al 30 por ciento" + (lineCard.isSelected() ? ", seleccionada" : ""));
        onlyCard.setContentDescription("Solo line art, sketch oculto" + (document.isLineArtOnly() ? ", seleccionado" : ""));
        colorIcon.setAccentColor(selectedColor);
        colorButton.setContentDescription("Elegir color. Color actual " + hex(selectedColor));
        updateBrushSize();
        undoButton.setEnabled(document.canUndo());
        undoButton.setAlpha(document.canUndo() ? 1 : .35f);
        redoButton.setEnabled(document.canRedo());
        redoButton.setAlpha(document.canRedo() ? 1 : .35f);
        motionButton.setContentDescription("Movimiento de las líneas: " + (document.isStrongMotion() ? "intenso" : "suave"));
        fileButton.setAlpha(session.isExporting() ? .6f : 1);
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
        session.setMediaListener(this::refreshMedia);
        refreshMedia();
    }

    @Override protected void onPause() {
        if (sizeControl != null) sizeControl.dismissPreview();
        if (sizePopup != null) sizePopup.dismiss();
        drawingView.setAnimationAllowed(false);
        session.saveDocument();
        session.store.setErrorListener(null);
        session.setMediaListener(null);
        super.onPause();
    }

    @Override protected void onDestroy() {
        if (sizePopup != null) sizePopup.dismiss();
        if (newDrawingDialog != null) newDrawingDialog.dismiss();
        if (motionMenu != null) motionMenu.dismiss();
        if (fileMenu != null) fileMenu.dismiss();
        super.onDestroy();
    }
}
