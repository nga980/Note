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
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
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
                if (!shouldShowRequestPermissionRationale(Manifest.permission.READ_MEDIA_IMAGES) &&
                        !shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    new AlertDialog.Builder(this)
                            .setMessage("Permission denied permanently. Please enable it in Settings.")
                            .setPositiveButton("Go to Settings", (dialog, which) -> {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                intent.setData(Uri.parse("package:" + getPackageName()));
                                startActivity(intent);
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                } else {
                    Toast.makeText(this, "Permission Denied! Cannot select image.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                try {
                    Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(uri));
                    binding.imageNote.setImageBitmap(bitmap);
                    binding.imageNote.setVisibility(View.VISIBLE);
                    binding.imageRemoveImage.setVisibility(View.VISIBLE);
                    selectedImagePath = getPathFromUri(uri);
                } catch (Exception e) {
                    Toast.makeText(this, "Failed to load image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error decoding image stream: " + e.getMessage(), e);
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
                                selectedDrawingPath = path;
                                if (binding.imageDrawing != null) {
                                    binding.imageDrawing.setImageURI(Uri.fromFile(drawingFile));
                                    binding.imageDrawing.setVisibility(View.VISIBLE);
                                    if (binding.imageRemoveDrawing != null) {
                                        binding.imageRemoveDrawing.setVisibility(View.VISIBLE);
                                    }
                                }
                            } else {
                                Toast.makeText(this, "Drawing file not found", Toast.LENGTH_SHORT).show();
                                Log.e(TAG, "Drawing file does not exist: " + path);
                            }
                        }
                    }
                }
        );

        binding.imageBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        binding.textDateTime.setText(
                new SimpleDateFormat("EEEE, dd MMMM yyyy HH:mm a", Locale.getDefault())
                        .format(new Date())
        );

        binding.imageSave.setOnClickListener(v -> saveNote());

        selectedNoteColor = DEFAULT_NOTE_COLOR;
        selectedImagePath = "";
        selectedDrawingPath = "";

        if (getIntent().getBooleanExtra("isViewOrUpdate", false)) {
            alreadyAvailableNote = (Note) getIntent().getSerializableExtra("note");
            setViewOrUpdateNote();
            binding.inputNote.post(this::scheduleButtonStateUpdate);
        }

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
                selectedDrawingPath = "";
                binding.imageDrawing.setVisibility(View.GONE);
                binding.imageRemoveDrawing.setVisibility(View.GONE);
                binding.imageDrawing.setImageBitmap(null);
            });
        }

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
            initialNoteText = binding.inputNote.getText().toString().trim();
            initialColor = selectedNoteColor;
            initialImagePath = selectedImagePath;
            initialWebLink = (binding.layoutWebURL.getVisibility() == View.VISIBLE) ? binding.textWebURL.getText().toString().trim() : "";
            initialDrawingPath = selectedDrawingPath;
        }
    }

    private boolean hasUnsavedChanges() {
        String currentTitle = binding.inputNoteTitle.getText().toString().trim();
        String currentSubtitle = binding.inputNoteSubtitle.getText().toString().trim();
        String currentNoteText = binding.inputNote.getText().toString().trim();
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

        if (alreadyAvailableNote.getImagePath() != null && !alreadyAvailableNote.getImagePath().trim().isEmpty()) {
            File imageFile = new File(alreadyAvailableNote.getImagePath());
            if (imageFile.exists()) {
                selectedImagePath = alreadyAvailableNote.getImagePath();
                binding.imageNote.setImageURI(Uri.fromFile(imageFile));
                binding.imageNote.setVisibility(View.VISIBLE);
                binding.imageRemoveImage.setVisibility(View.VISIBLE);
            } else {
                selectedImagePath = "";
                binding.imageNote.setVisibility(View.GONE);
                binding.imageRemoveImage.setVisibility(View.GONE);
                Log.e(TAG, "Image file not found: " + alreadyAvailableNote.getImagePath());
            }
        } else {
            binding.imageNote.setVisibility(View.GONE);
            binding.imageRemoveImage.setVisibility(View.GONE);
        }

        if (alreadyAvailableNote.getDrawingPath() != null && !alreadyAvailableNote.getDrawingPath().trim().isEmpty()) {
            File drawingFile = new File(alreadyAvailableNote.getDrawingPath());
            if (drawingFile.exists()) {
                selectedDrawingPath = alreadyAvailableNote.getDrawingPath();
                if (binding.imageDrawing != null) {
                    binding.imageDrawing.setImageURI(Uri.fromFile(drawingFile));
                    binding.imageDrawing.setVisibility(View.VISIBLE);
                    if (binding.imageRemoveDrawing != null) {
                        binding.imageRemoveDrawing.setVisibility(View.VISIBLE);
                    }
                }
            } else {
                selectedDrawingPath = "";
                if (binding.imageDrawing != null) binding.imageDrawing.setVisibility(View.GONE);
                if (binding.imageRemoveDrawing != null) binding.imageRemoveDrawing.setVisibility(View.GONE);
                Log.e(TAG, "Drawing file not found: " + alreadyAvailableNote.getDrawingPath());
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
    }

    private void saveNote() {
        final String noteTitle = binding.inputNoteTitle.getText().toString().trim();
        String noteSubtitle = binding.inputNoteSubtitle.getText().toString().trim();
        final Editable editableNoteText = binding.inputNote.getText();
        // Lấy chuỗi ngày tháng hiển thị từ TextView
        final String noteDateTimeDisplay = binding.textDateTime.getText().toString().trim();

        if (noteTitle.isEmpty()) {
            Toast.makeText(this, R.string.note_title_cannot_be_empty, Toast.LENGTH_SHORT).show(); // Sử dụng chuỗi đã sửa
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
        note.setDateTime(noteDateTimeDisplay); // Lưu chuỗi hiển thị
        note.setTimestamp(System.currentTimeMillis()); // <<< ĐẶT DẤU THỜI GIAN Ở ĐÂY
        note.setColor(selectedNoteColor);
        note.setImagePath(selectedImagePath);
        note.setDrawingPath(selectedDrawingPath);

        if (binding.layoutWebURL.getVisibility() == View.VISIBLE) {
            note.setWebLink(binding.textWebURL.getText().toString().trim());
        }

        // Cập nhật trạng thái ban đầu để tránh dialog "unsaved changes" không cần thiết
        initialTitle = noteTitle;
        initialSubtitle = noteSubtitle;
        initialNoteText = editableNoteText != null ? editableNoteText.toString().trim() : "";
        initialColor = selectedNoteColor;
        initialImagePath = selectedImagePath;
        initialWebLink = note.getWebLink() != null ? note.getWebLink().trim() : "";
        initialDrawingPath = selectedDrawingPath;
        // initialTimestamp = note.getTimestamp(); // Nếu bạn thêm timestamp vào hasUnsavedChanges

        if (alreadyAvailableNote != null) {
            note.setId(alreadyAvailableNote.getId());
            // Khi cập nhật, timestamp cũng sẽ được cập nhật thành thời gian hiện tại,
            // phản ánh thời điểm sửa đổi cuối cùng.
            noteViewModel.update(note);
        } else {
            // Đối với ghi chú mới, timestamp đã được đặt ở trên khi tạo đối tượng Note
            // hoặc có thể đặt lại ở đây nếu muốn chắc chắn.
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

                Editable editable = binding.inputNote.getEditableText();
                if (editable == null) return;

                int newCursorPos = binding.inputNote.getSelectionEnd();
                if (count > before && newCursorPos == binding.inputNote.getSelectionStart()) {
                    isFormattingText = true;
                    int charactersAddedCount = count - before;
                    int actualStart = newCursorPos - charactersAddedCount;
                    if (actualStart < 0) actualStart = 0;

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
                } else if (count < before) {
                    isFormattingText = true;
                    int actualEnd = start + before;
                    if (actualEnd > editable.length()) actualEnd = editable.length();
                    for (CharacterStyle span : editable.getSpans(start, actualEnd, CharacterStyle.class)) {
                        if (editable.getSpanStart(span) < start && editable.getSpanEnd(span) > actualEnd) {
                            Object newSpan = null;
                            if (span instanceof StyleSpan) {
                                newSpan = new StyleSpan(((StyleSpan) span).getStyle());
                            } else if (span instanceof UnderlineSpan) {
                                newSpan = new UnderlineSpan();
                            }
                            if (newSpan != null) {
                                try {
                                    editable.setSpan(newSpan, editable.getSpanStart(span), start, Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
                                    editable.setSpan(newSpan.getClass().newInstance(), actualEnd, editable.getSpanEnd(span), Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
                                } catch (Exception e) {
                                    Log.e(TAG, "Error splitting span: " + e.getMessage(), e);
                                }
                            }
                        }
                        editable.removeSpan(span);
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
                Log.d(TAG, "Add Image clicked");
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                selectImage();
            });
        } else {
            Log.e(TAG, "layoutAddImage is null. Check layout_miscellaneous.xml");
        }

        if (miscBinding.layoutAddUrl != null) {
            miscBinding.layoutAddUrl.setOnClickListener(v -> {
                Log.d(TAG, "Add URL clicked");
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                showAddURLDialog();
            });
        } else {
            Log.e(TAG, "layoutAddUrl is null. Check layout_miscellaneous.xml");
        }

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

    private void updateFormatButtonStates() {
        final LayoutMiscellaneousBinding miscBinding = binding.miscellaneousLayout;
        int activeColor = ContextCompat.getColor(this, R.color.colorAccent);
        int inactiveColor = Color.TRANSPARENT;

        miscBinding.buttonFormatBold.setBackgroundTintList(ColorStateList.valueOf(isBoldActive ? activeColor : inactiveColor));
        miscBinding.buttonFormatItalic.setBackgroundTintList(ColorStateList.valueOf(isItalicActive ? activeColor : inactiveColor));
        miscBinding.buttonFormatUnderline.setBackgroundTintList(ColorStateList.valueOf(isUnderlineActive ? activeColor : inactiveColor));
    }

    private void scheduleButtonStateUpdate() {
        handler.removeCallbacks(updateButtonStateRunnable);
        handler.postDelayed(updateButtonStateRunnable, 100);
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

        if (selectionStart != selectionEnd) {
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
                    editable.removeSpan(span);
                }
            }

            if (setActiveForTyping) {
                editable.setSpan(styleSpanToApply, selectionStart, selectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        updateFormatButtonStates();
        isFormattingText = false;
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
                        .setMessage("This permission is needed to select images from your device.")
                        .setPositiveButton("OK", (dialog, which) -> requestPermissionLauncher.launch(permissionToRequest))
                        .setNegativeButton("Cancel", null)
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
            Log.e(TAG, "Failed to parse color: " + colorToParse, e);
            gradientDrawable.setColor(Color.parseColor(DEFAULT_NOTE_COLOR));
        }
    }

    private String getPathFromUri(Uri contentUri) {
        if (contentUri == null) return "";
        if ("file".equalsIgnoreCase(contentUri.getScheme())) {
            return contentUri.getPath() != null ? contentUri.getPath() : "";
        }

        String filePath = "";
        String fileName = "image_" + System.currentTimeMillis() + ".jpg";
        try {
            InputStream inputStream = getContentResolver().openInputStream(contentUri);
            if (inputStream != null) {
                File outputFile = new File(getCacheDir(), fileName);
                FileOutputStream outputStream = new FileOutputStream(outputFile);
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                inputStream.close();
                outputStream.close();
                filePath = outputFile.getAbsolutePath();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error copying image to cache: " + e.getMessage(), e);
        }
        return filePath;
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