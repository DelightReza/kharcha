package com.delightreza.kharcha.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.delightreza.kharcha.MainActivity
import com.delightreza.kharcha.R
import com.delightreza.kharcha.data.AppDataStore
import com.delightreza.kharcha.data.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): ListenableWorker.Result = withContext(Dispatchers.IO) {
        try {
            val dataStore = AppDataStore(applicationContext)
            val repository = Repository(dataStore)

            // 1. Get currently cached data count BEFORE fetching new data
            val oldData = repository.getCachedData()
            val oldCount = oldData?.transactions?.size ?: 0

            // 2. Fetch fresh data from API (This also updates the cache)
            val newData = repository.fetchData()
            val newCount = newData?.transactions?.size ?: 0

            // 3. Compare and Notify
            if (newData != null && newCount > oldCount) {
                val diff = newCount - oldCount
                val latestTx = newData.transactions.firstOrNull()
                
                // Logic: If 1 new item, show details. If multiple, show count.
                val (title, contentText) = if (diff == 1 && latestTx != null) {
                    val symbol = if (latestTx.type == "credit") "+" else "-"
                    "New Transaction" to "${latestTx.whoOrBill}: $symbol${latestTx.amount.toInt()}"
                } else {
                    "Kharcha Update" to "$diff new transactions added."
                }

                sendNotification(title, contentText)
            }

            ListenableWorker.Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            // If network fails, try again later
            ListenableWorker.Result.retry()
        }
    }

    private fun sendNotification(title: String, message: String) {
        val channelId = "kharcha_sync_channel"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create Channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, 
                "Transaction Updates", 
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifies when new transactions are added"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Create Intent to open App when clicked
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            applicationContext, 
            0, 
            intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent) // Open app on click
            .setAutoCancel(true) // Remove notification on click
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
