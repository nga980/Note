package com.example.notes.activities;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat; // Import HtmlCompat
import androidx.lifecycle.ViewModelProvider;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build; // THÊM LẠI IMPORT NÀY
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.notes.R;
import com.example.notes.databinding.ActivityCreateNoteBinding;
import com.example.notes.databinding.LayoutMiscellaneousBinding;
import com.example.notes.entities.Note;
import com.example.notes.viewmodels.NoteViewModel;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

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

    private AlertDialog dialogAddURL;
    private AlertDialog dialogDeleteNote;

    private Note alreadyAvailableNote;
    private NoteViewModel noteViewModel;

    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<String> pickImageLauncher;

    private String initialTitle = "";
    private String initialSubtitle = "";
    private String initialNoteText = "";
    private String initialColor = "";
    private String initialImagePath = "";
    private String initialWebLink = "";
    private static final String DEFAULT_NOTE_COLOR = "#333333";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateNoteBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        noteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);

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

        binding.imageBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        binding.textDateTime.setText(
                new SimpleDateFormat("EEEE, dd MMMM yyyy HH:mm a", Locale.getDefault()) // Sửa yyyy thay cho  middelvingertje (glyph)
                        .format(new Date())
        );

        binding.imageSave.setOnClickListener(v -> saveNote());

        selectedNoteColor = DEFAULT_NOTE_COLOR;
        selectedImagePath = "";

        if (getIntent().getBooleanExtra("isViewOrUpdate", false)) {
            alreadyAvailableNote = (Note) getIntent().getSerializableExtra("note");
            setViewOrUpdateNote();
        }

        captureInitialNoteState();

        if (getIntent().hasExtra("quick_action_type")) {
            String type = getIntent().getStringExtra("quick_action_type");
            if ("image".equals(type)) {
                selectImage();
            } else if ("url".equals(type)) {
                showAddURLDialog();
            }
        }

        initMiscellaneous();
        setSubtitleIndicatorColor();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (hasUnsavedChanges()) {
                    new AlertDialog.Builder(CreateNoteActivity.this)
                            .setTitle("Unsaved Changes")
                            .setMessage("Do you want to discard your changes or keep editing?")
                            .setPositiveButton("Discard", (dialog, which) -> {
                                setEnabled(false);
                                getOnBackPressedDispatcher().onBackPressed();
                            })
                            .setNegativeButton("Keep Editing", (dialog, which) -> dialog.dismiss())
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
            // selectedNoteColor đã được cập nhật trong setViewOrUpdateNote nếu alreadyAvailableNote tồn tại
        } else {
            initialTitle = binding.inputNoteTitle.getText().toString().trim();
            initialSubtitle = binding.inputNoteSubtitle.getText().toString().trim();
            initialNoteText = binding.inputNote.getText().toString().trim();
            initialColor = selectedNoteColor;
            initialImagePath = selectedImagePath;
            initialWebLink = (binding.layoutWebURL.getVisibility() == View.VISIBLE) ? binding.textWebURL.getText().toString().trim() : "";
        }
    }

    private boolean hasUnsavedChanges() {
        String currentTitle = binding.inputNoteTitle.getText().toString().trim();
        String currentSubtitle = binding.inputNoteSubtitle.getText().toString().trim();
        String currentNoteText = binding.inputNote.getText().toString().trim();
        String currentWebLink = binding.layoutWebURL.getVisibility() == View.VISIBLE ? binding.textWebURL.getText().toString().trim() : "";

        if (!Objects.equals(currentTitle, initialTitle)) return true;
        if (!Objects.equals(currentSubtitle, initialSubtitle)) return true;
        if (!Objects.equals(currentNoteText, initialNoteText)) return true;
        if (!Objects.equals(selectedNoteColor, initialColor)) return true;
        if (!Objects.equals(selectedImagePath, initialImagePath)) return true;
        return !Objects.equals(currentWebLink, initialWebLink);
    }

    private void setViewOrUpdateNote() {
        if (alreadyAvailableNote == null) return;

        binding.inputNoteTitle.setText(alreadyAvailableNote.getTitle());
        binding.inputNoteSubtitle.setText(alreadyAvailableNote.getSubtitle());
        binding.textDateTime.setText(alreadyAvailableNote.getDate_time());

        if (alreadyAvailableNote.getNoteText() != null && !alreadyAvailableNote.getNoteText().trim().isEmpty()) {
            Spanned spannedText = HtmlCompat.fromHtml(alreadyAvailableNote.getNoteText(), HtmlCompat.FROM_HTML_MODE_LEGACY);
            binding.inputNote.setText(spannedText);
        } else {
            binding.inputNote.setText("");
        }

        if (alreadyAvailableNote.getImagePath() != null && !alreadyAvailableNote.getImagePath().trim().isEmpty()) {
            binding.imageNote.setImageBitmap(BitmapFactory.decodeFile(alreadyAvailableNote.getImagePath()));
            binding.imageNote.setVisibility(View.VISIBLE);
            binding.imageRemoveImage.setVisibility(View.VISIBLE);
            selectedImagePath = alreadyAvailableNote.getImagePath();
        }

        if (alreadyAvailableNote.getWebLink() != null && !alreadyAvailableNote.getWebLink().trim().isEmpty()) {
            binding.textWebURL.setText(alreadyAvailableNote.getWebLink());
            binding.layoutWebURL.setVisibility(View.VISIBLE);
        }

        if (alreadyAvailableNote.getColor() != null && !alreadyAvailableNote.getColor().trim().isEmpty()){
            selectedNoteColor = alreadyAvailableNote.getColor();
        } else {
            selectedNoteColor = DEFAULT_NOTE_COLOR;
        }
    }

    private void saveNote() {
        final String noteTitle = binding.inputNoteTitle.getText().toString().trim();
        String noteSubtitle = binding.inputNoteSubtitle.getText().toString().trim();
        final Editable editableNoteText = binding.inputNote.getText();
        final String noteDateTime = binding.textDateTime.getText().toString().trim();

        if (noteTitle.isEmpty()) {
            Toast.makeText(this, "Note title can't be empty!", Toast.LENGTH_SHORT).show();
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
        note.setDate_time(noteDateTime);
        note.setColor(selectedNoteColor);
        note.setImagePath(selectedImagePath);

        if (binding.layoutWebURL.getVisibility() == View.VISIBLE) {
            note.setWebLink(binding.textWebURL.getText().toString().trim());
        }

        initialTitle = noteTitle;
        initialSubtitle = noteSubtitle;
        initialNoteText = editableNoteText.toString().trim();
        initialColor = selectedNoteColor;
        initialImagePath = selectedImagePath;
        initialWebLink = note.getWebLink() != null ? note.getWebLink().trim() : "";

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

        // Đảm bảo các viewColor có tag là mã màu trong layout_miscellaneous.xml
        // Ví dụ: android:tag="#333333" cho viewColor1
        // Nếu chưa có, bạn cần thêm vào XML hoặc setTag bằng code ở đây.
        // Tuy nhiên, an toàn nhất là set trong XML vì layout editor sẽ thấy.
        // Nếu bạn chưa set tag trong XML, bạn có thể set ở đây một lần (không lý tưởng bằng XML):
        // miscBinding.viewColor1.setTag(DEFAULT_NOTE_COLOR);
        // miscBinding.viewColor2.setTag("#FDBE3B");
        // miscBinding.viewColor3.setTag("#FF4842");
        // miscBinding.viewColor4.setTag("#3A52FC");
        // miscBinding.viewColor5.setTag("#000000");


        miscBinding.viewColor1.setOnClickListener(colorClickListener);
        miscBinding.viewColor2.setOnClickListener(colorClickListener);
        miscBinding.viewColor3.setOnClickListener(colorClickListener);
        miscBinding.viewColor4.setOnClickListener(colorClickListener);
        miscBinding.viewColor5.setOnClickListener(colorClickListener);

        setCheckmarkOnSelectedColor(selectedNoteColor);

        miscBinding.buttonFormatBold.setOnClickListener(v -> applyOrRemoveStyle(new StyleSpan(Typeface.BOLD), StyleSpan.class, Typeface.BOLD));
        miscBinding.buttonFormatItalic.setOnClickListener(v -> applyOrRemoveStyle(new StyleSpan(Typeface.ITALIC), StyleSpan.class, Typeface.ITALIC));
        miscBinding.buttonFormatUnderline.setOnClickListener(v -> applyOrRemoveStyle(new UnderlineSpan(), UnderlineSpan.class, -1));

        miscBinding.layoutAddImage.setOnClickListener(v -> {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            selectImage();
        });

        miscBinding.layoutAddUrl.setOnClickListener(v -> {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            showAddURLDialog();
        });

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
            binding.imageNote.setImageBitmap(null);
            binding.imageNote.setVisibility(View.GONE);
            binding.imageRemoveImage.setVisibility(View.GONE);
            selectedImagePath = "";
        });

        binding.imageRemoveWebURL.setOnClickListener(v -> {
            binding.textWebURL.setText(null);
            binding.layoutWebURL.setVisibility(View.GONE);
        });
    }

    private void setCheckmarkOnSelectedColor(String hexColor) {
        final LayoutMiscellaneousBinding miscBinding = binding.miscellaneousLayout;
        if (hexColor == null) {
            hexColor = DEFAULT_NOTE_COLOR;
        }

        // Lấy tag từ XML hoặc gán giá trị mặc định nếu tag null
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

    private <T> void applyOrRemoveStyle(Object styleSpanToApply, Class<T> spanClassToQuery, int styleTypeToMatch) {
        Editable editable = binding.inputNote.getEditableText();
        if (editable == null) {
            return;
        }

        int selectionStart = binding.inputNote.getSelectionStart();
        int selectionEnd = binding.inputNote.getSelectionEnd();

        if (selectionStart < 0 || selectionEnd < 0 || selectionStart == selectionEnd) {
            Toast.makeText(this, "Hãy chọn một đoạn văn bản để định dạng.", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean styleExists = false;
        T[] existingSpans = editable.getSpans(selectionStart, selectionEnd, spanClassToQuery);

        for (T span : existingSpans) {
            boolean specificStyleMatch = false;
            int spanStart = editable.getSpanStart(span);
            int spanEnd = editable.getSpanEnd(span);

            if (spanStart < selectionEnd && spanEnd > selectionStart) {
                if (spanClassToQuery.isInstance(span)) {
                    if (span instanceof StyleSpan && styleTypeToMatch != -1) {
                        if (((StyleSpan) span).getStyle() == styleTypeToMatch) {
                            specificStyleMatch = true;
                        }
                    } else if (span instanceof UnderlineSpan && styleTypeToMatch == -1) {
                        specificStyleMatch = true;
                    }
                }
            }

            if (specificStyleMatch) {
                editable.removeSpan(span);
                styleExists = true;
            }
        }

        if (!styleExists) {
            editable.setSpan(styleSpanToApply, selectionStart, selectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

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
                    Toast.makeText(CreateNoteActivity.this, "Enter URL", Toast.LENGTH_SHORT).show();
                } else if (!Patterns.WEB_URL.matcher(url).matches()) {
                    Toast.makeText(CreateNoteActivity.this, "Enter valid URL", Toast.LENGTH_SHORT).show();
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
                intent.putExtra("isNoteDeleted", true);
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