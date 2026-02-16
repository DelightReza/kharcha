package com.delightreza.kharcha.data

import com.google.gson.annotations.SerializedName

// --- CONFIGURATION MODEL (From config.json) ---
data class AppConfig(
    val siteTitle: String = "Kharcha",
    val siteSubtitle: String = "House Fund",
    val currency: String = "SOM",
    val timeOffset: Double = 6.0,
    val repoOwner: String,
    val repoName: String,
    val repoBranch: String = "main",
    val dataFileName: String = "data.json",
    val people: List<String> = emptyList(),
    val billTypes: List<BillTypeConfig> = emptyList()
)

data class BillTypeConfig(
    val name: String,
    val icon: String
)

// --- DATA MODEL (From data.json) ---
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
    
    // Optionals
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
