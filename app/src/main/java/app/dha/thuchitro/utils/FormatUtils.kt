package app.dha.thuchitro.utils

import org.intellij.lang.annotations.Pattern
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.Date
import java.util.Locale

fun Long.toVndCurrency(signal: Boolean = true): String {
    val formatter = DecimalFormat("#,###")

    val symbols = formatter.decimalFormatSymbols
    symbols.groupingSeparator = '.'
    formatter.decimalFormatSymbols = symbols

    return (if (signal && this >0) "+" else "") + formatter.format(this) + "đ"
}

fun Date.format(pattern: String): String{
    val formatter = SimpleDateFormat(pattern, Locale.getDefault())
    return formatter.format(this)
}