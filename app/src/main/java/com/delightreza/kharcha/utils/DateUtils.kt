package com.delightreza.kharcha.utils

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

object DateUtils {

    /**
     * Display Logic
     * Input: "2025-01-20T14:30:00" (Saved String)
     * Output: "2025-01-20 14:30" (Formatted String)
     */
    fun formatToLocal(isoString: String): String {
        return try {
            // Remove 'Z' if present from old data
            val cleanString = isoString.replace("Z", "")
            val parsed = LocalDateTime.parse(cleanString)
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault())
            parsed.format(formatter)
        } catch (e: Exception) {
            // Fallback: Just swap T for space
            isoString.replace("T", " ").take(16)
        }
    }

    /**
     * Output: "2025-01-20"
     */
    fun formatToLocalDateOnly(isoString: String): String {
        return try {
            val cleanString = isoString.replace("Z", "")
            val parsed = LocalDateTime.parse(cleanString)
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())
            parsed.format(formatter)
        } catch (e: Exception) {
            isoString.split("T")[0]
        }
    }

    /**
     * Saving Logic (Current Device Time)
     * Returns: "2025-01-20T14:30:00.123" (No Z, No Offset)
     */
    fun getCurrentTime(): String {
        return LocalDateTime.now().toString()
    }

    /**
     * Saving Logic (From Date Picker)
     * Converts Calendar -> "2025-01-20T14:30:00.000"
     */
    fun getStringFromLocal(calendar: Calendar): String {
        val y = calendar.get(Calendar.YEAR)
        val m = calendar.get(Calendar.MONTH) + 1
        val d = calendar.get(Calendar.DAY_OF_MONTH)
        val h = calendar.get(Calendar.HOUR_OF_DAY)
        val min = calendar.get(Calendar.MINUTE)
        val s = 0
        
        // Manual ISO formatting to ensure local time is preserved exactly
        return String.format(
            Locale.US, 
            "%04d-%02d-%02dT%02d:%02d:%02d", 
            y, m, d, h, min, s
        )
    }
}
