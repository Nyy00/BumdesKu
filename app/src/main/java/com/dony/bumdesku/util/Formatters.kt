package com.dony.bumdesku.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.*

class ThousandSeparatorVisualTransformation : VisualTransformation {
    private val symbols = DecimalFormat().decimalFormatSymbols
    private val thousandsSeparator = symbols.groupingSeparator.toString()

    override fun filter(text: AnnotatedString): TransformedText {
        if (text.text.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }
        val originalText = text.text
        val formattedText = format(originalText)
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val separators = formattedText.count { it.toString() == thousandsSeparator }
                return offset + separators
            }
            override fun transformedToOriginal(offset: Int): Int {
                val separators = formattedText.substring(0, offset).count { it.toString() == thousandsSeparator }
                return offset - separators
            }
        }
        return TransformedText(AnnotatedString(formattedText), offsetMapping)
    }

    private fun format(text: String): String {
        return try {
            val number = text.toLong()
            val formatter = DecimalFormat("#,###")
            formatter.format(number)
        } catch (e: NumberFormatException) {
            text
        }
    }
}


fun formatCurrency(amount: Double): String {
    val localeID = Locale("in", "ID")
    val format = NumberFormat.getCurrencyInstance(localeID)
    return format.format(amount)
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID"))
    return sdf.format(Date(timestamp))
}

fun formatRupiah(number: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    return format.format(number)
}
