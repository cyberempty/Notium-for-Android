package com.notium.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.runtime.remember
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ---------------------------------------------------------------- strumenti dell'editor

private val RE_CHECK = Regex("^(\\s*)- \\[[ xX]\\] ")
private val RE_LISTHEAD = Regex("^([-*+]|\\d+\\.)\\s+")
private val RE_CONTINUE = Regex("^(\\s*)(- \\[[ xX]\\] |[-*+] |(\\d+)\\. )")
private val RE_HEADING_PREFIX = Regex("^#{1,6}\\s*")
private val RE_NUM = Regex("^\\d+\\.\\s")

fun TextFieldValue.wrapSelection(marker: String): TextFieldValue {
    val s = selection.min
    val e = selection.max
    return if (s == e) {
        TextFieldValue(text.substring(0, s) + marker + marker + text.substring(e), TextRange(s + marker.length))
    } else if (text.substring(s, e).let { it.startsWith(marker) && it.endsWith(marker) && it.length >= marker.length * 2 }) {
        // Gia' formattato: tolgo il marcatore (seconda pressione = annulla).
        val inner = text.substring(s + marker.length, e - marker.length)
        TextFieldValue(text.substring(0, s) + inner + text.substring(e), TextRange(s, s + inner.length))
    } else {
        TextFieldValue(
            text.substring(0, s) + marker + text.substring(s, e) + marker + text.substring(e),
            TextRange(s + marker.length, e + marker.length)
        )
    }
}

private fun TextFieldValue.mapSelectedLines(transform: (index: Int, line: String) -> String): TextFieldValue {
    val s = selection.min
    val e = selection.max
    val lineStart = if (s == 0) 0 else text.lastIndexOf('\n', s - 1) + 1
    val lineEnd = text.indexOf('\n', e).let { if (it < 0) text.length else it }
    val out = text.substring(lineStart, lineEnd).split("\n").mapIndexed(transform).joinToString("\n")
    return TextFieldValue(
        text.substring(0, lineStart) + out + text.substring(lineEnd),
        TextRange(lineStart, lineStart + out.length)
    )
}

fun TextFieldValue.heading(level: Int): TextFieldValue = mapSelectedLines { _, l ->
    "#".repeat(level) + " " + l.replace(RE_HEADING_PREFIX, "")
}

/** Testo normale: toglie titoli, elenchi e citazioni dalle righe selezionate. */
fun TextFieldValue.paragraph(): TextFieldValue = mapSelectedLines { _, l ->
    l.replace(RE_HEADING_PREFIX, "").replace(RE_CHECK, "$1").replace(RE_LISTHEAD, "").removePrefix("> ")
}

fun TextFieldValue.bulletList(): TextFieldValue = mapSelectedLines { _, l ->
    if (l.startsWith("- ") && !RE_CHECK.containsMatchIn(l)) l.removePrefix("- ") else "- " + l.replace(RE_CHECK, "$1")
}

fun TextFieldValue.numberedList(): TextFieldValue = mapSelectedLines { i, l ->
    if (RE_NUM.containsMatchIn(l)) l.replace(RE_NUM, "") else "${i + 1}. $l"
}

fun TextFieldValue.checklist(): TextFieldValue = mapSelectedLines { _, l ->
    if (RE_CHECK.containsMatchIn(l)) l.replace(RE_CHECK, "$1")
    else {
        val indent = l.takeWhile { it == ' ' }
        indent + "- [ ] " + l.trimStart().replace(RE_LISTHEAD, "")
    }
}

fun TextFieldValue.quote(): TextFieldValue = mapSelectedLines { _, l ->
    if (l.startsWith("> ")) l.removePrefix("> ") else "> $l"
}

fun TextFieldValue.indent(delta: Int): TextFieldValue = mapSelectedLines { _, l ->
    if (delta > 0) "  $l" else when {
        l.startsWith("  ") -> l.substring(2)
        l.startsWith(" ") || l.startsWith("\t") -> l.substring(1)
        else -> l
    }
}

/** Spunta o toglie la spunta alla riga della lista di controllo in cui si trova il cursore. */
fun TextFieldValue.toggleCheckHere(): TextFieldValue {
    val c = selection.min
    val ls = if (c == 0) 0 else text.lastIndexOf('\n', c - 1) + 1
    val le = text.indexOf('\n', c).let { if (it < 0) text.length else it }
    val line = text.substring(ls, le)
    val m = RE_CHECK.find(line) ?: return this
    val idx = ls + m.value.length - 3 // posizione del carattere dentro [ ]
    val now = text[idx]
    val repl = if (now == ' ') 'x' else ' '
    return TextFieldValue(text.substring(0, idx) + repl + text.substring(idx + 1), selection)
}

/** Usato dall'anteprima: inverte la spunta della riga [lineIndex]. */
fun toggleCheckAtLine(text: String, lineIndex: Int): String {
    val lines = text.split("\n").toMutableList()
    if (lineIndex !in lines.indices) return text
    val m = RE_CHECK.find(lines[lineIndex]) ?: return text
    val idx = m.value.length - 3
    val l = lines[lineIndex]
    lines[lineIndex] = l.substring(0, idx) + (if (l[idx] == ' ') 'x' else ' ') + l.substring(idx + 1)
    return lines.joinToString("\n")
}

fun TextFieldValue.insertText(t: String, cursorBack: Int = 0): TextFieldValue {
    val s = selection.min
    val e = selection.max
    val newText = text.substring(0, s) + t + text.substring(e)
    return TextFieldValue(newText, TextRange(s + t.length - cursorBack))
}

fun TextFieldValue.codeBlock(): TextFieldValue {
    val s = selection.min
    val e = selection.max
    val sel = text.substring(s, e)
    val ins = "```\n$sel\n```"
    return TextFieldValue(text.substring(0, s) + ins + text.substring(e), TextRange(s + 4, s + 4 + sel.length))
}

fun TextFieldValue.divider(): TextFieldValue = insertText("\n---\n")

fun nowText(): String = SimpleDateFormat("d MMMM yyyy, HH:mm", Locale.getDefault()).format(Date())

/** Come Samsung Notes: premendo Invio in un elenco, la riga successiva continua l'elenco (anche le caselle). */
fun autoContinue(old: TextFieldValue, new: TextFieldValue): TextFieldValue {
    if (new.text.length != old.text.length + 1 || !new.selection.collapsed) return new
    val c = new.selection.start
    if (c < 1 || new.text[c - 1] != '\n') return new
    if (old.selection.start != c - 1 && !old.selection.collapsed) return new
    val ls = if (c - 2 < 0) 0 else new.text.lastIndexOf('\n', c - 2) + 1
    val prev = new.text.substring(ls, c - 1)
    val m = RE_CONTINUE.find(prev) ?: return new
    val indent = m.groupValues[1]
    val marker = m.groupValues[2]
    if (prev.length == m.value.length) {
        // Elemento vuoto: Invio chiude l'elenco.
        return TextFieldValue(new.text.substring(0, ls) + new.text.substring(c - 1), TextRange(ls + 1))
    }
    val next = when {
        marker.startsWith("- [") -> "- [ ] "
        m.groupValues[3].isNotEmpty() -> "${m.groupValues[3].toInt() + 1}. "
        else -> marker
    }
    val ins = indent + next
    return TextFieldValue(new.text.substring(0, c) + ins + new.text.substring(c), TextRange(c + ins.length))
}

fun findMatches(text: String, q: String, limit: Int = 5000): List<Int> {
    if (q.isEmpty()) return emptyList()
    val out = ArrayList<Int>()
    var from = 0
    while (out.size < limit) {
        val i = text.indexOf(q, from, ignoreCase = true)
        if (i < 0) break
        out.add(i); from = i + maxOf(1, q.length)
    }
    return out
}

fun wordCount(t: String): Int {
    var n = 0
    var inWord = false
    for (ch in t) {
        if (ch.isWhitespace()) inWord = false else if (!inWord) { inWord = true; n++ }
    }
    return n
}

/** Modelli di nota ispirati a quelli di Samsung Notes (riunione, lista, giornata, diario, progetto). */
object NoteTemplates {
    val all: List<Pair<String, String>> = listOf(
        "Appunti di riunione" to "# Riunione\n\n**Data:** {date}\n**Partecipanti:** \n\n## Ordine del giorno\n- \n\n## Decisioni\n- \n\n## Azioni\n- [ ] \n",
        "Lista della spesa" to "# Spesa\n\n- [ ] \n- [ ] \n- [ ] \n",
        "Giornata" to "# Oggi\n\n## Priorità\n- [ ] \n\n## Da fare\n- [ ] \n\n## Note\n",
        "Diario" to "# {date}\n\n## Com'è andata\n\n## Cose belle\n- \n\n## Da migliorare\n- \n",
        "Progetto" to "# Progetto\n\n## Obiettivo\n\n## Passi\n- [ ] \n\n## Risorse\n- \n",
    )
    private val tx: Map<String, Array<String>> = mapOf(
        "Appunti di riunione" to arrayOf(
            "# Meeting\n\n**Date:** {date}\n**Attendees:** \n\n## Agenda\n- \n\n## Decisions\n- \n\n## Actions\n- [ ] \n",
            "# Reunión\n\n**Fecha:** {date}\n**Asistentes:** \n\n## Orden del día\n- \n\n## Decisiones\n- \n\n## Acciones\n- [ ] \n",
            "# 会议\n\n**日期：** {date}\n**参会人员：** \n\n## 议程\n- \n\n## 决定\n- \n\n## 待办\n- [ ] \n"),
        "Lista della spesa" to arrayOf(
            "# Shopping\n\n- [ ] \n- [ ] \n- [ ] \n", "# Compras\n\n- [ ] \n- [ ] \n- [ ] \n", "# 购物清单\n\n- [ ] \n- [ ] \n- [ ] \n"),
        "Giornata" to arrayOf(
            "# Today\n\n## Priorities\n- [ ] \n\n## To do\n- [ ] \n\n## Notes\n",
            "# Hoy\n\n## Prioridades\n- [ ] \n\n## Por hacer\n- [ ] \n\n## Notas\n",
            "# 今天\n\n## 优先事项\n- [ ] \n\n## 待办\n- [ ] \n\n## 备注\n"),
        "Diario" to arrayOf(
            "# {date}\n\n## How it went\n\n## Good things\n- \n\n## To improve\n- \n",
            "# {date}\n\n## Cómo fue\n\n## Cosas buenas\n- \n\n## A mejorar\n- \n",
            "# {date}\n\n## 今天怎么样\n\n## 美好的事\n- \n\n## 需要改进\n- \n"),
        "Progetto" to arrayOf(
            "# Project\n\n## Goal\n\n## Steps\n- [ ] \n\n## Resources\n- \n",
            "# Proyecto\n\n## Objetivo\n\n## Pasos\n- [ ] \n\n## Recursos\n- \n",
            "# 项目\n\n## 目标\n\n## 步骤\n- [ ] \n\n## 资源\n- \n"),
    )

    fun render(name: String, t: String, lang: com.notium.app.data.AppLang): String {
        val i = lang.idx
        val body = if (i >= 0) tx[name]?.get(i) ?: t else t
        return body.replace("{date}", nowText())
    }
}

// -------------------------------------------------- formattazione in tempo reale nell'editor

private val RE_BOLD = Regex("\\*\\*(.+?)\\*\\*")
private val RE_ITALIC = Regex("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)")
private val RE_CODE = Regex("`([^`\n]+)`")
private val RE_MARK = Regex("==(.+?)==")
private val RE_STRIKE = Regex("~~(.+?)~~")
private val RE_HEAD = Regex("^(#{1,3}) ")
private val RE_ANYCHECK = Regex("^\\s*- \\[([ xX])\\] ")
private val RE_BULLETLINE = Regex("^\\s*([-*+]|\\d+\\.) ")

/** Stile applicato mentre scrivi: non cambia il testo, quindi il cursore non si sposta mai. */
class MarkdownStyler(
    private val dim: Color, private val accent: Color, private val codeBg: Color, private val markBg: Color,
    private val findQuery: String, private val findBg: Color, private val baseSp: Float,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText =
        TransformedText(style(text.text), OffsetMapping.Identity)

    private fun style(src: String): AnnotatedString {
        if (src.length > 60_000) return AnnotatedString(src)
        return buildAnnotatedString {
            append(src)
            var pos = 0
            var inCode = false
            while (pos <= src.length) {
                var end = src.indexOf('\n', pos)
                if (end < 0) end = src.length
                val line = src.substring(pos, end)
                if (line.trimStart().startsWith("```")) {
                    addStyle(SpanStyle(color = dim, fontFamily = FontFamily.Monospace), pos, end)
                    inCode = !inCode
                } else if (inCode) {
                    addStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBg), pos, end)
                } else {
                    styleLine(pos, line)
                }
                pos = end + 1
            }
            if (findQuery.isNotEmpty()) {
                var from = 0
                var guard = 0
                while (guard++ < 2000) {
                    val i = src.indexOf(findQuery, from, ignoreCase = true)
                    if (i < 0) break
                    addStyle(SpanStyle(background = findBg), i, i + findQuery.length)
                    from = i + maxOf(1, findQuery.length)
                }
            }
        }
    }

    private fun AnnotatedString.Builder.styleLine(a: Int, line: String) {
        if (line.isEmpty()) return
        val b = a + line.length
        val h = RE_HEAD.find(line)
        if (h != null) {
            val level = h.groupValues[1].length
            val f = when (level) { 1 -> 1.35f; 2 -> 1.2f; else -> 1.1f }
            addStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = (baseSp * f).sp), a, b)
            addStyle(SpanStyle(color = dim), a, a + level + 1)
        }
        val chk = RE_ANYCHECK.find(line)
        if (chk != null) {
            val done = chk.groupValues[1].lowercase() == "x"
            val markerEnd = a + chk.value.length
            addStyle(SpanStyle(color = if (done) accent else dim, fontWeight = FontWeight.Bold), markerEnd - 6, markerEnd - 1)
            if (done) addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = dim), markerEnd, b)
        } else {
            val bl = RE_BULLETLINE.find(line)
            if (bl != null) addStyle(SpanStyle(color = accent, fontWeight = FontWeight.Bold), a + bl.value.length - 2, a + bl.value.length - 1)
        }
        if (line.startsWith(">")) addStyle(SpanStyle(fontStyle = FontStyle.Italic, color = dim), a, b)
        if (line.length >= 3 && line.trim() == "---") addStyle(SpanStyle(color = dim), a, b)
        if (line.indexOf('*') >= 0) {
            RE_BOLD.findAll(line).forEach { m ->
                addStyle(SpanStyle(fontWeight = FontWeight.Bold), a + m.range.first, a + m.range.last + 1)
                addStyle(SpanStyle(color = dim), a + m.range.first, a + m.range.first + 2)
                addStyle(SpanStyle(color = dim), a + m.range.last - 1, a + m.range.last + 1)
            }
            RE_ITALIC.findAll(line).forEach { m ->
                addStyle(SpanStyle(fontStyle = FontStyle.Italic), a + m.range.first, a + m.range.last + 1)
                addStyle(SpanStyle(color = dim), a + m.range.first, a + m.range.first + 1)
                addStyle(SpanStyle(color = dim), a + m.range.last, a + m.range.last + 1)
            }
        }
        if (line.indexOf('`') >= 0) RE_CODE.findAll(line).forEach { m ->
            addStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBg), a + m.range.first, a + m.range.last + 1)
        }
        if (line.indexOf("==") >= 0) RE_MARK.findAll(line).forEach { m ->
            addStyle(SpanStyle(background = markBg), a + m.range.first, a + m.range.last + 1)
            addStyle(SpanStyle(color = dim), a + m.range.first, a + m.range.first + 2)
            addStyle(SpanStyle(color = dim), a + m.range.last - 1, a + m.range.last + 1)
        }
        if (line.indexOf("~~") >= 0) RE_STRIKE.findAll(line).forEach { m ->
            addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), a + m.range.first, a + m.range.last + 1)
        }
    }
}

// ----------------------------------------------------------------- anteprima Markdown

fun parseInline(s: String, link: Color, codeBg: Color, markBg: Color = Color(0x55FFE066)): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < s.length) {
        when {
            s.startsWith("**", i) -> {
                val j = s.indexOf("**", i + 2)
                if (j != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(parseInline(s.substring(i + 2, j), link, codeBg, markBg)) }
                    i = j + 2
                } else { append("**"); i += 2 }
            }
            s.startsWith("~~", i) -> {
                val j = s.indexOf("~~", i + 2)
                if (j != -1) {
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) { append(parseInline(s.substring(i + 2, j), link, codeBg, markBg)) }
                    i = j + 2
                } else { append("~~"); i += 2 }
            }
            s.startsWith("==", i) -> {
                val j = s.indexOf("==", i + 2)
                if (j != -1) {
                    withStyle(SpanStyle(background = markBg)) { append(parseInline(s.substring(i + 2, j), link, codeBg, markBg)) }
                    i = j + 2
                } else { append("=="); i += 2 }
            }
            s[i] == '*' -> {
                val j = s.indexOf('*', i + 1)
                if (j > i + 1) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(parseInline(s.substring(i + 1, j), link, codeBg, markBg)) }
                    i = j + 1
                } else { append('*'); i++ }
            }
            s[i] == '`' -> {
                val j = s.indexOf('`', i + 1)
                if (j > i + 1) {
                    withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBg)) { append(s.substring(i + 1, j)) }
                    i = j + 1
                } else { append('`'); i++ }
            }
            s[i] == '[' -> {
                val mid = s.indexOf("](", i + 1)
                val end = if (mid != -1) s.indexOf(')', mid + 2) else -1
                if (mid != -1 && end != -1) {
                    withStyle(SpanStyle(color = link, textDecoration = TextDecoration.Underline)) { append(s.substring(i + 1, mid)) }
                    i = end + 1
                } else { append('['); i++ }
            }
            else -> { append(s[i]); i++ }
        }
    }
}

private sealed interface Block {
    data object Blank : Block
    data object Rule : Block
    data class Heading(val level: Int, val text: AnnotatedString) : Block
    data class Bullet(val indent: Int, val text: AnnotatedString) : Block
    data class Numbered(val n: String, val indent: Int, val text: AnnotatedString) : Block
    data class Check(val line: Int, val indent: Int, val done: Boolean, val text: AnnotatedString) : Block
    data class Quote(val text: AnnotatedString) : Block
    data class Code(val text: String) : Block
    data class Table(val rows: List<List<AnnotatedString>>) : Block
    data class Para(val text: AnnotatedString) : Block
}

private val RE_H = Regex("^(#{1,3})\\s+(.*)$")
private val RE_B = Regex("^(\\s*)[-*+]\\s+(.*)$")
private val RE_N = Regex("^(\\s*)(\\d+)\\.\\s+(.*)$")
private val RE_C = Regex("^(\\s*)- \\[([ xX])\\]\\s?(.*)$")
private val RE_SEP = Regex("^\\|?\\s*:?-{2,}:?\\s*(\\|\\s*:?-{2,}:?\\s*)*\\|?$")

private fun parseBlocks(text: String, link: Color, codeBg: Color, markBg: Color): List<Block> {
    val lines = text.split("\n")
    val out = ArrayList<Block>(lines.size)
    var i = 0
    while (i < lines.size) {
        val line = lines[i].trimEnd()
        when {
            line.trimStart().startsWith("```") -> {
                val sb = StringBuilder()
                i++
                while (i < lines.size && !lines[i].trimStart().startsWith("```")) { if (sb.isNotEmpty()) sb.append('\n'); sb.append(lines[i]); i++ }
                out.add(Block.Code(sb.toString()))
            }
            line.startsWith("|") -> {
                val rows = ArrayList<List<AnnotatedString>>()
                while (i < lines.size && lines[i].trimEnd().startsWith("|")) {
                    val l = lines[i].trim()
                    if (!RE_SEP.matches(l)) {
                        rows.add(l.trim('|').split("|").map { parseInline(it.trim(), link, codeBg, markBg) })
                    }
                    i++
                }
                out.add(Block.Table(rows))
                continue
            }
            line.isBlank() -> out.add(Block.Blank)
            RE_H.find(line) != null -> { val m = RE_H.find(line)!!; out.add(Block.Heading(m.groupValues[1].length, parseInline(m.groupValues[2], link, codeBg, markBg))) }
            RE_C.find(line) != null -> {
                val m = RE_C.find(line)!!
                out.add(Block.Check(i, m.groupValues[1].length / 2, m.groupValues[2].lowercase() == "x", parseInline(m.groupValues[3], link, codeBg, markBg)))
            }
            RE_B.find(line) != null -> { val m = RE_B.find(line)!!; out.add(Block.Bullet(m.groupValues[1].length / 2, parseInline(m.groupValues[2], link, codeBg, markBg))) }
            RE_N.find(line) != null -> { val m = RE_N.find(line)!!; out.add(Block.Numbered(m.groupValues[2], m.groupValues[1].length / 2, parseInline(m.groupValues[3], link, codeBg, markBg))) }
            line.trim() == "---" -> out.add(Block.Rule)
            line.startsWith(">") -> out.add(Block.Quote(parseInline(line.removePrefix(">").trim(), link, codeBg, markBg)))
            else -> out.add(Block.Para(parseInline(line, link, codeBg, markBg)))
        }
        i++
    }
    return out
}

@Composable
fun MarkdownText(
    text: String, modifier: Modifier = Modifier, family: FontFamily = FontFamily.Default,
    sizeSp: Float = 17f, spacing: Float = 1.5f, onToggleLine: ((Int) -> Unit)? = null,
) {
    val link = MaterialTheme.colorScheme.primary
    val codeBg = MaterialTheme.colorScheme.surfaceVariant
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val line = MaterialTheme.colorScheme.outlineVariant
    val markBg = Color(0x66FFD54F)
    val body = MaterialTheme.typography.bodyLarge.copy(fontFamily = family, fontSize = sizeSp.sp, lineHeight = (sizeSp * spacing).sp)
    // Il testo viene analizzato una sola volta finche' non cambia.
    val blocks = remember(text, link, codeBg) { parseBlocks(text, link, codeBg, markBg) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        blocks.forEach { b ->
            when (b) {
                Block.Blank -> Spacer(Modifier.height(6.dp))
                Block.Rule -> HorizontalDivider(Modifier.padding(vertical = 8.dp))
                is Block.Heading -> {
                    val style = when (b.level) {
                        1 -> MaterialTheme.typography.headlineMedium
                        2 -> MaterialTheme.typography.titleLarge
                        else -> MaterialTheme.typography.titleMedium
                    }
                    Text(b.text, style = style.copy(fontFamily = family, fontWeight = FontWeight.Bold), modifier = Modifier.padding(top = 8.dp))
                }
                is Block.Bullet -> Row(Modifier.padding(start = (b.indent * 18).dp)) {
                    Text("•", style = body, modifier = Modifier.width(22.dp).padding(start = 6.dp))
                    Text(b.text, style = body)
                }
                is Block.Numbered -> Row(Modifier.padding(start = (b.indent * 18).dp)) {
                    Text("${b.n}.", style = body, modifier = Modifier.width(28.dp))
                    Text(b.text, style = body)
                }
                is Block.Check -> Row(
                    Modifier.padding(start = (b.indent * 18).dp).let { m -> if (onToggleLine != null) m.clickable { onToggleLine(b.line) } else m },
                    verticalAlignment = androidx.compose.ui.Alignment.Top,
                ) {
                    Icon(
                        if (b.done) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank, if (b.done) "Fatto" else "Da fare",
                        Modifier.padding(top = 2.dp, end = 8.dp).size((sizeSp * 1.2f).dp),
                        tint = if (b.done) link else muted,
                    )
                    Text(
                        b.text,
                        style = if (b.done) body.copy(color = muted, textDecoration = TextDecoration.LineThrough) else body,
                    )
                }
                is Block.Quote -> Row(Modifier.height(androidx.compose.foundation.layout.IntrinsicSize.Min)) {
                    Box(Modifier.width(3.dp).fillMaxHeight().background(line))
                    Text(b.text, style = body.copy(fontStyle = FontStyle.Italic, color = muted), modifier = Modifier.padding(start = 12.dp))
                }
                is Block.Code -> Box(
                    Modifier.fillMaxWidth().background(codeBg, RoundedCornerShape(8.dp)).horizontalScroll(rememberScrollState()).padding(12.dp)
                ) { Text(b.text, style = body.copy(fontFamily = FontFamily.Monospace, fontSize = (sizeSp * 0.9f).sp)) }
                is Block.Table -> Column(Modifier.fillMaxWidth().background(codeBg.copy(alpha = 0.4f), RoundedCornerShape(8.dp))) {
                    b.rows.forEachIndexed { r, cells ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp, horizontal = 8.dp)) {
                            cells.forEach { c ->
                                Text(c, style = if (r == 0) body.copy(fontWeight = FontWeight.Bold) else body, modifier = Modifier.weight(1f).padding(end = 8.dp))
                            }
                        }
                        if (r < b.rows.size - 1) HorizontalDivider(color = line)
                    }
                }
                is Block.Para -> Text(b.text, style = body, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/** Evidenzia nel testo i termini cercati (senza distinzione tra maiuscole e minuscole). */
fun highlight(text: String, query: String, color: Color): AnnotatedString {
    val terms = query.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (terms.isEmpty()) return AnnotatedString(text)
    val lower = text.lowercase()
    val marks = BooleanArray(text.length)
    terms.forEach { t ->
        val lt = t.lowercase()
        var from = 0
        while (true) {
            val idx = lower.indexOf(lt, from)
            if (idx < 0) break
            for (k in idx until minOf(idx + lt.length, marks.size)) marks[k] = true
            from = idx + lt.length
        }
    }
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            var j = i
            while (j < text.length && marks[j] == marks[i]) j++
            if (marks[i]) withStyle(SpanStyle(background = color, fontWeight = FontWeight.SemiBold)) { append(text.substring(i, j)) }
            else append(text.substring(i, j))
            i = j
        }
    }
}
