package com.example.sitekiver01.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.sitekiver01.ui.theme.*
import kotlin.random.Random
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource

/**
 * SciFiBackground Component Global
 * Menampilkan background terintegrasi mesh terrain, grid bergerak, dan partikel melayang.
 */
@Composable
fun SciFiBackground(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SciFiBgDeep)
    ) {
        // ==================== LAYER 1: Background Image (Mesh Wireframe) ====================
        Image(
            painter = painterResource(id = com.example.sitekiver01.R.drawable.background2),
            contentDescription = "Sci-Fi Terrain Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.25f // Pertahankan alpha 0.25f agar grid dan teks di atasnya tetap terlihat kontras
        )

        // Gradient overlay gelap pemersatu warna malam agar kontras dengan teks
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            SciFiBgDeep.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // ==================== LAYER 2 & 3: Animated Grid & Floating Particles ====================
        // Menggunakan transpirasi infinite tunggal untuk menghemat konsumsi memori GPU
        val infiniteTransition = rememberInfiniteTransition(label = "sciFiAmbient")
        val density = LocalDensity.current

        // Konversi ukuran grid 70dp ke Pixel
        val gridSizePx = remember { with(density) { 70.dp.toPx() } }

        // Animasi pergeseran posisi Grid
        val gridOffset by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = gridSizePx,
            animationSpec = infiniteRepeatable(
                animation = tween(50000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "gridFlow"
        )

        // Nilai progress konstan untuk animasi partikel melayang (dari bawah ke atas)
        val particleProgress by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(30000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "particlesFlow"
        )

        // Inisialisasi data 40 partikel acak secara presisi agar posisi tidak berubah saat recomposition
        val particles = remember {
            List(40) {
                ParticleData(
                    startXPercent = Random.nextFloat(),
                    size = Random.nextFloat() * 3.5f + 1.5f,
                    speedFactor = Random.nextFloat() * 0.5f + 0.5f,
                    driftFactor = Random.nextFloat() * 30f - 15f
                )
            }
        }

        // Gambar Grid dan Partikel langsung di Canvas GPU murni (Ringan & Anti-Lag)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    // --- 1. PROSES GAMBAR GRID ---
                    var xPos = gridOffset
                    while (xPos < size.width) {
                        drawLine(
                            color = SciFiGridBlue,
                            start = Offset(xPos, 0f),
                            end = Offset(xPos, size.height),
                            strokeWidth = 1f
                        )
                        xPos += gridSizePx
                    }
                    var yPos = gridOffset
                    while (yPos < size.height) {
                        drawLine(
                            color = SciFiGridBlue,
                            start = Offset(0f, yPos),
                            end = Offset(size.width, yPos),
                            strokeWidth = 1f
                        )
                        yPos += gridSizePx
                    }

                    // --- 2. PROSES GAMBAR PARTIKEL MELAYANG ---
                    particles.forEach { p ->
                        val currentProgress = (particleProgress * p.speedFactor) % 1f
                        val yCoordinate = size.height * currentProgress
                        val xCoordinate = (size.width * p.startXPercent) + (p.driftFactor * (1f - currentProgress))

                        // Logika Fade-in di bawah layar & Fade-out di atas layar
                        val alphaFactor = if (currentProgress < 0.2f) {
                            currentProgress / 0.2f
                        } else if (currentProgress > 0.8f) {
                            (1f - currentProgress) / 0.2f
                        } else {
                            1f
                        }

                        drawCircle(
                            color = SciFiParticleBlu.copy(alpha = alphaFactor * SciFiParticleBlu.alpha),
                            radius = p.size,
                            center = Offset(xCoordinate, yCoordinate)
                        )
                    }
                }
        )
    }
}

/**
 * Data model struktur penyimpanan koordinat partikel acak
 */
data class ParticleData(
    val startXPercent: Float,
    val size: Float,
    val speedFactor: Float,
    val driftFactor: Float
)