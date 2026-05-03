package com.example.sitekiver01.screens

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import com.example.sitekiver01.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*
import com.example.sitekiver01.components.*
import com.example.sitekiver01.OrbitronFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LapKerjaScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onNavigateToWebView: (String, String) -> Unit,
    onNavigateToIsiLaporan: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val calendar = Calendar.getInstance()
    val localeId = Locale("id", "ID")
    val currentMonthName = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, localeId) ?: ""
    val currentYear = calendar.get(Calendar.YEAR).toString()

    var selectedBulans by remember { mutableStateOf(listOf(currentMonthName)) }
    var tanggalRangeText by remember { mutableStateOf("Tanggal...") }
    var tglAwalRaw by remember { mutableStateOf("") }
    var tglAkhirRaw by remember { mutableStateOf("") }
    var selectedMesins by remember { mutableStateOf(listOf("Semua Mesin")) }

    var dataList by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var showBulanMultiSelect by remember { mutableStateOf(false) }
    var showMesinMultiSelect by remember { mutableStateOf(false) }
    var showDateRangePicker by remember { mutableStateOf(false) }
    var selectedItemForDetail by remember { mutableStateOf<JSONObject?>(null) }

    val dateRangePickerState = rememberDateRangePickerState()

    val fetchData = suspend {
        isLoading = true
        withContext(Dispatchers.IO) {
            try {
                val filterBulan = if (tanggalRangeText.contains("...")) selectedBulans.joinToString(", ") { "$it $currentYear" } else ""
                val encodedBulan = URLEncoder.encode(filterBulan, "UTF-8")
                val encodedTglAwal = URLEncoder.encode(tglAwalRaw, "UTF-8")
                val encodedTglAkhir = URLEncoder.encode(tglAkhirRaw, "UTF-8")

                val scriptUrl = "https://script.google.com/macros/s/AKfycbwYHHf8ONKbs9m5CppnzUuo067CBvrqRRfLYzl5ABwOH81sVWnFD8AyPx6F6Vf3uC4/exec"
                val urlString = "$scriptUrl?action=getDataLapKerja&bulan=$encodedBulan&tglAwal=$encodedTglAwal&tglAkhir=$encodedTglAkhir"

                val response = URL(urlString).readText()
                if (response.trim().startsWith("[")) {
                    val jsonArray = JSONArray(response)
                    val list = mutableListOf<JSONObject>()
                    for (i in 0 until jsonArray.length()) list.add(jsonArray.getJSONObject(i))
                    dataList = list
                } else dataList = emptyList()
            } catch (e: Exception) {
                Log.e("LapKerja", "Error fetch: ${e.message}")
                dataList = emptyList()
            }
        }
        isLoading = false
    }

    LaunchedEffect(Unit) { fetchData() }

    if (showDateRangePicker) {
        DateRangePickerModalLocal(
            state = dateRangePickerState,
            onDismiss = { showDateRangePicker = false },
            onSave = {
                val start = dateRangePickerState.selectedStartDateMillis
                val end = dateRangePickerState.selectedEndDateMillis
                if (start != null && end != null) {
                    val sdf = SimpleDateFormat("dd/MM/yyyy", localeId)
                    val sdfDisplay = SimpleDateFormat("d MMM", localeId)
                    tglAwalRaw = sdf.format(Date(start))
                    tglAkhirRaw = sdf.format(Date(end))
                    tanggalRangeText = "${sdfDisplay.format(Date(start))} - ${sdfDisplay.format(Date(end))}"
                    showDateRangePicker = false
                    scope.launch { fetchData() }
                }
            }
        )
    }

    if (selectedItemForDetail != null) {
        JobDetailDialog(
            item = selectedItemForDetail!!,
            onDismiss = { selectedItemForDetail = null }
        )
    }

    val mesinOptions = remember(dataList) {
        val names = dataList.mapNotNull { it.optString("mesin").takeIf { m -> m.isNotBlank() } }.distinct().sorted()
        listOf("Semua Mesin") + names
    }

    if (showBulanMultiSelect) {
        val bulanOptions = listOf("Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember")
        MultiSelectDialogLocal(
            title = "Pilih Bulan",
            options = bulanOptions,
            selectedOptions = selectedBulans,
            onDismiss = { showBulanMultiSelect = false },
            onSave = { selected ->
                selectedBulans = selected
                tanggalRangeText = "Tanggal..."
                tglAwalRaw = ""
                tglAkhirRaw = ""
                showBulanMultiSelect = false
                scope.launch { fetchData() }
            }
        )
    }

    if (showMesinMultiSelect) {
        MultiSelectDialogLocal(
            title = "Pilih Mesin",
            options = mesinOptions,
            selectedOptions = selectedMesins,
            onDismiss = { showMesinMultiSelect = false },
            onSave = { selected ->
                selectedMesins = if (selected.contains("Semua Mesin") && !selectedMesins.contains("Semua Mesin")) listOf("Semua Mesin")
                else if (selected.size > 1 && selected.contains("Semua Mesin")) selected.filter { it != "Semua Mesin" }
                else if (selected.isEmpty()) listOf("Semua Mesin")
                else selected
                showMesinMultiSelect = false
            }
        )
    }

    val filteredData = remember(dataList, selectedMesins) {
        if (selectedMesins.contains("Semua Mesin")) dataList
        else dataList.filter { selectedMesins.contains(it.optString("mesin")) }
    }

    LapKerjaScreenContent(
        modifier = modifier,
        selectedBulanText = if (selectedBulans.size == 1) selectedBulans.first() else "${selectedBulans.size} Bulan",
        tanggalRangeText = tanggalRangeText,
        selectedMesinText = if (selectedMesins.contains("Semua Mesin")) "Semua Mesin" else if (selectedMesins.size == 1) selectedMesins.first() else "${selectedMesins.size} Mesin",
        dataList = filteredData,
        isLoading = isLoading,
        onBack = onBack,
        onBulanClick = { showBulanMultiSelect = true },
        onTanggalClick = { showDateRangePicker = true },
        onMesinClick = { showMesinMultiSelect = true },
        onSearchClick = { scope.launch { fetchData() } },
        onCreateReportClick = { onNavigateToIsiLaporan() },
        onRowClick = { selectedItemForDetail = it }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LapKerjaScreenContent(
    modifier: Modifier = Modifier,
    selectedBulanText: String,
    tanggalRangeText: String,
    selectedMesinText: String,
    dataList: List<JSONObject>,
    isLoading: Boolean,
    onBack: () -> Unit,
    onBulanClick: () -> Unit,
    onTanggalClick: () -> Unit,
    onMesinClick: () -> Unit,
    onSearchClick: () -> Unit,
    onCreateReportClick: () -> Unit,
    onRowClick: (JSONObject) -> Unit
) {
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
                        "LAPORAN PEKERJAAN",
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
            ModernSectionHeader("PENCARIAN DATA", Icons.Default.Search)

            ModernFormCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            ModernSearchField(selectedBulanText, "Pilih Bulan...", onBulanClick, true)
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            ModernSearchField(if (tanggalRangeText == "Tanggal...") "" else tanggalRangeText, "Tanggal...", onTanggalClick, true)
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            ModernSearchField(if (selectedMesinText == "Semua Mesin") "" else selectedMesinText, "Mesin...", onMesinClick, true)
                        }
                        IconButton(
                            onClick = onSearchClick,
                            modifier = Modifier.size(48.dp).background(GlassAccentCyan, RoundedCornerShape(12.dp))
                        ) {
                            Icon(Icons.Default.Search, null, tint = Color.Black)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                ModernSectionHeader("DATA LAPORAN KERJA", Icons.AutoMirrored.Filled.List)
                Button(
                    onClick = onCreateReportClick,
                    colors = ButtonDefaults.buttonColors(containerColor = GlassAccentCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp), tint = Color.Black)
                    Spacer(Modifier.width(4.dp))
                    Text("Buat Laporan", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }

            Spacer(Modifier.height(8.dp))

            ModernFormCard(modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = GlassAccentCyan)
                    } else if (dataList.isEmpty()) {
                        Text("Tidak ada data", modifier = Modifier.align(Alignment.Center), color = GlassTextMuted, fontSize = 14.sp)
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(modifier = Modifier.fillMaxWidth().background(GlassAccentCyan.copy(alpha = 0.1f)).padding(vertical = 10.dp)) {
                                HeaderCellLocal("Tanggal", 1.2f)
                                HeaderCellLocal("Mesin", 1.5f)
                                HeaderCellLocal("Laporan Pekerjaan", 3f)
                                HeaderCellLocal("Dur", 0.8f)
                            }
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                itemsIndexed(dataList) { index, item ->
                                    val tglRaw = item.optString("tanggal")
                                    val formattedDate = formatTableDate(tglRaw)
                                    val mName = item.optString("mesin")

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onRowClick(item) }
                                            .background(if (index % 2 == 0) Color.Transparent else Color.White.copy(alpha = 0.02f))
                                    ) {
                                        DataCellLocal(formattedDate, 1.2f)
                                        DataCellLocal(mName, 1.5f, maxLines = 1)
                                        DataCellLocal(item.optString("laporan"), 3f, TextAlign.Start)
                                        DataCellLocal(item.optString("durasi"), 0.8f)
                                    }
                                    HorizontalDivider(color = GlassBorder)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun RowScope.HeaderCellLocal(text: String, weight: Float) {
    Text(text, Modifier.weight(weight).padding(horizontal = 2.dp), color = GlassAccentCyan, fontWeight = FontWeight.Bold, fontSize = 9.sp, textAlign = TextAlign.Center)
}

@Composable
fun RowScope.DataCellLocal(text: String, weight: Float, textAlign: TextAlign = TextAlign.Center, maxLines: Int = 5) {
    Text(text, Modifier.weight(weight).padding(vertical = 8.dp, horizontal = 2.dp), fontSize = 8.sp, color = Color.White, textAlign = textAlign, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerModalLocal(state: DateRangePickerState, onDismiss: () -> Unit, onSave: () -> Unit) {
    val localeId = Locale("id", "ID")
    val config = LocalConfiguration.current
    val overrideConfig = remember { android.content.res.Configuration(config).apply { setLocale(localeId) } }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = GlassBase) {
            Column(modifier = Modifier.fillMaxSize()) {
                Surface(color = Color.Transparent, modifier = Modifier.fillMaxWidth().height(120.dp)) {
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopStart).background(Color.White.copy(alpha = 0.1f), CircleShape)) { Icon(Icons.Default.Close, null, tint = Color.White) }
                        Text(text = "SIMPAN", color = GlassAccentCyan, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.TopEnd).clickable { onSave() }.padding(8.dp))
                    }
                }
                CompositionLocalProvider(LocalConfiguration provides overrideConfig) {
                    DateRangePicker(
                        state = state,
                        modifier = Modifier.weight(1f),
                        title = null,
                        headline = null,
                        showModeToggle = false,
                        colors = DatePickerDefaults.colors(
                            containerColor = GlassBase,
                            titleContentColor = Color.White,
                            headlineContentColor = Color.White,
                            selectedDayContainerColor = GlassAccentCyan,
                            selectedDayContentColor = Color.Black,
                            todayContentColor = GlassAccentCyan,
                            todayDateBorderColor = GlassAccentCyan,
                            dayContentColor = Color.White,
                            weekdayContentColor = GlassTextMuted
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun MultiSelectDialogLocal(title: String, options: List<String>, selectedOptions: List<String>, onDismiss: () -> Unit, onSave: (List<String>) -> Unit) {
    var currentSelected by remember { mutableStateOf(selectedOptions) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1A1A1A),
            border = BorderStroke(1.dp, GlassBorder),
            modifier = Modifier.fillMaxWidth(0.9f).heightIn(max = 500.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                Spacer(Modifier.height(16.dp))
                LazyColumn(modifier = Modifier.weight(1f)) {
                    itemsIndexed(options) { _, option ->
                        val isSelected = currentSelected.contains(option)
                        val isSemua = option == "Semua Mesin"
                        Row(modifier = Modifier.fillMaxWidth().clickable {
                            if(isSemua) {
                                currentSelected = listOf("Semua Mesin")
                            } else {
                                val next = if (isSelected) currentSelected - option else currentSelected + option
                                currentSelected = next.filter { it != "Semua Mesin" }.ifEmpty { listOf("Semua Mesin") }
                            }
                        }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = null,
                                colors = CheckboxDefaults.colors(checkedColor = GlassAccentCyan, uncheckedColor = GlassBorder, checkmarkColor = Color.Black)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(text = option, fontSize = 14.sp, color = Color.White)
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Batal", color = GlassTextMuted) }
                    Button(
                        onClick = { onSave(currentSelected) },
                        colors = ButtonDefaults.buttonColors(containerColor = GlassAccentCyan),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Simpan", color = Color.Black) }
                }
            }
        }
    }
}

@Composable
fun JobDetailDialog(item: JSONObject, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = GlassBase
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color(0xFF1A1A1A), GlassBase)))
                        .padding(top = 40.dp, bottom = 24.dp, start = 20.dp, end = 20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = GlassAccentCyan.copy(alpha = 0.1f), shape = CircleShape, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.Engineering, null, tint = GlassAccentCyan, modifier = Modifier.padding(10.dp))
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("Detail Pekerjaan", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, fontFamily = OrbitronFontFamily)
                            Text("ID: #${item.optString("mesin").take(5).uppercase()}", color = GlassTextMuted, fontSize = 12.sp)
                        }
                        Spacer(Modifier.weight(1f))
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)
                        ) { Icon(Icons.Default.Close, null, tint = Color.White) }
                    }
                }

                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ModernDetailCard("IDENTITAS") {
                        DetailItemModern("TANGGAL", formatTableDate(item.optString("tanggal")), Icons.Default.CalendarToday)
                        DetailItemModern("BAGIAN", item.optString("bagian"), Icons.Default.Business)
                        DetailItemModern("NAMA MESIN", item.optString("mesin"), Icons.Default.PrecisionManufacturing)
                    }
                    ModernDetailCard("WAKTU & DURASI") {
                        Row(Modifier.fillMaxWidth()) {
                            Box(Modifier.weight(1f)) { DetailItemModern("JAM MULAI", formatToTime(item.optString("awal")), Icons.Default.Schedule) }
                            Box(Modifier.weight(1f)) { DetailItemModern("JAM SELESAI", formatToTime(item.optString("akhir")), Icons.Default.DoneAll) }
                        }
                        DetailItemModern("DURASI", "${item.optString("durasi")} Jam", Icons.Default.Timer, isHighlight = true)
                    }
                    ModernDetailCard("LAPORAN PEKERJAAN") {
                        Text(text = item.optString("laporan").ifBlank { "-" }, fontSize = 14.sp, color = Color.White, lineHeight = 20.sp)
                    }
                    ModernDetailCard("MATERIAL & HASIL") {
                        DetailItemModern("SPAREPART", item.optString("sparepart"), Icons.Default.SettingsSuggest)
                        Row(Modifier.fillMaxWidth()) {
                            Box(Modifier.weight(1f)) { DetailItemModern("ORDER", item.optString("order"), Icons.AutoMirrored.Filled.ReceiptLong) }
                            Box(Modifier.weight(1f)) {
                                val status = item.optString("statusOrder")
                                DetailItemModern("STATUS ORDER", status, Icons.AutoMirrored.Filled.FactCheck,
                                    valueColor = if(status.contains("Done", true)) GlassAccentGreen else GlassAccentCyan)
                            }
                        }
                        val nilai = item.optString("nilaiPerbaikan")
                        DetailItemModern("NILAI PERBAIKAN", nilai, Icons.Default.Stars,
                            valueColor = when {
                                nilai.equals("Bagus", true) -> GlassAccentGreen
                                nilai.equals("Cukup", true) -> GlassAccentAmber
                                else -> Color.Red
                            })
                    }
                    ModernDetailCard("KETERANGAN") {
                        Text(text = item.optString("keterangan").ifBlank { "-" }, fontSize = 13.sp, color = GlassTextMuted, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    }
                    Spacer(Modifier.height(24.dp))
                }

                Surface(color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().padding(20.dp).height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GlassAccentCyan)
                    ) {
                        Text("TUTUP", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun ModernDetailCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GlassAccentCyan, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = GlassSurface,
            border = BorderStroke(1.dp, GlassBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    }
}

@Composable
fun DetailItemModern(label: String, value: String, icon: ImageVector, isHighlight: Boolean = false, valueColor: Color = Color.White) {
    Row(modifier = Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(18.dp), tint = GlassAccentCyan)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(text = label, fontSize = 10.sp, color = GlassTextMuted)
            Text(text = if (value.isBlank() || value == "null") "-" else value, fontSize = 14.sp, color = valueColor, fontWeight = FontWeight.Medium)
        }
    }
}

fun formatTableDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val outputFormat = SimpleDateFormat("d MMM yyyy", Locale("id", "ID"))
        val date = inputFormat.parse(dateString)
        if (date != null) outputFormat.format(date) else dateString
    } catch (e: Exception) {
        dateString
    }
}

fun formatToTime(dateTimeString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val date = inputFormat.parse(dateTimeString)
        if (date != null) outputFormat.format(date) else {
            // Try extracting time manually if parse fails
            if (dateTimeString.contains(":")) {
                val parts = dateTimeString.split(" ")
                if (parts.size > 1) parts[1] else dateTimeString
            } else dateTimeString
        }
    } catch (e: Exception) {
        dateTimeString
    }
}
