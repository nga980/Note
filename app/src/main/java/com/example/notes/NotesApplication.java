package com.example.notes;

import android.app.Application;
import android.util.Log;

import androidx.work.Constraints;
// import androidx.work.NetworkType; // Bỏ comment nếu bạn dùng ràng buộc mạng
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.ExistingPeriodicWorkPolicy; // Thêm import này

import com.example.notes.workers.AutoDeleteWorker;

import java.util.concurrent.TimeUnit;

public class NotesApplication extends Application {

    private static final String TAG = "NotesApplication";
    private static final String AUTO_DELETE_WORK_TAG = "autoDeleteOldNotesWork";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Application onCreate: Scheduling auto delete work.");
        scheduleAutoDeleteTask();
    }

    private void scheduleAutoDeleteTask() {
        Constraints constraints = new Constraints.Builder()
                // .setRequiresCharging(true)
                // .setRequiredNetworkType(NetworkType.UNMETERED)
                .build();

        PeriodicWorkRequest autoDeleteWorkRequest =
                new PeriodicWorkRequest.Builder(AutoDeleteWorker.class, 1, TimeUnit.DAYS)
                        .setConstraints(constraints)
                        .addTag(AUTO_DELETE_WORK_TAG)
                        .build();

        WorkManager.getInstance(getApplicationContext()).enqueueUniquePeriodicWork(
                AUTO_DELETE_WORK_TAG,
                ExistingPeriodicWorkPolicy.KEEP, // <<< THAY ĐỔI TỪ REPLACE SANG KEEP
                autoDeleteWorkRequest
        );

        Log.d(TAG, "Auto delete work scheduling attempt (KEEP policy).");
    }
}