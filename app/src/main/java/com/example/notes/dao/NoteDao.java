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

    // ... (các phương thức hiện có) ...

    @Query("SELECT * FROM notes WHERE is_deleted = 0 ORDER BY timestamp DESC")
    LiveData<List<Note>> getAllNotes();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertNote(Note note);

    @Update
    void updateNote(Note note);

    @Query("SELECT * FROM notes WHERE id = :noteId AND is_deleted = 0")
    LiveData<Note> getNoteById(int noteId);

    @Query("UPDATE notes SET is_deleted = 1, deletion_time = :currentTime WHERE id = :noteId")
    void moveToTrash(int noteId, long currentTime);

    @Query("UPDATE notes SET is_deleted = 0, deletion_time = 0, timestamp = :restoreTime WHERE id = :noteId")
    void restoreFromTrash(int noteId, long restoreTime);

    @Query("SELECT * FROM notes WHERE is_deleted = 1 ORDER BY deletion_time DESC")
    LiveData<List<Note>> getTrashNotes();

    @Delete
    void permanentlyDeleteNote(Note note);

    @Delete
    void permanentlyDeleteNotes(List<Note> notes);

    @Query("SELECT * FROM notes WHERE is_deleted = 0 ORDER BY title ASC")
    LiveData<List<Note>> getAllNotesSortedByTitleAsc();

    @Query("SELECT * FROM notes WHERE is_deleted = 0 ORDER BY title DESC")
    LiveData<List<Note>> getAllNotesSortedByTitleDesc();

    @Query("SELECT * FROM notes WHERE is_deleted = 0 ORDER BY timestamp DESC")
    LiveData<List<Note>> getAllNotesSortedByDateModifiedDesc();

    @Query("SELECT * FROM notes WHERE is_deleted = 0 ORDER BY timestamp ASC")
    LiveData<List<Note>> getAllNotesSortedByDateModifiedAsc();

    @Query("SELECT * FROM notes WHERE is_deleted = 1 AND deletion_time > 0 AND deletion_time < :timestampLimit")
    List<Note> getExpiredTrashNotes(long timestampLimit);

    // >>> PHƯƠNG THỨC MỚI ĐỂ DỌN SẠCH THÙNG RÁC <<<
    @Query("DELETE FROM notes WHERE is_deleted = 1")
    void permanentlyDeleteAllTrashedNotes();
}