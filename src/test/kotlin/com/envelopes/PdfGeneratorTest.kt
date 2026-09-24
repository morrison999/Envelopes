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
}
