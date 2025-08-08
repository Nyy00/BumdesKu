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

    private const val PAGE_WIDTH = 792
    private const val PAGE_HEIGHT = 1120
    private const val MARGIN = 40f
    private var yPosition = 0f
    private lateinit var canvas: Canvas
    private lateinit var pdfDocument: PdfDocument
    private var currentPage: PdfDocument.Page? = null
    private var pageNumber = 1

    fun createReportPdf(
        context: Context,
        reportData: ReportData,
        transactions: List<Transaction>,
        allAccounts: List<Account>
    ) {
        if (!reportData.isGenerated) {
            Toast.makeText(context, "Silakan buat laporan terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        // Inisialisasi dokumen dan halaman pertama
        pdfDocument = PdfDocument()
        startNewPage()

        val titlePaint = Paint().apply { color = Color.BLACK; textSize = 20f; isFakeBoldText = true }
        val subtitlePaint = Paint().apply { color = Color.DKGRAY; textSize = 16f }
        val headerPaint = Paint().apply { color = Color.BLACK; textSize = 15f; isFakeBoldText = true }
        val textPaint = Paint().apply { color = Color.DKGRAY; textSize = 14f }
        val localeID = Locale("in", "ID")

        // --- Menggambar Header Laporan ---
        yPosition = MARGIN + 20f
        drawText("Laporan Keuangan BUMDes", MARGIN, yPosition, titlePaint)
        yPosition += 25f
        drawText(reportData.unitUsahaName, MARGIN, yPosition, subtitlePaint)
        yPosition += 15f
        val dateFormatPeriod = SimpleDateFormat("dd MMM yyyy", localeID)
        val periodText = "${dateFormatPeriod.format(Date(reportData.startDate))} - ${dateFormatPeriod.format(Date(reportData.endDate))}"
        drawText(periodText, MARGIN, yPosition, textPaint)
        yPosition += 40f

        // --- Menggambar Ringkasan Keuangan ---
        val currencyFormat = NumberFormat.getCurrencyInstance(localeID).apply { maximumFractionDigits = 0 }
        drawText("Total Pendapatan: ${currencyFormat.format(reportData.totalIncome)}", MARGIN, yPosition, textPaint)
        yPosition += 25f
        drawText("Total Beban: ${currencyFormat.format(reportData.totalExpenses)}", MARGIN, yPosition, textPaint)
        yPosition += 25f
        drawText("Laba Bersih: ${currencyFormat.format(reportData.netProfit)}", MARGIN, yPosition, headerPaint)
        yPosition += 50f

        // --- Menggambar Header Tabel Transaksi ---
        checkPageOverflow(50f) // Cek apakah header tabel muat
        drawText("Tgl", MARGIN, yPosition, headerPaint)
        drawText("Deskripsi", 120f, yPosition, headerPaint)
        drawText("Pemasukan", 450f, yPosition, headerPaint)
        drawText("Pengeluaran", 600f, yPosition, headerPaint)
        yPosition += 10f
        canvas.drawLine(MARGIN, yPosition, PAGE_WIDTH - MARGIN, yPosition, headerPaint)
        yPosition += 20f

        // --- Menggambar Data Transaksi (Looping) ---
        val dateFormatTable = SimpleDateFormat("dd-MM-yy", localeID)
        val pendapatanAccountIds = allAccounts.filter { it.category == AccountCategory.PENDAPATAN }.map { it.id }
        val bebanAccountIds = allAccounts.filter { it.category == AccountCategory.BEBAN }.map { it.id }

        for (tx in transactions) {
            val isIncome = tx.creditAccountId in pendapatanAccountIds
            val isExpense = tx.debitAccountId in bebanAccountIds

            if (isIncome || isExpense) {

                checkPageOverflow(25f) // Perkirakan tinggi satu baris adalah 25f

                drawText(dateFormatTable.format(Date(tx.date)), MARGIN, yPosition, textPaint)
                drawText(tx.description, 120f, yPosition, textPaint)

                if (isIncome) {
                    drawText(currencyFormat.format(tx.amount), 450f, yPosition, textPaint)
                }
                if (isExpense) {
                    drawText(currencyFormat.format(tx.amount), 600f, yPosition, textPaint)
                }
                yPosition += 25f
            }
        }

        // Selesaikan halaman terakhir dan simpan file
        pdfDocument.finishPage(currentPage)
        savePdfToFile(context, pdfDocument)
    }

    private fun startNewPage() {
        if (currentPage != null) {
            pdfDocument.finishPage(currentPage)
        }
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        currentPage = pdfDocument.startPage(pageInfo)
        canvas = currentPage!!.canvas
        yPosition = MARGIN
        pageNumber++
    }

    private fun checkPageOverflow(neededSpace: Float) {
        if (yPosition + neededSpace > PAGE_HEIGHT - MARGIN) {
            startNewPage()
        }
    }

    // Fungsi drawText sederhana untuk menghindari repetisi
    private fun drawText(text: String, x: Float, y: Float, paint: Paint) {
        canvas.drawText(text, x, y, paint)
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