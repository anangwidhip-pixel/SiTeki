package com.example.sitekiver01.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sitekiver01.ui.theme.*
import com.example.sitekiver01.OrbitronFontFamily
import com.example.sitekiver01.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.util.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures


// --- 1. MODEL DATA ---
data class RekapData(val bulan: String, val pencapaian: Float, val target: Float)
data class CombinedSheetData(
    val bulan: String, val jam: Float, val target: Float,
    val order: Float, val bagus: Float, val cukup: Float, val tidakBagus: Float
)
data class KpiData(val month: String, val value: Float)

// --- 2. KONSTANTA WARNA UTAMA ---
val ChartBlue = Color(0xFF06B6D4) // SciFiCyan
val ChartYellow = Color(0xFFD97706) // SciFiSaturday Amber
val ChartTeal = Color(0xFF9D50BB) // SciFiPurple
val ChartBackgroundDark = Color.Transparent
val DividerGray = Color.White.copy(alpha = 0.08f)

// --- 3. HELPER ---
fun getNamaBulan(dateString: String): String {
    val bulanIndo = mapOf(
        "Jan" to "Januari", "Feb" to "Februari", "Mar" to "Maret",
        "Apr" to "April", "May" to "Mei", "Jun" to "Juni",
        "Jul" to "Juli", "Aug" to "Agustus", "Sep" to "September",
        "Oct" to "Oktober", "Nov" to "November", "Dec" to "Desember"
    )
    return bulanIndo[dateString] ?: dateString
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KPIScreen(
    onBack: () -> Unit,
    onNavigateToDetailPerawatan: () -> Unit,
    onNavigateToDetailDowntime: () -> Unit
) {
    var sheetRekap by remember { mutableStateOf<List<RekapData>>(emptyList()) }
    var sheetCombined by remember { mutableStateOf<List<CombinedSheetData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val urlPencapaian = "https://script.google.com/macros/s/AKfycbyEO5MruO0r1StkK0iyEoQmfaa3iTZJDCAh4vg9-jdpqItGlt1yuPDe7orWDHwXRyU/exec"
    val urlCombined = "https://script.google.com/macros/s/AKfycbxWrt_-ItPd_61v0uLh1oLn1g0l3v5ov9ApsQFKoNuq8r7OGQIT8yXyRytgx7RSvbM/exec"

    val fetchData = suspend {
        try {
            val resP = withContext(Dispatchers.IO) { URL(urlPencapaian).readText() }
            sheetRekap = JSONArray(resP).let { arr ->
                (0 until arr.length()).map { i ->
                    val obj = arr.getJSONObject(i)
                    RekapData(obj.getString("bulan"), (obj.optDouble("pencapaian", 0.0) * 100).toFloat(), (obj.optDouble("target", 0.8) * 100).toFloat())
                }
            }
            val resC = withContext(Dispatchers.IO) { URL(urlCombined).readText() }
            val jsonC = JSONObject(resC)
            val arrC = jsonC.getJSONArray("rekap")
            sheetCombined = (0 until arrC.length()).map { i ->
                val obj = arrC.getJSONObject(i)
                CombinedSheetData(
                    obj.getString("bulan"), obj.optDouble("jam", 0.0).toFloat(),
                    obj.optDouble("target", 500.0).toFloat(), obj.optDouble("order", 0.0).toFloat(),
                    obj.optDouble("bagus", 0.0).toFloat(), obj.optDouble("cukup", 0.0).toFloat(),
                    obj.optDouble("tidakBagus", 0.0).toFloat()
                )
            }
        } catch (e: Exception) { e.printStackTrace() }
        finally { isLoading = false; isRefreshing = false }
    }

    LaunchedEffect(Unit) { fetchData() }

    Box(modifier = Modifier.fillMaxSize().background(GlassBase)) {

        // PONDASI UTAMA: Menggunakan Animasi Grid Siber Global
        SciFiBackground()

        KPIScreenContent(
            sheetRekap = sheetRekap,
            sheetCombined = sheetCombined,
            isLoading = isLoading,
            isRefreshing = isRefreshing,
            onRefresh = { isRefreshing = true; scope.launch { fetchData() } },
            onBack = onBack,
            onNavigateToDetailPerawatan = onNavigateToDetailPerawatan,
            onNavigateToDetailDowntime = onNavigateToDetailDowntime
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KPIScreenContent(
    sheetRekap: List<RekapData>,
    sheetCombined: List<CombinedSheetData>,
    isLoading: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    onNavigateToDetailPerawatan: () -> Unit,
    onNavigateToDetailDowntime: () -> Unit
) {
    var activeTooltip by remember { mutableStateOf<Pair<String, Int>?>(null) }

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
                    ) { Icon(Icons.Default.ArrowBackIosNew, "Back", tint = Color.White) }
                    Spacer(Modifier.width(16.dp))
                    Text(
                        "KPI DASHBOARD",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        fontFamily = OrbitronFontFamily,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pointerInput(Unit) {
                    detectTapGestures { activeTooltip = null }
                }
        ) {
            if (isLoading && !isRefreshing) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SciFiCyan)
                }
            } else {
                Column(modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
                    .pointerInput(Unit) {
                        detectTapGestures { activeTooltip = null }
                    }
                ) {
                    Spacer(Modifier.height(20.dp))

                    // HEADER KARTU UTAMA PERUSAHAAN (GLASSMORPHIC)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = SciFiGlass,
                        border = BorderStroke(1.dp, Brush.verticalGradient(listOf(SciFiBorderLight, Color.Transparent)))
                    ) {
                        Row(modifier = Modifier.padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("PT. PABRIK BESI BETON RAJA BESI", color = Color.Red, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, fontFamily = OrbitronFontFamily)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("MAINTENANCE PERFORMANCE", color = SciFiTextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = OrbitronFontFamily)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                KPIBox(label = "Armada", value = "20")
                                KPIBox(label = "Mesin", value = "47")
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // SECTION 1: PERAWATAN
                    ModernSectionHeader("PERAWATAN (%)", Icons.Default.Engineering)
                    KpiLineChart(
                        chartId = "CHART_PERAWATAN",
                        activeTooltip = activeTooltip,
                        onTooltipChange = { activeTooltip = it },
                        data = sheetRekap.map { KpiData(getNamaBulan(it.bulan), it.pencapaian) },
                        targetValue = sheetRekap.firstOrNull()?.target ?: 80f,
                        lineColor = SciFiCyan, unitName = "% Pencapaian", isPercentage = true,
                        onDetailClick = onNavigateToDetailPerawatan
                    )

                    Spacer(Modifier.height(24.dp))

                    // SECTION 2: DOWNTIME
                    ModernSectionHeader("DOWNTIME (JAM)", Icons.Default.Timer)
                    KpiLineChart(
                        chartId = "CHART_DOWNTIME",
                        activeTooltip = activeTooltip,
                        onTooltipChange = { activeTooltip = it },
                        data = sheetCombined.map { KpiData(getNamaBulan(it.bulan), it.jam) },
                        targetValue = sheetCombined.firstOrNull()?.target ?: 500f,
                        lineColor = SciFiSaturday, targetColor = Color.Red, isPercentage = false, maxValue = 1000f, unitName = "Total Jam",
                        onDetailClick = onNavigateToDetailDowntime
                    )

                    Spacer(Modifier.height(24.dp))

                    // SECTION 3: JML ORDER
                    ModernSectionHeader("JUMLAH ORDER (UNIT)", Icons.Default.AddShoppingCart)
                    KpiLineChart(
                        chartId = "CHART_ORDER",
                        activeTooltip = activeTooltip,
                        onTooltipChange = { activeTooltip = it },
                        data = sheetCombined.map { KpiData(getNamaBulan(it.bulan), it.order) },
                        targetValue = 0f, lineColor = SciFiPurple, isPercentage = false, maxValue = 350f, unitName = "Jumlah Order",
                        onDetailClick = {}
                    )

                    Spacer(Modifier.height(24.dp))

                    // SECTION 4: KUALITAS TABEL
                    ModernSectionHeader("KUALITAS PELAYANAN", Icons.Default.Stars)
                    KwalitasPelayananTable(sheetCombined)

                    Spacer(Modifier.height(40.dp))
                }
            }
        }
    }
}

@Composable
fun KpiLineChart(
    chartId: String,
    activeTooltip: Pair<String, Int>?,
    onTooltipChange: (Pair<String, Int>?) -> Unit,
    data: List<KpiData>,
    targetValue: Float,
    lineColor: Color = SciFiCyan,
    targetColor: Color = SciFiSaturday,
    isPercentage: Boolean = true,
    maxValue: Float = 100f,
    unitName: String = "",
    onDetailClick: () -> Unit = {}
) {
    val textMeasurer = rememberTextMeasurer()
    val selectedIndex = if (activeTooltip?.first == chartId) activeTooltip.second else null
    val labelStyle = TextStyle(fontSize = 10.sp, color = SciFiTextMuted)
    val valueStyle = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = lineColor)

    Box(modifier = Modifier.fillMaxWidth().height(320.dp)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(310.dp)
                .pointerInput(Unit) {
                    detectTapGestures { onTooltipChange(null) }
                },
            shape = RoundedCornerShape(24.dp),
            color = SciFiGlass, // Menggunakan warna kaca transparan siber
            border = BorderStroke(1.dp, Brush.verticalGradient(listOf(SciFiBorderLight, Color.Transparent)))
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "Detail >",
                        style = TextStyle(fontSize = 12.sp, color = SciFiCyan, fontWeight = FontWeight.Bold, fontFamily = OrbitronFontFamily),
                        modifier = Modifier
                            .clickable { onDetailClick() }
                            .padding(4.dp)
                    )
                }

                Canvas(modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(data) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val marginLeft = 15.dp.toPx()
                                val chartWidth = size.width - marginLeft - 15.dp.toPx()
                                val spacing = chartWidth / (if (data.size > 1) data.size - 1 else 1)
                                val index = ((offset.x - marginLeft) / spacing).toInt().coerceIn(0, data.size - 1)
                                onTooltipChange(Pair(chartId, index))
                            },
                            onDrag = { change, _ ->
                                val marginLeft = 15.dp.toPx()
                                val chartWidth = size.width - marginLeft - 15.dp.toPx()
                                val spacing = chartWidth / (if (data.size > 1) data.size - 1 else 1)
                                val index = ((change.position.x - marginLeft) / spacing).toInt().coerceIn(0, data.size - 1)
                                onTooltipChange(Pair(chartId, index))
                            },
                            onDragEnd = { },
                            onDragCancel = { }
                        )
                    }
                    .pointerInput(data) {
                        detectTapGestures { offset ->
                            val marginLeft = 15.dp.toPx()
                            val chartWidth = size.width - marginLeft - 15.dp.toPx()
                            val spacing = chartWidth / (if (data.size > 1) data.size - 1 else 1)
                            val index = ((offset.x - marginLeft) / spacing).toInt().coerceIn(0, data.size - 1)
                            onTooltipChange(Pair(chartId, index))
                        }
                    }
                ) {
                    if (size.width <= 0 || size.height <= 0) return@Canvas
                    val marginLeft = 20.dp.toPx()
                    val marginRight = 20.dp.toPx()
                    val chartWidth = size.width - marginLeft - marginRight
                    val chartHeight = size.height - 110.dp.toPx()
                    val marginTop = 20.dp.toPx()
                    val spacing = chartWidth / (if (data.size > 1) data.size - 1 else 1)

                    listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { p ->
                        val y = marginTop + chartHeight - (chartHeight * p)
                        drawLine(Color.White.copy(0.04f), Offset(marginLeft, y), Offset(marginLeft + chartWidth, y))
                    }

                    val tY = marginTop + chartHeight - (chartHeight * (targetValue / maxValue))
                    if (targetValue > 0) {
                        drawLine(targetColor, Offset(marginLeft, tY), Offset(marginLeft + chartWidth, tY), 3f)
                        val targetText = if (isPercentage) "Target ${targetValue.toInt()}%" else "Target Max ${targetValue.toInt()} Jam"
                        val pillBgColor = if (!isPercentage) Color(0xFFC23B22) else targetColor
                        val pillTextColor = if (!isPercentage) Color.White else Color.Black
                        val targetPillStyle = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = pillTextColor, fontFamily = OrbitronFontFamily)
                        val textLayout = textMeasurer.measure(targetText, targetPillStyle)
                        val boxWidth = textLayout.size.width + 20f
                        val boxX = marginLeft + chartWidth - boxWidth
                        drawRoundRect(pillBgColor, Offset(boxX, tY - textLayout.size.height - 12f), Size(boxWidth, textLayout.size.height + 8f), CornerRadius(6.dp.toPx()))
                        drawText(textMeasurer, targetText, Offset(boxX + 10f, tY - textLayout.size.height - 8f), style = targetPillStyle)
                    }

                    val points = data.mapIndexed { i, d -> Offset(marginLeft + (i * spacing), marginTop + chartHeight - (chartHeight * (d.value / maxValue))) }
                    if (points.size > 1) {
                        val strokePath = Path().apply {
                            moveTo(points[0].x, points[0].y)
                            for (i in 0 until points.size - 1) {
                                val p1 = points[i]
                                val p2 = points[i + 1]
                                val conX = (p1.x + p2.x) / 2f
                                cubicTo(conX, p1.y, conX, p2.y, p2.x, p2.y)
                            }
                        }
                        drawPath(strokePath, lineColor, style = Stroke(5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                        val fillPath = Path().apply { addPath(strokePath); lineTo(points.last().x, marginTop + chartHeight); lineTo(points.first().x, marginTop + chartHeight); close() }
                        drawPath(fillPath, brush = Brush.verticalGradient(listOf(lineColor.copy(0.12f), Color.Transparent)))
                    }

                    selectedIndex?.let { index ->
                        val pt = points[index]
                        drawLine(
                            color = Color.White.copy(0.15f),
                            start = Offset(pt.x, marginTop),
                            end = Offset(pt.x, marginTop + chartHeight),
                            strokeWidth = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                        )
                        drawCircle(Color.White, 10f, pt)
                        drawCircle(lineColor, 6f, pt)
                    }

                    points.forEachIndexed { i, pt ->
                        val staticVal = "${data[i].value.toInt()}${if (isPercentage) "%" else ""}"
                        drawText(textMeasurer, staticVal, Offset(pt.x - 15f, pt.y - 35f), style = valueStyle)
                        drawCircle(Color.White, 5f, pt)
                        drawCircle(lineColor, 3f, pt)
                        drawText(textMeasurer, data[i].month.take(3), Offset(pt.x - 12.dp.toPx(), marginTop + chartHeight + 15.dp.toPx()), style = labelStyle)
                    }
                }
            }
        }

        // TOOLTIP DETECT POP-UP (GLASSMORPHIC THEME)
        selectedIndex?.let { index ->
            val item = data[index]
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
                    .width(165.dp),
                color = Color(0xFF0F172A),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, lineColor.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(getNamaBulan(item.month), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White, fontFamily = OrbitronFontFamily)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(lineColor, CircleShape))
                            Spacer(Modifier.width(6.dp))
                            Text(unitName, fontSize = 10.sp, color = SciFiTextMuted)
                        }
                        val valueString = if (unitName == "Jumlah Order") item.value.toInt().toString() else String.format(Locale.US, "%.1f", item.value)
                        Text(text = valueString, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = Color.White, fontFamily = OrbitronFontFamily)
                    }
                }
            }
        }
    }
}

@Composable
fun KPIBox(label: String, value: String) {
    Surface(
        modifier = Modifier.size(width = 80.dp, height = 60.dp),
        color = Color.White.copy(alpha = 0.04f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, SciFiBorderLight)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(text = label, color = SciFiTextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = OrbitronFontFamily)
            Text(text = value, color = Color(0xFF10B981), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, fontFamily = OrbitronFontFamily)
        }
    }
}

@Composable
fun KwalitasPelayananTable(data: List<CombinedSheetData>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = SciFiGlass,
        border = BorderStroke(1.dp, Brush.verticalGradient(listOf(SciFiBorderLight, Color.Transparent)))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("BULAN", color = SciFiCyan, modifier = Modifier.weight(1.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = OrbitronFontFamily)
                Text("BAGUS", color = SciFiCyan, modifier = Modifier.weight(1.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = OrbitronFontFamily)
                Text("CUKUP", color = SciFiCyan, modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = OrbitronFontFamily)
                Text("TDK BGS", color = SciFiCyan, modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = OrbitronFontFamily)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = SciFiBorderLight)
            data.forEachIndexed { index, item ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${index + 1}. ${getNamaBulan(item.bulan)}", color = Color.White, modifier = Modifier.weight(1.5f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Row(modifier = Modifier.weight(1.5f), verticalAlignment = Alignment.CenterVertically) {
                        Text(item.bagus.toInt().toString(), color = Color.White, fontSize = 12.sp, modifier = Modifier.width(30.dp), fontWeight = FontWeight.Bold)
                        val barWidth = (item.bagus / 350f).coerceIn(0f, 1f)
                        Box(modifier = Modifier.fillMaxWidth(barWidth).height(8.dp).background(SciFiSaturday, RoundedCornerShape(4.dp)))
                    }
                    Text(item.cukup.toInt().toString(), color = Color.White, modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center, fontSize = 12.sp)
                    Text(item.tidakBagus.toInt().toString(), color = Color.White, modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center, fontSize = 12.sp)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun KPIScreenPreview() {
    val sampleRekapData = listOf(RekapData("Jan", 85f, 80f), RekapData("Feb", 78f, 80f))
    val sampleCombinedData = listOf(CombinedSheetData("Jan", 450f, 500f, 300f, 250f, 40f, 10f))
    SiTekiVer01Theme {
        KPIScreenContent(sheetRekap = sampleRekapData, sheetCombined = sampleCombinedData, isLoading = false, isRefreshing = false, onRefresh = {}, onBack = {}, onNavigateToDetailPerawatan = {}, onNavigateToDetailDowntime = {})
    }
}