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

class Repository(private val dataStore: AppDataStore? = null) {
    private val api: GitHubApi
    
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

            if (!src.exemptions.isNullOrEmpty()) {
                jsonObject.add("exemptions", context.serialize(src.exemptions))
            }
            if (src.parentId != null) {
                jsonObject.addProperty("parentId", src.parentId)
            }
            if (src.distributionTotal != null) {
                jsonObject.addProperty("distributionTotal", src.distributionTotal)
            }

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

    suspend fun getCachedData(): KharchaData? = withContext(Dispatchers.IO) {
        if (dataStore == null) return@withContext null
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
        try {
            val data = api.getPublicData(System.currentTimeMillis())
            if (dataStore != null) {
                val json = gson.toJson(data)
                dataStore.saveCache(json)
            }
            return@withContext data
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    suspend fun verifyToken(token: String): Boolean = withContext(Dispatchers.IO) {
        try {
            api.getFileDetails("token $token")
            true
        } catch (e: Exception) { false }
    }

    suspend fun addTransaction(token: String, newTx: Transaction): Boolean = withContext(Dispatchers.IO) {
        try {
            val (authHeader, fileDetails, currentData) = getLatestData(token)

            currentData.transactions.add(0, newTx)
            updateTotals(currentData, newTx, add = true)

            commitData(authHeader, currentData, fileDetails.sha, "Add ${newTx.type}")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun addDistribution(token: String, totalAmount: Double, note: String, date: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val (authHeader, fileDetails, currentData) = getLatestData(token)

            val people = Constants.MEMBERS
            val splitAmount = totalAmount / people.size
            val parentId = "tx_dist_${System.currentTimeMillis()}"

            people.forEachIndexed { index, person ->
                val tx = Transaction(
                    id = "${parentId}_$index",
                    type = "credit",
                    whoOrBill = person,
                    note = note,
                    amount = splitAmount,
                    date = date,
                    parentId = parentId,
                    distributionTotal = totalAmount
                )
                currentData.transactions.add(0, tx)
                updateTotals(currentData, tx, add = true)
            }

            commitData(authHeader, currentData, fileDetails.sha, "Distribution of $totalAmount")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteTransaction(token: String, transactionId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val (authHeader, fileDetails, currentData) = getLatestData(token)
            
            // Find target. If it has a parentId, we must delete ALL with that parentId (Distribution)
            val targetTx = currentData.transactions.find { it.id == transactionId } ?: return@withContext false
            
            val toDelete = if (targetTx.parentId != null) {
                currentData.transactions.filter { it.parentId == targetTx.parentId }
            } else {
                listOf(targetTx)
            }

            toDelete.forEach { tx ->
                updateTotals(currentData, tx, add = false) // Reverse impact
                currentData.transactions.remove(tx)
            }

            commitData(authHeader, currentData, fileDetails.sha, "Delete transaction ${targetTx.id}")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun editTransaction(token: String, updatedTx: Transaction): Boolean = withContext(Dispatchers.IO) {
        try {
            val (authHeader, fileDetails, currentData) = getLatestData(token)

            val index = currentData.transactions.indexOfFirst { it.id == updatedTx.id }
            if (index == -1) return@withContext false
            val oldTx = currentData.transactions[index]

            // 1. Revert Old
            updateTotals(currentData, oldTx, add = false)
            
            // 2. Apply New
            updateTotals(currentData, updatedTx, add = true)
            
            // 3. Swap in list
            currentData.transactions[index] = updatedTx

            commitData(authHeader, currentData, fileDetails.sha, "Edit transaction ${updatedTx.id}")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Helper to avoid repetition
    private suspend fun getLatestData(token: String): Triple<String, GitHubFileResponse, KharchaData> {
        val authHeader = "token $token"
        val fileDetails = api.getFileDetails(authHeader)
        val currentJson = String(Base64.decode(fileDetails.content, Base64.NO_WRAP))
        val currentData = gson.fromJson(currentJson, KharchaData::class.java)
        return Triple(authHeader, fileDetails, currentData)
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

    private suspend fun commitData(authHeader: String, data: KharchaData, sha: String, msg: String) {
        val jsonContent = gson.toJson(data)
        val encodedContent = Base64.encodeToString(jsonContent.toByteArray(), Base64.NO_WRAP)
        
        val request = UpdateFileRequest(
            message = "App: $msg",
            content = encodedContent,
            sha = sha
        )

        api.updateFile(authHeader, request)
        
        if (dataStore != null) {
            dataStore.saveCache(jsonContent)
        }
    }

    fun calculateBalances(data: KharchaData): Map<String, Double> {
        val peopleList = Constants.MEMBERS
        val balances = peopleList.associateWith { 0.0 }.toMutableMap()

        data.transactions.forEach { tx ->
            if (tx.type == "credit") {
                val current = balances[tx.whoOrBill] ?: 0.0
                balances[tx.whoOrBill] = current + tx.amount
            } else {
                val exemptions = tx.exemptions ?: emptyList()
                val contributors = peopleList.filter { !exemptions.contains(it) }
                if (contributors.isNotEmpty()) {
                    val splitAmount = tx.amount / contributors.size
                    contributors.forEach { person ->
                        val current = balances[person] ?: 0.0
                        balances[person] = current - splitAmount
                    }
                }
            }
        }
        return balances
    }
}
