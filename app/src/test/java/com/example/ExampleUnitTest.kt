package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun docx_export_createsValidZip() {
    val byteArrayOutputStream = java.io.ByteArrayOutputStream()
    com.example.data.exporter.DocxExporter.createDocx(
        title = "Physics Chapter 1",
        subtitle = "Class 10 Science",
        content = "# Kinematics\n• v = u + at\n• s = ut + 0.5at²",
        outputStream = byteArrayOutputStream
    )
    val bytes = byteArrayOutputStream.toByteArray()
    assertTrue("DOCX byte array should not be empty", bytes.isNotEmpty())
    // Check ZIP magic header (PK\x03\x04)
    assertEquals(0x50.toByte(), bytes[0])
    assertEquals(0x4B.toByte(), bytes[1])
  }
}
