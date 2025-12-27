package com.delightreza.kharcha.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "settings")

class AppDataStore(private val context: Context) {
    companion object {
        val GITHUB_TOKEN = stringPreferencesKey("github_token")
        val SELECTED_USER = stringPreferencesKey("selected_user")
        val CACHED_DATA = stringPreferencesKey("cached_json_data")
    }

    val tokenFlow: Flow<String?> = context.dataStore.data.map { it[GITHUB_TOKEN] }
    val userFlow: Flow<String?> = context.dataStore.data.map { it[SELECTED_USER] }

    suspend fun saveToken(token: String) {
        context.dataStore.edit { it[GITHUB_TOKEN] = token }
    }
    
    suspend fun saveUser(user: String) {
        context.dataStore.edit { it[SELECTED_USER] = user }
    }

    // Cache Methods
    suspend fun saveCache(json: String) {
        context.dataStore.edit { it[CACHED_DATA] = json }
    }

    suspend fun getCache(): String? {
        return context.dataStore.data.map { it[CACHED_DATA] }.first()
    }

    suspend fun clearUser() {
        context.dataStore.edit { it.remove(SELECTED_USER) }
    }
}
