package com.delightreza.kharcha.data

import com.google.gson.annotations.SerializedName

data class KharchaData(
    val people: MutableMap<String, Double> = mutableMapOf(),
    val billTypes: MutableMap<String, Double> = mutableMapOf(),
    val transactions: MutableList<Transaction> = mutableListOf()
)

data class Transaction(
    val id: String,
    val type: String, // "credit" or "debit"
    val whoOrBill: String,
    val note: String,
    val amount: Double,
    val date: String,
    val exemptions: List<String>? = null, // Optional for credit
    val parentId: String? = null
)

// GitHub API Request/Response
data class GitHubFileResponse(val sha: String, val content: String)
data class UpdateFileRequest(
    val message: String,
    val content: String, // Base64
    val sha: String
)
