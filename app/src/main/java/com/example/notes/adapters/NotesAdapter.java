package com.example.notes.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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

import java.util.List;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {

    private static final String TAG = "NotesAdapter";
    private List<Note> notes;
    private NotesListener notesListener;
    // Định nghĩa màu mặc định
    private static final String DEFAULT_NOTE_COLOR = "#333333";


    public NotesAdapter(List<Note> notes, NotesListener notesListener) {
        this.notes = notes;
        this.notesListener = notesListener;
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
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) { // position ở đây vẫn ổn vì nó được dùng để lấy data ban đầu
        holder.setNote(notes.get(position));
        holder.layoutNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // SỬA LỖI TẠI ĐÂY: Sử dụng holder.getAdapterPosition()
                int currentPosition = holder.getAdapterPosition();
                // Luôn kiểm tra RecyclerView.NO_POSITION trước khi sử dụng
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

            // Các log này hữu ích khi debug layout
            if (textTitle == null) Log.e(TAG, "NoteViewHolder: textTitle is null.");
            if (textSubTitle == null) Log.e(TAG, "NoteViewHolder: textSubTitle is null.");
            if (textDateTime == null) Log.e(TAG, "NoteViewHolder: textDateTime is null.");
            if (layoutNote == null) Log.e(TAG, "NoteViewHolder: layoutNote is null.");
            if (imageNoteItem == null) Log.e(TAG, "NoteViewHolder: imageNoteItem is null. Check R.id.imageNote in item_container_note.xml");
        }

        void setNote(Note note) {
            if (note == null) {
                Log.w(TAG, "Attempting to set a null note to ViewHolder");
                if (layoutNote != null) layoutNote.setVisibility(View.GONE); // Ẩn view nếu note null
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
                    // Bạn có thể muốn ẩn textDateTime nếu không có ngày giờ
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
                    parsedColor = Color.parseColor(DEFAULT_NOTE_COLOR); // Màu mặc định nếu lỗi
                }

                if (background instanceof GradientDrawable) {
                    ((GradientDrawable) background).setColor(parsedColor);
                } else {
                    layoutNote.setBackgroundColor(parsedColor);
                }
            }

            if (imageNoteItem != null) {
                if (note.getImagePath() != null && !note.getImagePath().trim().isEmpty()) {
                    // CẢI THIỆN: Việc decode bitmap nên được thực hiện ở background thread,
                    // đặc biệt nếu ảnh lớn hoặc danh sách dài.
                    // Cân nhắc dùng thư viện như Glide hoặc Picasso.
                    try {
                        Bitmap bitmap = BitmapFactory.decodeFile(note.getImagePath());
                        if (bitmap != null) {
                            imageNoteItem.setImageBitmap(bitmap);
                            imageNoteItem.setVisibility(View.VISIBLE);
                        } else {
                            Log.w(TAG, "Bitmap rỗng từ file path: " + note.getImagePath() + " cho note: " + (note.getTitle() != null ? note.getTitle() : "N/A"));
                            imageNoteItem.setVisibility(View.GONE);
                        }
                    } catch (OutOfMemoryError oom) { // Bắt lỗi cụ thể hơn
                        Log.e(TAG, "Lỗi OutOfMemoryError khi decode file ảnh: " + note.getImagePath(), oom);
                        imageNoteItem.setVisibility(View.GONE);
                        // Có thể thông báo cho người dùng hoặc giải phóng bộ nhớ nếu cần
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
    // interface NotesListener nên được định nghĩa (nếu chưa có ở file khác)
    // public interface NotesListener {
    // void onNoteClicked(Note note, int position);
    // }
}