package com.example.notes.activities;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat; // Cho việc tô màu drawable

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory; // Thêm để tải bitmap
import android.graphics.Canvas; // Thêm để vẽ bitmap lên canvas của DrawingView
import android.graphics.Color;
import android.graphics.drawable.Drawable; // Cho việc tô màu drawable
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView; // Cho việc tô màu button
import android.widget.Toast;

import com.example.notes.R;
import com.example.notes.databinding.ActivityDrawingBinding;
import com.example.notes.viewmodels.DrawingView;

import java.io.File;
import java.io.FileInputStream; // Thêm để đọc file
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
    public static final String EXTRA_DRAWING_PATH = "extra_drawing_path"; // Để trả về đường dẫn đã lưu
    public static final String EXTRA_DRAWING_PATH_TO_LOAD = "extra_drawing_path_to_load"; // Để tải bản vẽ có sẵn
    public static final String DRAWINGS_DIR_NAME_STATIC = "drawings"; // Tên thư mục, dùng static để CreateNoteActivity có thể truy cập

    private ActivityDrawingBinding binding;
    private DrawingView drawingView;
    private ActivityResultLauncher<String> requestWritePermissionLauncher;
    private OnBackPressedCallback onBackPressedCallback;
    private String pathToLoad; // Đường dẫn bản vẽ cần tải (nếu có)
    private int selectedColorViewId = R.id.colorPickerBlack; // Giữ ID của view màu đang chọn


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDrawingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        drawingView = binding.drawingView;

        if (getIntent().hasExtra(EXTRA_DRAWING_PATH_TO_LOAD)) {
            pathToLoad = getIntent().getStringExtra(EXTRA_DRAWING_PATH_TO_LOAD);
            loadDrawingFromPath(pathToLoad);
        }


        requestWritePermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        Log.d(TAG, "Write permission granted. Calling actuallySaveDrawing.");
                        actuallySaveDrawing();
                    } else {
                        Toast.makeText(this, R.string.permission_needed_to_save_drawing, Toast.LENGTH_LONG).show();
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                });

        setupDrawingTools();
        setupActionButtons();
        toggleDrawingInstructions();
        setupOnBackPressedHandling();
    }

    private void loadDrawingFromPath(String path) {
        if (path != null && !path.isEmpty()) {
            File drawingFile = new File(path);
            if (drawingFile.exists()) {
                try (InputStream inputStream = new FileInputStream(drawingFile)) {
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    if (bitmap != null) {
                        // Cần một cách để DrawingView có thể vẽ một bitmap ban đầu
                        // Hoặc, nếu DrawingView hỗ trợ, đặt bitmap làm nền và vẽ lên đó
                        // Hiện tại, DrawingView không có phương thức setBitmap,
                        // nên việc "tải" để "chỉnh sửa" sẽ phức tạp hơn.
                        // Cách đơn giản là hiển thị nó trong một ImageView riêng nếu chỉ để xem.
                        // Nếu muốn chỉnh sửa, DrawingView cần được nâng cấp.
                        // Tạm thời, ta có thể coi như bắt đầu vẽ mới, hoặc người dùng phải vẽ lại nếu muốn sửa.
                        // Để giữ nguyên logic hiện tại, ta sẽ không load bitmap vào DrawingView để sửa.
                        // Nếu mục đích là sửa, DrawingView cần phương thức như `loadBitmap(Bitmap b)`
                        // để vẽ bitmap đó lên canvas của nó.
                        Log.d(TAG, "Bitmap loaded from path, but DrawingView needs enhancement to edit it.");
                        // drawingView.loadBitmap(bitmap); // Giả sử có phương thức này
                        // toggleDrawingInstructions(); // Cập nhật sau khi tải
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error loading drawing from path: " + path, e);
                    Toast.makeText(this, "Error loading existing drawing.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }


    private void setupDrawingTools() {
        drawingView.setCurrentColor(Color.BLACK);
        drawingView.setStrokeWidth(8f);
        updateColorButtonSelection(R.id.colorPickerBlack, Color.BLACK);


        binding.colorPickerBlack.setOnClickListener(v -> updateColorButtonSelection(R.id.colorPickerBlack, Color.BLACK));
        binding.colorPickerRed.setOnClickListener(v -> updateColorButtonSelection(R.id.colorPickerRed, Color.RED));
        binding.colorPickerBlue.setOnClickListener(v -> updateColorButtonSelection(R.id.colorPickerBlue, Color.BLUE));
        // Thêm các màu khác nếu có
        // binding.colorPickerGreen.setOnClickListener(v -> updateColorButtonSelection(R.id.colorPickerGreen, Color.GREEN));

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
                    .setIcon(R.drawable.ic_delete) // Cân nhắc dùng icon phù hợp hơn
                    .show();
        });
    }

    private void updateColorButtonSelection(int viewId, int color) {
        // Bỏ chọn view màu cũ
        View oldSelectedView = findViewById(selectedColorViewId);
        if (oldSelectedView != null) {
            // Giả sử bạn muốn thay đổi viền hoặc background để chỉ ra lựa chọn
            // Ví dụ: đặt lại background về mặc định (nếu có)
            oldSelectedView.setBackgroundColor(getOriginalColorForView(selectedColorViewId)); // Cần một cách để lấy màu gốc
            // Hoặc nếu bạn dùng drawable có selector, chỉ cần cập nhật trạng thái
            // oldSelectedView.setSelected(false);
            // Nếu bạn làm viền to hơn cho màu được chọn:
            if (oldSelectedView instanceof ImageView) {
                // ((ImageView) oldSelectedView).setPadding(0,0,0,0); // ví dụ
            } else {
                // oldSelectedView.setElevation(0f); // ví dụ
            }
        }

        // Chọn view màu mới
        View newSelectedView = findViewById(viewId);
        if (newSelectedView != null) {
            // Ví dụ: thay đổi background hoặc thêm viền
            // newSelectedView.setBackgroundColor(Color.LTGRAY); // Hoặc một màu highlight
            // newSelectedView.setSelected(true);
            // Nếu bạn làm viền to hơn cho màu được chọn:
            if (newSelectedView instanceof ImageView) {
                // ((ImageView) newSelectedView).setPadding(5,5,5,5); // ví dụ
            } else {
                // newSelectedView.setElevation(8f); // ví dụ
            }
        }
        drawingView.setCurrentColor(color);
        selectedColorViewId = viewId; // Cập nhật ID của view màu đang được chọn
    }
    // Helper để lấy màu gốc của các view màu (cần thiết nếu bạn thay đổi background của chúng khi chọn)
    private int getOriginalColorForView(int viewId) {
        if (viewId == R.id.colorPickerBlack) return Color.BLACK;
        if (viewId == R.id.colorPickerRed) return Color.RED;
        if (viewId == R.id.colorPickerBlue) return Color.BLUE;
        // if (viewId == R.id.colorPickerGreen) return Color.GREEN;
        return Color.TRANSPARENT; // Mặc định
    }


    private void setupActionButtons() {
        binding.imageDrawingBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        binding.imageDrawingSave.setOnClickListener(v -> {
            Log.d(TAG, getString(R.string.save_button_clicked));
            if (drawingView.hasDrawing()) {
                Log.d(TAG, getString(R.string.drawing_found_proceeding_to_savedrawingandfinish));
                // Kiểm tra quyền trước khi lưu nếu là Pre-Q và chưa có quyền
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                        ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, getString(R.string.requesting_write_external_storage_permission_for_pre_q_device));
                    requestWritePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                } else {
                    actuallySaveDrawing();
                }
            } else {
                Log.w(TAG, getString(R.string.no_drawing_to_save));
                Toast.makeText(this, R.string.nothing_to_save, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void toggleDrawingInstructions() {
        if (binding.textViewDrawingInstructions != null) {
            binding.textViewDrawingInstructions.setVisibility(drawingView.hasDrawing() ? View.GONE : View.VISIBLE);
        }
    }

    // saveDrawingAndFinish không còn cần thiết vì logic quyền được xử lý trong imageDrawingSave.setOnClickListener
    // private void saveDrawingAndFinish() { ... }


    private void actuallySaveDrawing() {
        Bitmap bitmap = drawingView.getBitmap();
        if (bitmap == null || bitmap.isRecycled() || bitmap.getWidth() == 0 || bitmap.getHeight() == 0) {
            Toast.makeText(this, R.string.error_creating_drawing_bitmap, Toast.LENGTH_SHORT).show();
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        String fileName = "NoteDrawing_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + "_" + UUID.randomUUID().toString().substring(0, 6) + ".png";
        String finalSavedPath = null; // Đường dẫn sẽ được trả về (trong internal storage)
        Uri imageUriForMediaStore = null; // URI cho MediaStore (nếu lưu public)
        boolean saveSucceeded = false;

        // 1. Tạo thư mục lưu trữ nội bộ ổn định cho bản vẽ
        File internalDrawingsDir = new File(getFilesDir(), DRAWINGS_DIR_NAME_STATIC);
        if (!internalDrawingsDir.exists()) {
            if (!internalDrawingsDir.mkdirs()) {
                Log.e(TAG, "Failed to create internal drawings directory: " + internalDrawingsDir.getAbsolutePath());
                Toast.makeText(this, getString(R.string.error_creating_directory) + ": " + internalDrawingsDir.getName(), Toast.LENGTH_LONG).show();
                setResult(RESULT_CANCELED);
                finish();
                return;
            }
        }
        File internalFileToSave = new File(internalDrawingsDir, fileName);

        try {
            // 2. Lưu bitmap vào thư mục nội bộ này trước tiên
            try (FileOutputStream internalFos = new FileOutputStream(internalFileToSave)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, internalFos);
                internalFos.flush();
                finalSavedPath = internalFileToSave.getAbsolutePath(); // Đây là đường dẫn chính sẽ được sử dụng bởi ứng dụng
                saveSucceeded = true;
                Log.d(TAG, "Drawing saved to internal primary storage: " + finalSavedPath);
            } catch (IOException e) {
                Log.e(TAG, "Error saving drawing to internal primary storage: " + e.getMessage(), e);
                throw e;
            }

            // 3. (Tùy chọn) Nếu vẫn muốn lưu vào MediaStore cho public visibility (Android Q+)
            // hoặc public Pictures (Pre-Q)
            if (saveSucceeded) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentResolver resolver = getContentResolver();
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                    contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
                    contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + getString(R.string.app_drawing_folder_name));

                    imageUriForMediaStore = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                    if (imageUriForMediaStore != null) {
                        try (OutputStream fosMediaStore = resolver.openOutputStream(imageUriForMediaStore);
                             InputStream fis = new FileInputStream(internalFileToSave)) { // Đọc từ file nội bộ đã lưu
                            if (fosMediaStore != null) { // fis đã được kiểm tra khi mở FileInputStream
                                byte[] buffer = new byte[4096];
                                int bytesRead;
                                while ((bytesRead = fis.read(buffer)) != -1) {
                                    fosMediaStore.write(buffer, 0, bytesRead);
                                }
                                Log.d(TAG, getString(R.string.drawing_saved_to_mediastore) + imageUriForMediaStore.toString());
                            } else {
                                Log.w(TAG, "Failed to open stream for MediaStore copy.");
                                // Xóa entry MediaStore nếu không copy được
                                try { getContentResolver().delete(imageUriForMediaStore, null, null); }
                                catch (Exception ex) { Log.e(TAG, "Error deleting incomplete MediaStore entry."); }
                            }
                        } catch (IOException e_mediastore) {
                            Log.w(TAG, "Error copying drawing to MediaStore: " + e_mediastore.getMessage());
                            if (imageUriForMediaStore != null) {
                                try { getContentResolver().delete(imageUriForMediaStore, null, null); }
                                catch (Exception ex) { Log.e(TAG, "Error deleting incomplete MediaStore entry."); }
                            }
                        }
                    } else {
                        Log.w(TAG, getString(R.string.error_creating_mediastore_entry));
                    }
                } else { // Pre-Q: Lưu vào public Pictures
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                        File publicAppDir = new File(picturesDir, getString(R.string.app_drawing_folder_name));
                        if (!publicAppDir.exists() && !publicAppDir.mkdirs()) {
                            Log.w(TAG, "Cannot create public directory: " + publicAppDir.getAbsolutePath());
                        } else {
                            File publicImageFile = new File(publicAppDir, fileName);
                            try (FileOutputStream fosPublic = new FileOutputStream(publicImageFile);
                                 InputStream fis = new FileInputStream(internalFileToSave)) {
                                byte[] buffer = new byte[4096];
                                int bytesRead;
                                while ((bytesRead = fis.read(buffer)) != -1) {
                                    fosPublic.write(buffer, 0, bytesRead);
                                }
                                Log.d(TAG, getString(R.string.drawing_saved_to_pre_q) + publicImageFile.getAbsolutePath());
                                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                                mediaScanIntent.setData(Uri.fromFile(publicImageFile));
                                sendBroadcast(mediaScanIntent);
                            } catch (IOException e_public) {
                                Log.w(TAG, "Error copying drawing to public storage (Pre-Q): " + e_public.getMessage());
                            }
                        }
                    } else {
                        Log.w(TAG, "WRITE_EXTERNAL_STORAGE permission not granted for Pre-Q public save.");
                    }
                }
            }

            // Trả về kết quả với đường dẫn nội bộ
            if (saveSucceeded && finalSavedPath != null) {
                Toast.makeText(this, R.string.drawing_saved_successfully, Toast.LENGTH_SHORT).show();
                Intent resultIntent = new Intent();
                resultIntent.putExtra(EXTRA_DRAWING_PATH, finalSavedPath);
                setResult(RESULT_OK, resultIntent);
            } else {
                setResult(RESULT_CANCELED); // Lỗi đã được log ở trên
            }

        } catch (IOException e) {
            Log.e(TAG, "Fatal error saving drawing: " + e.getMessage(), e);
            Toast.makeText(this, getString(R.string.error_saving_drawing) + ": " + (e.getMessage() != null ? e.getMessage() : "Unknown IO error"), Toast.LENGTH_LONG).show();
            setResult(RESULT_CANCELED);
            if (internalFileToSave.exists()) { // Dọn dẹp file nội bộ nếu có lỗi
                internalFileToSave.delete();
            }
            if (imageUriForMediaStore != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try { getContentResolver().delete(imageUriForMediaStore, null, null); }
                catch (Exception ex) { Log.e(TAG, getString(R.string.error_deleting_mediastore_entry_on_failure) + ex.getMessage()); }
            }
        } catch (Exception e) { // Bắt các lỗi không mong muốn khác
            Log.e(TAG, getString(R.string.unexpected_error_saving_drawing) + e.getMessage(), e);
            Toast.makeText(this, getString(R.string.error_saving_drawing) + ": " + (e.getMessage() != null ? e.getMessage() : "Unexpected error"), Toast.LENGTH_LONG).show();
            setResult(RESULT_CANCELED);
            if (internalFileToSave.exists()) {
                internalFileToSave.delete();
            }
        } finally {
            finish(); // Luôn finish activity
        }
    }


    private void showUnsavedChangesDialog(final OnBackPressedCallback callback) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.unsaved_drawing_title)
                .setMessage(R.string.confirm_save_drawing)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    // Quyền đã được xử lý trong onClick của nút Save
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        requestWritePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                        // Nếu quyền được cấp, actuallySaveDrawing sẽ được gọi, sau đó finish()
                        // Nếu không, finish() sẽ được gọi từ launcher callback
                    } else {
                        actuallySaveDrawing(); // Sẽ tự finish()
                    }
                })
                .setNegativeButton(R.string.discard_drawing, (dialog, which) -> {
                    setResult(RESULT_CANCELED);
                    callback.setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed(); // Gọi lại để thoát
                })
                .setNeutralButton(R.string.cancel, null)
                .setCancelable(false) // Quan trọng: không cho hủy dialog bằng cách chạm ra ngoài
                .show();
    }

    private void setupOnBackPressedHandling() {
        onBackPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawingView.hasDrawing() && (pathToLoad == null || drawingView.isModifiedSinceLoad())) { // Chỉ hỏi nếu có thay đổi hoặc là bản vẽ mới
                    showUnsavedChangesDialog(this);
                } else {
                    setResult(RESULT_CANCELED); // Không có thay đổi hoặc không có gì để lưu
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed(); // Thoát bình thường
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