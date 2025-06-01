package com.example.notes.listeners;

import com.example.notes.entities.Note;

public interface NotesListener {
    void onNoteClicked(Note note, int position);

    // Nếu bạn vẫn muốn giữ lại dialog xóa từ MainActivity (ví dụ: xóa bằng long press)
    // bạn có thể gọi nó từ onNoteLongClicked trong adapter
    // Và trong listener này, gọi showDeleteNoteDialog(note);
    void onNoteLongClicked(Note note, int position);
}
