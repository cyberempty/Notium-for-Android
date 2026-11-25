package com.notium.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

/**
 * Editor a "testo formattato": il grassetto si vede grasso, i titoli grandi, le liste con i pallini.
 * Dentro la nota il testo resta salvato come Markdown (cosi' ricerca, esportazione e vecchie note funzionano),
 * ma chi scrive non vede mai simboli come ** o #.
 */
enum class Fmt(val line: Boolean) { BOLD(false), ITALIC(false), STRIKE(false), MARK(false), CODE(false), H1(true), H2(true), H3(true) }

data class Span(val s: Int, val e: Int, val k: Fmt)

object RP {
    const val BUL = "• "
    const val CHK0 = "□ "
    const val CHK1 = "■ "
    const val QUOTE = "│ "
    const val DIVIDER = "────────────────"
}

data class Rich(val tf: TextFieldValue, val spans: List<Span> = emptyList()) {
    val text: String get() = tf.text

    // ------------------------------------------------------------ stato dello stile

    fun activeAt(pos: Int, forward: Boolean): Set<Fmt> =
        spans.filter {
            !it.k.line && (if (forward) it.s <= pos && pos < it.e else it.s < pos && pos <= it.e)
        }.map { it.k }.toSet()

    /** Stili attivi per evidenziare i pulsanti della barra. */
    fun activeKinds(pending: Set<Fmt>?): Set<Fmt> {
        val r = tf.selection
        if (!r.collapsed) {
            return Fmt.values().filter { k -> !k.line && spans.any { it.k == k && it.s <= r.min && it.e >= r.max } }.toSet()
        }
        return pending ?: activeAt(r.start, false)
    }

    fun toggle(k: Fmt): Rich {
        val r = tf.selection
        if (r.collapsed) return this
        val a = r.min; val b = r.max
        val covered = spans.any { it.k == k && it.s <= a && it.e >= b }
        val sp = RichOps.cut(spans, k, a, b)
        return copy(spans = RichOps.normalize(if (covered) sp else sp + Span(a, b, k), text))
    }

    // ------------------------------------------------------------ modifiche

    /** Sostituisce [p, p+removed) con [ins]; il nuovo testo riceve gli stili [kinds]. */
    fun edit(p: Int, removed: Int, ins: String, kinds: Set<Fmt> = emptySet(), sel: TextRange? = null): Rich {
        val nt = text.replaceRange(p, p + removed, ins)
        var sp = RichOps.adjust(spans, p, removed, ins.length, !ins.contains('\n'))
        sp = RichOps.setKinds(sp, p, p + ins.length, kinds)
        return Rich(TextFieldValue(nt, sel ?: TextRange(p + ins.length)), RichOps.normalize(sp, nt))
    }

    /** Dal testo prodotto dalla tastiera ricava il nuovo contenuto mantenendo gli stili. */
    fun fromKeyboard(nv: TextFieldValue, pending: Set<Fmt>?): Rich {
        val old = text; val nw = nv.text
        if (nw == old) return Rich(nv, spans)
        val delta = nw.length - old.length
        val maxPre = minOf(old.length, nw.length)
        val cap = if (nv.selection.collapsed) (if (delta > 0) nv.selection.start - delta else nv.selection.start).coerceIn(0, maxPre) else maxPre
        var pre = 0
        while (pre < cap && old[pre] == nw[pre]) pre++
        var suf = 0
        while (suf < old.length - pre && suf < nw.length - pre && old[old.length - 1 - suf] == nw[nw.length - 1 - suf]) suf++
        val removed = old.length - pre - suf
        val insLen = nw.length - pre - suf
        val desired = pending ?: activeAt(pre, removed > 0)
        val inserted = nw.substring(pre, pre + insLen)
        var sp = RichOps.adjust(spans, pre, removed, insLen, !inserted.contains('\n'))
        if (insLen > 0) sp = RichOps.setKinds(sp, pre, pre + insLen, desired)
        return Rich(nv, RichOps.normalize(sp, nw))
    }

    /** Invio dentro una lista: continua l'elenco (o lo chiude se la voce e' vuota). */
    fun afterEnter(oldLen: Int): Rich {
        if (text.length != oldLen + 1) return this
        val c = tf.selection
        if (!c.collapsed) return this
        val nl = c.start - 1
        if (nl < 0 || text[nl] != '\n') return this
        val ls = RichOps.lineStart(text, nl)
        val pf = RichOps.prefixAt(text, ls, nl)
        if (pf.kind == '-') return this
        if (nl == ls + pf.indent + pf.len) return edit(ls + pf.indent, pf.len + 1, "")
        val next = when (pf.kind) {
            'b' -> RP.BUL
            'u', 'k' -> RP.CHK0
            'q' -> RP.QUOTE
            else -> ((text.substring(ls + pf.indent, ls + pf.indent + pf.len - 2).toIntOrNull() ?: 0) + 1).toString() + ". "
        }
        return edit(c.start, 0, " ".repeat(pf.indent) + next)
    }

    /** Cancellando lo spazio del simbolo di lista, toglie tutto il simbolo. */
    fun afterDelete(old: String): Rich {
        if (text.length != old.length - 1 || !tf.selection.collapsed) return this
        val pos = tf.selection.start
        if (pos > old.length) return this
        val ls = RichOps.lineStart(old, pos)
        val le = RichOps.lineEnd(old, pos)
        val pf = RichOps.prefixAt(old, ls, le)
        if (pf.kind == '-' || pf.len < 2) return this
        if (pos == ls + pf.indent + pf.len - 1) return edit(ls + pf.indent, pf.len - 1, "")
        return this
    }

    fun insertPlain(t: String): Rich {
        val r = tf.selection
        return edit(r.min, r.max - r.min, t)
    }

    fun insertDivider(): Rich {
        val r = tf.selection
        val atStart = r.min == 0 || text[r.min - 1] == '\n'
        return edit(r.min, r.max - r.min, (if (atStart) "" else "\n") + RP.DIVIDER + "\n")
    }

    fun insertMarkdown(md: String): Rich {
        val (t, sp) = RichCodec.fromMarkdown(md)
        val r = tf.selection
        val p = r.min
        val nt = text.replaceRange(p, r.max, t)
        var all = RichOps.adjust(spans, p, r.max - p, t.length, false)
        all = RichOps.setKinds(all, p, p + t.length, emptySet())
        all = all + sp.map { Span(it.s + p, it.e + p, it.k) }
        return Rich(TextFieldValue(nt, TextRange(p + t.length)), RichOps.normalize(all, nt))
    }

    // ------------------------------------------------------------ righe

    fun selLines(): List<Pair<Int, Int>> {
        val a = tf.selection.min
        var b = tf.selection.max
        if (b > a && b <= text.length && text[b - 1] == '\n') b--
        val out = ArrayList<Pair<Int, Int>>()
        var s = RichOps.lineStart(text, a)
        while (true) {
            val e = RichOps.lineEnd(text, s)
            out.add(s to e)
            if (e >= b || e >= text.length) break
            s = e + 1
        }
        return out
    }

    private fun applyEdits(edits: List<Triple<Int, Int, String>>, spansIn: List<Span> = spans): Rich {
        var t = text
        var sp = spansIn
        var a = tf.selection.start
        var b = tf.selection.end
        for ((pos, rem, ins) in edits.sortedByDescending { it.first }) {
            t = t.replaceRange(pos, pos + rem, ins)
            sp = RichOps.setKinds(RichOps.adjust(sp, pos, rem, ins.length, true), pos, pos + ins.length, emptySet())
            a = RichOps.mapPos(a, pos, rem, ins.length)
            b = RichOps.mapPos(b, pos, rem, ins.length)
        }
        return Rich(TextFieldValue(t, TextRange(a.coerceIn(0, t.length), b.coerceIn(0, t.length))), RichOps.normalize(sp, t))
    }

    private fun withoutLineKinds(lines: List<Pair<Int, Int>>): List<Span> =
        spans.filterNot { sp -> sp.k.line && lines.any { (ls, le) -> sp.s <= le && sp.e >= ls } }

    private fun setList(target: Char): Rich {
        val lines = selLines()
        val infos = lines.map { (s, e) -> RichOps.prefixAt(text, s, e) }
        val same = infos.all { it.kind == target || (target == 'u' && it.kind == 'k') }
        val edits = lines.mapIndexed { i, (s, _) ->
            val pf = infos[i]
            val np = if (same) "" else when (target) {
                'b' -> RP.BUL
                'u' -> RP.CHK0
                'q' -> RP.QUOTE
                else -> "${i + 1}. "
            }
            Triple(s + pf.indent, pf.len, np)
        }
        return applyEdits(edits, withoutLineKinds(lines))
    }

    fun bullets() = setList('b')
    fun numbered() = setList('n')
    fun checklist() = setList('u')
    fun quote() = setList('q')

    fun toggleCheck(): Rich {
        val lines = selLines()
        val infos = lines.map { (s, e) -> RichOps.prefixAt(text, s, e) }
        if (infos.none { it.kind == 'u' || it.kind == 'k' }) return checklist()
        val edits = ArrayList<Triple<Int, Int, String>>()
        lines.forEachIndexed { i, (s, _) ->
            val pf = infos[i]
            if (pf.kind == 'u') edits.add(Triple(s + pf.indent, 2, RP.CHK1))
            else if (pf.kind == 'k') edits.add(Triple(s + pf.indent, 2, RP.CHK0))
        }
        return applyEdits(edits)
    }

    fun toggleCheckAt(pos: Int): Rich? {
        val ls = RichOps.lineStart(text, pos)
        val le = RichOps.lineEnd(text, pos)
        val pf = RichOps.prefixAt(text, ls, le)
        val at = ls + pf.indent
        if (pf.kind != 'u' && pf.kind != 'k') return null
        if (pos > at + 1) return null
        val nt = text.replaceRange(at, at + 2, if (pf.kind == 'u') RP.CHK1 else RP.CHK0)
        return Rich(TextFieldValue(nt, TextRange(at + 2)), spans)
    }

    fun indent(delta: Int): Rich {
        val edits = ArrayList<Triple<Int, Int, String>>()
        for ((s, e) in selLines()) {
            if (delta > 0) edits.add(Triple(s, 0, "  "))
            else {
                var n = 0
                while (n < 2 && s + n < e && text[s + n] == ' ') n++
                if (n > 0) edits.add(Triple(s, n, ""))
            }
        }
        return if (edits.isEmpty()) this else applyEdits(edits)
    }

    /** level 0 = testo normale. */
    fun heading(level: Int): Rich {
        val lines0 = selLines()
        // un titolo non convive con i simboli di lista
        val infos = lines0.map { (s, e) -> RichOps.prefixAt(text, s, e) }
        val removePrefix = lines0.mapIndexedNotNull { i, (s, _) -> if (infos[i].len > 0) Triple(s + infos[i].indent, infos[i].len, "") else null }
        val base = if (removePrefix.isEmpty()) this else applyEdits(removePrefix)
        val lines = base.selLines()
        val kind = when (level) { 1 -> Fmt.H1; 2 -> Fmt.H2; 3 -> Fmt.H3; else -> null }
        val already = kind != null && lines.all { (ls, le) -> base.spans.any { it.k == kind && it.s <= le && it.e >= ls } }
        var sp = base.withoutLineKinds(lines)
        if (kind != null && !already) sp = sp + lines.map { (ls, le) -> Span(ls, le, kind) }
        return Rich(base.tf, RichOps.normalize(sp, base.text))
    }
}

data class Pfx(val indent: Int, val kind: Char, val len: Int)

object RichOps {
    private val NUM = Regex("^\\d+\\. ")

    fun lineStart(t: String, i: Int): Int = if (i <= 0) 0 else t.lastIndexOf('\n', i - 1) + 1
    fun lineEnd(t: String, i: Int): Int { val k = t.indexOf('\n', i.coerceIn(0, t.length)); return if (k < 0) t.length else k }

    fun prefixAt(t: String, ls: Int, le: Int): Pfx {
        var ind = 0
        while (ls + ind < le && t[ls + ind] == ' ') ind++
        val at = ls + ind
        fun has(p: String) = le - at >= p.length && t.startsWith(p, at)
        return when {
            has(RP.BUL) -> Pfx(ind, 'b', 2)
            has(RP.CHK0) -> Pfx(ind, 'u', 2)
            has(RP.CHK1) -> Pfx(ind, 'k', 2)
            has(RP.QUOTE) -> Pfx(ind, 'q', 2)
            else -> {
                val m = NUM.find(t.substring(at, le))
                if (m != null) Pfx(ind, 'n', m.value.length) else Pfx(ind, '-', 0)
            }
        }
    }

    private fun isEnd(c: Char) = c == '.' || c == '!' || c == '?' || c == '。' || c == '！' || c == '？' || c == '…'

    /** Intervallo dell'intera frase che contiene [pos] (confini: . ! ? e a capo). */
    fun sentenceAround(t: String, pos: Int): TextRange {
        if (t.isEmpty()) return TextRange(0)
        val p = pos.coerceIn(0, t.length)
        val ls = lineStart(t, p)
        val le = lineEnd(t, p)
        var s = p
        while (s > ls && !isEnd(t[s - 1])) s--
        var e = p
        while (e < le && !isEnd(t[e])) e++
        if (e < le) e++ // include il segno di fine frase
        // salta spazi iniziali e il simbolo di lista
        val pf = prefixAt(t, ls, le)
        if (s <= ls + pf.indent + pf.len) s = ls + pf.indent + pf.len
        while (s < e && t[s].isWhitespace()) s++
        while (e > s && t[e - 1].isWhitespace()) e--
        return if (e > s) TextRange(s, e) else TextRange(p)
    }

    fun mapPos(pos: Int, p: Int, removed: Int, ins: Int): Int = when {
        pos >= p + removed -> pos + ins - removed
        pos > p -> p + ins
        else -> pos
    }

    private fun emptyLine(t: String, i: Int): Boolean = (i >= t.length || t[i] == '\n') && (i == 0 || t[i - 1] == '\n')

    fun cut(spans: List<Span>, k: Fmt, a: Int, b: Int): List<Span> {
        val out = ArrayList<Span>()
        for (sp in spans) {
            if (sp.k != k || sp.e <= a || sp.s >= b) { out.add(sp); continue }
            if (sp.s < a) out.add(Span(sp.s, a, k))
            if (sp.e > b) out.add(Span(b, sp.e, k))
        }
        return out
    }

    /** Imposta esattamente [kinds] come stili di carattere sull'intervallo. */
    fun setKinds(spans: List<Span>, from: Int, to: Int, kinds: Set<Fmt>): List<Span> {
        if (to <= from) return spans
        val out = ArrayList<Span>()
        for (sp in spans) {
            if (sp.k.line || sp.e <= from || sp.s >= to) { out.add(sp); continue }
            if (sp.s < from) out.add(Span(sp.s, from, sp.k))
            if (sp.e > to) out.add(Span(to, sp.e, sp.k))
        }
        for (k in kinds) if (!k.line) out.add(Span(from, to, k))
        return out
    }

    fun adjust(spans: List<Span>, p: Int, removed: Int, ins: Int, noNewline: Boolean): List<Span> {
        val end0 = p + removed
        val d = ins - removed
        return spans.map { sp ->
            if (sp.k.line && sp.e == p && removed == 0 && ins > 0 && noNewline) {
                Span(sp.s, p + ins, sp.k)
            } else {
                val ns = when {
                    sp.s < p -> sp.s
                    sp.s == p && removed > 0 -> p
                    sp.s >= end0 -> sp.s + d
                    else -> p + ins
                }
                val ne = when {
                    sp.e <= p -> sp.e
                    sp.e >= end0 -> sp.e + d
                    else -> p
                }
                Span(ns, ne, sp.k)
            }
        }
    }

    fun normalize(spans: List<Span>, text: String): List<Span> {
        val n = text.length
        val out = ArrayList<Span>()
        for (k in Fmt.values()) {
            val list = spans.filter { it.k == k }.map { Span(it.s.coerceIn(0, n), it.e.coerceIn(0, n), k) }.sortedBy { it.s }
            if (k.line) {
                for (sp in list) {
                    var e = sp.e
                    val nl = text.indexOf('\n', sp.s)
                    if (nl >= 0 && nl < e) e = nl
                    if (e > sp.s) out.add(Span(sp.s, e, k))
                    else if (e == sp.s && emptyLine(text, sp.s)) out.add(Span(sp.s, e, k))
                }
            } else {
                var cur: Span? = null
                for (sp in list) {
                    if (sp.e <= sp.s) continue
                    val c = cur
                    if (c != null && sp.s <= c.e) cur = Span(c.s, maxOf(c.e, sp.e), k)
                    else { if (c != null) out.add(c); cur = sp }
                }
                if (cur != null) out.add(cur)
            }
        }
        return out
    }
}

/** Conversione tra il testo formattato e il Markdown che viene salvato. */
object RichCodec {
    private val MD_ORDER = listOf(Fmt.BOLD, Fmt.ITALIC, Fmt.STRIKE, Fmt.MARK, Fmt.CODE)
    private fun mark(k: Fmt) = when (k) { Fmt.BOLD -> "**"; Fmt.ITALIC -> "*"; Fmt.STRIKE -> "~~"; Fmt.MARK -> "=="; else -> "`" }

    // ---------------------------------------------------------------- Rich -> Markdown

    fun toMarkdown(text: String, spans: List<Span>): String {
        val sb = StringBuilder()
        var ls = 0
        while (ls <= text.length) {
            val le = RichOps.lineEnd(text, ls)
            sb.append(lineToMd(text, spans, ls, le))
            if (le >= text.length) break
            sb.append('\n')
            ls = le + 1
        }
        return sb.toString()
    }

    private val NEEDS_ESC = Regex("^(#{1,6} |[-*+] |> |-{3,}\\s*$|\\*{3,}\\s*$|_{3,}\\s*$)")

    private fun lineToMd(t: String, spans: List<Span>, ls: Int, le: Int): String {
        if (t.substring(ls, le) == RP.DIVIDER) return "---"
        val h = spans.firstOrNull { it.k.line && it.s <= le && it.e >= ls }
        if (h != null) {
            val lv = when (h.k) { Fmt.H1 -> 1; Fmt.H2 -> 2; else -> 3 }
            return "#".repeat(lv) + " " + inlineMd(t, spans, ls, le).trimStart()
        }
        val pf = RichOps.prefixAt(t, ls, le)
        val ind = " ".repeat(pf.indent)
        return when (pf.kind) {
            'b' -> ind + "- " + inlineMd(t, spans, ls + pf.indent + pf.len, le)
            'u' -> ind + "- [ ] " + inlineMd(t, spans, ls + pf.indent + pf.len, le)
            'k' -> ind + "- [x] " + inlineMd(t, spans, ls + pf.indent + pf.len, le)
            'q' -> ind + "> " + inlineMd(t, spans, ls + pf.indent + pf.len, le)
            else -> {
                val inl = inlineMd(t, spans, ls + pf.indent, le)
                ind + (if (NEEDS_ESC.containsMatchIn(inl)) "\\" else "") + inl
            }
        }
    }

    private fun esc(s: String): String =
        s.replace("\\", "\\\\").replace("*", "\\*").replace("~~", "\\~\\~").replace("==", "\\=\\=").replace("`", "\\`")

    private fun inlineMd(t: String, spans: List<Span>, a: Int, b: Int): String {
        if (b <= a) return ""
        val cuts = sortedSetOf(a, b)
        for (sp in spans) if (!sp.k.line) { if (sp.s in a..b) cuts.add(sp.s); if (sp.e in a..b) cuts.add(sp.e) }
        val pts = cuts.toList()
        val segs = ArrayList<Pair<Set<Fmt>, String>>()
        for (i in 0 until pts.size - 1) {
            val s = pts[i]; val e = pts[i + 1]
            val ks = MD_ORDER.filter { k -> spans.any { it.k == k && it.s <= s && it.e >= e } }.toSet()
            val txt = t.substring(s, e)
            if (segs.isNotEmpty() && segs.last().first == ks) segs[segs.size - 1] = ks to (segs.last().second + txt)
            else segs.add(ks to txt)
        }
        val sb = StringBuilder()
        for ((ks, raw) in segs) {
            if (ks.isEmpty()) { sb.append(esc(raw)); continue }
            val core = raw.trim()
            if (core.isEmpty()) { sb.append(raw); continue }
            val lead = raw.substring(0, raw.indexOf(core))
            val trail = raw.substring(lead.length + core.length)
            val ordered = MD_ORDER.filter { it in ks }
            sb.append(lead)
            ordered.forEach { sb.append(mark(it)) }
            sb.append(if (Fmt.CODE in ks) core else esc(core))
            ordered.reversed().forEach { sb.append(mark(it)) }
            sb.append(trail)
        }
        return sb.toString()
    }

    // ---------------------------------------------------------------- Markdown -> Rich

    private val HEAD = Regex("^(#{1,6}) (.*)$")
    private val CHECK = Regex("^[-*+] \\[( |x|X)\\](?: (.*))?$")
    private val BULLET = Regex("^[-*+] (.*)$")
    private val RULE = Regex("^(-{3,}|\\*{3,}|_{3,})$")

    fun fromMarkdown(md: String): Pair<String, List<Span>> {
        val sb = StringBuilder()
        val spans = ArrayList<Span>()
        var first = true
        var fence = false
        fun newLine() { if (!first) sb.append('\n'); first = false }
        for (raw in md.replace("\r\n", "\n").split("\n")) {
            if (raw.trimStart().startsWith("```")) { fence = !fence; continue }
            newLine()
            if (fence) {
                val st = sb.length
                sb.append(raw)
                if (sb.length > st) spans.add(Span(st, sb.length, Fmt.CODE))
                continue
            }
            val ind = raw.takeWhile { it == ' ' }.length
            val rest = raw.substring(ind)
            if (ind == 0 && RULE.matches(rest.trim())) { sb.append(RP.DIVIDER); continue }
            val hm = HEAD.find(rest)
            if (hm != null) {
                val st = sb.length
                inline(hm.groupValues[2], sb, spans)
                if (sb.length > st) spans.add(Span(st, sb.length, when (hm.groupValues[1].length) { 1 -> Fmt.H1; 2 -> Fmt.H2; else -> Fmt.H3 }))
                continue
            }
            sb.append(" ".repeat(ind))
            val cm = CHECK.find(rest)
            val bm = BULLET.find(rest)
            when {
                cm != null -> { sb.append(if (cm.groupValues[1] == " ") RP.CHK0 else RP.CHK1); inline(cm.groupValues[2], sb, spans) }
                bm != null -> { sb.append(RP.BUL); inline(bm.groupValues[1], sb, spans) }
                rest.startsWith("> ") -> { sb.append(RP.QUOTE); inline(rest.substring(2), sb, spans) }
                else -> inline(rest, sb, spans)
            }
        }
        return sb.toString() to RichOps.normalize(spans, sb.toString())
    }

    private fun inline(s: String, sb: StringBuilder, spans: MutableList<Span>) {
        val open = HashMap<Fmt, Int>()
        var i = 0
        fun tryMark(m: String, k: Fmt): Boolean {
            if (!s.startsWith(m, i)) return false
            val st = open[k]
            if (st != null) {
                if (sb.length > st) spans.add(Span(st, sb.length, k))
                open.remove(k); i += m.length; return true
            }
            if (s.indexOf(m, i + m.length) > 0) { open[k] = sb.length; i += m.length; return true }
            return false
        }
        while (i < s.length) {
            val c = s[i]
            if (open.containsKey(Fmt.CODE) && c != '`') { sb.append(c); i++; continue }
            if (c == '\\' && i + 1 < s.length && "\\*~=`#->+_".indexOf(s[i + 1]) >= 0) { sb.append(s[i + 1]); i += 2; continue }
            if (tryMark("**", Fmt.BOLD) || tryMark("~~", Fmt.STRIKE) || tryMark("==", Fmt.MARK) || tryMark("`", Fmt.CODE) || tryMark("*", Fmt.ITALIC)) continue
            sb.append(c); i++
        }
    }

    fun load(md: String): Rich {
        val (t, sp) = fromMarkdown(md)
        return Rich(TextFieldValue(t), sp)
    }
}

/** Applica lo stile al testo mentre scrivi (nessun carattere viene nascosto: il cursore non salta mai). */
class RichStyler(
    private val spans: List<Span>,
    private val base: Float,
    private val dim: Color,
    private val codeBg: Color,
    private val markBg: Color,
    private val findQ: String,
    private val findBg: Color,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText =
        TransformedText(build(text.text), OffsetMapping.Identity)

    private fun build(text: String): AnnotatedString = buildAnnotatedString {
        append(text)
        val n = text.length
        for (sp in spans) {
            val s = sp.s.coerceIn(0, n); val e = sp.e.coerceIn(0, n)
            if (e <= s) continue
            val st = when (sp.k) {
                Fmt.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
                Fmt.ITALIC -> SpanStyle(textGeometricTransform = androidx.compose.ui.text.style.TextGeometricTransform(1f, -0.25f))
                Fmt.STRIKE -> SpanStyle(textDecoration = TextDecoration.LineThrough)
                Fmt.MARK -> SpanStyle(background = markBg)
                Fmt.CODE -> SpanStyle(fontFamily = FontFamily.Monospace, background = codeBg)
                Fmt.H1 -> SpanStyle(fontSize = (base * 1.5f).sp, fontWeight = FontWeight.Bold)
                Fmt.H2 -> SpanStyle(fontSize = (base * 1.28f).sp, fontWeight = FontWeight.Bold)
                Fmt.H3 -> SpanStyle(fontSize = (base * 1.12f).sp, fontWeight = FontWeight.Bold)
            }
            addStyle(st, s, e)
        }
        var ls = 0
        while (ls <= n) {
            val le = RichOps.lineEnd(text, ls)
            val pf = RichOps.prefixAt(text, ls, le)
            val at = ls + pf.indent
            when (pf.kind) {
                'u' -> addStyle(SpanStyle(fontSize = (base * 1.15f).sp), at, at + 1)
                'k' -> {
                    addStyle(SpanStyle(fontSize = (base * 1.15f).sp), at, at + 1)
                    if (le > at + 2) addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = dim), at + 2, le)
                }
                'q' -> addStyle(SpanStyle(color = dim, textGeometricTransform = androidx.compose.ui.text.style.TextGeometricTransform(1f, -0.25f)), at, le)
            }
            if (text.substring(ls, le) == RP.DIVIDER) addStyle(SpanStyle(color = dim), ls, le)
            if (le >= n) break
            ls = le + 1
        }
        if (findQ.isNotEmpty()) for (m in findMatches(text, findQ)) addStyle(SpanStyle(background = findBg), m, minOf(n, m + findQ.length))
    }
}
