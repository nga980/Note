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

    public LiveData<List<Note>> getAllNotes() {
        return noteDao.getAllNotes();
    }

    public void insert(Note note) {
        // isPinned đã được đặt mặc định trong Entity
        note.setDeleted(false);
        note.setDeletionTime(0);
        // timestamp đã được đặt khi tạo Note hoặc sẽ được cập nhật khi lưu
        appExecutors.diskIO().execute(() -> noteDao.insertNote(note));
    }

    public void moveToTrash(Note note) {
        final long currentTime = System.currentTimeMillis();
        // DAO sẽ tự động bỏ ghim khi chuyển vào thùng rác
        appExecutors.diskIO().execute(() -> noteDao.moveToTrash(note.getId(), currentTime));
    }

    public void delete(Note note) {
        moveToTrash(note);
    }

    public void update(Note note) {
        // isPinned sẽ được lấy từ đối tượng note truyền vào (đã được set trong CreateNoteActivity)
        note.setDeleted(false);
        note.setDeletionTime(0);
        // timestamp đã được cập nhật trong CreateNoteActivity trước khi gọi update
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
        final long currentTime = System.currentTimeMillis(); // Thời gian khôi phục, cũng là timestamp mới
        // DAO sẽ tự động đặt is_pinned = 0 và cập nhật timestamp
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

    public void emptyAllTrash() {
        appExecutors.diskIO().execute(new Runnable() {
            @Override
            public void run() {
                noteDao.permanentlyDeleteAllTrashedNotes();
            }
        });
    }

    // >>> PHƯƠNG THỨC MỚI ĐỂ CẬP NHẬT TRẠNG THÁI GHIM <<<
    public void updatePinStatus(int noteId, boolean isPinned) {
        final long currentTime = System.currentTimeMillis(); // Cập nhật timestamp khi ghim/bỏ ghim
        appExecutors.diskIO().execute(() -> noteDao.updatePinStatus(noteId, isPinned, currentTime));
    }
}