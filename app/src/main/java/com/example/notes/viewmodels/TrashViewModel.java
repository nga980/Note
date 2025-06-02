package com.example.notes.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.example.notes.entities.Note;
import com.example.notes.repositories.NoteRepository;

import java.util.List;

public class TrashViewModel extends AndroidViewModel {
    private NoteRepository repository;
    private LiveData<List<Note>> trashNotes;

    public TrashViewModel(@NonNull Application application) {
        super(application);
        repository = new NoteRepository(application);
        trashNotes = repository.getTrashNotes(); // Lấy danh sách ghi chú trong thùng rác
    }

    /**
     * Trả về LiveData chứa danh sách các ghi chú trong thùng rác.
     */
    public LiveData<List<Note>> getTrashNotes() {
        return trashNotes;
    }

    /**
     * Khôi phục một ghi chú từ thùng rác.
     */
    public void restoreNote(Note note) {
        repository.restoreNoteFromTrash(note);
    }

    /**
     * Xóa vĩnh viễn một ghi chú khỏi thùng rác (và cơ sở dữ liệu).
     */
    public void permanentlyDeleteNote(Note note) {
        repository.permanentlyDeleteNote(note);
    }

    /**
     * Kích hoạt việc xóa tự động các ghi chú cũ trong thùng rác.
     * (Việc gọi định kỳ sẽ được xử lý bởi WorkManager).
     */
    public void triggerAutoDeleteOldTrashedNotes() {
        repository.autoPermanentlyDeleteOldTrashedNotes();
    }
}