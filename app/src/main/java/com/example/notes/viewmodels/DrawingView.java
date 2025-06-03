package com.example.notes.viewmodels;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class DrawingView extends View {

    private Path currentDrawingPath; // Đổi tên để rõ ràng hơn
    private Paint currentDrawingPaint; // Bút vẽ cho nét hiện tại

    // Danh sách để lưu các nét vẽ đã hoàn thành và thuộc tính của chúng
    private List<CompletedPath> completedPaths = new ArrayList<>();

    private int currentColor = Color.BLACK;
    private float currentStrokeWidth = 8f;

    // Lớp nội bộ để lưu một Path đã hoàn thành cùng với Paint của nó
    private static class CompletedPath {
        Path path;
        Paint paint;

        CompletedPath(Path path, Paint paint) {
            this.path = path; // Lưu một bản sao của Path
            this.paint = new Paint(paint); // Lưu một bản sao của Paint
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
    }

    public void setCurrentColor(int color) {
        currentColor = color;
        // Nếu đang vẽ dở, currentDrawingPaint sẽ được cập nhật.
        // Nếu không, màu này sẽ được dùng cho nét vẽ tiếp theo.
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
            invalidate();
        }
    }

    public void clearCanvas() {
        completedPaths.clear();
        if (currentDrawingPath != null) {
            currentDrawingPath.reset(); // Xóa cả đường dẫn đang vẽ dở nếu có
        }
        invalidate();
    }

    public Bitmap getBitmap() {
        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE); // Nền trắng cho bitmap
        for (CompletedPath cp : completedPaths) {
            canvas.drawPath(cp.path, cp.paint);
        }
        // Không cần vẽ currentDrawingPath ở đây vì nó chưa hoàn thành (chưa ACTION_UP)
        return bitmap;
    }

    public boolean hasDrawing() {
        return !completedPaths.isEmpty();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (CompletedPath cp : completedPaths) {
            canvas.drawPath(cp.path, cp.paint);
        }
        // Vẽ nét hiện tại đang được người dùng kéo (nếu có)
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
                currentDrawingPath = new Path(); // Tạo Path MỚI cho mỗi nét vẽ
                currentDrawingPath.moveTo(touchX, touchY);

                currentDrawingPaint = new Paint(); // Tạo Paint MỚI cho nét vẽ này
                currentDrawingPaint.setAntiAlias(true);
                currentDrawingPaint.setColor(currentColor); // Lấy màu hiện tại
                currentDrawingPaint.setStyle(Paint.Style.STROKE);
                currentDrawingPaint.setStrokeJoin(Paint.Join.ROUND);
                currentDrawingPaint.setStrokeCap(Paint.Cap.ROUND);
                currentDrawingPaint.setStrokeWidth(currentStrokeWidth); // Lấy độ dày nét hiện tại
                // Không thêm vào completedPaths ngay, chỉ thêm khi ACTION_UP
                invalidate(); // Yêu cầu vẽ lại để thấy điểm bắt đầu
                return true;

            case MotionEvent.ACTION_MOVE:
                if (currentDrawingPath != null) {
                    currentDrawingPath.lineTo(touchX, touchY);
                }
                break;

            case MotionEvent.ACTION_UP:
                if (currentDrawingPath != null && currentDrawingPaint != null) {
                    // Khi nhấc tay lên, lưu nét vẽ hiện tại vào danh sách các nét đã hoàn thành
                    // Quan trọng: tạo một đối tượng Path mới để lưu trữ, không phải tham chiếu đến currentDrawingPath
                    completedPaths.add(new CompletedPath(new Path(currentDrawingPath), currentDrawingPaint));
                    currentDrawingPath.reset(); // Sẵn sàng cho nét vẽ tiếp theo (hoặc có thể reset ở ACTION_DOWN)
                    // Hoặc đặt currentDrawingPath = null; currentDrawingPaint = null; để chỉ vẽ khi chạm xuống
                }
                break;

            default:
                return false;
        }
        invalidate();
        return true;
    }
}