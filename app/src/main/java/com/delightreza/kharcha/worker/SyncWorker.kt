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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlin.math.abs

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
            val newData = repository.fetchData() ?: return@withContext ListenableWorker.Result.retry()
            val newCount = newData.transactions.size

            // --- PART A: NEW TRANSACTION NOTIFICATION ---
            if (newCount > oldCount) {
                val diff = newCount - oldCount
                val latestTx = newData.transactions.firstOrNull()
                
                val (title, contentText) = if (diff == 1 && latestTx != null) {
                    val symbol = if (latestTx.type == "credit") "+" else "-"
                    "New Transaction" to "${latestTx.whoOrBill}: $symbol${latestTx.amount.toInt()}"
                } else {
                    "Kharcha Update" to "$diff new transactions added."
                }

                sendNotification(1001, title, contentText)
            }

            // --- PART B: DEBT NAGGING (Every Hour) ---
            // Get the user currently logged into THIS device
            val currentUser = dataStore.userFlow.firstOrNull()

            if (!currentUser.isNullOrEmpty()) {
                val balances = repository.calculateBalances(newData)
                val myBalance = balances[currentUser] ?: 0.0

                // If currently in negative, SEND NAG!
                if (myBalance < 0) {
                    val debt = abs(myBalance).toInt()
                    
                    // Random list of annoying messages
                    val naggingMessages = listOf(
                        "🚨 EMERGENCY: You owe $debt SOM! Pay up or wash dishes for a month! 🍽️",
                        "Hello? It's your conscience. You owe $debt SOM. Fix it. 📞",
                        "Friendly reminder: You are the reason we can't have nice things. Owe: $debt SOM. 🌚",
                        "Stop ignoring this! You are in negative -$debt SOM. 📉",
                        "Money doesn't grow on trees, but your debt does! Pay $debt SOM now! 🌳",
                        "Knock knock. Who's there? A debt of $debt SOM. 🚪",
                        "Wallet looking heavy? Lighten it by paying your $debt SOM debt! 💸"
                    )

                    // Fixed ID (999) ensures this specific notification stays updated
                    sendNotification(
                        999, 
                        "⚠️ Debt Alert!", 
                        naggingMessages.random() // Pick a random annoyance
                    )
                }
            }

            ListenableWorker.Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            // If network fails, try again later
            ListenableWorker.Result.retry()
        }
    }

    private fun sendNotification(notificationId: Int, title: String, message: String) {
        val channelId = "kharcha_sync_channel"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create Channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, 
                "Transaction Updates", 
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifies regarding account status and transactions"
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
            .setStyle(NotificationCompat.BigTextStyle().bigText(message)) // Allows long nagging text to show fully
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent) // Open app on click
            .setAutoCancel(true) // Remove notification on click
            .build()

        notificationManager.notify(notificationId, notification)
    }
}
