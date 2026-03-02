package com.delightreza.kharcha.data

import com.google.gson.annotations.SerializedName

// --- CONFIGURATION MODEL (V2 Structure) ---
data class AppConfig(
    val siteTitle: String = "Fund",
    val siteSubtitle: String = "Expense Tracker",
    val currency: String = "₹",
    
    val repoOwner: String = "",
    val repoName: String = "",
    val repoBranch: String = "main",
    val dataFileName: String = "data.json",
    
    // FIX: Map JSON key "people" to Kotlin "members"
    @SerializedName("people")
    val members: List<MemberConfig> = emptyList(),
    
    @SerializedName("billTypes")
    val billTypes: List<BillTypeConfig> = emptyList()
)

data class MemberConfig(
    val id: String,
    val name: String,
    val active: Boolean = true
)

data class BillTypeConfig(
    val id: String,
    val name: String,
    val icon: String,
    val active: Boolean = true
)

// --- DATA MODEL ---
data class KharchaData(
    // Maps for legacy structure, largely ignored in V2 logic
    val people: MutableMap<String, Double> = mutableMapOf(), 
    val billTypes: MutableMap<String, Double> = mutableMapOf(),
    
    val transactions: MutableList<Transaction> = mutableListOf()
)

data class Transaction(
    val id: String,
    val type: String, // "credit", "debit"
    
    // V2 Fields
    val payerId: String? = null,
    val billTypeId: String? = null,
    val splitAmong: List<String>? = null,
    
    // Legacy / Display fallback
    var whoOrBill: String = "", 
    
    val note: String,
    val amount: Double,
    val date: String, // Stored as UTC ISO String
    
    // Legacy Field
    val exemptions: List<String>? = null,
    
    // Grouping
    val parentId: String? = null,
    val distributionTotal: Double? = null
)

// GitHub API Request/Response
data class GitHubFileResponse(val sha: String, val content: String)

data class UpdateFileRequest(
    val message: String,
    val content: String, // Base64
    val sha: String? 
)
