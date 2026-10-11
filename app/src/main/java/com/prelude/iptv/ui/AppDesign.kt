package com.prelude.iptv.ui

import com.prelude.iptv.R

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.ui.design.MotionSystem

/**
 * Παλέτα «Horizon» (prototypes/redesign_horizon.html, εγκεκριμένη 2026-10-12).
 * Ζεστό σχεδόν-μαύρο αντί για καθαρό μαύρο, τρία επίπεδα επιφάνειας, και ΕΝΑ
 * accent: το κόκκινο της γραμμής-ορίζοντα του logo (βλ. ui/design/Horizon.kt).
 */
object IptvColors {
    val Background = Color(0xFF0B0A0D)
    val BackgroundRaised = Color(0xFF100E12)
    val Surface = Color(0xFF141217)
    val SurfaceRaised = Color(0xFF1C1A20)
    val SurfaceSelected = Color(0xFF27242B)
    val SurfacePressed = Color(0xFF2E2A33)
    val Divider = Color(0x1FFFECEC)
    val DividerStrong = Color(0x33FFECEC)
    val Primary = Color(0xFFE5303A)
    /** Η λάμψη γύρω από τη γραμμή-ορίζοντα και τις μπάρες προόδου. */
    val Glow = Color(0xFFFF5A5A)
    val OnPrimary = Color.White
    val TextPrimary = Color(0xFFF4EFEF)
    val TextSecondary = Color(0xFFABA3A9)
    val TextTertiary = Color(0xFF7E777D)
    val TextMuted = TextTertiary
    val Success = Color(0xFF4FD18B)
    val Warning = Color(0xFFF2C14E)
    val Info = Color(0xFF4AA8FF)
    val Error = Color(0xFFFF6B6B)
    val Focus = Color.White
    val Scrim = Color.Black
}

@Immutable
data class AppSpacing(
    val screenHorizontal: Dp,
    val screenVertical: Dp,
    val sectionGap: Dp,
    val itemGap: Dp,
    val compactGap: Dp,
    val cardRadius: Dp,
    val panelRadius: Dp,
    val buttonRadius: Dp,
    val minTouchTarget: Dp
)

val LocalAppSpacing = staticCompositionLocalOf {
    AppSpacing(
        screenHorizontal = 16.dp,
        screenVertical = 12.dp,
        sectionGap = 24.dp,
        itemGap = 12.dp,
        compactGap = 8.dp,
        cardRadius = StreamingRadius.Card,
        panelRadius = StreamingRadius.Panel,
        buttonRadius = StreamingRadius.Button,
        minTouchTarget = 48.dp
    )
}

object AppDimens {
    val MobileHorizontal = 16.dp
    val TvHorizontal = 32.dp
    val MobileVertical = 12.dp
    val TvVertical = 20.dp
    val CardRadius = 9.dp
    val ButtonRadius = 7.dp
    val PanelRadius = 14.dp
    val FocusBorder = 2.dp
}

/**
 * Γραμματοσειρές Horizon, και οι δύο με πλήρη ελληνικά (OFL, βλ. docs/licenses/fonts).
 * Body: Commissioner για κείμενο, κουμπιά, ετικέτες.
 * Display: Advent Pro, γεωμετρική όπως το λογότυπο, μόνο για τίτλους.
 */
object AppFonts {
    val Body = FontFamily(
        Font(R.font.commissioner_regular, FontWeight.Normal),
        Font(R.font.commissioner_medium, FontWeight.Medium),
        Font(R.font.commissioner_semibold, FontWeight.SemiBold),
        Font(R.font.commissioner_bold, FontWeight.Bold),
    )
    val Display = FontFamily(
        Font(R.font.advent_pro_medium, FontWeight.Medium),
        Font(R.font.advent_pro_semibold, FontWeight.SemiBold),
        Font(R.font.advent_pro_bold, FontWeight.Bold),
    )
}

private val BaseTypography = Typography()

private val IptvTypography = Typography(
    displayLarge = BaseTypography.displayLarge.copy(fontFamily = AppFonts.Display),
    displayMedium = BaseTypography.displayMedium.copy(fontFamily = AppFonts.Display),
    headlineLarge = BaseTypography.headlineLarge.copy(fontFamily = AppFonts.Display),
    headlineSmall = BaseTypography.headlineSmall.copy(fontFamily = AppFonts.Display),
    titleSmall = BaseTypography.titleSmall.copy(fontFamily = AppFonts.Body),
    labelMedium = BaseTypography.labelMedium.copy(fontFamily = AppFonts.Body),
    labelSmall = BaseTypography.labelSmall.copy(fontFamily = AppFonts.Body),
    displaySmall = TextStyle(
        fontFamily = AppFonts.Display,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 34.sp,
        lineHeight = 38.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = AppFonts.Display,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 27.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = AppFonts.Body,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 25.sp
    ),
    titleMedium = TextStyle(
        fontFamily = AppFonts.Body,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 21.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = AppFonts.Body,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 23.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = AppFonts.Body,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = AppFonts.Body,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp
    ),
    labelLarge = TextStyle(
        fontFamily = AppFonts.Body,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 18.sp
    )
)

private val AppColorScheme = darkColorScheme(
    primary = IptvColors.Primary,
    onPrimary = IptvColors.OnPrimary,
    background = IptvColors.Background,
    onBackground = IptvColors.TextPrimary,
    surface = IptvColors.Surface,
    onSurface = IptvColors.TextPrimary,
    surfaceVariant = IptvColors.SurfaceRaised,
    onSurfaceVariant = IptvColors.TextSecondary,
    outline = IptvColors.DividerStrong,
    error = IptvColors.Error
)

@Composable
fun IptvTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalAppSpacing provides LocalAppSpacing.current.copy(
            cardRadius = StreamingRadius.Card,
            panelRadius = StreamingRadius.Panel,
            buttonRadius = StreamingRadius.Button
        )
    ) {
        MotionSystem {
            MaterialTheme(
                colorScheme = AppColorScheme,
                typography = IptvTypography,
                content = content
            )
        }
    }
}

val NeutralBorder = BorderStroke(1.dp, IptvColors.Divider)
