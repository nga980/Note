package com.example.notes.database;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.annotation.NonNull;

import android.content.Context;
import android.util.Log; // Thêm Log để theo dõi migration
import android.database.Cursor;

import com.example.notes.dao.NoteDao;
import com.example.notes.entities.Note;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@Database(entities = {Note.class}, version = 4, exportSchema = false) // Đảm bảo version là 4 (hoặc version mới nhất của bạn)
public abstract class NoteDatabase extends RoomDatabase {

    private static volatile NoteDatabase noteDatabase;
    private static final String TAG_MIGRATION = "NoteDB_Migration";

    public static NoteDatabase getDatabase(final Context context) {
        if (noteDatabase == null) {
            synchronized (NoteDatabase.class) {
                if (noteDatabase == null) {
                    noteDatabase = Room.databaseBuilder(context.getApplicationContext(),
                                    NoteDatabase.class, "notes_db")
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4) // Đảm bảo MIGRATION_3_4 được thêm vào
                            .build();
                }
            }
        }
        return noteDatabase;
    }

    public abstract NoteDao noteDao();

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            Log.i(TAG_MIGRATION, "Running MIGRATION_1_2");
            database.execSQL("ALTER TABLE notes ADD COLUMN is_deleted INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE notes ADD COLUMN deletion_time INTEGER NOT NULL DEFAULT 0");
            Log.i(TAG_MIGRATION, "Finished MIGRATION_1_2");
        }
    };

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            Log.i(TAG_MIGRATION, "Running MIGRATION_2_3");
            database.execSQL("ALTER TABLE notes ADD COLUMN drawing_path TEXT DEFAULT NULL");
            Log.i(TAG_MIGRATION, "Finished MIGRATION_2_3");
        }
    };

    // MIGRATION TỪ VERSION 3 SANG 4 (ĐÃ SỬA)
    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            Log.i(TAG_MIGRATION, "Running MIGRATION_3_4");
            // Thêm cột timestamp
            database.execSQL("ALTER TABLE notes ADD COLUMN timestamp INTEGER NOT NULL DEFAULT 0");
            Log.d(TAG_MIGRATION, "Added timestamp column to notes table.");

            // >>> SỬA LỖI: Tạo index trên cột timestamp <<<
            database.execSQL("CREATE INDEX IF NOT EXISTS index_notes_timestamp ON notes(timestamp)");
            Log.d(TAG_MIGRATION, "Created index index_notes_timestamp on notes(timestamp).");

            // Cập nhật timestamp cho các bản ghi hiện có (logic này giữ nguyên từ trước)
            Cursor cursor = database.query("SELECT id, date_time FROM notes");
            if (cursor.moveToFirst()) {
                // Cố gắng sử dụng định dạng mà bạn đã dùng để lưu date_time
                // Ví dụ: "EEEE, dd MMMM yyyy HH:mm a" (thêm yyyy để đầy đủ hơn)
                SimpleDateFormat displayFormatDefault = new SimpleDateFormat("EEEE, dd MMMM yyyy HH:mm a", Locale.getDefault());
                SimpleDateFormat displayFormatUS = new SimpleDateFormat("EEEE, dd MMMM yyyy HH:mm a", Locale.US); // Fallback

                int idColumnIndex = cursor.getColumnIndex("id");
                int dateTimeColumnIndex = cursor.getColumnIndex("date_time");

                if (idColumnIndex == -1 || dateTimeColumnIndex == -1) {
                    Log.e(TAG_MIGRATION, "MIGRATION_3_4: Could not find id or date_time column.");
                    cursor.close();
                    return;
                }

                do {
                    int id = cursor.getInt(idColumnIndex);
                    String dateTimeStr = cursor.getString(dateTimeColumnIndex);
                    // Đặt một giá trị mặc định hợp lý, ví dụ: thời gian hiện tại trừ đi một chút dựa trên id
                    // để các ghi chú cũ hơn có timestamp nhỏ hơn nếu không parse được.
                    long calculatedTimestamp = System.currentTimeMillis() - ((long)id * 60000); // trừ đi 1 phút cho mỗi id giảm dần

                    if (dateTimeStr != null && !dateTimeStr.isEmpty()) {
                        Date date = null;
                        try {
                            date = displayFormatDefault.parse(dateTimeStr);
                        } catch (ParseException e1) {
                            try {
                                date = displayFormatUS.parse(dateTimeStr);
                            } catch (ParseException e2) {
                                Log.w(TAG_MIGRATION, "MIGRATION_3_4: Could not parse date_time '" + dateTimeStr + "' for note ID " + id + ". Using default calculated timestamp.");
                            }
                        }
                        if (date != null) {
                            calculatedTimestamp = date.getTime();
                        }
                    }
                    database.execSQL("UPDATE notes SET timestamp = " + calculatedTimestamp + " WHERE id = " + id);
                } while (cursor.moveToNext());
            }
            cursor.close();
            Log.d(TAG_MIGRATION, "MIGRATION_3_4: Finished attempting to update timestamps for existing notes.");
            Log.i(TAG_MIGRATION, "Finished MIGRATION_3_4");
        }
    };
}