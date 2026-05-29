package com.example.sitekiver01.screens

import android.util.Log
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.sitekiver01.ui.theme.*
import com.example.sitekiver01.OrbitronFontFamily
import com.example.sitekiver01.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

// --- 1. MODEL DATA ---
data class RekapDataListrik(val bulan: String, val pencapaian: Float, val target: Float)

// --- 2. KONSTANTA WARNA UTAMA CHIEF CHANNELS ---
val ChartBlueLocal = Color(0xFF06B6D4) // SciFiCyan
val ChartYellowLocal = Color(0xFFD97706) // SciFiSaturday Amber
val DividerGrayLocal = Color.White.copy(alpha = 0.08f)

// --- 3. HELPER ---
fun getNamaBulanLengkap(dateString: String): String {
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
fun DataListrikScreen(
    onBack: () -> Unit,
    onEditData: (JSONObject) -> Unit
) {
    val scope = rememberCoroutineScope()
    val localeId = remember { Locale("id", "ID") }

    val calendar = Calendar.getInstance()
    val currentMonthName = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, localeId) ?: ""
    val currentYear = calendar.get(Calendar.YEAR).toString()

    var selectedBulan by remember { mutableStateOf(currentMonthName) }
    var tanggalRangeText by remember { mutableStateOf("Tanggal...") }
    var tglAwalRaw by remember { mutableStateOf("") }
    var tglAkhirRaw by remember { mutableStateOf("") }

    var dataList by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var showBulanDropdown by remember { mutableStateOf(false) }
    var showDateRangePicker by remember { mutableStateOf(false) }

    val dateRangePickerState = rememberDateRangePickerState()

    val fetchData = suspend {
        isLoading = true
        withContext(Dispatchers.IO) {
            try {
                val filterBulan = if (tanggalRangeText.contains("...")) "$selectedBulan $currentYear" else ""

                val encodedBulan = URLEncoder.encode(filterBulan, "UTF-8")
                val encodedTglAwal = URLEncoder.encode(tglAwalRaw, "UTF-8")
                val encodedTglAkhir = URLEncoder.encode(tglAkhirRaw, "UTF-8")

                val urlString = "$SCRIPT_URL?action=getData&bulan=$encodedBulan&tglAwal=$encodedTglAwal&tglAkhir=$encodedTglAkhir"

                val response = URL(urlString).readText()
                val jsonArray = JSONArray(response)
                val list = mutableListOf<JSONObject>()

                val sdf = SimpleDateFormat("dd/MM/yyyy", localeId)
                val startLimit = if (tglAwalRaw.isNotEmpty()) try { sdf.parse(tglAwalRaw) } catch(e: Exception) { null } else null
                val endLimit = if (tglAkhirRaw.isNotEmpty()) try { sdf.parse(tglAkhirRaw) } catch(e: Exception) { null } else null

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    if (startLimit != null && endLimit != null) {
                        val itemDateStr = obj.optString("tanggal")
                        val itemDate = try { sdf.parse(itemDateStr) } catch(e: Exception) { null }
                        if (itemDate != null && (itemDate.before(startLimit) || itemDate.after(endLimit))) {
                            continue
                        }
                    }
                    list.add(obj)
                }
                dataList = list
            } catch (e: Exception) {
                Log.e("DataListrik", "Error fetching data: ${e.message}", e)
                dataList = emptyList()
            }
        }
        isLoading = false
    }

    LaunchedEffect(Unit) { fetchData() }

    Box(modifier = Modifier.fillMaxSize().background(GlassBase)) {

        // PONDASI UTAMA: Background Mesh Grid Animasi Global
        SciFiBackground()

        if (showDateRangePicker) {
            DateRangePickerModalDataListrik(
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

        val bulanOptions = listOf("Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember")

        if (showBulanDropdown) {
            SingleSelectDialogDataListrik(
                title = "Pilih Bulan",
                options = bulanOptions,
                selectedOption = selectedBulan,
                onDismiss = { showBulanDropdown = false },
                onSave = { selected ->
                    selectedBulan = selected
                    tanggalRangeText = "Tanggal..."
                    tglAwalRaw = ""
                    tglAkhirRaw = ""
                    showBulanDropdown = false
                    scope.launch { fetchData() }
                }
            )
        }

        DataListrikScreenContent(
            selectedBulan = selectedBulan,
            tanggalRangeText = tanggalRangeText,
            dataList = dataList,
            isLoading = isLoading,
            onBack = onBack,
            onEditData = onEditData,
            onBulanClick = { showBulanDropdown = true },
            onTanggalClick = { showDateRangePicker = true },
            onSearchClick = { scope.launch { fetchData() } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangePickerModalDataListrik(
    state: DateRangePickerState,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val localeId = remember { Locale("id", "ID") }
    val config = LocalConfiguration.current
    val overrideConfig = remember { android.content.res.Configuration(config).apply { setLocale(localeId) } }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF070D19) // Menyelaraskan dengan warna dasar siber deep dark navy
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Surface(
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth().height(120.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.align(Alignment.TopStart).background(Color.White.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                        }

                        Text(
                            text = "SIMPAN",
                            color = SciFiCyan,
                            fontWeight = FontWeight.Bold,
                            fontFamily = OrbitronFontFamily,
                            modifier = Modifier.align(Alignment.TopEnd).clickable { onSave() }.padding(8.dp)
                        )

                        Column(modifier = Modifier.align(Alignment.BottomStart)) {
                            Text(
                                text = "PILIH RENTANG TANGGAL",
                                color = SciFiTextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = OrbitronFontFamily
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val start = state.selectedStartDateMillis
                                val end = state.selectedEndDateMillis
                                val sdf = SimpleDateFormat("d MMM yyyy", localeId)
                                val rangeText = if (start != null && end != null) {
                                    "${sdf.format(Date(start))} – ${sdf.format(Date(end))}"
                                } else if (start != null) {
                                    sdf.format(Date(start))
                                } else {
                                    "Pilih Rentang"
                                }

                                Text(
                                    text = rangeText,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.Default.Edit, contentDescription = null, tint = SciFiCyan, modifier = Modifier.size(18.dp))
                            }
                        }
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
                            containerColor = Color(0xFF070D19),
                            titleContentColor = Color.White,
                            headlineContentColor = Color.White,
                            selectedDayContainerColor = SciFiCyan,
                            selectedDayContentColor = Color.Black,
                            todayContentColor = SciFiCyan,
                            todayDateBorderColor = SciFiCyan,
                            dayContentColor = Color.White,
                            weekdayContentColor = SciFiTextMuted
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun SingleSelectDialogDataListrik(
    title: String,
    options: List<String>,
    selectedOption: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var currentSelected by remember { mutableStateOf(selectedOption) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A), // SciFiBrandCard
            border = BorderStroke(1.dp, SciFiBorderMedium),
            modifier = Modifier.fillMaxWidth(0.9f).heightIn(max = 450.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White, fontFamily = OrbitronFontFamily)
                Spacer(Modifier.height(16.dp))

                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    itemsIndexed(options) { _, option ->
                        val isSelected = currentSelected == option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { currentSelected = option }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { currentSelected = option },
                                colors = RadioButtonDefaults.colors(selectedColor = SciFiCyan, unselectedColor = SciFiBorderLight)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(text = option, fontSize = 14.sp, color = Color.White)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Batal", color = SciFiTextMuted) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(currentSelected) },
                        colors = ButtonDefaults.buttonColors(containerColor = SciFiCyan),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Simpan", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataListrikScreenContent(
    selectedBulan: String,
    tanggalRangeText: String,
    dataList: List<JSONObject>,
    isLoading: Boolean,
    onBack: () -> Unit,
    onEditData: (JSONObject) -> Unit,
    onBulanClick: () -> Unit,
    onTanggalClick: () -> Unit,
    onSearchClick: () -> Unit
) {
    var showEditDialog by remember { mutableStateOf<JSONObject?>(null) }

    if (showEditDialog != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = null },
            containerColor = Color(0xFF0F172A),
            modifier = Modifier.border(1.dp, SciFiBorderLight, RoundedCornerShape(28.dp)),
            title = { Text("Konfirmasi Tindakan", fontWeight = FontWeight.Bold, color = Color.White, fontFamily = OrbitronFontFamily) },
            text = { Text("Apakah Anda ingin merubah atau mengedit baris rekaman data kelistrikan ini?", color = SciFiTextMuted) },
            confirmButton = {
                Button(
                    onClick = {
                        onEditData(showEditDialog!!)
                        showEditDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SciFiCyan)
                ) { Text("Ya", color = Color.Black, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = null }) { Text("Tidak", color = SciFiTextMuted) }
            }
        )
    }

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
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBackIosNew,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = "DETAIL ENERGI LISTRIK",
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
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(12.dp))
            Text("PENCARIAN DATA METRIC", color = SciFiCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = OrbitronFontFamily)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    SearchInputFieldDataListrik(
                        value = selectedBulan,
                        placeholder = "Pilih Bulan...",
                        onClick = onBulanClick,
                        isDropdownIcon = true
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    SearchInputFieldDataListrik(
                        value = if (tanggalRangeText == "Tanggal...") "" else tanggalRangeText,
                        placeholder = "Tanggal...",
                        onClick = onTanggalClick,
                        isSearchIcon = true,
                        onSearchClick = onSearchClick
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    color = SciFiCyan.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                    border = BorderStroke(1.dp, SciFiCyan.copy(alpha = 0.3f))
                ) {
                    Text(
                        "HASIL DATA METRIC KVAR",
                        color = SciFiCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = OrbitronFontFamily,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            TableDataDataListrik(
                modifier = Modifier.weight(1f),
                data = dataList,
                isLoading = isLoading,
                onRowClick = { showEditDialog = it }
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SearchInputFieldDataListrik(
    value: String,
    onClick: () -> Unit,
    placeholder: String,
    isSearchIcon: Boolean = false,
    isDropdownIcon: Boolean = false,
    onSearchClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isPlaceholder = value.isEmpty() || value.contains("...")
    val displayText = if (isPlaceholder) placeholder else value

    Surface(
        modifier = modifier
            .height(42.dp)
            .clickable(onClick = onClick),
        border = BorderStroke(1.dp, SciFiBorderLight),
        shape = RoundedCornerShape(21.dp),
        color = Color.White.copy(alpha = 0.03f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = displayText,
                modifier = Modifier.weight(1f),
                color = if (isPlaceholder) SciFiTextMuted else Color.White,
                fontSize = 12.sp,
                fontStyle = if (isPlaceholder) FontStyle.Italic else FontStyle.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (isDropdownIcon) {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = SciFiTextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
            if (isSearchIcon) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(SciFiCyan, CircleShape)
                        .clickable { onSearchClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TableDataDataListrik(
    modifier: Modifier = Modifier,
    data: List<JSONObject>,
    isLoading: Boolean,
    onRowClick: (JSONObject) -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = SciFiGlass,
        border = BorderStroke(1.dp, Brush.verticalGradient(listOf(SciFiBorderLight, Color.Transparent))),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().background(SciFiCyan.copy(alpha = 0.08f))) {
                TableCellLocal("Tgl", weight = 1.2f, isHeader = true)
                TableCellLocal("Jam", weight = 1f, isHeader = true)
                TableCellLocal("KWH", weight = 1f, isHeader = true)
                TableCellLocal("KVAR", weight = 1f, isHeader = true)
                TableCellLocal("Selisih", weight = 1f, isHeader = true)
                TableCellLocal("Kesimpulan", weight = 1.5f, isHeader = true)
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SciFiCyan)
                }
            } else if (data.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Tidak ada data", color = SciFiTextMuted, fontSize = 14.sp)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(data) { index, item ->
                        val bgColor = if (index % 2 != 0) Color.White.copy(alpha = 0.01f) else Color.Transparent
                        Row(modifier = Modifier
                            .fillMaxWidth()
                            .background(bgColor)
                            .clickable { onRowClick(item) }
                        ) {
                            TableCellLocal(item.optString("tanggal"), weight = 1.2f)
                            TableCellLocal(item.optString("jam"), weight = 1f)
                            TableCellLocal(item.optString("kwh"), weight = 1f)
                            TableCellLocal(item.optString("kvar"), weight = 1f)
                            TableCellLocal(item.optString("selisih"), weight = 1f)
                            TableCellLocal(item.optString("kesimpulan"), weight = 1.5f)
                        }
                        HorizontalDivider(color = SciFiBorderLight.copy(alpha = 0.3f))
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.TableCellLocal(
    text: String,
    weight: Float,
    isHeader: Boolean = false
) {
    Text(
        text = text,
        modifier = Modifier
            .weight(weight)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        color = if (isHeader) SciFiCyan else Color.White,
        fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
        fontSize = if (isHeader) 10.sp else 9.sp,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}