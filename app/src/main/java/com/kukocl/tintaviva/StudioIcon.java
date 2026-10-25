package com.kukocl.tintaviva;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/** Small vector icons drawn without an external icon library. */
final class StudioIcon extends View {
    private final String kind;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int color;
    private int accentColor = 0xff37a58a;

    StudioIcon(Context context, String kind, int color) {
        super(context);
        this.kind = kind;
        this.color = color;
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    void setColor(int color) { this.color = color; invalidate(); }
    void setAccentColor(int color) { accentColor = color; invalidate(); }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int saved = canvas.save();
        float side = Math.min(getWidth(), getHeight());
        canvas.translate((getWidth() - side) / 2, (getHeight() - side) / 2);
        canvas.scale(side / 24, side / 24);
        paint.setColor(color);
        paint.setStrokeWidth(1.8f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStyle(Paint.Style.STROKE);
        Path path = new Path();
        switch (kind) {
            case "size":
                path.moveTo(4, 5); path.lineTo(20, 5);
                canvas.drawPath(path, paint);
                path.reset();
                paint.setStrokeWidth(4);
                path.moveTo(4, 12); path.lineTo(20, 12);
                canvas.drawPath(path, paint);
                path.reset();
                paint.setStrokeWidth(6);
                path.moveTo(5, 20); path.lineTo(19, 20);
                break;
            case "eyedropper":
                int pipette = canvas.save();
                canvas.rotate(45, 12, 12);
                path.moveTo(9.5f, 7); path.lineTo(9.5f, 5.5f);
                path.arcTo(9.5f, 3, 14.5f, 8, 180, 180, false);
                path.lineTo(14.5f, 7); path.close();
                paint.setStyle(Paint.Style.FILL_AND_STROKE);
                canvas.drawPath(path, paint);
                path.reset();
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawRoundRect(8.5f, 7, 15.5f, 9.5f, .8f, .8f, paint);
                path.moveTo(10.5f, 9.5f); path.lineTo(10.5f, 16.5f);
                path.lineTo(12, 19); path.lineTo(13.5f, 16.5f); path.lineTo(13.5f, 9.5f);
                canvas.drawPath(path, paint);
                canvas.restoreToCount(pipette);
                path.reset();
                paint.setStyle(Paint.Style.FILL);
                path.moveTo(4.2f, 18.4f);
                path.cubicTo(3.6f, 19.3f, 2.6f, 20.3f, 2.6f, 21);
                path.arcTo(2.6f, 19.4f, 5.8f, 22.6f, 180, -180, false);
                path.cubicTo(5.8f, 20.3f, 4.8f, 19.3f, 4.2f, 18.4f);
                path.close();
                break;
            case "menu":
                paint.setStyle(Paint.Style.FILL);
                path.addCircle(12, 5, 1.8f, Path.Direction.CW);
                path.addCircle(12, 12, 1.8f, Path.Direction.CW);
                path.addCircle(12, 19, 1.8f, Path.Direction.CW);
                break;
            case "motion":
                path.moveTo(3, 12); path.cubicTo(6, 2, 9, 2, 12, 12);
                path.cubicTo(15, 22, 18, 22, 21, 12);
                break;
            case "redo":
                path.moveTo(16, 5); path.lineTo(21, 10); path.lineTo(16, 15);
                path.moveTo(20, 10); path.lineTo(10, 10);
                path.cubicTo(2, 10, 2, 20, 10, 20);
                break;
            case "new":
                path.moveTo(4, 3); path.lineTo(14, 3); path.lineTo(20, 9);
                path.lineTo(20, 21); path.lineTo(4, 21); path.close();
                path.moveTo(14, 3); path.lineTo(14, 9); path.lineTo(20, 9);
                path.moveTo(8, 15); path.lineTo(16, 15);
                path.moveTo(12, 11); path.lineTo(12, 19);
                break;
            case "undo":
                path.moveTo(8, 5); path.lineTo(3, 10); path.lineTo(8, 15);
                path.moveTo(4, 10); path.lineTo(14, 10);
                path.cubicTo(22, 10, 22, 20, 14, 20);
                break;
            case "eraser":
                path.moveTo(4, 14); path.lineTo(13, 4); path.lineTo(21, 11);
                path.lineTo(13, 20); path.lineTo(10, 20); path.close();
                path.moveTo(9, 9); path.lineTo(17, 16);
                path.moveTo(11, 20); path.lineTo(22, 20);
                break;
            case "brush":
                path.moveTo(9, 15); path.lineTo(17, 4);
                path.cubicTo(20, 1, 23, 4, 20, 7);
                path.lineTo(12, 17); path.close();
                path.moveTo(9, 16); path.cubicTo(3, 14, 8, 23, 2, 21);
                path.cubicTo(8, 24, 13, 21, 11, 18);
                break;
            case "palette":
                path.moveTo(12, 3);
                path.cubicTo(1, 3, 0, 19, 10, 21);
                path.cubicTo(16, 23, 12, 17, 16, 16);
                path.cubicTo(24, 16, 24, 3, 12, 3);
                path.close();
                break;
            default:
                path.moveTo(4, 9); path.cubicTo(8, 1, 22, 4, 21, 13);
                path.cubicTo(20, 24, 4, 24, 5, 15);
                path.cubicTo(6, 7, 17, 9, 14, 16);
                paint.setStrokeWidth(2.4f);
        }
        canvas.drawPath(path, paint);
        if (kind.equals("palette")) {
            paint.setStyle(Paint.Style.FILL);
            for (float[] dot : new float[][]{{8, 8}, {14, 7}, {18, 11}}) {
                canvas.drawCircle(dot[0], dot[1], 1.2f, paint);
            }
            paint.setColor(accentColor);
            canvas.drawCircle(8, 15, 3, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(color);
            paint.setStrokeWidth(.8f);
            canvas.drawCircle(8, 15, 3, paint);
        }
        canvas.restoreToCount(saved);
    }
}
