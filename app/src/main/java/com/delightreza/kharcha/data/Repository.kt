package com.delightreza.kharcha.data

import android.util.Base64
import com.delightreza.kharcha.utils.DateUtils
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Type
import java.net.URI

class Repository(private val dataStore: AppDataStore) {
    private val api: GitHubApi
    
    // Custom Serializer to match Web's JSON format (compact, skipping nulls)
    private class TransactionSerializer : JsonSerializer<Transaction> {
        override fun serialize(src: Transaction, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
            val jsonObject = JsonObject()
            jsonObject.addProperty("id", src.id)
            jsonObject.addProperty("type", src.type)
            
            if (!src.payerId.isNullOrEmpty()) jsonObject.addProperty("payerId", src.payerId)
            // Fallback for V1 compatibility
            else if (src.type == "credit") jsonObject.addProperty("whoOrBill", src.whoOrBill)

            if (!src.billTypeId.isNullOrEmpty()) jsonObject.addProperty("billTypeId", src.billTypeId)
            // Fallback for V1 compatibility
            else if (src.type == "debit") jsonObject.addProperty("whoOrBill", src.whoOrBill)

            jsonObject.addProperty("note", src.note)
            
            // Format numbers to avoid unnecessary decimals (100.0 -> 100)
            if (src.amount % 1.0 == 0.0) jsonObject.addProperty("amount", src.amount.toInt()) 
            else jsonObject.addProperty("amount", src.amount)
            
            jsonObject.addProperty("date", src.date)
            
            if (!src.splitAmong.isNullOrEmpty()) {
                val array = JsonArray()
                src.splitAmong.forEach { array.add(it) }
                jsonObject.add("splitAmong", array)
            }

            if (src.splitAmong.isNullOrEmpty() && !src.exemptions.isNullOrEmpty()) {
                val array = JsonArray()
                src.exemptions.forEach { array.add(it) }
                jsonObject.add("exemptions", array)
            }

            if (src.parentId != null) jsonObject.addProperty("parentId", src.parentId)
            if (src.distributionTotal != null) jsonObject.addProperty("distributionTotal", src.distributionTotal)
            return jsonObject
        }
    }

    private val gson = GsonBuilder()
        .registerTypeAdapter(Transaction::class.java, TransactionSerializer())
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create()

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        api = retrofit.create(GitHubApi::class.java)
    }

    // --- CONFIGURATION METHODS ---
    fun getSavedRepos(): Flow<Set<String>> = dataStore.savedReposFlow

    suspend fun removeSavedRepo(url: String) {
        dataStore.removeSavedRepo(url)
    }

    suspend fun setActiveConfig(url: String): AppConfig? = withContext(Dispatchers.IO) {
        try {
            val config = api.fetchConfig(url)
            val json = gson.toJson(config)
            dataStore.saveConfigCache(json)
            dataStore.saveConfigUrl(url)
            dataStore.addSavedRepo(url, config.siteTitle)
            return@withContext config
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    suspend fun getAppConfig(): AppConfig? {
        return dataStore.getConfigCache()
    }

    suspend fun updateRemoteConfig(token: String, newConfig: AppConfig): Boolean = withContext(Dispatchers.IO) {
        try {
            val configUrl = dataStore.configUrlFlow.firstOrNull() ?: return@withContext false
            val (owner, repo, path) = parseGitHubUrl(configUrl) ?: return@withContext false

            val authHeader = "token $token"
            val fileDetails = api.getFileDetails(authHeader, owner, repo, path)

            val rawJson = gson.toJson(newConfig)
            // Compact JSON to single line inner objects
            val compactJson = rawJson.replace(Regex("""\{\s*([^{}]+?)\s*\}""", RegexOption.DOT_MATCHES_ALL)) { matchResult ->
                "{ " + matchResult.groupValues[1].replace(Regex("""\s+"""), " ").trim() + " }"
            }

            val encodedContent = Base64.encodeToString(compactJson.toByteArray(), Base64.NO_WRAP)
            
            val request = UpdateFileRequest(
                message = "App: Update Configuration",
                content = encodedContent,
                sha = fileDetails.sha
            )

            api.updateFile(authHeader, owner, repo, path, request)
            dataStore.saveConfigCache(rawJson)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun parseGitHubUrl(url: String): Triple<String, String, String>? {
        try {
            val uri = URI(url)
            val host = uri.host 
            val path = uri.path.trimStart('/') 
            if (host.contains("github.io")) {
                val owner = host.split(".")[0]
                val parts = path.split("/", limit = 2)
                if (parts.size == 2) return Triple(owner, parts[0], parts[1])
            }
        } catch (e: Exception) { e.printStackTrace() }
        return null
    }

    // --- DATA METHODS ---
    suspend fun getCachedData(): KharchaData? = withContext(Dispatchers.IO) {
        val json = dataStore.getCache()
        if (!json.isNullOrEmpty()) {
            try { return@withContext gson.fromJson(json, KharchaData::class.java) } catch (e: Exception) { e.printStackTrace() }
        }
        return@withContext null
    }

    suspend fun fetchData(): KharchaData? = withContext(Dispatchers.IO) {
        val config = getAppConfig() ?: return@withContext null
        try {
            val dataUrl = "https://${config.repoOwner}.github.io/${config.repoName}/${config.dataFileName}?t=${System.currentTimeMillis()}"
            val data = api.getPublicData(dataUrl)
            val json = gson.toJson(data)
            dataStore.saveCache(json)
            return@withContext data
        } catch (e: Exception) { e.printStackTrace(); return@withContext null }
    }

    suspend fun verifyToken(token: String): Boolean = withContext(Dispatchers.IO) {
        val config = getAppConfig() ?: return@withContext false
        try {
            api.getFileDetails("token $token", config.repoOwner, config.repoName, config.dataFileName)
            true
        } catch (e: Exception) { false }
    }

    private suspend fun getLatestDataAndContext(token: String): Triple<String, String?, KharchaData>? {
        val config = getAppConfig() ?: return null
        val authHeader = "token $token"
        
        try {
            val fileDetails = api.getFileDetails(authHeader, config.repoOwner, config.repoName, config.dataFileName)
            val currentJson = String(Base64.decode(fileDetails.content, Base64.NO_WRAP))
            val currentData = gson.fromJson(currentJson, KharchaData::class.java)
            return Triple(authHeader, fileDetails.sha, currentData)
        } catch (e: HttpException) {
            if (e.code() == 404) {
                // Initialize empty data if file doesn't exist
                val defaultData = KharchaData(mutableMapOf(), mutableMapOf(), mutableListOf())
                return Triple(authHeader, null, defaultData)
            }
            return null
        } catch (e: Exception) {
            return null
        }
    }

    private suspend fun commitData(authHeader: String, data: KharchaData, sha: String?, msg: String) {
        val config = getAppConfig() ?: return
        
        // Update aggregates (V1 legacy support)
        if (config.members.isNotEmpty()) data.people.putAll(config.members.associate { it.id to 0.0 })
        if (config.billTypes.isNotEmpty()) data.billTypes.putAll(config.billTypes.associate { it.id to 0.0 })
        
        val rawJson = gson.toJson(data)
        
        // Compact Arrays for cleaner git diffs
        val compactJson = rawJson.replace(Regex(""""splitAmong":\s*\[(.*?)\]""", RegexOption.DOT_MATCHES_ALL)) { 
            "\"splitAmong\": [" + it.groupValues[1].replace(Regex("""\s+"""), " ").trim() + "]"
        }.replace(Regex(""""exemptions":\s*\[(.*?)\]""", RegexOption.DOT_MATCHES_ALL)) { 
            "\"exemptions\": [" + it.groupValues[1].replace(Regex("""\s+"""), " ").trim() + "]"
        }

        val encodedContent = Base64.encodeToString(compactJson.toByteArray(), Base64.NO_WRAP)
        val request = UpdateFileRequest(message = "App: $msg", content = encodedContent, sha = sha)
        
        api.updateFile(authHeader, config.repoOwner, config.repoName, config.dataFileName, request)
        dataStore.saveCache(rawJson)
    }

    suspend fun addTransaction(token: String, newTx: Transaction): Boolean = withContext(Dispatchers.IO) {
        try {
            val ctx = getLatestDataAndContext(token) ?: return@withContext false
            val (authHeader, sha, currentData) = ctx
            currentData.transactions.add(0, newTx)
            commitData(authHeader, currentData, sha, "Add ${newTx.type}")
            true
        } catch (e: Exception) { e.printStackTrace(); false }
    }

    // --- NEW FEATURE: Distribution ---
    suspend fun addDistribution(token: String, totalAmount: Double, note: String, date: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val ctx = getLatestDataAndContext(token) ?: return@withContext false
            val (authHeader, sha, currentData) = ctx
            val config = getAppConfig() ?: return@withContext false
            
            val activeMembers = config.members.filter { it.active }.map { it.id }
            if (activeMembers.isEmpty()) return@withContext false
            
            val splitAmount = totalAmount / activeMembers.size
            val parentId = "tx_dist_${System.currentTimeMillis()}"
            
            // Create a credit transaction for EACH active member
            activeMembers.forEachIndexed { index, personId ->
                val tx = Transaction(
                    id = "${parentId}_$index", 
                    type = "credit", 
                    payerId = personId, 
                    whoOrBill = personId, // fallback
                    note = note.ifEmpty { "Distribution" }, 
                    amount = splitAmount, 
                    date = date, 
                    parentId = parentId, 
                    distributionTotal = totalAmount
                )
                currentData.transactions.add(0, tx)
            }
            commitData(authHeader, currentData, sha, "Distribution of $totalAmount")
            true
        } catch (e: Exception) { e.printStackTrace(); false }
    }

    // --- NEW FEATURE: Settlement ---
    suspend fun addSettlement(token: String, payerId: String, receiverId: String, amount: Double, note: String, date: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val ctx = getLatestDataAndContext(token) ?: return@withContext false
            val (authHeader, sha, currentData) = ctx
            val parentId = "tx_set_${System.currentTimeMillis()}"
            
            val payerConfig = getAppConfig()?.members?.find { it.id == payerId }
            val receiverConfig = getAppConfig()?.members?.find { it.id == receiverId }
            val payerName = payerConfig?.name ?: payerId
            val receiverName = receiverConfig?.name ?: receiverId

            // Payer (Gave Cash -> Balance goes up)
            val payerTx = Transaction(
                id = "${parentId}_payer", type = "credit", 
                payerId = payerId, whoOrBill = payerId,
                note = "Settlement to $receiverName" + (if(note.isNotEmpty()) ": $note" else ""),
                amount = amount, date = date, parentId = parentId
            )
            
            // Receiver (Got Cash -> Balance goes down)
            val receiverTx = Transaction(
                id = "${parentId}_rcvr", type = "credit", 
                payerId = receiverId, whoOrBill = receiverId,
                note = "Settlement from $payerName" + (if(note.isNotEmpty()) ": $note" else ""),
                amount = -amount, date = date, parentId = parentId
            )
            
            currentData.transactions.add(0, payerTx)
            currentData.transactions.add(0, receiverTx)
            commitData(authHeader, currentData, sha, "Settlement")
            true
        } catch (e: Exception) { e.printStackTrace(); false }
    }

    // --- NEW FEATURE: Transfer ---
    suspend fun addTransfer(token: String, senderId: String, recipientId: String, amount: Double, note: String, date: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val ctx = getLatestDataAndContext(token) ?: return@withContext false
            val (authHeader, sha, currentData) = ctx
            val parentId = "tx_trf_${System.currentTimeMillis()}"
            
            val senderName = getAppConfig()?.members?.find { it.id == senderId }?.name ?: senderId
            val recipientName = getAppConfig()?.members?.find { it.id == recipientId }?.name ?: recipientId

            // Sender (Gave Fund -> Balance goes down)
            val senderTx = Transaction(
                id = "${parentId}_send", type = "credit", 
                payerId = senderId, whoOrBill = senderId,
                note = "Transfer to $recipientName" + (if(note.isNotEmpty()) ": $note" else ""),
                amount = -amount, date = date, parentId = parentId
            )
            
            // Recipient (Got Fund -> Balance goes up)
            val recipientTx = Transaction(
                id = "${parentId}_rcpt", type = "credit", 
                payerId = recipientId, whoOrBill = recipientId,
                note = "Transfer from $senderName" + (if(note.isNotEmpty()) ": $note" else ""),
                amount = amount, date = date, parentId = parentId
            )
            
            currentData.transactions.add(0, senderTx)
            currentData.transactions.add(0, recipientTx)
            commitData(authHeader, currentData, sha, "Transfer")
            true
        } catch (e: Exception) { e.printStackTrace(); false }
    }

    suspend fun deleteTransaction(token: String, transactionId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val ctx = getLatestDataAndContext(token) ?: return@withContext false
            val (authHeader, sha, currentData) = ctx
            val targetTx = currentData.transactions.find { it.id == transactionId } ?: return@withContext false
            
            // Delete group if parentId exists
            val toDelete = if (targetTx.parentId != null) 
                currentData.transactions.filter { it.parentId == targetTx.parentId } 
            else listOf(targetTx)
            
            currentData.transactions.removeAll(toDelete)
            commitData(authHeader, currentData, sha, "Delete transaction")
            true
        } catch (e: Exception) { e.printStackTrace(); false }
    }

    suspend fun editTransaction(token: String, updatedTx: Transaction): Boolean = withContext(Dispatchers.IO) {
        try {
            val ctx = getLatestDataAndContext(token) ?: return@withContext false
            val (authHeader, sha, currentData) = ctx
            val index = currentData.transactions.indexOfFirst { it.id == updatedTx.id }
            if (index == -1) return@withContext false
            currentData.transactions[index] = updatedTx
            commitData(authHeader, currentData, sha, "Edit transaction")
            true
        } catch (e: Exception) { e.printStackTrace(); false }
    }

    // --- BALANCE CALCULATION (Matches Web Logic) ---
    suspend fun calculateBalances(data: KharchaData): Map<String, Double> {
        val config = dataStore.getConfigCache() ?: return emptyMap()
        val balances = config.members.associate { it.id to 0.0 }.toMutableMap()
        
        data.transactions.forEach { tx ->
            if (tx.type == "credit") {
                // Direct credit to user
                val pid = tx.payerId ?: tx.whoOrBill
                balances[pid] = (balances[pid] ?: 0.0) + tx.amount
            } else {
                // Debit (Shared Bill)
                val payers = if (!tx.splitAmong.isNullOrEmpty()) {
                    // V2: Use snapshot list
                    tx.splitAmong
                } else {
                    // V1: Use exemptions logic (All - Exempt)
                    val exemptions = tx.exemptions ?: emptyList()
                    config.members.map { it.id }.filter { !exemptions.contains(it) }
                }
                
                if (payers.isNotEmpty()) {
                    val splitAmount = tx.amount / payers.size
                    payers.forEach { pid ->
                        balances[pid] = (balances[pid] ?: 0.0) - splitAmount
                    }
                }
            }
        }
        return balances
    }
}
