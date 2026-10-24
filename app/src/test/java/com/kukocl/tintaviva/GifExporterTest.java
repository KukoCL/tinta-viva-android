package com.kukocl.tintaviva;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.Color;

import com.kukocl.tintaviva.core.DrawingDocument;
import com.kukocl.tintaviva.core.Layer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.w3c.dom.Node;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.util.Arrays;
import java.util.Iterator;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = Application.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class GifExporterTest {
    @Test public void exportedGifDecodesAsEightAnimatedFramesWithAOneSecondInfiniteLoop() throws Exception {
        DrawingDocument document = new DrawingDocument();
        document.setActiveLayer(Layer.LINE_ART);
        document.addStroke(Color.BLACK, 50, new float[]{150, 300, 400, 400, 800, 300});
        byte[] gif = encode(new GifExporter.Snapshot(document, null));
        assertEquals("GIF89a", new String(gif, 0, 6, java.nio.charset.StandardCharsets.US_ASCII));
        try (GifReader reader = new GifReader(gif)) {
                assertEquals(8, reader.count());
                assertEquals(216, reader.width());
                assertEquals(270, reader.height());
                int delay = 0;
                boolean changed = false;
                int[] first = reader.pixels(0);
                for (int frame = 0; frame < 8; frame++) {
                    Node tree = reader.metadata(frame);
                    Node control = find(tree, "GraphicControlExtension");
                    delay += Integer.parseInt(control.getAttributes().getNamedItem("delayTime").getNodeValue());
                    int[] pixels = reader.pixels(frame);
                    if (!Arrays.equals(first, pixels)) changed = true;
                    if (frame == 0) {
                        Node loop = find(tree, "ApplicationExtension");
                        assertEquals("NETSCAPE", loop.getAttributes().getNamedItem("applicationID").getNodeValue());
                        byte[] looping = (byte[]) Class.forName("javax.imageio.metadata.IIOMetadataNode")
                                .getMethod("getUserObject").invoke(loop);
                        assertArrayEquals(new byte[]{1, 0, 0}, looping);
                    }
                }
                assertEquals(100, delay);
                assertTrue("El GIF debe contener animación real", changed);
        }
    }

    @Test public void snapshotKeepsTheCapturedImageOpacityAndStrokesAfterTheEditorChanges() throws Exception {
        DrawingDocument document = new DrawingDocument();
        document.addStroke(Color.BLUE, 150, new float[]{200, 675, 880, 675});
        Bitmap image = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888);
        image.eraseColor(Color.RED);
        document.setActiveLayer(Layer.LINE_ART);
        GifExporter.Snapshot snapshot = new GifExporter.Snapshot(document, image);
        document.clear();
        document.showOnlyLineArt();
        try (GifReader frame = new GifReader(encode(snapshot))) {
            assertEquals(Color.rgb(178, 178, 255), frame.pixel(0, 108, 135));
            assertEquals(Color.rgb(255, 178, 178), frame.pixel(0, 108, 70));
        }
        GifExporter.Snapshot hidden = new GifExporter.Snapshot(document, image);
        try (GifReader empty = new GifReader(encode(hidden))) {
            assertEquals(Color.WHITE, empty.pixel(0, 108, 70));
        }
    }

    private byte[] encode(GifExporter.Snapshot snapshot) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        GifExporter.write(output, snapshot, 216, 270);
        return output.toByteArray();
    }

    private Node find(Node node, String name) {
        if (node.getNodeName().equals(name)) return node;
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            Node found = find(child, name);
            if (found != null) return found;
        }
        return null;
    }

    /** Android's compile classpath omits desktop APIs; the independent decoder runs on the test JDK. */
    private static final class GifReader implements AutoCloseable {
        private final Class<?> type = Class.forName("javax.imageio.ImageReader");
        private final Class<?> imageType = Class.forName("java.awt.image.BufferedImage");
        private final Object reader;
        private final Closeable input;

        GifReader(byte[] gif) throws Exception {
            Class<?> io = Class.forName("javax.imageio.ImageIO");
            Iterator<?> readers = (Iterator<?>) io.getMethod("getImageReadersByFormatName", String.class)
                    .invoke(null, "gif");
            reader = readers.next();
            input = (Closeable) io.getMethod("createImageInputStream", Object.class)
                    .invoke(null, new ByteArrayInputStream(gif));
            type.getMethod("setInput", Object.class).invoke(reader, input);
        }

        int count() throws Exception { return (Integer) type.getMethod("getNumImages", boolean.class).invoke(reader, true); }
        int width() throws Exception { return (Integer) type.getMethod("getWidth", int.class).invoke(reader, 0); }
        int height() throws Exception { return (Integer) type.getMethod("getHeight", int.class).invoke(reader, 0); }

        int[] pixels(int frame) throws Exception {
            Object image = type.getMethod("read", int.class).invoke(reader, frame);
            return (int[]) imageType.getMethod("getRGB", int.class, int.class, int.class, int.class,
                    int[].class, int.class, int.class).invoke(image, 0, 0, width(), height(), null, 0, width());
        }

        int pixel(int frame, int x, int y) throws Exception { return pixels(frame)[y * width() + x]; }

        Node metadata(int frame) throws Exception {
            Object metadata = type.getMethod("getImageMetadata", int.class).invoke(reader, frame);
            return (Node) Class.forName("javax.imageio.metadata.IIOMetadata")
                    .getMethod("getAsTree", String.class).invoke(metadata, "javax_imageio_gif_image_1.0");
        }

        @Override public void close() throws Exception {
            try { type.getMethod("dispose").invoke(reader); }
            finally { input.close(); }
        }
    }
}
