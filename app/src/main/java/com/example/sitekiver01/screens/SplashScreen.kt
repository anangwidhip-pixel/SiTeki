package com.example.sitekiver01.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sitekiver01.components.SciFiBackground
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    val steps = listOf(
        "Membuka ruang kerja",
        "Menghubungkan layanan data",
        "Menyusun modul operasional",
        "Siap digunakan"
    )
    var progress by remember { mutableFloatStateOf(0f) }
    var reveal by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(progress, tween(260), label = "startupProgress")

    LaunchedEffect(Unit) {
        reveal = true
        repeat(100) { tick ->
            progress = (tick + 1) / 100f
            delay(28)
        }
        delay(260)
        onTimeout()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        SciFiBackground()

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 34.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "PT. PABRIK BESI BETON RAJA BESI",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Text(
                    "01 / START",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black
                )
            }

            AnimatedVisibility(
                visible = reveal,
                enter = fadeIn(tween(700)) + slideInVertically(tween(700)) { 50 }
            ) {
                Column {
                    StartupMark()
                    Spacer(Modifier.height(28.dp))
                    Text(
                        "SiTeki",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 56.sp,
                        lineHeight = 58.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-2).sp
                    )
                    Text(
                        "Engineering workspace",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Satu ruang kerja untuk menjaga mesin,\npekerjaan, dan tim tetap bergerak.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            steps[(progress * (steps.size - 1)).toInt().coerceIn(0, steps.lastIndex)],
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${(animatedProgress * 100).toInt()}%",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    LinearProgressIndicator(
                        progress = animatedProgress,
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Square
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Sinkronisasi aman · konfigurasi lokal",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StartupMark() {
    val transition = rememberInfiniteTransition(label = "startupMark")
    val rotation by transition.animateFloat(
        0f,
        360f,
        infiniteRepeatable(tween(5200, easing = LinearEasing)),
        label = "startupRotation"
    )
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline

    Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawArc(
                color = outline,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(1.dp.toPx())
            )
            drawArc(
                color = primary,
                startAngle = rotation,
                sweepAngle = 112f,
                useCenter = false,
                style = Stroke(5.dp.toPx(), cap = StrokeCap.Square)
            )
            drawLine(
                color = primary,
                start = Offset(size.width * 0.28f, size.height * 0.72f),
                end = Offset(size.width * 0.72f, size.height * 0.28f),
                strokeWidth = 3.dp.toPx()
            )
        }
        Box(Modifier.size(12.dp).background(primary, CircleShape))
    }
}
