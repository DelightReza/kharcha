package com.delightreza.kharcha.data

import android.util.Base64
import com.delightreza.kharcha.utils.Constants
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Type

class Repository(private val dataStore: AppDataStore) {
    private val api: GitHubApi
    
    // Custom Serializer to handle Double/Int formatting for JSON
    private class TransactionSerializer : JsonSerializer<Transaction> {
        override fun serialize(src: Transaction, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
            val jsonObject = JsonObject()
            jsonObject.addProperty("id", src.id)
            jsonObject.addProperty("type", src.type)
            jsonObject.addProperty("whoOrBill", src.whoOrBill)
            jsonObject.addProperty("note", src.note)
            
            if (src.amount % 1.0 == 0.0) {
                jsonObject.addProperty("amount", src.amount.toInt())
            } else {
                jsonObject.addProperty("amount", src.amount)
            }
            
            jsonObject.addProperty("date", src.date)

            if (!src.exemptions.isNullOrEmpty()) jsonObject.add("exemptions", context.serialize(src.exemptions))
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

    suspend fun setActiveConfig(url: String): AppConfig? = withContext(Dispatchers.IO) {
        try {
            // 1. Fetch Config
            val config = api.fetchConfig(url)
            
            // 2. Save Config & URL
            val json = gson.toJson(config)
            dataStore.saveConfigCache(json)
            dataStore.saveConfigUrl(url)
            
            return@withContext config
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    suspend fun getAppConfig(): AppConfig? {
        return dataStore.getConfigCache()
    }

    // --- DATA METHODS ---

    suspend fun getCachedData(): KharchaData? = withContext(Dispatchers.IO) {
        val json = dataStore.getCache()
        if (!json.isNullOrEmpty()) {
            try {
                return@withContext gson.fromJson(json, KharchaData::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return@withContext null
    }

    suspend fun fetchData(): KharchaData? = withContext(Dispatchers.IO) {
        val config = getAppConfig() ?: return@withContext null
        
        try {
            // Construct Data URL: https://{owner}.github.io/{repo}/{filename}?t={timestamp}
            val dataUrl = "https://${config.repoOwner}.github.io/${config.repoName}/${config.dataFileName}?t=${System.currentTimeMillis()}"
            val data = api.getPublicData(dataUrl)
            
            val json = gson.toJson(data)
            dataStore.saveCache(json)
            
            return@withContext data
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    suspend fun verifyToken(token: String): Boolean = withContext(Dispatchers.IO) {
        val config = getAppConfig() ?: return@withContext false
        try {
            // Verify by trying to fetch file metadata
            api.getFileDetails("token $token", config.repoOwner, config.repoName, config.dataFileName)
            true
        } catch (e: Exception) { false }
    }

    // --- WRITE OPERATIONS ---

    private suspend fun getLatestDataAndContext(token: String): Triple<String, GitHubFileResponse, KharchaData>? {
        val config = getAppConfig() ?: return null
        val authHeader = "token $token"
        
        val fileDetails = api.getFileDetails(authHeader, config.repoOwner, config.repoName, config.dataFileName)
        val currentJson = String(Base64.decode(fileDetails.content, Base64.NO_WRAP))
        val currentData = gson.fromJson(currentJson, KharchaData::class.java)
        
        return Triple(authHeader, fileDetails, currentData)
    }

    private suspend fun commitData(authHeader: String, data: KharchaData, sha: String, msg: String) {
        val config = getAppConfig() ?: return
        val jsonContent = gson.toJson(data)
        val encodedContent = Base64.encodeToString(jsonContent.toByteArray(), Base64.NO_WRAP)
        
        val request = UpdateFileRequest(
            message = "App: $msg",
            content = encodedContent,
            sha = sha
        )

        api.updateFile(authHeader, config.repoOwner, config.repoName, config.dataFileName, request)
        dataStore.saveCache(jsonContent)
    }

    suspend fun addTransaction(token: String, newTx: Transaction): Boolean = withContext(Dispatchers.IO) {
        try {
            val ctx = getLatestDataAndContext(token) ?: return@withContext false
            val (authHeader, fileDetails, currentData) = ctx

            currentData.transactions.add(0, newTx)
            updateTotals(currentData, newTx, add = true)

            commitData(authHeader, currentData, fileDetails.sha, "Add ${newTx.type}")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // (Simplified logic for other adds to save space - they follow same pattern)
    suspend fun addDistribution(token: String, totalAmount: Double, note: String, date: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val ctx = getLatestDataAndContext(token) ?: return@withContext false
            val (authHeader, fileDetails, currentData) = ctx
            
            // Use people from Config for distribution logic, or fallback to Data if empty
            val config = getAppConfig()
            val people = if (!config?.people.isNullOrEmpty()) config!!.people.sorted() else currentData.people.keys.toList().sorted()
            
            if (people.isEmpty()) return@withContext false

            val splitAmount = totalAmount / people.size
            val parentId = "tx_dist_${System.currentTimeMillis()}"

            people.forEachIndexed { index, person ->
                val tx = Transaction(
                    id = "${parentId}_$index", type = "credit", whoOrBill = person, note = note,
                    amount = splitAmount, date = date, parentId = parentId, distributionTotal = totalAmount
                )
                currentData.transactions.add(0, tx)
                updateTotals(currentData, tx, add = true)
            }
            commitData(authHeader, currentData, fileDetails.sha, "Distribution of $totalAmount")
            true
        } catch (e: Exception) { e.printStackTrace(); false }
    }

    suspend fun addSettlement(token: String, payer: String, receiver: String, amount: Double, note: String, date: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val ctx = getLatestDataAndContext(token) ?: return@withContext false
            val (authHeader, fileDetails, currentData) = ctx
            val parentId = "tx_set_${System.currentTimeMillis()}"

            val payerTx = Transaction(id = "${parentId}_payer", type = "credit", whoOrBill = payer, note = if(note.isNotEmpty()) "Settlement: $note" else "Settlement to $receiver", amount = amount, date = date, parentId = parentId)
            val receiverTx = Transaction(id = "${parentId}_rcvr", type = "credit", whoOrBill = receiver, note = if(note.isNotEmpty()) "Settlement: $note" else "Settlement from $payer", amount = -amount, date = date, parentId = parentId)

            currentData.transactions.add(0, payerTx)
            currentData.transactions.add(0, receiverTx)
            updateTotals(currentData, payerTx, add = true)
            updateTotals(currentData, receiverTx, add = true)

            commitData(authHeader, currentData, fileDetails.sha, "Settlement: $payer -> $receiver")
            true
        } catch (e: Exception) { e.printStackTrace(); false }
    }

    suspend fun addTransfer(token: String, sender: String, recipient: String, amount: Double, note: String, date: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val ctx = getLatestDataAndContext(token) ?: return@withContext false
            val (authHeader, fileDetails, currentData) = ctx
            val parentId = "tx_trf_${System.currentTimeMillis()}"

            val senderTx = Transaction(id = "${parentId}_send", type = "credit", whoOrBill = sender, note = if(note.isNotEmpty()) "Transfer: $note" else "Transfer to $recipient", amount = -amount, date = date, parentId = parentId)
            val recipientTx = Transaction(id = "${parentId}_rcpt", type = "credit", whoOrBill = recipient, note = if(note.isNotEmpty()) "Transfer: $note" else "Transfer from $sender", amount = amount, date = date, parentId = parentId)

            currentData.transactions.add(0, senderTx)
            currentData.transactions.add(0, recipientTx)
            updateTotals(currentData, senderTx, add = true)
            updateTotals(currentData, recipientTx, add = true)

            commitData(authHeader, currentData, fileDetails.sha, "Transfer: $sender -> $recipient")
            true
        } catch (e: Exception) { e.printStackTrace(); false }
    }

    suspend fun deleteTransaction(token: String, transactionId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val ctx = getLatestDataAndContext(token) ?: return@withContext false
            val (authHeader, fileDetails, currentData) = ctx
            val targetTx = currentData.transactions.find { it.id == transactionId } ?: return@withContext false
            
            val toDelete = if (targetTx.parentId != null) {
                currentData.transactions.filter { it.parentId == targetTx.parentId }
            } else {
                listOf(targetTx)
            }

            toDelete.forEach { tx ->
                updateTotals(currentData, tx, add = false) 
                currentData.transactions.remove(tx)
            }
            commitData(authHeader, currentData, fileDetails.sha, "Delete transaction")
            true
        } catch (e: Exception) { e.printStackTrace(); false }
    }

    suspend fun editTransaction(token: String, updatedTx: Transaction): Boolean = withContext(Dispatchers.IO) {
        try {
            val ctx = getLatestDataAndContext(token) ?: return@withContext false
            val (authHeader, fileDetails, currentData) = ctx
            val index = currentData.transactions.indexOfFirst { it.id == updatedTx.id }
            if (index == -1) return@withContext false
            
            val oldTx = currentData.transactions[index]
            updateTotals(currentData, oldTx, add = false)
            updateTotals(currentData, updatedTx, add = true)
            currentData.transactions[index] = updatedTx

            commitData(authHeader, currentData, fileDetails.sha, "Edit transaction")
            true
        } catch (e: Exception) { e.printStackTrace(); false }
    }

    private fun updateTotals(data: KharchaData, tx: Transaction, add: Boolean) {
        val multiplier = if (add) 1.0 else -1.0
        val amount = tx.amount * multiplier

        if (tx.type == "credit") {
            val oldVal = data.people[tx.whoOrBill] ?: 0.0
            data.people[tx.whoOrBill] = oldVal + amount
        } else {
            val oldVal = data.billTypes[tx.whoOrBill] ?: 0.0
            data.billTypes[tx.whoOrBill] = oldVal + amount
        }
    }

    fun calculateBalances(data: KharchaData): Map<String, Double> {
        val peopleKeys = data.people.keys
        val peopleList = if (peopleKeys.isNotEmpty()) peopleKeys.toList() else Constants.DEFAULT_MEMBERS
        val balances = peopleList.associateWith { 0.0 }.toMutableMap()

        data.transactions.forEach { tx ->
            if (tx.type == "credit") {
                if (!balances.containsKey(tx.whoOrBill)) balances[tx.whoOrBill] = 0.0
                val current = balances[tx.whoOrBill] ?: 0.0
                balances[tx.whoOrBill] = current + tx.amount
            } else {
                val exemptions = tx.exemptions ?: emptyList()
                val contributors = peopleList.filter { !exemptions.contains(it) }
                if (contributors.isNotEmpty()) {
                    val splitAmount = tx.amount / contributors.size
                    contributors.forEach { person ->
                        if (!balances.containsKey(person)) balances[person] = 0.0
                        val current = balances[person] ?: 0.0
                        balances[person] = current - splitAmount
                    }
                }
            }
        }
        return balances
    }
}
