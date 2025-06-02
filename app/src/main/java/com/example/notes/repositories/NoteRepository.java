package com.example.notes.repositories;

import android.app.Application;
import androidx.lifecycle.LiveData;
import com.example.notes.dao.NoteDao;
import com.example.notes.database.NoteDatabase;
import com.example.notes.entities.Note;
import com.example.notes.executors.AppExecutors;

import java.util.Calendar; // THÊM IMPORT NÀY
import java.util.List;

public class NoteRepository {
    private final NoteDao noteDao;
    private final AppExecutors appExecutors;

    public NoteRepository(Application application) {
        NoteDatabase database = NoteDatabase.getDatabase(application);
        noteDao = database.noteDao();
        appExecutors = AppExecutors.getInstance();
    }

    // Phương thức getAllNotes() và các phương thức sắp xếp hiện tại
    // sẽ tự động chỉ lấy các ghi chú active nếu bạn đã thêm "WHERE is_deleted = 0"
    // vào các truy vấn tương ứng trong NoteDao.java.

    public LiveData<List<Note>> getAllNotes() {
        return noteDao.getAllNotes(); // Giả sử đã cập nhật trong DAO để chỉ lấy is_deleted = 0
    }

    public void insert(Note note) {
        // Khi chèn note mới, đảm bảo isDeleted là false và deletionTime là 0
        note.setDeleted(false);
        note.setDeletionTime(0);
        appExecutors.diskIO().execute(() -> noteDao.insertNote(note));
    }

    /**
     * Di chuyển một ghi chú vào thùng rác.
     * Thay vì xóa hẳn, chúng ta cập nhật trạng thái isDeleted và deletionTime.
     */
    public void moveToTrash(Note note) {
        final long currentTime = System.currentTimeMillis();
        note.setDeleted(true); // Cập nhật đối tượng note nếu cần dùng ngay sau đó
        note.setDeletionTime(currentTime); // Cập nhật đối tượng note
        // Cách 1: Dùng @Query trong DAO (khuyến nghị cho hành động cụ thể này)
        appExecutors.diskIO().execute(() -> noteDao.moveToTrash(note.getId(), currentTime));
        // Cách 2: Nếu bạn muốn dùng @Update và đã cập nhật isDeleted, deletionTime cho đối tượng 'note'
        // appExecutors.diskIO().execute(() -> noteDao.updateNote(note));
    }

    /**
     * Phương thức delete() cũ giờ sẽ là moveToTrash().
     * Để giữ tính tương thích hoặc nếu bạn muốn một tên gọi chung chung cho hành động "xóa" từ UI.
     */
    public void delete(Note note) {
        moveToTrash(note);
    }


    public void update(Note note) {
        // Khi cập nhật, đảm bảo note này không phải đang trong thùng rác,
        // hoặc logic cập nhật của bạn cho phép sửa note trong thùng rác (ít phổ biến).
        // Thường thì note trong thùng rác sẽ không được cập nhật nội dung.
        // Nếu note này đang active, isDeleted phải là false.
        note.setDeleted(false); // Đảm bảo note đang được cập nhật là active
        note.setDeletionTime(0); // Reset thời gian xóa nếu có
        appExecutors.diskIO().execute(() -> noteDao.updateNote(note));
    }

    public LiveData<Note> getNoteById(int noteId) {
        // noteDao.getNoteById() nên đã được cập nhật để chỉ lấy is_deleted = 0
        return noteDao.getNoteById(noteId);
    }

    // Các phương thức sắp xếp, chúng sẽ tự động lấy note active nếu DAO đã được cập nhật
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


    // --- CÁC PHƯƠNG THỨC MỚI CHO THÙNG RÁC VÀ XÓA VĨNH VIỄN ---

    /**
     * Lấy danh sách các ghi chú trong thùng rác.
     */
    public LiveData<List<Note>> getTrashNotes() {
        return noteDao.getTrashNotes();
    }

    /**
     * Khôi phục một ghi chú từ thùng rác.
     */
    public void restoreNoteFromTrash(Note note) {
        appExecutors.diskIO().execute(() -> noteDao.restoreFromTrash(note.getId()));
    }

    /**
     * Xóa vĩnh viễn một ghi chú.
     */
    public void permanentlyDeleteNote(Note note) {
        appExecutors.diskIO().execute(() -> noteDao.permanentlyDeleteNote(note));
    }

    /**
     * Tự động xóa vĩnh viễn các ghi chú trong thùng rác đã quá 30 ngày.
     * Phương thức này nên được gọi từ một tác vụ nền (ví dụ: WorkManager).
     */
    public void autoPermanentlyDeleteOldTrashedNotes() {
        appExecutors.diskIO().execute(() -> {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_YEAR, -30); // Lùi lại 30 ngày
            long thirtyDaysAgoTimestamp = calendar.getTimeInMillis();

            List<Note> expiredNotes = noteDao.getExpiredTrashNotes(thirtyDaysAgoTimestamp);
            if (expiredNotes != null && !expiredNotes.isEmpty()) {
                noteDao.permanentlyDeleteNotes(expiredNotes); // Giả sử DAO có phương thức này
            }
        });
    }
}