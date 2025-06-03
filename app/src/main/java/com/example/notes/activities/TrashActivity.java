package com.example.notes.activities;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.notes.R;
import com.example.notes.adapters.TrashNotesAdapter;
import com.example.notes.databinding.ActivityTrashBinding;
import com.example.notes.entities.Note;
import com.example.notes.listeners.NotesListener;
import com.example.notes.viewmodels.TrashViewModel;

import java.util.ArrayList;

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
                trashNotesAdapter.updateTrashNotesList(notes);
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
        trashNotesAdapter = new TrashNotesAdapter(new ArrayList<>(), this);
        binding.trashRecyclerView.setAdapter(trashNotesAdapter);
    }

    private void showConfirmEmptyTrashDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_empty_trash)
                .setMessage(R.string.are_you_sure_empty_trash_message) // Sử dụng chuỗi đã định nghĩa
                .setPositiveButton(R.string.delete_permanently, (dialog, which) -> {
                    trashViewModel.emptyAllTrash();
                    Toast.makeText(TrashActivity.this, R.string.trash_bin_emptied, Toast.LENGTH_SHORT).show(); // Sử dụng chuỗi mới
                })
                .setNegativeButton(R.string.cancel, null)
                .setIcon(R.drawable.ic_delete)
                .show();
    }

    @Override
    public void onNoteClicked(Note note, int position) {
        // Hiện tại không có hành động
    }

    @Override
    public void onNoteLongClicked(Note note, int position) {
        // Hiện tại không có hành động
    }

    @Override
    public void onRestoreClicked(Note note, int position) {
        trashViewModel.restoreNote(note);
        Toast.makeText(this, R.string.note_restored, Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
    }

    @Override
    public void onPermanentlyDeleteClicked(final Note note, int position) {
        String message;
        String noteTitle = note.getTitle();
        if (noteTitle != null && !noteTitle.isEmpty()) {
            message = getString(R.string.confirm_permanent_delete_message_with_title, noteTitle);
        } else {
            message = getString(R.string.dialog_message_confirm_permanent_delete); // Chuỗi chung khi không có tiêu đề
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