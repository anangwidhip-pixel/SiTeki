package com.example.sitekiver01.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sitekiver01.OrbitronFontFamily
import com.example.sitekiver01.components.SciFiBackground
import com.example.sitekiver01.ui.theme.GlassBase
import com.example.sitekiver01.ui.theme.SciFiCyan
import com.example.sitekiver01.ui.theme.SciFiTextMuted
import kotlinx.coroutines.delay

/**
 * Splash/loading modern SiTeki.
 *
 * Loading ini menampilkan simulasi proses pembacaan database.
 * Durasi dapat diubah melalui nilai totalDuration.
 */
@Composable
fun SplashScreen(
    onTimeout: () -> Unit
) {
    val loadingSteps = remember {
        listOf(
            "Menghubungkan ke server",
            "Memverifikasi sesi pengguna",
            "Membaca database aplikasi",
            "Menyinkronkan data terbaru",
            "Menyiapkan dashboard"
        )
    }

    var progress by remember { mutableFloatStateOf(0f) }
    var currentStep by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        // Total loading sekitar 3,6 detik.
        val totalDuration = 3_600L
        val interval = 36L
        val totalTick = (totalDuration / interval).toInt()

        for (tick in 0..totalTick) {
            progress = tick.toFloat() / totalTick.toFloat()

            currentStep = when {
                progress < 0.18f -> 0
                progress < 0.38f -> 1
                progress < 0.63f -> 2
                progress < 0.84f -> 3
                else -> 4
            }

            delay(interval)
        }

        progress = 1f
        currentStep = loadingSteps.lastIndex

        delay(300)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        GlassBase,
                        Color(0xFF071419),
                        Color(0xFF020708)
                    )
                )
            )
    ) {
        SciFiBackground()

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DatabaseLoadingIcon()

            Spacer(modifier = Modifier.height(26.dp))

            Text(
                text = "SITEKI",
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                color = SciFiCyan,
                fontFamily = OrbitronFontFamily,
                letterSpacing = 5.sp
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = "SISTEM TEKNISI TERINTEGRASI",
                fontSize = 9.sp,
                color = Color.White.copy(alpha = 0.58f),
                fontFamily = OrbitronFontFamily,
                letterSpacing = 1.3.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(34.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF08171B).copy(alpha = 0.88f)
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = SciFiCyan.copy(alpha = 0.28f)
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 10.dp
                )
            ) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = 20.dp,
                        vertical = 20.dp
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "DATABASE INITIALIZATION",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SciFiCyan,
                                fontFamily = OrbitronFontFamily,
                                letterSpacing = 1.sp
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Secure data synchronization",
                                fontSize = 11.sp,
                                color = SciFiTextMuted
                            )
                        }

                        Text(
                            text = "${(progress * 100).toInt()}%",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = OrbitronFontFamily
                        )
                    }

                    Spacer(modifier = Modifier.height(17.dp))

                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(50)),
                        color = SciFiCyan,
                        trackColor = Color.White.copy(alpha = 0.09f),
                        strokeCap = StrokeCap.Round
                    )

                    Spacer(modifier = Modifier.height(19.dp))

                    loadingSteps.forEachIndexed { index, label ->
                        LoadingStepRow(
                            label = label,
                            stepIndex = index,
                            currentStep = currentStep,
                            isFinished = progress >= 1f
                        )

                        if (index != loadingSteps.lastIndex) {
                            Spacer(modifier = Modifier.height(11.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                PulsingDot()

                Spacer(modifier = Modifier.width(9.dp))

                Text(
                    text = if (progress >= 1f) {
                        "SISTEM SIAP DIGUNAKAN"
                    } else {
                        loadingSteps[currentStep].uppercase() + "..."
                    },
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.62f),
                    fontFamily = OrbitronFontFamily,
                    letterSpacing = 0.8.sp
                )
            }
        }

        Text(
            text = "Mohon tunggu, sistem sedang menyiapkan data",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 30.dp),
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.34f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DatabaseLoadingIcon() {
    val infiniteTransition =
        rememberInfiniteTransition(label = "databaseLoading")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2_200),
            repeatMode = RepeatMode.Restart
        ),
        label = "databaseRotation"
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "databasePulse"
    )

    Box(
        modifier = Modifier.size(112.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = rotation
                }
        ) {
            val strokeWidth = 3.dp.toPx()

            drawArc(
                color = SciFiCyan.copy(alpha = 0.95f),
                startAngle = 4f,
                sweepAngle = 76f,
                useCenter = false,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round
                )
            )

            drawArc(
                color = SciFiCyan.copy(alpha = 0.28f),
                startAngle = 112f,
                sweepAngle = 45f,
                useCenter = false,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round
                )
            )

            drawArc(
                color = SciFiCyan.copy(alpha = 0.62f),
                startAngle = 197f,
                sweepAngle = 105f,
                useCenter = false,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round
                )
            )
        }

        Box(
            modifier = Modifier
                .size(76.dp)
                .graphicsLayer {
                    scaleX = pulse
                    scaleY = pulse
                }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            SciFiCyan.copy(alpha = 0.22f),
                            Color(0xFF071A20)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = SciFiCyan.copy(alpha = 0.55f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "DB",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = SciFiCyan,
                    fontFamily = OrbitronFontFamily,
                    letterSpacing = 1.sp
                )

                Text(
                    text = "SYNC",
                    fontSize = 7.sp,
                    color = Color.White.copy(alpha = 0.55f),
                    fontFamily = OrbitronFontFamily,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
private fun LoadingStepRow(
    label: String,
    stepIndex: Int,
    currentStep: Int,
    isFinished: Boolean
) {
    val isCompleted =
        isFinished || stepIndex < currentStep

    val isActive =
        !isFinished && stepIndex == currentStep

    val indicatorColor = when {
        isCompleted -> SciFiCyan
        isActive -> Color.White
        else -> Color.White.copy(alpha = 0.18f)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(
                    indicatorColor.copy(
                        alpha = if (isCompleted) 0.17f else 0.08f
                    )
                )
                .border(
                    width = 1.dp,
                    color = indicatorColor.copy(alpha = 0.72f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when {
                    isCompleted -> "✓"
                    isActive -> "•"
                    else -> ""
                },
                fontSize = if (isActive) 16.sp else 11.sp,
                fontWeight = FontWeight.Bold,
                color = indicatorColor
            )
        }

        Spacer(modifier = Modifier.width(11.dp))

        Text(
            text = label,
            modifier = Modifier.weight(1f),
            fontSize = 12.sp,
            color = when {
                isCompleted -> Color.White.copy(alpha = 0.78f)
                isActive -> Color.White
                else -> Color.White.copy(alpha = 0.30f)
            }
        )

        Text(
            text = when {
                isCompleted -> "OK"
                isActive -> "READING"
                else -> "WAIT"
            },
            fontSize = 8.sp,
            color = indicatorColor,
            fontFamily = OrbitronFontFamily,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun PulsingDot() {
    val infiniteTransition =
        rememberInfiniteTransition(label = "statusPulse")

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650),
            repeatMode = RepeatMode.Reverse
        ),
        label = "statusAlpha"
    )

    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(SciFiCyan.copy(alpha = alpha))
    )
}
