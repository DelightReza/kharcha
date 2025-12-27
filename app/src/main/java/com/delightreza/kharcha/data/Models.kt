package com.delightreza.kharcha.data

import com.google.gson.annotations.SerializedName

data class KharchaData(
    val people: MutableMap<String, Double> = mutableMapOf(),
    val billTypes: MutableMap<String, Double> = mutableMapOf(),
    val transactions: MutableList<Transaction> = mutableListOf()
)

data class Transaction(
    // 1. ID
    val id: String,
    
    // 2. Type
    val type: String, // "credit" or "debit"
    
    // 3. Who/Bill
    val whoOrBill: String,
    
    // 4. Note
    val note: String,
    
    // 5. Amount
    val amount: Double,
    
    // 6. Date
    val date: String,
    
    // Optionals (will appear at the end if present)
    val exemptions: List<String>? = null,
    val parentId: String? = null,
    val distributionTotal: Double? = null
)

// GitHub API Request/Response
data class GitHubFileResponse(val sha: String, val content: String)
data class UpdateFileRequest(
    val message: String,
    val content: String, // Base64
    val sha: String
)
