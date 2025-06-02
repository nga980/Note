package com.example.notes.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.notes.entities.Note;
import com.example.notes.repositories.NoteRepository;

import java.util.List;

public class NoteViewModel extends AndroidViewModel {
    private NoteRepository repository;
    private LiveData<List<Note>> allNotes;

    public enum SortCriteria {
        DATE_MODIFIED_DESC,
        DATE_MODIFIED_ASC,
        TITLE_ASC,
        TITLE_DESC
    }

    private MutableLiveData<SortCriteria> currentSortOrder = new MutableLiveData<>();

    public NoteViewModel(@NonNull Application application) {
        super(application);
        repository = new NoteRepository(application);

        if (currentSortOrder.getValue() == null) {
            currentSortOrder.setValue(SortCriteria.DATE_MODIFIED_DESC);
        }

        allNotes = Transformations.switchMap(currentSortOrder, sortCriteria -> {
            if (sortCriteria == null) {
                return repository.getAllNotesSortedByDateModifiedDesc();
            }
            switch (sortCriteria) {
                case TITLE_ASC:
                    return repository.getAllNotesSortedByTitleAsc();
                case TITLE_DESC:
                    return repository.getAllNotesSortedByTitleDesc();
                case DATE_MODIFIED_ASC:
                    return repository.getAllNotesSortedByDateModifiedAsc();
                case DATE_MODIFIED_DESC:
                default:
                    return repository.getAllNotesSortedByDateModifiedDesc();
            }
        });
    }

    public LiveData<List<Note>> getAllNotes() {
        return allNotes; // LiveData này đã tự động lọc is_deleted = 0 từ Repository/DAO
    }

    public void setSortOrder(SortCriteria sortCriteria) {
        currentSortOrder.setValue(sortCriteria);
    }

    public SortCriteria getCurrentSortOrderValue() {
        return currentSortOrder.getValue();
    }

    public void insert(Note note) {
        repository.insert(note);
    }

    /**
     * Phương thức này giờ sẽ di chuyển ghi chú vào thùng rác,
     * vì repository.delete() đã được cập nhật để gọi moveToTrash().
     */
    public void delete(Note note) {
        repository.delete(note); // repository.delete() thực chất là moveToTrash()
    }

    public void update(Note note) {
        repository.update(note);
    }

    public LiveData<Note> getNoteById(int noteId) {
        // Repository.getNoteById() cũng nên chỉ trả về note active
        return repository.getNoteById(noteId);
    }
}