package com.example.sitekiver01.screens

import android.app.Activity
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.sitekiver01.ui.theme.OrbitronFontFamily
import com.example.sitekiver01.ui.theme.RajabesiDarkNavy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import com.example.sitekiver01.components.ModernFormCard
import com.example.sitekiver01.components.ModernSectionHeader
import com.example.sitekiver01.components.ModernClickableField
import com.example.sitekiver01.ui.theme.*

// --- DATA MODELS ---
data class DowntimeData(val bulan: String, val jam: Float)
data class ComponentDowntime(val nama: String, val jam: Float)
data class SectionDowntime(val nama: String, val jam: Float, val color: Color)
data class MachineDowntime(val nama: String, val jam: Float)

data class RawLaporanKerja(
    val bulan: String,
    val bagian: String,
    val mesin: String,
    val komponen: String,
    val totalJam: Float,
    val jenis: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailDowntimeScreen(onBack: () -> Unit) {
    var selectedMonth by remember { mutableStateOf("Semua Bulan") }
    var selectedType by remember { mutableStateOf("Semua Jenis") }
    var expandedMonth by remember { mutableStateOf(false) }
    var expandedType by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    var downtimeMonthly by remember { mutableStateOf<List<DowntimeData>>(emptyList()) }
    var rawLaporanList by remember { mutableStateOf<List<RawLaporanKerja>>(emptyList()) }

    val urlCombinedData = "https://script.google.com/macros/s/AKfycbwXEeFSt5dCP-gPUtSbLX1WCfvPfSe7wJGnMs4vwEt1djVQNVjXxUdv8_ly9uFvM4o/exec"

    val monthMap = mapOf(
        "Januari" to "Jan", "Februari" to "Feb", "Maret" to "Mar", "April" to "Apr",
        "Mei" to "Mei", "Juni" to "Jun", "Juli" to "Jul", "Agustus" to "Agu",
        "September" to "Sep", "Oktober" to "Okt", "November" to "Nov", "Desember" to "Des"
    )

    LaunchedEffect(Unit) {
        isLoading = true
        try {
            withContext(Dispatchers.IO) {
                val response = URL(urlCombinedData).readText()
                val jsonResponse = JSONObject(response)

                val rekapArray = jsonResponse.getJSONArray("rekap")
                val tempRekap = mutableListOf<DowntimeData>()
                for (i in 0 until rekapArray.length()) {
                    val obj = rekapArray.getJSONObject(i)
                    tempRekap.add(DowntimeData(obj.optString("bulan", ""), obj.optDouble("jam", 0.0).toFloat()))
                }
                downtimeMonthly = tempRekap

                val laporanArray = jsonResponse.getJSONArray("laporan_mentah")
                val tempLaporan = mutableListOf<RawLaporanKerja>()
                for (i in 0 until laporanArray.length()) {
                    val obj = laporanArray.getJSONObject(i)
                    tempLaporan.add(RawLaporanKerja(
                        bulan = obj.optString("bulan", ""),
                        bagian = obj.optString("bagian", "Lainnya"),
                        mesin = obj.optString("mesin", "Unknown"),
                        komponen = obj.optString("komponen", "Lain-lain"),
                        totalJam = obj.optDouble("total_jam", 0.0).toFloat(),
                        jenis = obj.optString("jenis", "Lainnya")
                    ))
                }
                rawLaporanList = tempLaporan
            }
        } catch (e: Exception) {
            Log.e("DowntimeDetail", "Error: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    val allJenisOptions = remember(rawLaporanList) {
        listOf("Semua Jenis") + rawLaporanList.map { it.jenis }
            .filter { it.isNotBlank() && it != "null" }
            .distinct()
            .sorted()
    }

    val baseFilteredData = remember(selectedMonth, selectedType, rawLaporanList) {
        val targetMonthShort = monthMap[selectedMonth] ?: ""
        rawLaporanList.filter {
            val monthMatch = if (selectedMonth == "Semua Bulan") true else it.bulan == targetMonthShort
            val typeMatch = if (selectedType == "Semua Jenis") true else it.jenis == selectedType
            monthMatch && typeMatch
        }
    }

    val filteredTopMachines = remember(selectedMonth, rawLaporanList) {
        val targetMonthShort = monthMap[selectedMonth] ?: ""
        rawLaporanList.filter { if (selectedMonth == "Semua Bulan") true else it.bulan == targetMonthShort }
            .groupBy { it.mesin }.map { (name, list) -> MachineDowntime(name, list.sumOf { it.totalJam.toDouble() }.toFloat()) }
            .sortedByDescending { it.jam }.take(10)
    }

    val filteredComponentData = remember(baseFilteredData) {
        baseFilteredData.groupBy { it.komponen }.map { (name, list) -> ComponentDowntime(name, list.sumOf { it.totalJam.toDouble() }.toFloat()) }
            .sortedByDescending { it.jam }.take(5)
    }

    val filteredSectionData = remember(baseFilteredData) {
        // Penyesuaian palet warna Tree-Map agar masuk ke tema Glassmorphic Gelap
        val colors = listOf(Color(0xFF00E5FF), Color(0xFF00B0FF), Color(0xFFFFB300), Color(0xFFD500F9), Color(0xFF76FF03))
        baseFilteredData.groupBy { it.bagian }.map { (name, list) -> name to list.sumOf { it.totalJam.toDouble() }.toFloat() }
            .sortedByDescending { it.second }.take(5).mapIndexed { index, pair -> SectionDowntime(pair.first, pair.second, colors.getOrElse(index) { Color.Gray }) }
    }

    // --- REVISI TEMA STATUS BAR (GLASS STYLE) ---
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = GlassBase.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    Scaffold(
        containerColor = Color.Transparent, // Menyesuaikan tema DetailPerawatanScreen
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
                        Icon(Icons.Default.ArrowBackIosNew, "Back", tint = GlassAccentCyan)
                    }
                    Spacer(Modifier.width(16.dp))
                    Text(
                        "DETAIL PERFORMANCE",
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
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(20.dp))

            // Header Card dengan tema Glassmorphism
            ModernFormCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("PT. RAJA BESI", color = GlassAccentCyan, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, fontFamily = OrbitronFontFamily)
                        Text("DOWNTIME MESIN - 2026", color = GlassTextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val totalNow = if (downtimeMonthly.isNotEmpty()) downtimeMonthly.sumOf { it.jam.toDouble() } / 1000 else 0.0
                        StatBoxDowntime("DownTime -> Now", "${String.format("%.1f", totalNow)} rb")
                        StatBoxDowntime("Target /Th", "6.000")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Dropdown filter dengan Glass-Style background menu
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    ModernClickableField(selectedMonth, "Bulan") { expandedMonth = true }
                    DropdownMenu(
                        expanded = expandedMonth,
                        onDismissRequest = { expandedMonth = false },
                        modifier = Modifier
                            .background(GlassSurface)
                            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                    ) {
                        listOf("Semua Bulan", "Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember").forEach { m ->
                            DropdownMenuItem(text = { Text(m, color = Color.White, fontSize = 15.sp) }, onClick = { selectedMonth = m; expandedMonth = false })
                        }
                    }
                }
                Box(modifier = Modifier.weight(1f)) {
                    ModernClickableField(selectedType, "Jenis") { expandedType = true }
                    DropdownMenu(
                        expanded = expandedType,
                        onDismissRequest = { expandedType = false },
                        modifier = Modifier
                            .background(GlassSurface)
                            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                    ) {
                        allJenisOptions.forEach { t ->
                            DropdownMenuItem(text = { Text(t, color = Color.White, fontSize = 15.sp) }, onClick = { selectedType = t; expandedType = false })
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // --- REVISI ANIMASI MASUK KONTEN (AnimatedVisibility) ---
            AnimatedVisibility(
                visible = !isLoading,
                enter = fadeIn(animationSpec = tween(500)) + slideInVertically(animationSpec = tween(500), initialOffsetY = { 60 })
            ) {
                Column {
                    ModernSectionHeader("TOTAL DOWNTIME (JAM)", Icons.Default.Timer)
                    DowntimeLineChart(data = downtimeMonthly)

                    Spacer(modifier = Modifier.height(32.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            ModernSectionHeader("KOMPONEN", Icons.Default.SettingsInputComponent)
                            ComponentBarChart(data = filteredComponentData)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            ModernSectionHeader("PER BAGIAN", Icons.Default.Business)
                            SectionTreeMap(data = filteredSectionData)
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    ModernSectionHeader("TOP 10 DOWNTIME TERBANYAK", Icons.Default.TrendingDown)
                    TopDowntimeHorizontalChart(data = filteredTopMachines)
                }
            }

            Spacer(modifier = Modifier.height(120.dp))
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = GlassAccentCyan)
            }
        }
    }
}

// --- REVISI TEMA GRAFIK UTAMA (LINE CHART) ---
@Composable
fun DowntimeLineChart(data: List<DowntimeData>) {
    val textMeasurer = rememberTextMeasurer()
    val months = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
    val yellowLine = Color(0xFFFFD700)
    val targetRed = Color(0xFFFF3B30) // Lebih vibrant khas tema dark neon

    Card(
        modifier = Modifier.fillMaxWidth().height(320.dp).padding(top = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = GlassSurface),
        border = BorderStroke(1.dp, GlassBorder),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 30.dp)) {
            val width = size.width; val height = size.height - 40.dp.toPx(); val maxVal = 1200f; val spacing = width / 11f; val targetJam = 500f; val targetY = height - (targetJam / maxVal * height)

            // Grid Lines transparan khas tema glass
            listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { p ->
                val gridY = height * p
                drawLine(color = Color.White.copy(alpha = 0.05f), start = Offset(0f, gridY), end = Offset(width, gridY), strokeWidth = 1f)
            }

            drawLine(color = targetRed, start = Offset(0f, targetY), end = Offset(width, targetY), strokeWidth = 2.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)))
            val targetText = "Target Max 500 Jam"; val targetPillStyle = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            val textLayout = textMeasurer.measure(targetText, targetPillStyle); val boxWidth = textLayout.size.width + 16f; val boxHeight = textLayout.size.height + 8f; val boxX = width - boxWidth; val boxY = targetY - textLayout.size.height - 15f
            drawRoundRect(color = targetRed, topLeft = Offset(boxX, boxY), size = Size(boxWidth, boxHeight), cornerRadius = CornerRadius(6.dp.toPx()))
            drawText(textMeasurer, targetText, Offset(boxX + 8f, boxY + 4f), style = targetPillStyle)

            if (data.isNotEmpty()) {
                val points = (0..11).map { index -> val value = data.find { it.bulan == months[index] }?.jam ?: 0f; Offset(index * spacing, height - (value / maxVal * height)) }
                val curvePath = Path().apply { moveTo(points[0].x, points[0].y); for (i in 0 until points.size - 1) { val p1 = points[i]; val p2 = points[i + 1]; cubicTo((p1.x + p2.x) / 2, p1.y, (p1.x + p2.x) / 2, p2.y, p2.x, p2.y) } }
                val fillPath = Path().apply { addPath(curvePath); lineTo(points.last().x, height); lineTo(points.first().x, height); close() }

                drawPath(path = fillPath, brush = Brush.verticalGradient(colors = listOf(yellowLine.copy(alpha = 0.15f), Color.Transparent), startY = points.minOf { it.y }, endY = height))
                drawPath(curvePath, yellowLine, style = Stroke(width = 5.5f, cap = StrokeCap.Round))

                points.forEachIndexed { i, pt ->
                    drawCircle(Color.White, 4.5f, pt)
                    drawCircle(yellowLine, 3f, pt)
                    drawText(textMeasurer, months[i], Offset(pt.x - 10.dp.toPx(), height + 18.dp.toPx()), style = TextStyle(color = GlassTextMuted, fontSize = 9.sp))
                    val jamValue = data.find { it.bulan == months[i] }?.jam ?: 0f
                    if (jamValue > 0) {
                        drawText(textMeasurer, jamValue.toInt().toString(), Offset(pt.x - 12.dp.toPx(), pt.y - 25.dp.toPx()), style = TextStyle(color = yellowLine, fontSize = 10.sp, fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

// --- REVISI TEMA STATBOX KACA ---
@Composable
fun StatBoxDowntime(label: String, value: String) {
    Box(
        modifier = Modifier
            .size(width = 85.dp, height = 62.dp)
            .background(GlassSurface, RoundedCornerShape(16.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = GlassTextMuted, fontSize = 9.sp, textAlign = TextAlign.Center, lineHeight = 11.sp, fontWeight = FontWeight.Bold)
            Text(value, color = GlassAccentCyan, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

// --- REVISI TEMA GRAFIK HORIZONTAL (TOP 10) ---
@Composable
fun TopDowntimeHorizontalChart(data: List<MachineDowntime>) {
    val textMeasurer = rememberTextMeasurer(); val barColor = Color(0xFFF50057) // Neon Pink accent
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        colors = CardDefaults.cardColors(containerColor = GlassSurface),
        border = BorderStroke(1.dp, GlassBorder),
        elevation = CardDefaults.cardElevation(0.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        if (data.isEmpty()) { Box(Modifier.fillMaxWidth().height(200.dp), Alignment.Center) { Text("Tidak ada data", color = GlassTextMuted, fontSize = 12.sp) } }
        else {
            Column(modifier = Modifier.padding(16.dp)) {
                val maxVal = (data.maxOfOrNull { it.jam } ?: 1f).coerceAtLeast(1f)
                Canvas(modifier = Modifier.fillMaxWidth().height((data.size * 40 + 40).dp)) {
                    val chartWidth = size.width * 0.7f; val leftMargin = size.width * 0.25f; val barHeight = 18.dp.toPx(); val spacing = 40.dp.toPx()
                    for (i in 0..4) {
                        val x = leftMargin + (i * chartWidth / 4)
                        drawLine(color = Color.White.copy(alpha = 0.05f), start = Offset(x, 0f), end = Offset(x, data.size * spacing), strokeWidth = 1f)
                        drawText(textMeasurer, "${(i * maxVal / 4).toInt()}", Offset(x - 10f, data.size * spacing + 8f), style = TextStyle(GlassTextMuted, 9.sp))
                    }
                    data.forEachIndexed { index, item ->
                        val y = index * spacing + 10f; val currentBarWidth = (item.jam / maxVal) * chartWidth
                        drawText(textMeasurer, item.nama.take(12), Offset(5f, y + 2f), style = TextStyle(Color.White, 10.sp, textAlign = TextAlign.End), size = Size(leftMargin - 15f, barHeight))
                        drawRoundRect(barColor, Offset(leftMargin, y), Size(currentBarWidth, barHeight), CornerRadius(4.dp.toPx()))

                        // --- PERBAIKAN DI SINI: Memformat nilai jam menjadi 2 angka di belakang koma ---
                        val formattedJam = String.format("%.2f", item.jam)
                        drawText(textMeasurer, formattedJam, Offset(leftMargin + currentBarWidth + 8f, y + 2f), style = TextStyle(GlassAccentCyan, 10.sp, fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

// --- REVISI TEMA GRAPH TREEMAP (PER BAGIAN) ---
@Composable
fun SectionTreeMap(data: List<SectionDowntime>) {
    Card(
        modifier = Modifier.fillMaxWidth().height(250.dp).padding(top = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
        border = BorderStroke(1.dp, GlassBorder.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        if (data.isEmpty()) Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Tidak ada data", fontSize = 10.sp, color = GlassTextMuted) }
        else {
            Row(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                Box(modifier = Modifier.weight(0.6f).fillMaxHeight().padding(2.dp).clip(RoundedCornerShape(12.dp)).background(data[0].color.copy(alpha = 0.75f)).border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp)).padding(12.dp)) {
                    Column { Text(data[0].nama, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis); Text("${data[0].jam.toInt()} h", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp) }
                }
                Column(modifier = Modifier.weight(0.4f).fillMaxHeight()) {
                    data.drop(1).forEach { item ->
                        Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(2.dp).clip(RoundedCornerShape(8.dp)).background(item.color.copy(alpha = 0.75f)).border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp)).padding(8.dp)) {
                            Column { Text(item.nama, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text("${item.jam.toInt()} h", color = Color.White.copy(alpha = 0.8f), fontSize = 8.sp) }
                        }
                    }
                }
            }
        }
    }
}

// --- REVISI TEMA COMPONENT BAR CHART ---
@Composable
fun ComponentBarChart(data: List<ComponentDowntime>) {
    val textMeasurer = rememberTextMeasurer()
    Card(
        modifier = Modifier.fillMaxWidth().height(250.dp).padding(top = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GlassSurface),
        border = BorderStroke(1.dp, GlassBorder),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        if (data.isEmpty()) Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Tidak ada data", fontSize = 10.sp, color = GlassTextMuted) }
        else {
            Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                val canvasWidth = size.width; val canvasHeight = size.height - 60.dp.toPx(); val maxVal = (data.maxOfOrNull { it.jam } ?: 1f).coerceAtLeast(1f); val barWidth = (canvasWidth / (data.size * 2f)).coerceAtMost(32.dp.toPx())

                listOf(0.25f, 0.5f, 0.75f, 1f).forEach { p ->
                    val y = canvasHeight * p
                    drawLine(color = Color.White.copy(alpha = 0.05f), start = Offset(0f, y), end = Offset(canvasWidth, y), strokeWidth = 1f)
                }

                data.forEachIndexed { index, item ->
                    val barHeight = (item.jam / maxVal) * canvasHeight; val xOffset = (index * (canvasWidth / data.size)) + (barWidth / 2)
                    drawRoundRect(GlassAccentCyan, Offset(xOffset, canvasHeight - barHeight + 15.dp.toPx()), Size(barWidth, barHeight), CornerRadius(6.dp.toPx()))
                    drawText(textMeasurer, "${item.jam.toInt()}", Offset(xOffset + 2.dp.toPx(), canvasHeight - barHeight - 5.dp.toPx()), style = TextStyle(Color.White, 9.sp, fontWeight = FontWeight.Bold))
                    drawText(textMeasurer, item.nama.take(8), Offset(xOffset - 2.dp.toPx(), canvasHeight + 22.dp.toPx()), style = TextStyle(GlassTextMuted, 8.sp))
                }
            }
        }
    }
}