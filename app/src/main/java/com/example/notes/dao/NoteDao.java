package com.example.notes.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.notes.entities.Note;

import java.util.List;

@Dao
public interface NoteDao {

    // Sắp xếp mặc định: ghim lên đầu, sau đó theo thời gian sửa đổi mới nhất
    @Query("SELECT * FROM notes WHERE is_deleted = 0 ORDER BY is_pinned DESC, timestamp DESC")
    LiveData<List<Note>> getAllNotes();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertNote(Note note);

    @Update
    void updateNote(Note note);

    @Query("SELECT * FROM notes WHERE id = :noteId AND is_deleted = 0")
    LiveData<Note> getNoteById(int noteId);

    // Khi chuyển vào thùng rác, bỏ ghim
    @Query("UPDATE notes SET is_deleted = 1, deletion_time = :currentTime, is_pinned = 0 WHERE id = :noteId")
    void moveToTrash(int noteId, long currentTime);

    // Khi khôi phục từ thùng rác, is_pinned vẫn là 0 (người dùng có thể ghim lại)
    // Cập nhật timestamp khi khôi phục
    @Query("UPDATE notes SET is_deleted = 0, deletion_time = 0, timestamp = :restoreTime, is_pinned = 0 WHERE id = :noteId")
    void restoreFromTrash(int noteId, long restoreTime);

    @Query("SELECT * FROM notes WHERE is_deleted = 1 ORDER BY deletion_time DESC")
    LiveData<List<Note>> getTrashNotes();

    @Delete
    void permanentlyDeleteNote(Note note);

    @Delete
    void permanentlyDeleteNotes(List<Note> notes);

    @Query("SELECT * FROM notes WHERE is_deleted = 0 ORDER BY is_pinned DESC, title ASC")
    LiveData<List<Note>> getAllNotesSortedByTitleAsc();

    @Query("SELECT * FROM notes WHERE is_deleted = 0 ORDER BY is_pinned DESC, title DESC")
    LiveData<List<Note>> getAllNotesSortedByTitleDesc();

    @Query("SELECT * FROM notes WHERE is_deleted = 0 ORDER BY is_pinned DESC, timestamp DESC")
    LiveData<List<Note>> getAllNotesSortedByDateModifiedDesc();

    @Query("SELECT * FROM notes WHERE is_deleted = 0 ORDER BY is_pinned DESC, timestamp ASC")
    LiveData<List<Note>> getAllNotesSortedByDateModifiedAsc();

    @Query("SELECT * FROM notes WHERE is_deleted = 1 AND deletion_time > 0 AND deletion_time < :timestampLimit")
    List<Note> getExpiredTrashNotes(long timestampLimit);

    @Query("DELETE FROM notes WHERE is_deleted = 1")
    void permanentlyDeleteAllTrashedNotes();

    // >>> PHƯƠNG THỨC MỚI ĐỂ CẬP NHẬT TRẠNG THÁI GHIM <<<
    // Cập nhật cả timestamp vì hành động ghim/bỏ ghim cũng được coi là một loại "sửa đổi"
    @Query("UPDATE notes SET is_pinned = :isPinned, timestamp = :currentTime WHERE id = :noteId")
    void updatePinStatus(int noteId, boolean isPinned, long currentTime);
}