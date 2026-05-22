package com.example.sitekiver01.screens

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sitekiver01.ui.theme.*

@Composable
fun LainnyaScreen(
    onBack: () -> Unit,
    onNavigateToKatalog: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GlassBase)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Menu Lainnya",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                fontFamily = OrbitronFontFamily
            )
        }

        Spacer(Modifier.height(24.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            val menuItems = listOf(
                MenuItem("Teknisi", Icons.Default.Person, GlassAccentCyan, "Manajemen Teknisi"),
                MenuItem("Travo", Icons.Default.ElectricBolt, Color(0xFFFF9800), "Monitoring Trafo"),
                MenuItem("Lemburan", Icons.Default.AccessTime, Color(0xFF4CAF50), "Pengajuan Lembur"),
                MenuItem("Katalog", Icons.Default.MenuBook, GlassAccentCyan, "Katalog Produk"),
                MenuItem("Menu 5", Icons.Default.Star, Color(0xFF9C27B0), "Penilaian"),
                MenuItem("Menu 6", Icons.Default.Info, Color(0xFF00BCD4), "Informasi"),
                MenuItem("Menu 7", Icons.Default.Favorite, Color(0xFFFF5722), "Favorit"),
                MenuItem("Setting", Icons.Default.Settings, GlassAccentCyan, "Pengaturan")
            )

            items(menuItems) { item ->
                NeonMenuCard(item) {
                    if (item.title == "Katalog") {
                        onNavigateToKatalog()
                    } else {
                        Toast.makeText(context, "Membuka ${item.title}", Toast.LENGTH_SHORT).show()
                    }
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
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f)
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .scale(scale)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF1F1F1F),
        border = BorderStroke(2.dp, menuItem.color.copy(alpha = 0.6f)),
        tonalElevation = 12.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Strong Neon Glow
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(24.dp)
                    .background(menuItem.color.copy(alpha = if (isPressed) 0.5f else 0.25f), RoundedCornerShape(24.dp))
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(78.dp)
                        .background(menuItem.color.copy(alpha = 0.2f), CircleShape)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = menuItem.icon,
                        contentDescription = null,
                        tint = menuItem.color,
                        modifier = Modifier.size(46.dp)
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = menuItem.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )

                Text(
                    text = menuItem.subtitle,
                    fontSize = 11.5.sp,
                    color = Color.White.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                    maxLines = 1
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