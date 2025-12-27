package com.delightreza.kharcha.data

import android.util.Base64
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class Repository {
    private val api: GitHubApi

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        api = retrofit.create(GitHubApi::class.java)
    }

    suspend fun fetchData(): KharchaData? = withContext(Dispatchers.IO) {
        try {
            api.getPublicData(System.currentTimeMillis())
        } catch (e: Exception) { e.printStackTrace(); null }
    }

    // --- NEW: Verification Logic ---
    suspend fun verifyToken(token: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Attempt to fetch file details with the token
            // If token is invalid or has no access, this throws an exception (401/404)
            api.getFileDetails("token $token")
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun addTransaction(token: String, currentData: KharchaData, newTx: Transaction): Boolean = withContext(Dispatchers.IO) {
        try {
            currentData.transactions.add(0, newTx)
            if (newTx.type == "credit") {
                val oldVal = currentData.people[newTx.whoOrBill] ?: 0.0
                currentData.people[newTx.whoOrBill] = oldVal + newTx.amount
            } else {
                val oldVal = currentData.billTypes[newTx.whoOrBill] ?: 0.0
                currentData.billTypes[newTx.whoOrBill] = oldVal + newTx.amount
            }

            val authHeader = "token $token"
            val fileDetails = api.getFileDetails(authHeader)
            
            val jsonString = Gson().toJson(currentData)
            val encodedContent = Base64.encodeToString(jsonString.toByteArray(), Base64.NO_WRAP)
            
            val request = UpdateFileRequest(
                message = "Mobile App: Add ${newTx.type} - ${newTx.amount}",
                content = encodedContent,
                sha = fileDetails.sha
            )

            api.updateFile(authHeader, request)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
