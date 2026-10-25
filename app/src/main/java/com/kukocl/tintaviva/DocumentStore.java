package com.kukocl.tintaviva;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.AtomicFile;

import com.kukocl.tintaviva.core.DrawingDocument;
import com.kukocl.tintaviva.core.Layer;
import com.kukocl.tintaviva.core.Stroke;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Single writer plus AtomicFile keeps the last completed drawing if a write is interrupted. */
public final class DocumentStore {
    public interface ErrorListener { void onSaveError(); }
    private final AtomicFile file;
    private final ExecutorService writer = Executors.newSingleThreadExecutor();
    private final Context context;
    private final Handler mainThread = new Handler(Looper.getMainLooper());
    private volatile ErrorListener listener;
    private volatile boolean loadFailed;
    private String loadedImageName;
    private Bitmap loadedImage;

    public DocumentStore(Context context) {
        this.context = context.getApplicationContext();
        file = new AtomicFile(new File(context.getFilesDir(), "drawing-v1.json"));
    }

    public void setErrorListener(ErrorListener listener) { this.listener = listener; }
    public String getLoadedImageName() { return loadedImageName; }
    public Bitmap getLoadedImage() { return loadedImage; }
    Bitmap takeLoadedImage() {
        Bitmap image = loadedImage;
        loadedImage = null;
        return image;
    }

    public boolean loadInto(DrawingDocument document) {
        try {
            JSONObject root = new JSONObject(new String(file.readFully(), StandardCharsets.UTF_8));
            if (root.getInt("version") != 1) throw new JSONException("Unsupported version");
            JSONArray saved = root.getJSONArray("strokes");
            List<Stroke> strokes = new ArrayList<>();
            for (int i = 0; i < saved.length(); i++) {
                JSONObject object = saved.getJSONObject(i);
                JSONArray coordinates = object.getJSONArray("points");
                float[] points = new float[coordinates.length()];
                for (int j = 0; j < points.length; j++) points[j] = (float) coordinates.getDouble(j);
                strokes.add(new Stroke(object.getLong("id"), Layer.valueOf(object.getString("layer")),
                        object.getInt("color"), (float) object.getDouble("width"), points));
            }
            String imageName = root.optString("sketchImage", null);
            Bitmap image = imageName == null ? null : SketchImages.load(context, imageName);
            document.restore(strokes);
            loadedImageName = imageName;
            loadedImage = image;
            return true;
        } catch (FileNotFoundException missing) {
            return true;
        } catch (IOException | JSONException | IllegalArgumentException error) {
            // Keep an unreadable original. Never silently replace it with an empty drawing.
            loadFailed = true;
            return false;
        }
    }

    public Future<?> save(List<Stroke> snapshot) {
        return save(snapshot, null);
    }

    public Future<?> save(List<Stroke> snapshot, String imageName) {
        return writer.submit(() -> {
            FileOutputStream output = null;
            try {
                if (loadFailed) {
                    File backup = new File(context.getFilesDir(), "drawing-unreadable-" + System.currentTimeMillis() + ".json");
                    if (file.getBaseFile().exists() && !file.getBaseFile().renameTo(backup)) {
                        throw new IOException("Could not preserve original");
                    }
                    loadFailed = false;
                }
                JSONObject root = new JSONObject();
                root.put("version", 1);
                JSONArray strokes = new JSONArray();
                for (Stroke stroke : snapshot) {
                    JSONObject object = new JSONObject();
                    object.put("id", stroke.id);
                    object.put("layer", stroke.layer.name());
                    object.put("color", stroke.color);
                    object.put("width", stroke.width);
                    JSONArray points = new JSONArray();
                    for (float coordinate : stroke.copyPoints()) points.put((double) coordinate);
                    object.put("points", points);
                    strokes.put(object);
                }
                root.put("strokes", strokes);
                if (imageName != null) root.put("sketchImage", imageName);
                byte[] bytes = root.toString().getBytes(StandardCharsets.UTF_8);
                output = file.startWrite();
                output.write(bytes);
                file.finishWrite(output);
            } catch (IOException | JSONException error) {
                if (output != null) file.failWrite(output);
                mainThread.post(() -> {
                    ErrorListener current = listener;
                    if (current != null) current.onSaveError();
                });
            }
        });
    }

    public void close() { writer.shutdown(); }
}
