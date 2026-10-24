package com.kukocl.tintaviva;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import com.kukocl.tintaviva.core.DrawingDocument;
import com.kukocl.tintaviva.core.Layer;
import com.kukocl.tintaviva.core.LineAnimation;
import com.kukocl.tintaviva.core.Stroke;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Shared document-space rendering for the editor and exported animation. */
final class DrawingRenderer {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Map<Long, Path[]> paths = new HashMap<>();

    void clear() { paths.clear(); }

    void retain(List<Stroke> strokes) {
        Set<Long> ids = new HashSet<>();
        for (Stroke stroke : strokes) ids.add(stroke.id);
        paths.keySet().retainAll(ids);
    }

    void draw(Canvas canvas, List<Stroke> strokes, Bitmap sketchImage, float sketchOpacity,
              boolean strongMotion, int frame) {
        if (sketchOpacity > 0) {
            int saved = canvas.saveLayerAlpha(0, 0, DrawingDocument.WIDTH, DrawingDocument.HEIGHT,
                    Math.round(sketchOpacity * 255));
            drawSketchImage(canvas, sketchImage);
            drawLayer(canvas, strokes, Layer.SKETCH, strongMotion, frame);
            canvas.restoreToCount(saved);
        }
        drawLayer(canvas, strokes, Layer.LINE_ART, strongMotion, frame);
    }

    void drawSketchImage(Canvas canvas, Bitmap image) {
        if (image == null) return;
        float scale = Math.min((float) DrawingDocument.WIDTH / image.getWidth(),
                (float) DrawingDocument.HEIGHT / image.getHeight());
        float width = image.getWidth() * scale, height = image.getHeight() * scale;
        float left = (DrawingDocument.WIDTH - width) / 2;
        float top = (DrawingDocument.HEIGHT - height) / 2;
        paint.setAlpha(255);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawBitmap(image, null, new RectF(left, top, left + width, top + height), paint);
    }

    void drawLayer(Canvas canvas, List<Stroke> strokes, Layer layer, boolean strongMotion, int frame) {
        for (Stroke stroke : strokes) {
            if (stroke.layer != layer) continue;
            Path[] frames = paths.get(stroke.id);
            if (frames == null) {
                frames = new Path[layer == Layer.LINE_ART ? LineAnimation.FRAME_COUNT : 1];
                paths.put(stroke.id, frames);
            }
            int index = layer == Layer.LINE_ART ? Math.floorMod(frame, LineAnimation.FRAME_COUNT) : 0;
            if (frames[index] == null) {
                float[] points = LineAnimation.pointsAt(stroke, index, strongMotion);
                frames[index] = layer == Layer.LINE_ART
                        ? AnimatedStrokePath.create(points, LineAnimation.widthsAt(stroke, index)) : makePath(points);
            }
            paint.setColor(stroke.color);
            paint.setAlpha(255);
            paint.setStrokeWidth(stroke.width);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            if (layer == Layer.LINE_ART) {
                paint.setStyle(Paint.Style.FILL);
                canvas.drawPath(frames[index], paint);
            } else if (stroke.pointCount() == 1) {
                float[] point = stroke.copyPoints();
                paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(point[0], point[1], stroke.width / 2, paint);
            } else {
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawPath(frames[index], paint);
            }
        }
    }

    static Path makePath(float[] points) {
        Path path = new Path();
        if (points.length == 0) return path;
        path.moveTo(points[0], points[1]);
        for (int i = 2; i < points.length; i += 2) path.lineTo(points[i], points[i + 1]);
        return path;
    }
}
