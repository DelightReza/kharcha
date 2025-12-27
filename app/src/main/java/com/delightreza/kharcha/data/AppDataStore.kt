package com.delightreza.kharcha.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "settings")

class AppDataStore(private val context: Context) {
    companion object {
        val GITHUB_TOKEN = stringPreferencesKey("github_token")
    }

    val tokenFlow: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[GITHUB_TOKEN] }

    suspend fun saveToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[GITHUB_TOKEN] = token
        }
    }
    
    suspend fun clearToken() {
        context.dataStore.edit { it.remove(GITHUB_TOKEN) }
    }
}
