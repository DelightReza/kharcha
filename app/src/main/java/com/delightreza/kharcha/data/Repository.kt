package com.delightreza.kharcha.data

import android.util.Base64
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
    
    // CUSTOM SERIALIZER: Enforces the exact field order
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
            val authHeader = "token $token"
            val fileDetails = api.getFileDetails(authHeader)
            val currentJson = String(Base64.decode(fileDetails.content, Base64.NO_WRAP))
            val currentData = gson.fromJson(currentJson, KharchaData::class.java)

            currentData.transactions.add(0, newTx)
            
            if (newTx.type == "credit") {
                val oldVal = currentData.people[newTx.whoOrBill] ?: 0.0
                currentData.people[newTx.whoOrBill] = oldVal + newTx.amount
            } else {
                val oldVal = currentData.billTypes[newTx.whoOrBill] ?: 0.0
                currentData.billTypes[newTx.whoOrBill] = oldVal + newTx.amount
            }

            commitData(authHeader, currentData, fileDetails.sha, "Add ${newTx.type}")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // NEW: Function to handle Distribution (Bulk Transactions)
    suspend fun addDistribution(token: String, totalAmount: Double, note: String, date: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val authHeader = "token $token"
            val fileDetails = api.getFileDetails(authHeader)
            val currentJson = String(Base64.decode(fileDetails.content, Base64.NO_WRAP))
            val currentData = gson.fromJson(currentJson, KharchaData::class.java)

            val people = listOf("Raza", "Salman", "Mujeeb", "Gulam", "Rana", "Naved", "Musawwar", "Nizamuddin")
            val splitAmount = totalAmount / people.size
            val parentId = "tx_dist_${System.currentTimeMillis()}"

            // Create transactions for everyone
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
                
                // Add to list
                currentData.transactions.add(0, tx)
                
                // Update totals
                val oldVal = currentData.people[person] ?: 0.0
                currentData.people[person] = oldVal + splitAmount
            }

            commitData(authHeader, currentData, fileDetails.sha, "Distribution of $totalAmount")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private suspend fun commitData(authHeader: String, data: KharchaData, sha: String, msg: String) {
        val jsonContent = gson.toJson(data)
        val encodedContent = Base64.encodeToString(jsonContent.toByteArray(), Base64.NO_WRAP)
        
        val request = UpdateFileRequest(
            message = "Mobile App: $msg",
            content = encodedContent,
            sha = sha
        )

        api.updateFile(authHeader, request)
        
        if (dataStore != null) {
            dataStore.saveCache(jsonContent)
        }
    }

    fun calculateBalances(data: KharchaData): Map<String, Double> {
        val peopleList = listOf("Raza", "Salman", "Mujeeb", "Gulam", "Rana", "Naved", "Musawwar", "Nizamuddin")
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
