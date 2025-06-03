package com.example.notes.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.notes.repositories.NoteRepository; // Đảm bảo import đúng

public class AutoDeleteWorker extends Worker {

    private static final String TAG = "AutoDeleteWorker";

    public AutoDeleteWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "AutoDeleteWorker: Work is starting.");
        try {
            // Lấy NoteRepository. Cần Application context.
            // getApplicationContext() trả về Context của ứng dụng.
            NoteRepository repository = new NoteRepository((android.app.Application) getApplicationContext());
            repository.autoPermanentlyDeleteOldTrashedNotes();
            Log.d(TAG, "AutoDeleteWorker: Old trashed notes deletion process triggered.");
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "AutoDeleteWorker: Error while trying to delete old trashed notes.", e);
            return Result.failure();
        }
    }
}