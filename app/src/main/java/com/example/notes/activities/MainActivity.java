package com.example.notes.activities;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.notes.R;
import com.example.notes.adapters.NotesAdapter;
import com.example.notes.entities.Note;
import com.example.notes.database.NoteDatabase;
import com.example.notes.listeners.NotesListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements NotesListener {
    public static final String EXTRA_QUICK_ACTION = "quick_action";
    public static final String ACTION_ADD_NOTE = "add_note";
    public static final String ACTION_ADD_IMAGE = "add_image";
    public static final String ACTION_ADD_WEB_LINK = "add_web_link";

    public RecyclerView notesRecyclerView;
    private List<Note> noteList;
    private List<Note> fullNoteList;
    private NotesAdapter notesAdapter;
    private int noteClickedPosition = -1;
    private EditText inputSearch;
    private ActivityResultLauncher<Intent> noteActivityLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        executorService = Executors.newSingleThreadExecutor();

        // Khởi tạo inputSearch
        inputSearch = findViewById(R.id.inputSearch);
        if (inputSearch == null) {
            Log.e("MainActivity", "inputSearch EditText is null");
        }

        // Khởi tạo Quick Actions
        ImageView imageAddNote = findViewById(R.id.imageAddNote);
        ImageView imageAddImage = findViewById(R.id.imageAddImage);
        ImageView imageAddWebLink = findViewById(R.id.imageAddWebLink);
        ImageView imageAddNoteMain = findViewById(R.id.imageAddNoteMain);

        // Thiết lập launcher cho quyền truy cập bộ nhớ
        setupPermissionLauncher();

        // Quick Action: Thêm ghi chú mới
        imageAddNote.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CreateNoteActivity.class);
            intent.putExtra(EXTRA_QUICK_ACTION, ACTION_ADD_NOTE);
            noteActivityLauncher.launch(intent);
        });

        // Quick Action: Thêm ghi chú với hình ảnh
        imageAddImage.setOnClickListener(v -> {
            String permissionToRequest;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                permissionToRequest = Manifest.permission.READ_MEDIA_IMAGES;
            } else {
                permissionToRequest = Manifest.permission.READ_EXTERNAL_STORAGE;
            }
            if (ContextCompat.checkSelfPermission(this, permissionToRequest) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(permissionToRequest);
            } else {
                Intent intent = new Intent(MainActivity.this, CreateNoteActivity.class);
                intent.putExtra(EXTRA_QUICK_ACTION, ACTION_ADD_IMAGE);
                noteActivityLauncher.launch(intent);
            }
        });

        // Quick Action: Thêm ghi chú với liên kết web
        imageAddWebLink.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CreateNoteActivity.class);
            intent.putExtra(EXTRA_QUICK_ACTION, ACTION_ADD_WEB_LINK);
            noteActivityLauncher.launch(intent);
        });

        // Nút thêm ghi chú chính
        imageAddNoteMain.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CreateNoteActivity.class);
            intent.putExtra(EXTRA_QUICK_ACTION, ACTION_ADD_NOTE);
            noteActivityLauncher.launch(intent);
        });

        notesRecyclerView = findViewById(R.id.noteRecyclerView);
        setupRecyclerView();

        // Thiết lập tìm kiếm
        setupSearch();

        noteActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            Log.d("MainActivity", "Thao tác ghi chú thành công, làm mới danh sách.");
                            getNotes();
                        } else if (result.getResultCode() == Activity.RESULT_CANCELED) {
                            Log.d("MainActivity", "Thao tác ghi chú đã bị hủy.");
                        }
                    }
                });

        getNotes();
    }

    private void setupPermissionLauncher() {
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        Intent intent = new Intent(MainActivity.this, CreateNoteActivity.class);
                        intent.putExtra(EXTRA_QUICK_ACTION, ACTION_ADD_IMAGE);
                        noteActivityLauncher.launch(intent);
                    } else {
                        Toast.makeText(this, "Quyền truy cập bộ nhớ bị từ chối!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onNoteClicked(Note note, int position) {
        noteClickedPosition = position;
        Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
        intent.putExtra(CreateNoteActivity.EXTRA_IS_VIEW_OR_UPDATE, true);
        intent.putExtra(CreateNoteActivity.EXTRA_NOTE, note);
        noteActivityLauncher.launch(intent);
    }

    private void setupRecyclerView() {
        notesRecyclerView.setLayoutManager(
                new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        );
        noteList = new ArrayList<>();
        fullNoteList = new ArrayList<>();
        notesAdapter = new NotesAdapter(noteList, this);
        notesRecyclerView.setAdapter(notesAdapter);
    }

    private void setupSearch() {
        if (inputSearch != null) {
            inputSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterNotes(s.toString().trim());
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void filterNotes(String query) {
        if (fullNoteList == null) {
            Log.w("MainActivity", "fullNoteList is null, cannot filter notes");
            return;
        }

        List<Note> filteredList = new ArrayList<>();
        if (query.isEmpty()) {
            filteredList.addAll(fullNoteList);
        } else {
            String lowerCaseQuery = query.toLowerCase(Locale.getDefault());
            for (Note note : fullNoteList) {
                boolean matches = false;
                if (note.getTitle() != null && note.getTitle().toLowerCase(Locale.getDefault()).contains(lowerCaseQuery)) {
                    matches = true;
                } else if (note.getSubtitle() != null && note.getSubtitle().toLowerCase(Locale.getDefault()).contains(lowerCaseQuery)) {
                    matches = true;
                } else if (note.getNoteText() != null && note.getNoteText().toLowerCase(Locale.getDefault()).contains(lowerCaseQuery)) {
                    matches = true;
                }
                if (matches) {
                    filteredList.add(note);
                }
            }
        }

        noteList.clear();
        noteList.addAll(filteredList);
        notesAdapter.notifyDataSetChanged();

        if (filteredList.isEmpty() && !query.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy ghi chú nào.", Toast.LENGTH_SHORT).show();
        }
    }

    private void getNotes() {
        Log.d("MainActivity", "Đang cố gắng lấy danh sách ghi chú từ database.");
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                final List<Note> notesFromDb;
                try {
                    notesFromDb = NoteDatabase
                            .getDatabase(getApplicationContext())
                            .noteDao().getAllNotes();
                    ContextCompat.getMainExecutor(MainActivity.this).execute(new Runnable() {
                        @Override
                        public void run() {
                            handleGetNotesResult(notesFromDb);
                        }
                    });
                } catch (Exception e) {
                    Log.e("GetNotes", "Lỗi khi lấy danh sách ghi chú từ database", e);
                    ContextCompat.getMainExecutor(MainActivity.this).execute(new Runnable() {
                        @Override
                        public void run() {
                            handleGetNotesResult(null);
                        }
                    });
                }
            }
        });
    }

    private void handleGetNotesResult(List<Note> notes) {
        if (notes != null) {
            fullNoteList.clear();
            fullNoteList.addAll(notes);
            noteList.clear();
            noteList.addAll(notes);
            notesAdapter.notifyDataSetChanged();

            if (notes.isEmpty()) {
                Log.d("MY_NOTES", "Không có ghi chú nào để hiển thị.");
                Toast.makeText(MainActivity.this, "Không tìm thấy ghi chú nào.", Toast.LENGTH_SHORT).show();
            }

            if (inputSearch != null && !inputSearch.getText().toString().trim().isEmpty()) {
                filterNotes(inputSearch.getText().toString().trim());
            }
        } else {
            Log.e("MY_NOTES", "Không thể tải danh sách ghi chú do có lỗi.");
            Toast.makeText(MainActivity.this, "Lỗi khi tải danh sách ghi chú!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            Log.d("MainActivity", "Đang tắt ExecutorService.");
            executorService.shutdown();
        }
    }
}