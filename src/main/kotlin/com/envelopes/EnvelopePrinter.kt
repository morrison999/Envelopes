package com.envelopes

import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.print.PageFormat
import java.awt.print.Paper
import java.awt.print.Printable
import java.awt.print.PrinterJob

object EnvelopePrinter {

    fun printEnvelope(
        envelopeConfig: EnvelopeConfig,
        recipient: Address? = null,
        returnAddr: Address = EnvelopeConstants.DEFAULT_RETURN_ADDRESS,
        rotate90: Boolean = true
    ): Boolean {
        val job = PrinterJob.getPrinterJob()
        job.setJobName("Envelope - ${envelopeConfig.name}")

        val pageFormat = job.defaultPage()
        val paper = Paper()
        val width = envelopeConfig.widthPt
        val height = envelopeConfig.heightPt

        if (rotate90) {
            // Paper orientation in printer feed tray is portrait (short edge first)
            val feedWidth = height
            val feedHeight = width
            paper.setSize(feedWidth, feedHeight)
            paper.setImageableArea(0.0, 0.0, feedWidth, feedHeight)
            pageFormat.paper = paper
            pageFormat.orientation = PageFormat.PORTRAIT
        } else {
            paper.setSize(width, height)
            paper.setImageableArea(0.0, 0.0, width, height)
            pageFormat.paper = paper
            pageFormat.orientation = PageFormat.LANDSCAPE
        }

        job.setPrintable(Printable { graphics, _, pageIndex ->
            if (pageIndex > 0) return@Printable Printable.NO_SUCH_PAGE

            val g2d = graphics as Graphics2D
            g2d.color = Color.BLACK
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)

            if (rotate90) {
                // Rotate 90 degrees clockwise for printer feed slot
                g2d.translate(height, 0.0)
                g2d.rotate(Math.PI / 2.0)
            }

            val heightPt = envelopeConfig.heightPt

            // 1. Return Address (Top-Left corner)
            val returnLeft = 0.5 * EnvelopeConstants.INCH
            val returnTop = heightPt - 0.5 * EnvelopeConstants.INCH
            val lineSpacing = 13.0

            val retName = returnAddr.name
            val retStreet = returnAddr.street
            val retStreet2 = returnAddr.street2
            val retCityStateZip = listOf(
                returnAddr.city,
                "${returnAddr.state} ${returnAddr.zip}".trim()
            ).filter { it.isNotBlank() }.joinToString(", ")

            val fontBold = Font(Font.SANS_SERIF, Font.BOLD, 10)
            val fontRegular = Font(Font.SANS_SERIF, Font.PLAIN, 10)

            val retLines = mutableListOf<Pair<String, Font>>()
            if (retName.isNotBlank()) retLines.add(retName to fontBold)
            if (retStreet.isNotBlank()) retLines.add(retStreet to fontRegular)
            if (retStreet2.isNotBlank()) retLines.add(retStreet2 to fontRegular)
            if (retCityStateZip.isNotBlank()) retLines.add(retCityStateZip to fontRegular)

            retLines.forEachIndexed { idx, (lineText, font) ->
                val yPdf = returnTop - (idx * lineSpacing)
                val yJava2D = (heightPt - yPdf).toFloat()
                g2d.font = font
                g2d.drawString(lineText, returnLeft.toFloat(), yJava2D)
            }

            // 2. Recipient Address
            if (!envelopeConfig.isWindowed && recipient != null) {
                val recipientLeft = envelopeConfig.recipientLeft
                val recipientBaseline = envelopeConfig.recipientBottom
                val recipLineSpacing = 16.0

                val recipName = recipient.name
                val recipStreet = recipient.street
                val recipStreet2 = recipient.street2
                val recipCityStateZip = listOf(
                    recipient.city,
                    "${recipient.state} ${recipient.zip}".trim()
                ).filter { it.isNotBlank() }.joinToString(", ")

                val recipFontBold = Font(Font.SANS_SERIF, Font.BOLD, 12)
                val recipFontRegular = Font(Font.SANS_SERIF, Font.PLAIN, 12)

                val recipLines = mutableListOf<Pair<String, Font>>()
                if (recipName.isNotBlank()) recipLines.add(recipName to recipFontBold)
                if (recipStreet.isNotBlank()) recipLines.add(recipStreet to recipFontRegular)
                if (recipStreet2.isNotBlank()) recipLines.add(recipStreet2 to recipFontRegular)
                if (recipCityStateZip.isNotBlank()) recipLines.add(recipCityStateZip to recipFontRegular)

                val numLines = recipLines.size
                recipLines.forEachIndexed { idx, (lineText, font) ->
                    val yPdf = recipientBaseline + ((numLines - 1 - idx) * recipLineSpacing)
                    val yJava2D = (heightPt - yPdf).toFloat()
                    g2d.font = font
                    g2d.drawString(lineText, recipientLeft.toFloat(), yJava2D)
                }
            }

            Printable.PAGE_EXISTS
        }, pageFormat)

        return if (job.printDialog()) {
            job.print()
            true
        } else {
            false
        }
    }
}
