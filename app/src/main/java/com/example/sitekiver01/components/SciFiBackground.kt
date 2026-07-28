package com.example.sitekiver01.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.sitekiver01.ui.theme.ForgeAmber
import com.example.sitekiver01.ui.theme.ForgeBase
import com.example.sitekiver01.ui.theme.ForgeBaseRaised
import com.example.sitekiver01.ui.theme.ForgeBlue
import com.example.sitekiver01.ui.theme.ForgePrimary
import kotlin.math.PI
import kotlin.math.sin

/**
 * Ambient background baru: industrial, tenang, dan ringan.
 * Semua animasi digambar pada satu Canvas sehingga tidak menambah banyak node UI.
 */
@Composable
fun SciFiBackground(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val background = colors.background
    val raised = colors.surfaceVariant
    val primary = colors.primary
    val secondary = colors.secondary
    val tertiary = colors.tertiary
    val transition = rememberInfiniteTransition(label = "forgeAmbient")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ambientDrift"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.38f,
        targetValue = 0.72f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambientPulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(raised.copy(alpha = 0.72f), background, background)
                )
            )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val travel = drift * (size.width + 420f) - 210f
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(primary.copy(alpha = 0.10f * pulse), Color.Transparent),
                    center = Offset(travel, size.height * 0.20f),
                    radius = size.minDimension * 0.62f
                ),
                radius = size.minDimension * 0.62f,
                center = Offset(travel, size.height * 0.20f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(secondary.copy(alpha = 0.065f), Color.Transparent),
                    center = Offset(size.width - travel * 0.45f, size.height * 0.72f),
                    radius = size.minDimension * 0.52f
                ),
                radius = size.minDimension * 0.52f,
                center = Offset(size.width - travel * 0.45f, size.height * 0.72f)
            )

            val phase = drift * 2f * PI.toFloat()
            val ribbonOffset = (drift * 260f) - 130f
            rotate(degrees = -11f, pivot = center) {
                repeat(3) { index ->
                    val y = size.height * (0.22f + index * 0.31f) + ribbonOffset
                    drawRect(
                        color = when (index) {
                            0 -> primary.copy(alpha = 0.028f)
                            1 -> secondary.copy(alpha = 0.022f)
                            else -> tertiary.copy(alpha = 0.018f)
                        },
                        topLeft = Offset(-size.width * 0.25f, y),
                        size = androidx.compose.ui.geometry.Size(size.width * 1.5f, 42f + index * 13f)
                    )
                }
            }

            repeat(9) { index ->
                val orbit = phase + index * 0.72f
                val centerX = size.width * (0.5f + 0.46f * sin(orbit))
                val centerY = size.height * ((index + 1) / 10f)
                val dot = 1.5f + (index % 3)
                drawCircle(
                    color = if (index % 2 == 0) primary.copy(alpha = 0.14f) else secondary.copy(alpha = 0.10f),
                    radius = dot,
                    center = Offset(centerX, centerY)
                )
            }

            val ruleY = size.height * (0.14f + drift * 0.72f)
            drawLine(
                color = primary.copy(alpha = 0.08f),
                start = Offset(size.width * 0.08f, ruleY),
                end = Offset(size.width * 0.23f, ruleY),
                strokeWidth = 2f
            )
            repeat(4) { index ->
                val markerX = size.width * 0.08f + index * 14f
                drawLine(
                    color = primary.copy(alpha = 0.12f),
                    start = Offset(markerX, ruleY - 5f),
                    end = Offset(markerX, ruleY + 5f),
                    strokeWidth = 1f
                )
            }
        }
    }
}
