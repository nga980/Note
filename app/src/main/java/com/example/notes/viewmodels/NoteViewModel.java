package com.example.notes.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData; // THÊM IMPORT NÀY
import androidx.lifecycle.Transformations; // THÊM IMPORT NÀY

import com.example.notes.entities.Note;
import com.example.notes.repositories.NoteRepository;

import java.util.List;

public class NoteViewModel extends AndroidViewModel {
    private NoteRepository repository;
    private LiveData<List<Note>> allNotes; // Sẽ được cập nhật qua Transformations.switchMap

    // Enum để định nghĩa các tiêu chí sắp xếp
    public enum SortCriteria {
        DATE_MODIFIED_DESC, // Sắp xếp theo ngày sửa đổi mới nhất (mặc định)
        DATE_MODIFIED_ASC,  // Sắp xếp theo ngày sửa đổi cũ nhất
        TITLE_ASC,          // Sắp xếp theo tên A-Z
        TITLE_DESC          // Sắp xếp theo tên Z-A
    }

    private MutableLiveData<SortCriteria> currentSortOrder = new MutableLiveData<>();

    public NoteViewModel(@NonNull Application application) {
        super(application);
        repository = new NoteRepository(application);

        // Đặt giá trị mặc định cho tiêu chí sắp xếp TRƯỚC KHI thiết lập allNotes
        // Điều này đảm bảo allNotes được khởi tạo với một truy vấn hợp lệ.
        if (currentSortOrder.getValue() == null) {
            currentSortOrder.setValue(SortCriteria.DATE_MODIFIED_DESC); // Mặc định là ngày sửa đổi mới nhất
        }

        // Sử dụng Transformations.switchMap để thay đổi nguồn LiveData dựa trên currentSortOrder
        allNotes = Transformations.switchMap(currentSortOrder, sortCriteria -> {
            if (sortCriteria == null) {
                // Trường hợp an toàn, trả về sắp xếp mặc định nếu sortCriteria là null
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
                default: // Bao gồm cả trường hợp mặc định
                    return repository.getAllNotesSortedByDateModifiedDesc();
            }
        });
    }

    /**
     * Trả về LiveData chứa danh sách các ghi chú đã được sắp xếp.
     * LiveData này sẽ tự động cập nhật khi tiêu chí sắp xếp thay đổi.
     */
    public LiveData<List<Note>> getAllNotes() {
        return allNotes;
    }

    /**
     * Thiết lập tiêu chí sắp xếp mới.
     * Việc này sẽ kích hoạt Transformations.switchMap để cập nhật LiveData allNotes.
     * @param sortCriteria Tiêu chí sắp xếp mới.
     */
    public void setSortOrder(SortCriteria sortCriteria) {
        currentSortOrder.setValue(sortCriteria);
    }

    /**
     * Lấy giá trị tiêu chí sắp xếp hiện tại.
     * @return Tiêu chí sắp xếp đang được áp dụng.
     */
    public SortCriteria getCurrentSortOrderValue() {
        return currentSortOrder.getValue();
    }


    public void insert(Note note) {
        repository.insert(note);
    }

    public void delete(Note note) {
        repository.delete(note);
    }

    public void update(Note note) {
        repository.update(note);
    }

    // Phương thức getNoteById vẫn giữ nguyên, không liên quan đến sắp xếp danh sách
    public LiveData<Note> getNoteById(int noteId) {
        return repository.getNoteById(noteId);
    }
}