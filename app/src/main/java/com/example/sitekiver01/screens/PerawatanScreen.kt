package com.example.sitekiver01.screens

import android.app.Activity
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import com.example.sitekiver01.R
import com.example.sitekiver01.components.PerawatanCardItem
import com.example.sitekiver01.components.ModernFormCard
import com.example.sitekiver01.components.ModernSectionHeader
import com.example.sitekiver01.ui.theme.*
import com.example.sitekiver01.OrbitronFontFamily

@Composable
fun PerawatanScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onNavigateToWebView: (String, String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(GlassBase)) {
        // Animated Background Orbs
        val infiniteTransition = rememberInfiniteTransition(label = "orbs")
        val orbOffset by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 100f,
            animationSpec = infiniteRepeatable(
                animation = tween(8000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ), label = "orbMove"
        )

        Box(modifier = Modifier
            .size(400.dp)
            .offset(x = (-100).dp + orbOffset.dp, y = (-100).dp + (orbOffset/2).dp)
            .background(GlassAccentPurple.copy(alpha = 0.15f), CircleShape)
            .blur(100.dp))
        Box(modifier = Modifier
            .size(350.dp)
            .align(Alignment.BottomEnd)
            .offset(x = 100.dp - orbOffset.dp, y = 100.dp - (orbOffset/3).dp)
            .background(GlassAccentCyan.copy(alpha = 0.12f), CircleShape)
            .blur(80.dp))

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Surface(
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .fillMaxWidth()
                            .height(72.dp)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back", tint = Color.White)
                        }
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = "HALAMAN PERAWATAN",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            fontFamily = OrbitronFontFamily
                        )
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(20.dp))
                ModernSectionHeader("PERAWATAN ARMADA", Icons.Default.LocalShipping)
                ModernFormCard {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        PerawatanCardItem(
                            title = "MOBILE CRANE",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp),
                            imageRes = R.drawable.mcrane,
                            onClick = { onNavigateToWebView("https://forms.gle/9oeGnfNFH7NctVSL6", "MOBILE CRANE") }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            PerawatanCardItem(
                                title = "FORKLIFT",
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(110.dp),
                                imageRes = R.drawable.forklift,
                                onClick = { onNavigateToWebView("https://forms.gle/Je8ErK1qeXrgP5rz8", "FORKLIFT") }
                            )
                            PerawatanCardItem(
                                title = "TRAILLER",
                                modifier = Modifier
                                    .weight(1f)
                                    .height(110.dp),
                                imageRes = R.drawable.trailler,
                                onClick = { onNavigateToWebView("https://forms.gle/MYqeNrEjUBqHfZwF7", "TRAILLER") }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            PerawatanCardItem(
                                title = "UMUM",
                                modifier = Modifier
                                    .weight(1f)
                                    .height(110.dp),
                                imageRes = R.drawable.umum,
                                onClick = { onNavigateToWebView("https://forms.gle/1xRJ95qbFvMHYP7e9", "UMUM") }
                            )
                            PerawatanCardItem(
                                title = "DUMP TRUCK",
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(110.dp),
                                imageRes = R.drawable.dump,
                                onClick = { onNavigateToWebView("https://forms.gle/6xrAHKmjTsiXNHFK9", "DUMP TRUCK") }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                ModernSectionHeader("PERAWATAN MESIN", Icons.Default.Build)
                ModernFormCard {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        PerawatanCardItem(
                            title = "ALAT UJI",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp),
                            imageRes = R.drawable.uji,
                            onClick = { onNavigateToWebView("https://forms.gle/JgK6rWyN1pwFoUJB7", "ALAT UJI") }
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            PerawatanCardItem(
                                title = "BEVEL",
                                modifier = Modifier
                                    .width(110.dp)
                                    .height(236.dp),
                                imageRes = R.drawable.bevel,
                                onClick = { onNavigateToWebView("https://forms.gle/Ay4jcffwEmmQkYDv6", "BEVEL") }
                            )
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                PerawatanCardItem(
                                    title = "GENSET",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp),
                                    imageRes = R.drawable.genset,
                                    onClick = { onNavigateToWebView("https://forms.gle/Qag9jZr5o4LGMHi58", "GENSET") }
                                )
                                PerawatanCardItem(
                                    title = "KOMPRESSOR",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp),
                                    imageRes = R.drawable.kompressor,
                                    onClick = { onNavigateToWebView("https://forms.gle/ggni6sJT5NSa9X2b9", "KOMPRESSOR") }
                                )
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                PerawatanCardItem(
                                    title = "PERAKITAN",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp),
                                    imageRes = R.drawable.perakitan,
                                    onClick = { onNavigateToWebView("https://forms.gle/fiGKBtoiSQFo3cTc8", "PERAKITAN") }
                                )
                                PerawatanCardItem(
                                    title = "LAKOP",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp),
                                    imageRes = R.drawable.lakop,
                                    onClick = { onNavigateToWebView("https://forms.gle/TkddC4kQBAQwqjyu8", "LAKOP") }
                                )
                            }
                            PerawatanCardItem(
                                title = "KOP",
                                modifier = Modifier
                                    .width(110.dp)
                                    .height(236.dp),
                                imageRes = R.drawable.kop,
                                onClick = { onNavigateToWebView("https://forms.gle/SvpGjLeoKneRjT6w9", "KOP") }
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            PerawatanCardItem(
                                title = "PIPA ERW",
                                modifier = Modifier
                                    .width(110.dp)
                                    .height(236.dp),
                                imageRes = R.drawable.pipa,
                                onClick = { onNavigateToWebView("https://forms.gle/8YTgW2oC1SvoiRnT8", "PIPA ERW") }
                            )
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                PerawatanCardItem(
                                    title = "POTONG BAHAN",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp),
                                    imageRes = R.drawable.pbahan,
                                    onClick = { onNavigateToWebView("https://forms.gle/ugTC9Ywu1di4dZ4i7", "POTONG BAHAN") }
                                )
                                PerawatanCardItem(
                                    title = "SLITTING",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp),
                                    imageRes = R.drawable.slitting,
                                    onClick = { onNavigateToWebView("https://forms.gle/F4SMAk4CeaS9zBGw9", "SLITTING") }
                                )
                            }
                        }

                        PerawatanCardItem(
                            title = "VERLOOP",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp),
                            imageRes = R.drawable.verloop,
                            onClick = { onNavigateToWebView("https://forms.gle/QDfHwhcJtqvEnt3N8", "VERLOOP") }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PerawatanScreenPreview() {
    SiTekiVer01Theme {
        PerawatanScreen(onBack = {}, onNavigateToWebView = { _, _ -> })
    }
}
