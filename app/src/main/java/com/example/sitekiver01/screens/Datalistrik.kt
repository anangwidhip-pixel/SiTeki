package com.example.sitekiver01.screens

import android.app.Activity
import android.util.Log
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
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
import com.example.sitekiver01.OrbitronFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataListrikScreen(
    onBack: () -> Unit,
    onEditData: (JSONObject) -> Unit
) {
    val scope = rememberCoroutineScope()
    val localeId = Locale("id", "ID")

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
    val localeId = Locale("id", "ID")
    val config = LocalConfiguration.current
    val overrideConfig = remember { android.content.res.Configuration(config).apply { setLocale(localeId) } }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = GlassBase
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
                            color = GlassAccentCyan,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.TopEnd).clickable { onSave() }.padding(8.dp)
                        )

                        Column(modifier = Modifier.align(Alignment.BottomStart)) {
                            Text(
                                text = "PILIH TANGGAL",
                                color = GlassTextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val start = state.selectedStartDateMillis
                                val end = state.selectedEndDateMillis
                                val sdf = SimpleDateFormat("d MMM", localeId)
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
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.Default.Edit, contentDescription = null, tint = GlassAccentCyan, modifier = Modifier.size(18.dp))
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
            color = Color(0xFF1A1A1A),
            border = BorderStroke(1.dp, GlassBorder),
            modifier = Modifier.fillMaxWidth(0.9f).heightIn(max = 450.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                Spacer(Modifier.height(16.dp))

                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    itemsIndexed(options) { _, option ->
                        val isSelected = currentSelected == option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { currentSelected = option }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { currentSelected = option },
                                colors = RadioButtonDefaults.colors(selectedColor = GlassAccentCyan, unselectedColor = GlassBorder)
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
                    ) {
                        Text("Simpan", color = Color.Black)
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
            containerColor = Color(0xFF1A1A1A),
            title = { Text("Konfirmasi", fontWeight = FontWeight.Bold, color = Color.White) },
            text = { Text("Apakah Ingin Edit Data?", color = GlassTextMuted) },
            confirmButton = {
                Button(
                    onClick = {
                        onEditData(showEditDialog!!)
                        showEditDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GlassAccentCyan)
                ) { Text("Ya", color = Color.Black) }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = null }) { Text("Tidak", color = GlassTextMuted) }
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
                        text = "DETAIL PERFORMANCE",
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
        ) {
            Spacer(Modifier.height(20.dp))
            Text("PENCARIAN DATA", color = GlassAccentCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))

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

            Spacer(Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    color = GlassAccentCyan.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                    border = BorderStroke(1.dp, GlassAccentCyan.copy(alpha = 0.3f))
                ) {
                    Text(
                        "HASIL DATA KVAR",
                        color = GlassAccentCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
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
            .height(40.dp)
            .clickable(onClick = onClick),
        border = BorderStroke(1.dp, GlassBorder),
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.05f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = displayText,
                modifier = Modifier.weight(1f),
                color = if (isPlaceholder) GlassTextMuted else Color.White,
                fontSize = 11.sp,
                fontStyle = if (isPlaceholder) FontStyle.Italic else FontStyle.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (isDropdownIcon) {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = GlassTextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
            if (isSearchIcon) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(GlassAccentCyan, CircleShape)
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
        color = GlassSurface,
        border = BorderStroke(1.dp, GlassBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().background(GlassAccentCyan.copy(alpha = 0.1f))) {
                TableCellLocal("Tgl", weight = 1.2f, isHeader = true)
                TableCellLocal("Jam", weight = 1f, isHeader = true)
                TableCellLocal("KWH", weight = 1f, isHeader = true)
                TableCellLocal("KVAR", weight = 1f, isHeader = true)
                TableCellLocal("Selisih", weight = 1f, isHeader = true)
                TableCellLocal("Kesimpulan", weight = 1.5f, isHeader = true)
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GlassAccentCyan)
                }
            } else if (data.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Tidak ada data", color = GlassTextMuted, fontSize = 14.sp)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(data) { index, item ->
                        val bgColor = if (index % 2 != 0) Color.White.copy(alpha = 0.02f) else Color.Transparent
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
                        HorizontalDivider(color = GlassBorder)
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
        color = if (isHeader) GlassAccentCyan else Color.White,
        fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
        fontSize = if (isHeader) 10.sp else 9.sp,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Preview(showBackground = true)
@Composable
fun DataListrikScreenPreview() {
    val sampleData = remember {
        listOf(
            JSONObject().apply {
                put("tanggal", "01/04/2024")
                put("jam", "08:00")
                put("kwh", "100.5")
                put("kvar", "50.2")
                put("selisih", "50.3")
                put("kesimpulan", "AMAN")
            },
            JSONObject().apply {
                put("tanggal", "02/04/2024")
                put("jam", "09:00")
                put("kwh", "120.0")
                put("kvar", "130.0")
                put("selisih", "-10.0")
                put("kesimpulan", "POTENSI DENDA")
            },
            JSONObject().apply {
                put("tanggal", "03/04/2024")
                put("jam", "10:00")
                put("kwh", "110.0")
                put("kvar", "105.0")
                put("selisih", "5.0")
                put("kesimpulan", "AMAN")
            }
        )
    }

    SiTekiVer01Theme {
        DataListrikScreenContent(
            selectedBulan = "April",
            tanggalRangeText = "1 Apr - 30 Apr",
            dataList = sampleData,
            isLoading = false,
            onBack = {},
            onEditData = {},
            onBulanClick = {},
            onTanggalClick = {},
            onSearchClick = {}
        )
    }
}
