package com.example.sitekiver01.screens

import android.util.Log
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sitekiver01.ui.theme.OrbitronFontFamily
import com.example.sitekiver01.ui.theme.RajabesiDarkNavy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import com.example.sitekiver01.components.ModernFormCard
import com.example.sitekiver01.components.ModernSectionHeader
import com.example.sitekiver01.components.ModernClickableField

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
        val colors = listOf(Color(0xFF1976D2), Color(0xFF42A5F5), Color(0xFFFFA000), Color(0xFF7B1FA2), Color(0xFF9C27B0))
        baseFilteredData.groupBy { it.bagian }.map { (name, list) -> name to list.sumOf { it.totalJam.toDouble() }.toFloat() }
            .sortedByDescending { it.second }.take(5).mapIndexed { index, pair -> SectionDowntime(pair.first, pair.second, colors.getOrElse(index) { Color.Gray }) }
    }

    Scaffold(
        topBar = {
            Surface(
                color = RajabesiDarkNavy,
                tonalElevation = 8.dp,
                modifier = Modifier.clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            ) {
                Row(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .fillMaxWidth()
                        .height(72.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBackIosNew, "Back", tint = Color.White) }
                    Spacer(Modifier.width(8.dp))
                    Text("DETAIL PERFORMANCE", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(brush = Brush.verticalGradient(colors = listOf(Color(0xFFF8FAFC), Color(0xFFF1F5F9))))
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            ModernFormCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("PT. RAJA BESI - KIC", color = Color.Red, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, fontFamily = OrbitronFontFamily)
                        Text("Downtime Mesin - 2026", color = RajabesiDarkNavy, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val totalNow = if (downtimeMonthly.isNotEmpty()) downtimeMonthly.sumOf { it.jam.toDouble() } / 1000 else 0.0
                        StatBoxDowntime("DownTime -> Now", "${String.format("%.1f", totalNow)} rb")
                        StatBoxDowntime("Target /Th", "6.000")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    ModernClickableField(selectedMonth, "Bulan") { expandedMonth = true }
                    DropdownMenu(expanded = expandedMonth, onDismissRequest = { expandedMonth = false }, modifier = Modifier.background(Color.White)) {
                        listOf("Semua Bulan", "Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember").forEach { m ->
                            DropdownMenuItem(text = { Text(m) }, onClick = { selectedMonth = m; expandedMonth = false })
                        }
                    }
                }
                Box(modifier = Modifier.weight(1f)) {
                    ModernClickableField(selectedType, "Jenis") { expandedType = true }
                    DropdownMenu(expanded = expandedType, onDismissRequest = { expandedType = false }, modifier = Modifier.background(Color.White)) {
                        allJenisOptions.forEach { t ->
                            DropdownMenuItem(text = { Text(t) }, onClick = { selectedType = t; expandedType = false })
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
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

            Spacer(modifier = Modifier.height(100.dp))
        }
        if (isLoading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = RajabesiDarkNavy) }
        }
    }
}

@Composable
fun DowntimeLineChart(data: List<DowntimeData>) {
    val textMeasurer = rememberTextMeasurer()
    val months = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
    val yellowLine = Color(0xFFFFD700)
    val targetRed = Color(0xFFFF0000)

    Card(modifier = Modifier.fillMaxWidth().height(300.dp).padding(top = 8.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 30.dp)) {
            val width = size.width; val height = size.height - 40.dp.toPx(); val maxVal = 1200f; val spacing = width / 11f; val targetJam = 500f; val targetY = height - (targetJam / maxVal * height)
            drawLine(color = targetRed, start = Offset(0f, targetY), end = Offset(width, targetY), strokeWidth = 2f)
            val targetText = "Target Max 500 Jam"; val targetPillStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            val textLayout = textMeasurer.measure(targetText, targetPillStyle); val boxWidth = textLayout.size.width + 24f; val boxHeight = textLayout.size.height + 10f; val boxX = width - boxWidth; val boxY = targetY - textLayout.size.height - 15f
            drawRoundRect(color = targetRed, topLeft = Offset(boxX, boxY), size = Size(boxWidth, boxHeight), cornerRadius = CornerRadius(4.dp.toPx()))
            drawText(textMeasurer, targetText, Offset(boxX + 12f, targetY - textLayout.size.height - 10f), style = targetPillStyle)
            if (data.isNotEmpty()) {
                val points = (0..11).map { index -> val value = data.find { it.bulan == months[index] }?.jam ?: 0f; Offset(index * spacing, height - (value / maxVal * height)) }
                val curvePath = Path().apply { moveTo(points[0].x, points[0].y); for (i in 0 until points.size - 1) { val p1 = points[i]; val p2 = points[i + 1]; cubicTo((p1.x + p2.x) / 2, p1.y, (p1.x + p2.x) / 2, p2.y, p2.x, p2.y) } }
                val fillPath = Path().apply { addPath(curvePath); lineTo(points.last().x, height); lineTo(points.first().x, height); close() }
                drawPath(path = fillPath, brush = Brush.verticalGradient(colors = listOf(yellowLine.copy(alpha = 0.3f), Color.Transparent), startY = points.minOf { it.y }, endY = height))
                drawPath(curvePath, yellowLine, style = Stroke(width = 6f, cap = StrokeCap.Round))
                points.forEachIndexed { i, pt -> drawText(textMeasurer, months[i], Offset(pt.x - 10.dp.toPx(), height + 15.dp.toPx()), style = TextStyle(color = Color.Gray, fontSize = 10.sp)); val jamValue = data.find { it.bulan == months[i] }?.jam ?: 0f; drawText(textMeasurer, jamValue.toInt().toString(), Offset(pt.x - 10.dp.toPx(), pt.y - 25.dp.toPx()), style = TextStyle(color = yellowLine, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)) }
            }
        }
    }
}

@Composable
fun StatBoxDowntime(label: String, value: String) {
    Box(modifier = Modifier.size(width = 85.dp, height = 60.dp).background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp)).border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = Color(0xFF64748B), fontSize = 8.sp, textAlign = TextAlign.Center, lineHeight = 10.sp, fontWeight = FontWeight.Bold)
            Text(value, color = Color(0xFF2E7D32), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun TopDowntimeHorizontalChart(data: List<MachineDowntime>) {
    val textMeasurer = rememberTextMeasurer(); val barColor = Color(0xFFC2185B)
    Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp), shape = RoundedCornerShape(16.dp)) {
        if (data.isEmpty()) { Box(Modifier.fillMaxWidth().height(200.dp), Alignment.Center) { Text("Tidak ada data", color = Color.Gray, fontSize = 12.sp) } }
        else {
            Column(modifier = Modifier.padding(16.dp)) {
                val maxVal = (data.maxOfOrNull { it.jam } ?: 1f).coerceAtLeast(1f)
                Canvas(modifier = Modifier.fillMaxWidth().height((data.size * 40 + 40).dp)) {
                    val chartWidth = size.width * 0.7f; val leftMargin = size.width * 0.25f; val barHeight = 20.dp.toPx(); val spacing = 40.dp.toPx()
                    for (i in 0..4) { val x = leftMargin + (i * chartWidth / 4); drawLine(color = Color.Black.copy(alpha = 0.1f), start = Offset(x, 0f), end = Offset(x, data.size * spacing), strokeWidth = 1f); drawText(textMeasurer, "${(i * maxVal / 4).toInt()}", Offset(x - 10f, data.size * spacing + 5f), style = TextStyle(Color.Gray, 10.sp)) }
                    data.forEachIndexed { index, item -> val y = index * spacing + 10f; val currentBarWidth = (item.jam / maxVal) * chartWidth; drawText(textMeasurer, item.nama.take(12), Offset(5f, y + 2f), style = TextStyle(Color.Black, 10.sp, textAlign = TextAlign.End), size = Size(leftMargin - 15f, barHeight)); drawRect(barColor, Offset(leftMargin, y), Size(currentBarWidth, barHeight)); drawText(textMeasurer, "${item.jam}", Offset(leftMargin + currentBarWidth + 5f, y + 2f), style = TextStyle(RajabesiDarkNavy, 10.sp, fontWeight = FontWeight.Bold)) }
                }
            }
        }
    }
}

@Composable
fun SectionTreeMap(data: List<SectionDowntime>) {
    Card(modifier = Modifier.fillMaxWidth().height(250.dp).padding(top = 8.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        if (data.isEmpty()) Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Tidak ada data", fontSize = 10.sp, color = Color.Gray) }
        else {
            Row(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                Box(modifier = Modifier.weight(0.6f).fillMaxHeight().padding(2.dp).clip(RoundedCornerShape(8.dp)).background(data[0].color).padding(12.dp)) {
                    Column { Text(data[0].nama, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis); Text("${data[0].jam.toInt()} h", color = Color.White, fontSize = 10.sp) }
                }
                Column(modifier = Modifier.weight(0.4f).fillMaxHeight()) { data.drop(1).forEach { item -> Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(2.dp).clip(RoundedCornerShape(8.dp)).background(item.color).padding(8.dp)) { Column { Text(item.nama, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text("${item.jam.toInt()} h", color = Color.White, fontSize = 8.sp) } } } }
            }
        }
    }
}

@Composable
fun ComponentBarChart(data: List<ComponentDowntime>) {
    val textMeasurer = rememberTextMeasurer()
    Card(modifier = Modifier.fillMaxWidth().height(250.dp).padding(top = 8.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        if (data.isEmpty()) Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Tidak ada data", fontSize = 10.sp, color = Color.Gray) }
        else {
            Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                val canvasWidth = size.width; val canvasHeight = size.height - 60.dp.toPx(); val maxVal = (data.maxOfOrNull { it.jam } ?: 1f).coerceAtLeast(1f); val barWidth = (canvasWidth / (data.size * 2f)).coerceAtMost(40.dp.toPx())
                data.forEachIndexed { index, item -> val barHeight = (item.jam / maxVal) * canvasHeight; val xOffset = (index * (canvasWidth / data.size)) + (barWidth / 2); drawRoundRect(Color(0xFF2196F3), Offset(xOffset, canvasHeight - barHeight + 15.dp.toPx()), Size(barWidth, barHeight), CornerRadius(4.dp.toPx())); drawText(textMeasurer, "${item.jam.toInt()}", Offset(xOffset, canvasHeight - barHeight - 5.dp.toPx()), style = TextStyle(RajabesiDarkNavy, 9.sp, fontWeight = FontWeight.Bold)); drawText(textMeasurer, item.nama.take(8), Offset(xOffset - 5.dp.toPx(), canvasHeight + 20.dp.toPx()), style = TextStyle(Color.Gray, 8.sp)) }
            }
        }
    }
}
