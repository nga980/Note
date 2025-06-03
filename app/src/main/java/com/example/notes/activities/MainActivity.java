package com.example.notes.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import com.example.notes.R;
import com.example.notes.adapters.NotesAdapter;
import com.example.notes.databinding.ActivityMainBinding;
import com.example.notes.entities.Note;
import com.example.notes.listeners.NotesListener;
import com.example.notes.viewmodels.NoteViewModel;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements NotesListener {

    private NotesAdapter notesAdapter;
    private NoteViewModel noteViewModel;
    private AlertDialog dialogDeleteNote;
    private Note noteToDeleteOnClick;
    private ActivityMainBinding binding;
    private ActivityResultLauncher<Intent> createOrUpdateNoteLauncher;
    private ActivityResultLauncher<Intent> trashActivityLauncher;
    private static final String PREF_SORT_ORDER = "pref_sort_order";
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        createOrUpdateNoteLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        // Ghi chú được tạo/cập nhật thành công, LiveData sẽ tự động cập nhật danh sách
                        Log.d(TAG, "Note operation successful, LiveData should update the list.");
                    }
                });

        trashActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        // Có thể cần làm mới danh sách ghi chú ở đây nếu một ghi chú được khôi phục
                        // Tuy nhiên, với LiveData, điều này thường được xử lý tự động
                        Log.d(TAG, "Trash activity returned OK, LiveData should handle updates.");
                    }
                });

        binding.imageAddNoteMain.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
            createOrUpdateNoteLauncher.launch(intent);
        });

        binding.imageAddImage.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
            intent.putExtra("quick_action_type", "image");
            createOrUpdateNoteLauncher.launch(intent);
        });

        binding.imageAddWebLink.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
            intent.putExtra("quick_action_type", "url");
            createOrUpdateNoteLauncher.launch(intent);
        });

        binding.imageAddDrawingQuick.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
            intent.putExtra("quick_action_type", "drawing");
            createOrUpdateNoteLauncher.launch(intent);
        });

        binding.noteRecyclerView.setLayoutManager(
                new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        );

        notesAdapter = new NotesAdapter(new ArrayList<>(), this);
        binding.noteRecyclerView.setAdapter(notesAdapter);

        noteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);

        loadSortPreference(); // Load and apply sort preference

        noteViewModel.getAllNotes().observe(this, notes -> {
            if (notes != null) {
                Log.d(TAG, "Notes received: " + notes.size() + " items. Current sort: " + (noteViewModel.getCurrentSortOrderValue() != null ? noteViewModel.getCurrentSortOrderValue().name() : "null"));
                notesAdapter.updateNotesList(notes); // Cập nhật adapter với danh sách đã sắp xếp
                binding.noteRecyclerView.setVisibility(View.VISIBLE);
                binding.textNoNotes.setVisibility(notes.isEmpty() ? View.VISIBLE : View.GONE);

                // Áp dụng lại tìm kiếm nếu có từ khóa
                String currentSearchKeyword = binding.inputSearch.getText().toString();
                if (!currentSearchKeyword.isEmpty()) {
                    notesAdapter.searchNotes(currentSearchKeyword);
                }
            } else {
                Log.e(TAG, "Notes received: null");
                binding.noteRecyclerView.setVisibility(View.GONE); // Ẩn RecyclerView nếu danh sách là null
                binding.textNoNotes.setVisibility(View.VISIBLE); // Hiện thông báo không có ghi chú
            }
        });

        binding.inputSearch.addTextChangedListener(new TextWatcher() {
            private final Handler handler = new Handler(Looper.getMainLooper());
            private Runnable searchRunnable = () -> {
                if (notesAdapter != null) { // Kiểm tra null cho notesAdapter
                    notesAdapter.searchNotes(binding.inputSearch.getText().toString());
                }
            };
            private static final long SEARCH_DELAY = 300;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                handler.removeCallbacks(searchRunnable);
            }

            @Override
            public void afterTextChanged(Editable s) {
                handler.postDelayed(searchRunnable, SEARCH_DELAY);
            }
        });

        binding.imageSortNotes.setOnClickListener(this::showSortMenu);

        binding.imageGoToTrash.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, TrashActivity.class);
            trashActivityLauncher.launch(intent);
        });
    }

    private void showSortMenu(View anchorView) {
        PopupMenu popupMenu = new PopupMenu(this, anchorView);
        popupMenu.getMenuInflater().inflate(R.menu.sort_options_menu, popupMenu.getMenu());

        NoteViewModel.SortCriteria currentCriteria = noteViewModel.getCurrentSortOrderValue();
        if (currentCriteria != null) {
            if (currentCriteria == NoteViewModel.SortCriteria.TITLE_ASC) {
                popupMenu.getMenu().findItem(R.id.sort_by_name_asc).setChecked(true);
            } else if (currentCriteria == NoteViewModel.SortCriteria.TITLE_DESC) {
                popupMenu.getMenu().findItem(R.id.sort_by_name_desc).setChecked(true);
            } else if (currentCriteria == NoteViewModel.SortCriteria.DATE_MODIFIED_DESC) {
                popupMenu.getMenu().findItem(R.id.sort_by_date_newest).setChecked(true);
            } else if (currentCriteria == NoteViewModel.SortCriteria.DATE_MODIFIED_ASC) {
                popupMenu.getMenu().findItem(R.id.sort_by_date_oldest).setChecked(true);
            }
        }

        popupMenu.setOnMenuItemClickListener(item -> {
            NoteViewModel.SortCriteria selectedCriteria = null;
            int itemId = item.getItemId();
            if (itemId == R.id.sort_by_name_asc) {
                selectedCriteria = NoteViewModel.SortCriteria.TITLE_ASC;
            } else if (itemId == R.id.sort_by_name_desc) {
                selectedCriteria = NoteViewModel.SortCriteria.TITLE_DESC;
            } else if (itemId == R.id.sort_by_date_newest) {
                selectedCriteria = NoteViewModel.SortCriteria.DATE_MODIFIED_DESC;
            } else if (itemId == R.id.sort_by_date_oldest) {
                selectedCriteria = NoteViewModel.SortCriteria.DATE_MODIFIED_ASC;
            }

            if (selectedCriteria != null && selectedCriteria != currentCriteria) {
                noteViewModel.setSortOrder(selectedCriteria); // Điều này sẽ kích hoạt switchMap và LiveData
                saveSortPreference(selectedCriteria); // Lưu lựa chọn
                // Không cần gọi notifyDataSetChanged ở đây vì LiveData sẽ làm điều đó
                Log.d(TAG, "Sort order changed to: " + selectedCriteria.name());
            }
            // Không cần thiết lập setChecked(true) ở đây vì menu sẽ đóng lại.
            // Trạng thái checked sẽ được thiết lập lại khi menu được mở lần sau.
            return true;
        });
        popupMenu.show();
    }

    private void saveSortPreference(NoteViewModel.SortCriteria criteria) {
        SharedPreferences prefs = getSharedPreferences("NotePrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_SORT_ORDER, criteria.name());
        editor.apply();
        Log.d(TAG, "Saved sort preference: " + criteria.name());
    }

    private void loadSortPreference() {
        SharedPreferences prefs = getSharedPreferences("NotePrefs", MODE_PRIVATE);
        String defaultSortOrder = NoteViewModel.SortCriteria.DATE_MODIFIED_DESC.name();
        String sortOrderName = prefs.getString(PREF_SORT_ORDER, defaultSortOrder);
        try {
            NoteViewModel.SortCriteria criteria = NoteViewModel.SortCriteria.valueOf(sortOrderName);
            noteViewModel.setSortOrder(criteria); // Áp dụng tùy chọn đã lưu
            Log.d(TAG, "Loaded sort preference: " + criteria.name());
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid sort preference: " + sortOrderName + ", defaulting to " + defaultSortOrder, e);
            NoteViewModel.SortCriteria criteria = NoteViewModel.SortCriteria.DATE_MODIFIED_DESC;
            noteViewModel.setSortOrder(criteria); // Đặt lại về mặc định nếu có lỗi
            saveSortPreference(criteria); // Và lưu lại giá trị mặc định này
        }
    }

    @Override
    public void onNoteClicked(Note note, int position) {
        Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
        intent.putExtra("isViewOrUpdate", true);
        intent.putExtra("note", note);
        createOrUpdateNoteLauncher.launch(intent);
    }

    @Override
    public void onNoteLongClicked(Note note, int position) {
        noteToDeleteOnClick = note;
        showDeleteNoteDialog();
    }

    private void showDeleteNoteDialog() {
        if (dialogDeleteNote == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            View view = LayoutInflater.from(this).inflate(R.layout.layout_delete_note, null);
            builder.setView(view);
            dialogDeleteNote = builder.create();
            if (dialogDeleteNote.getWindow() != null) {
                dialogDeleteNote.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }
            view.findViewById(R.id.textDeleteNote).setOnClickListener(v -> {
                if (noteToDeleteOnClick != null) {
                    noteViewModel.delete(noteToDeleteOnClick); // Sẽ di chuyển vào thùng rác
                    Log.d(TAG, "Note moved to trash: " + noteToDeleteOnClick.getTitle());
                    noteToDeleteOnClick = null; // Đặt lại để tránh xóa nhầm
                }
                if (dialogDeleteNote != null && dialogDeleteNote.isShowing()) {
                    dialogDeleteNote.dismiss();
                }
            });
            view.findViewById(R.id.textCancel).setOnClickListener(v -> {
                noteToDeleteOnClick = null; // Đặt lại nếu hủy
                if (dialogDeleteNote != null && dialogDeleteNote.isShowing()) {
                    dialogDeleteNote.dismiss();
                }
            });
        }
        // Đảm bảo dialog hiển thị đúng ghi chú nếu nó được tái sử dụng
        // (Mặc dù trong trường hợp này, nó có thể không cần thiết vì noteToDeleteOnClick được cập nhật mỗi lần)
        dialogDeleteNote.show();
    }
}