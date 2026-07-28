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
    onNavigateToLemburan: () -> Unit,
    onNavigateToTravo: () -> Unit //
) {
    val context = LocalContext.current
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // PONDASI UTAMA: Background Mesh Grid Animasi Global
        SciFiBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // TOP BAR NAVIGASI SIBER
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                ) {
                    Icon(Icons.Default.ArrowBackIosNew, "Back", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    text = "Ruang pendukung",
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(Modifier.height(4.dp))
            Text(
                "Pilih modul untuk melanjutkan pekerjaan.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )

            // GRID MENU UTAMA (TRANSPARAN KACA HUD)
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                // REVISI: Menu "USER" dihapus, tersisa 7 menu fungsional agar simetris & bersih
                val menuItems = listOf(
                    MenuItem("Teknisi", Icons.Default.Person, primary, "Manajemen profil dan akses teknisi"),
                    MenuItem("Travo", Icons.Default.ElectricBolt, tertiary, "Inspeksi dan monitoring transformator"),
                    MenuItem("Lemburan", Icons.Default.AccessTime, secondary, "Pengajuan dan riwayat kerja lembur"),
                    MenuItem("Katalog", Icons.Default.MenuBook, primary, "Referensi produk dan kebutuhan teknik")
                )

                items(menuItems) { item ->
                    NeonMenuCard(
                        menuItem = item,
                        onClick = {
                            when (item.title.uppercase()) {
                                "TEKNISI"  -> onNavigateToUserMgmt()
                                "KATALOG"  -> onNavigateToKatalog()
                                "LEMBURAN" -> onNavigateToLemburan()
                                // --- TAMBAHKAN KODE INI ---
                                "TRAVO"    -> onNavigateToTravo()
                                // --------------------------
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
            .height(82.dp)
            .scale(scale)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = if (isPressed) 0.dp else 3.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(menuItem.color.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = menuItem.icon,
                        contentDescription = null,
                        tint = menuItem.color,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(menuItem.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(3.dp))
                    Text(menuItem.subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(Icons.Default.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
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
