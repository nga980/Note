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
import android.content.res.ColorStateList; // THÊM IMPORT NÀY
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextWatcher; // THÊM IMPORT NÀY
import android.text.style.CharacterStyle; // THÊM IMPORT NÀY
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText; // Đã có
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.notes.R;
import com.example.notes.databinding.ActivityCreateNoteBinding;
import com.example.notes.databinding.LayoutMiscellaneousBinding;
import com.example.notes.entities.Note;
import com.example.notes.viewmodels.NoteViewModel;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class CreateNoteActivity extends AppCompatActivity {

    private static final String TAG = "CreateNoteActivity";

    private ActivityCreateNoteBinding binding;

    private String selectedNoteColor;
    private String selectedImagePath;
    private String selectedDrawingPath;

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

    // BIẾN TRẠNG THÁI CHO ĐỊNH DẠNG VĂN BẢN
    private boolean isBoldActive = false;
    private boolean isItalicActive = false;
    private boolean isUnderlineActive = false;

    // Biến để theo dõi trạng thái của TextWatcher, tránh vòng lặp vô hạn
    private boolean isFormattingText = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateNoteBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        noteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);

        // ... (requestPermissionLauncher, pickImageLauncher, drawingActivityLauncher giữ nguyên) ...
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        actualLaunchImagePicker();
                    } else {
                        Toast.makeText(this, "Permission Denied!", Toast.LENGTH_SHORT).show();
                    }
                });

        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
                            if (inputStream != null) {
                                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                                binding.imageNote.setImageBitmap(bitmap);
                                binding.imageNote.setVisibility(View.VISIBLE);
                                binding.imageRemoveImage.setVisibility(View.VISIBLE);
                                selectedImagePath = getPathFromUri(uri);
                            }
                        } catch (Exception exception) {
                            Toast.makeText(this, exception.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Error decoding image stream: " + exception.getMessage(), exception);
                        }
                    }
                });

        drawingActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        String path = result.getData().getStringExtra(DrawActivity.EXTRA_DRAWING_PATH);
                        if (path != null && !path.isEmpty()) {
                            selectedDrawingPath = path;
                            if (binding.imageDrawing != null) {
                                binding.imageDrawing.setImageURI(Uri.fromFile(new File(selectedDrawingPath)));
                                binding.imageDrawing.setVisibility(View.VISIBLE);
                                if (binding.imageRemoveDrawing != null) {
                                    binding.imageRemoveDrawing.setVisibility(View.VISIBLE);
                                }
                            }
                            toggleDrawingInstructionsVisibility();
                        }
                    }
                }
        );


        binding.imageBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        binding.textDateTime.setText(
                new SimpleDateFormat("EEEE, dd MMMM yyyy HH:mm a", Locale.getDefault()) // Sửa lại yyyy
                        .format(new Date())
        );

        binding.imageSave.setOnClickListener(v -> saveNote());

        selectedNoteColor = DEFAULT_NOTE_COLOR;
        selectedImagePath = "";
        selectedDrawingPath = "";

        if (getIntent().getBooleanExtra("isViewOrUpdate", false)) {
            alreadyAvailableNote = (Note) getIntent().getSerializableExtra("note");
            setViewOrUpdateNote();
            binding.inputNote.post(this::updateButtonStatesBasedOnCursor); // Cập nhật trạng thái nút sau khi tải nội dung
        }

        captureInitialNoteState();

        if (getIntent().hasExtra("quick_action_type")) {
            String type = getIntent().getStringExtra("quick_action_type");
            if ("image".equals(type)) {
                selectImage();
            } else if ("url".equals(type)) {
                showAddURLDialog();
            } else if ("drawing".equals(type)) { // THÊM TRƯỜNG HỢP NÀY
                // Mở DrawingActivity ngay lập tức
                Intent drawingIntent = new Intent(getApplicationContext(), DrawActivity.class);
                drawingActivityLauncher.launch(drawingIntent);
            }
        }

        initMiscellaneous(); // Gọi updateButtonStatesBasedOnCursor và TextWatcher ở đây
        setSubtitleIndicatorColor();


        if (binding.imageRemoveDrawing != null) {
            binding.imageRemoveDrawing.setOnClickListener(v -> {
                selectedDrawingPath = null;
                binding.imageDrawing.setVisibility(View.GONE);
                binding.imageRemoveDrawing.setVisibility(View.GONE);
                binding.imageDrawing.setImageBitmap(null);
                toggleDrawingInstructionsVisibility();
            });
        }
        toggleDrawingInstructionsVisibility();


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
        // ... (giữ nguyên logic captureInitialNoteState hiện tại của bạn) ...
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
            initialNoteText = binding.inputNote.getText().toString().trim();
            initialColor = selectedNoteColor;
            initialImagePath = selectedImagePath;
            initialWebLink = (binding.layoutWebURL.getVisibility() == View.VISIBLE) ? binding.textWebURL.getText().toString().trim() : "";
            initialDrawingPath = selectedDrawingPath;
        }
    }

    private boolean hasUnsavedChanges() {
        // ... (giữ nguyên logic hasUnsavedChanges hiện tại của bạn, bao gồm cả so sánh drawingPath) ...
        String currentTitle = binding.inputNoteTitle.getText().toString().trim();
        String currentSubtitle = binding.inputNoteSubtitle.getText().toString().trim();
        String currentNoteText = binding.inputNote.getText().toString().trim();
        String currentWebLink = binding.layoutWebURL.getVisibility() == View.VISIBLE ? binding.textWebURL.getText().toString().trim() : "";

        if (!Objects.equals(currentTitle, initialTitle)) return true;
        if (!Objects.equals(currentSubtitle, initialSubtitle)) return true;
        if (!Objects.equals(currentNoteText, initialNoteText)) return true;
        if (!Objects.equals(selectedNoteColor, initialColor)) return true;
        if (!Objects.equals(selectedImagePath, initialImagePath)) return true;
        if (!Objects.equals(currentWebLink, initialWebLink)) return true;
        return !Objects.equals(selectedDrawingPath, initialDrawingPath);
    }

    private void setViewOrUpdateNote() {
        // ... (giữ nguyên logic setViewOrUpdateNote hiện tại của bạn, bao gồm cả hiển thị drawing) ...
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

        if (alreadyAvailableNote.getImagePath() != null && !alreadyAvailableNote.getImagePath().trim().isEmpty()) {
            selectedImagePath = alreadyAvailableNote.getImagePath();
            binding.imageNote.setImageURI(Uri.fromFile(new File(selectedImagePath)));
            binding.imageNote.setVisibility(View.VISIBLE);
            binding.imageRemoveImage.setVisibility(View.VISIBLE);
        } else {
            binding.imageNote.setVisibility(View.GONE);
            binding.imageRemoveImage.setVisibility(View.GONE);
        }

        if (alreadyAvailableNote.getDrawingPath() != null && !alreadyAvailableNote.getDrawingPath().trim().isEmpty()) {
            selectedDrawingPath = alreadyAvailableNote.getDrawingPath();
            if (binding.imageDrawing != null) {
                binding.imageDrawing.setImageURI(Uri.fromFile(new File(selectedDrawingPath)));
                binding.imageDrawing.setVisibility(View.VISIBLE);
                if (binding.imageRemoveDrawing != null) {
                    binding.imageRemoveDrawing.setVisibility(View.VISIBLE);
                }
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

        if (alreadyAvailableNote.getColor() != null && !alreadyAvailableNote.getColor().trim().isEmpty()){
            selectedNoteColor = alreadyAvailableNote.getColor();
        } else {
            selectedNoteColor = DEFAULT_NOTE_COLOR;
        }
        toggleDrawingInstructionsVisibility();
    }


    private void saveNote() {
        // ... (giữ nguyên logic saveNote hiện tại của bạn, bao gồm cả lưu drawingPath) ...
        final String noteTitle = binding.inputNoteTitle.getText().toString().trim();
        String noteSubtitle = binding.inputNoteSubtitle.getText().toString().trim();
        final Editable editableNoteText = binding.inputNote.getText();
        final String noteDateTime = binding.textDateTime.getText().toString().trim();

        if (noteTitle.isEmpty()) {
            Toast.makeText(this, R.string.note_title_cant_be_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        String htmlNoteContent = HtmlCompat.toHtml(editableNoteText, HtmlCompat.TO_HTML_PARAGRAPH_LINES_INDIVIDUAL);

        if (noteSubtitle.isEmpty() && !editableNoteText.toString().trim().isEmpty()) {
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
        note.setDateTime(noteDateTime);
        note.setColor(selectedNoteColor);
        note.setImagePath(selectedImagePath);
        note.setDrawingPath(selectedDrawingPath);

        if (binding.layoutWebURL.getVisibility() == View.VISIBLE) {
            note.setWebLink(binding.textWebURL.getText().toString().trim());
        }

        initialTitle = noteTitle;
        initialSubtitle = noteSubtitle;
        initialNoteText = editableNoteText.toString().trim();
        initialColor = selectedNoteColor;
        initialImagePath = selectedImagePath;
        initialWebLink = note.getWebLink() != null ? note.getWebLink().trim() : "";
        initialDrawingPath = selectedDrawingPath;

        if (alreadyAvailableNote != null) {
            note.setId(alreadyAvailableNote.getId());
            noteViewModel.update(note);
        } else {
            noteViewModel.insert(note);
        }

        Intent intent = new Intent();
        setResult(RESULT_OK, intent);
        finish();
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

        // ... (colorClickListener và setCheckmarkOnSelectedColor giữ nguyên) ...
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

        miscBinding.viewColor1.setOnClickListener(colorClickListener);
        miscBinding.viewColor2.setOnClickListener(colorClickListener);
        miscBinding.viewColor3.setOnClickListener(colorClickListener);
        miscBinding.viewColor4.setOnClickListener(colorClickListener);
        miscBinding.viewColor5.setOnClickListener(colorClickListener);
        setCheckmarkOnSelectedColor(selectedNoteColor);


        // --- CẬP NHẬT XỬ LÝ NÚT ĐỊNH DẠNG ---
        updateFormatButtonStates(); // Cập nhật giao diện nút ban đầu

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


        // --- TEXTWATCHER VÀ LISTENER CHO EditText ---
        binding.inputNote.addTextChangedListener(new TextWatcher() {
            int currentCursorPos;
            String textBeforeChange;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (isFormattingText) return;
                currentCursorPos = binding.inputNote.getSelectionStart();
                textBeforeChange = s.toString();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isFormattingText) return;

                // Nếu người dùng đang gõ (thêm ký tự) và không có vùng chọn
                if (count > before && binding.inputNote.getSelectionStart() == binding.inputNote.getSelectionEnd()) {
                    isFormattingText = true;
                    Editable editable = binding.inputNote.getEditableText();
                    int newCursorPos = binding.inputNote.getSelectionEnd();
                    int charactersAddedCount = count - before;
                    int actualStart = newCursorPos - charactersAddedCount;

                    if (actualStart < 0) actualStart = 0;


                    // Xóa các style cũ tại vị trí chèn nếu chúng không khớp với style active
                    // (Điều này phức tạp, tạm thời bỏ qua để tránh xung đột với typing-style)

                    if (isBoldActive) {
                        editable.setSpan(new StyleSpan(Typeface.BOLD), actualStart, newCursorPos, Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
                    }
                    if (isItalicActive) {
                        editable.setSpan(new StyleSpan(Typeface.ITALIC), actualStart, newCursorPos, Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
                    }
                    if (isUnderlineActive) {
                        editable.setSpan(new UnderlineSpan(), actualStart, newCursorPos, Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
                    }
                    isFormattingText = false;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (isFormattingText) return;
                // Cập nhật trạng thái nút dựa trên vị trí con trỏ mới sau khi văn bản thay đổi
                // Sử dụng post để đảm bảo selection đã ổn định
                binding.inputNote.post(() -> {
                    if (!isFormattingText) { // Kiểm tra lại cờ để tránh xung đột
                        updateButtonStatesBasedOnCursor();
                    }
                });
            }
        });

        // Cập nhật trạng thái nút khi click vào EditText hoặc khi focus thay đổi
        binding.inputNote.setOnClickListener(v -> updateButtonStatesBasedOnCursor());
        binding.inputNote.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                updateButtonStatesBasedOnCursor();
            }
        });
        // Gọi một lần khi bắt đầu để đảm bảo các nút có trạng thái đúng
        binding.inputNote.post(this::updateButtonStatesBasedOnCursor);


        // ... (code layoutAddImage, layoutAddUrl, layoutAddDrawing, etc. giữ nguyên) ...
        if (miscBinding.layoutAddDrawing != null) {
            miscBinding.layoutAddDrawing.setOnClickListener(v -> {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                Intent intent = new Intent(getApplicationContext(), DrawActivity.class);
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

    // --- CÁC PHƯƠNG THỨC MỚI CHO ĐỊNH DẠNG NÂNG CAO ---

    private void updateFormatButtonStates() {
        final LayoutMiscellaneousBinding miscBinding = binding.miscellaneousLayout;
        int activeColor = ContextCompat.getColor(this, R.color.colorAccent); // Ví dụ: màu vàng hoặc màu chính của app
        int inactiveColor = Color.TRANSPARENT; // Hoặc màu nền mặc định của nút

        miscBinding.buttonFormatBold.setBackgroundTintList(ColorStateList.valueOf(isBoldActive ? activeColor : inactiveColor));
        miscBinding.buttonFormatItalic.setBackgroundTintList(ColorStateList.valueOf(isItalicActive ? activeColor : inactiveColor));
        miscBinding.buttonFormatUnderline.setBackgroundTintList(ColorStateList.valueOf(isUnderlineActive ? activeColor : inactiveColor));
    }


    private void updateButtonStatesBasedOnCursor() {
        if (isFormattingText) return; // Bỏ qua nếu đang trong quá trình tự định dạng

        Editable editable = binding.inputNote.getEditableText();
        if (editable == null) return;

        int selectionStart = binding.inputNote.getSelectionStart();
        int selectionEnd = binding.inputNote.getSelectionEnd();

        if (selectionStart < 0 || selectionEnd < 0) return; // Vùng chọn không hợp lệ

        if (selectionStart == selectionEnd) { // Chỉ là con trỏ, không có vùng chọn
            // Kiểm tra style tại vị trí con trỏ (hoặc ký tự ngay trước đó)
            // Các biến is...Active sẽ phản ánh điều này
            isBoldActive = hasStyleAtCursor(editable, selectionStart, StyleSpan.class, Typeface.BOLD);
            isItalicActive = hasStyleAtCursor(editable, selectionStart, StyleSpan.class, Typeface.ITALIC);
            isUnderlineActive = hasStyleAtCursor(editable, selectionStart, UnderlineSpan.class, -1);
        } else { // Có vùng chọn
            // Kiểm tra xem TOÀN BỘ vùng chọn có style đó không
            isBoldActive = isSelectionStyled(editable, selectionStart, selectionEnd, StyleSpan.class, Typeface.BOLD);
            isItalicActive = isSelectionStyled(editable, selectionStart, selectionEnd, StyleSpan.class, Typeface.ITALIC);
            isUnderlineActive = isSelectionStyled(editable, selectionStart, selectionEnd, UnderlineSpan.class, -1);
        }
        updateFormatButtonStates();
    }

    private boolean hasStyleAtCursor(Editable editable, int position, Class<? extends CharacterStyle> styleClass, int styleType) {
        if (position > 0) { // Kiểm tra ký tự ngay trước con trỏ
            CharacterStyle[] spans = editable.getSpans(position - 1, position, styleClass);
            for (CharacterStyle span : spans) {
                if (span instanceof StyleSpan) {
                    if (((StyleSpan) span).getStyle() == styleType) return true;
                } else if (span instanceof UnderlineSpan && styleClass == UnderlineSpan.class) {
                    return true;
                }
            }
        }
        // Ngoài ra, nếu đang ở cuối một đoạn text vừa được gõ với style active,
        // các biến is...Active có thể vẫn là true.
        // Logic này cần tinh chỉnh để quyết định xem nên ưu tiên style của text hay style đang active cho typing.
        // Hiện tại, nó sẽ phản ánh style của text.
        return false;
    }

    private boolean isSelectionStyled(Editable editable, int start, int end, Class<? extends CharacterStyle> styleClass, int styleType) {
        // Kiểm tra xem có style nào thuộc loại này bao phủ toàn bộ vùng chọn không
        CharacterStyle[] spans = editable.getSpans(start, end, styleClass);
        for (CharacterStyle span : spans) {
            if (editable.getSpanStart(span) <= start && editable.getSpanEnd(span) >= end) {
                if (span instanceof StyleSpan) {
                    if (((StyleSpan) span).getStyle() == styleType) return true;
                } else if (span instanceof UnderlineSpan && styleClass == UnderlineSpan.class) {
                    return true;
                }
            }
        }
        return false;
    }


    private <T extends CharacterStyle> void applyStyleToSelectionOrToggleTypingMode(Object styleSpanToApply, Class<T> spanClassToQuery, int styleTypeToMatch, boolean setActiveForTyping) {
        // Các biến isBoldActive, isItalicActive, isUnderlineActive đã được cập nhật bởi listener của nút
        // Giờ chúng ta chỉ cần áp dụng hoặc xóa style cho vùng chọn (nếu có)

        isFormattingText = true; // Đặt cờ để TextWatcher bỏ qua thay đổi này

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
            // Xóa các span cũ cùng loại trong vùng chọn
            T[] existingSpans = editable.getSpans(selectionStart, selectionEnd, spanClassToQuery);
            for (T span : existingSpans) {
                boolean specificStyleMatch = false;
                if (spanClassToQuery.isInstance(span)) {
                    if (span instanceof StyleSpan && styleTypeToMatch != -1) {
                        if (((StyleSpan) span).getStyle() == styleTypeToMatch) specificStyleMatch = true;
                    } else if (span instanceof UnderlineSpan && styleTypeToMatch == -1 && spanClassToQuery == UnderlineSpan.class) {
                        specificStyleMatch = true;
                    }
                }
                if (specificStyleMatch) {
                    // Chỉ xóa nếu span nằm hoàn toàn trong vùng chọn hoặc giao với nó một cách hợp lý
                    // Để đơn giản, ta có thể xóa hết rồi áp dụng lại nếu cần
                    editable.removeSpan(span);
                }
            }

            if (setActiveForTyping) { // Nếu nút đang được BẬT
                editable.setSpan(styleSpanToApply, selectionStart, selectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        // Nếu không có vùng chọn, TextWatcher sẽ xử lý việc áp dụng style khi gõ,
        // dựa trên các biến is...Active đã được toggle.

        updateFormatButtonStates(); // Cập nhật giao diện nút
        isFormattingText = false;
    }


    // ... (toggleDrawingInstructionsVisibility, setCheckmarkOnSelectedColor giữ nguyên) ...
    private void toggleDrawingInstructionsVisibility() {
        if (binding.textDrawingPlaceholder != null) {
            boolean drawingVisible = selectedDrawingPath != null && !selectedDrawingPath.isEmpty() &&
                    binding.imageDrawing != null && binding.imageDrawing.getVisibility() == View.VISIBLE;
            binding.textDrawingPlaceholder.setVisibility(drawingVisible ? View.GONE : View.VISIBLE);
        }
    }

    private void setCheckmarkOnSelectedColor(String hexColor) {
        final LayoutMiscellaneousBinding miscBinding = binding.miscellaneousLayout;
        if (hexColor == null) {
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


    // ... (selectImage, actualLaunchImagePicker, setSubtitleIndicatorColor, getPathFromUri, showAddURLDialog, showDeleteNoteDialog giữ nguyên) ...
    private void selectImage() {
        String permissionToRequest;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionToRequest = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permissionToRequest = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(getApplicationContext(), permissionToRequest)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(permissionToRequest);
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
            Log.e(TAG, "Failed to parse color: " + colorToParse, e);
            gradientDrawable.setColor(Color.parseColor(DEFAULT_NOTE_COLOR));
        }
    }

    private String getPathFromUri(Uri contentUri) {
        String filePath = "";
        if (contentUri == null) return filePath;
        Cursor cursor = null;
        try {
            if ("content".equalsIgnoreCase(contentUri.getScheme())) {
                String[] projection = {MediaStore.Images.Media.DATA};
                cursor = getContentResolver().query(contentUri, projection, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    filePath = cursor.getString(index);
                }
            }
            if ((filePath == null || filePath.isEmpty()) && "file".equalsIgnoreCase(contentUri.getScheme())) {
                filePath = contentUri.getPath();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting path from Uri: " + contentUri.toString(), e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return (filePath != null) ? filePath : "";
    }

    private void showAddURLDialog() {
        if (dialogAddURL == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(CreateNoteActivity.this);
            View view = LayoutInflater.from(this).inflate(
                    R.layout.layout_add_url,
                    null
            );
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
            View view = LayoutInflater.from(this).inflate(
                    R.layout.layout_delete_note,
                    null
            );
            builder.setView(view);
            dialogDeleteNote = builder.create();
            if (dialogDeleteNote.getWindow() != null) {
                dialogDeleteNote.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            view.findViewById(R.id.textDeleteNote).setOnClickListener(v -> {
                if (alreadyAvailableNote != null) {
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
}