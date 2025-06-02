package com.example.notes.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete; // Sẽ dùng cho xóa vĩnh viễn
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.notes.entities.Note;

import java.util.List;

@Dao
public interface NoteDao {

    // SỬA LẠI: Chỉ lấy các ghi chú CHƯA BỊ XÓA (isDeleted = 0)
    @Query("SELECT * FROM notes WHERE is_deleted = 0 ORDER BY id DESC")
    LiveData<List<Note>> getAllNotes(); // Giữ nguyên tên hoặc đổi thành getAllActiveNotes()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertNote(Note note); // Dùng cho cả tạo mới và cập nhật (do OnConflictStrategy.REPLACE)

    // @Delete // Không dùng @Delete trực tiếp để "di chuyển vào thùng rác" nữa
    // void deleteNote(Note note); // Bỏ phương thức này nếu bạn không dùng nó nữa

    @Update // Dùng để cập nhật note, bao gồm cả việc đánh dấu isDeleted
    void updateNote(Note note);

    @Query("SELECT * FROM notes WHERE id = :noteId AND is_deleted = 0") // Chỉ lấy nếu chưa bị xóa
    LiveData<Note> getNoteById(int noteId);


    // --- CÁC TRUY VẤN CHO THÙNG RÁC ---

    /**
     * Di chuyển ghi chú vào thùng rác (đánh dấu isDeleted = true và setzen deletionTime)
     * Thay vì dùng @Update, dùng @Query sẽ rõ ràng hơn cho hành động này.
     * Note: updateNote(note) cũng có thể làm điều này nếu bạn set isDeleted và deletionTime trong đối tượng Note trước khi gọi.
     */
    @Query("UPDATE notes SET is_deleted = 1, deletion_time = :currentTime WHERE id = :noteId")
    void moveToTrash(int noteId, long currentTime);

    /**
     * Khôi phục ghi chú từ thùng rác (đánh dấu isDeleted = false, reset deletionTime)
     */
    @Query("UPDATE notes SET is_deleted = 0, deletion_time = 0 WHERE id = :noteId")
    void restoreFromTrash(int noteId);

    /**
     * Lấy tất cả các ghi chú trong thùng rác (isDeleted = 1), sắp xếp theo thời gian xóa mới nhất.
     */
    @Query("SELECT * FROM notes WHERE is_deleted = 1 ORDER BY deletion_time DESC")
    LiveData<List<Note>> getTrashNotes();

    /**
     * Xóa vĩnh viễn một ghi chú khỏi cơ sở dữ liệu.
     */
    @Delete // Dùng @Delete cho việc xóa vĩnh viễn thực sự
    void permanentlyDeleteNote(Note note);

    /**
     * Xóa vĩnh viễn nhiều ghi chú (ví dụ: các ghi chú cũ trong thùng rác).
     */
    @Delete
    void permanentlyDeleteNotes(List<Note> notes);


    // --- CÁC TRUY VẤN SẮP XẾP (SỬA LẠI ĐỂ LỌC is_deleted = 0) ---

    @Query("SELECT * FROM notes WHERE is_deleted = 0 ORDER BY title ASC")
    LiveData<List<Note>> getAllNotesSortedByTitleAsc();

    @Query("SELECT * FROM notes WHERE is_deleted = 0 ORDER BY title DESC")
    LiveData<List<Note>> getAllNotesSortedByTitleDesc();

    @Query("SELECT * FROM notes WHERE is_deleted = 0 ORDER BY date_time DESC")
    LiveData<List<Note>> getAllNotesSortedByDateModifiedDesc();

    @Query("SELECT * FROM notes WHERE is_deleted = 0 ORDER BY date_time ASC")
    LiveData<List<Note>> getAllNotesSortedByDateModifiedAsc();

    // --- TRUY VẤN CHO GIAI ĐOẠN 2: XÓA TỰ ĐỘNG SAU 30 NGÀY ---
    /**
     * Lấy các ghi chú trong thùng rác đã quá hạn (ví dụ: deletionTime < thirtyDaysAgoTimestamp)
     */
    @Query("SELECT * FROM notes WHERE is_deleted = 1 AND deletion_time > 0 AND deletion_time < :timestamp")
    List<Note> getExpiredTrashNotes(long timestamp); // Trả về List, không phải LiveData vì sẽ xử lý nền
}