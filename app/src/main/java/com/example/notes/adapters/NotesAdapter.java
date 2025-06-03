package com.example.notes.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView; // Thêm import này
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy; // Cho tùy chọn cache
import com.example.notes.R;
import com.example.notes.entities.Note;
import com.example.notes.listeners.NotesListener;
import com.makeramen.roundedimageview.RoundedImageView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {

    private static final String TAG = "NotesAdapter";
    private List<Note> notes;
    private NotesListener notesListener;
    private List<Note> notesSource;

    private static final String DEFAULT_NOTE_COLOR = "#333333";


    public NotesAdapter(List<Note> notes, NotesListener notesListener) {
        this.notes = notes;
        this.notesListener = notesListener;
        this.notesSource = new ArrayList<>(notes);
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.item_container_note,
                parent,
                false
        );
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        holder.setNote(notes.get(position));
        holder.layoutNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int currentPosition = holder.getBindingAdapterPosition();
                if (currentPosition != RecyclerView.NO_POSITION && notesListener != null) {
                    notesListener.onNoteClicked(notes.get(currentPosition), currentPosition);
                }
            }
        });
        holder.layoutNote.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                int currentPosition = holder.getBindingAdapterPosition();
                if (currentPosition != RecyclerView.NO_POSITION && notesListener != null) {
                    notesListener.onNoteLongClicked(notes.get(currentPosition), currentPosition);
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public int getItemCount() {
        return notes != null ? notes.size() : 0;
    }

    public void updateNotesList(List<Note> newNotes) {
        this.notes.clear();
        this.notes.addAll(newNotes);
        this.notesSource.clear();
        this.notesSource.addAll(newNotes);
        notifyDataSetChanged();
    }


    public void searchNotes(final String searchKeyword) {
        Log.d(TAG, "searchNotes called with keyword: " + searchKeyword);
        notes.clear();
        if (searchKeyword == null || searchKeyword.trim().isEmpty()) {
            Log.d(TAG, "Keyword is empty, loading all notes.");
            notes.addAll(notesSource);
        } else {
            String lowerCaseKeyword = searchKeyword.toLowerCase();
            Log.d(TAG, "Filtering with lowerCaseKeyword: " + lowerCaseKeyword);
            for (Note note : notesSource) {
                boolean matches = false;
                if (note.getTitle() != null && note.getTitle().toLowerCase().contains(lowerCaseKeyword)) {
                    matches = true;
                }
                if (!matches && note.getSubtitle() != null && note.getSubtitle().toLowerCase().contains(lowerCaseKeyword)) {
                    matches = true;
                }
                // Chuyển đổi HTML noteText sang plain text trước khi tìm kiếm
                String plainNoteText = "";
                if (note.getNoteText() != null) {
                    plainNoteText = note.getNoteText().toLowerCase();
                }

                if (!matches && !plainNoteText.isEmpty() && plainNoteText.contains(lowerCaseKeyword)) {
                    matches = true;
                }

                if (matches) {
                    notes.add(note);
                }
            }
        }
        Log.d(TAG, "Number of notes after filtering: " + notes.size());

        notifyDataSetChanged();
    }


    static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView textTitle, textSubTitle, textDateTime;
        LinearLayout layoutNote;
        RoundedImageView imageNoteItem;
        ImageView imagePinned; // <<< THÊM IMAGEVIEW CHO ICON GHIM
        Context context;

        NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            context = itemView.getContext();
            textTitle = itemView.findViewById(R.id.textTitle);
            textSubTitle = itemView.findViewById(R.id.textSubTitle);
            textDateTime = itemView.findViewById(R.id.textDateTime);
            layoutNote = itemView.findViewById(R.id.layoutNote);
            imageNoteItem = itemView.findViewById(R.id.imageNote);
            imagePinned = itemView.findViewById(R.id.imagePinned); // <<< KHỞI TẠO IMAGEVIEW GHIM

            if (textTitle == null) Log.e(TAG, "NoteViewHolder: textTitle is null.");
            if (textSubTitle == null) Log.e(TAG, "NoteViewHolder: textSubTitle is null.");
            if (textDateTime == null) Log.e(TAG, "NoteViewHolder: textDateTime is null.");
            if (layoutNote == null) Log.e(TAG, "NoteViewHolder: layoutNote is null.");
            if (imageNoteItem == null) Log.e(TAG, "NoteViewHolder: imageNoteItem is null. Check R.id.imageNote in item_container_note.xml");
            if (imagePinned == null) Log.e(TAG, "NoteViewHolder: imagePinned is null. Check R.id.imagePinned in item_container_note.xml");
        }

        void setNote(Note note) {
            if (note == null) {
                Log.w(TAG, "Attempting to set a null note to ViewHolder");
                if (layoutNote != null) layoutNote.setVisibility(View.GONE);
                return;
            }
            if (layoutNote != null) layoutNote.setVisibility(View.VISIBLE);


            if (textTitle != null) {
                textTitle.setText(note.getTitle());
            }

            if (textSubTitle != null) {
                if (note.getSubtitle() != null && !note.getSubtitle().trim().isEmpty()) {
                    textSubTitle.setText(note.getSubtitle());
                    textSubTitle.setVisibility(View.VISIBLE);
                } else {
                    textSubTitle.setVisibility(View.GONE);
                }
            }

            if (textDateTime != null) {
                if (note.getDateTime() != null && !note.getDateTime().trim().isEmpty()) {
                    textDateTime.setText(note.getDateTime());
                    textDateTime.setVisibility(View.VISIBLE);
                } else {
                    textDateTime.setVisibility(View.GONE);
                }
            }

            if (layoutNote != null) {
                Object background = layoutNote.getBackground();
                String colorStr = note.getColor();
                int parsedColor;

                try {
                    if (colorStr != null && !colorStr.trim().isEmpty()) {
                        parsedColor = Color.parseColor(colorStr);
                    } else {
                        parsedColor = Color.parseColor(DEFAULT_NOTE_COLOR);
                    }
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Invalid color code: " + colorStr, e);
                    parsedColor = Color.parseColor(DEFAULT_NOTE_COLOR);
                }

                if (background instanceof GradientDrawable) {
                    ((GradientDrawable) background).setColor(parsedColor);
                } else {
                    layoutNote.setBackgroundColor(parsedColor); // Fallback nếu background không phải GradientDrawable
                }
            }

            if (imageNoteItem != null) {
                if (note.getImagePath() != null && !note.getImagePath().trim().isEmpty()) {
                    File imageFile = new File(note.getImagePath());
                    if (imageFile.exists()) {
                        Glide.with(context)
                                .load(imageFile) // Tải từ File object
                                .placeholder(R.drawable.ic_placeholder_image)
                                .error(R.drawable.ic_placeholder_image)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .into(imageNoteItem);
                        imageNoteItem.setVisibility(View.VISIBLE);
                    } else {
                        Log.w(TAG, "Image file does not exist: " + note.getImagePath() + " for note: " + (note.getTitle() != null ? note.getTitle() : "N/A"));
                        imageNoteItem.setVisibility(View.GONE);
                    }
                } else {
                    imageNoteItem.setVisibility(View.GONE);
                    Glide.with(context).clear(imageNoteItem);
                }
            }

            // >>> HIỂN THỊ ICON GHIM <<<
            if (imagePinned != null) {
                if (note.isPinned()) {
                    imagePinned.setVisibility(View.VISIBLE);
                } else {
                    imagePinned.setVisibility(View.GONE);
                }
            }
        }
    }
}