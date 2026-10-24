package com.kukocl.tintaviva;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;

import com.kukocl.tintaviva.core.DrawingDocument;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = Application.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class SketchImagesTest {
    private Context context;
    @Before public void setUp() { context = RuntimeEnvironment.getApplication(); }

    @Test public void importedImageIsBoundedAndSurvivesWithoutItsOriginalFile() throws Exception {
        File source = source(2400, 1200);
        SketchImages.Imported image = SketchImages.importImage(context, Uri.fromFile(source));
        assertEquals(1080, image.bitmap.getWidth());
        assertEquals(540, image.bitmap.getHeight());
        assertEquals(Color.RED, image.bitmap.getPixel(100, 100));
        assertTrue(source.delete());
        assertEquals(Color.RED, SketchImages.load(context, image.name).getPixel(100, 100));
    }

    @Test @Config(sdk = 26)
    public void androidEightAlsoImportsAndFitsImages() throws Exception {
        SketchImages.Imported image = SketchImages.importImage(context, Uri.fromFile(source(2400, 1200)));
        assertEquals(1080, image.bitmap.getWidth());
        assertEquals(540, image.bitmap.getHeight());
        assertEquals(Color.RED, image.bitmap.getPixel(100, 100));
    }

    @Test public void savingAndReopeningKeepsTheImageAndStrokesAndSupportsRemovingOnlyTheImage() throws Exception {
        SketchImages.Imported image = SketchImages.importImage(context, Uri.fromFile(source(200, 100)));
        DrawingDocument document = new DrawingDocument();
        document.addStroke(Color.BLUE, 12, new float[]{100, 100});
        DocumentStore store = new DocumentStore(context);
        DocumentStore reopened = new DocumentStore(context);
        try {
            store.save(document.snapshot(), image.name).get(5, TimeUnit.SECONDS);
            DrawingDocument restored = new DrawingDocument();
            assertTrue(reopened.loadInto(restored));
            assertEquals(1, restored.getStrokes().size());
            assertEquals(Color.BLUE, restored.getStrokes().get(0).color);
            assertEquals(image.name, reopened.getLoadedImageName());
            assertNotNull(reopened.getLoadedImage());
            store.save(document.snapshot(), null).get(5, TimeUnit.SECONDS);
            assertTrue(reopened.loadInto(restored));
            assertNull(reopened.getLoadedImage());
            assertEquals(1, restored.getStrokes().size());
            document.clear();
            store.save(document.snapshot(), null).get(5, TimeUnit.SECONDS);
            assertTrue(reopened.loadInto(restored));
            assertTrue(restored.getStrokes().isEmpty());
        } finally { store.close(); reopened.close(); }
    }

    @Test public void invalidImagesAreRejected() throws Exception {
        File invalid = File.createTempFile("invalid-", ".png", context.getCacheDir());
        try (FileOutputStream output = new FileOutputStream(invalid)) { output.write(new byte[]{1, 2, 3}); }
        try {
            SketchImages.importImage(context, Uri.fromFile(invalid));
            fail("Un archivo inválido no debe aceptarse como imagen");
        } catch (IOException expected) { }
    }

    private File source(int width, int height) throws Exception {
        File source = File.createTempFile("source-", ".png", context.getCacheDir());
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(Color.RED);
        try (FileOutputStream output = new FileOutputStream(source)) {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output));
        }
        bitmap.recycle();
        return source;
    }
}
