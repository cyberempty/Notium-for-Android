package com.notium.app.security

enum class Strength(val label: String, val fraction: Float) {
    EMPTY("", 0f), WEAK("Debole", 0.25f), FAIR("Discreta", 0.5f), GOOD("Buona", 0.75f), STRONG("Ottima", 1f)
}

object PasswordStrength {
    const val MIN_LENGTH = 10

    private val common = listOf(
        "password", "123456", "qwerty", "letmein", "admin", "iloveyou", "notium", "notely", "abc123", "111111",
        "000000", "azerty", "welcome", "ciaociao", "password1"
    )

    fun evaluate(pw: CharSequence): Strength {
        if (pw.isEmpty()) return Strength.EMPTY
        val s = pw.toString()
        val lower = s.lowercase()
        if (common.any { lower.contains(it) } && s.length < 16) return Strength.WEAK
        if (s.toSet().size <= 3) return Strength.WEAK
        var classes = 0
        if (s.any { it.isLowerCase() }) classes++
        if (s.any { it.isUpperCase() }) classes++
        if (s.any { it.isDigit() }) classes++
        if (s.any { !it.isLetterOrDigit() }) classes++
        var score = 0
        score += when {
            s.length >= 16 -> 3
            s.length >= 12 -> 2
            s.length >= MIN_LENGTH -> 1
            else -> 0
        }
        score += if (classes >= 3) 2 else if (classes == 2) 1 else 0
        return when {
            s.length < 8 -> Strength.WEAK
            score >= 5 -> Strength.STRONG
            score >= 4 -> Strength.GOOD
            score >= 2 -> Strength.FAIR
            else -> Strength.WEAK
        }
    }

    /** Messaggio d'errore se la password non e' accettabile, altrimenti null. */
    fun problem(pw: CharSequence): String? = when {
        pw.length < MIN_LENGTH -> "Almeno $MIN_LENGTH caratteri."
        evaluate(pw) == Strength.WEAK -> "Password troppo debole: mescola lettere, numeri e simboli."
        else -> null
    }
}
