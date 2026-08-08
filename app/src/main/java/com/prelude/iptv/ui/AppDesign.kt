package com.prelude.iptv.ui

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.ui.design.MotionSystem

object IptvColors {
    val Background = Color(0xFF080808)
    val BackgroundRaised = Color(0xFF101010)
    val Surface = Color(0xFF181818)
    val SurfaceRaised = Color(0xFF222222)
    val SurfaceSelected = Color(0xFF2A2A2A)
    val SurfacePressed = Color(0xFF303030)
    val Divider = Color(0x24FFFFFF)
    val DividerStrong = Color(0x3DFFFFFF)
    val Primary = Color(0xFFE50914)
    val OnPrimary = Color.White
    val TextPrimary = Color(0xFFF5F5F5)
    val TextSecondary = Color(0xFFB3B3B3)
    val TextTertiary = Color(0xFF7A7A7A)
    val TextMuted = TextTertiary
    val Success = Color(0xFF46D369)
    val Warning = Color(0xFFF5C451)
    /**
     * Γαλάζιο για τιμές που ο χρήστης έχει διαλέξει και μπορεί να ξαναδιαλέξει
     * (π.χ. η κατηγορία ενός rail στην «Επεξεργασία αρχικής»). Ξεχωρίζει από το
     * [Primary], που σημαίνει «προσοχή, κάτι σβήνει».
     */
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

private val IptvTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 34.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.4).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 27.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.25).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 25.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 21.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 23.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
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
