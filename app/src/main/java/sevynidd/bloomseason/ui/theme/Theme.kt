package sevynidd.bloomseason.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import sevynidd.bloomseason.core.Season

/** Akzent, Text darauf, Container und Text auf dem Container, nach Material 3. */
@Immutable
data class SeasonPalette(val accent: Color, val onAccent: Color, val container: Color, val onContainer: Color)

/** Die vier Jahreszeiten als feste Datenfarben (Kalender, Jahreslauf, Statistik). */
@Immutable
data class SeasonColors(
    val winter: SeasonPalette,
    val spring: SeasonPalette,
    val summer: SeasonPalette,
    val autumn: SeasonPalette,
) {
    fun of(season: Season): SeasonPalette = when (season) {
        Season.WINTER -> winter
        Season.SPRING -> spring
        Season.SUMMER -> summer
        Season.AUTUMN -> autumn
    }
}

/** Neutrale Flächen, die nicht in das Material-3-Farbschema passen. */
@Immutable
data class Neutral(val nav: Color, val bar: Color)

private fun palette(accent: Long, onAccent: Long, container: Long, onContainer: Long) =
    SeasonPalette(Color(accent), Color(onAccent), Color(container), Color(onContainer))

private val DarkSeasons = SeasonColors(
    winter = palette(0xFFA9C4E6, 0xFF0E1D30, 0xFF23324A, 0xFFD6E3F7),
    spring = palette(0xFFA6D49B, 0xFF0F2410, 0xFF24392A, 0xFFCDEBC4),
    summer = palette(0xFFE8C677, 0xFF2A1E04, 0xFF3E3119, 0xFFF6E2B4),
    autumn = palette(0xFFEBA484, 0xFF33150A, 0xFF47291C, 0xFFF8D5C5),
)

private val LightSeasons = SeasonColors(
    winter = palette(0xFF3B5F8C, 0xFFFFFFFF, 0xFFD8E4F3, 0xFF142D4A),
    spring = palette(0xFF3D6A3A, 0xFFFFFFFF, 0xFFD6EACD, 0xFF11290F),
    summer = palette(0xFF7A5B0F, 0xFFFFFFFF, 0xFFF4E3B8, 0xFF2E2100),
    autumn = palette(0xFF9B4A28, 0xFFFFFFFF, 0xFFF0DDCC, 0xFF3A1406),
)

private val DarkNeutral = Neutral(nav = Color(0xFF1A1917), bar = Color(0xFF4A453F))
private val LightNeutral = Neutral(nav = Color(0xFFECE6DE), bar = Color(0xFFD3CABF))

val LocalSeasonColors = staticCompositionLocalOf { DarkSeasons }
val LocalNeutral = staticCompositionLocalOf { DarkNeutral }

val seasonColors: SeasonColors
    @Composable @ReadOnlyComposable get() = LocalSeasonColors.current

val neutral: Neutral
    @Composable @ReadOnlyComposable get() = LocalNeutral.current

private fun scheme(dark: Boolean, season: SeasonPalette): ColorScheme {
    val bg = Color(if (dark) 0xFF121110 else 0xFFF5F1EB)
    val card = Color(if (dark) 0xFF1F1D1B else 0xFFFFFFFF)
    val hi = Color(if (dark) 0xFF2C2926 else 0xFFEFE9E1)
    val ink = Color(if (dark) 0xFFECE7E1 else 0xFF1E1B18)
    val muted = Color(if (dark) 0xFFB5ADA4 else 0xFF5C554E)
    val outline = Color(if (dark) 0xFF8C847B else 0xFF7D746B)
    val divider = Color(if (dark) 0xFF34302C else 0xFFE2DBD2)
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = season.accent,
        onPrimary = season.onAccent,
        primaryContainer = season.container,
        onPrimaryContainer = season.onContainer,
        secondary = season.accent,
        onSecondary = season.onAccent,
        secondaryContainer = season.container,
        onSecondaryContainer = season.onContainer,
        background = bg,
        onBackground = ink,
        surface = bg,
        onSurface = ink,
        surfaceVariant = hi,
        onSurfaceVariant = muted,
        surfaceTint = Color.Transparent,
        outline = outline,
        outlineVariant = divider,
        surfaceContainerLowest = bg,
        surfaceContainerLow = card,
        surfaceContainer = card,
        surfaceContainerHigh = hi,
        surfaceContainerHighest = hi,
    )
}

private val BloomTypography = Typography().let { base ->
    base.copy(
        displayMedium = base.displayMedium.copy(
            fontSize = 45.sp, lineHeight = 52.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.8).sp,
        ),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Medium),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Medium),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Medium),
    )
}

/** Zahlen in Tabellenziffern, damit sich Werte beim Wechsel nicht verschieben. */
val Tabular = TextStyle(fontFeatureSettings = "tnum")

@Composable
fun BloomSeasonTheme(
    season: Season,
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val seasons = if (dark) DarkSeasons else LightSeasons
    CompositionLocalProvider(
        LocalSeasonColors provides seasons,
        LocalNeutral provides if (dark) DarkNeutral else LightNeutral,
    ) {
        MaterialTheme(
            colorScheme = scheme(dark, seasons.of(season)),
            typography = BloomTypography,
            content = content,
        )
    }
}
