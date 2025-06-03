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

    public TrashViewModel(@NonNull Application application) {
        super(application);
        repository = new NoteRepository(application);
    }

    public LiveData<List<Note>> getTrashNotes() {
        return repository.getTrashNotes();
    }

    public void restoreNote(Note note) {
        repository.restoreNoteFromTrash(note);
    }

    public void permanentlyDeleteNote(Note note) {
        repository.permanentlyDeleteNote(note);
    }

    // Phương thức này có thể được gọi định kỳ bởi WorkManager hoặc một cơ chế tương tự
    public void triggerAutoDeleteOldTrashedNotes() {
        repository.autoPermanentlyDeleteOldTrashedNotes();
    }

    // >>> PHƯƠNG THỨC MỚI ĐỂ DỌN SẠCH THÙNG RÁC <<<
    public void emptyAllTrash() {
        repository.emptyAllTrash();
    }
}