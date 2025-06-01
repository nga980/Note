package com.example.notes.repositories;

import android.app.Application;
import androidx.lifecycle.LiveData;
import com.example.notes.dao.NoteDao;
import com.example.notes.database.NoteDatabase;
import com.example.notes.entities.Note;
import com.example.notes.executors.AppExecutors;

import java.util.List;

public class NoteRepository {
    private NoteDao noteDao;
    private LiveData<List<Note>> allNotes;
    private AppExecutors appExecutors;

    public NoteRepository(Application application) {
        NoteDatabase database = NoteDatabase.getDatabase(application);
        noteDao = database.noteDao();
        allNotes = noteDao.getAllNotes();
        appExecutors = AppExecutors.getInstance();
    }

    public LiveData<List<Note>> getAllNotes() {
        return allNotes;
    }

    public void insert(Note note) {
        appExecutors.diskIO().execute(() -> noteDao.insertNote(note));
    }

    public void delete(Note note) {
        appExecutors.diskIO().execute(() -> noteDao.deleteNote(note));
    }

    public void update(Note note) {
        // Nếu bạn dùng @Insert(onConflict = OnConflictStrategy.REPLACE),
        // bạn có thể gọi insert(note) ở đây.
        // Hoặc nếu bạn có phương thức updateNote riêng trong DAO:
        appExecutors.diskIO().execute(() -> noteDao.updateNote(note));
    }

    public LiveData<Note> getNoteById(int noteId) {
        return noteDao.getNoteById(noteId); // Room tự quản lý thread cho LiveData
    }
}