package com.notium.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.notium.app.data.AnimStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.notium.app.data.EditorFont
import com.notium.app.data.Settings
import com.notium.app.data.ThemeMode

/** Colori d'accento selezionabili. 0 = monocromatico (bianco su scuro, nero su chiaro). */
val AccentChoices: List<Pair<String, Int>> = listOf(
    "Monocromatico" to 0,
    "Grigio" to 0xFF9E9E9E.toInt(),
    "Azzurro" to 0xFF7FA6D9.toInt(),
    "Verde" to 0xFF7FC8A9.toInt(),
    "Rosso" to 0xFFD98C7F.toInt(),
    "Ambra" to 0xFFD9B97F.toInt(),
    "Viola" to 0xFFA68FD9.toInt(),
)

/** Impostazioni di animazione disponibili a tutte le schermate. */
data class AnimConfig(
    val enabled: Boolean, val speed: Float, val style: AnimStyle,
    val press: Boolean = true, val list: Boolean = true, val pressScale: Float = 0.97f,
) {
    fun ms(base: Int): Int = if (!enabled) 0 else (base / speed.coerceIn(0.25f, 3f)).toInt()

    /** Animazione fluida con curva di accelerazione/decelerazione naturale. */
    fun <T> smooth(base: Int): androidx.compose.animation.core.TweenSpec<T> =
        androidx.compose.animation.core.tween(ms(base), easing = androidx.compose.animation.core.FastOutSlowInEasing)
}

val LocalAnim = staticCompositionLocalOf { AnimConfig(true, 1f, AnimStyle.SLIDE) }

private fun scheme(dark: Boolean, black: Boolean, accent: Int, bg: Int, highContrast: Boolean): ColorScheme {
    val c = schemeBg(dark, black, accent, bg)
    if (!highContrast) return c
    return if (c.background.luminance() < 0.5f) c.copy(
        onBackground = Color.White, onSurface = Color.White, onSurfaceVariant = Color(0xFFD0D0D4),
        outline = Color(0xFF8A8A90), outlineVariant = Color(0xFF55555A),
    ) else c.copy(
        onBackground = Color.Black, onSurface = Color.Black, onSurfaceVariant = Color(0xFF2E2E33),
        outline = Color(0xFF707076), outlineVariant = Color(0xFF9A9AA0),
    )
}

private fun schemeBg(dark: Boolean, black: Boolean, accent: Int, bg: Int): ColorScheme {
    val base = baseScheme(dark, black, accent)
    if (bg == 0) return base
    val b = Color(bg).copy(alpha = 1f)
    val toward = if (dark) Color.White else Color.Black
    val tinted = base.copy(
        background = b,
        surface = lerp(b, toward, 0.05f),
        surfaceVariant = lerp(b, toward, 0.10f),
    )
    val isLightBg = b.luminance() > 0.5f
    return if (isLightBg == dark) {
        // Sfondo chiaro in tema scuro (o viceversa): inverto i colori del testo per restare leggibile.
        if (isLightBg) tinted.copy(
            onBackground = Color(0xFF111111), onSurface = Color(0xFF111111), onSurfaceVariant = Color(0xFF5E5E63),
            outline = Color(0xFFB4B4B8), outlineVariant = Color(0xFFDADADD),
        ) else tinted.copy(
            onBackground = Color(0xFFEDEDED), onSurface = Color(0xFFEDEDED), onSurfaceVariant = Color(0xFF9B9B9F),
            outline = Color(0xFF4A4A4E), outlineVariant = Color(0xFF2B2B2E),
        )
    } else tinted
}

private fun baseScheme(dark: Boolean, black: Boolean, accent: Int): ColorScheme {
    val mono = accent == 0
    return if (dark) {
        val primary = if (mono) Color(0xFFF2F2F2) else Color(accent)
        darkColorScheme(
            primary = primary, onPrimary = Color(0xFF0A0A0A),
            primaryContainer = Color(0xFF2A2A2A), onPrimaryContainer = Color(0xFFF2F2F2),
            secondary = Color(0xFFB5B5B5), onSecondary = Color(0xFF0A0A0A),
            secondaryContainer = Color(0xFF262626), onSecondaryContainer = Color(0xFFE0E0E0),
            background = if (black) Color(0xFF000000) else Color(0xFF0B0B0C),
            onBackground = Color(0xFFEDEDED),
            surface = if (black) Color(0xFF0A0A0A) else Color(0xFF151516),
            onSurface = Color(0xFFEDEDED),
            surfaceVariant = if (black) Color(0xFF161616) else Color(0xFF1F1F21),
            onSurfaceVariant = Color(0xFF9B9B9F),
            outline = Color(0xFF4A4A4E), outlineVariant = Color(0xFF2B2B2E),
            error = Color(0xFFFF8A80),
        )
    } else {
        val primary = if (mono) Color(0xFF111111) else Color(accent).copy(alpha = 1f)
        lightColorScheme(
            primary = primary, onPrimary = Color.White,
            primaryContainer = Color(0xFFE6E6E6), onPrimaryContainer = Color(0xFF111111),
            secondary = Color(0xFF555555), onSecondary = Color.White,
            secondaryContainer = Color(0xFFE9E9E9), onSecondaryContainer = Color(0xFF222222),
            background = Color(0xFFF4F4F5), onBackground = Color(0xFF111111),
            surface = Color.White, onSurface = Color(0xFF111111),
            surfaceVariant = Color(0xFFEAEAEB), onSurfaceVariant = Color(0xFF5E5E63),
            outline = Color(0xFFB4B4B8), outlineVariant = Color(0xFFDADADD),
            error = Color(0xFFB3261E),
        )
    }
}

private fun Typography.withFamily(f: FontFamily) = Typography(
    displayLarge = displayLarge.copy(fontFamily = f), displayMedium = displayMedium.copy(fontFamily = f),
    displaySmall = displaySmall.copy(fontFamily = f), headlineLarge = headlineLarge.copy(fontFamily = f),
    headlineMedium = headlineMedium.copy(fontFamily = f), headlineSmall = headlineSmall.copy(fontFamily = f),
    titleLarge = titleLarge.copy(fontFamily = f), titleMedium = titleMedium.copy(fontFamily = f),
    titleSmall = titleSmall.copy(fontFamily = f), bodyLarge = bodyLarge.copy(fontFamily = f),
    bodyMedium = bodyMedium.copy(fontFamily = f), bodySmall = bodySmall.copy(fontFamily = f),
    labelLarge = labelLarge.copy(fontFamily = f), labelMedium = labelMedium.copy(fontFamily = f),
    labelSmall = labelSmall.copy(fontFamily = f),
)

fun EditorFont.family(): FontFamily = when (this) {
    EditorFont.SANS -> FontFamily.Default
    EditorFont.SERIF -> FontFamily.Serif
    EditorFont.MONO -> FontFamily.Monospace
}

@Composable
fun NotiumTheme(settings: Settings, content: @Composable () -> Unit) {
    val dark = when (settings.theme) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.BLACK -> true
    }
    val r = settings.cornerRadius
    val black = settings.theme == ThemeMode.BLACK
    // Calcolati una volta sola finche' i valori non cambiano: gli slider restano fluidi.
    val shapes = remember(r) {
        Shapes(
            extraSmall = RoundedCornerShape((r / 3).dp), small = RoundedCornerShape((r / 2).dp),
            medium = RoundedCornerShape(r.dp), large = RoundedCornerShape((r + 6).dp),
            extraLarge = RoundedCornerShape((r + 12).dp),
        )
    }
    val colors = remember(dark, black, settings.accent, settings.bgColor, settings.highContrast) {
        scheme(dark, black, settings.accent, settings.bgColor, settings.highContrast)
    }
    val typography = remember(settings.uiFont) { Typography().withFamily(settings.uiFont.family()) }
    val anim = remember(settings.animations, settings.animSpeed, settings.animStyle, settings.animPress, settings.animList, settings.pressScale) {
        AnimConfig(settings.animations, settings.animSpeed, settings.animStyle, settings.animPress, settings.animList, settings.pressScale)
    }
    val base = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(base.density, base.fontScale * settings.fontScale),
        LocalAnim provides anim,
    ) {
        MaterialTheme(colorScheme = colors, shapes = shapes, typography = typography, content = content)
    }
}
