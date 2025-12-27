package com.delightreza.kharcha.data

import android.util.Base64
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class Repository(private val dataStore: AppDataStore? = null) {
    private val api: GitHubApi
    
    // FIXED: Use Pretty Printing to keep JSON formatted nicely
    private val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        api = retrofit.create(GitHubApi::class.java)
    }

    // ... (fetchData and getCachedData methods remain the same) ...

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
            // Save to cache
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
            
            // 1. Fetch fresh data explicitly from API
            val fileDetails = api.getFileDetails(authHeader)
            val currentJson = String(Base64.decode(fileDetails.content, Base64.NO_WRAP))
            val currentData = gson.fromJson(currentJson, KharchaData::class.java)

            // 2. Update Data
            currentData.transactions.add(0, newTx)
            
            // Recalculate totals based on the new transaction
            if (newTx.type == "credit") {
                val oldVal = currentData.people[newTx.whoOrBill] ?: 0.0
                currentData.people[newTx.whoOrBill] = oldVal + newTx.amount
            } else {
                val oldVal = currentData.billTypes[newTx.whoOrBill] ?: 0.0
                currentData.billTypes[newTx.whoOrBill] = oldVal + newTx.amount
            }

            // 3. Commit with Pretty Printing
            val jsonContent = gson.toJson(currentData)
            val encodedContent = Base64.encodeToString(jsonContent.toByteArray(), Base64.NO_WRAP)
            
            val request = UpdateFileRequest(
                message = "Mobile App: Add ${newTx.type} - ${newTx.amount}",
                content = encodedContent,
                sha = fileDetails.sha
            )

            api.updateFile(authHeader, request)
            
            // Update cache immediately
            if (dataStore != null) {
                dataStore.saveCache(jsonContent)
            }
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
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
