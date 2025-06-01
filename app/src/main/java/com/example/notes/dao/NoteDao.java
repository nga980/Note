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

    // Phương thức gốc của bạn, có thể đổi tên hoặc giữ nguyên tùy vào cách ViewModel sử dụng
    @Query("SELECT * FROM notes ORDER BY id DESC")
    LiveData<List<Note>> getAllNotes(); // Hoặc getAllNotesDefaultOrder()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertNote(Note note);

    @Delete
    void deleteNote(Note note);

    @Update
    void updateNote(Note note);

    @Query("SELECT * FROM notes WHERE id = :noteId")
    LiveData<Note> getNoteById(int noteId);


    // CÁC TRUY VẤN SẮP XẾP MỚI:

    /**
     * Lấy tất cả ghi chú sắp xếp theo tiêu đề tăng dần (A-Z).
     */
    @Query("SELECT * FROM notes ORDER BY title ASC")
    LiveData<List<Note>> getAllNotesSortedByTitleAsc();

    /**
     * Lấy tất cả ghi chú sắp xếp theo tiêu đề giảm dần (Z-A).
     */
    @Query("SELECT * FROM notes ORDER BY title DESC")
    LiveData<List<Note>> getAllNotesSortedByTitleDesc();

    /**
     * Lấy tất cả ghi chú sắp xếp theo thời gian cập nhật (dateTime) giảm dần (Mới nhất trước).
     * Trường 'dateTime' trong Note của bạn hiện đang hoạt động như thời gian cập nhật cuối cùng.
     */
    @Query("SELECT * FROM notes ORDER BY date_time DESC")
    LiveData<List<Note>> getAllNotesSortedByDateModifiedDesc();

    /**
     * Lấy tất cả ghi chú sắp xếp theo thời gian cập nhật (dateTime) tăng dần (Cũ nhất trước).
     */
    @Query("SELECT * FROM notes ORDER BY date_time ASC")
    LiveData<List<Note>> getAllNotesSortedByDateModifiedAsc();

}