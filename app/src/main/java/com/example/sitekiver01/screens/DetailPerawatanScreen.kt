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
import androidx.compose.ui.draw.drawWithCache
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
import com.example.sitekiver01.components.ModernClickableField
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
import com.example.sitekiver01.ui.theme.*
import kotlin.math.sin

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

                val resDetail = URL("$urlDetailApi?bulan=$selectedMonth&jenis=$selectedType").readText()
                val root = JSONObject(resDetail)
                jumlahArmada = root.optInt("total_jenis", 0)
                jumlahMesin = root.optInt("total_mesin", 0)

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
            window.statusBarColor = GlassBase.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

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

            // Header
            ModernFormCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "PT. RAJA BESI",
                            color = GlassAccentCyan,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = OrbitronFontFamily
                        )
                        Text(
                            "MAINTENANCE PERFORMANCE - 2026",
                            color = GlassTextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatBoxDashboard("Armada", "$jumlahArmada")
                        StatBoxDashboard("Mesin", "$jumlahMesin")
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Dropdowns
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    ModernClickableField(selectedMonth, "Bulan") { onExpandedMonthChange(true) }
                    DropdownMenu(
                        expanded = expandedMonth,
                        onDismissRequest = { onExpandedMonthChange(false) },
                        modifier = Modifier
                            .background(GlassSurface)
                            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                    ) {
                        availableMonths.forEach { month ->
                            DropdownMenuItem(
                                text = { Text(month, color = Color.White, fontSize = 15.sp) },
                                onClick = {
                                    onMonthChange(month)
                                    onExpandedMonthChange(false)
                                }
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    ModernClickableField(selectedType, "Jenis") { onExpandedTypeChange(true) }
                    DropdownMenu(
                        expanded = expandedType,
                        onDismissRequest = { onExpandedTypeChange(false) },
                        modifier = Modifier
                            .background(GlassSurface)
                            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                    ) {
                        availableTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type, color = Color.White, fontSize = 15.sp) },
                                onClick = {
                                    onTypeChange(type)
                                    onExpandedTypeChange(false)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // ================== CHARTS ==================
            ModernSectionHeader("PENCAPAIAN PERAWATAN (%)", Icons.Default.Analytics)
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(320.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GlassAccentCyan)
                }
            } else {
                PencapaianTahunanLineChart(data = rekapTahunanList)
            }

            Spacer(Modifier.height(32.dp))

            ModernSectionHeader("PENCAPAIAN MENURUT JENIS", Icons.Default.Category)
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(460.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GlassAccentCyan)
                }
            } else {
                JenisBarChartLocal(data = jenisDataList)
            }

            Spacer(Modifier.height(32.dp))

            val machineTitle = if (selectedType == "Semua Jenis") "10 NAMA MESIN TERENDAH" else "PENCAPAIAN NAMA MESIN"
            ModernSectionHeader(machineTitle, Icons.Default.Settings)
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(460.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GlassAccentCyan)
                }
            } else {
                MachineBarChartLocal(data = machineDataList)
            }

            Spacer(Modifier.height(120.dp))
        }
    }
}

@Composable
fun StatBoxDashboard(label: String, value: String) {
    Box(
        modifier = Modifier
            .size(width = 78.dp, height = 62.dp)
            .background(GlassSurface, RoundedCornerShape(16.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                color = GlassTextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = value,
                color = GlassAccentCyan,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
fun ModernClickableField(
    value: String,
    placeholder: String,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GlassSurface.copy(alpha = 0.6f)),
        border = BorderStroke(1.dp, GlassBorder),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (value.isEmpty()) placeholder else value,
                color = if (value.isEmpty()) GlassTextMuted else Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = GlassAccentCyan, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun MachineBarChartLocal(data: List<MachineKpiData>) {
    val textMeasurer = rememberTextMeasurer()
    Card(
        modifier = Modifier.fillMaxWidth().height(450.dp).padding(top = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GlassSurface),
        border = BorderStroke(1.dp, GlassBorder),
        elevation = CardDefaults.cardElevation(0.dp)
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
                    drawText(textMeasurer, "${item.persentase.toInt()}%", Offset(x + (barWidth / 2) - 12.dp.toPx(), marginTop + chartHeight - barHeight - 15.dp.toPx()), style = TextStyle(color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold))
                    rotate(-90f, Offset(x + barWidth / 2, marginTop + chartHeight + 10.dp.toPx())) {
                        val label = if(item.namaMesin.length > 20) item.namaMesin.take(18)+".." else item.namaMesin
                        val textLayout = textMeasurer.measure(label, style = TextStyle(color = Color.White, fontSize = 8.sp))
                        drawText(textMeasurer, label, Offset(x + barWidth / 2 - textLayout.size.width - 10.dp.toPx(), marginTop + chartHeight + 5.dp.toPx()), style = TextStyle(color = Color.White, fontSize = 8.sp))
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
    Card(
        modifier = Modifier.fillMaxWidth().height(420.dp).padding(top = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.045f)),
        border = BorderStroke(1.dp, GlassBorder.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val marginLeft = 15.dp.toPx(); val marginTop = 40.dp.toPx(); val marginBottom = 120.dp.toPx(); val marginRight = 15.dp.toPx()
            val chartWidth = size.width - marginLeft - marginRight; val chartHeight = size.height - marginTop - marginBottom
            listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { p ->
                val y = marginTop + chartHeight - (chartHeight * p)
                drawLine(color = Color.White.copy(alpha = 0.08f), start = Offset(marginLeft, y), end = Offset(marginLeft + chartWidth, y), strokeWidth = 1f)
            }
            if (data.isNotEmpty()) {
                val barWidth = chartWidth / (data.size.coerceAtLeast(1) * 1.4f); val spacing = (chartWidth - (data.size * barWidth)) / (data.size + 1).coerceAtLeast(1)
                data.forEachIndexed { index, item ->
                    val x = marginLeft + spacing + index * (barWidth + spacing); val barHeight = (item.persentase / 100f).coerceIn(0f, 1f) * chartHeight
                    drawRect(GlassAccentCyan, Offset(x, marginTop + chartHeight - barHeight), Size(barWidth, barHeight))
                    drawText(textMeasurer, "${item.persentase.toInt()}%", Offset(x + (barWidth / 2) - 12.dp.toPx(), marginTop + chartHeight - barHeight - 15.dp.toPx()), style = TextStyle(color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold))
                    rotate(-90f, Offset(x + barWidth / 2, marginTop + chartHeight + 10.dp.toPx())) {
                        val textLayout = textMeasurer.measure(item.namaJenis, style = TextStyle(color = GlassTextMuted, fontSize = 9.sp))
                        drawText(textMeasurer, item.namaJenis, Offset(x + barWidth / 2 - textLayout.size.width - 10.dp.toPx(), marginTop + chartHeight + 5.dp.toPx()), style = TextStyle(color = GlassTextMuted, fontSize = 9.sp))
                    }
                }
            }
            val targetValue = 80f; val targetY = marginTop + chartHeight - (chartHeight * (targetValue / 100f))
            drawLine(color = Color(0xFFFFC107), start = Offset(marginLeft, targetY), end = Offset(marginLeft + chartWidth, targetY), strokeWidth = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f))
            val targetText = "Target 80%"; val targetPillStyle = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            val textLayoutResult = textMeasurer.measure(targetText, targetPillStyle)
            val pillWidth = textLayoutResult.size.width + 16f; val pillHeight = textLayoutResult.size.height + 8f
            drawRoundRect(color = Color(0xFFFFC107), topLeft = Offset(marginLeft + chartWidth - pillWidth, targetY - pillHeight - 5f), size = Size(pillWidth, pillHeight), cornerRadius = CornerRadius(4.dp.toPx()))
            drawText(textMeasurer, targetText, Offset(marginLeft + chartWidth - pillWidth + 8f, targetY - pillHeight - 2f), style = targetPillStyle)
        }
    }
}

@Composable
fun PencapaianTahunanLineChart(data: List<RekapDataDetail>) {
    val textMeasurer = rememberTextMeasurer()
    val lineColor = GlassAccentCyan
    val targetColor = Color(0xFFFF9F0A)
    Card(
        modifier = Modifier.fillMaxWidth().height(320.dp).padding(top = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = GlassSurface),
        border = BorderStroke(1.dp, GlassBorder),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (size.width <= 0 || size.height <= 0) return@Canvas
            val marginLeft = 40.dp.toPx(); val marginRight = 20.dp.toPx(); val marginTop = 40.dp.toPx(); val marginBottom = 60.dp.toPx()
            val chartWidth = size.width - marginLeft - marginRight; val chartHeight = size.height - marginTop - marginBottom
            listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { p ->
                val y = marginTop + chartHeight - (chartHeight * p)
                drawLine(color = Color.White.copy(alpha = 0.05f), start = Offset(marginLeft, y), end = Offset(marginLeft + chartWidth, y), strokeWidth = 1f)
            }
            val months = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
            if (data.isNotEmpty()) {
                val points = (0..11).map { i ->
                    val value = data.getOrNull(i)?.pencapaian ?: 0f
                    Offset(marginLeft + i * (chartWidth / 11f), marginTop + chartHeight - (chartHeight * (value / 100f)))
                }
                val fillPath = Path().apply {
                    moveTo(points[0].x, marginTop + chartHeight)
                    lineTo(points[0].x, points[0].y)
                    for (i in 0 until points.size - 1) {
                        val p1 = points[i]; val p2 = points[i + 1]; val conX = (p1.x + p2.x) / 2f
                        cubicTo(conX, p1.y, conX, p2.y, p2.x, p2.y)
                    }
                    lineTo(points.last().x, marginTop + chartHeight)
                    close()
                }
                drawPath(fillPath, brush = Brush.verticalGradient(colors = listOf(lineColor.copy(alpha = 0.18f), Color.Transparent)))
                val strokePath = Path().apply {
                    moveTo(points[0].x, points[0].y)
                    for (i in 0 until points.size - 1) {
                        val p1 = points[i]; val p2 = points[i + 1]; val conX = (p1.x + p2.x) / 2f
                        cubicTo(conX, p1.y, conX, p2.y, p2.x, p2.y)
                    }
                }
                drawPath(strokePath, lineColor, style = Stroke(5.5f, cap = StrokeCap.Round))
                points.forEachIndexed { i, pt ->
                    drawCircle(Color.White, 4.5f, pt)
                    drawCircle(lineColor, 3f, pt)
                    val percent = "${data.getOrNull(i)?.pencapaian?.toInt() ?: 0}%"
                    drawText(textMeasurer, percent, Offset(pt.x - 18.dp.toPx(), pt.y - 28.dp.toPx()), style = TextStyle(color = lineColor, fontSize = 10.sp, fontWeight = FontWeight.Bold))
                    drawText(textMeasurer, months[i], Offset(pt.x - 12.dp.toPx(), marginTop + chartHeight + 18.dp.toPx()), style = TextStyle(color = GlassTextMuted, fontSize = 9.sp))
                }
            }
            val targetY = marginTop + chartHeight - (chartHeight * 0.8f)
            drawLine(color = targetColor, start = Offset(marginLeft, targetY), end = Offset(marginLeft + chartWidth, targetY), strokeWidth = 2.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)))
            val targetText = "Target 80%"; val targetPillStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            val textLayout = textMeasurer.measure(targetText, targetPillStyle); val pillW = textLayout.size.width + 24f
            drawRoundRect(color = targetColor, topLeft = Offset(marginLeft + chartWidth - pillW - 8f, targetY - textLayout.size.height - 22f), size = Size(pillW, textLayout.size.height + 12f), cornerRadius = CornerRadius(8.dp.toPx()))
            drawText(textMeasurer, targetText, Offset(marginLeft + chartWidth - pillW + 4f, targetY - textLayout.size.height - 18f), style = targetPillStyle)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DetailPerawatanScreenPreview() {
    SiTekiVer01Theme {
        DetailPerawatanScreen(onBack = {})
    }
}