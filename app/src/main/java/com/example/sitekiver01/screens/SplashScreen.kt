package com.example.sitekiver01.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sitekiver01.OrbitronFontFamily
import com.example.sitekiver01.components.SciFiBackground
import com.example.sitekiver01.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    // Simulasi loading selama 2.5 detik
    LaunchedEffect(Unit) {
        delay(2500)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GlassBase),
        contentAlignment = Alignment.Center
    ) {
        // Efek background mesh grid yang sama dengan Login
        SciFiBackground()

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // LOGO / NAMA APLIKASI
            Text(
                text = "SITEKI",
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                color = SciFiCyan,
                fontFamily = OrbitronFontFamily,
                letterSpacing = 4.sp
            )

            Text(
                text = "SYSTEM INITIALIZATION...",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.5f),
                fontFamily = OrbitronFontFamily,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            // LOADING ANIMATION
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = SciFiCyan,
                strokeWidth = 3.dp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "VERIFIKASI SERVER...",
                fontSize = 9.sp,
                color = SciFiTextMuted,
                fontFamily = OrbitronFontFamily
            )
        }
    }
}