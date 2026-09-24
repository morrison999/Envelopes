package com.envelopes

import java.io.File
import java.io.FileOutputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.Locale

object PdfGenerator {

    // Matches the fonts' /WinAnsiEncoding so accented characters (é, ñ, ü...) render correctly.
    // Characters outside windows-1252 are written as '?'.
    private val PDF_TEXT_CHARSET: Charset = Charset.forName("windows-1252")

    /** The user's Documents folder, falling back to their home folder, so saves never target the install dir. */
    fun defaultOutputDir(): File {
        val home = File(System.getProperty("user.home") ?: ".")
        val documents = File(home, "Documents")
        return if (documents.isDirectory) documents else home
    }

    fun pdfEscape(text: String?): String {
        if (text == null) return ""
        return text
            .replace("\\", "\\\\")
            .replace("(", "\\(")
            .replace(")", "\\)")
    }

    fun writePurePdf(
        file: File,
        widthPt: Double,
        heightPt: Double,
        streamCommands: List<String>,
        rotate90: Boolean = true
    ) {
        val streamContent = streamCommands.joinToString("\n")
        val streamBytes = streamContent.toByteArray(PDF_TEXT_CHARSET)
        val streamLen = streamBytes.size

        val rotateAttr = if (rotate90) " /Rotate 90" else ""

        val objects = listOf(
            // Obj 1: Catalog
            "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj",
            // Obj 2: Pages tree
            "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj",
            // Obj 3: Page object
            String.format(
                Locale.US,
                "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 %.2f %.2f]%s /Resources << /Font << /F1 4 0 R /F2 5 0 R /F3 6 0 R >> >> /Contents 7 0 R >>\nendobj",
                widthPt,
                heightPt,
                rotateAttr
            ),
            // Obj 4: Helvetica-Bold
            "4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>\nendobj",
            // Obj 5: Helvetica
            "5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>\nendobj",
            // Obj 6: Helvetica-Oblique
            "6 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Oblique /Encoding /WinAnsiEncoding >>\nendobj",
            // Obj 7: Contents Stream
            "7 0 obj\n<< /Length $streamLen >>\nstream\n$streamContent\nendstream\nendobj"
        )

        val header = "%PDF-1.4\n%\u00e2\u00e3\u00cf\u00d3\n"
        val headerBytes = header.toByteArray(StandardCharsets.ISO_8859_1)

        val bodyOffsets = mutableListOf<Long>()
        var currentOffset = headerBytes.size.toLong()

        val bodyByteList = mutableListOf<ByteArray>()
        for (obj in objects) {
            bodyOffsets.add(currentOffset)
            val objStr = obj + "\n"
            val objBytes = objStr.toByteArray(PDF_TEXT_CHARSET)
            bodyByteList.add(objBytes)
            currentOffset += objBytes.size
        }

        val xrefOffset = currentOffset
        val xrefBuilder = StringBuilder()
        xrefBuilder.append("xref\n")
        xrefBuilder.append("0 ").append(objects.size + 1).append("\n")
        xrefBuilder.append("0000000000 65535 f \n")
        for (offset in bodyOffsets) {
            xrefBuilder.append(String.format(Locale.US, "%010d 00000 n \n", offset))
        }
        val xrefBytes = xrefBuilder.toString().toByteArray(StandardCharsets.UTF_8)

        val trailer = "trailer\n<< /Size ${objects.size + 1} /Root 1 0 R >>\nstartxref\n$xrefOffset\n%%EOF\n"
        val trailerBytes = trailer.toByteArray(StandardCharsets.UTF_8)

        file.parentFile?.mkdirs()
        FileOutputStream(file).use { fos ->
            fos.write(headerBytes)
            for (b in bodyByteList) {
                fos.write(b)
            }
            fos.write(xrefBytes)
            fos.write(trailerBytes)
        }
    }

    fun generateEnvelopePdf(
        envelopeConfig: EnvelopeConfig,
        recipient: Address? = null,
        returnAddr: Address = EnvelopeConstants.DEFAULT_RETURN_ADDRESS,
        outputFile: File = File(envelopeConfig.filename),
        rotate90: Boolean = true
    ): File {
        val widthPt = envelopeConfig.widthPt
        val heightPt = envelopeConfig.heightPt
        val commands = mutableListOf<String>()

        // ---------------------------------------------------------
        // 1. Return Address (Top-Left corner)
        // ---------------------------------------------------------
        val returnLeft = 0.5 * EnvelopeConstants.INCH
        val returnTop = heightPt - 0.5 * EnvelopeConstants.INCH
        val lineSpacing = 13.0

        val retName = pdfEscape(returnAddr.name)
        val retStreet = pdfEscape(returnAddr.street)
        val retStreet2 = pdfEscape(returnAddr.street2)
        val retCityStateZip = pdfEscape(
            listOf(
                returnAddr.city,
                "${returnAddr.state} ${returnAddr.zip}".trim()
            ).filter { it.isNotBlank() }.joinToString(", ")
        )

        val retLines = mutableListOf<Pair<String, String>>()
        if (retName.isNotBlank()) retLines.add(retName to "/F1 9.5 Tf")
        if (retStreet.isNotBlank()) retLines.add(retStreet to "/F2 9.5 Tf")
        if (retStreet2.isNotBlank()) retLines.add(retStreet2 to "/F2 9.5 Tf")
        if (retCityStateZip.isNotBlank()) retLines.add(retCityStateZip to "/F2 9.5 Tf")

        retLines.forEachIndexed { idx, (lineText, fontCmd) ->
            val yPos = returnTop - (idx * lineSpacing)
            commands.addAll(
                listOf(
                    "BT",
                    fontCmd,
                    String.format(Locale.US, "1 0 0 1 %.2f %.2f Tm", returnLeft, yPos),
                    "($lineText) Tj",
                    "ET"
                )
            )
        }

        // ---------------------------------------------------------
        // 2. Delivery / Recipient Address
        // ---------------------------------------------------------
        if (!envelopeConfig.isWindowed && recipient != null) {
            val recipientLeft = envelopeConfig.recipientLeft
            val recipientBaseline = envelopeConfig.recipientBottom
            val recipLineSpacing = 16.0

            val recipName = pdfEscape(recipient.name)
            val recipStreet = pdfEscape(recipient.street)
            val recipStreet2 = pdfEscape(recipient.street2)
            val recipCityStateZip = pdfEscape(
                listOf(
                    recipient.city,
                    "${recipient.state} ${recipient.zip}".trim()
                ).filter { it.isNotBlank() }.joinToString(", ")
            )

            val recipLines = mutableListOf<Pair<String, String>>()
            if (recipName.isNotBlank()) recipLines.add(recipName to "/F1 11.5 Tf")
            if (recipStreet.isNotBlank()) recipLines.add(recipStreet to "/F2 11.5 Tf")
            if (recipStreet2.isNotBlank()) recipLines.add(recipStreet2 to "/F2 11.5 Tf")
            if (recipCityStateZip.isNotBlank()) recipLines.add(recipCityStateZip to "/F2 11.5 Tf")

            val numLines = recipLines.size
            recipLines.forEachIndexed { idx, (lineText, fontCmd) ->
                val yPos = recipientBaseline + ((numLines - 1 - idx) * recipLineSpacing)
                commands.addAll(
                    listOf(
                        "BT",
                        fontCmd,
                        String.format(Locale.US, "1 0 0 1 %.2f %.2f Tm", recipientLeft, yPos),
                        "($lineText) Tj",
                        "ET"
                    )
                )
            }
        }

        writePurePdf(outputFile, widthPt, heightPt, commands, rotate90)
        return outputFile
    }
}
