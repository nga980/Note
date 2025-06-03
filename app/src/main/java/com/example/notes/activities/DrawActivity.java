package com.example.notes.activities;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class DrawActivity extends AppCompatActivity {

    private static final String TAG = "DrawingActivity";
    public static final String EXTRA_DRAWING_PATH = "extra_drawing_path";

    private ActivityDrawingBinding binding;
    private DrawingView drawingView;
    private ActivityResultLauncher<String> requestWritePermissionLauncher;
    private OnBackPressedCallback onBackPressedCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDrawingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        drawingView = binding.drawingView;

        requestWritePermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        Log.d(TAG, "Write permission granted. Calling actuallySaveDrawing.");
                        actuallySaveDrawing();
                    } else {
                        Toast.makeText(this, R.string.permission_needed_to_save_drawing, Toast.LENGTH_LONG).show();
                        setResult(RESULT_CANCELED);
                        finish(); // <-- SỬA LỖI: Thêm finish() khi quyền bị từ chối
                    }
                });

        setupDrawingTools();
        setupActionButtons();
        toggleDrawingInstructions();
        setupOnBackPressedHandling();
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
                    .setTitle(R.string.clear_drawing_title)
                    .setMessage(R.string.confirm_clear_drawing)
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                        drawingView.clearCanvas();
                        toggleDrawingInstructions();
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .setIcon(R.drawable.ic_delete)
                    .show();
        });
    }

    private void setupActionButtons() {
        binding.imageDrawingBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        binding.imageDrawingSave.setOnClickListener(v -> {
            Log.d(TAG, "Save button clicked."); // Log để kiểm tra
            if (drawingView.hasDrawing()) {
                Log.d(TAG, "Drawing found, proceeding to saveDrawingAndFinish.");
                saveDrawingAndFinish();
            } else {
                Log.w(TAG, "No drawing to save.");
                Toast.makeText(this, R.string.nothing_to_save, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void toggleDrawingInstructions() {
        if (binding.textViewDrawingInstructions != null) {
            binding.textViewDrawingInstructions.setVisibility(drawingView.hasDrawing() ? View.GONE : View.VISIBLE);
        }
    }

    private void saveDrawingAndFinish() {
        if (!drawingView.hasDrawing()) {
            Toast.makeText(this, R.string.nothing_to_save, Toast.LENGTH_SHORT).show();
            return;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Requesting WRITE_EXTERNAL_STORAGE permission for pre-Q device.");
                requestWritePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                return; // Chờ kết quả của yêu cầu quyền
            }
        }
        Log.d(TAG, "Proceeding with actuallySaveDrawing.");
        actuallySaveDrawing();
    }

    private void actuallySaveDrawing() {
        Bitmap bitmap = drawingView.getBitmap();
        if (bitmap == null || bitmap.isRecycled() || bitmap.getWidth() == 0 || bitmap.getHeight() == 0) {
            Toast.makeText(this, R.string.error_creating_drawing_bitmap, Toast.LENGTH_SHORT).show();
            setResult(RESULT_CANCELED);
            finish(); // <-- SỬA LỖI: Thêm finish() khi bitmap không hợp lệ
            return;
        }

        String fileName = "NoteDrawing_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + "_" + UUID.randomUUID().toString().substring(0, 6) + ".png";
        String savedPath = null;
        Uri imageUriForQ = null;
        boolean saveSucceeded = false;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver resolver = getContentResolver();
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
                // Sử dụng tên thư mục đã dịch nếu có, hoặc tên tiếng Anh từ strings.xml
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + getString(R.string.app_drawing_folder_name));

                imageUriForQ = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                if (imageUriForQ != null) {
                    try (OutputStream fos = resolver.openOutputStream(imageUriForQ)) {
                        if (fos != null) {
                            bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);
                            fos.flush();
                            Log.d(TAG, "Drawing saved to MediaStore: " + imageUriForQ.toString());

                            // Sao chép vào thư mục nội bộ để có đường dẫn ổn định
                            File internalAppDir = new File(getFilesDir(), getString(R.string.app_drawing_folder_name_cache));
                            if (!internalAppDir.exists() && !internalAppDir.mkdirs()) {
                                throw new IOException("Failed to create internal directory: " + internalAppDir.getAbsolutePath());
                            }
                            File internalFile = new File(internalAppDir, fileName);
                            try (InputStream inputStream = resolver.openInputStream(imageUriForQ);
                                 FileOutputStream internalFos = new FileOutputStream(internalFile)) {
                                byte[] buffer = new byte[1024];
                                int bytesRead;
                                while ((bytesRead = inputStream.read(buffer)) != -1) {
                                    internalFos.write(buffer, 0, bytesRead);
                                }
                                savedPath = internalFile.getAbsolutePath();
                                saveSucceeded = true;
                                Log.d(TAG, "Drawing copied to internal path: " + savedPath);
                            }
                        } else {
                            throw new IOException(getString(R.string.error_opening_outputstream));
                        }
                    }
                } else {
                    throw new IOException(getString(R.string.error_creating_mediastore_entry));
                }
            } else { // Pre-Q
                File storageDir;
                File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                storageDir = new File(picturesDir, getString(R.string.app_drawing_folder_name));

                if (!storageDir.exists() && !storageDir.mkdirs()) {
                    Log.w(TAG, "Cannot create public directory, falling back to internal: " + storageDir.getAbsolutePath());
                    storageDir = new File(getFilesDir(), getString(R.string.app_drawing_folder_name_cache));
                    if (!storageDir.exists() && !storageDir.mkdirs()) {
                        throw new IOException(getString(R.string.error_creating_directory) + ": " + storageDir.getAbsolutePath());
                    }
                }

                File imageFile = new File(storageDir, fileName);
                try (FileOutputStream fos = new FileOutputStream(imageFile)) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);
                    fos.flush();
                    savedPath = imageFile.getAbsolutePath();
                    saveSucceeded = true;
                    Log.d(TAG, "Drawing saved to (Pre-Q): " + savedPath);

                    // Chỉ quét media nếu lưu vào thư mục public và có thể truy cập
                    if (imageFile.canRead() && Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) && savedPath.contains(picturesDir.getAbsolutePath())) {
                        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                        mediaScanIntent.setData(Uri.fromFile(imageFile));
                        sendBroadcast(mediaScanIntent);
                    }
                }
            }

            if (saveSucceeded && savedPath != null) {
                Toast.makeText(this, R.string.drawing_saved_successfully, Toast.LENGTH_SHORT).show();
                Intent resultIntent = new Intent();
                resultIntent.putExtra(EXTRA_DRAWING_PATH, savedPath);
                setResult(RESULT_OK, resultIntent);
                finish();
            } else if (!saveSucceeded) { // Nếu saveSucceeded vẫn là false
                throw new IOException("Saving failed, path not set.");
            }

        } catch (IOException e) {
            Log.e(TAG, "Error saving drawing: " + e.getMessage(), e);
            Toast.makeText(this, getString(R.string.error_saving_drawing) + ": " + (e.getMessage() != null ? e.getMessage() : "Unknown IO error"), Toast.LENGTH_LONG).show();
            setResult(RESULT_CANCELED);
            if (imageUriForQ != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    getContentResolver().delete(imageUriForQ, null, null);
                } catch (Exception ex) {
                    Log.e(TAG, "Error deleting MediaStore entry on failure: " + ex.getMessage());
                }
            }
            finish(); // <-- SỬA LỖI: Thêm finish() khi có IOException
        } catch (Exception e) { // Bắt các lỗi không mong muốn khác
            Log.e(TAG, "Unexpected error saving drawing: " + e.getMessage(), e);
            Toast.makeText(this, getString(R.string.error_saving_drawing) + ": " + (e.getMessage() != null ? e.getMessage() : "Unexpected error"), Toast.LENGTH_LONG).show();
            setResult(RESULT_CANCELED);
            finish(); // <-- SỬA LỖI: Thêm finish() khi có lỗi khác
        }
    }


    private void showUnsavedChangesDialog(final OnBackPressedCallback callback) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.unsaved_drawing_title)
                .setMessage(R.string.confirm_save_drawing)
                .setPositiveButton(R.string.save, (dialog, which) -> saveDrawingAndFinish())
                .setNegativeButton(R.string.discard_drawing, (dialog, which) -> {
                    setResult(RESULT_CANCELED);
                    callback.setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                })
                .setNeutralButton(R.string.cancel, null)
                .setCancelable(false)
                .show();
    }

    private void setupOnBackPressedHandling() {
        onBackPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawingView.hasDrawing()) {
                    showUnsavedChangesDialog(this);
                } else {
                    setResult(RESULT_CANCELED);
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (onBackPressedCallback != null) {
            onBackPressedCallback.remove();
        }
    }
}