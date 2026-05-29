package com.example.sitekiver01.screens

import android.util.Log
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

data class ActualRecord(
    val nama_mesin: String,
    val tanggal: String,
    val jenis_perawatan: String,
    val jenis: String
)

@Composable
fun JadPerawatanScreen(
    onBack: () -> Unit,
    onNavigateToWebView: (String, String) -> Unit = { _, _ -> },
    onNavigateToIsiPerawatan: (String, String, String) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val calendar = Calendar.getInstance()

    var selectedMonth by remember { mutableIntStateOf(calendar.get(Calendar.MONTH)) }
    var selectedYear by remember { mutableIntStateOf(calendar.get(Calendar.YEAR)) }
    var actualRecords by remember { mutableStateOf<List<ActualRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var expandedMonth by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val months = listOf("Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember")

    val order = listOf(
        "Mobile Crane B", "Truck Dump DT39", "Truck Trailler H1983HG", "Mobile Crane C",
        "Forklift A FD35", "Truck Dump DT52", "Truck Trailler H8318QO", "Mobile Crane D",
        "Truck Dump DT55", "Panther H8629JA", "Truck Trailler H8696OA", "Mobile Crane E",
        "Truck Dump DT98", "Truck Trailler H8697OA", "Panther H1669SQ", "Mobile Crane A",
        "Truck Dump DT139", "Truck Trailler H1358KS", "Forklift B FD250", "Suzuki APV",
        "Kop C", "Kop D", "Kop E", "Kop F", "Perlengkapan Cat", "Line 13", "Line 14", "Line 15",
        "Lakop D", "Slitting", "Line 16", "Line 17", "Line 18", "Tes Bending TELKOM",
        "Tes Jatuh Telkom", "Bevel", "Tes Bending PLN", "Verloop D", "Verloop H", "Verloop E",
        "Verloop F", "Kompressor 01", "Line 01", "Line 02", "Line 03", "Verloop A", "Lakop A",
        "Kop A", "Kop B", "Kompressor 02", "Line 04", "Line 05", "Line 06", "Verloop B",
        "Verloop C", "Lakop B", "Lakop C", "Line 07", "Line 08", "Line 09", "Pipa ERW",
        "Verloop G", "Potong Bahan A", "Potong Spiral", "Genset 01", "Line 10", "Line 11", "Line 12"
    )

    val fetchData = suspend {
        isLoading = true
        errorMessage = null
        withContext(Dispatchers.IO) {
            try {
                val url = URL("https://script.google.com/macros/s/AKfycbwQ7ocBNsl4x5-rGLrSyvkyluhSRl3B_LvmkA3cFuvuL9pBbVAOUI3i_Vu6jwfkfOA/exec?action=getPerawatan")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.instanceFollowRedirects = true

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONArray(response)
                    val list = mutableListOf<ActualRecord>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        list.add(ActualRecord(
                            nama_mesin = obj.optString("nama_mesin"),
                            tanggal = obj.optString("tanggal"),
                            jenis_perawatan = obj.optString("jenis_perawatan"),
                            jenis = obj.optString("jenis")
                        ))
                    }
                    actualRecords = list
                } else {
                    errorMessage = "Server Error: $responseCode"
                }
            } catch (e: Exception) {
                Log.e("JadPerawatan", "Error: ${e.message}")
                errorMessage = "Gagal memuat data: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { fetchData() }

    val machinesWithJenis = order.map { name ->
        val item = actualRecords.find { it.nama_mesin == name }
        (item?.jenis ?: "UMUM") to name
    }

    Box(modifier = Modifier.fillMaxSize().background(GlassBase)) {

        // PONDASI UTAMA: Background Mesh Grid Animasi Global
        SciFiBackground()

        JadPerawatanContent(
            selectedMonth = selectedMonth,
            selectedYear = selectedYear,
            machinesList = machinesWithJenis,
            actualRecords = actualRecords,
            isLoading = isLoading,
            expandedMonth = expandedMonth,
            errorMessage = errorMessage,
            months = months,
            onBack = onBack,
            onMonthClick = { expandedMonth = true },
            onMonthDismiss = { expandedMonth = false },
            onMonthSelect = { index ->
                selectedMonth = index
                expandedMonth = false
                scope.launch { fetchData() }
            },
            onMachineClick = { machine ->
                val url = getPerawatanUrl(machine.first)
                onNavigateToWebView(url, machine.first)
            },
            onCellClick = onNavigateToIsiPerawatan,
            modifier = modifier
        )
    }
}

@Composable
fun JadPerawatanContent(
    selectedMonth: Int,
    selectedYear: Int,
    machinesList: List<Pair<String, String>>,
    actualRecords: List<ActualRecord>,
    isLoading: Boolean,
    expandedMonth: Boolean,
    errorMessage: String?,
    months: List<String>,
    onBack: () -> Unit,
    onMonthClick: () -> Unit,
    onMonthDismiss: () -> Unit,
    onMonthSelect: (Int) -> Unit,
    onMachineClick: (Pair<String, String>) -> Unit,
    onCellClick: (String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Surface(color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
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
                        "JADWAL PERAWATAN",
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
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(12.dp))
            Text("PENCARIAN DATA METRIC", color = SciFiCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = OrbitronFontFamily)

            // PANEL FILTER BULAN DAN TAHUN (GLASSMORPHIC)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = SciFiGlass,
                border = BorderStroke(1.dp, Brush.verticalGradient(listOf(SciFiBorderLight, Color.Transparent)))
            ) {
                Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        Surface(
                            modifier = Modifier
                                .height(42.dp)
                                .fillMaxWidth()
                                .clickable { onMonthClick() },
                            border = BorderStroke(1.dp, SciFiBorderLight),
                            shape = RoundedCornerShape(21.dp),
                            color = Color.White.copy(alpha = 0.03f)
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = months[selectedMonth],
                                    modifier = Modifier.weight(1f),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                                Icon(Icons.Default.ArrowDropDown, null, tint = SciFiTextMuted, modifier = Modifier.size(20.dp))
                            }
                        }

                        DropdownMenu(
                            expanded = expandedMonth,
                            onDismissRequest = { onMonthDismiss() },
                            modifier = Modifier.background(Color(0xFF0F172A)).border(1.dp, SciFiBorderLight)
                        ) {
                            months.forEachIndexed { index, name ->
                                DropdownMenuItem(
                                    text = { Text(name, fontSize = 12.sp, color = Color.White) },
                                    onClick = { onMonthSelect(index) }
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.width(100.dp)) {
                        Surface(
                            modifier = Modifier
                                .height(42.dp)
                                .fillMaxWidth(),
                            border = BorderStroke(1.dp, SciFiBorderLight),
                            shape = RoundedCornerShape(21.dp),
                            color = Color.White.copy(alpha = 0.03f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = selectedYear.toString(), color = Color.White, fontSize = 12.sp, fontFamily = OrbitronFontFamily)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // LEGENDA INDIKATOR PLAN & ACTUAL
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendItem(Color(0xFF10B981), "Plan (M)") // SciFiStatusM Green
                LegendItem(Color(0xFF2563EB), "Plan (B)") // SciFiBlue
                Text("✓ Actual", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = OrbitronFontFamily)
            }

            Spacer(Modifier.height(4.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    color = SciFiCyan.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                    border = BorderStroke(1.dp, SciFiCyan.copy(alpha = 0.3f))
                ) {
                    Text(
                        "DATA JADWAL PERAWATAN",
                        color = SciFiCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = OrbitronFontFamily,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // STRUKTUR UTAMA TABEL JADWAL (GLASSMORPHIC BOX)
            Surface(
                modifier = modifier.weight(1f).fillMaxWidth(),
                color = SciFiGlass,
                border = BorderStroke(1.dp, Brush.verticalGradient(listOf(SciFiBorderLight, Color.Transparent))),
                shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp, topEnd = 12.dp)
            ) {
                if (isLoading) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = SciFiCyan) }
                } else if (errorMessage != null) {
                    Box(Modifier.fillMaxSize().padding(16.dp), Alignment.Center) {
                        Text(errorMessage, color = Color(0xFFC23B22), textAlign = TextAlign.Center, fontSize = 12.sp)
                    }
                } else {
                    MaintenanceStickyTable(machinesList, actualRecords, selectedMonth, selectedYear, onMachineClick, onCellClick)
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size(12.dp).background(color, RoundedCornerShape(2.dp)))
        Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun MaintenanceStickyTable(
    machines: List<Pair<String, String>>,
    actualRecords: List<ActualRecord>,
    month: Int,
    year: Int,
    onMachineClick: (Pair<String, String>) -> Unit,
    onCellClick: (String, String, String) -> Unit
) {
    val calendar = Calendar.getInstance().apply { set(year, month, 1) }
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val today = Calendar.getInstance()
    val isCurrentMonth = today.get(Calendar.MONTH) == month && today.get(Calendar.YEAR) == year
    val todayDate = today.get(Calendar.DAY_OF_MONTH)

    val scrollState = rememberScrollState()
    val nameColumnWidth = 130.dp
    val dayCellWidth = 44.dp
    val headerHeight = 56.dp
    val dataRowHeight = 40.dp

    Column(modifier = Modifier.fillMaxSize()) {
        // HEADER ROW 1 (Dates / Hari)
        Row(modifier = Modifier.fillMaxWidth().background(SciFiCyan.copy(alpha = 0.08f))) {
            Box(
                modifier = Modifier
                    .size(nameColumnWidth, headerHeight)
                    .border(0.5.dp, SciFiBorderLight.copy(alpha = 0.3f))
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text("Mesin / Tanggal", color = SciFiCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = OrbitronFontFamily)
            }

            Row(modifier = Modifier.horizontalScroll(scrollState)) {
                for (i in 1..daysInMonth) {
                    val isSun = isSunday(i, month, year)
                    val isSat = isSaturday(i, month, year)
                    val isHoliday = isIndonesianHoliday(i, month, year)
                    val isToday = isCurrentMonth && i == todayDate

                    val bgColor = when {
                        isToday -> SciFiCyan.copy(alpha = 0.35f)
                        isSun || isHoliday -> Color(0xFFC23B22).copy(alpha = 0.4f) // SciFiHoliday Red
                        isSat -> Color(0xFFD97706).copy(alpha = 0.4f) // SciFiSaturday Amber
                        else -> Color.Transparent
                    }
                    val textColor = Color.White

                    Box(
                        modifier = Modifier
                            .width(dayCellWidth)
                            .height(headerHeight)
                            .background(bgColor)
                            .border(0.5.dp, SciFiBorderLight.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(getDayNameShort(i, month, year), color = if (isToday) SciFiCyan else textColor.copy(alpha = 0.6f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(i.toString(), color = if (isToday) SciFiCyan else textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = OrbitronFontFamily)
                        }
                    }
                }
            }
        }

        // HEADER ROW 2 (P / A Plan & Actual Channels)
        Row(modifier = Modifier.fillMaxWidth().background(SciFiCyan.copy(alpha = 0.03f))) {
            Box(
                modifier = Modifier
                    .size(nameColumnWidth, 24.dp)
                    .border(0.5.dp, SciFiBorderLight.copy(alpha = 0.3f)),
                contentAlignment = Alignment.CenterStart
            ) {}

            Row(modifier = Modifier.horizontalScroll(scrollState)) {
                for (i in 1..daysInMonth) {
                    Row(modifier = Modifier.width(dayCellWidth)) {
                        Box(modifier = Modifier.weight(1f).height(24.dp).border(0.5.dp, SciFiBorderLight.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                            Text("P", color = SciFiCyan.copy(alpha = 0.8f), fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = OrbitronFontFamily)
                        }
                        Box(modifier = Modifier.weight(1f).height(24.dp).border(0.5.dp, SciFiBorderLight.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                            Text("A", color = Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = OrbitronFontFamily)
                        }
                    }
                }
            }
        }

        // BODY ROWS (Daftar Mesin Dinamis)
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(machines) { index, machine ->
                val rowBgColor = if (index % 2 != 0) Color.White.copy(alpha = 0.01f) else Color.Transparent

                Row(modifier = Modifier
                    .fillMaxWidth()
                    .background(rowBgColor)
                ) {
                    Box(
                        modifier = Modifier
                            .size(nameColumnWidth, dataRowHeight)
                            .border(0.5.dp, SciFiBorderLight.copy(alpha = 0.3f))
                            .clickable { onMachineClick(machine) }
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(machine.second, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    Row(modifier = Modifier.horizontalScroll(scrollState)) {
                        for (i in 1..daysInMonth) {
                            val status = getMaintenanceStatus(machine.second, i, month, year)
                            val dateKey = String.format(Locale.US, "%02d/%02d/%d", i, month + 1, year)
                            val actualEntry = actualRecords.find { it.nama_mesin == machine.second && it.tanggal.trim() == dateKey }

                            var actualMark = ""
                            if (actualEntry != null) {
                                actualMark = if (actualEntry.jenis_perawatan == "B") "✓✓" else "✓"
                            }

                            val pBgColor = when (status) {
                                "M" -> Color(0xFF10B981) // SciFiStatusM
                                "B" -> Color(0xFF2563EB) // SciFiBlue
                                else -> Color.Transparent
                            }

                            val aBgColor = when (actualMark) {
                                "✓✓" -> Color(0xFF2563EB).copy(alpha = 0.15f)
                                "✓" -> Color(0xFF10B981).copy(alpha = 0.15f)
                                else -> Color.Transparent
                            }
                            val aTextColor = if (actualMark == "✓✓") Color(0xFF2563EB) else Color(0xFF10B981)

                            Row(modifier = Modifier
                                .width(dayCellWidth)
                                .height(dataRowHeight)
                                .clickable { onCellClick(machine.second, dateKey, status) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .background(pBgColor)
                                        .border(0.5.dp, SciFiBorderLight.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (status.isNotEmpty()) {
                                        Text(status, color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .background(aBgColor)
                                        .border(0.5.dp, SciFiBorderLight.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (actualMark.isNotEmpty()) {
                                        Text(actualMark, color = aTextColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun getPerawatanUrl(jenis: String): String {
    return when (jenis.uppercase(Locale.US)) {
        "MOBILE CRANE" -> "https://forms.gle/9oeGnfNFH7NctVSL6"
        "FORKLIFT" -> "https://forms.gle/Je8ErK1qeXrgP5rz8"
        "TRAILLER" -> "https://forms.gle/MYqeNrEjUBqHfZwF7"
        "UMUM" -> "https://forms.gle/1xRJ95qbFvMHYP7e9"
        "DUMP TRUCK" -> "https://forms.gle/6xrAHKmjTsiXNHFK9"
        "ALAT UJI" -> "https://forms.gle/JgK6rWyN1pwFoUJB7"
        "BEVEL" -> "https://forms.gle/Ay4jcffwEmmQkYDv6"
        "GENSET" -> "https://forms.gle/Qag9jZr5o4LGMHi58"
        "KOMPRESSOR" -> "https://forms.gle/ggni6sJT5NSa9X2b9"
        "PERAKITAN" -> "https://forms.gle/fiGKBtoiSQFo3cTc8"
        "LAKOP" -> "https://forms.gle/TkddC4kQBAQwqjyu8"
        "KOP" -> "https://forms.gle/SvpGjLeoKneRjT6w9"
        "PIPA ERW" -> "https://forms.gle/8YTgW2oC1SvoiRnT8"
        "POTONG BAHAN" -> "https://forms.gle/ugTC9Ywu1di4dZ4i7"
        "SLITTING" -> "https://forms.gle/F4SMAk4CeaS9zBGw9"
        "VERLOOP" -> "https://forms.gle/QDfHwhcJtqvEnt3N8"
        else -> "https://forms.gle/1xRJ95qbFvMHYP7e9"
    }
}

fun getMaintenanceStatus(name: String, day: Int, month: Int, year: Int): String {
    val cal = Calendar.getInstance().apply { set(year, month, day) }
    val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
    if (dayOfWeek == Calendar.SUNDAY || isIndonesianHoliday(day, month, year)) return ""

    val rules = mapOf(
        "Mobile Crane B" to (Calendar.MONDAY to listOf(3)),
        "Truck Dump DT39" to (Calendar.MONDAY to listOf(2)),
        "Truck Trailler H1983HG" to (Calendar.MONDAY to listOf(1)),
        "Mobile Crane C" to (Calendar.MONDAY to listOf(4)),
        "Kop C" to (Calendar.MONDAY to listOf(3)),
        "Kop D" to (Calendar.MONDAY to listOf(2)),
        "Kop E" to (Calendar.MONDAY to listOf(1)),
        "Kop F" to (Calendar.MONDAY to listOf(4)),
        "Line 13" to (Calendar.MONDAY to listOf(2)),
        "Line 14" to (Calendar.MONDAY to listOf(1)),
        "Line 15" to (Calendar.MONDAY to listOf(4)),
        "Lakop D" to (Calendar.MONDAY to listOf(3)),
        "Forklift A FD35" to (Calendar.TUESDAY to listOf(3)),
        "Truck Dump DT52" to (Calendar.TUESDAY to listOf(2)),
        "Truck Trailler H8318QO" to (Calendar.TUESDAY to listOf(1)),
        "Mobile Crane D" to (Calendar.TUESDAY to listOf(4)),
        "Slitting" to (Calendar.TUESDAY to listOf(2)),
        "Line 16" to (Calendar.TUESDAY to listOf(1)),
        "Line 17" to (Calendar.TUESDAY to listOf(4)),
        "Line 18" to (Calendar.TUESDAY to listOf(3)),
        "Tes Bending TELKOM" to (Calendar.TUESDAY to listOf(2)),
        "Tes Jatuh Telkom" to (Calendar.TUESDAY to listOf(1)),
        "Bevel" to (Calendar.TUESDAY to listOf(4)),
        "Tes Bending PLN" to (Calendar.TUESDAY to listOf(3)),
        "Verloop D" to (Calendar.TUESDAY to listOf(2)),
        "Truck Dump DT55" to (Calendar.WEDNESDAY to listOf(3)),
        "Panther H8629JA" to (Calendar.WEDNESDAY to listOf(2)),
        "Truck Trailler H8696OA" to (Calendar.WEDNESDAY to listOf(1)),
        "Mobile Crane E" to (Calendar.WEDNESDAY to listOf(4)),
        "Verloop H" to (Calendar.WEDNESDAY to listOf(1)),
        "Verloop E" to (Calendar.WEDNESDAY to listOf(4)),
        "Verloop F" to (Calendar.WEDNESDAY to listOf(3)),
        "Kompressor 01" to (Calendar.WEDNESDAY to listOf(2)),
        "Line 01" to (Calendar.WEDNESDAY to listOf(1)),
        "Line 02" to (Calendar.WEDNESDAY to listOf(4)),
        "Line 03" to (Calendar.WEDNESDAY to listOf(3)),
        "Verloop A" to (Calendar.WEDNESDAY to listOf(2)),
        "Lakop A" to (Calendar.WEDNESDAY to listOf(1)),
        "Truck Dump DT98" to (Calendar.THURSDAY to listOf(3)),
        "Truck Trailler H8697OA" to (Calendar.THURSDAY to listOf(2)),
        "Panther H1669SQ" to (Calendar.THURSDAY to listOf(1)),
        "Kop A" to (Calendar.THURSDAY to listOf(4)),
        "Kop B" to (Calendar.THURSDAY to listOf(3)),
        "Kompressor 02" to (Calendar.THURSDAY to listOf(2)),
        "Line 04" to (Calendar.THURSDAY to listOf(1)),
        "Line 05" to (Calendar.THURSDAY to listOf(4)),
        "Line 06" to (Calendar.THURSDAY to listOf(3)),
        "Verloop B" to (Calendar.THURSDAY to listOf(2)),
        "Verloop C" to (Calendar.THURSDAY to listOf(1)),
        "Lakop B" to (Calendar.THURSDAY to listOf(3)),
        "Lakop C" to (Calendar.FRIDAY to listOf(3)),
        "Mobile Crane A" to (Calendar.FRIDAY to listOf(3)),
        "Truck Dump DT139" to (Calendar.FRIDAY to listOf(2)),
        "Truck Trailler H1358KS" to (Calendar.FRIDAY to listOf(1)),
        "Line 07" to (Calendar.FRIDAY to listOf(2)),
        "Line 08" to (Calendar.FRIDAY to listOf(1)),
        "Line 09" to (Calendar.FRIDAY to listOf(4)),
        "Pipa ERW" to (Calendar.FRIDAY to listOf(3)),
        "Verloop G" to (Calendar.FRIDAY to listOf(2)),
        "Potong Bahan A" to (Calendar.FRIDAY to listOf(1)),
        "Forklift B FD250" to (Calendar.SATURDAY to listOf(1)),
        "Suzuki APV" to (Calendar.SATURDAY to listOf(3)),
        "Potong Spiral" to (Calendar.SATURDAY to listOf(4)),
        "Genset 01" to (Calendar.SATURDAY to listOf(3)),
        "Line 10" to (Calendar.SATURDAY to listOf(2)),
        "Line 11" to (Calendar.SATURDAY to listOf(1)),
        "Line 12" to (Calendar.SATURDAY to listOf(3))
    )

    val rule = rules[name.trim()] ?: return ""
    val targetDay = rule.first
    val bWeeks = rule.second

    if (dayOfWeek != targetDay) return ""

    var occurrence = 0
    val tempCal = Calendar.getInstance().apply { set(year, month, 1) }
    for (d in 1..day) {
        tempCal.set(Calendar.DAY_OF_MONTH, d)
        if (tempCal.get(Calendar.DAY_OF_WEEK) == targetDay) occurrence++
    }

    if (occurrence > 4) return ""
    return if (occurrence in bWeeks) "B" else "M"
}

fun getDayNameShort(day: Int, month: Int, year: Int): String {
    val cal = Calendar.getInstance().apply { set(year, month, day) }
    return when (cal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.SUNDAY -> "Min"
        Calendar.MONDAY -> "Sen"
        Calendar.TUESDAY -> "Sel"
        Calendar.WEDNESDAY -> "Rab"
        Calendar.THURSDAY -> "Kam"
        Calendar.FRIDAY -> "Jum"
        Calendar.SATURDAY -> "Sab"
        else -> ""
    }
}

fun isSunday(day: Int, month: Int, year: Int): Boolean = Calendar.getInstance().apply { set(year, month, day) }.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY

// PERBAIKAN BUG: Mengubah rujukan yang salah dari Calendar.SUNDAY menjadi Calendar.SATURDAY
fun isSaturday(day: Int, month: Int, year: Int): Boolean = Calendar.getInstance().apply { set(year, month, day) }.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY

fun isIndonesianHoliday(day: Int, month: Int, year: Int): Boolean {
    val holidays2024 = mapOf(0 to listOf(1), 1 to listOf(8, 10), 2 to listOf(11, 29, 31), 3 to listOf(10, 11), 4 to listOf(1, 9, 23), 5 to listOf(1, 17), 6 to listOf(7), 7 to listOf(17), 8 to listOf(16), 11 to listOf(25))
    val holidays2025 = mapOf(0 to listOf(1, 27, 29), 1 to listOf(10), 2 to listOf(28, 29, 31), 3 to listOf(1, 18), 4 to listOf(1, 12, 29), 5 to listOf(1, 7, 27), 7 to listOf(17), 8 to listOf(5), 11 to listOf(25))
    val holidays2026 = mapOf(0 to listOf(1, 19), 1 to listOf(17), 2 to listOf(20, 21, 28), 3 to listOf(3, 5), 4 to listOf(1, 14, 31), 5 to listOf(1, 22), 7 to listOf(17), 8 to listOf(5), 11 to listOf(25))

    val holidayMap = when (year) {
        2024 -> holidays2024
        2025 -> holidays2025
        2026 -> holidays2026
        else -> emptyMap()
    }
    return holidayMap[month]?.contains(day) ?: false
}

@Preview(showBackground = true)
@Composable
fun JadPerawatanScreenPreview() {
    SiTekiVer01Theme {
        JadPerawatanContent(
            selectedMonth = 0,
            selectedYear = 2026,
            machinesList = listOf("UMUM" to "Mobile Crane B"),
            actualRecords = emptyList(),
            isLoading = false,
            expandedMonth = false,
            errorMessage = null,
            months = listOf("Januari"),
            onBack = {},
            onMonthClick = {},
            onMonthDismiss = {},
            onMonthSelect = {},
            onMachineClick = {},
            onCellClick = { _, _, _ -> }
        )
    }
}