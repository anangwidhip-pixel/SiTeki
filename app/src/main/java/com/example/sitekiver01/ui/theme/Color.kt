package com.example.sitekiver01.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

val RajabesiDarkNavy = Color(0xFF001E50)
val RajabesiBackgroundGray = Color(0xFFF5F5F5)
val RajabesiPrimaryBlue = Color(0xFF005FC3)
val RajabesiButtonBlue = Color(0xFF2196F3)

// ==================== GLASS THEME COLORS (ORIGINAL) ====================
val GlassBase = Color(0xFF08080A)
val GlassSurface = Color(0xFFFFFFFF).copy(alpha = 0.05f)
val GlassBorder = Color(0xFFFFFFFF).copy(alpha = 0.12f)
val GlassAccentCyan = Color(0xFF00E5FF)
val GlassAccentPurple = Color(0xFF9D50BB)
val GlassAccentGreen = Color(0xFF039BE5)
val GlassAccentAmber = Color(0xFFFF9F0A)
val GlassTextMuted = Color(0xFFA0A0AB)

// ==================== SCIFI THEME COLORS (NEW - From HTML) ====================
// Background & Base
val SciFiBgDeep = Color(0xFF020617)           // Very deep navy/black
val SciFiBrandDark = Color(0xFF050B14)        // Brand dark navy
val SciFiBrandCard = Color(0xFF0F172A)        // Card/surface background
val SciFiGlass = Color(0xFF0F172A).copy(alpha = 0.15f)

// Primary Accents
val SciFiCyan = Color(0xFF06B6D4)             // Neon cyan (primary)
val SciFiBlue = Color(0xFF3B82F6)             // Bright blue
val SciFiPurple = Color(0xFF9D50BB)           // Neon purple (secondary)

// Status Colors (for calendar/table)
val SciFiStatusM = Color(0xFF10B981)          // Green - Plan Mingguan
val SciFiStatusB = Color(0xFF1E40AF)          // Dark Blue - Plan Bulanan
val SciFiActual = Color(0xFF06B6D4)           // Cyan - Actual/Done
val SciFiHoliday = Color(0xFFC23B22)          // Red - Holiday
val SciFiSaturday = Color(0xFFD97706)         // Amber - Saturday

// Text Colors
val SciFiTextPrimary = Color(0xFFF8FAFC)      // Off-white
val SciFiTextMuted = Color(0xFF94A3B8)        // Muted gray
val SciFiTextSecondary = Color(0xFFA0A0AB)    // Secondary muted

// Glassmorphism Layers
val SciFiGlassLight = Color(0xFFFFFFFF).copy(alpha = 0.08f)    // Lighter glass
val SciFiGlassMedium = Color(0xFFFFFFFF).copy(alpha = 0.12f)   // Medium glass
val SciFiBorderLight = Color(0xFFFFFFFF).copy(alpha = 0.08f)   // Light border
val SciFiBorderMedium = Color(0xFFFFFFFF).copy(alpha = 0.12f)  // Medium border

// Grid & Effects
val SciFiGridBlue = Color(0xFF3B82F6).copy(alpha = 0.15f)      // Grid line color
val SciFiParticleBlu = Color(0xFF3B82F6).copy(alpha = 0.7f)    // Particle color

// Glow Effects (for text shadows)
val SciFiGlowCyan = Color(0xFF06B6D4).copy(alpha = 0.8f)       // Cyan glow
val SciFiGlowBlue = Color(0xFF3B82F6).copy(alpha = 0.6f)       // Blue glow

// ==================== ALIASES FOR CONSISTENCY ====================
// Gunakan nama-nama baru tapi yang kompatibel dengan kode existing
val TechCyan = SciFiCyan                       // For backward compatibility
val TechBg = SciFiBgDeep                       // For backward compatibility
val TechGlass = SciFiBrandCard                 // For backward compatibility