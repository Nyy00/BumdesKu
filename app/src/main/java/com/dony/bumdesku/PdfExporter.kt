package com.dony.bumdesku

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.dony.bumdesku.data.Account
import com.dony.bumdesku.data.AccountCategory
import com.dony.bumdesku.data.ReportData
import com.dony.bumdesku.data.Transaction
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

object PdfExporter {

    fun createReportPdf(
        context: Context,
        reportData: ReportData,
        transactions: List<Transaction>,
        allAccounts: List<Account> // Parameter ini sudah kita tambahkan sebelumnya
    ) {
        val pageHeight = 1120
        val pagewidth = 792

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pagewidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 20f
            isFakeBoldText = true
        }

        val subtitlePaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 16f
        }

        val headerPaint = Paint().apply {
            color = Color.BLACK
            textSize = 15f
            isFakeBoldText = true
        }

        val textPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 14f
        }

        // --- Menggambar konten PDF ---
        var yPosition = 60f
        val localeID = Locale("in", "ID")

        // Judul Laporan
        canvas.drawText("Laporan Keuangan BUMDes", 40f, yPosition, titlePaint)
        yPosition += 25f
        canvas.drawText(reportData.unitUsahaName, 40f, yPosition, subtitlePaint)
        yPosition += 15f
        val dateFormatPeriod = SimpleDateFormat("dd MMM yyyy", localeID)
        val periodText = "${dateFormatPeriod.format(Date(reportData.startDate))} - ${dateFormatPeriod.format(Date(reportData.endDate))}"
        canvas.drawText(periodText, 40f, yPosition, textPaint)
        yPosition += 40f

        // Ringkasan
        val currencyFormat = NumberFormat.getCurrencyInstance(localeID).apply { maximumFractionDigits = 0 }
        canvas.drawText("Total Pendapatan: ${currencyFormat.format(reportData.totalIncome)}", 40f, yPosition, textPaint)
        yPosition += 25f
        canvas.drawText("Total Beban: ${currencyFormat.format(reportData.totalExpenses)}", 40f, yPosition, textPaint)
        yPosition += 25f
        canvas.drawText("Laba Bersih: ${currencyFormat.format(reportData.netProfit)}", 40f, yPosition, headerPaint)
        yPosition += 50f

        // Header Tabel
        canvas.drawText("Tgl", 40f, yPosition, headerPaint)
        canvas.drawText("Deskripsi", 120f, yPosition, headerPaint)
        canvas.drawText("Pemasukan", 450f, yPosition, headerPaint)
        canvas.drawText("Pengeluaran", 600f, yPosition, headerPaint)
        yPosition += 10f

        canvas.drawLine(40f, yPosition, pagewidth - 40f, yPosition, headerPaint)
        yPosition += 20f

        // ✅ --- LOGIKA TABEL YANG DIPERBAIKI ---
        val dateFormatTable = SimpleDateFormat("dd-MM-yy", localeID)

        // Dapatkan daftar ID untuk akun pendapatan dan beban
        val pendapatanAccountIds = allAccounts.filter { it.category == AccountCategory.PENDAPATAN }.map { it.id }
        val bebanAccountIds = allAccounts.filter { it.category == AccountCategory.BEBAN }.map { it.id }

        for (tx in transactions) {
            canvas.drawText(dateFormatTable.format(Date(tx.date)), 40f, yPosition, textPaint)
            canvas.drawText(tx.description, 120f, yPosition, textPaint)

            // Cek apakah ini transaksi pendapatan atau beban
            if (tx.creditAccountId in pendapatanAccountIds) {
                // Jika kreditnya adalah akun pendapatan, tampilkan di kolom Pemasukan
                canvas.drawText(currencyFormat.format(tx.amount), 450f, yPosition, textPaint)
            } else if (tx.debitAccountId in bebanAccountIds) {
                // Jika debitnya adalah akun beban, tampilkan di kolom Pengeluaran
                canvas.drawText(currencyFormat.format(tx.amount), 600f, yPosition, textPaint)
            }
            yPosition += 25f
        }
        // --- AKHIR LOGIKA TABEL ---

        pdfDocument.finishPage(page)
        savePdfToFile(context, pdfDocument)
    }

    private fun savePdfToFile(context: Context, pdfDocument: PdfDocument) {
        val simpleDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val fileName = "Laporan_BumdesKu_${simpleDateFormat.format(Date())}.pdf"

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    resolver.openOutputStream(it).use { outputStream ->
                        pdfDocument.writeTo(outputStream)
                        Toast.makeText(context, "PDF berhasil disimpan di folder Downloads", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
                FileOutputStream(file).use {
                    pdfDocument.writeTo(it)
                    Toast.makeText(context, "PDF berhasil disimpan di folder Downloads", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal menyimpan PDF: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            pdfDocument.close()
        }
    }
}