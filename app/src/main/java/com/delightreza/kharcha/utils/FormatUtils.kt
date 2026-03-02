package com.delightreza.kharcha.utils

import java.util.Locale

object FormatUtils {
    /**
     * Formats a Double.
     * If whole number: "1,000"
     * If decimal: "1,000.50"
     */
    fun formatAmount(amount: Double): String {
        return if (amount % 1.0 == 0.0) {
            String.format(Locale.US, "%,d", amount.toInt())
        } else {
            String.format(Locale.US, "%,.2f", amount)
        }
    }
}
