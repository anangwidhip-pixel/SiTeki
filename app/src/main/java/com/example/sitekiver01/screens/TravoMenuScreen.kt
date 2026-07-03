package com.example.sitekiver01.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sitekiver01.OrbitronFontFamily
import com.example.sitekiver01.components.*
import com.example.sitekiver01.ui.theme.*

@Composable
fun TravoMenuScreen(
    onBack: () -> Unit,
    onNavigateToInspeksi: () -> Unit,
    onNavigateToDataTravo: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(GlassBase)) {
        // Efek Background Grid Global
        SciFiBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // HEADER NAVIGASI
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(Icons.Default.ArrowBackIosNew, "Back", tint = Color.White)
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    text = "MANAJEMEN TRAVO",
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    fontFamily = OrbitronFontFamily,
                    fontSize = 18.sp,
                    letterSpacing = 1.sp
                )
            }

            Spacer(Modifier.height(32.dp))

            // GRID MENU
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MenuTravoCard(
                    title = "INSPEKSI\nTRAVO",
                    icon = Icons.Default.Checklist,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToInspeksi
                )
                MenuTravoCard(
                    title = "DATA\nTRAVO",
                    icon = Icons.Default.SettingsInputComponent,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToDataTravo
                )
            }
        }
    }
}

@Composable
fun MenuTravoCard(title: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(150.dp),
        shape = RoundedCornerShape(20.dp),
        color = SciFiGlass,
        border = BorderStroke(1.dp, SciFiBorderLight.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SciFiCyan,
                modifier = Modifier.size(45.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontFamily = OrbitronFontFamily,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }
    }
}