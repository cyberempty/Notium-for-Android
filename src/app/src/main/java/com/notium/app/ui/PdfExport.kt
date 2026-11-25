package com.notium.app.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.io.OutputStream

/** Crea un PDF (A4) dal testo della nota, interamente sul telefono. */
object PdfExport {
    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 48

    private class Item(val layout: StaticLayout, val x: Int, val gapBefore: Int, val rule: Boolean = false)

    private fun paint(size: Float, bold: Boolean = false, italic: Boolean = false, mono: Boolean = false, gray: Boolean = false): TextPaint =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            color = if (gray) Color.rgb(90, 90, 96) else Color.BLACK
            typeface = Typeface.create(
                if (mono) Typeface.MONOSPACE else Typeface.SANS_SERIF,
                when { bold && italic -> Typeface.BOLD_ITALIC; bold -> Typeface.BOLD; italic -> Typeface.ITALIC; else -> Typeface.NORMAL }
            )
        }

    private fun layout(text: CharSequence, p: TextPaint, width: Int): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, p, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL).setLineSpacing(0f, 1.18f).setIncludePad(false).build()

    private fun plain(s: String): String =
        s.replace(Regex("\\*\\*(.+?)\\*\\*"), "$1").replace(Regex("~~(.+?)~~"), "$1").replace(Regex("==(.+?)=="), "$1")
            .replace(Regex("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)"), "$1")
            .replace(Regex("`([^`]+)`"), "$1").replace(Regex("\\[([^\\]]*)\\]\\([^)]*\\)"), "$1")

    fun write(out: OutputStream, title: String, body: String) {
        val width = PAGE_W - 2 * MARGIN
        val items = ArrayList<Item>()
        if (title.isNotBlank()) items.add(Item(layout(title.trim(), paint(22f, bold = true), width), 0, 0))
        var inCode = false
        for (raw in body.split("\n")) {
            val line = raw.trimEnd()
            val t = line.trimStart()
            val indent = (line.length - t.length) / 2 * 14
            when {
                t.startsWith("```") -> inCode = !inCode
                inCode -> items.add(Item(layout(line.ifEmpty { " " }, paint(10.5f, mono = true), width), 0, 0))
                line.isBlank() -> items.add(Item(layout(" ", paint(6f), width), 0, 0))
                t.startsWith("### ") -> items.add(Item(layout(plain(t.drop(4)), paint(14f, bold = true), width), 0, 8))
                t.startsWith("## ") -> items.add(Item(layout(plain(t.drop(3)), paint(16f, bold = true), width), 0, 10))
                t.startsWith("# ") -> items.add(Item(layout(plain(t.drop(2)), paint(19f, bold = true), width), 0, 12))
                Regex("^- \\[[xX]\\] ").containsMatchIn(t) -> items.add(Item(layout("[x]  " + plain(t.drop(6)), paint(12f, gray = true), width - indent), indent, 2))
                Regex("^- \\[ \\] ").containsMatchIn(t) -> items.add(Item(layout("[  ]  " + plain(t.drop(6)), paint(12f), width - indent), indent, 2))
                Regex("^[-*+] ").containsMatchIn(t) -> items.add(Item(layout("•  " + plain(t.drop(2)), paint(12f), width - indent), indent, 1))
                Regex("^\\d+\\. ").containsMatchIn(t) -> items.add(Item(layout(plain(t), paint(12f), width - indent), indent, 1))
                t.startsWith(">") -> items.add(Item(layout(plain(t.removePrefix(">").trim()), paint(12f, italic = true, gray = true), width - 18), 18, 2))
                t == "---" -> items.add(Item(layout(" ", paint(4f), width), 0, 4, rule = true))
                t.startsWith("|") -> {
                    if (!Regex("^\\|?\\s*:?-{2,}").containsMatchIn(t)) items.add(Item(layout(t.trim('|').split("|").joinToString("   |   ") { plain(it.trim()) }, paint(11.5f), width), 0, 1))
                }
                else -> items.add(Item(layout(plain(line), paint(12f), width - indent), indent, 3))
            }
        }

        val doc = PdfDocument()
        var pageNo = 0
        var page: PdfDocument.Page? = null
        var canvas: Canvas? = null
        var y = MARGIN
        fun newPage() {
            page?.let { doc.finishPage(it) }
            pageNo++
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create())
            canvas = page!!.canvas
            y = MARGIN
        }
        newPage()
        for (item in items) {
            if (y > MARGIN) y += item.gapBefore
            var offset = 0
            val h = item.layout.height
            while (offset < h) {
                val avail = PAGE_H - MARGIN - y
                if (avail < 36) { newPage(); continue }
                val chunk = minOf(h - offset, avail)
                val c = canvas!!
                c.save()
                c.translate((MARGIN + item.x).toFloat(), y.toFloat())
                c.clipRect(0f, offset.toFloat(), (width - item.x).toFloat() + 2f, (offset + chunk).toFloat())
                c.translate(0f, -offset.toFloat())
                if (item.rule) {
                    val lp = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }
                    c.drawLine(0f, h / 2f, width.toFloat(), h / 2f, lp)
                } else item.layout.draw(c)
                c.restore()
                offset += chunk
                y += chunk
                if (offset < h) newPage()
            }
        }
        page?.let { doc.finishPage(it) }
        doc.writeTo(out)
        doc.close()
    }
}
