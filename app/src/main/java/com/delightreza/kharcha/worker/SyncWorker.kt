package com.delightreza.kharcha.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker // FIXED: Added specific import
import androidx.work.WorkerParameters
import com.delightreza.kharcha.R
import com.delightreza.kharcha.data.AppDataStore
import com.delightreza.kharcha.data.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    // FIXED: Explicit return type to resolve ambiguity
    override suspend fun doWork(): ListenableWorker.Result = withContext(Dispatchers.IO) {
        try {
            val dataStore = AppDataStore(applicationContext)
            val repository = Repository(dataStore)

            // 1. Get currently cached data count
            val oldData = repository.getCachedData()
            val oldCount = oldData?.transactions?.size ?: 0

            // 2. Fetch fresh data from API
            val newData = repository.fetchData()
            val newCount = newData?.transactions?.size ?: 0

            // 3. Compare and Notify
            if (newData != null && newCount > oldCount) {
                val diff = newCount - oldCount
                val latestTx = newData.transactions.firstOrNull()
                
                val contentText = if (diff == 1 && latestTx != null) {
                    "${latestTx.whoOrBill}: ${latestTx.type} ${latestTx.amount}"
                } else {
                    "$diff new transactions added."
                }

                sendNotification("New Kharcha Activity", contentText)
            }

            // FIXED: Using ListenableWorker.Result
            ListenableWorker.Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            ListenableWorker.Result.retry()
        }
    }

    private fun sendNotification(title: String, message: String) {
        val channelId = "kharcha_sync_channel"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Sync Updates", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }
}
