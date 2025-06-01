package com.example.notes.activities;

import androidx.activity.result.ActivityResultLauncher; // Thêm import này
import androidx.activity.result.contract.ActivityResultContracts; // Thêm import này
// Vẫn cần cho các mục đích khác nếu có, nhưng không cho onActivityResult
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.app.Activity; // Thêm import này cho Activity.RESULT_OK
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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

public class MainActivity extends AppCompatActivity implements NotesListener {

    private NotesAdapter notesAdapter;
    private NoteViewModel noteViewModel;

    private AlertDialog dialogDeleteNote;
    private Note noteToDeleteOnClick;

    private ActivityMainBinding binding;

    // Khai báo ActivityResultLauncher
    private ActivityResultLauncher<Intent> createOrUpdateNoteLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Khởi tạo createOrUpdateNoteLauncher
        createOrUpdateNoteLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        String actionPerformed = (data != null) ? data.getStringExtra("action_performed") : null;

                        if ("deleted".equals(actionPerformed)) {
                            Toast.makeText(MainActivity.this, "Note deleted", Toast.LENGTH_SHORT).show();
                        } else if ("added".equals(actionPerformed)) {
                            Toast.makeText(MainActivity.this, "Note added", Toast.LENGTH_SHORT).show();
                            // LiveData sẽ cập nhật adapter, sau đó kiểm tra itemCount
                            // Việc cuộn có thể cần một chút trì hoãn nhỏ hoặc lắng nghe thay đổi của adapter
                            if (notesAdapter.getItemCount() > 0) {
                                binding.noteRecyclerView.smoothScrollToPosition(0);
                            }
                        } else if ("updated".equals(actionPerformed)) {
                            Toast.makeText(MainActivity.this, "Note updated", Toast.LENGTH_SHORT).show();
                        } else {
                            // Trường hợp chung nếu không có action_performed (không nên xảy ra nếu CreateNoteActivity được cập nhật đúng)
                            // Hoặc nếu CreateNoteActivity chỉ trả về RESULT_OK mà không có data.
                            Toast.makeText(MainActivity.this, "Operation successful!", Toast.LENGTH_SHORT).show();
                        }
                        // Danh sách sẽ tự động cập nhật qua LiveData observer
                    }
                });

        binding.imageAddNoteMain.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
            // Không cần đặt extra "isViewOrUpdate" vì mặc định là false (thêm mới)
            createOrUpdateNoteLauncher.launch(intent); // Sử dụng launcher mới
        });

        binding.noteRecyclerView.setLayoutManager(
                new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        );

        notesAdapter = new NotesAdapter(new ArrayList<>(), this);
        binding.noteRecyclerView.setAdapter(notesAdapter);

        noteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);
        noteViewModel.getAllNotes().observe(this, notes -> {
            if (notes != null) {
                notesAdapter.updateNotesList(notes);
                if (notesAdapter.getItemCount() == 0) {
                    binding.noteRecyclerView.setVisibility(View.GONE);
                } else {
                    binding.noteRecyclerView.setVisibility(View.VISIBLE);
                }
                // Áp dụng lại tìm kiếm nếu có
                String currentSearchKeyword = binding.inputSearch.getText().toString();
                if (!currentSearchKeyword.isEmpty()) { // Chỉ tìm khi có từ khoá
                    notesAdapter.searchNotes(currentSearchKeyword);
                }
            }
        });

        binding.inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                notesAdapter.cancelTimer();
            }

            @Override
            public void afterTextChanged(Editable s) {
                notesAdapter.searchNotes(s.toString());
            }
        });
    }

    @Override
    public void onNoteClicked(Note note, int position) {
        Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
        intent.putExtra("isViewOrUpdate", true);
        intent.putExtra("note", note);
        createOrUpdateNoteLauncher.launch(intent); // Sử dụng launcher mới
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
                    null // (ViewGroup) findViewById(R.id.layoutDeleteNoteContainer)
            );
            builder.setView(view);
            dialogDeleteNote = builder.create();
            if (dialogDeleteNote.getWindow() != null) {
                dialogDeleteNote.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }
            view.findViewById(R.id.textDeleteNote).setOnClickListener(v -> {
                if (noteToDeleteOnClick != null) {
                    noteViewModel.delete(noteToDeleteOnClick);
                    // LiveData sẽ tự động cập nhật UI và hiển thị Toast nếu cần
                     Toast.makeText(this, "Note deleted by long press", Toast.LENGTH_SHORT).show(); // Không cần nữa vì LiveData sẽ xử lý
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