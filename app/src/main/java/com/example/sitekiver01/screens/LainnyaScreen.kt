package com.example.sitekiver01.screens

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sitekiver01.OrbitronFontFamily
import com.example.sitekiver01.components.*
import com.example.sitekiver01.ui.theme.*

@Composable
fun LainnyaScreen(
    onBack: () -> Unit,
    onNavigateToKatalog: () -> Unit,
    onNavigateToUserMgmt: () -> Unit, // Satu gerbang untuk database siber teknisi detail
    onNavigateToLemburan: () -> Unit
) {
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize().background(GlassBase)) {

        // PONDASI UTAMA: Background Mesh Grid Animasi Global
        SciFiBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // TOP BAR NAVIGASI SIBER
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(Icons.Default.ArrowBackIosNew, "Back", tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    text = "MENU LAINNYA",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    fontFamily = OrbitronFontFamily,
                    letterSpacing = 1.sp
                )
            }

            Spacer(Modifier.height(4.dp))
            ModernSectionHeader("NAVIGASI SUB SISTEM", Icons.Default.Apps)

            // GRID MENU UTAMA (TRANSPARAN KACA HUD)
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                // REVISI: Menu "USER" dihapus, tersisa 7 menu fungsional agar simetris & bersih
                val menuItems = listOf(
                    MenuItem("Teknisi", Icons.Default.Person, SciFiCyan, "Manajemen Teknisi"),
                    MenuItem("Travo", Icons.Default.ElectricBolt, SciFiSaturday, "Monitoring Trafo"),
                    MenuItem("Lemburan", Icons.Default.AccessTime, SciFiStatusM, "Pengajuan Lembur"),
                    MenuItem("Katalog", Icons.Default.MenuBook, SciFiCyan, "Katalog Produk"),
                    MenuItem("Menu 6", Icons.Default.Info, SciFiCyan, "Informasi"),
                    MenuItem("Menu 7", Icons.Default.Favorite, Color(0xFFC23B22), "Favorit"),
                    MenuItem("Setting", Icons.Default.Settings, SciFiCyan, "Pengaturan")
                )

                items(menuItems) { item ->
                    NeonMenuCard(
                        menuItem = item,
                        onClick = {
                            when (item.title.uppercase()) {
                                "TEKNISI"  -> onNavigateToUserMgmt() // Tombol TEKNISI sekarang mengarah ke User Management A-Z
                                "KATALOG"  -> onNavigateToKatalog()
                                "LEMBURAN" -> onNavigateToLemburan()
                                else -> Toast.makeText(context, "Membuka ${item.title}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NeonMenuCard(
    menuItem: MenuItem,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
        label = "menuScale"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.05f)
            .scale(scale)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = SciFiGlass,
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = if (isPressed) listOf(menuItem.color, Color.Transparent)
                else listOf(SciFiBorderLight, Color.Transparent)
            )
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Efek Reaktor Neon Glow Belakang Saat Ditekan
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(20.dp)
                    .background(
                        color = menuItem.color.copy(alpha = if (isPressed) 0.18f else 0.03f),
                        shape = RoundedCornerShape(24.dp)
                    )
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(12.dp).align(Alignment.Center)
            ) {
                // Lingkaran Dudukan Ikon Menu
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(menuItem.color.copy(alpha = 0.08f), CircleShape)
                        .border(1.dp, menuItem.color.copy(alpha = 0.2f), CircleShape)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = menuItem.icon,
                        contentDescription = null,
                        tint = menuItem.color,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = menuItem.title.uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White,
                    fontFamily = OrbitronFontFamily,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = menuItem.subtitle,
                    fontSize = 11.sp,
                    color = SciFiTextMuted,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

data class MenuItem(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color,
    val subtitle: String
)