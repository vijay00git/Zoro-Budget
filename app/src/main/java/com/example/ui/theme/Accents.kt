package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

/** True while the app is drawing its dark colour scheme (set by SmartBudgetTheme). */
val LocalIsDarkTheme = staticCompositionLocalOf { false }

/**
 * The colours one account, category or savings section uses everywhere:
 * - [fill]: a soft tint for tiles and icon circles;
 * - [ink]: a strong shade for icons and text on [fill] (at least 4.5:1 contrast);
 * - [solid]: a full colour for badges with white text on them.
 */
data class Accent(val fill: Color, val ink: Color, val solid: Color)

/** The colours offered in the category and account colour pickers. */
val AccentPalette = listOf(
    "#1E40AF", "#3730A3", "#5B21B6", "#9D174D", "#991B1B", "#9A3412",
    "#854D0E", "#166534", "#115E59", "#155E75", "#334155", "#7C2D12"
)

private val LightTile = Color(0xFFFFFFFF)
private val DarkTile = Color(0xFF1A1C21)

fun parseHexColor(hex: String?, fallback: Color = Color(0xFF334155)): Color =
    try {
        if (hex.isNullOrBlank()) fallback else Color(android.graphics.Color.parseColor(hex))
    } catch (e: IllegalArgumentException) {
        fallback
    }

private fun contrast(a: Color, b: Color): Float {
    val la = a.luminance() + 0.05f
    val lb = b.luminance() + 0.05f
    return if (la > lb) la / lb else lb / la
}

/** Moves [color] toward [target] in small steps until it reaches 4.5:1 contrast against [against]. */
private fun untilReadable(color: Color, against: Color, target: Color): Color {
    var c = color
    var step = 0
    while (contrast(c, against) < 4.5f && step < 20) {
        c = lerp(c, target, 0.1f)
        step++
    }
    return c
}

/** Works out the tint and readable shades for any stored colour, in light or dark mode. */
fun accentFor(base: Color, dark: Boolean): Accent {
    return if (dark) {
        val fill = lerp(DarkTile, base, 0.30f)
        Accent(fill = fill, ink = untilReadable(lerp(base, Color.White, 0.35f), fill, Color.White), solid = untilReadable(base, Color.White, Color.Black))
    } else {
        val fill = lerp(LightTile, base, 0.14f)
        Accent(fill = fill, ink = untilReadable(base, fill, Color.Black), solid = untilReadable(base, Color.White, Color.Black))
    }
}

@Composable
@ReadOnlyComposable
fun accentOf(hex: String?): Accent = accentFor(parseHexColor(hex), LocalIsDarkTheme.current)

@Composable
@ReadOnlyComposable
fun accentOf(color: Color): Accent = accentFor(color, LocalIsDarkTheme.current)
