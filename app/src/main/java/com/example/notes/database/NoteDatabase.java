package com.example.notes.database;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.annotation.NonNull;

import android.content.Context;

import com.example.notes.dao.NoteDao;
import com.example.notes.entities.Note;

@Database(entities = {Note.class}, version = 3, exportSchema = false) // TĂNG VERSION LÊN 3
public abstract class NoteDatabase extends RoomDatabase {

    private static volatile NoteDatabase noteDatabase;

    public static NoteDatabase getDatabase(final Context context) {
        if (noteDatabase == null) {
            synchronized (NoteDatabase.class) {
                if (noteDatabase == null) {
                    noteDatabase = Room.databaseBuilder(context.getApplicationContext(),
                                    NoteDatabase.class, "notes_db")
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3) // THÊM MIGRATION MỚI
                            .build();
                }
            }
        }
        return noteDatabase;
    }

    public abstract NoteDao noteDao();

    // Migration từ version 1 sang 2 (bạn đã có cái này)
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE notes ADD COLUMN is_deleted INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE notes ADD COLUMN deletion_time INTEGER NOT NULL DEFAULT 0");
        }
    };

    // ĐỊNH NGHĨA MIGRATION MỚI TỪ VERSION 2 SANG 3
    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Thêm cột drawing_path kiểu TEXT, có thể null
            database.execSQL("ALTER TABLE notes ADD COLUMN drawing_path TEXT DEFAULT NULL");
        }
    };
}