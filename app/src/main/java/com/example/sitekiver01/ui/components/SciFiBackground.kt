package com.example.sitekiver01.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import com.example.sitekiver01.ui.theme.GlassBase

@Composable
fun SciFiBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GlassBase)
    ) {
        // ==================== GRID PATTERN ====================
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridSize = 50.dp.toPx()
            val lineColor = Color(0xFF00E5FF).copy(alpha = 0.1f)
            
            // Vertical lines
            var x = 0f
            while (x < size.width) {
                drawLine(
                    color = lineColor,
                    start = androidx.compose.ui.geometry.Offset(x, 0f),
                    end = androidx.compose.ui.geometry.Offset(x, size.height),
                    strokeWidth = 1f
                )
                x += gridSize
            }
            
            // Horizontal lines
            var y = 0f
            while (y < size.height) {
                drawLine(
                    color = lineColor,
                    start = androidx.compose.ui.geometry.Offset(0f, y),
                    end = androidx.compose.ui.geometry.Offset(size.width, y),
                    strokeWidth = 1f
                )
                y += gridSize
            }
        }

        // ==================== FLOATING PARTICLES ====================
        val infiniteTransition = rememberInfiniteTransition(label = "particles")
        
        // Particle 1
        val particle1X by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 100f,
            animationSpec = infiniteRepeatable(
                animation = tween(8000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "particle1X"
        )
        
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = particle1X.dp - 150.dp, y = 50.dp)
                .background(
                    Color(0xFF9D50BB).copy(alpha = 0.15f),
                    shape = androidx.compose.foundation.shape.CircleShape
                )
                .blur(80.dp)
        )

        // Particle 2
        val particle2Y by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 150f,
            animationSpec = infiniteRepeatable(
                animation = tween(10000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "particle2Y"
        )
        
        Box(
            modifier = Modifier
                .size(250.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 50.dp, y = -particle2Y.dp + 75.dp)
                .background(
                    Color(0xFF00E5FF).copy(alpha = 0.12f),
                    shape = androidx.compose.foundation.shape.CircleShape
                )
                .blur(100.dp)
        )

        // Particle 3
        val particle3X by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 80f,
            animationSpec = infiniteRepeatable(
                animation = tween(7000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "particle3X"
        )
        
        Box(
            modifier = Modifier
                .size(200.dp)
                .offset(x = -particle3X.dp + 40.dp, y = 200.dp)
                .background(
                    Color(0xFF039BE5).copy(alpha = 0.1f),
                    shape = androidx.compose.foundation.shape.CircleShape
                )
                .blur(90.dp)
        )

        // ==================== GRADIENT OVERLAY ====================
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.3f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.4f)
                            )
                        )
                    )
            )
        }
    }
}

// ==================== CANVAS IMPORT FIX ====================
import androidx.compose.foundation.Canvas
