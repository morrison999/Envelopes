package com.envelopes

import java.io.File
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PdfGeneratorTest {

    @Test
    fun testAddressFormatSingleLine() {
        val addr = Address(
            name = "David Morrison",
            street = "1900 Grace Ave",
            street2 = "#242",
            city = "Harlingen",
            state = "TX",
            zip = "78550"
        )
        assertEquals("David Morrison, 1900 Grace Ave, #242, Harlingen, TX 78550", addr.formatSingleLine())
    }

    @Test
    fun testPdfEscape() {
        assertEquals("Hello \\(World\\)", PdfGenerator.pdfEscape("Hello (World)"))
        assertEquals("Backslash \\\\ test", PdfGenerator.pdfEscape("Backslash \\ test"))
        assertEquals("", PdfGenerator.pdfEscape(null))
    }

    @Test
    fun testGeneratePdfStandardEnvelopeWithRotation() {
        val tempFile = File.createTempFile("test_env10", ".pdf")
        tempFile.deleteOnExit()

        val recipient = Address(
            name = "Jane Doe",
            street = "123 Main St",
            street2 = "Apt 4B",
            city = "Austin",
            state = "TX",
            zip = "78701"
        )

        val env10 = EnvelopeConstants.ENVELOPES.getValue("1")
        val generated = PdfGenerator.generateEnvelopePdf(
            envelopeConfig = env10,
            recipient = recipient,
            returnAddr = EnvelopeConstants.DEFAULT_RETURN_ADDRESS,
            outputFile = tempFile,
            rotate90 = true
        )

        assertTrue(generated.exists())
        assertTrue(generated.length() > 0)

        val content = generated.readBytes().toString(StandardCharsets.ISO_8859_1)
        assertTrue(content.startsWith("%PDF-1.4"))
        assertTrue(content.contains("/Rotate 90"))
        assertTrue(content.contains("Jane Doe"))
        assertTrue(content.contains("123 Main St"))
        assertTrue(content.contains("Austin, TX 78701"))
        assertTrue(content.contains("David Morrison"))
        assertTrue(content.contains("%%EOF"))
    }

    @Test
    fun testGeneratePdfWithoutRotation() {
        val tempFile = File.createTempFile("test_env_norotate", ".pdf")
        tempFile.deleteOnExit()

        val env10 = EnvelopeConstants.ENVELOPES.getValue("1")
        val generated = PdfGenerator.generateEnvelopePdf(
            envelopeConfig = env10,
            recipient = Address(name = "Jane Doe"),
            returnAddr = EnvelopeConstants.DEFAULT_RETURN_ADDRESS,
            outputFile = tempFile,
            rotate90 = false
        )

        assertTrue(generated.exists())
        val content = generated.readBytes().toString(StandardCharsets.ISO_8859_1)
        assertFalse(content.contains("/Rotate 90"))
    }

    @Test
    fun testGeneratePdfWindowedEnvelope() {
        val tempFile = File.createTempFile("test_env_windowed", ".pdf")
        tempFile.deleteOnExit()

        val envWindowed = EnvelopeConstants.ENVELOPES.getValue("2")
        val generated = PdfGenerator.generateEnvelopePdf(
            envelopeConfig = envWindowed,
            recipient = null,
            returnAddr = EnvelopeConstants.DEFAULT_RETURN_ADDRESS,
            outputFile = tempFile
        )

        assertTrue(generated.exists())
        assertTrue(generated.length() > 0)

        val content = generated.readBytes().toString(StandardCharsets.ISO_8859_1)
        assertTrue(content.startsWith("%PDF-1.4"))
        assertTrue(content.contains("David Morrison"))
        assertTrue(content.contains("%%EOF"))
    }

    @Test
    fun testAccentedCharactersUseWinAnsiEncoding() {
        val tempFile = File.createTempFile("test_env_accents", ".pdf")
        tempFile.deleteOnExit()

        PdfGenerator.generateEnvelopePdf(
            envelopeConfig = EnvelopeConstants.ENVELOPES.getValue("1"),
            recipient = Address(name = "José Peña", street = "12 Calle Niño", city = "Harlingen", state = "TX", zip = "78550"),
            outputFile = tempFile
        )

        val bytes = tempFile.readBytes()
        val content = bytes.toString(charset("windows-1252"))
        assertTrue(content.contains("/Encoding /WinAnsiEncoding"))
        assertTrue(content.contains("(José Peña) Tj"))
        assertFalse(bytes.toString(StandardCharsets.ISO_8859_1).contains("JosÃ©"))

        // The stream /Length must match the encoded byte count.
        val length = Regex("/Length (\\d+)").find(content)!!.groupValues[1].toInt()
        val start = content.indexOf("stream\n") + "stream\n".length
        val end = content.indexOf("\nendstream")
        assertEquals(length, end - start)

        // Every xref offset must point at the start of its object.
        val xref = content.substring(content.indexOf("xref\n"))
        Regex("(\\d{10}) 00000 n").findAll(xref).forEachIndexed { idx, m ->
            val offset = m.groupValues[1].toInt()
            assertTrue(content.startsWith("${idx + 1} 0 obj", offset))
        }
    }

    @Test
    fun testDefaultOutputDirIsUnderUserHome() {
        val home = File(System.getProperty("user.home")).absoluteFile
        assertTrue(PdfGenerator.defaultOutputDir().absoluteFile.path.startsWith(home.path))
    }
}
