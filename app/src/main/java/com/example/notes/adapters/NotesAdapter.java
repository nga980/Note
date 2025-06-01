package com.example.notes.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler; // Thêm import này
import android.os.Looper;  // Thêm import này
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.notes.R;
import com.example.notes.entities.Note;
import com.example.notes.listeners.NotesListener;
import com.makeramen.roundedimageview.RoundedImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {

    private static final String TAG = "NotesAdapter";
    private List<Note> notes;
    private NotesListener notesListener;
    // Thêm danh sách này để giữ bản gốc của tất cả các ghi chú
    private List<Note> notesSource;

    // Định nghĩa màu mặc định
    private static final String DEFAULT_NOTE_COLOR = "#333333";

    private Timer timer;


    public NotesAdapter(List<Note> notes, NotesListener notesListener) {
        this.notes = notes;
        this.notesListener = notesListener;
        this.notesSource = new ArrayList<>(notes); // <<<< KHỞI TẠO notesSource
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
    }

    @Override
    public int getItemCount() {
        return notes != null ? notes.size() : 0;
    }

    // Phương thức để cập nhật danh sách notes từ bên ngoài (ví dụ từ MainActivity sau khi fetch từ DB)
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
                // Bạn có thể muốn tìm kiếm cả trong note.getNoteText() nữa
                if (!matches && note.getNoteText() != null && note.getNoteText().toLowerCase().contains(lowerCaseKeyword)) {
                    matches = true;
                }

                if (matches) {
                    notes.add(note);
                }
            }
        }
        Log.d(TAG, "Number of notes after filtering: " + notes.size());
        // Sử dụng Handler để đảm bảo notifyDataSetChanged chạy trên Main Thread
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
            }
        });
    }


    public void cancelTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
            Log.d(TAG, "Timer cancelled in adapter.");
        }
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView textTitle, textSubTitle, textDateTime;
        LinearLayout layoutNote;
        RoundedImageView imageNoteItem;

        NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.textTitle);
            textSubTitle = itemView.findViewById(R.id.textSubTitle);
            textDateTime = itemView.findViewById(R.id.textDateTime);
            layoutNote = itemView.findViewById(R.id.layoutNote);
            imageNoteItem = itemView.findViewById(R.id.imageNote);

            if (textTitle == null) Log.e(TAG, "NoteViewHolder: textTitle is null.");
            if (textSubTitle == null) Log.e(TAG, "NoteViewHolder: textSubTitle is null.");
            if (textDateTime == null) Log.e(TAG, "NoteViewHolder: textDateTime is null.");
            if (layoutNote == null) Log.e(TAG, "NoteViewHolder: layoutNote is null.");
            if (imageNoteItem == null) Log.e(TAG, "NoteViewHolder: imageNoteItem is null. Check R.id.imageNote in item_container_note.xml");
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
                if (note.getDate_time() != null && !note.getDate_time().trim().isEmpty()) {
                    textDateTime.setText(note.getDate_time());
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
                    Log.e(TAG, "Mã màu không hợp lệ: " + colorStr, e);
                    parsedColor = Color.parseColor(DEFAULT_NOTE_COLOR);
                }

                if (background instanceof GradientDrawable) {
                    ((GradientDrawable) background).setColor(parsedColor);
                } else {
                    layoutNote.setBackgroundColor(parsedColor);
                }
            }

            if (imageNoteItem != null) {
                if (note.getImagePath() != null && !note.getImagePath().trim().isEmpty()) {
                    try {
                        Bitmap bitmap = BitmapFactory.decodeFile(note.getImagePath());
                        if (bitmap != null) {
                            imageNoteItem.setImageBitmap(bitmap);
                            imageNoteItem.setVisibility(View.VISIBLE);
                        } else {
                            Log.w(TAG, "Bitmap rỗng từ file path: " + note.getImagePath() + " cho note: " + (note.getTitle() != null ? note.getTitle() : "N/A"));
                            imageNoteItem.setVisibility(View.GONE);
                        }
                    } catch (OutOfMemoryError oom) {
                        Log.e(TAG, "Lỗi OutOfMemoryError khi decode file ảnh: " + note.getImagePath(), oom);
                        imageNoteItem.setVisibility(View.GONE);
                    } catch (Exception e) {
                        Log.e(TAG, "Lỗi khác khi decode file ảnh: " + note.getImagePath(), e);
                        imageNoteItem.setVisibility(View.GONE);
                    }
                } else {
                    imageNoteItem.setVisibility(View.GONE);
                }
            }
        }
    }
}