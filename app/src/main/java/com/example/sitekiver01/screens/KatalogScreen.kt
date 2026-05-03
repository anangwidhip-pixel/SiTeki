package com.example.sitekiver01.screens

import android.app.Activity
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.sitekiver01.R
import com.example.sitekiver01.components.ModernSectionHeader
import com.example.sitekiver01.ui.theme.*
import com.example.sitekiver01.OrbitronFontFamily

data class KatalogItem(val title: String, val icon: Int, val url: String)

@Composable
fun KatalogScreen(
    onBack: () -> Unit,
    onNavigateToWebView: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val katalogItems = remember {
        listOf(
            KatalogItem("Bearing", R.drawable.bearing, "https://drive.google.com/file/d/1tN3fUbcZZFIik2ne-qhtUgzoHXfVqR8y/view?usp=drive_link"),
            KatalogItem("Baut", R.drawable.bolts, "https://drive.google.com/file/d/1CYVlwlHJ2vlEVinisZBqxXI1VgNC9Bqs/view?usp=drive_link"),
            KatalogItem("Chain Coupling", R.drawable.chain, "https://drive.google.com/file/d/1-djNvG4UZl_bS2l_4MuUnpv-Adj_EEgi/view?usp=drive_link"),
            KatalogItem("Circlip", R.drawable.circlip, "https://drive.google.com/file/d/1UH5kRt9hzUjjnrBVgFRODjToZpld6m_j/view?usp=drive_link"),
            KatalogItem("Flange", R.drawable.flange, "https://drive.google.com/file/d/1_NR0iIaRgz41jjMCnKc5P6zH2UKaLp7Q/view?usp=drive_link"),
            KatalogItem("Matras", R.drawable.matras, "https://drive.google.com/file/d/1S-sr9L7zuZstlZ_LjIVpYWqi-txINKDb/view?usp=drive_link"),
            KatalogItem("Nepple", R.drawable.nipple, "https://drive.google.com/file/d/10_grZG5JKpbrWthKkPbD89pf9WAhTqDL/view?usp=drive_link"),
            KatalogItem("O-Ring", R.drawable.oring, "https://drive.google.com/file/d/1zqcM5-r60p7U0byWZaHW3I4xYIXG2eG2/view?usp=share_link"),
            KatalogItem("Pillow Block", R.drawable.pillow, "https://drive.google.com/file/d/1ctS41dKaJYfyhxX1qspLuoOlkI-5cpBi/view?usp=drive_link"),
            KatalogItem("Ukuran Pipa", R.drawable.pipa1, "https://drive.google.com/file/d/1InSoY3vw6VzV6QWJSfquZMbRAm70mJYV/view?usp=drive_link"),
            KatalogItem("Ring Matahari", R.drawable.rsun, "https://drive.google.com/file/d/1-djNvG4UZl_bS2l_4MuUnpv-Adj_EEgi/view?usp=drive_link"),
            KatalogItem("Seals", R.drawable.seals, "https://drive.google.com/file/d/1XtOBj08kLdcr0kXWW3cNRpP3W6fCZNSn/view?usp=drive_link"),
            KatalogItem("Inch ke mm (Pipa)", R.drawable.ukpipamm, "https://drive.google.com/file/d/1BT-TrHtNPmDuOafY9VUPq0t39rLfzH61/view?usp=drive_link")
        )
    }

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
                        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars).fillMaxWidth().height(72.dp).padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)
                        ) { Icon(Icons.Default.ArrowBackIosNew, "Back", tint = Color.White) }
                        Spacer(Modifier.width(16.dp))
                        Text(
                            "DAFTAR KATALOG",
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
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(Modifier.height(20.dp))
                ModernSectionHeader("KOLEKSI KATALOG", Icons.Default.AutoAwesomeMotion)
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(katalogItems) { item ->
                        ModernKatalogCard(item = item, onClick = { onNavigateToWebView(item.url, item.title) })
                    }
                }
            }
        }
    }
}

@Composable
fun ModernKatalogCard(item: KatalogItem, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(28.dp),
        color = GlassSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(80.dp).background(Color.White.copy(alpha = 0.05f), CircleShape).padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(painter = painterResource(id = item.icon), contentDescription = item.title, modifier = Modifier.fillMaxSize())
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = item.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.Center, color = Color.White, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun KatalogScreenPreview() { SiTekiVer01Theme { KatalogScreen(onBack = {}, onNavigateToWebView = { _, _ -> }) } }
