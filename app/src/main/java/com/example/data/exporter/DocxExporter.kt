package com.example.data.exporter

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object DocxExporter {

    private fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    fun createDocx(
        title: String,
        subtitle: String,
        content: String,
        outputStream: OutputStream
    ) {
        val zipOut = ZipOutputStream(outputStream)

        // 1. [Content_Types].xml
        val contentTypesXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
              <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
              <Default Extension="xml" ContentType="application/xml"/>
              <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
            </Types>
        """.trimIndent()
        zipOut.putNextEntry(ZipEntry("[Content_Types].xml"))
        zipOut.write(contentTypesXml.toByteArray(StandardCharsets.UTF_8))
        zipOut.closeEntry()

        // 2. _rels/.rels
        val relsXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
              <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
            </Relationships>
        """.trimIndent()
        zipOut.putNextEntry(ZipEntry("_rels/.rels"))
        zipOut.write(relsXml.toByteArray(StandardCharsets.UTF_8))
        zipOut.closeEntry()

        // 3. word/document.xml
        val bodyBuilder = StringBuilder()

        // Document Title
        bodyBuilder.append("""
            <w:p>
              <w:pPr>
                <w:jc w:val="center"/>
                <w:spacing w:before="240" w:after="120"/>
              </w:pPr>
              <w:r>
                <w:rPr>
                  <w:rFonts w:ascii="Calibri" w:hAnsi="Calibri"/>
                  <w:b/>
                  <w:color w:val="1E3A8A"/>
                  <w:sz w:val="48"/>
                </w:rPr>
                <w:t>${escapeXml(title)}</w:t>
              </w:r>
            </w:p>
        """.trimIndent())

        if (subtitle.isNotBlank()) {
            bodyBuilder.append("""
                <w:p>
                  <w:pPr>
                    <w:jc w:val="center"/>
                    <w:spacing w:after="280"/>
                  </w:pPr>
                  <w:r>
                    <w:rPr>
                      <w:rFonts w:ascii="Calibri" w:hAnsi="Calibri"/>
                      <w:i/>
                      <w:color w:val="64748B"/>
                      <w:sz w:val="22"/>
                    </w:rPr>
                    <w:t>${escapeXml(subtitle)}</w:t>
                  </w:r>
                </w:p>
            """.trimIndent())
        }

        val lines = content.split("\n")
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                bodyBuilder.append("<w:p><w:pPr><w:spacing w:after=\"120\"/></w:pPr></w:p>")
                continue
            }

            val isH1 = trimmed.startsWith("# ")
            val isH2 = trimmed.startsWith("## ")
            val isH3 = trimmed.startsWith("### ")
            val isBullet = trimmed.startsWith("•") || trimmed.startsWith("- ") || trimmed.startsWith("* ")

            val cleanText = when {
                isH3 -> trimmed.removePrefix("###").trim()
                isH2 -> trimmed.removePrefix("##").trim()
                isH1 -> trimmed.removePrefix("#").trim()
                isBullet -> trimmed.removePrefix("•").removePrefix("-").removePrefix("*").trim()
                else -> trimmed
            }

            val szVal = when {
                isH1 -> "34"
                isH2 -> "28"
                isH3 -> "26"
                else -> "24"
            }

            val colorVal = when {
                isH1 || isH2 -> "1E3A8A"
                isH3 -> "0F766E"
                else -> "1E293B"
            }

            val isBold = isH1 || isH2 || isH3 || trimmed.startsWith("**")

            bodyBuilder.append("<w:p><w:pPr>")
            if (isBullet) {
                bodyBuilder.append("<w:ind w:left=\"400\"/>")
            }
            bodyBuilder.append("<w:spacing w:after=\"${if (isH1 || isH2) "160" else "100"}\"/>")
            bodyBuilder.append("</w:pPr>")

            if (isBullet) {
                bodyBuilder.append("<w:r><w:rPr><w:b/></w:rPr><w:t>• </w:t></w:r>")
            }

            bodyBuilder.append("""
                <w:r>
                  <w:rPr>
                    <w:rFonts w:ascii="Calibri" w:hAnsi="Calibri"/>
                    ${if (isBold) "<w:b/>" else ""}
                    <w:color w:val="$colorVal"/>
                    <w:sz w:val="$szVal"/>
                  </w:rPr>
                  <w:t xml:space="preserve">${escapeXml(cleanText)}</w:t>
                </w:r>
            """.trimIndent())
            bodyBuilder.append("</w:p>")
        }

        val documentXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
              <w:body>
                $bodyBuilder
              </w:body>
            </w:document>
        """.trimIndent()

        zipOut.putNextEntry(ZipEntry("word/document.xml"))
        zipOut.write(documentXml.toByteArray(StandardCharsets.UTF_8))
        zipOut.closeEntry()

        zipOut.finish()
    }

    fun saveToInternalFile(
        context: Context,
        fileName: String,
        title: String,
        subtitle: String,
        content: String
    ): File {
        val dir = File(context.filesDir, "Documents").apply { mkdirs() }
        val sanitized = fileName.replace("[^a-zA-Z0-9_.-]".toRegex(), "_")
        val finalName = if (sanitized.endsWith(".docx", ignoreCase = true)) sanitized else "$sanitized.docx"
        val file = File(dir, finalName)
        FileOutputStream(file).use { out ->
            createDocx(title, subtitle, content, out)
        }
        return file
    }
}
