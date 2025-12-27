package com.delightreza.kharcha.utils

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DateUtils {
    // Returns "2025-12-27 21:30" (Local Time)
    fun formatToLocal(isoString: String): String {
        return try {
            val instant = Instant.parse(isoString)
            val zoneId = ZoneId.systemDefault() // Gets device's current timezone
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            instant.atZone(zoneId).format(formatter)
        } catch (e: Exception) {
            isoString.replace("T", " ").take(16) // Fallback
        }
    }

    // Returns "2025-12-27" (Local Date)
    fun formatToLocalDateOnly(isoString: String): String {
        return try {
            val instant = Instant.parse(isoString)
            val zoneId = ZoneId.systemDefault()
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            instant.atZone(zoneId).format(formatter)
        } catch (e: Exception) {
            isoString.split("T")[0] // Fallback
        }
    }
}
