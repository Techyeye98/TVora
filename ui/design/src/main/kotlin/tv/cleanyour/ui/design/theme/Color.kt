package tv.cleanyour.ui.design.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// DESIGN-SYSTEM 2 and 11. Dark is the default and the brand; light mirrors the roles one to one.
@Immutable
data class CytColors(
    val bg: Color, val surface1: Color, val surface2: Color, val surface3: Color,
    val rim: Color, val outline: Color, val outlineStrong: Color,
    val textPrimary: Color, val textSecondary: Color, val textTertiary: Color,
    val primary: Color, val onPrimary: Color, val primaryPressed: Color, val accent: Color,
    val glowBrand: Color, val glowTv: Color,
    val success: Color, val successContainer: Color, val onSuccessContainer: Color,
    val warning: Color, val warningContainer: Color, val onWarningContainer: Color,
    val danger: Color, val dangerFill: Color, val onDanger: Color,
    val dangerContainer: Color, val onDangerContainer: Color,
    val scrim: Color,
    // Glass recipe (DESIGN-SYSTEM 5.1): fill top and bottom, specular top, rim.
    val glassFillTop: Color, val glassFillBottom: Color, val glassSpecular: Color,
    // The springy highlight: brighter fill, an 80% rim and three outer rings.
    val highlightFillTop: Color, val highlightFillBottom: Color, val highlightSpecular: Color,
    val highlightRim: Color, val highlightRing: Color,
    // Ambient glow core alpha (24% dark, 14% light) and its mid stop.
    val glowCoreAlpha: Float, val glowMidAlpha: Float,
    val isDark: Boolean,
)

val DarkCytColors = CytColors(
    bg = Color(0xFF0A1116), surface1 = Color(0xFF111D25), surface2 = Color(0xFF172731),
    surface3 = Color(0xFF223540), rim = Color(0xFF39525F), outline = Color(0xFF263C48),
    outlineStrong = Color(0xFF67838F), textPrimary = Color(0xFFF0F7F5),
    textSecondary = Color(0xFFB2C3C9), textTertiary = Color(0xFF91A8B1),
    primary = Color(0xFF8CE8CA), onPrimary = Color(0xFF09241B), primaryPressed = Color(0xFF60CCAB),
    accent = Color(0xFFBBC6FF),
    glowBrand = Color(0xFF8CE8CA), glowTv = Color(0xFF66869A),
    success = Color(0xFF30D158), successContainer = Color(0xFF0D2A16),
    onSuccessContainer = Color(0xFF5CE07E), warning = Color(0xFFFFB340),
    warningContainer = Color(0xFF33240A), onWarningContainer = Color(0xFFFFC46B),
    danger = Color(0xFFFF6961), dangerFill = Color(0xFFFF453A), onDanger = Color(0xFF0A1116),
    dangerContainer = Color(0xFF3A1512), onDangerContainer = Color(0xFFFF8A80),
    scrim = Color(0xA3000000),
    glassFillTop = Color(0xFF172731), glassFillBottom = Color(0xFF172731),
    glassSpecular = Color.Transparent,
    highlightFillTop = Color(0xFF223D3F), highlightFillBottom = Color(0xFF223D3F),
    highlightSpecular = Color.Transparent, highlightRim = Color.White.copy(alpha = 0.80f),
    highlightRing = Color.Transparent,
    glowCoreAlpha = 0.06f, glowMidAlpha = 0.02f,
    isDark = true,
)

val LightCytColors = CytColors(
    bg = Color(0xFFEDF3EF), surface1 = Color(0xFFFFFFFF), surface2 = Color(0xFFF4F8F5),
    surface3 = Color(0xFFDFEAE5), rim = Color(0xFFCADBD2), outline = Color(0xFFD4E1DA),
    outlineStrong = Color(0xFF749488), textPrimary = Color(0xFF0C231D),
    textSecondary = Color(0xFF35564B), textTertiary = Color(0xFF4C6B61),
    primary = Color(0xFF075E4C), onPrimary = Color(0xFFFFFFFF), primaryPressed = Color(0xFF2A2A2E),
    accent = Color(0xFF36579B),
    glowBrand = Color(0xFF075E4C), glowTv = Color(0xFF66869A),
    success = Color(0xFF157030), successContainer = Color(0xFFE3F4E8),
    onSuccessContainer = Color(0xFF0F5A26), warning = Color(0xFF8F5300),
    warningContainer = Color(0xFFFFF1DB), onWarningContainer = Color(0xFF7A4600),
    danger = Color(0xFFC01B2C), dangerFill = Color(0xFFC01B2C), onDanger = Color(0xFFFFFFFF),
    dangerContainer = Color(0xFFFDE7E9), onDangerContainer = Color(0xFFA3121F),
    scrim = Color(0x66000000),
    glassFillTop = Color.White, glassFillBottom = Color.White,
    glassSpecular = Color.Transparent,
    highlightFillTop = Color(0xFFD7F4E8), highlightFillBottom = Color(0xFFD7F4E8),
    highlightSpecular = Color.Transparent, highlightRim = Color.Black.copy(alpha = 0.55f),
    highlightRing = Color.Transparent,
    glowCoreAlpha = 0.14f, glowMidAlpha = 0.06f,
    isDark = false,
)

// Stock Material 3 components inherit the brand without wrappers (DESIGN-SYSTEM 11).
fun CytColors.toM3(): ColorScheme = (if (isDark) darkColorScheme() else lightColorScheme()).copy(
    primary = primary, onPrimary = onPrimary, background = bg, onBackground = textPrimary,
    surface = bg, onSurface = textPrimary, onSurfaceVariant = textSecondary,
    surfaceContainerLowest = bg, surfaceContainerLow = surface1, surfaceContainer = surface2,
    surfaceContainerHigh = surface3, surfaceContainerHighest = surface3,
    surfaceVariant = surface3, inverseSurface = surface3, inverseOnSurface = textPrimary,
    outline = outlineStrong, outlineVariant = outline, error = danger,
    errorContainer = dangerContainer, onErrorContainer = onDangerContainer,
    secondary = accent, onSecondary = onPrimary, tertiary = accent, scrim = scrim,
    primaryContainer = surface3, onPrimaryContainer = textPrimary,
    secondaryContainer = surface3, onSecondaryContainer = textPrimary,
)
