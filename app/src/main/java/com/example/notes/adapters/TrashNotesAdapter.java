package com.example.notes.adapters;

import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.notes.R;
import com.example.notes.databinding.ItemContainerTrashNoteBinding; // Sử dụng ViewBinding
import com.example.notes.entities.Note;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class TrashNotesAdapter extends RecyclerView.Adapter<TrashNotesAdapter.TrashNoteViewHolder> {

    private List<Note> trashNotes;
    private TrashNoteListener trashNoteListener;
    // Không cần search nữa vì đây là danh sách cố định từ thùng rác
    // private Timer timer;
    // private List<Note> notesSource; // Không cần search

    public TrashNotesAdapter(List<Note> trashNotes, TrashNoteListener trashNoteListener) {
        this.trashNotes = trashNotes;
        this.trashNoteListener = trashNoteListener;
        // this.notesSource = trashNotes; // Không cần
    }

    public void updateTrashNotesList(List<Note> newTrashNotes) {
        this.trashNotes.clear();
        this.trashNotes.addAll(newTrashNotes);
        // this.notesSource.clear();
        // this.notesSource.addAll(newTrashNotes);
        notifyDataSetChanged();
    }


    @NonNull
    @Override
    public TrashNoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Sử dụng ViewBinding
        ItemContainerTrashNoteBinding binding = ItemContainerTrashNoteBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new TrashNoteViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TrashNoteViewHolder holder, int position) {
        holder.setNote(trashNotes.get(position));
    }

    @Override
    public int getItemCount() {
        return trashNotes.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    class TrashNoteViewHolder extends RecyclerView.ViewHolder {
        ItemContainerTrashNoteBinding binding; // Sử dụng ViewBinding

        TrashNoteViewHolder(ItemContainerTrashNoteBinding itemContainerTrashNoteBinding) {
            super(itemContainerTrashNoteBinding.getRoot());
            binding = itemContainerTrashNoteBinding;
        }

        void setNote(final Note note) {
            binding.textTrashTitle.setText(note.getTitle());
            if (note.getSubtitle() == null || note.getSubtitle().trim().isEmpty()) {
                binding.textTrashSubtitle.setVisibility(View.GONE);
            } else {
                binding.textTrashSubtitle.setText(note.getSubtitle());
                binding.textTrashSubtitle.setVisibility(View.VISIBLE);
            }

            // Định dạng thời gian xóa
            if (note.getDeletionTime() > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
                binding.textTrashDeletionTime.setText("Đã xóa: " + sdf.format(new Date(note.getDeletionTime())));
            } else {
                binding.textTrashDeletionTime.setText("Không rõ thời gian xóa");
            }


            GradientDrawable gradientDrawable = (GradientDrawable) binding.layoutTrashNote.getBackground();
            if (note.getColor() != null && !note.getColor().trim().isEmpty()) {
                try {
                    gradientDrawable.setColor(Color.parseColor(note.getColor()));
                } catch (IllegalArgumentException e) {
                    gradientDrawable.setColor(Color.parseColor("#333333")); // Màu mặc định nếu parse lỗi
                }
            } else {
                gradientDrawable.setColor(Color.parseColor("#333333"));
            }

            binding.imageRestoreNote.setOnClickListener(v -> trashNoteListener.onRestoreClicked(note, getAdapterPosition()));
            binding.imageDeletePermanently.setOnClickListener(v -> trashNoteListener.onPermanentlyDeleteClicked(note, getAdapterPosition()));
        }
    }

    // Interface để xử lý click
    public interface TrashNoteListener {
        void onRestoreClicked(Note note, int position);
        void onPermanentlyDeleteClicked(Note note, int position);
    }
}