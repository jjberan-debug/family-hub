package com.beranfamily.hub.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object Palette {
    val Background = Color(0xFFF7F4EE)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceMuted = Color(0xFFEFEBE3)
    val Ink = Color(0xFF26282B)
    val InkSoft = Color(0xFF6B6F76)
    val Line = Color(0xFFE2DDD3)
    val Accent = Color(0xFF2F5D62)
    val AccentSoft = Color(0xFFDCE8E6)
    val Warm = Color(0xFFE07A5F)
}

private val colours = lightColorScheme(
    primary = Palette.Accent,
    onPrimary = Color.White,
    primaryContainer = Palette.AccentSoft,
    onPrimaryContainer = Palette.Accent,
    secondary = Palette.Warm,
    background = Palette.Background,
    onBackground = Palette.Ink,
    surface = Palette.Surface,
    onSurface = Palette.Ink,
    surfaceVariant = Palette.SurfaceMuted,
    onSurfaceVariant = Palette.InkSoft,
    outline = Palette.Line,
    outlineVariant = Palette.Line
)

private val base = Typography()
private val type = Typography(
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Light, fontSize = 44.sp),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.copy(fontSize = 17.sp),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp)
)

@Composable
fun FamilyTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colours, typography = type, content = content)
}
