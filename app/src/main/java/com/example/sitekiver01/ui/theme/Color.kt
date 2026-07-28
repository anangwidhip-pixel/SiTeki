package com.example.sitekiver01.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * SiTeki Forge — industrial operations palette.
 *
 * Nama alias lama tetap disediakan agar seluruh layar lama langsung mengikuti
 * design system baru tanpa mengubah logika bisnisnya.
 */
val ForgeBase = Color(0xFF07110E)
val ForgeBaseRaised = Color(0xFF0B1713)
val ForgeSurface = Color(0xFF111F1A)
val ForgeSurfaceHigh = Color(0xFF172A23)
val ForgeLine = Color(0xFF29443A)
val ForgePrimary = Color(0xFF55DFA6)
val ForgePrimaryDeep = Color(0xFF15966A)
val ForgeBlue = Color(0xFF72C7F4)
val ForgeAmber = Color(0xFFF2B457)
val ForgeRed = Color(0xFFFF6B6B)
val ForgeViolet = Color(0xFFA990F7)
val ForgeText = Color(0xFFF2F8F5)
val ForgeTextMuted = Color(0xFF94AAA1)
val ForgeTextFaint = Color(0xFF60776E)

// SiTeki Atelier — companion palette for light mode.
val AtelierPaper = Color(0xFFF4F1E9)
val AtelierPaperRaised = Color(0xFFFFFDF7)
val AtelierInk = Color(0xFF17201C)
val AtelierInkMuted = Color(0xFF5F6C65)
val AtelierLine = Color(0xFFD6DDD7)
val AtelierPrimary = Color(0xFF006B4F)
val AtelierPrimarySoft = Color(0xFFD4F4E5)
val AtelierBlue = Color(0xFF176785)
val AtelierAmber = Color(0xFF9A5700)

val Purple80 = ForgePrimary
val PurpleGrey80 = ForgeTextMuted
val Pink80 = ForgeAmber
val Purple40 = ForgePrimaryDeep
val PurpleGrey40 = ForgeTextMuted
val Pink40 = ForgeAmber

val RajabesiDarkNavy = ForgeBase
val RajabesiBackgroundGray = ForgeBaseRaised
val RajabesiPrimaryBlue = ForgePrimaryDeep
val RajabesiButtonBlue = ForgePrimary

val GlassBase = ForgeBase
val GlassSurface = ForgeSurface.copy(alpha = 0.94f)
val GlassBorder = ForgeLine.copy(alpha = 0.82f)
val GlassAccentCyan = ForgePrimary
val GlassAccentPurple = ForgeViolet
val GlassAccentGreen = ForgePrimary
val GlassAccentAmber = ForgeAmber
val GlassTextMuted = ForgeTextMuted

val SciFiBgDeep = ForgeBase
val SciFiBrandDark = ForgeBaseRaised
val SciFiBrandCard = ForgeSurface
val SciFiGlass = ForgeSurface.copy(alpha = 0.92f)

val SciFiCyan = ForgePrimary
val SciFiBlue = ForgeBlue
val SciFiPurple = ForgeViolet

val SciFiStatusM = ForgePrimary
val SciFiStatusB = ForgeBlue
val SciFiActual = ForgePrimary
val SciFiHoliday = ForgeRed
val SciFiSaturday = ForgeAmber

val SciFiTextPrimary = ForgeText
val SciFiTextMuted = ForgeTextMuted
val SciFiTextSecondary = ForgeTextFaint

val SciFiGlassLight = ForgeSurfaceHigh.copy(alpha = 0.82f)
val SciFiGlassMedium = ForgeSurfaceHigh.copy(alpha = 0.94f)
val SciFiBorderLight = ForgeLine.copy(alpha = 0.62f)
val SciFiBorderMedium = ForgeLine

val SciFiGridBlue = ForgePrimary.copy(alpha = 0.055f)
val SciFiParticleBlu = ForgePrimary.copy(alpha = 0.34f)
val SciFiGlowCyan = ForgePrimary.copy(alpha = 0.45f)
val SciFiGlowBlue = ForgeBlue.copy(alpha = 0.32f)

val TechCyan = ForgePrimary
val TechBg = ForgeBase
val TechGlass = ForgeSurface
