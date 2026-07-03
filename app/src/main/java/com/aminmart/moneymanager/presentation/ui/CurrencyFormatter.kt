package com.aminmart.moneymanager.presentation.ui

import java.text.NumberFormat
import java.util.Locale

/**
 * Central currency formatting. Single source of truth for how monetary values
 * are rendered across the app, so changing locale/currency later touches one file.
 */
object CurrencyFormatter {

    private val locale = Locale("id", "ID")

    private val formatter: NumberFormat = NumberFormat.getCurrencyInstance(locale).apply {
        maximumFractionDigits = 0
    }

    /** e.g. "Rp1.500.000". */
    fun format(amount: Double): String = formatter.format(amount)

    fun format(amount: Long): String = formatter.format(amount)

    /** Signed amount for income (+) / expense (-) display. */
    fun formatSigned(amount: Double, isIncome: Boolean): String {
        val sign = if (isIncome) "+" else "-"
        return "$sign${format(amount)}"
    }
}
