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

    private Path currentPath;
    private Paint currentPaint;
    private List<Path> paths = new ArrayList<>();
    private List<Paint> paints = new ArrayList<>(); // Lưu trữ Paint cho mỗi Path

    private int currentColor = Color.BLACK;
    private float currentStrokeWidth = 8f; // Độ dày nét vẽ mặc định

    public DrawingView(Context context) {
        this(context, null);
    }

    public DrawingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        currentPath = new Path();
        currentPaint = new Paint();
        currentPaint.setAntiAlias(true);
        currentPaint.setColor(currentColor);
        currentPaint.setStyle(Paint.Style.STROKE);
        currentPaint.setStrokeJoin(Paint.Join.ROUND);
        currentPaint.setStrokeCap(Paint.Cap.ROUND);
        currentPaint.setStrokeWidth(currentStrokeWidth);
    }

    public void setCurrentColor(int color) {
        currentColor = color;
        // Tạo một Paint mới cho nét vẽ tiếp theo nếu muốn mỗi nét có màu riêng
        // Hoặc cập nhật currentPaint nếu muốn tất cả các nét sau này có màu mới
        currentPaint.setColor(currentColor);
    }

    public void setStrokeWidth(float width) {
        currentStrokeWidth = width;
        currentPaint.setStrokeWidth(currentStrokeWidth);
    }

    public void undo() {
        if (!paths.isEmpty()) {
            paths.remove(paths.size() - 1);
            paints.remove(paints.size() - 1);
            invalidate(); // Vẽ lại view
        }
    }

    public void clearCanvas() {
        paths.clear();
        paints.clear();
        currentPath.reset(); // Xóa cả đường dẫn đang vẽ dở
        invalidate();
    }

    public Bitmap getBitmap() {
        // Tạo Bitmap với kích thước của View
        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        // Vẽ nền trắng cho bitmap (tùy chọn, nếu không bản vẽ sẽ có nền trong suốt)
        canvas.drawColor(Color.WHITE);
        // Vẽ lại tất cả các path đã lưu lên canvas của bitmap
        for (int i = 0; i < paths.size(); i++) {
            canvas.drawPath(paths.get(i), paints.get(i));
        }
        // Vẽ cả path hiện tại nếu có (mặc dù thường là đã được add vào paths khi ACTION_UP)
        // canvas.drawPath(currentPath, currentPaint); // Cân nhắc nếu cần
        return bitmap;
    }

    public boolean hasDrawing() {
        return !paths.isEmpty();
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // Vẽ các đường đã hoàn thành
        for (int i = 0; i < paths.size(); i++) {
            canvas.drawPath(paths.get(i), paints.get(i));
        }
        // Vẽ đường hiện tại đang được tay người dùng kéo
        canvas.drawPath(currentPath, currentPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float touchX = event.getX();
        float touchY = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                currentPath.reset(); // Bắt đầu một đường mới
                currentPath.moveTo(touchX, touchY);
                // Tạo một Paint object mới cho path này, dựa trên cài đặt hiện tại
                Paint newPaint = new Paint(currentPaint); // Sao chép cài đặt hiện tại
                paints.add(newPaint);
                paths.add(currentPath); // Thêm path hiện tại vào danh sách để nó được vẽ ngay
                return true;

            case MotionEvent.ACTION_MOVE:
                currentPath.lineTo(touchX, touchY);
                break;

            case MotionEvent.ACTION_UP:
                // Path đã được thêm vào paths ở ACTION_DOWN
                // currentPath sẽ được reset ở ACTION_DOWN tiếp theo
                break;

            default:
                return false;
        }
        invalidate(); // Yêu cầu View vẽ lại
        return true;
    }
}
