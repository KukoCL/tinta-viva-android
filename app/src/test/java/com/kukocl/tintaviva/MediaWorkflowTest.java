package com.kukocl.tintaviva;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.PopupMenu;
import androidx.core.content.FileProvider;

import com.kukocl.tintaviva.core.Layer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowPopupMenu;
import org.robolectric.util.ReflectionHelpers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, qualifiers = "w360dp-h640dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class MediaWorkflowTest {
    private ActivityController<MainActivity> controller;
    private MainActivity activity;
    private TintaVivaApp session;

    @Before public void setUp() {
        session = (TintaVivaApp) RuntimeEnvironment.getApplication();
        // Robolectric assigns a new cache directory per test; Android keeps one per install.
        ((Map<?, ?>) ReflectionHelpers.getStaticField(FileProvider.class, "sCache")).clear();
        // Android knows GIFs; Robolectric's MIME table starts empty.
        shadowOf(MimeTypeMap.getSingleton()).addExtensionMimeTypeMapping("gif", "image/gif");
        controller = Robolectric.buildActivity(MainActivity.class).setup();
        activity = controller.get();
    }

    @After public void tearDown() {
        controller.pause().stop().destroy();
        session.store.close();
    }

    @Test public void fileMenuOffersFourActionsAndImportUsesTheAndroidImagePicker() {
        PopupMenu menu = menu();
        assertEquals(4, menu.getMenu().size());
        assertFalse(menu.getMenu().findItem(R.id.action_remove_sketch).isEnabled());
        assertTrue(menu.getMenu().performIdentifierAction(R.id.action_import_sketch, 0));
        menu.dismiss();
        ShadowActivity.IntentForResult request = shadowOf(activity).getNextStartedActivityForResult();
        assertEquals(Intent.ACTION_OPEN_DOCUMENT, request.intent.getAction());
        assertEquals("image/*", request.intent.getType());
        assertTrue(request.intent.hasCategory(Intent.CATEGORY_OPENABLE));
        activity.onActivityResult(request.requestCode, Activity.RESULT_CANCELED, null);
        assertNull(session.sketchImage);
        assertFalse(session.isImporting());
    }

    @Test public void sharesTheOriginalAnimatedGifAsAFileSoTelegramDoesNotFlattenIt() throws Exception {
        session.document.setActiveLayer(Layer.LINE_ART);
        session.document.addStroke(Color.BLACK, 20, new float[]{100, 100, 700, 700});
        choose(R.id.action_export_gif);
        awaitMedia();
        Intent chooser = shadowOf(activity).getNextStartedActivity();
        assertNotNull(chooser);
        assertEquals(Intent.ACTION_CHOOSER, chooser.getAction());
        Intent send = chooser.getParcelableExtra(Intent.EXTRA_INTENT);
        assertNotNull(send);
        assertEquals(Intent.ACTION_SEND, send.getAction());
        assertEquals("application/octet-stream", send.getType());
        assertTrue((send.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0);
        Uri uri = send.getParcelableExtra(Intent.EXTRA_STREAM);
        assertEquals("content", uri.getScheme());
        assertEquals(activity.getPackageName() + ".exports", uri.getAuthority());
        assertEquals(uri, send.getClipData().getItemAt(0).getUri());
        assertEquals("image/gif", activity.getContentResolver().getType(uri));
        try (android.database.Cursor metadata = activity.getContentResolver().query(uri,
                new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}, null, null, null)) {
            assertNotNull(metadata);
            assertTrue(metadata.moveToFirst());
            assertTrue(metadata.getString(0).endsWith(".gif"));
            assertTrue(metadata.getLong(1) > 0);
        }
        try (InputStream input = activity.getContentResolver().openInputStream(uri)) {
            assertNotNull(input);
            byte[] gif = input.readAllBytes();
            assertEquals("GIF89a", new String(gif, 0, 6, StandardCharsets.US_ASCII));
            try (GifExporterTest.GifReader reader = new GifExporterTest.GifReader(gif)) {
                assertEquals(8, reader.count());
                assertEquals(540, reader.width());
                assertEquals(675, reader.height());
                int[] first = reader.pixels(0);
                boolean animated = false;
                for (int frame = 1; frame < reader.count(); frame++) {
                    if (!Arrays.equals(first, reader.pixels(frame))) animated = true;
                }
                assertTrue("El archivo compartido debe conservar la animación", animated);
            }
        }
    }

    @Test public void importingAnImageKeepsExistingStrokesSelectsSketchAndSurvivesRotation() throws Exception {
        session.document.addStroke(Color.BLUE, 12, new float[]{100, 100});
        choose(R.id.action_import_sketch);
        ShadowActivity.IntentForResult request = shadowOf(activity).getNextStartedActivityForResult();
        File source = File.createTempFile("reference-", ".png", session.getCacheDir());
        Bitmap image = reference();
        try (FileOutputStream output = new FileOutputStream(source)) {
            assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, output));
        }
        activity.onActivityResult(request.requestCode, Activity.RESULT_OK, new Intent().setData(Uri.fromFile(source)));
        awaitMedia();
        assertNotNull(session.sketchImage);
        assertEquals(Color.RED, session.sketchImage.getPixel(5, 5));
        assertEquals(1, session.document.getStrokes().size());
        assertEquals(Layer.SKETCH, session.document.getActiveLayer());
        assertTrue(activity.findViewById(R.id.tool_brush).isSelected());
        assertTrue(source.delete());
        controller.recreate();
        activity = controller.get();
        assertEquals(Color.RED, session.sketchImage.getPixel(5, 5));
        assertEquals(1, session.document.getStrokes().size());
    }

    @Test public void removingAnImageKeepsStrokesAndNewCanvasRequiresConfirmationToClearBoth() {
        session.sketchImage = reference();
        session.document.addStroke(Color.BLUE, 12, new float[]{100, 100});
        choose(R.id.action_remove_sketch);
        assertNull(session.sketchImage);
        assertEquals(1, session.document.getStrokes().size());
        session.sketchImage = reference();
        choose(R.id.action_new_drawing);
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();
        assertNotNull(session.sketchImage);
        assertEquals(1, session.document.getStrokes().size());
        choose(R.id.action_new_drawing);
        ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();
        assertNull(session.sketchImage);
        assertTrue(session.document.getStrokes().isEmpty());
        assertFalse(session.document.canUndo());
        assertFalse(session.document.canRedo());
    }

    @Test public void newCanvasCancelsAnImageImportThatFinishesLater() throws Exception {
        File source = File.createTempFile("reference-", ".png", session.getCacheDir());
        try (FileOutputStream output = new FileOutputStream(source)) {
            assertTrue(reference().compress(Bitmap.CompressFormat.PNG, 100, output));
        }
        session.importSketchImage(Uri.fromFile(source));
        choose(R.id.action_new_drawing);
        ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();
        choose(R.id.action_export_gif);
        awaitMedia();
        assertNull("Una importación anterior no debe reaparecer en el lienzo nuevo", session.sketchImage);
        assertTrue(session.document.getStrokes().isEmpty());
    }

    private Bitmap reference() {
        Bitmap bitmap = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(Color.RED);
        return bitmap;
    }

    private PopupMenu menu() {
        View decor = activity.getWindow().getDecorView();
        decor.measure(View.MeasureSpec.makeMeasureSpec(360, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(640, View.MeasureSpec.EXACTLY));
        decor.layout(0, 0, 360, 640);
        activity.findViewById(R.id.action_file_menu).performClick();
        return ShadowPopupMenu.getLatestPopupMenu();
    }

    private void choose(int action) {
        PopupMenu menu = menu();
        assertTrue(menu.getMenu().performIdentifierAction(action, 0));
        menu.dismiss();
    }

    private void awaitMedia() throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        while (session.isImporting() || session.isExporting()) {
            shadowOf(Looper.getMainLooper()).idle();
            if (System.nanoTime() > deadline) fail("La operación de imagen no terminó a tiempo");
            Thread.sleep(10);
        }
        shadowOf(Looper.getMainLooper()).idle();
    }
}
