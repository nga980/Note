package com.example.notes.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.notes.R;
import com.example.notes.databinding.ActivityDrawingBinding;
import com.example.notes.viewmodels.DrawingView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class DrawActivity extends AppCompatActivity {

    private static final String TAG = "DrawingActivity";
    public static final String EXTRA_DRAWING_PATH = "extra_drawing_path";
    // Bỏ REQUEST_CODE_WRITE_STORAGE_PERMISSION vì sẽ dùng ActivityResultLauncher

    private ActivityDrawingBinding binding;
    private DrawingView drawingView;

    // ActivityResultLauncher để xin quyền
    private ActivityResultLauncher<String> requestWritePermissionLauncher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDrawingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        drawingView = binding.drawingView;

        // Khởi tạo requestWritePermissionLauncher
        requestWritePermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        actuallySaveDrawing(); // Quyền đã được cấp, tiến hành lưu
                    } else {
                        Toast.makeText(this, "Cần quyền ghi để lưu bản vẽ.", Toast.LENGTH_LONG).show();
                        setResult(RESULT_CANCELED);
                        // Không finish() ở đây để người dùng có thể thử lại hoặc thoát
                    }
                });

        setupDrawingTools();
        setupActionButtons();
        toggleDrawingInstructions();
    }

    private void setupDrawingTools() {
        drawingView.setCurrentColor(Color.BLACK);
        drawingView.setStrokeWidth(8f);

        binding.colorPickerBlack.setOnClickListener(v -> drawingView.setCurrentColor(Color.BLACK));
        binding.colorPickerRed.setOnClickListener(v -> drawingView.setCurrentColor(Color.RED));
        binding.colorPickerBlue.setOnClickListener(v -> drawingView.setCurrentColor(Color.BLUE));

        binding.buttonUndo.setOnClickListener(v -> {
            drawingView.undo();
            toggleDrawingInstructions();
        });
        binding.buttonClearCanvas.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.clear_drawing_title) // Sử dụng string resource
                    .setMessage(R.string.confirm_clear_drawing) // Sử dụng string resource
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                        drawingView.clearCanvas();
                        toggleDrawingInstructions();
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .show();
        });
    }
    // Thêm các string resource: clear_drawing_title, confirm_clear_drawing

    private void setupActionButtons() {
        binding.imageDrawingBack.setOnClickListener(v -> {
            if (drawingView.hasDrawing()) {
                showUnsavedChangesDialog();
            } else {
                setResult(RESULT_CANCELED);
                finish();
            }
        });

        binding.imageDrawingSave.setOnClickListener(v -> {
            if (drawingView.hasDrawing()) {
                saveDrawingAndFinish();
            } else {
                Toast.makeText(this, R.string.nothing_to_save, Toast.LENGTH_SHORT).show(); // Sử dụng string resource
                setResult(RESULT_CANCELED);
                finish();
            }
        });
    }
    // Thêm string resource: nothing_to_save

    private void toggleDrawingInstructions() {
        if (binding.textViewDrawingInstructions != null) {
            binding.textViewDrawingInstructions.setVisibility(drawingView.hasDrawing() ? View.GONE : View.VISIBLE);
        }
    }

    private void saveDrawingAndFinish() {
        // Với Android 10 (API 29) trở lên, không cần xin quyền WRITE_EXTERNAL_STORAGE
        // để ghi vào MediaStore hoặc thư mục dành riêng cho ứng dụng.
        // Với Android < 10, nếu ghi vào bộ nhớ ngoài công cộng, chúng ta cần quyền.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                // Xin quyền bằng ActivityResultLauncher
                requestWritePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                return; // Việc lưu sẽ được xử lý sau khi có kết quả quyền
            }
        }
        // Nếu quyền đã được cấp (cho < Q) hoặc không cần quyền (>= Q)
        actuallySaveDrawing();
    }

    private void actuallySaveDrawing() {
        Bitmap bitmap = drawingView.getBitmap();
        if (bitmap == null) {
            Toast.makeText(this, R.string.error_creating_drawing_bitmap, Toast.LENGTH_SHORT).show(); // Sử dụng string resource
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        String fileName = "NoteDrawing_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + "_" + UUID.randomUUID().toString().substring(0, 6) + ".png";
        String savedPath = null; // Sẽ lưu Uri.toString() hoặc đường dẫn file tuyệt đối

        OutputStream fos = null;
        Uri imageUri = null;
        File imageFileForBelowQ = null;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver resolver = getContentResolver();
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "MyNotesDrawings");

                imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                if (imageUri != null) {
                    fos = resolver.openOutputStream(imageUri);
                    savedPath = imageUri.toString(); // Lưu Uri dưới dạng string
                } else {
                    throw new IOException("Không thể tạo MediaStore entry.");
                }
            } else {
                // Lưu vào thư mục public Pictures cho Android < Q
                // Hoặc thư mục internal của ứng dụng nếu muốn an toàn hơn và không cần quyền
                File storageDir = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES), "MyNotesDrawings");

                if (!storageDir.exists() && !storageDir.mkdirs()) {
                    Log.e(TAG, "Không thể tạo thư mục: " + storageDir.getAbsolutePath());
                    // Thay thế: Lưu vào thư mục cache nội bộ của ứng dụng
                    storageDir = new File(getCacheDir(), "MyNotesDrawings");
                    if (!storageDir.exists()) storageDir.mkdirs(); // Thử tạo lại
                }
                imageFileForBelowQ = new File(storageDir, fileName);
                fos = new FileOutputStream(imageFileForBelowQ);
                savedPath = imageFileForBelowQ.getAbsolutePath();
            }

            if (fos != null) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                Toast.makeText(this, R.string.drawing_saved, Toast.LENGTH_SHORT).show(); // Sử dụng string resource

                Intent resultIntent = new Intent();
                resultIntent.putExtra(EXTRA_DRAWING_PATH, savedPath);
                setResult(RESULT_OK, resultIntent);
            } else {
                throw new IOException("Không thể mở OutputStream để lưu ảnh.");
            }

        } catch (IOException e) {
            Log.e(TAG, "Lỗi khi lưu bản vẽ: " + e.getMessage(), e);
            Toast.makeText(this, getString(R.string.error_saving_drawing) + ": " + e.getMessage(), Toast.LENGTH_LONG).show(); // Sử dụng string resource
            setResult(RESULT_CANCELED);
            // Nếu có lỗi, xóa entry MediaStore (nếu đã tạo)
            if (imageUri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    getContentResolver().delete(imageUri, null, null);
                } catch (Exception ex) {
                    Log.e(TAG, "Lỗi khi xóa MediaStore entry không thành công: " + ex.getMessage());
                }
            }
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    Log.e(TAG, "Lỗi khi đóng FileOutputStream: " + e.getMessage());
                }
            }
            // Chỉ quét file nếu lưu vào bộ nhớ ngoài cho Android < Q và không dùng MediaStore
            if (imageFileForBelowQ != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && imageFileForBelowQ.exists()) {
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                mediaScanIntent.setData(Uri.fromFile(imageFileForBelowQ));
                sendBroadcast(mediaScanIntent);
            }
            finish();
        }
    }
    // Thêm string resource: error_creating_drawing_bitmap, drawing_saved, error_saving_drawing


    // Bỏ qua onRequestPermissionsResult vì đã dùng ActivityResultLauncher
    // @Override
    // public void onRequestPermissionsResult(...)

    private void showUnsavedChangesDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.unsaved_drawing_title) // Sử dụng string resource
                .setMessage(R.string.confirm_save_drawing) // Sử dụng string resource
                .setPositiveButton(R.string.save, (dialog, which) -> saveDrawingAndFinish()) // Sử dụng string resource
                .setNegativeButton(R.string.discard_drawing, (dialog, which) -> { // Sử dụng string resource
                    setResult(RESULT_CANCELED);
                    finish();
                })
                .setNeutralButton(R.string.cancel, null) // Sử dụng string resource
                .show();
    }
    // Thêm string resource: unsaved_drawing_title, confirm_save_drawing, save, discard_drawing, cancel

    // Phương thức getPathFromUri không còn thực sự cần thiết nếu bạn làm việc với Uri
    // và chỉ trả về Uri.toString() hoặc đường dẫn bạn đã tạo.
    // Nếu CreateNoteActivity cần path tuyệt đối, nó có thể thử tự chuyển đổi từ Uri nếu cần,
    // nhưng tốt nhất là CreateNoteActivity cũng làm việc với Uri.
    // Tạm thời bỏ phương thức này để khuyến khích làm việc với Uri.
    /*
    private String getPathFromUri(Uri contentUri) { ... }
    */


    @Override
    public void onBackPressed() { // Sử dụng onBackPressed() đã được un-deprecated
        if (drawingView.hasDrawing()) {
            showUnsavedChangesDialog();
        } else {
            setResult(RESULT_CANCELED);
            super.onBackPressed(); // Gọi super để xử lý back mặc định
        }
    }
}