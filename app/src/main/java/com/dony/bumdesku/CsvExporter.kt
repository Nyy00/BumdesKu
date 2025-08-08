package com.dony.bumdesku

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.dony.bumdesku.data.ReportData
import com.dony.bumdesku.data.Transaction
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class CsvExporter(private val context: Context) {

    private val localeID = Locale("in", "ID")
    private val dateFormat = SimpleDateFormat("dd-MM-yyyy", localeID)

    private fun escapeCsv(text: String?): String {
        val safeText = text ?: ""
        if (safeText.contains(",") || safeText.contains("\"") || safeText.contains("\n")) {
            return "\"${safeText.replace("\"", "\"\"")}\""
        }
        return safeText
    }

    private fun writeCsvToStream(outputStream: OutputStream, content: String) {
        outputStream.bufferedWriter().use { writer ->
            writer.write(content)
        }
    }

    private fun getCsvOutputStream(fileName: String): OutputStream? {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
        return uri?.let { resolver.openOutputStream(it) }
    }

    fun exportLaporanKeuanganToCsv(reportData: ReportData, transactions: List<Transaction>, startDate: Date, endDate: Date) {
        val fileName = "Laporan_Keuangan_${dateFormat.format(startDate)}_${dateFormat.format(endDate)}.csv"
        val stringBuilder = StringBuilder()

        stringBuilder.append("LAPORAN KEUANGAN\n")
        stringBuilder.append("Periode,${dateFormat.format(startDate)} - ${dateFormat.format(endDate)}\n")
        stringBuilder.append("\n")

        stringBuilder.append("Ringkasan Laba Rugi\n")
        stringBuilder.append("Total Pendapatan,${reportData.totalIncome.toLong()}\n")
        stringBuilder.append("Total Beban,${reportData.totalExpenses.toLong()}\n")
        stringBuilder.append("Laba/Rugi Bersih,${reportData.netProfit.toLong()}\n")
        stringBuilder.append("\n")

        stringBuilder.append("Rincian Transaksi\n")
        stringBuilder.append("Tanggal,No Bukti,Keterangan,Debit,Kredit\n")
        transactions.forEach { trx ->
            // Baris Debit
            stringBuilder.append(
                "${dateFormat.format(Date(trx.date))}," +
                        "${escapeCsv(trx.id)}," +
                        "${escapeCsv(trx.debitAccountName)}," +
                        "${trx.amount.toLong()}," +
                        "0\n"
            )
            // Baris Kredit
            stringBuilder.append(
                "${dateFormat.format(Date(trx.date))}," +
                        "${escapeCsv(trx.id)}," +
                        "   ${escapeCsv(trx.creditAccountName)}," +
                        "0," +
                        "${trx.amount.toLong()}\n"
            )
        }

        getCsvOutputStream(fileName)?.let {
            writeCsvToStream(it, stringBuilder.toString())
        }
    }
}