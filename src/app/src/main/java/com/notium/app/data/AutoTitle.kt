package com.notium.app.data

/** Titolo automatico: se la nota non ha titolo, si usa la prima frase del testo. */
object AutoTitle {
    private val MARK = Regex("^(?:\\s*(?:#{1,6}\\s+|>\\s?|[-*+]\\s+\\[[ xX]\\]\\s+|[-*+]\\s+|\\d+\\.\\s+))+")
    private val INLINE = Regex("\\*\\*|~~|==|`")
    private val LINK = Regex("\\[([^\\]]*)\\]\\([^)]*\\)")
    private val ESCAPED = Regex("\\\\([*_`#>~=\\-.!\\[\\]])")
    private val SENTENCE = Regex("^(.+?[.!?…])(?:\\s|$)")

    fun from(content: String, max: Int = 60): String {
        for (raw in content.lineSequence()) {
            var l = raw.trim()
            if (l.isEmpty() || l.startsWith("```") || l == "---") continue
            l = MARK.replace(l, "")
            l = LINK.replace(l, "$1")
            l = INLINE.replace(l, "")
            l = ESCAPED.replace(l, "$1").trim()
            if (l.isEmpty()) continue
            var s = (SENTENCE.find(l)?.groupValues?.get(1) ?: l).trim().trimEnd('.').trim()
            if (s.isEmpty()) continue
            if (s.length > max) {
                val cut = s.lastIndexOf(' ', max)
                s = s.substring(0, if (cut > max / 2) cut else max).trimEnd() + "…"
            }
            return s
        }
        return ""
    }
}
