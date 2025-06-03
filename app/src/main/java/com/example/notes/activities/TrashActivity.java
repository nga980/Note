package com.example.notes.activities;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.annotation.SuppressLint;
// Bỏ import android.content.Intent; nếu không dùng trực tiếp
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.notes.R;
import com.example.notes.adapters.TrashNotesAdapter;
import com.example.notes.databinding.ActivityTrashBinding;
import com.example.notes.entities.Note;
import com.example.notes.listeners.NotesListener; // Giữ lại nếu bạn vẫn dùng nó cho mục đích khác
import com.example.notes.viewmodels.TrashViewModel;

import java.util.ArrayList;
// Bỏ import java.util.List; nếu không dùng trực tiếp List ở đây

// SỬA LỖI: Thêm TrashNotesAdapter.TrashNoteListener vào implements
public class TrashActivity extends AppCompatActivity implements NotesListener, TrashNotesAdapter.TrashNoteListener {

    private ActivityTrashBinding binding;
    private TrashViewModel trashViewModel;
    private TrashNotesAdapter trashNotesAdapter;
    private static final String TAG = "TrashActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTrashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupRecyclerView();
        trashViewModel = new ViewModelProvider(this).get(TrashViewModel.class);

        trashViewModel.getTrashNotes().observe(this, notes -> {
            if (notes != null) {
                Log.d(TAG, "Trash notes updated: " + notes.size() + " items.");
                trashNotesAdapter.updateTrashNotesList(notes); // Giả sử phương thức này tồn tại trong adapter
                if (notes.isEmpty()) {
                    binding.textEmptyTrashMessage.setVisibility(View.VISIBLE);
                    binding.trashRecyclerView.setVisibility(View.GONE);
                    binding.imageEmptyTrash.setVisibility(View.GONE);
                } else {
                    binding.textEmptyTrashMessage.setVisibility(View.GONE);
                    binding.trashRecyclerView.setVisibility(View.VISIBLE);
                    binding.imageEmptyTrash.setVisibility(View.VISIBLE);
                }
            } else {
                Log.d(TAG, "Trash notes list is null.");
                binding.textEmptyTrashMessage.setVisibility(View.VISIBLE);
                binding.trashRecyclerView.setVisibility(View.GONE);
                binding.imageEmptyTrash.setVisibility(View.GONE);
            }
        });

        binding.imageBackFromTrash.setOnClickListener(v -> {
            finish();
        });

        binding.imageEmptyTrash.setOnClickListener(v -> {
            if (trashNotesAdapter.getItemCount() > 0) {
                showConfirmEmptyTrashDialog();
            } else {
                Toast.makeText(TrashActivity.this, R.string.trash_is_empty, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupRecyclerView() {
        binding.trashRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        // 'this' bây giờ là một TrashNotesAdapter.TrashNoteListener hợp lệ
        trashNotesAdapter = new TrashNotesAdapter(new ArrayList<>(), this);
        binding.trashRecyclerView.setAdapter(trashNotesAdapter);
    }

    private void showConfirmEmptyTrashDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_empty_trash)
                .setMessage(R.string.are_you_sure_empty_trash_message)
                .setPositiveButton(R.string.delete_permanently, (dialog, which) -> {
                    trashViewModel.emptyAllTrash();
                    // LiveData sẽ tự cập nhật UI.
                    // Toast này đã được thay đổi để phản ánh rằng thùng rác trống, không phải ghi chú đã được dọn sạch (vì đã trống)
                    // Nếu bạn muốn một thông báo cụ thể "Thùng rác đã được dọn sạch", hãy tạo một string resource mới.
                    Toast.makeText(TrashActivity.this, R.string.trash_is_empty, Toast.LENGTH_SHORT).show(); // Sử dụng string mới
                })
                .setNegativeButton(R.string.cancel, null)
                .setIcon(R.drawable.ic_delete)
                .show();
    }

    // Triển khai các phương thức từ NotesListener (nếu vẫn cần thiết cho mục đích khác)
    @Override
    public void onNoteClicked(Note note, int position) {
        // Hành động khi click vào note trong thùng rác (nếu có)
    }

    @Override
    public void onNoteLongClicked(Note note, int position) {
        // Hành động khi long click vào note trong thùng rác (nếu có)
    }

    // SỬA LỖI: Triển khai các phương thức từ TrashNotesAdapter.TrashNoteListener
    @Override
    public void onRestoreClicked(Note note, int position) {
        trashViewModel.restoreNote(note);
        Toast.makeText(this, R.string.note_restored, Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK); // Thông báo cho Activity trước (MainActivity) rằng có thay đổi
    }

    @SuppressLint("StringFormatInvalid") // Giữ lại nếu string resource là một format string
    @Override
    public void onPermanentlyDeleteClicked(final Note note, int position) {
        String message;
        // Đảm bảo string resource 'are_you_sure_want_to_delete_this_note_permanently' là một format string
        // hoặc xử lý trường hợp nó không phải là format string.
        try {
            message = getString(R.string.are_you_sure_want_to_delete_this_note_permanently, note.getTitle());
        } catch (Exception e) {
            // Fallback nếu string resource không đúng định dạng hoặc note.getTitle() là null
            message = getString(R.string.confirm_permanent_delete); // Hoặc một thông báo chung chung hơn
            Log.e(TAG, "Error formatting permanent delete message. Check string resource 'are_you_sure_want_to_delete_this_note_permanently'.", e);
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_permanent_delete)
                .setMessage(message)
                .setPositiveButton(R.string.delete_permanently, (dialog, whichButton) -> {
                    trashViewModel.permanentlyDeleteNote(note);
                    Toast.makeText(this, R.string.note_permanently_deleted, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .setIcon(R.drawable.ic_delete)
                .show();
    }
}