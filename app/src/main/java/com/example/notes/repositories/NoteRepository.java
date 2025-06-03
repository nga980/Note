package com.example.notes.repositories;

import android.app.Application;
import androidx.lifecycle.LiveData;
import com.example.notes.dao.NoteDao;
import com.example.notes.database.NoteDatabase;
import com.example.notes.entities.Note;
import com.example.notes.executors.AppExecutors;

import java.util.Calendar;
import java.util.List;

public class NoteRepository {
    private final NoteDao noteDao;
    private final AppExecutors appExecutors;

    public NoteRepository(Application application) {
        NoteDatabase database = NoteDatabase.getDatabase(application);
        noteDao = database.noteDao();
        appExecutors = AppExecutors.getInstance();
    }

    // ... (các phương thức hiện có) ...
    public LiveData<List<Note>> getAllNotes() {
        return noteDao.getAllNotes();
    }

    public void insert(Note note) {
        note.setDeleted(false);
        note.setDeletionTime(0);
        appExecutors.diskIO().execute(() -> noteDao.insertNote(note));
    }

    public void moveToTrash(Note note) {
        final long currentTime = System.currentTimeMillis();
        appExecutors.diskIO().execute(() -> noteDao.moveToTrash(note.getId(), currentTime));
    }

    public void delete(Note note) {
        moveToTrash(note);
    }

    public void update(Note note) {
        note.setDeleted(false);
        note.setDeletionTime(0);
        appExecutors.diskIO().execute(() -> noteDao.updateNote(note));
    }

    public LiveData<Note> getNoteById(int noteId) {
        return noteDao.getNoteById(noteId);
    }

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

    public LiveData<List<Note>> getTrashNotes() {
        return noteDao.getTrashNotes();
    }

    public void restoreNoteFromTrash(Note note) {
        final long currentTime = System.currentTimeMillis();
        appExecutors.diskIO().execute(() -> noteDao.restoreFromTrash(note.getId(), currentTime));
    }

    public void permanentlyDeleteNote(Note note) {
        appExecutors.diskIO().execute(() -> noteDao.permanentlyDeleteNote(note));
    }

    public void autoPermanentlyDeleteOldTrashedNotes() {
        appExecutors.diskIO().execute(() -> {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_YEAR, -30);
            long thirtyDaysAgoTimestamp = calendar.getTimeInMillis();

            List<Note> expiredNotes = noteDao.getExpiredTrashNotes(thirtyDaysAgoTimestamp);
            if (expiredNotes != null && !expiredNotes.isEmpty()) {
                noteDao.permanentlyDeleteNotes(expiredNotes);
            }
        });
    }

    // >>> BỎ CHÚ THÍCH VÀ TRIỂN KHAI PHƯƠNG THỨC NÀY <<<
    /**
     * Dọn sạch toàn bộ thùng rác.
     * Phương thức này sẽ xóa vĩnh viễn TẤT CẢ các ghi chú đang trong thùng rác.
     */
    public void emptyAllTrash() {
        appExecutors.diskIO().execute(new Runnable() {
            @Override
            public void run() {
                noteDao.permanentlyDeleteAllTrashedNotes();
            }
        });
    }
}