package com.example.data.exporter

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {

    private const val PAGE_WIDTH = 595 // Standard A4 width in postscript points
    private const val PAGE_HEIGHT = 842 // Standard A4 height in postscript points
    private const val MARGIN_X = 45f
    private const val MARGIN_TOP = 50f
    private const val MARGIN_BOTTOM = 50f
    private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN_X * 2)

    fun createPdf(
        title: String,
        subtitle: String,
        content: String,
        outputStream: OutputStream
    ) {
        val document = PdfDocument()

        val textPaint = TextPaint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 12f
            isAntiAlias = true
        }

        val headingPaint = TextPaint().apply {
            color = Color.rgb(30, 58, 138) // Deep Blue
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val titlePaint = TextPaint().apply {
            color = Color.rgb(30, 58, 138)
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val subPaint = TextPaint().apply {
            color = Color.rgb(100, 116, 139)
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            strokeWidth = 1f
        }

        val lines = content.split("\n")
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        fun drawHeaderAndFooter(c: Canvas, pNum: Int) {
            // Header
            c.drawText("StudyMate AI  |  Study Notes", MARGIN_X, 35f, subPaint)
            val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
            val dateWidth = subPaint.measureText(dateStr)
            c.drawText(dateStr, PAGE_WIDTH - MARGIN_X - dateWidth, 35f, subPaint)
            c.drawLine(MARGIN_X, 42f, PAGE_WIDTH - MARGIN_X, 42f, linePaint)

            // Footer
            val footerY = PAGE_HEIGHT - 25f
            c.drawLine(MARGIN_X, footerY - 10f, PAGE_WIDTH - MARGIN_X, footerY - 10f, linePaint)
            val footerText = "Generated with StudyMate AI  •  Page $pNum"
            val footerWidth = subPaint.measureText(footerText)
            c.drawText(footerText, (PAGE_WIDTH - footerWidth) / 2f, footerY, subPaint)
        }

        drawHeaderAndFooter(canvas, pageNumber)

        var currentY = MARGIN_TOP + 15f

        // Draw Document Title on first page
        val titleLayout = StaticLayout.Builder.obtain(title, 0, title.length, titlePaint, CONTENT_WIDTH.toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(2f, 1f)
            .setIncludePad(false)
            .build()
        titleLayout.draw(canvas)
        currentY += titleLayout.height + 6f

        if (subtitle.isNotBlank()) {
            val subLayout = StaticLayout.Builder.obtain(subtitle, 0, subtitle.length, subPaint, CONTENT_WIDTH.toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(2f, 1f)
                .setIncludePad(false)
                .build()
            canvas.save()
            canvas.translate(0f, currentY)
            subLayout.draw(canvas)
            canvas.restore()
            currentY += subLayout.height + 15f
        } else {
            currentY += 10f
        }

        // Draw divider under title
        canvas.drawLine(MARGIN_X, currentY - 5f, PAGE_WIDTH - MARGIN_X, currentY - 5f, linePaint)
        currentY += 10f

        val maxY = PAGE_HEIGHT - MARGIN_BOTTOM - 20f

        for (line in lines) {
            val trimmed = line.trim()
            val isHeading = trimmed.startsWith("#") || (trimmed.endsWith(":") && trimmed.length < 50)
            val isBullet = trimmed.startsWith("•") || trimmed.startsWith("-") || trimmed.startsWith("*")

            val cleanText = when {
                trimmed.startsWith("###") -> trimmed.removePrefix("###").trim()
                trimmed.startsWith("##") -> trimmed.removePrefix("##").trim()
                trimmed.startsWith("#") -> trimmed.removePrefix("#").trim()
                else -> trimmed
            }

            val currentPaint = if (isHeading) headingPaint else textPaint

            val displayLine = if (cleanText.isEmpty()) " " else cleanText
            val layout = StaticLayout.Builder.obtain(displayLine, 0, displayLine.length, currentPaint, CONTENT_WIDTH.toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(3f, 1f)
                .setIncludePad(false)
                .build()

            val neededHeight = layout.height + (if (isHeading) 12f else 6f)

            if (currentY + neededHeight > maxY) {
                // Finish current page and create new page
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                drawHeaderAndFooter(canvas, pageNumber)
                currentY = MARGIN_TOP + 10f
            }

            if (isHeading) {
                currentY += 8f
            }

            canvas.save()
            canvas.translate(MARGIN_X + (if (isBullet && !isHeading) 10f else 0f), currentY)
            layout.draw(canvas)
            canvas.restore()

            currentY += layout.height + (if (isHeading) 8f else 4f)
        }

        document.finishPage(page)
        document.writeTo(outputStream)
        document.close()
    }

    fun saveToInternalFile(
        context: Context,
        fileName: String,
        title: String,
        subtitle: String,
        content: String
    ): File {
        val dir = File(context.filesDir, "PDFs").apply { mkdirs() }
        val sanitized = fileName.replace("[^a-zA-Z0-9_.-]".toRegex(), "_")
        val finalName = if (sanitized.endsWith(".pdf", ignoreCase = true)) sanitized else "$sanitized.pdf"
        val file = File(dir, finalName)
        FileOutputStream(file).use { out ->
            createPdf(title, subtitle, content, out)
        }
        return file
    }
}
