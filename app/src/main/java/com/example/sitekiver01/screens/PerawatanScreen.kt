package com.example.sitekiver01.screens

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import java.text.SimpleDateFormat
import java.util.*

// Fungsi pembantu global untuk mencocokkan tanggal secara fleksibel (menangani 1/1 vs 01/01)
private fun isDateMatch(d1: String, d2: String): Boolean {
    return try {
        val p1 = d1.trim().split("/")
        val p2 = d2.trim().split("/")
        if (p1.size != 3 || p2.size != 3) d1.trim() == d2.trim()
        else p1[0].toInt() == p2[0].toInt() && p1[1].toInt() == p2[1].toInt() && p1[2].toInt() == p2[2].toInt()
    } catch (e: Exception) {
        d1.trim() == d2.trim()
    }
}

// ==================== MAIN SCREEN ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerawatanScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onNavigateToWebView: (String, String) -> Unit = { _, _ -> },
    onNavigateToIsiPerawatan: (String, String, String) -> Unit = { _, _, _ -> },
    onNavigateToDetail: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val calendar = Calendar.getInstance()

    var actualRecords by remember { mutableStateOf<List<ActualRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
        if (!isRefreshing) isLoading = true
        errorMessage = null
        withContext(Dispatchers.IO) {
            try {
                val url = URL("https://script.google.com/macros/s/AKfycbwQ7ocBNsl4x5-rGLrSyvkyluhSRl3B_LvmkA3cFuvuL9pBbVAOUI3i_Vu6jwfkfOA/exec?action=getPerawatan")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
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
                }
            } catch (e: Exception) {
                Log.e("PerawatanScreen", "Error: ${e.message}")
                errorMessage = "Gagal memuat data: ${e.message}"
            } finally {
                isLoading = false
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(Unit) { fetchData() }

    val machinesWithJenis = order.map { name ->
        val item = actualRecords.find { it.nama_mesin.trim().equals(name.trim(), ignoreCase = true) }
        (item?.jenis ?: "UMUM") to name
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // PONDASI UTAMA: Background Mesh Grid Animasi Global
        SciFiBackground()

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
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                        ) { Icon(Icons.Default.ArrowBackIosNew, "Back", tint = MaterialTheme.colorScheme.onSurface) }
                        Spacer(Modifier.width(16.dp))
                        Text(
                            "PERAWATAN MESIN",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            fontFamily = OrbitronFontFamily,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        ) { padding ->
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { isRefreshing = true; scope.launch { fetchData() } },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (isLoading && !isRefreshing) {
                    DatabaseLoadingState(label = "Menyinkronkan data perawatan", modifier = Modifier.fillMaxSize())
                } else {
                    PerawatanContent(
                        machinesList = machinesWithJenis,
                        actualRecords = actualRecords,
                        onCellClick = onNavigateToIsiPerawatan
                    )
                }
            }
        }
    }
}

@Composable
fun PerawatanContent(
    machinesList: List<Pair<String, String>>,
    actualRecords: List<ActualRecord>,
    onCellClick: (String, String, String) -> Unit
) {
    // PERBAIKAN LOCALE WARNING: Membungkus Date Formatter ke dalam remember
    val todayKey = remember {
        val todayCal = Calendar.getInstance()
        String.format(Locale.US, "%02d/%02d/%d",
            todayCal.get(Calendar.DAY_OF_MONTH), todayCal.get(Calendar.MONTH) + 1, todayCal.get(Calendar.YEAR))
    }
    val currentDay = remember { Calendar.getInstance().get(Calendar.DAY_OF_MONTH) }
    val currentMonth = remember { Calendar.getInstance().get(Calendar.MONTH) }
    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }

    // ==================== PERLU DIRAWAT HARI INI ====================
    val todayTasks = remember(machinesList, actualRecords, todayKey) {
        machinesList.filter { machine ->
            val status = getMaintenanceStatus(machine.second, currentDay, currentMonth, currentYear)
            if (status.isEmpty()) return@filter false

            val alreadyDoneToday = actualRecords.any { record ->
                record.nama_mesin.trim().equals(machine.second.trim(), ignoreCase = true) &&
                        isDateMatch(record.tanggal, todayKey)
            }
            !alreadyDoneToday
        }
    }

    // ==================== TERLEWAT (1 Minggu Terakhir) ====================
    val overdueTasks = remember(machinesList, actualRecords, todayTasks) {
        machinesList.filter { machine ->
            if (todayTasks.any { it.second.trim().equals(machine.second.trim(), ignoreCase = true) }) return@filter false

            for (i in 1..7) {
                val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
                val day = cal.get(Calendar.DAY_OF_MONTH)
                val month = cal.get(Calendar.MONTH)
                val year = cal.get(Calendar.YEAR)

                val status = getMaintenanceStatus(machine.second, day, month, year)
                if (status.isNotEmpty()) {
                    val dateKey = String.format(Locale.US, "%02d/%02d/%d", day, month + 1, year)
                    val alreadyDone = actualRecords.any { record ->
                        record.nama_mesin.trim().equals(machine.second.trim(), ignoreCase = true) &&
                                isDateMatch(record.tanggal, dateKey)
                    }
                    if (!alreadyDone) return@filter true
                }
            }
            false
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }

        // SECTION 1: PERLU DIRAWAT HARI INI
        item {
            CollapsibleSection(
                title = "PERLU DIRAWAT HARI INI",
                icon = Icons.Default.CheckCircle,
                color = Color(0xFF10B981), // SciFiStatusM Green
                count = todayTasks.size
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    todayTasks.chunked(2).forEach { rowTasks ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowTasks.forEach { machine ->
                                Box(modifier = Modifier.weight(1f)) {
                                    MaintenanceCard(machine, todayKey, actualRecords, onCellClick)
                                }
                            }
                            if (rowTasks.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // SECTION 2: TERLEWAT (1 MINGGU)
        item {
            CollapsibleSection(
                title = "TERLEWAT (1 MINGGU)",
                icon = Icons.Default.Error,
                color = Color(0xFFC23B22), // SciFiHoliday Red
                count = overdueTasks.size
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    overdueTasks.chunked(2).forEach { rowTasks ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowTasks.forEach { machine ->
                                Box(modifier = Modifier.weight(1f)) {
                                    MaintenanceCard(machine, null, actualRecords, onCellClick, true)
                                }
                            }
                            if (rowTasks.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(40.dp)) }
    }
}

@Composable
fun CollapsibleSection(
    title: String,
    icon: ImageVector,
    color: Color,
    count: Int,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(true) } // Di-default true agar data langsung HUD terlihat

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f),
                fontFamily = OrbitronFontFamily,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "($count)",
                color = color,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = OrbitronFontFamily
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = color,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Box(modifier = Modifier.padding(top = 4.dp)) {
                content()
            }
        }
    }
}

@Composable
fun MaintenanceCard(
    machine: Pair<String, String>,
    dateKey: String?,
    actualRecords: List<ActualRecord>,
    onCellClick: (String, String, String) -> Unit,
    isOverdue: Boolean = false
) {
    val finalData = remember(machine, dateKey, actualRecords, isOverdue) {
        if (isOverdue) {
            var statusFound = ""
            var dateFound: String? = null
            for (i in 1..7) {
                val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
                val day = cal.get(Calendar.DAY_OF_MONTH)
                val month = cal.get(Calendar.MONTH)
                val year = cal.get(Calendar.YEAR)
                val checkKey = String.format(Locale.US, "%02d/%02d/%d", day, month + 1, year)
                val status = getMaintenanceStatus(machine.second, day, month, year)
                if (status.isNotEmpty()) {
                    val hasActual = actualRecords.any { record ->
                        record.nama_mesin.trim().equals(machine.second.trim(), ignoreCase = true) &&
                                isDateMatch(record.tanggal, checkKey)
                    }
                    if (!hasActual) {
                        statusFound = status
                        dateFound = checkKey
                        break
                    }
                }
            }
            statusFound to dateFound
        } else {
            val parts = dateKey?.split("/") ?: emptyList()
            val status = if (parts.size == 3) getMaintenanceStatus(machine.second, parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt()) else ""
            status to dateKey
        }
    }

    val finalStatus = finalData.first
    val finalDateKey = finalData.second

    if (finalStatus.isEmpty() || finalDateKey == null) return

    // PANEL KARTU KACA (Glassmorphic Outer Surface)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = SciFiGlass,
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = if (isOverdue) listOf(Color(0xFFC23B22).copy(alpha = 0.4f), Color.Transparent)
                else listOf(SciFiBorderLight, Color.Transparent)
            )
        )
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    machine.second,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (finalStatus.isNotEmpty()) {
                    Text(
                        finalStatus,
                        color = if (finalStatus == "B") Color(0xFF3B82F6) else Color(0xFF10B981),
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        fontFamily = OrbitronFontFamily,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (isOverdue) "Terlewat: $finalDateKey" else "Jadwal Aktif",
                color = if (isOverdue) Color(0xFFF87171) else Color(0xFFF59E0B),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(14.dp))

            // BUTTON SUBMIT PERAWATAN (CORE REACTOR GLOW)
            Button(
                onClick = {
                    onCellClick(machine.second, finalDateKey, finalStatus)
                },
                modifier = Modifier.fillMaxWidth().height(34.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = if (isOverdue) listOf(Color(0xFFC23B22), Color(0xFF7F1D1D))
                                else listOf(SciFiCyan, SciFiBlue)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "RAWAT",
                        color = if (isOverdue) Color.White else Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = OrbitronFontFamily
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PerawatanScreenPreview() {
    SiTekiVer01Theme {
        PerawatanScreen(onBack = {}, onNavigateToIsiPerawatan = { _, _, _ -> })
    }
}
