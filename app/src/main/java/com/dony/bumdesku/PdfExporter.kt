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
        transactions: List<Transaction>
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

        // Judul
        canvas.drawText("Laporan Keuangan BUMDes", 40f, yPosition, titlePaint)
        yPosition += 40f

        // Ringkasan
        val localeID = Locale("in", "ID")
        val currencyFormat = NumberFormat.getCurrencyInstance(localeID).apply { maximumFractionDigits = 0 }
        canvas.drawText("Total Pemasukan: ${currencyFormat.format(reportData.totalIncome)}", 40f, yPosition, textPaint)
        yPosition += 25f
        canvas.drawText("Total Pengeluaran: ${currencyFormat.format(reportData.totalExpenses)}", 40f, yPosition, textPaint)
        yPosition += 25f
        canvas.drawText("Laba Bersih: ${currencyFormat.format(reportData.netProfit)}", 40f, yPosition, headerPaint)
        yPosition += 50f

        // Header Tabel
        canvas.drawText("Tgl", 40f, yPosition, headerPaint)
        canvas.drawText("Deskripsi", 120f, yPosition, headerPaint)
        canvas.drawText("Pemasukan", 450f, yPosition, headerPaint)
        canvas.drawText("Pengeluaran", 600f, yPosition, headerPaint)
        yPosition += 25f

        // Garis pemisah
        canvas.drawLine(40f, yPosition - 20, pagewidth - 40f, yPosition - 20, headerPaint)
        canvas.drawLine(40f, yPosition, pagewidth - 40f, yPosition, headerPaint)
        yPosition += 10f


        // Isi Tabel (daftar transaksi)
        val dateFormat = SimpleDateFormat("dd-MM-yy", localeID)
        for (tx in transactions) {
            yPosition += 20f
            canvas.drawText(dateFormat.format(Date(tx.date)), 40f, yPosition, textPaint)
            canvas.drawText(tx.description, 120f, yPosition, textPaint)

            if (tx.type == "PEMASUKAN") {
                canvas.drawText(currencyFormat.format(tx.amount), 450f, yPosition, textPaint)
            } else {
                canvas.drawText(currencyFormat.format(tx.amount), 600f, yPosition, textPaint)
            }
        }

        // Selesaikan halaman PDF
        pdfDocument.finishPage(page)

        // Simpan file PDF
        savePdfToFile(context, pdfDocument)
    }

    private fun savePdfToFile(context: Context, pdfDocument: PdfDocument) {
        val simpleDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val fileName = "Laporan_BumdesKu_${simpleDateFormat.format(Date())}.pdf"

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Untuk Android 10 dan ke atas (Scoped Storage)
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
                // Untuk Android 9 dan ke bawah
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