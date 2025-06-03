package com.example.notes.viewmodels;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff; // Thêm cho việc clear canvas khi load bitmap
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class DrawingView extends View {

    private Path currentDrawingPath;
    private Paint currentDrawingPaint;
    private List<CompletedPath> completedPaths = new ArrayList<>();
    private Bitmap loadedBitmap = null; // Bitmap được tải lên
    private Paint bitmapPaint; // Paint để vẽ loadedBitmap

    private int currentColor = Color.BLACK;
    private float currentStrokeWidth = 8f;
    private boolean isModified = false; // Cờ theo dõi thay đổi

    // Lớp nội bộ để lưu một Path đã hoàn thành cùng với Paint của nó
    private static class CompletedPath {
        Path path;
        Paint paint;

        CompletedPath(Path path, Paint paint) {
            this.path = new Path(path); // Luôn tạo bản sao của Path
            this.paint = new Paint(paint); // Luôn tạo bản sao của Paint
        }
    }

    public DrawingView(Context context) {
        this(context, null);
    }

    public DrawingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // currentDrawingPath và currentDrawingPaint sẽ được khởi tạo khi bắt đầu vẽ (ACTION_DOWN)
        bitmapPaint = new Paint(Paint.DITHER_FLAG);
    }

    public void setCurrentColor(int color) {
        currentColor = color;
        if (currentDrawingPaint != null) {
            currentDrawingPaint.setColor(currentColor);
        }
    }

    public void setStrokeWidth(float width) {
        currentStrokeWidth = width;
        if (currentDrawingPaint != null) {
            currentDrawingPaint.setStrokeWidth(currentStrokeWidth);
        }
    }

    public void undo() {
        if (!completedPaths.isEmpty()) {
            completedPaths.remove(completedPaths.size() - 1);
            isModified = true;
            invalidate();
        }
    }

    public void clearCanvas() {
        completedPaths.clear();
        if (currentDrawingPath != null) {
            currentDrawingPath.reset();
        }
        loadedBitmap = null; // Xóa cả bitmap đã tải nếu có
        isModified = true; // Coi việc xóa là một thay đổi
        invalidate();
    }

    /**
     * Tải một bitmap lên canvas để chỉnh sửa hoặc làm nền.
     * @param bitmap Bitmap để tải.
     */
    public void loadBitmap(Bitmap bitmap) {
        if (bitmap != null) {
            // Tạo một bản sao có thể thay đổi của bitmap để tránh các vấn đề về immutable bitmap
            this.loadedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            // Xóa các đường vẽ cũ nếu có
            completedPaths.clear();
            if (currentDrawingPath != null) {
                currentDrawingPath.reset();
            }
            isModified = false; // Reset cờ modified khi tải bitmap mới
            invalidate();
        }
    }


    public Bitmap getBitmap() {
        // Tạo bitmap kết quả với kích thước của View
        Bitmap resultBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas resultCanvas = new Canvas(resultBitmap);
        // Vẽ nền trắng trước
        resultCanvas.drawColor(Color.WHITE);

        // Vẽ bitmap đã tải (nếu có) làm lớp dưới cùng
        if (loadedBitmap != null && !loadedBitmap.isRecycled()) {
            resultCanvas.drawBitmap(loadedBitmap, 0, 0, bitmapPaint);
        }

        // Vẽ các đường đã hoàn thành lên trên
        for (CompletedPath cp : completedPaths) {
            resultCanvas.drawPath(cp.path, cp.paint);
        }
        // Không vẽ currentDrawingPath vì nó chưa hoàn thành (chưa ACTION_UP)
        return resultBitmap;
    }


    public boolean hasDrawing() {
        // Coi là có bản vẽ nếu có bitmap đã tải hoặc có các đường vẽ
        return loadedBitmap != null || !completedPaths.isEmpty();
    }

    public boolean isModifiedSinceLoad() {
        return isModified;
    }

    public void resetModifiedFlag() {
        isModified = false;
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Vẽ bitmap đã tải (nếu có) trước
        if (loadedBitmap != null && !loadedBitmap.isRecycled()) {
            canvas.drawBitmap(loadedBitmap, 0, 0, bitmapPaint);
        }

        // Sau đó vẽ các đường đã hoàn thành
        for (CompletedPath cp : completedPaths) {
            canvas.drawPath(cp.path, cp.paint);
        }

        // Cuối cùng, vẽ nét hiện tại đang được người dùng kéo (nếu có)
        if (currentDrawingPath != null && currentDrawingPaint != null) {
            canvas.drawPath(currentDrawingPath, currentDrawingPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float touchX = event.getX();
        float touchY = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                currentDrawingPath = new Path();
                currentDrawingPath.moveTo(touchX, touchY);

                currentDrawingPaint = new Paint();
                currentDrawingPaint.setAntiAlias(true);
                currentDrawingPaint.setColor(currentColor);
                currentDrawingPaint.setStyle(Paint.Style.STROKE);
                currentDrawingPaint.setStrokeJoin(Paint.Join.ROUND);
                currentDrawingPaint.setStrokeCap(Paint.Cap.ROUND);
                currentDrawingPaint.setStrokeWidth(currentStrokeWidth);

                isModified = true; // Bất kỳ hành động chạm nào cũng coi là sửa đổi
                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                if (currentDrawingPath != null) {
                    currentDrawingPath.lineTo(touchX, touchY);
                    isModified = true;
                }
                break;

            case MotionEvent.ACTION_UP:
                if (currentDrawingPath != null && currentDrawingPaint != null) {
                    completedPaths.add(new CompletedPath(currentDrawingPath, currentDrawingPaint));
                    currentDrawingPath.reset(); // Reset cho nét vẽ tiếp theo
                    // currentDrawingPath = null; // Hoặc có thể đặt là null
                    // currentDrawingPaint = null;
                    isModified = true;
                }
                break;

            default:
                return false;
        }
        invalidate();
        return true;
    }
}