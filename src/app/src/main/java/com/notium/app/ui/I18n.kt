package com.notium.app.ui

import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import com.notium.app.data.AppLang
import java.util.Locale

val LocalLang = staticCompositionLocalOf { AppLang.EN }

/**
 * Traduzioni. I testi nel codice sono in italiano e fanno da chiave: il dizionario (I18nData) li
 * traduce in inglese, spagnolo e cinese. Segnaposto: {1} = numero, <1> = nome o testo da tradurre.
 */
object I18n {
    fun locale(l: AppLang): Locale = when (l) {
        AppLang.EN -> Locale.ENGLISH
        AppLang.ES -> Locale("es")
        AppLang.IT -> Locale.ITALIAN
        AppLang.ZH -> Locale.SIMPLIFIED_CHINESE
    }

    private class Tpl(val regex: Regex, val hint: String, val slots: List<Pair<Char, Int>>, val out: Array<String>)

    private val exact = HashMap<String, Array<String>>(1024)
    private val templates = ArrayList<Tpl>()
    private val TOKEN = Regex("""\{(\d)\}|<(\d)>""")

    init {
        for (line in I18nData.rows.lineSequence()) {
            if (line.isBlank()) continue
            val p = line.split(" ::: ")
            if (p.size != 4) continue
            val key = p[0]
            val out = arrayOf(p[1], p[2], p[3])
            if (TOKEN.containsMatchIn(key)) {
                val slots = ArrayList<Pair<Char, Int>>()
                val sb = StringBuilder("^")
                var last = 0
                var hint = ""
                for (m in TOKEN.findAll(key)) {
                    val lit = key.substring(last, m.range.first)
                    if (lit.length > hint.length) hint = lit
                    sb.append(Regex.escape(lit))
                    if (m.groupValues[1].isNotEmpty()) { sb.append("(\\d+(?:[.,]\\d+)?)"); slots.add('n' to m.groupValues[1].toInt()) }
                    else { sb.append("(.+?)"); slots.add('s' to m.groupValues[2].toInt()) }
                    last = m.range.last + 1
                }
                val tail = key.substring(last)
                if (tail.length > hint.length) hint = tail
                sb.append(Regex.escape(tail)).append("$")
                templates.add(Tpl(Regex(sb.toString(), RegexOption.DOT_MATCHES_ALL), hint, slots, out))
            } else exact[key] = out
        }
    }

    fun tr(s: String, lang: AppLang): String {
        if (lang == AppLang.IT || s.isEmpty()) return s
        val i = lang.idx
        exact[s]?.let { return it[i] }
        val lead = s.takeWhile { it.isWhitespace() }
        val trail = s.takeLastWhile { it.isWhitespace() }
        if (lead.length == s.length) return s
        if (lead.isNotEmpty() || trail.isNotEmpty()) return lead + tr(s.trim(), lang) + trail
        if (s.indexOf('\n') >= 0) return s.split("\n").joinToString("\n") { tr(it, lang) }
        for (t in templates) {
            if (t.hint.length >= 2 && !s.contains(t.hint)) continue
            val m = t.regex.matchEntire(s) ?: continue
            var res = t.out[i]
            t.slots.forEachIndexed { gi, (kind, id) ->
                val v = m.groupValues[gi + 1]
                res = res.replace(if (kind == 'n') "{$id}" else "<$id>", if (kind == 'n') v else tr(v, lang))
            }
            return res
        }
        return s
    }
}

/** Sostituisce il Text di Material: traduce automaticamente i testi nella lingua scelta. */
@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = LocalTextStyle.current,
) {
    androidx.compose.material3.Text(
        text = I18n.tr(text, LocalLang.current), modifier = modifier, color = color, fontSize = fontSize, fontStyle = fontStyle,
        fontWeight = fontWeight, fontFamily = fontFamily, letterSpacing = letterSpacing, textDecoration = textDecoration,
        textAlign = textAlign, lineHeight = lineHeight, overflow = overflow, softWrap = softWrap, maxLines = maxLines,
        minLines = minLines, onTextLayout = onTextLayout, style = style,
    )
}

@Composable
fun Text(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    inlineContent: Map<String, InlineTextContent> = mapOf(),
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current,
) {
    androidx.compose.material3.Text(
        text = text, modifier = modifier, color = color, fontSize = fontSize, fontStyle = fontStyle,
        fontWeight = fontWeight, fontFamily = fontFamily, letterSpacing = letterSpacing, textDecoration = textDecoration,
        textAlign = textAlign, lineHeight = lineHeight, overflow = overflow, softWrap = softWrap, maxLines = maxLines,
        minLines = minLines, inlineContent = inlineContent, onTextLayout = onTextLayout, style = style,
    )
}

/** Sostituisce l'Icon di Material: traduce anche le descrizioni per l'accessibilita' (TalkBack). */
@Composable
fun Icon(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = androidx.compose.material3.LocalContentColor.current,
) {
    androidx.compose.material3.Icon(
        imageVector = imageVector, contentDescription = contentDescription?.let { I18n.tr(it, LocalLang.current) },
        modifier = modifier, tint = tint,
    )
}

@Composable
fun Icon(
    painter: androidx.compose.ui.graphics.painter.Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = androidx.compose.material3.LocalContentColor.current,
) {
    androidx.compose.material3.Icon(
        painter = painter, contentDescription = contentDescription?.let { I18n.tr(it, LocalLang.current) },
        modifier = modifier, tint = tint,
    )
}
