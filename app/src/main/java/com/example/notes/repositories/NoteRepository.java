package com.example.notes.repositories;

import android.app.Application;
import androidx.lifecycle.LiveData;
import com.example.notes.dao.NoteDao;
import com.example.notes.database.NoteDatabase;
import com.example.notes.entities.Note;
import com.example.notes.executors.AppExecutors; // Đảm bảo class này được triển khai đúng

import java.util.List;

public class NoteRepository {
    private final NoteDao noteDao;
    private final AppExecutors appExecutors;

    public NoteRepository(Application application) {
        NoteDatabase database = NoteDatabase.getDatabase(application);
        noteDao = database.noteDao();
        // Không khởi tạo allNotes ở đây nữa, ViewModel sẽ quyết định LiveData nào được quan sát
        appExecutors = AppExecutors.getInstance(); // Giữ nguyên cách bạn lấy instance AppExecutors
    }

    /**
     * Trả về danh sách ghi chú theo thứ tự mặc định từ DAO (ví dụ: ORDER BY id DESC).
     * ViewModel có thể sử dụng phương thức này nếu không có tiêu chí sắp xếp nào được chọn
     * hoặc cho trạng thái ban đầu.
     */
    public LiveData<List<Note>> getAllNotes() {
        return noteDao.getAllNotes(); // Giả sử getAllNotes() trong DAO là thứ tự mặc định bạn muốn
        // Hoặc nếu bạn đã đổi tên nó trong DAO thành getAllNotesDefaultOrder(),
        // thì gọi tên đó ở đây.
    }

    public void insert(Note note) {
        appExecutors.diskIO().execute(() -> noteDao.insertNote(note));
    }

    public void delete(Note note) {
        appExecutors.diskIO().execute(() -> noteDao.deleteNote(note));
    }

    public void update(Note note) {
        appExecutors.diskIO().execute(() -> noteDao.updateNote(note));
    }

    public LiveData<Note> getNoteById(int noteId) {
        return noteDao.getNoteById(noteId);
    }

    // CÁC PHƯƠNG THỨC MỚI ĐỂ HỖ TRỢ SẮP XẾP

    public LiveData<List<Note>> getAllNotesSortedByTitleAsc() {
        return noteDao.getAllNotesSortedByTitleAsc();
    }

    public LiveData<List<Note>> getAllNotesSortedByTitleDesc() {
        return noteDao.getAllNotesSortedByTitleDesc();
    }

    public LiveData<List<Note>> getAllNotesSortedByDateModifiedDesc() {
        return noteDao.getAllNotesSortedByDateModifiedDesc();
    }

    public LiveData<List<Note>> getAllNotesSortedByDateModifiedAsc() {
        return noteDao.getAllNotesSortedByDateModifiedAsc();
    }
}