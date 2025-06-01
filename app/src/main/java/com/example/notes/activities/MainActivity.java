package com.example.notes.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu; // THÊM IMPORT NÀY
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences; // THÊM IMPORT NÀY
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager; // THÊM IMPORT NÀY (hoặc androidx.preference.PreferenceManagerCompat)
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.example.notes.R;
import com.example.notes.adapters.NotesAdapter;
import com.example.notes.databinding.ActivityMainBinding;
import com.example.notes.entities.Note;
import com.example.notes.listeners.NotesListener;
import com.example.notes.viewmodels.NoteViewModel;

import java.util.ArrayList;
import java.util.List; // Đảm bảo import này tồn tại

public class MainActivity extends AppCompatActivity implements NotesListener {

    private NotesAdapter notesAdapter;
    private NoteViewModel noteViewModel;

    private AlertDialog dialogDeleteNote;
    private Note noteToDeleteOnClick;

    private ActivityMainBinding binding;
    private ActivityResultLauncher<Intent> createOrUpdateNoteLauncher;

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
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // Không cần xử lý data.getStringExtra("action_performed") cụ thể ở đây nữa
                        // vì LiveData sẽ tự động cập nhật danh sách.
                        // Toast.makeText(MainActivity.this, "Operation successful!", Toast.LENGTH_SHORT).show();
                        // Logic cuộn có thể vẫn hữu ích nếu bạn thêm note mới và muốn xem nó ngay
                        if (result.getData() != null && "added".equals(result.getData().getStringExtra("action_performed"))) {
                            if (notesAdapter.getItemCount() > 0) {
                                binding.noteRecyclerView.smoothScrollToPosition(0);
                            }
                        }
                    }
                });

        binding.imageAddNoteMain.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
            createOrUpdateNoteLauncher.launch(intent);
        });

        // Xử lý Quick Actions
        binding.imageAddNote.setOnClickListener(v -> binding.imageAddNoteMain.performClick());
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


        binding.noteRecyclerView.setLayoutManager(
                new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        );

        // Khởi tạo adapter với danh sách rỗng ban đầu
        notesAdapter = new NotesAdapter(new ArrayList<>(), this);
        binding.noteRecyclerView.setAdapter(notesAdapter);

        noteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);

        // Tải và áp dụng tùy chọn sắp xếp đã lưu (hoặc mặc định)
        loadSortPreference(); // Gọi trước khi observe allNotes

        noteViewModel.getAllNotes().observe(this, notes -> {
            if (notes != null) {
                Log.d(TAG, "Notes received: " + notes.size() + " items. Current sort: " + (noteViewModel.getCurrentSortOrderValue() != null ? noteViewModel.getCurrentSortOrderValue().name() : "null"));
                notesAdapter.updateNotesList(notes);
                binding.noteRecyclerView.setVisibility(notes.isEmpty() ? View.GONE : View.VISIBLE);

                // Áp dụng lại tìm kiếm nếu có
                String currentSearchKeyword = binding.inputSearch.getText().toString();
                if (!currentSearchKeyword.isEmpty()) {
                    notesAdapter.searchNotes(currentSearchKeyword);
                }
            } else {
                Log.d(TAG, "Notes received: null");
                binding.noteRecyclerView.setVisibility(View.GONE);
            }
        });

        binding.inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (notesAdapter != null) { // Kiểm tra null cho notesAdapter
                    notesAdapter.cancelTimer();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (notesAdapter != null) { // Kiểm tra null cho notesAdapter
                    notesAdapter.searchNotes(s.toString());
                }
            }
        });

        // Listener cho nút sắp xếp mới (imageSortNotes từ activity_main.xml)
        binding.imageSortNotes.setOnClickListener(this::showSortMenu);
    }

    private void showSortMenu(View anchorView) {
        PopupMenu popupMenu = new PopupMenu(this, anchorView);
        popupMenu.getMenuInflater().inflate(R.menu.sort_options_menu, popupMenu.getMenu());

        // Đánh dấu mục menu hiện tại đang được chọn (tùy chọn)
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
            NoteViewModel.SortCriteria selectedCriteria = noteViewModel.getCurrentSortOrderValue(); // Giữ lại tiêu chí hiện tại nếu không có gì được chọn
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

            if (selectedCriteria != null) {
                noteViewModel.setSortOrder(selectedCriteria);
                saveSortPreference(selectedCriteria);
                item.setChecked(true); // Đánh dấu mục được chọn
            }
            return true;
        });
        popupMenu.show();
    }

    private void saveSortPreference(NoteViewModel.SortCriteria criteria) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_SORT_ORDER, criteria.name());
        editor.apply();
        Log.d(TAG, "Saved sort preference: " + criteria.name());
    }

    private void loadSortPreference() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        // Mặc định là sắp xếp theo ngày sửa đổi mới nhất nếu chưa có gì được lưu
        String defaultSortOrderName = NoteViewModel.SortCriteria.DATE_MODIFIED_DESC.name();
        String sortOrderName = prefs.getString(PREF_SORT_ORDER, defaultSortOrderName);
        NoteViewModel.SortCriteria criteria;
        try {
            criteria = NoteViewModel.SortCriteria.valueOf(sortOrderName);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid sort preference found: " + sortOrderName + ", defaulting to " + defaultSortOrderName, e);
            criteria = NoteViewModel.SortCriteria.DATE_MODIFIED_DESC; // Mặc định an toàn
            saveSortPreference(criteria); // Lưu lại giá trị mặc định nếu giá trị cũ bị lỗi
        }
        noteViewModel.setSortOrder(criteria);
        Log.d(TAG, "Loaded sort preference: " + criteria.name());
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
                if (noteToDeleteOnClick != null) {
                    noteViewModel.delete(noteToDeleteOnClick);
                    // Toast đã bị comment ở bản gốc, LiveData sẽ tự cập nhật UI
                }
                if (dialogDeleteNote != null && dialogDeleteNote.isShowing()) {
                    dialogDeleteNote.dismiss();
                }
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