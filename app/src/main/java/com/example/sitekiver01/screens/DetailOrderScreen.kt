package com.example.sitekiver01.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sitekiver01.OrbitronFontFamily
import com.example.sitekiver01.components.*
import com.example.sitekiver01.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DetailOrderScreen(
    order: OrderItem,
    onBack: () -> Unit,
    onLakukanPerbaikan: (OrderItem) -> Unit
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
                        ) { Icon(Icons.Default.ArrowBackIosNew, "Back", tint = Color.White) }
                        Spacer(Modifier.width(16.dp))
                        Text(
                            "DETAIL ORDER KERJA",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            fontFamily = OrbitronFontFamily
                        )
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(12.dp))

                // Section Identitas & Status
                ModernSectionHeader("IDENTITAS & STATUS", Icons.Default.Info)
                ModernFormCard {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f)) {
                                DetailItemModern("TANGGAL", order.tanggal, Icons.Default.CalendarToday)
                            }
                            Surface(
                                color = if (order.status.equals("Open", true)) GlassAccentGreen.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (order.status.equals("Open", true)) GlassAccentGreen.copy(alpha = 0.5f) else GlassBorder)
                            ) {
                                Text(
                                    order.status.uppercase(),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    color = if (order.status.equals("Open", true)) GlassAccentGreen else GlassTextMuted,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        DetailItemModern("NAMA MESIN", order.namaMesin, Icons.Default.PrecisionManufacturing)
                        DetailItemModern("BAGIAN ORDER", order.bagianOrder, Icons.Default.Business)
                        DetailItemModern("NAMA PEMBUAT", order.namaOrder, Icons.Default.Person)
                        DetailItemModern("BAGIAN TUJUAN", order.bagianTujuan, Icons.AutoMirrored.Filled.AltRoute)
                    }
                }

                // Section Detail Kerusakan
                ModernSectionHeader("DETAIL KERUSAKAN", Icons.Default.Build)
                ModernFormCard {
                    Column {
                        DetailItemModern("URGENSI", order.urgensi, Icons.Default.Warning, 
                            valueColor = when(order.urgensi) {
                                "Penting Sekali" -> Color.Red
                                "Penting" -> GlassAccentAmber
                                else -> GlassAccentCyan
                            }
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("DESKRIPSI KERUSAKAN", fontSize = 10.sp, color = GlassTextMuted, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = order.kerusakan.ifBlank { "Tidak ada deskripsi kerusakan." },
                            fontSize = 14.sp,
                            color = Color.White,
                            lineHeight = 22.sp
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                if (order.status.equals("Open", true)) {
                    ModernButton(
                        text = "LAKUKAN PERBAIKAN",
                        onClick = { onLakukanPerbaikan(order) },
                        modifier = Modifier.fillMaxWidth(),
                        icon = Icons.Default.Engineering
                    )
                }

                Spacer(Modifier.height(50.dp))
            }
        }
    }
}

@Composable
fun DetailItemModern(label: String, value: String, icon: ImageVector, valueColor: Color = Color.White) {
    Row(modifier = Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(18.dp), tint = GlassAccentCyan)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(text = label, fontSize = 10.sp, color = GlassTextMuted)
            Text(
                text = if (value.isBlank() || value == "null") "-" else value,
                fontSize = 14.sp,
                color = valueColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
