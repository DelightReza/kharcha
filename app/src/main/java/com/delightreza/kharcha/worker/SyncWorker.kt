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
                        // Classical
                        "🚨 EMERGENCY: You owe $debt SOM! Pay up or wash dishes for a month! 🍽️",
                        "Hello? It's your conscience. You owe $debt SOM. Fix it. 📞",
                        "Friendly reminder: You are the reason we can't have nice things. Owe: $debt SOM. 🌚",
                        "Stop ignoring this! You are in the red: -$debt SOM. 📉",
                        "Money doesn't grow on trees, but your debt does! Pay $debt SOM now! 🌳",
                        "Knock knock. Who's there? A debt of $debt SOM. 🚪",
                        "Wallet looking heavy? Lighten it by paying your $debt SOM debt! 💸",
            
                        // Pop Culture / Gen Z
                        "I am once again asking for your financial support. Specifically: $debt SOM. 🧤",
                        "Winter is coming... and so is the bill for $debt SOM. ❄️",
                        "You owe $debt SOM. This is not the way. 👽",
                        "Emotional Damage: You still owe $debt SOM. 💔",
                        "Tell me you're broke without telling me you're broke. Balance: -$debt SOM. 💀",
                        "It's a beautiful day to pay your $debt SOM debt. ☀️",
                        "Swiper no swiping! But you... you need to be paying $debt SOM. 🦊",
                        "Are you waiting for a letter from Hogwarts to pay this $debt SOM? 🦉",
                        "Simulating financial responsibility... Error. Debt: $debt SOM. 🤖",
            
                        // Aggressive / Panic
                        "⚠️ CODE RED: $debt SOM MISSING. DEPLOYING SEARCH PARTY. 🚁",
                        "Your kneecaps look lovely today. Would be a shame if you didn't pay $debt SOM. 🔨",
                        "I told your mom you owe $debt SOM. She's disappointed. 👵",
                        "FBI OPEN UP! Just kidding, but seriously, pay the $debt SOM. 👮",
                        "Every second you wait, the $debt SOM gets sadder. 😢",
                        "I'm not saying aliens will abduct you, but they prefer people with 0 debt. Current: $debt SOM. 🛸",
                        "RUN! (To the bank to pay your $debt SOM). 🏃",
            
                        // Passive Aggressive
                        "I guess we're just pretending this $debt SOM doesn't exist? Cool. ☕",
                        "Your credit score is watching you read this and do nothing about the $debt SOM. 👀",
                        "Seen at 10:00 AM. Still hasn't paid $debt SOM. Typical. ✔️✔️",
                        "Oh, you're buying coffee? Must be nice having $debt SOM debt. ☕",
                        "I love how relaxed you are about owing $debt SOM. Inspiring, really. 🧘",
                        "Don't worry, I'll just sit here and wait for my $debt SOM. In the dark. Alone. 🕯️",
            
                        // Tech / Glitch
                        "404 Error: Payment of $debt SOM not found. Please refresh wallet. 💻",
                        "System Update: You still owe $debt SOM. Do not turn off your conscience. 🔋",
                        "Syncing... Syncing... Failed. Reason: Negative balance of $debt SOM. 📡",
                        "Warning: Low Balance. You are -$debt SOM in the hole. Recharge immediately. 🔋",
                        "Ctrl+Alt+Delete this debt of $debt SOM immediately. ⌨️",
            
                        // Short & Punchy
                        "Pay. $debt SOM. Now. 😡",
                        "Bruh. $debt SOM? Really? 😑",
                        "Yikes. -$debt SOM. 😬",
                        "Fix this: $debt SOM. 🛠️",
                        "Account status: Broke ($debt SOM). 📉",
                        "Debt: $debt SOM. Dignity: 0. 📉",
            
                        // Guilt Trips
                        "Think of the children! (Or just the people you owe $debt SOM to). 👶",
                        "Every time you ignore this, a puppy cries. You owe $debt SOM. 🐶",
                        "To owe is human. To pay is divine. Be divine. Pay $debt SOM. ✨",
                        "Do it for your ancestors. Pay the $debt SOM. 🏛️",
            
                        // The "Helpful" Assistant
                        "Suggestion: Sell a kidney? You owe $debt SOM. (Legal disclaimer: Don't). 🏥",
                        "Tip: Walking is free. Not paying your $debt SOM debt is not. 🚶",
                        "Did you know? $debt SOM is enough to buy... well, your freedom from this notification. 🌈",
                        "Math lesson: You - $debt SOM = Sadness. You + Payment = Happiness. 🧮",
            
                        // Random / Absurdist
                        "The spirits say... you owe $debt SOM. 🔮",
                        "A bird pooped on my car today. It's a sign you need to pay $debt SOM. 🐦",
                        "If I had a SOM for every time you ignored this, I'd have... well, $debt SOM. 💰",
                        "Banana. Monkey. Pay your $debt SOM. 🍌",
                        "You have won a free iPhone! Just kidding, pay your $debt SOM. 📱",
            
                        // Local / Cultural (General)
                        "No Plov for you until you pay $debt SOM! 🥘",
                        "Even the tea is getting cold waiting for your $debt SOM. ☕",
                        "Don't be that guy. Pay the $debt SOM. 🤦",
            
                        // More fillers to hit the "100" feel
                        "Tic Toc. The clock is ticking on $debt SOM. ⏰",
                        "Mission Impossible: Pay $debt SOM. 🕵️",
                        "Loading financial stability... Stuck at -$debt SOM. ⏳",
                        "Printer out of ink. Can't print money. Pay $debt SOM. 🖨️",
                        "Alert! Alert! $debt SOM deficit detected! 🚨",
                        "This is your final warning (until the next hour). Pay $debt SOM. ⚠️",
                        "Look at your wallet. Now look at me. Pay $debt SOM. 🐴",
                        "Keep calm and pay $debt SOM. 👑",
                        "Eat. Sleep. Pay $debt SOM. Repeat. 🔁",
                        "Just do it. Pay $debt SOM. ✔️",
                        "May the funds be with you. You need $debt SOM. ⚔️",
                        "Houston, we have a problem. It's a $debt SOM debt. 🚀",
                        "Are you okay? You seem to be allergic to paying $debt SOM. 🤧",
                        "Ghosting your friends is bad. Ghosting your $debt SOM debt is worse. 👻",
                        "Swipe right on payment. Swipe left on debt ($debt SOM). 📱",
                        "Be a hero. Save the economy. Pay $debt SOM. 🦸",
                        "Legend says he still owes $debt SOM to this day... 📜",
                        "Don't make me use ALL CAPS TO ASK FOR $debt SOM. 🅰️",
                        "Error 503: Service Unavailable until $debt SOM is paid. 🛑",
                        "Searching for money... 0 results found. Debt: $debt SOM. 🔍",
                        "I bet you think this notification is annoying. So is owing $debt SOM. 🤡",
                        "Reality check: You are poor. Specifically, -$debt SOM poor. 📉",
                        "Achievement Locked: 'Debt Free'. Pay $debt SOM to unlock. 🏆",
                        "Insert coin to continue. (Insert $debt SOM). 🕹️",
                        "Game Over. Insert $debt SOM to try again. 👾",
                        "Your free trial of 'Ignoring Debt' has expired. Owe: $debt SOM. ⏳",
                        "Roses are red, violets are blue, you owe $debt SOM, and that is true. 🌹",
                        "Violets are blue, roses are red, pay $debt SOM or you're dead (financially). 🥀",
                        "Haiku for you: \nWallet is empty. \nDebt is very high today. \nPay $debt SOM now. 🎋",
                        "Knock knock. It's the debt collector. ($debt SOM). 👊",
                        "Who needs enemies when you have a debt of $debt SOM? 🤷",
                        "Feeling lonely? Your debt of $debt SOM is always with you. 🫂",
                        "Money talks. Yours is saying 'Goodbye' ($debt SOM). 👋",
                        "I'm watching you. Always. Pay $debt SOM. 👁️",
                        "Is it me you're looking for? No, it's $debt SOM. 🎤",
                        "Never gonna give you up... never gonna let you ignore $debt SOM. 🕺",
                        "Identify theft is not a joke, Jim! Neither is $debt SOM! 👓",
                        "That's what she said: 'Pay $debt SOM!' 🏢",
                        "Bears. Beets. Battlestar Galactica. Pay $debt SOM. 🐻",
                        "How you doin'? (Broke, apparently. -$debt SOM). 🍕",
                        "Pivot! Pivot! Pivot your money to this debt of $debt SOM! 🛋️",
                        "Smelly Cat, Smelly Cat, why aren't you paying $debt SOM? 🐈",
                        "Could you BE any more in debt? ($debt SOM). 👔",
                        "Live long and prosper (after you pay $debt SOM). 🖖",
                        "The force is weak with this wallet. (-$debt SOM). 🌑",
                        "Chewbacca noise! (Translation: Pay $debt SOM). 🦍",
                        "Why so serious? It's just $debt SOM. 🃏"
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
