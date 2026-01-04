package com.delightreza.kharcha

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.delightreza.kharcha.worker.SyncWorker
import java.util.concurrent.TimeUnit

class KharchaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        setupWorker()
    }

    private fun setupWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // CHANGED: Interval set to 1 Hour
        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "KharchaSyncWork",
            ExistingPeriodicWorkPolicy.KEEP, // Keeps the existing work if valid, prevents duplicates
            syncRequest
        )
    }
}
