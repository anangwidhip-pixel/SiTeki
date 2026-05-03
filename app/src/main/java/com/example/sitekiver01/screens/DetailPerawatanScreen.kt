package com.example.sitekiver01.screens

import android.app.Activity
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.sitekiver01.ui.theme.OrbitronFontFamily
import com.example.sitekiver01.ui.theme.SiTekiVer01Theme
import com.example.sitekiver01.ui.theme.RajabesiDarkNavy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import com.example.sitekiver01.components.ModernFormCard
import com.example.sitekiver01.components.ModernSectionHeader

// --- DATA MODELS ---
data class JenisKpiData(val namaJenis: String, val persentase: Float)
data class MachineKpiData(val namaMesin: String, val persentase: Float)
data class RekapDataDetail(val bulan: String, val pencapaian: Float, val target: Float)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailPerawatanScreen(onBack: () -> Unit) {
    var selectedMonth by remember { mutableStateOf("Januari") }
    var selectedType by remember { mutableStateOf("Semua Jenis") }
    var expandedMonth by remember { mutableStateOf(false) }
    var expandedType by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    var jumlahArmada by remember { mutableIntStateOf(0) }
    var jumlahMesin by remember { mutableIntStateOf(0) }

    var jenisDataList by remember { mutableStateOf<List<JenisKpiData>>(emptyList()) }
    var machineDataList by remember { mutableStateOf<List<MachineKpiData>>(emptyList()) }
    var rekapTahunanList by remember { mutableStateOf<List<RekapDataDetail>>(emptyList()) }

    val availableMonths = listOf("Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember")
    val availableTypes = listOf("Semua Jenis", "Verloop", "Trailler", "Bevel", "Slitting", "Genset", "Forklift", "Kop", "Lakop", "Alat Uji", "Kompressor", "Pipa ERW", "Potong Bahan", "Perakitan", "Dump", "Mobile Crane", "Umum")

    val urlPencapaianTahunan = "https://script.google.com/macros/s/AKfycbyEO5MruO0r1StkK0iyEoQmfaa3iTZJDCAh4vg9-jdpqItGlt1yuPDe7orWDHwXRyU/exec"
    val urlDetailApi = "https://script.google.com/macros/s/AKfycbzxLaO2nEOxkUmBQnM0jAYzay7GGKBnOjhR3Afk9ZLUadK145ZdvOE-0NvIJ55EFKs/exec"

    LaunchedEffect(selectedMonth, selectedType) {
        isLoading = true
        try {
            withContext(Dispatchers.IO) {
                // 1. Data Tahunan (Statis)
                val resTahunan = URL(urlPencapaianTahunan).readText()
                val jsonTahunan = JSONArray(resTahunan)
                val tempTahunan = mutableListOf<RekapDataDetail>()
                for (i in 0 until jsonTahunan.length()) {
                    val obj = jsonTahunan.getJSONObject(i)
                    tempTahunan.add(RekapDataDetail(
                        bulan = obj.optString("bulan", ""),
                        pencapaian = (obj.optDouble("pencapaian", 0.0) * 100).toFloat(),
                        target = (obj.optDouble("target", 0.8) * 100).toFloat()
                    ))
                }
                rekapTahunanList = tempTahunan

                // 2. Data Detail (Dinamis)
                val resDetail = URL("$urlDetailApi?bulan=$selectedMonth&jenis=$selectedType").readText()
                val root = JSONObject(resDetail)
                jumlahArmada = root.optInt("total_jenis", 0)
                jumlahMesin = root.optInt("total_mesin", 0)

                // Parsing Jenis
                val jsonJenis = root.optJSONArray("data_per_jenis")
                val tempJenis = mutableListOf<JenisKpiData>()
                jsonJenis?.let {
                    for (i in 0 until it.length()) {
                        val obj = it.getJSONObject(i)
                        val nama = obj.optString("jenis", "")
                        if (nama != "Pengecatan") {
                            tempJenis.add(JenisKpiData(nama, obj.optDouble("pencapaian", 0.0).toFloat()))
                        }
                    }
                }
                jenisDataList = tempJenis

                // Parsing Mesin
                val jsonMesin = root.optJSONArray("data_per_mesin")
                val tempMesinRaw = mutableListOf<MachineKpiData>()
                jsonMesin?.let {
                    for (i in 0 until it.length()) {
                        val obj = it.getJSONObject(i)
                        val namaMesin = obj.optString("nama_mesin", "")
                        if (namaMesin != "Perlengkapan Cat") {
                            tempMesinRaw.add(MachineKpiData(namaMesin, obj.optDouble("pencapaian", 0.0).toFloat()))
                        }
                    }
                }
                machineDataList = if (selectedType == "Semua Jenis") tempMesinRaw.sortedBy { it.persentase }.take(10) else tempMesinRaw
            }
        } catch (e: Exception) {
            Log.e("DetailScreen", "Error: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    DetailPerawatanScreenContent(
        selectedMonth = selectedMonth,
        selectedType = selectedType,
        expandedMonth = expandedMonth,
        onExpandedMonthChange = { expandedMonth = it },
        expandedType = expandedType,
        onExpandedTypeChange = { expandedType = it },
        isLoading = isLoading,
        jumlahArmada = jumlahArmada,
        jumlahMesin = jumlahMesin,
        jenisDataList = jenisDataList,
        machineDataList = machineDataList,
        rekapTahunanList = rekapTahunanList,
        availableMonths = availableMonths,
        availableTypes = availableTypes,
        onMonthChange = { selectedMonth = it },
        onTypeChange = { selectedType = it },
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailPerawatanScreenContent(
    selectedMonth: String,
    selectedType: String,
    expandedMonth: Boolean,
    onExpandedMonthChange: (Boolean) -> Unit,
    expandedType: Boolean,
    onExpandedTypeChange: (Boolean) -> Unit,
    isLoading: Boolean,
    jumlahArmada: Int,
    jumlahMesin: Int,
    jenisDataList: List<JenisKpiData>,
    machineDataList: List<MachineKpiData>,
    rekapTahunanList: List<RekapDataDetail>,
    availableMonths: List<String>,
    availableTypes: List<String>,
    onMonthChange: (String) -> Unit,
    onTypeChange: (String) -> Unit,
    onBack: () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = RajabesiDarkNavy.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
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
            // Header Section
            ModernFormCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("PT. RAJA BESI", color = Color.Red, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, fontFamily = OrbitronFontFamily)
                        Text("MAINTENANCE PERFORMANCE - 2026", color = RajabesiDarkNavy, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatBoxDashboard("Armada", "$jumlahArmada")
                        StatBoxDashboard("Mesin", "$jumlahMesin")
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Dropdowns
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    ModernClickableField(selectedMonth, "Bulan") { onExpandedMonthChange(true) }
                    DropdownMenu(expanded = expandedMonth, onDismissRequest = { onExpandedMonthChange(false) }, modifier = Modifier.background(Color.White)) {
                        availableMonths.forEach { m -> DropdownMenuItem(text = { Text(m) }, onClick = { onMonthChange(m); onExpandedMonthChange(false) }) }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    ModernClickableField(selectedType, "Jenis") { onExpandedTypeChange(true) }
                    DropdownMenu(expanded = expandedType, onDismissRequest = { onExpandedTypeChange(false) }, modifier = Modifier.background(Color.White)) {
                        availableTypes.forEach { t -> DropdownMenuItem(text = { Text(t) }, onClick = { onTypeChange(t); onExpandedTypeChange(false) }) }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            ModernSectionHeader("PENCAPAIAN PERAWATAN (%)", Icons.Default.Analytics)
            PencapaianTahunanLineChart(data = rekapTahunanList)

            Spacer(modifier = Modifier.height(32.dp))

            ModernSectionHeader("PENCAPAIAN MENURUT JENIS", Icons.Default.Category)
            JenisBarChartLocal(data = jenisDataList)

            Spacer(modifier = Modifier.height(32.dp))

            val machineTitle = if(selectedType == "Semua Jenis") "10 NAMA MESIN TERENDAH" else "PENCAPAIAN NAMA MESIN"
            ModernSectionHeader(machineTitle, Icons.Default.Settings)
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = RajabesiDarkNavy) }
            } else {
                MachineBarChartLocal(data = machineDataList)
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun StatBoxDashboard(label: String, value: String) {
    Box(
        modifier = Modifier.size(width = 75.dp, height = 60.dp).background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp)).border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = Color(0xFF64748B), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(value, color = Color(0xFF2E7D32), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun MachineBarChartLocal(data: List<MachineKpiData>) {
    val textMeasurer = rememberTextMeasurer()
    Card(
        modifier = Modifier.fillMaxWidth().height(450.dp).padding(top = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val marginLeft = 15.dp.toPx(); val marginTop = 40.dp.toPx(); val marginBottom = 140.dp.toPx(); val marginRight = 15.dp.toPx()
            val chartWidth = size.width - marginLeft - marginRight; val chartHeight = size.height - marginTop - marginBottom
            listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { p ->
                val y = marginTop + chartHeight - (chartHeight * p)
                drawLine(color = Color.LightGray.copy(alpha = 0.3f), start = Offset(marginLeft, y), end = Offset(marginLeft + chartWidth, y), strokeWidth = 1f)
            }
            if (data.isNotEmpty()) {
                val barWidth = chartWidth / (data.size.coerceAtLeast(1) * 1.6f); val spacing = (chartWidth - (data.size * barWidth)) / (data.size + 1).coerceAtLeast(1)
                data.forEachIndexed { index, item ->
                    val x = marginLeft + spacing + index * (barWidth + spacing); val barHeight = (item.persentase / 100f).coerceIn(0f, 1f) * chartHeight
                    drawRect(color = Color(0xFF4CAF50), topLeft = Offset(x, marginTop + chartHeight - barHeight), size = Size(barWidth, barHeight))
                    drawText(textMeasurer, "${item.persentase.toInt()}%", Offset(x + (barWidth / 2) - 12.dp.toPx(), marginTop + chartHeight - barHeight - 15.dp.toPx()), style = TextStyle(color = Color.DarkGray, fontSize = 8.sp, fontWeight = FontWeight.Bold))
                    rotate(-90f, Offset(x + barWidth / 2, marginTop + chartHeight + 10.dp.toPx())) {
                        val label = if(item.namaMesin.length > 20) item.namaMesin.take(18)+".." else item.namaMesin
                        val textLayout = textMeasurer.measure(label, style = TextStyle(color = Color.DarkGray, fontSize = 8.sp))
                        drawText(textMeasurer, label, Offset(x + barWidth / 2 - textLayout.size.width - 10.dp.toPx(), marginTop + chartHeight + 5.dp.toPx()), style = TextStyle(color = Color.DarkGray, fontSize = 8.sp))
                    }
                }
            }
            val targetValue = 80f; val targetY = marginTop + chartHeight - (chartHeight * (targetValue / 100f))
            drawLine(color = Color(0xFFFFD700), start = Offset(marginLeft, targetY), end = Offset(marginLeft + chartWidth, targetY), strokeWidth = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f))
            val targetText = "Target 80%"; val targetPillStyle = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            val textLayoutResult = textMeasurer.measure(targetText, targetPillStyle)
            val pillWidth = textLayoutResult.size.width + 16f; val pillHeight = textLayoutResult.size.height + 8f
            drawRoundRect(color = Color(0xFFFFD700), topLeft = Offset(marginLeft + chartWidth - pillWidth, targetY - pillHeight - 5f), size = Size(pillWidth, pillHeight), cornerRadius = CornerRadius(4.dp.toPx()))
            drawText(textMeasurer, targetText, Offset(marginLeft + chartWidth - pillWidth + 8f, targetY - pillHeight - 2f), style = targetPillStyle)
        }
    }
}

@Composable
fun JenisBarChartLocal(data: List<JenisKpiData>) {
    val textMeasurer = rememberTextMeasurer()
    Card(modifier = Modifier.fillMaxWidth().height(420.dp).padding(top = 8.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val marginLeft = 15.dp.toPx(); val marginTop = 40.dp.toPx(); val marginBottom = 120.dp.toPx(); val marginRight = 15.dp.toPx()
            val chartWidth = size.width - marginLeft - marginRight; val chartHeight = size.height - marginTop - marginBottom
            listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { p ->
                val y = marginTop + chartHeight - (chartHeight * p)
                drawLine(color = Color.LightGray.copy(alpha = 0.3f), start = Offset(marginLeft, y), end = Offset(marginLeft + chartWidth, y), strokeWidth = 1f)
            }
            if (data.isNotEmpty()) {
                val barWidth = chartWidth / (data.size.coerceAtLeast(1) * 1.4f); val spacing = (chartWidth - (data.size * barWidth)) / (data.size + 1).coerceAtLeast(1)
                data.forEachIndexed { index, item ->
                    val x = marginLeft + spacing + index * (barWidth + spacing); val barHeight = (item.persentase / 100f).coerceIn(0f, 1f) * chartHeight
                    drawRect(Color(0xFF3B82F6), Offset(x, marginTop + chartHeight - barHeight), Size(barWidth, barHeight))
                    drawText(textMeasurer, "${item.persentase.toInt()}%", Offset(x + (barWidth / 2) - 12.dp.toPx(), marginTop + chartHeight - barHeight - 15.dp.toPx()), style = TextStyle(color = RajabesiDarkNavy, fontSize = 8.sp, fontWeight = FontWeight.Bold))
                    rotate(-90f, Offset(x + barWidth / 2, marginTop + chartHeight + 10.dp.toPx())) {
                        val textLayout = textMeasurer.measure(item.namaJenis, style = TextStyle(color = RajabesiDarkNavy, fontSize = 9.sp))
                        drawText(textMeasurer, item.namaJenis, Offset(x + barWidth / 2 - textLayout.size.width - 10.dp.toPx(), marginTop + chartHeight + 5.dp.toPx()), style = TextStyle(color = RajabesiDarkNavy, fontSize = 9.sp))
                    }
                }
            }
            val targetValue = 80f; val targetY = marginTop + chartHeight - (chartHeight * (targetValue / 100f))
            drawLine(color = Color(0xFFFFD700), start = Offset(marginLeft, targetY), end = Offset(marginLeft + chartWidth, targetY), strokeWidth = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f))
            val targetText = "Target 80%"; val targetPillStyle = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            val textLayoutResult = textMeasurer.measure(targetText, targetPillStyle)
            val pillWidth = textLayoutResult.size.width + 16f; val pillHeight = textLayoutResult.size.height + 8f
            drawRoundRect(color = Color(0xFFFFD700), topLeft = Offset(marginLeft + chartWidth - pillWidth, targetY - pillHeight - 5f), size = Size(pillWidth, pillHeight), cornerRadius = CornerRadius(4.dp.toPx()))
            drawText(textMeasurer, targetText, Offset(marginLeft + chartWidth - pillWidth + 8f, targetY - pillHeight - 2f), style = targetPillStyle)
        }
    }
}

@Composable
fun PencapaianTahunanLineChart(data: List<RekapDataDetail>) {
    val textMeasurer = rememberTextMeasurer(); val lineColor = Color(0xFF3B82F6); val targetColor = Color(0xFFBB00FF); val months = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
    Card(modifier = Modifier.fillMaxWidth().height(280.dp).padding(top = 8.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val marginLeft = 20.dp.toPx(); val marginRight = 20.dp.toPx(); val marginTop = 50.dp.toPx(); val marginBottom = 40.dp.toPx(); val chartWidth = size.width - marginLeft - marginRight; val chartHeight = size.height - marginTop - marginBottom
            if (chartWidth <= 0 || chartHeight <= 0) return@Canvas
            val spacing = chartWidth / 11f; val targetValue = 80f; val targetY = marginTop + chartHeight - (chartHeight * (targetValue / 100f))
            drawLine(color = targetColor, start = Offset(marginLeft, targetY), end = Offset(marginLeft + chartWidth, targetY), strokeWidth = 2f)
            val targetText = "Target 80%"; val targetPillStyle = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White); val textLayoutResult = textMeasurer.measure(targetText, targetPillStyle); val boxWidth = textLayoutResult.size.width + 24f; val boxHeight = textLayoutResult.size.height + 8f
            drawRoundRect(color = targetColor, topLeft = Offset(marginLeft + chartWidth - boxWidth, targetY - boxHeight - 5f), size = Size(boxWidth, boxHeight), cornerRadius = CornerRadius(4.dp.toPx()))
            drawText(textMeasurer, targetText, Offset(marginLeft + chartWidth - boxWidth + 12f, targetY - boxHeight - 2f), style = targetPillStyle)
            if (data.isNotEmpty()) {
                val points = (0..11).map { i -> val value = data.getOrNull(i)?.pencapaian ?: 0f; Offset(marginLeft + i * spacing, marginTop + chartHeight - (value / 100f * chartHeight)) }
                val fillPath = Path().apply { moveTo(points[0].x, marginTop + chartHeight); lineTo(points[0].x, points[0].y); for (i in 0 until points.size - 1) { val p1 = points[i]; val p2 = points[i + 1]; val conX = (p1.x + p2.x) / 2f; cubicTo(conX, p1.y, conX, p2.y, p2.x, p2.y) }; lineTo(points.last().x, marginTop + chartHeight); close() }
                drawPath(path = fillPath, brush = Brush.verticalGradient(colors = listOf(lineColor.copy(alpha = 0.2f), Color.Transparent), startY = points.minOf { it.y }, endY = marginTop + chartHeight))
                val strokePath = Path().apply { moveTo(points[0].x, points[0].y); for (i in 0 until points.size - 1) { val p1 = points[i]; val p2 = points[i + 1]; val conX = (p1.x + p2.x) / 2f; cubicTo(conX, p1.y, conX, p2.y, p2.x, p2.y) } }
                drawPath(strokePath, lineColor, style = Stroke(5f, cap = StrokeCap.Round))
                points.forEachIndexed { i, pt -> drawCircle(lineColor, 4f, pt); drawText(textMeasurer, months[i], Offset(pt.x - 10.dp.toPx(), marginTop + chartHeight + 15.dp.toPx()), style = TextStyle(color = Color.Gray, fontSize = 9.sp)); val percentValue = "${data.getOrNull(i)?.pencapaian?.toInt() ?: 0}%"; drawText(textMeasurer, percentValue, Offset(pt.x - 10.dp.toPx(), pt.y - 20.dp.toPx()), style = TextStyle(color = lineColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)) }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DetailPerawatanScreenPreview() { SiTekiVer01Theme { DetailPerawatanScreen(onBack = {}) } }

@Composable
fun ModernClickableField(value: String, placeholder: String, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = value.ifEmpty { placeholder }, color = if (value.isEmpty()) Color.Gray else Color.Black, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, null, tint = Color.Gray)
        }
    }
}
