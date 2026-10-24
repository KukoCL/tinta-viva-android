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

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

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
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(gif))) {
            ImageReader reader = ImageIO.getImageReadersByFormatName("gif").next();
            try {
                reader.setInput(input);
                assertEquals(8, reader.getNumImages(true));
                assertEquals(216, reader.getWidth(0));
                assertEquals(270, reader.getHeight(0));
                int delay = 0;
                boolean changed = false;
                int[] first = reader.read(0).getRGB(0, 0, 216, 270, null, 0, 216);
                for (int frame = 0; frame < 8; frame++) {
                    Node tree = reader.getImageMetadata(frame).getAsTree("javax_imageio_gif_image_1.0");
                    Node control = find(tree, "GraphicControlExtension");
                    delay += Integer.parseInt(control.getAttributes().getNamedItem("delayTime").getNodeValue());
                    int[] pixels = reader.read(frame).getRGB(0, 0, 216, 270, null, 0, 216);
                    if (!Arrays.equals(first, pixels)) changed = true;
                    if (frame == 0) {
                        javax.imageio.metadata.IIOMetadataNode loop = (javax.imageio.metadata.IIOMetadataNode)
                                find(tree, "ApplicationExtension");
                        assertEquals("NETSCAPE", loop.getAttribute("applicationID"));
                        assertArrayEquals(new byte[]{1, 0, 0}, (byte[]) loop.getUserObject());
                    }
                }
                assertEquals(100, delay);
                assertTrue("El GIF debe contener animación real", changed);
            } finally { reader.dispose(); }
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
        BufferedImage frame = ImageIO.read(new ByteArrayInputStream(encode(snapshot)));
        assertEquals(Color.rgb(178, 178, 255), frame.getRGB(108, 135));
        assertEquals(Color.rgb(255, 178, 178), frame.getRGB(108, 70));
        GifExporter.Snapshot hidden = new GifExporter.Snapshot(document, image);
        BufferedImage empty = ImageIO.read(new ByteArrayInputStream(encode(hidden)));
        assertEquals(Color.WHITE, empty.getRGB(108, 70));
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
}
