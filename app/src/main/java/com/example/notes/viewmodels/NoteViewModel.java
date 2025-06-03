package com.example.notes.viewmodels;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import com.example.notes.entities.Note;
import com.example.notes.repositories.NoteRepository;
import java.util.List;

public class NoteViewModel extends AndroidViewModel {
    public enum SortCriteria {
        TITLE_ASC, TITLE_DESC, DATE_MODIFIED_DESC, DATE_MODIFIED_ASC
    }

    private NoteRepository repository;
    private MutableLiveData<SortCriteria> currentSortOrderLiveData = new MutableLiveData<>();
    private LiveData<List<Note>> allNotes;

    public NoteViewModel(Application application) {
        super(application);
        repository = new NoteRepository(application);
        // Đặt thứ tự sắp xếp mặc định. MainActivity sẽ tải tùy chọn của người dùng và có thể ghi đè giá trị này.
        currentSortOrderLiveData.setValue(SortCriteria.DATE_MODIFIED_DESC);

        allNotes = Transformations.switchMap(currentSortOrderLiveData, criteria -> {
            if (criteria == null) { // Lý tưởng nhất là không xảy ra nếu giá trị mặc định được đặt
                return repository.getAllNotesSortedByDateModifiedDesc();
            }
            switch (criteria) {
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
        return allNotes;
    }

    public SortCriteria getCurrentSortOrderValue() {
        return currentSortOrderLiveData.getValue();
    }

    public void setSortOrder(SortCriteria criteria) {
        currentSortOrderLiveData.setValue(criteria);
    }

    public void delete(Note note) {
        repository.delete(note); // Lệnh này gọi moveToTrash trong repository
    }

    public void insert(Note note) {
        repository.insert(note);
    }

    public void update(Note note) {
        repository.update(note);
    }
}