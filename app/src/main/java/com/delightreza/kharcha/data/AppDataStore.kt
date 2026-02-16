package com.delightreza.kharcha.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "settings")

class AppDataStore(private val context: Context) {
    companion object {
        val GITHUB_TOKEN = stringPreferencesKey("github_token")
        val SELECTED_USER = stringPreferencesKey("selected_user")
        val CACHED_DATA = stringPreferencesKey("cached_json_data")
        
        // Active Session
        val CONFIG_URL = stringPreferencesKey("config_url")
        val CACHED_CONFIG = stringPreferencesKey("cached_config_json")

        // History
        val SAVED_REPOS = stringSetPreferencesKey("saved_repo_urls")
    }

    val tokenFlow: Flow<String?> = context.dataStore.data.map { it[GITHUB_TOKEN] }
    val userFlow: Flow<String?> = context.dataStore.data.map { it[SELECTED_USER] }
    val configUrlFlow: Flow<String?> = context.dataStore.data.map { it[CONFIG_URL] }
    val savedReposFlow: Flow<Set<String>> = context.dataStore.data.map { it[SAVED_REPOS] ?: emptySet() }

    suspend fun saveToken(token: String) {
        context.dataStore.edit { it[GITHUB_TOKEN] = token }
    }
    
    suspend fun saveUser(user: String) {
        context.dataStore.edit { it[SELECTED_USER] = user }
    }

    suspend fun saveConfigUrl(url: String) {
        context.dataStore.edit { it[CONFIG_URL] = url }
    }

    // Add to History (Set ensures uniqueness)
    suspend fun addSavedRepo(url: String) {
        context.dataStore.edit { preferences ->
            val currentSet = preferences[SAVED_REPOS] ?: emptySet()
            preferences[SAVED_REPOS] = currentSet + url
        }
    }

    suspend fun removeSavedRepo(url: String) {
        context.dataStore.edit { preferences ->
            val currentSet = preferences[SAVED_REPOS] ?: emptySet()
            preferences[SAVED_REPOS] = currentSet - url
        }
    }

    // Data Cache
    suspend fun saveCache(json: String) {
        context.dataStore.edit { it[CACHED_DATA] = json }
    }

    suspend fun getCache(): String? {
        return context.dataStore.data.map { it[CACHED_DATA] }.first()
    }

    // Config Cache
    suspend fun saveConfigCache(json: String) {
        context.dataStore.edit { it[CACHED_CONFIG] = json }
    }

    suspend fun getConfigCache(): AppConfig? {
        val json = context.dataStore.data.map { it[CACHED_CONFIG] }.first()
        return if (!json.isNullOrEmpty()) {
            try {
                Gson().fromJson(json, AppConfig::class.java)
            } catch (e: Exception) { null }
        } else null
    }

    suspend fun clearUser() {
        context.dataStore.edit { it.remove(SELECTED_USER) }
    }
    
    // Updated: Only clears ACTIVE session data, NOT the saved repo history
    suspend fun clearConfig() {
        context.dataStore.edit { 
            it.remove(CONFIG_URL)
            it.remove(CACHED_CONFIG)
            it.remove(CACHED_DATA)
            it.remove(SELECTED_USER)
            // Note: SAVED_REPOS is intentionally NOT removed
        }
    }
}
