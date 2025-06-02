package com.example.notes.activities;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager; // Hoặc StaggeredGridLayoutManager nếu muốn
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.notes.R;
import com.example.notes.adapters.TrashNotesAdapter;
import com.example.notes.databinding.ActivityTrashBinding; // Sử dụng ViewBinding
import com.example.notes.entities.Note;
import com.example.notes.viewmodels.TrashViewModel;

import java.util.ArrayList;
import java.util.List;

public class TrashActivity extends AppCompatActivity implements TrashNotesAdapter.TrashNoteListener {

    private ActivityTrashBinding binding;
    private TrashViewModel trashViewModel;
    private TrashNotesAdapter trashNotesAdapter;
    private List<Note> deletedNotesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTrashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.imageBackFromTrash.setOnClickListener(v -> {
            getOnBackPressedDispatcher().onBackPressed();
        });

        setupRecyclerView();

        deletedNotesList = new ArrayList<>();
        trashNotesAdapter = new TrashNotesAdapter(deletedNotesList, this);
        binding.trashRecyclerView.setAdapter(trashNotesAdapter);

        trashViewModel = new ViewModelProvider(this).get(TrashViewModel.class);
        trashViewModel.getTrashNotes().observe(this, notes -> {
            if (notes != null && !notes.isEmpty()) {
                deletedNotesList.clear();
                deletedNotesList.addAll(notes);
                trashNotesAdapter.notifyDataSetChanged(); // Hoặc dùng DiffUtil
                binding.trashRecyclerView.setVisibility(View.VISIBLE);
                binding.textEmptyTrashMessage.setVisibility(View.GONE);
                binding.imageEmptyTrash.setVisibility(View.VISIBLE); // Hiện nút dọn rác
            } else {
                deletedNotesList.clear();
                trashNotesAdapter.notifyDataSetChanged();
                binding.trashRecyclerView.setVisibility(View.GONE);
                binding.textEmptyTrashMessage.setVisibility(View.VISIBLE);
                binding.imageEmptyTrash.setVisibility(View.GONE); // Ẩn nút dọn rác
            }
        });

        // Xử lý nút dọn sạch thùng rác (nếu bạn muốn thêm nút này và logic cho nó)
        binding.imageEmptyTrash.setOnClickListener(v -> {
            // Hỏi xác nhận trước khi dọn sạch toàn bộ thùng rác
            new AlertDialog.Builder(this)
                    .setTitle(R.string.empty_trash)
                    .setMessage(R.string.confirm_empty_trash)
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                        // Gọi một phương thức trong ViewModel để xóa tất cả các note trong thùng rác
                        // trashViewModel.emptyAllTrash(); // Bạn cần thêm phương thức này vào ViewModel và Repository/DAO
                        Toast.makeText(TrashActivity.this, "Chức năng này cần được triển khai trong ViewModel/Repository", Toast.LENGTH_LONG).show();
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .setIcon(R.drawable.ic_delete)
                    .show();
        });
    }

    private void setupRecyclerView() {
        binding.trashRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        // Bạn cũng có thể dùng StaggeredGridLayoutManager nếu muốn
        // binding.trashRecyclerView.setLayoutManager(
        //        new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        // );
    }

    @Override
    public void onRestoreClicked(Note note, int position) {
        trashViewModel.restoreNote(note);
        // LiveData sẽ tự động cập nhật danh sách, adapter sẽ được thông báo
        Toast.makeText(this, getString(R.string.note_restored) + ": " + note.getTitle(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPermanentlyDeleteClicked(final Note note, int position) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_permanently)
                .setMessage(R.string.confirm_permanent_delete)
                .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                    trashViewModel.permanentlyDeleteNote(note);
                    // LiveData sẽ tự động cập nhật danh sách
                    Toast.makeText(this, getString(R.string.note_permanently_deleted) + ": " + note.getTitle(), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.no, null).show();
    }
}