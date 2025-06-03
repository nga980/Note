package com.example.notes.activities;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;
import androidx.lifecycle.ViewModelProvider;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
// import android.database.Cursor; // Không dùng
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore; // Không còn dùng trực tiếp trong getPathFromUri
import android.provider.Settings;
import android.text.Editable;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.CharacterStyle;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.notes.R;
import com.example.notes.databinding.ActivityCreateNoteBinding;
import com.example.notes.databinding.LayoutMiscellaneousBinding;
import com.example.notes.entities.Note;
import com.example.notes.viewmodels.NoteViewModel;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream; // Đảm bảo import này
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID; // Thêm để tạo tên file duy nhất hơn

public class CreateNoteActivity extends AppCompatActivity {

    private static final String TAG = "CreateNoteActivity";
    public static final String IMAGES_DIR_NAME = "images"; // Tên thư mục lưu ảnh nội bộ

    private ActivityCreateNoteBinding binding;
    private String selectedNoteColor;
    private String selectedImagePath; // Sẽ lưu đường dẫn đến file trong getFilesDir()/images
    private String selectedDrawingPath; // Sẽ lưu đường dẫn đến file trong getFilesDir()/drawings
    private AlertDialog dialogAddURL;
    private AlertDialog dialogDeleteNote;
    private Note alreadyAvailableNote;
    private NoteViewModel noteViewModel;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<String> pickImageLauncher;
    private ActivityResultLauncher<Intent> drawingActivityLauncher;
    private String initialTitle = "";
    private String initialSubtitle = "";
    private String initialNoteText = "";
    private String initialColor = "";
    private String initialImagePath = "";
    private String initialWebLink = "";
    private String initialDrawingPath = "";
    private static final String DEFAULT_NOTE_COLOR = "#333333";
    private boolean isBoldActive = false;
    private boolean isItalicActive = false;
    private boolean isUnderlineActive = false;
    private boolean isFormattingText = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable updateButtonStateRunnable = this::updateButtonStatesBasedOnCursor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateNoteBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        noteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);

        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                actualLaunchImagePicker();
            } else {
                // Kiểm tra xem người dùng có chọn "Don't ask again" không
                String permissionNeeded = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                        Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;
                if (!shouldShowRequestPermissionRationale(permissionNeeded)) {
                    new AlertDialog.Builder(this)
                            .setMessage(R.string.permission_denied_permanently)
                            .setPositiveButton(R.string.go_to_settings, (dialog, which) -> {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                intent.setData(Uri.parse("package:" + getPackageName()));
                                startActivity(intent);
                            })
                            .setNegativeButton(R.string.cancel, null)
                            .show();
                } else {
                    Toast.makeText(this, R.string.permission_denied_permanently, Toast.LENGTH_SHORT).show();
                }
            }
        });

        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                selectedImagePath = copyFileToInternalStorage(uri, IMAGES_DIR_NAME); // Lưu vào internal storage
                if (selectedImagePath != null && !selectedImagePath.isEmpty()) {
                    try {
                        // Hiển thị ảnh từ đường dẫn nội bộ đã lưu
                        binding.imageNote.setImageURI(Uri.fromFile(new File(selectedImagePath)));
                        binding.imageNote.setVisibility(View.VISIBLE);
                        binding.imageRemoveImage.setVisibility(View.VISIBLE);
                    } catch (Exception e) {
                        Toast.makeText(this, getString(R.string.failed_to_load_image) + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, getString(R.string.error_decoding_image_stream) + e.getMessage(), e);
                        selectedImagePath = ""; // Reset nếu lỗi
                        binding.imageNote.setVisibility(View.GONE);
                        binding.imageRemoveImage.setVisibility(View.GONE);
                    }
                } else {
                    Toast.makeText(this, getString(R.string.failed_to_load_image) + " (Could not copy to internal storage)", Toast.LENGTH_SHORT).show();
                    binding.imageNote.setVisibility(View.GONE);
                    binding.imageRemoveImage.setVisibility(View.GONE);
                }
            }
        });

        drawingActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        String path = result.getData().getStringExtra(DrawActivity.EXTRA_DRAWING_PATH);
                        if (path != null && !path.isEmpty()) {
                            File drawingFile = new File(path);
                            if (drawingFile.exists()) {
                                selectedDrawingPath = path; // DrawActivity đã trả về đường dẫn nội bộ
                                if (binding.imageDrawing != null) {
                                    binding.imageDrawing.setImageURI(Uri.fromFile(drawingFile));
                                    binding.imageDrawing.setVisibility(View.VISIBLE);
                                    if (binding.imageRemoveDrawing != null) {
                                        binding.imageRemoveDrawing.setVisibility(View.VISIBLE);
                                    }
                                }
                            } else {
                                Toast.makeText(this, R.string.drawing_file_not_found, Toast.LENGTH_SHORT).show();
                                Log.e(TAG, getString(R.string.drawing_file_does_not_exist) + path);
                                selectedDrawingPath = ""; // Reset nếu file không tồn tại
                            }
                        }
                    }
                }
        );

        binding.imageBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        binding.textDateTime.setText(
                new SimpleDateFormat("EEEE, dd MMMM yyyy HH:mm a", Locale.getDefault()) // Sử dụng yyyy thay cho GGGG
                        .format(new Date())
        );

        binding.imageSave.setOnClickListener(v -> saveNote());

        selectedNoteColor = DEFAULT_NOTE_COLOR;
        selectedImagePath = "";
        selectedDrawingPath = "";

        if (getIntent().getBooleanExtra("isViewOrUpdate", false)) {
            alreadyAvailableNote = (Note) getIntent().getSerializableExtra("note");
            setViewOrUpdateNote();
        }
        // Gọi captureInitialNoteState sau khi setViewOrUpdateNote hoặc sau giá trị mặc định
        captureInitialNoteState();


        if (getIntent().hasExtra("quick_action_type")) {
            String type = getIntent().getStringExtra("quick_action_type");
            if ("image".equals(type)) {
                selectImage();
            } else if ("url".equals(type)) {
                showAddURLDialog();
            } else if ("drawing".equals(type)) {
                Intent drawingIntent = new Intent(getApplicationContext(), DrawActivity.class);
                drawingActivityLauncher.launch(drawingIntent);
            }
        }

        initMiscellaneous();
        setSubtitleIndicatorColor();

        if (binding.imageRemoveDrawing != null) {
            binding.imageRemoveDrawing.setOnClickListener(v -> {
                deleteFileFromInternalStorage(selectedDrawingPath); // Xóa file nếu có
                selectedDrawingPath = "";
                binding.imageDrawing.setVisibility(View.GONE);
                binding.imageRemoveDrawing.setVisibility(View.GONE);
                binding.imageDrawing.setImageBitmap(null);
            });
        }
        binding.inputNote.post(this::scheduleButtonStateUpdate);


        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (hasUnsavedChanges()) {
                    new AlertDialog.Builder(CreateNoteActivity.this)
                            .setTitle(R.string.unsaved_changes)
                            .setMessage(R.string.confirm_discard_changes)
                            .setPositiveButton(R.string.discard, (dialog, which) -> {
                                setEnabled(false);
                                getOnBackPressedDispatcher().onBackPressed();
                            })
                            .setNegativeButton(R.string.keep_editing, (dialog, which) -> dialog.dismiss())
                            .show();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void captureInitialNoteState() {
        if (alreadyAvailableNote != null) {
            initialTitle = alreadyAvailableNote.getTitle() != null ? alreadyAvailableNote.getTitle().trim() : "";
            initialSubtitle = alreadyAvailableNote.getSubtitle() != null ? alreadyAvailableNote.getSubtitle().trim() : "";
            if (alreadyAvailableNote.getNoteText() != null && !alreadyAvailableNote.getNoteText().trim().isEmpty()) {
                Spanned spannedInitialText = HtmlCompat.fromHtml(alreadyAvailableNote.getNoteText(), HtmlCompat.FROM_HTML_MODE_LEGACY);
                initialNoteText = spannedInitialText.toString().trim();
            } else {
                initialNoteText = "";
            }
            initialColor = alreadyAvailableNote.getColor() != null ? alreadyAvailableNote.getColor() : DEFAULT_NOTE_COLOR;
            initialImagePath = alreadyAvailableNote.getImagePath() != null ? alreadyAvailableNote.getImagePath() : "";
            initialWebLink = alreadyAvailableNote.getWebLink() != null ? alreadyAvailableNote.getWebLink().trim() : "";
            initialDrawingPath = alreadyAvailableNote.getDrawingPath() != null ? alreadyAvailableNote.getDrawingPath() : "";
        } else {
            initialTitle = binding.inputNoteTitle.getText().toString().trim();
            initialSubtitle = binding.inputNoteSubtitle.getText().toString().trim();
            Editable currentEditable = binding.inputNote.getText();
            initialNoteText = currentEditable != null ? currentEditable.toString().trim() : "";
            initialColor = selectedNoteColor;
            initialImagePath = selectedImagePath;
            initialWebLink = (binding.layoutWebURL.getVisibility() == View.VISIBLE) ? binding.textWebURL.getText().toString().trim() : "";
            initialDrawingPath = selectedDrawingPath;
        }
    }

    private boolean hasUnsavedChanges() {
        String currentTitle = binding.inputNoteTitle.getText().toString().trim();
        String currentSubtitle = binding.inputNoteSubtitle.getText().toString().trim();
        Editable currentEditable = binding.inputNote.getText();
        String currentNoteText = currentEditable != null ? currentEditable.toString().trim() : "";
        String currentWebLink = binding.layoutWebURL.getVisibility() == View.VISIBLE ? binding.textWebURL.getText().toString().trim() : "";

        return !Objects.equals(currentTitle, initialTitle) ||
                !Objects.equals(currentSubtitle, initialSubtitle) ||
                !Objects.equals(currentNoteText, initialNoteText) ||
                !Objects.equals(selectedNoteColor, initialColor) ||
                !Objects.equals(selectedImagePath, initialImagePath) ||
                !Objects.equals(currentWebLink, initialWebLink) ||
                !Objects.equals(selectedDrawingPath, initialDrawingPath);
    }

    private void setViewOrUpdateNote() {
        if (alreadyAvailableNote == null) return;

        binding.inputNoteTitle.setText(alreadyAvailableNote.getTitle());
        binding.inputNoteSubtitle.setText(alreadyAvailableNote.getSubtitle());
        binding.textDateTime.setText(alreadyAvailableNote.getDateTime());

        if (alreadyAvailableNote.getNoteText() != null && !alreadyAvailableNote.getNoteText().trim().isEmpty()) {
            Spanned spannedText = HtmlCompat.fromHtml(alreadyAvailableNote.getNoteText(), HtmlCompat.FROM_HTML_MODE_LEGACY);
            binding.inputNote.setText(spannedText);
        } else {
            binding.inputNote.setText("");
        }

        selectedImagePath = alreadyAvailableNote.getImagePath(); // Gán giá trị ban đầu
        if (selectedImagePath != null && !selectedImagePath.trim().isEmpty()) {
            File imageFile = new File(selectedImagePath);
            if (imageFile.exists()) {
                binding.imageNote.setImageURI(Uri.fromFile(imageFile));
                binding.imageNote.setVisibility(View.VISIBLE);
                binding.imageRemoveImage.setVisibility(View.VISIBLE);
            } else {
                selectedImagePath = ""; // Reset nếu file không tồn tại
                binding.imageNote.setVisibility(View.GONE);
                binding.imageRemoveImage.setVisibility(View.GONE);
                Log.e(TAG, getString(R.string.image_file_not_found) + alreadyAvailableNote.getImagePath());
            }
        } else {
            binding.imageNote.setVisibility(View.GONE);
            binding.imageRemoveImage.setVisibility(View.GONE);
        }

        selectedDrawingPath = alreadyAvailableNote.getDrawingPath(); // Gán giá trị ban đầu
        if (selectedDrawingPath != null && !selectedDrawingPath.trim().isEmpty()) {
            File drawingFile = new File(selectedDrawingPath);
            if (drawingFile.exists()) {
                if (binding.imageDrawing != null) {
                    binding.imageDrawing.setImageURI(Uri.fromFile(drawingFile));
                    binding.imageDrawing.setVisibility(View.VISIBLE);
                    if (binding.imageRemoveDrawing != null) {
                        binding.imageRemoveDrawing.setVisibility(View.VISIBLE);
                    }
                }
            } else {
                selectedDrawingPath = ""; // Reset nếu file không tồn tại
                if (binding.imageDrawing != null) binding.imageDrawing.setVisibility(View.GONE);
                if (binding.imageRemoveDrawing != null) binding.imageRemoveDrawing.setVisibility(View.GONE);
                Log.e(TAG, getString(R.string.drawing_file_not_found) + " " + alreadyAvailableNote.getDrawingPath());
            }
        } else {
            if (binding.imageDrawing != null) binding.imageDrawing.setVisibility(View.GONE);
            if (binding.imageRemoveDrawing != null) binding.imageRemoveDrawing.setVisibility(View.GONE);
        }

        if (alreadyAvailableNote.getWebLink() != null && !alreadyAvailableNote.getWebLink().trim().isEmpty()) {
            binding.textWebURL.setText(alreadyAvailableNote.getWebLink());
            binding.layoutWebURL.setVisibility(View.VISIBLE);
        } else {
            binding.layoutWebURL.setVisibility(View.GONE);
        }

        if (alreadyAvailableNote.getColor() != null && !alreadyAvailableNote.getColor().trim().isEmpty()) {
            selectedNoteColor = alreadyAvailableNote.getColor();
        } else {
            selectedNoteColor = DEFAULT_NOTE_COLOR;
        }
        setSubtitleIndicatorColor();
        if (binding.miscellaneousLayout != null && binding.miscellaneousLayout.getRoot().findViewById(R.id.layoutMiscellaneous) != null) {
            setCheckmarkOnSelectedColor(selectedNoteColor);
        }
    }


    private void saveNote() {
        final String noteTitle = binding.inputNoteTitle.getText().toString().trim();
        String noteSubtitle = binding.inputNoteSubtitle.getText().toString().trim();
        final Editable editableNoteText = binding.inputNote.getText();
        final String noteDateTimeDisplay = binding.textDateTime.getText().toString().trim();

        if (noteTitle.isEmpty()) {
            Toast.makeText(this, R.string.note_title_cannot_be_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        String htmlNoteContent = "";
        if (editableNoteText != null) {
            htmlNoteContent = HtmlCompat.toHtml(editableNoteText, HtmlCompat.TO_HTML_PARAGRAPH_LINES_INDIVIDUAL);
        }

        if (noteSubtitle.isEmpty() && editableNoteText != null && !editableNoteText.toString().trim().isEmpty()) {
            String plainContent = editableNoteText.toString().trim();
            if (plainContent.length() > 100) {
                noteSubtitle = plainContent.substring(0, 100) + "...";
            } else {
                noteSubtitle = plainContent;
            }
        }

        final Note note = new Note();
        note.setTitle(noteTitle);
        note.setSubtitle(noteSubtitle);
        note.setNoteText(htmlNoteContent);
        note.setDateTime(noteDateTimeDisplay);
        note.setTimestamp(System.currentTimeMillis()); // Luôn cập nhật timestamp khi lưu
        note.setColor(selectedNoteColor);
        note.setImagePath(selectedImagePath); // Đã là đường dẫn nội bộ
        note.setDrawingPath(selectedDrawingPath); // Đã là đường dẫn nội bộ

        if (binding.layoutWebURL.getVisibility() == View.VISIBLE) {
            note.setWebLink(binding.textWebURL.getText().toString().trim());
        }

        // Cập nhật initial state để hasUnsavedChanges hoạt động đúng
        captureInitialNoteStateAfterSave(note);


        if (alreadyAvailableNote != null) {
            note.setId(alreadyAvailableNote.getId());
            // Xóa file ảnh/bản vẽ cũ nếu đường dẫn thay đổi hoặc bị xóa
            handleOldFileDeletion(alreadyAvailableNote.getImagePath(), selectedImagePath);
            handleOldFileDeletion(alreadyAvailableNote.getDrawingPath(), selectedDrawingPath);
            noteViewModel.update(note);
        } else {
            noteViewModel.insert(note);
        }

        Intent intent = new Intent();
        setResult(RESULT_OK, intent);
        finish();
    }

    private void captureInitialNoteStateAfterSave(Note savedNote) {
        initialTitle = savedNote.getTitle() != null ? savedNote.getTitle().trim() : "";
        initialSubtitle = savedNote.getSubtitle() != null ? savedNote.getSubtitle().trim() : "";
        if (savedNote.getNoteText() != null && !savedNote.getNoteText().trim().isEmpty()) {
            Spanned spannedText = HtmlCompat.fromHtml(savedNote.getNoteText(), HtmlCompat.FROM_HTML_MODE_LEGACY);
            initialNoteText = spannedText.toString().trim();
        } else {
            initialNoteText = "";
        }
        initialColor = savedNote.getColor() != null ? savedNote.getColor() : DEFAULT_NOTE_COLOR;
        initialImagePath = savedNote.getImagePath() != null ? savedNote.getImagePath() : "";
        initialWebLink = savedNote.getWebLink() != null ? savedNote.getWebLink().trim() : "";
        initialDrawingPath = savedNote.getDrawingPath() != null ? savedNote.getDrawingPath() : "";
    }

    private void handleOldFileDeletion(String oldPath, String newPath) {
        // Nếu có đường dẫn cũ, và nó khác đường dẫn mới (hoặc đường dẫn mới rỗng nghĩa là file bị xóa)
        if (oldPath != null && !oldPath.isEmpty() && !oldPath.equals(newPath)) {
            deleteFileFromInternalStorage(oldPath);
        }
    }


    private void initMiscellaneous() {
        final LayoutMiscellaneousBinding miscBinding = binding.miscellaneousLayout;
        final BottomSheetBehavior<LinearLayout> bottomSheetBehavior = BottomSheetBehavior.from(miscBinding.getRoot());

        miscBinding.textMiscellaneous.setOnClickListener(v -> {
            if (bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            } else {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });

        View.OnClickListener colorClickListener = v -> {
            String newColor = DEFAULT_NOTE_COLOR;
            Object tag = v.getTag();
            if (tag instanceof String) {
                newColor = (String) tag;
            }
            selectedNoteColor = newColor;
            setSubtitleIndicatorColor();
            setCheckmarkOnSelectedColor(selectedNoteColor);
        };

        miscBinding.viewColor1.setTag("#333333");
        miscBinding.viewColor1.setOnClickListener(colorClickListener);
        miscBinding.viewColor2.setTag("#FDBE3B");
        miscBinding.viewColor2.setOnClickListener(colorClickListener);
        miscBinding.viewColor3.setTag("#FF4842");
        miscBinding.viewColor3.setOnClickListener(colorClickListener);
        miscBinding.viewColor4.setTag("#3A52FC");
        miscBinding.viewColor4.setOnClickListener(colorClickListener);
        miscBinding.viewColor5.setTag("#000000");
        miscBinding.viewColor5.setOnClickListener(colorClickListener);

        setCheckmarkOnSelectedColor(selectedNoteColor);
        updateFormatButtonStates();

        miscBinding.buttonFormatBold.setOnClickListener(v -> {
            isBoldActive = !isBoldActive;
            applyStyleToSelectionOrToggleTypingMode(new StyleSpan(Typeface.BOLD), StyleSpan.class, Typeface.BOLD, isBoldActive);
        });

        miscBinding.buttonFormatItalic.setOnClickListener(v -> {
            isItalicActive = !isItalicActive;
            applyStyleToSelectionOrToggleTypingMode(new StyleSpan(Typeface.ITALIC), StyleSpan.class, Typeface.ITALIC, isItalicActive);
        });

        miscBinding.buttonFormatUnderline.setOnClickListener(v -> {
            isUnderlineActive = !isUnderlineActive;
            applyStyleToSelectionOrToggleTypingMode(new UnderlineSpan(), UnderlineSpan.class, -1, isUnderlineActive);
        });

        binding.inputNote.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (isFormattingText) return;
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isFormattingText) return;
                Editable editable = binding.inputNote.getEditableText();
                if (editable == null) return;

                int selectionStart = binding.inputNote.getSelectionStart();
                int selectionEnd = binding.inputNote.getSelectionEnd();

                if (count > before && selectionStart == selectionEnd) { // Chỉ áp dụng khi gõ và không có vùng chọn
                    isFormattingText = true;
                    int charactersAddedCount = count - before;
                    int actualStartOfNewChars = selectionEnd - charactersAddedCount;
                    if (actualStartOfNewChars < 0) actualStartOfNewChars = 0;

                    if (isBoldActive) {
                        editable.setSpan(new StyleSpan(Typeface.BOLD), actualStartOfNewChars, selectionEnd, Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
                    }
                    if (isItalicActive) {
                        editable.setSpan(new StyleSpan(Typeface.ITALIC), actualStartOfNewChars, selectionEnd, Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
                    }
                    if (isUnderlineActive) {
                        editable.setSpan(new UnderlineSpan(), actualStartOfNewChars, selectionEnd, Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
                    }
                    isFormattingText = false;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (isFormattingText) return;
                scheduleButtonStateUpdate();
            }
        });

        binding.inputNote.setOnClickListener(v -> scheduleButtonStateUpdate());
        binding.inputNote.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                scheduleButtonStateUpdate();
            }
        });
        binding.inputNote.post(this::scheduleButtonStateUpdate);

        if (miscBinding.layoutAddImage != null) {
            miscBinding.layoutAddImage.setOnClickListener(v -> {
                Log.d(TAG, getString(R.string.add_image_clicked));
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                selectImage();
            });
        } else {
            Log.e(TAG, getString(R.string.layoutaddimage_is_null_check_layout_miscellaneous_xml));
        }

        if (miscBinding.layoutAddUrl != null) {
            miscBinding.layoutAddUrl.setOnClickListener(v -> {
                Log.d(TAG, getString(R.string.add_url_clicked));
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                showAddURLDialog();
            });
        } else {
            Log.w(TAG, "layoutAddUrl is null.");
        }

        if (miscBinding.layoutAddDrawing != null) {
            miscBinding.layoutAddDrawing.setOnClickListener(v -> {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                Intent intent = new Intent(getApplicationContext(), DrawActivity.class);
                // Truyền đường dẫn bản vẽ hiện tại nếu có để DrawActivity có thể tải nó
                if (selectedDrawingPath != null && !selectedDrawingPath.isEmpty()) {
                    intent.putExtra(DrawActivity.EXTRA_DRAWING_PATH_TO_LOAD, selectedDrawingPath);
                }
                drawingActivityLauncher.launch(intent);
            });
        }

        if (alreadyAvailableNote != null) {
            miscBinding.layoutDeleteNote.setVisibility(View.VISIBLE);
            miscBinding.layoutDeleteNote.setOnClickListener(v -> {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                showDeleteNoteDialog();
            });
        } else {
            miscBinding.layoutDeleteNote.setVisibility(View.GONE);
        }

        binding.imageRemoveImage.setOnClickListener(v -> {
            deleteFileFromInternalStorage(selectedImagePath); // Xóa file nếu có
            selectedImagePath = "";
            binding.imageNote.setVisibility(View.GONE);
            binding.imageRemoveImage.setVisibility(View.GONE);
            binding.imageNote.setImageBitmap(null);
        });

        binding.imageRemoveWebURL.setOnClickListener(v -> {
            binding.textWebURL.setText(null);
            binding.layoutWebURL.setVisibility(View.GONE);
        });
    }

    private void updateFormatButtonStates() {
        final LayoutMiscellaneousBinding miscBinding = binding.miscellaneousLayout;
        if (miscBinding == null) return;
        int activeColor = ContextCompat.getColor(this, R.color.colorAccent);
        int inactiveColor = Color.TRANSPARENT;

        miscBinding.buttonFormatBold.setBackgroundTintList(ColorStateList.valueOf(isBoldActive ? activeColor : inactiveColor));
        miscBinding.buttonFormatItalic.setBackgroundTintList(ColorStateList.valueOf(isItalicActive ? activeColor : inactiveColor));
        miscBinding.buttonFormatUnderline.setBackgroundTintList(ColorStateList.valueOf(isUnderlineActive ? activeColor : inactiveColor));
    }

    private void scheduleButtonStateUpdate() {
        handler.removeCallbacks(updateButtonStateRunnable);
        handler.postDelayed(updateButtonStateRunnable, 50);
    }

    private void updateButtonStatesBasedOnCursor() {
        if (isFormattingText) return;
        Editable editable = binding.inputNote.getEditableText();
        if (editable == null) return;

        int selectionStart = binding.inputNote.getSelectionStart();
        int selectionEnd = binding.inputNote.getSelectionEnd();

        if (selectionStart < 0 || selectionEnd < 0) return;

        if (selectionStart == selectionEnd) {
            isBoldActive = hasStyleAtCursor(editable, selectionStart, StyleSpan.class, Typeface.BOLD);
            isItalicActive = hasStyleAtCursor(editable, selectionStart, StyleSpan.class, Typeface.ITALIC);
            isUnderlineActive = hasStyleAtCursor(editable, selectionStart, UnderlineSpan.class, -1);
        } else {
            isBoldActive = isSelectionStyled(editable, selectionStart, selectionEnd, StyleSpan.class, Typeface.BOLD);
            isItalicActive = isSelectionStyled(editable, selectionStart, selectionEnd, StyleSpan.class, Typeface.ITALIC);
            isUnderlineActive = isSelectionStyled(editable, selectionStart, selectionEnd, UnderlineSpan.class, -1);
        }
        updateFormatButtonStates();
    }

    private boolean hasStyleAtCursor(Editable editable, int position, Class<? extends CharacterStyle> styleClass, int styleType) {
        if (position > 0) {
            CharacterStyle[] spans = editable.getSpans(position - 1, position, styleClass);
            for (CharacterStyle span : spans) {
                if (span instanceof StyleSpan) {
                    if (((StyleSpan) span).getStyle() == styleType) return true;
                } else if (span instanceof UnderlineSpan && styleClass == UnderlineSpan.class) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isSelectionStyled(Editable editable, int start, int end, Class<? extends CharacterStyle> styleClass, int styleType) {
        // Kiểm tra xem toàn bộ vùng chọn có style đó không.
        // Nếu bất kỳ ký tự nào trong vùng chọn không có style, thì coi như isStyled là false.
        if (start == end) return hasStyleAtCursor(editable, start, styleClass, styleType);

        for (int i = start; i < end; i++) {
            CharacterStyle[] spans = editable.getSpans(i, i + 1, styleClass);
            boolean charHasStyle = false;
            for (CharacterStyle span : spans) {
                if (span instanceof StyleSpan && ((StyleSpan) span).getStyle() == styleType) {
                    charHasStyle = true;
                    break;
                } else if (span instanceof UnderlineSpan && styleClass == UnderlineSpan.class) {
                    charHasStyle = true;
                    break;
                }
            }
            if (!charHasStyle) return false; // Nếu một ký tự không có style, toàn bộ vùng chọn không được coi là styled.
        }
        return true; // Tất cả ký tự trong vùng chọn đều có style.
    }


    private <T extends CharacterStyle> void applyStyleToSelectionOrToggleTypingMode(Object styleSpanToApply, Class<T> spanClassToQuery, int styleTypeToMatch, boolean setActiveForTyping) {
        isFormattingText = true;
        Editable editable = binding.inputNote.getEditableText();
        if (editable == null) {
            isFormattingText = false;
            return;
        }

        int selectionStart = binding.inputNote.getSelectionStart();
        int selectionEnd = binding.inputNote.getSelectionEnd();

        if (selectionStart < 0 || selectionEnd < 0) {
            isFormattingText = false;
            return;
        }

        if (selectionStart != selectionEnd) { // Có vùng chọn
            boolean currentSelectionIsStyled = isSelectionStyled(editable, selectionStart, selectionEnd, spanClassToQuery, styleTypeToMatch);

            if (currentSelectionIsStyled) { // Nếu đã styled -> xóa style
                T[] spansToRemove = editable.getSpans(selectionStart, selectionEnd, spanClassToQuery);
                for (T span : spansToRemove) {
                    if (span instanceof StyleSpan && ((StyleSpan) span).getStyle() == styleTypeToMatch) {
                        editable.removeSpan(span);
                    } else if (span instanceof UnderlineSpan && spanClassToQuery == UnderlineSpan.class) {
                        editable.removeSpan(span);
                    }
                }
            } else { // Nếu chưa styled -> áp dụng style
                // Trước khi áp dụng style mới, xóa các style xung đột có thể có từng phần
                T[] existingSpans = editable.getSpans(selectionStart, selectionEnd, spanClassToQuery);
                for (T span : existingSpans) {
                    if (span instanceof StyleSpan && ((StyleSpan) span).getStyle() == styleTypeToMatch) {
                        editable.removeSpan(span);
                    } else if (span instanceof UnderlineSpan && spanClassToQuery == UnderlineSpan.class) {
                        editable.removeSpan(span);
                    }
                }
                editable.setSpan(styleSpanToApply, selectionStart, selectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        // Trạng thái (isBoldActive, v.v.) đã được cập nhật bởi listener của button.
        // onTextChanged sẽ xử lý việc áp dụng style khi gõ nếu không có vùng chọn.
        updateFormatButtonStates();
        isFormattingText = false;
    }


    private void setCheckmarkOnSelectedColor(String hexColor) {
        final LayoutMiscellaneousBinding miscBinding = binding.miscellaneousLayout;
        if (miscBinding == null) return;
        if (hexColor == null || hexColor.trim().isEmpty()) {
            hexColor = DEFAULT_NOTE_COLOR;
        }
        String tagColor1 = miscBinding.viewColor1.getTag() instanceof String ? (String) miscBinding.viewColor1.getTag() : DEFAULT_NOTE_COLOR;
        String tagColor2 = miscBinding.viewColor2.getTag() instanceof String ? (String) miscBinding.viewColor2.getTag() : "#FDBE3B";
        String tagColor3 = miscBinding.viewColor3.getTag() instanceof String ? (String) miscBinding.viewColor3.getTag() : "#FF4842";
        String tagColor4 = miscBinding.viewColor4.getTag() instanceof String ? (String) miscBinding.viewColor4.getTag() : "#3A52FC";
        String tagColor5 = miscBinding.viewColor5.getTag() instanceof String ? (String) miscBinding.viewColor5.getTag() : "#000000";

        miscBinding.imageColor1.setVisibility(hexColor.equalsIgnoreCase(tagColor1) ? View.VISIBLE : View.GONE);
        miscBinding.imageColor2.setVisibility(hexColor.equalsIgnoreCase(tagColor2) ? View.VISIBLE : View.GONE);
        miscBinding.imageColor3.setVisibility(hexColor.equalsIgnoreCase(tagColor3) ? View.VISIBLE : View.GONE);
        miscBinding.imageColor4.setVisibility(hexColor.equalsIgnoreCase(tagColor4) ? View.VISIBLE : View.GONE);
        miscBinding.imageColor5.setVisibility(hexColor.equalsIgnoreCase(tagColor5) ? View.VISIBLE : View.GONE);
    }

    private void selectImage() {
        String permissionToRequest;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionToRequest = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permissionToRequest = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(this, permissionToRequest) != PackageManager.PERMISSION_GRANTED) {
            if (shouldShowRequestPermissionRationale(permissionToRequest)) {
                new AlertDialog.Builder(this)
                        .setMessage(R.string.this_permission_is_needed_to_select_images_from_your_device)
                        .setPositiveButton(R.string.ok, (dialog, which) -> requestPermissionLauncher.launch(permissionToRequest))
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            } else {
                requestPermissionLauncher.launch(permissionToRequest);
            }
        } else {
            actualLaunchImagePicker();
        }
    }

    private void actualLaunchImagePicker() {
        pickImageLauncher.launch("image/*");
    }

    private void setSubtitleIndicatorColor() {
        GradientDrawable gradientDrawable = (GradientDrawable) binding.viewSubtitleIndicator.getBackground();
        String colorToParse = (selectedNoteColor != null && !selectedNoteColor.trim().isEmpty()) ? selectedNoteColor : DEFAULT_NOTE_COLOR;
        try {
            gradientDrawable.setColor(Color.parseColor(colorToParse));
        } catch (IllegalArgumentException e) {
            Log.e(TAG, getString(R.string.failed_to_parse_color) + colorToParse, e);
            gradientDrawable.setColor(Color.parseColor(DEFAULT_NOTE_COLOR));
        }
    }

    /**
     * Copies a file from a content URI to the app's internal storage.
     * @param sourceUri The content URI of the source file.
     * @param targetDirectoryName The name of the directory within getFilesDir() to save the file.
     * @return The absolute path to the saved file in internal storage, or null if failed.
     */
    private String copyFileToInternalStorage(Uri sourceUri, String targetDirectoryName) {
        if (sourceUri == null) return null;

        String fileExtension = ".dat"; // Default extension
        String mimeType = getContentResolver().getType(sourceUri);
        if (mimeType != null) {
            if (mimeType.startsWith("image/")) {
                fileExtension = "." + mimeType.substring(mimeType.lastIndexOf('/') + 1);
                if (fileExtension.equals(".jpeg")) fileExtension = ".jpg"; // Normalize
            }
            // Add more mime types if needed for other file types
        }

        // Create a unique file name
        String fileName = targetDirectoryName.substring(0, Math.min(targetDirectoryName.length(), 4)) + "_" +
                UUID.randomUUID().toString() + fileExtension;

        File internalDir = new File(getFilesDir(), targetDirectoryName);
        if (!internalDir.exists()) {
            if (!internalDir.mkdirs()) {
                Log.e(TAG, "Failed to create internal directory: " + internalDir.getAbsolutePath());
                return null;
            }
        }

        File outputFile = new File(internalDir, fileName);

        try (InputStream inputStream = getContentResolver().openInputStream(sourceUri);
             OutputStream outputStream = new FileOutputStream(outputFile)) {

            if (inputStream == null) {
                Log.e(TAG, "Could not open input stream for URI: " + sourceUri);
                return null;
            }

            byte[] buffer = new byte[4096]; // 4KB buffer
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            Log.d(TAG, "File copied to internal storage: " + outputFile.getAbsolutePath());
            return outputFile.getAbsolutePath();

        } catch (Exception e) {
            Log.e(TAG, "Error copying file to internal storage: " + e.getMessage(), e);
            if (outputFile.exists()) { // Clean up if copy failed partially
                outputFile.delete();
            }
            return null;
        }
    }

    private void deleteFileFromInternalStorage(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return;
        }
        File fileToDelete = new File(filePath);
        // An toàn hơn: chỉ xóa file nếu nó nằm trong thư mục của ứng dụng
        if (fileToDelete.exists() && fileToDelete.isFile() &&
                (filePath.startsWith(getFilesDir() + File.separator + IMAGES_DIR_NAME) ||
                        filePath.startsWith(getFilesDir() + File.separator + DrawActivity.DRAWINGS_DIR_NAME_STATIC))) { // Sử dụng hằng số từ DrawActivity
            if (fileToDelete.delete()) {
                Log.d(TAG, "Successfully deleted internal file: " + filePath);
            } else {
                Log.w(TAG, "Failed to delete internal file: " + filePath);
            }
        } else if (fileToDelete.exists()){
            Log.w(TAG, "Attempted to delete file outside app's designated directories or file does not exist: " + filePath);
        }
    }


    private void showAddURLDialog() {
        if (dialogAddURL == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(CreateNoteActivity.this);
            View view = LayoutInflater.from(this).inflate(R.layout.layout_add_url, null);
            builder.setView(view);

            dialogAddURL = builder.create();
            if (dialogAddURL.getWindow() != null) {
                dialogAddURL.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            final EditText inputURL = view.findViewById(R.id.inputUrl);
            inputURL.requestFocus();

            view.findViewById(R.id.textAdd).setOnClickListener(v1 -> {
                String url = inputURL.getText().toString().trim();
                if (url.isEmpty()) {
                    Toast.makeText(CreateNoteActivity.this, R.string.enter_url, Toast.LENGTH_SHORT).show();
                } else if (!Patterns.WEB_URL.matcher(url).matches()) {
                    Toast.makeText(CreateNoteActivity.this, R.string.enter_valid_url, Toast.LENGTH_SHORT).show();
                } else {
                    binding.textWebURL.setText(url);
                    binding.layoutWebURL.setVisibility(View.VISIBLE);
                    dialogAddURL.dismiss();
                }
            });

            view.findViewById(R.id.textCancel).setOnClickListener(v1 -> dialogAddURL.dismiss());
        }
        dialogAddURL.show();
    }

    private void showDeleteNoteDialog() {
        if (dialogDeleteNote == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(CreateNoteActivity.this);
            View view = LayoutInflater.from(this).inflate(R.layout.layout_delete_note, null);
            builder.setView(view);
            dialogDeleteNote = builder.create();
            if (dialogDeleteNote.getWindow() != null) {
                dialogDeleteNote.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            view.findViewById(R.id.textDeleteNote).setOnClickListener(v -> {
                if (alreadyAvailableNote != null) {
                    // Xóa file media liên quan trước khi xóa note khỏi DB
                    deleteFileFromInternalStorage(alreadyAvailableNote.getImagePath());
                    deleteFileFromInternalStorage(alreadyAvailableNote.getDrawingPath());
                    noteViewModel.delete(alreadyAvailableNote);
                }
                Intent intent = new Intent();
                intent.putExtra("action_performed", "deleted_to_trash");
                setResult(RESULT_OK, intent);

                if (dialogDeleteNote != null && dialogDeleteNote.isShowing()) {
                    dialogDeleteNote.dismiss();
                }
                finish();
            });
            view.findViewById(R.id.textCancel).setOnClickListener(v -> {
                if (dialogDeleteNote != null && dialogDeleteNote.isShowing()) {
                    dialogDeleteNote.dismiss();
                }
            });
        }
        dialogDeleteNote.show();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(updateButtonStateRunnable);
        super.onDestroy();
    }
}