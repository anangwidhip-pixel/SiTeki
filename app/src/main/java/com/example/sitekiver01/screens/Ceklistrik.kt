package com.example.sitekiver01.screens

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sitekiver01.components.*
import com.example.sitekiver01.ui.theme.*
import com.example.sitekiver01.OrbitronFontFamily
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

// URL SCRIPT GOOGLE SHEETS
const val SCRIPT_URL = "https://script.google.com/macros/s/AKfycbx4YbnLXFsnwDDV-Kso7Lx3Cu2R6tEYBkaEnRM_fnU-RBUoSWo-xZR9DIoHfzjwYd0/exec"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CekListrikScreen(
    onBack: () -> Unit,
    onNavigateToDetail: () -> Unit,
    editData: JSONObject? = null,
    onEditFinished: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val localDensity = LocalDensity.current // PENTING: Ambil density secara aman di level atas composable

    fun getCurrentDate(): String = String.format(Locale.getDefault(), "%02d/%02d/%04d", Calendar.getInstance().get(Calendar.DAY_OF_MONTH), Calendar.getInstance().get(Calendar.MONTH) + 1, Calendar.getInstance().get(Calendar.YEAR))
    fun getCurrentTime(): String = String.format(Locale.getDefault(), "%02d:%02d", Calendar.getInstance().get(Calendar.HOUR_OF_DAY), Calendar.getInstance().get(Calendar.MINUTE))

    var tanggalPrev by remember { mutableStateOf("-") }
    var tanggalCurrent by remember { mutableStateOf(editData?.optString("tanggal") ?: getCurrentDate()) }
    var jamPrev by remember { mutableStateOf("-") }
    var jamCurrent by remember { mutableStateOf(editData?.optString("jam") ?: getCurrentTime()) }

    var huheHPrev by remember { mutableStateOf("0.0") }
    var huheHCurrent by remember { mutableStateOf(editData?.optString("huhe_h") ?: "") }
    var huheHHPrev by remember { mutableStateOf("0.0") }
    var huheHHCurrent by remember { mutableStateOf(editData?.optString("huhe_hh") ?: "") }
    var huarHEHPrev by remember { mutableStateOf("0.0") }
    var huarHEHCurrent by remember { mutableStateOf(editData?.optString("huar_heh") ?: "") }
    var huarHHPrev by remember { mutableStateOf("0.0") }
    var huarHHCurrent by remember { mutableStateOf(editData?.optString("huar_hh") ?: "") }

    var gridPlnPrev by remember { mutableStateOf("0.0") }
    var gridPlnCurrent by remember { mutableStateOf(editData?.optString("grid_pln") ?: "") }
    var pvPltsPrev by remember { mutableStateOf("0.0") }
    var pvPltsCurrent by remember { mutableStateOf(editData?.optString("pv_plts") ?: "") }
    var toGridPrev by remember { mutableStateOf("0.0") }
    var toGridCurrent by remember { mutableStateOf(editData?.optString("to_grid") ?: "") }

    var petugasPrev by remember { mutableStateOf("-") }
    var petugasCurrent by remember { mutableStateOf(editData?.optString("petugas") ?: "Pilih") }
    var petugasList by remember { mutableStateOf<List<String>>(emptyList()) }
    var showPetugasDropdown by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    val hH = huheHCurrent.toFloatOrNull() ?: 0f
    val hHH = huheHHCurrent.toFloatOrNull() ?: 0f
    val aHEH = huarHEHCurrent.toFloatOrNull() ?: 0f
    val aHH = huarHHCurrent.toFloatOrNull() ?: 0f

    val nilaiKWH = (hH - hHH) * 0.62f
    val nilaiKVAR = aHEH - aHH
    val selisih = nilaiKWH - nilaiKVAR

    val isCalculated = huheHCurrent.isNotEmpty() && huheHHCurrent.isNotEmpty() && huarHEHCurrent.isNotEmpty() && huarHHCurrent.isNotEmpty()

    val infiniteTransition = rememberInfiniteTransition(label = "blink")
    val alphaAnim by infiniteTransition.animateFloat(initialValue = 1f, targetValue = 0f, animationSpec = infiniteRepeatable(animation = tween(500), repeatMode = RepeatMode.Reverse), label = "alpha")

    val fetchData = suspend {
        isLoading = true
        withContext(Dispatchers.IO) {
            try {
                val url = URL(SCRIPT_URL)
                val conn = url.openConnection() as HttpURLConnection
                conn.instanceFollowRedirects = true

                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)

                if (json.has("prevData")) {
                    val prev = json.getJSONObject("prevData")
                    tanggalPrev = prev.optString("tanggal", "-")
                    jamPrev = prev.optString("jam", "-")
                    huheHPrev = prev.optString("huhe_h", "0.0")
                    huheHHPrev = prev.optString("huhe_hh", "0.0")
                    huarHEHPrev = prev.optString("huar_heh", "0.0")
                    huarHHPrev = prev.optString("huar_hh", "0.0")
                    if (editData == null && huarHHCurrent.isEmpty()) huarHHCurrent = huarHHPrev
                    gridPlnPrev = prev.optString("grid_pln", "0.0")
                    pvPltsPrev = prev.optString("pv_plts", "0.0")
                    toGridPrev = prev.optString("to_grid", "0.0")
                    petugasPrev = prev.optString("petugas", "-")
                }

                if (json.has("petugas")) {
                    val petArray = json.getJSONArray("petugas")
                    val list = mutableListOf<String>()
                    for (i in 0 until petArray.length()) list.add(petArray.getString(i))
                    petugasList = list
                }
            } catch (e: Exception) {
                Log.e("CekListrik", "Error fetching data: ${e.message}", e)
            }
        }
        isLoading = false
        isRefreshing = false
    }

    LaunchedEffect(Unit) { fetchData() }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            containerColor = Color(0xFF0F172A),
            modifier = Modifier.border(1.dp, SciFiBorderLight, RoundedCornerShape(28.dp)),
            title = { Text(text = "Konfirmasi Data", fontWeight = FontWeight.Bold, color = Color.White, fontFamily = OrbitronFontFamily) },
            text = { Text(text = "Pastikan Data Anda Benar! Apakah Ingin Periksa Data Atau Simpan", color = SciFiTextMuted) },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        scope.launch {
                            isLoading = true
                            val kesSimpan = if (nilaiKWH > nilaiKVAR) "AMAN" else "POTENSI DENDA"
                            val result = submitData(tanggalCurrent, jamCurrent, hH, hHH, aHEH, aHH, gridPlnCurrent.toFloatOrNull() ?: 0f, pvPltsCurrent.toFloatOrNull() ?: 0f, toGridCurrent.toFloatOrNull() ?: 0f, petugasCurrent, nilaiKWH, nilaiKVAR, selisih, kesSimpan, editRow = editData?.optInt("row"))
                            if (result) {
                                if (editData == null) {
                                    huheHCurrent = ""; huheHHCurrent = ""; huarHEHCurrent = ""; huarHHCurrent = ""
                                    gridPlnCurrent = ""; pvPltsCurrent = ""; toGridCurrent = ""
                                    petugasCurrent = "Pilih"; tanggalCurrent = getCurrentDate(); jamCurrent = getCurrentTime(); fetchData()
                                } else {
                                    onEditFinished()
                                    onBack()
                                }
                            }
                            isLoading = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SciFiCyan)
                ) { Text(text = "Simpan", color = Color.Black, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text(text = "Cek", color = SciFiTextMuted)
                }
            }
        )
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
                            modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)
                        ) { Icon(Icons.Default.ArrowBackIosNew, "Back", tint = MaterialTheme.colorScheme.onSurface) }
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = if (editData == null) "Energi Listrik" else "Edit Data Listrik",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            fontFamily = OrbitronFontFamily,
                            letterSpacing = 1.sp
                        )
                        Spacer(Modifier.weight(1f))
                        IconButton(
                            onClick = onNavigateToDetail,
                            modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Icon(Icons.Default.History, "History", tint = Color.White)
                        }
                    }
                }
            }
        ) { innerPadding ->
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { isRefreshing = true; scope.launch { fetchData() } },
                modifier = Modifier.padding(innerPadding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // INFO SECTION
                    GlassCard {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, null, tint = SciFiCyan, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("INFO TERAKHIR", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            HorizontalDivider(color = SciFiBorderLight)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                InfoItem("TANGGAL", tanggalPrev)
                                InfoItem("JAM", jamPrev)
                                InfoItem("PETUGAS", petugasPrev)
                            }
                        }
                    }

                    // INPUT SECTION
                    GlassCard {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Bolt, null, tint = SciFiCyan)
                                Spacer(Modifier.width(8.dp))
                                Text("INPUT DATA KWH", color = Color.White, fontWeight = FontWeight.Bold)
                            }

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(Modifier.weight(1f)) {
                                    ModernDualEditField("HUHE H (KWH)", huheHPrev, huheHCurrent) { huheHCurrent = it }
                                }
                                Box(Modifier.weight(1f)) {
                                    ModernDualEditField("HUHE HH (KWH)", huheHHPrev, huheHHCurrent) { huheHHCurrent = it }
                                }
                            }

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(Modifier.weight(1f)) {
                                    ModernDualEditField("HUAR HEH (KVAR)", huarHEHPrev, huarHEHCurrent) { huarHEHCurrent = it }
                                }
                                Box(Modifier.weight(1f)) {
                                    ModernDualEditField("HUAR HH (KVAR)", huarHHPrev, huarHHCurrent) { huarHHCurrent = it }
                                }
                            }
                        }
                    }

                    // PLTS SECTION
                    GlassCard {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.SolarPower, null, tint = SciFiCyan)
                                Spacer(Modifier.width(8.dp))
                                Text("INPUT PLTS & GRID", color = Color.White, fontWeight = FontWeight.Bold)
                            }

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(Modifier.weight(1f)) {
                                    ModernDualEditField("GRID PLN", gridPlnPrev, gridPlnCurrent) { gridPlnCurrent = it }
                                }
                                Box(Modifier.weight(1f)) {
                                    ModernDualEditField("PV PLTS", pvPltsPrev, pvPltsCurrent) { pvPltsCurrent = it }
                                }
                                Box(Modifier.weight(1f)) {
                                    ModernDualEditField("TO GRID", toGridPrev, toGridCurrent) { toGridCurrent = it }
                                }
                            }
                        }
                    }

                    // CALCULATION SECTION
                    if (isCalculated) {
                        val statusColor = if (nilaiKWH > nilaiKVAR) SciFiCyan else Color(0xFFFF5252)
                        val statusText = if (nilaiKWH > nilaiKVAR) "AMAN" else "POTENSI DENDA"

                        GlassCard(borderColor = statusColor.copy(alpha = 0.5f)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Calculate, null, tint = statusColor)
                                    Spacer(Modifier.width(8.dp))
                                    Text("HASIL PERHITUNGAN", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                HorizontalDivider(color = SciFiBorderLight)
                                CalculationRow("NILAI KWH (0.62)", String.format("%.2f", nilaiKWH), Color.White)
                                CalculationRow("NILAI KVAR", String.format("%.2f", nilaiKVAR), Color.White)
                                CalculationRow("SELISIH", String.format("%.2f", selisih), statusColor)

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                        .background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                        .border(1.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = statusText,
                                        color = statusColor,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        modifier = Modifier.alpha(if (statusText == "POTENSI DENDA") alphaAnim else 1f)
                                    )
                                }
                            }
                        }
                    }

                    // DATE & PETUGAS SECTION
                    GlassCard {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(Modifier.weight(1f)) {
                                    Text("TANGGAL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SciFiCyan)
                                    OutlinedButton(
                                        onClick = {
                                            val c = Calendar.getInstance()
                                            DatePickerDialog(context, { _, y, m, d ->
                                                tanggalCurrent = String.format("%02d/%02d/%04d", d, m + 1, y)
                                            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                                        },
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, SciFiBorderLight),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                    ) { Text(tanggalCurrent) }
                                }
                                Column(Modifier.weight(1f)) {
                                    Text("JAM", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SciFiCyan)
                                    OutlinedButton(
                                        onClick = {
                                            val c = Calendar.getInstance()
                                            TimePickerDialog(context, { _, h, min ->
                                                jamCurrent = String.format("%02d:%02d", h, min)
                                            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()
                                        },
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, SciFiBorderLight),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                    ) { Text(jamCurrent) }
                                }
                            }

                            Column {
                                Text("PETUGAS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SciFiCyan)
                                Box(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                                    OutlinedButton(
                                        onClick = { showPetugasDropdown = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, SciFiBorderLight),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                    ) {
                                        Text(petugasCurrent)
                                        Spacer(Modifier.weight(1f))
                                        Icon(Icons.Default.ArrowDropDown, null)
                                    }
                                    DropdownMenu(
                                        expanded = showPetugasDropdown,
                                        onDismissRequest = { showPetugasDropdown = false },
                                        modifier = Modifier.background(Color(0xFF0F172A)).border(1.dp, SciFiBorderLight)
                                    ) {
                                        petugasList.forEach { p ->
                                            DropdownMenuItem(
                                                text = { Text(p, color = Color.White) },
                                                onClick = { petugasCurrent = p; showPetugasDropdown = false }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // GLOW SUBMIT BUTTON (PERBAIKAN TOTAL: Konversi murni lewat density aman)
                    val infiniteTransitionPulse = rememberInfiniteTransition(label = "btnPulseGlow")
                    val glowBlur by infiniteTransitionPulse.animateFloat(
                        initialValue = 6f,
                        targetValue = 14f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ), label = "btnPulse"
                    )

                    Box(
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Perbaikan rumus pembagian densitas tanpa memicu 'unresolved reference run'
                        val blurRadiusDp = (glowBlur / localDensity.density).dp

                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .fillMaxHeight(0.8f)
                                .background(SciFiCyan.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                .blur(blurRadiusDp)
                        )

                        Button(
                            onClick = {
                                if (huheHCurrent.isEmpty() || huheHHCurrent.isEmpty() || huarHEHCurrent.isEmpty() || huarHHCurrent.isEmpty() || petugasCurrent == "Pilih") {
                                    android.widget.Toast.makeText(context, "Lengkapi Semua Data!", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    showConfirmDialog = true
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.horizontalGradient(colors = listOf(SciFiCyan, SciFiBlue)), RoundedCornerShape(16.dp))
                                    .border(1.dp, SciFiBorderMedium, RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                                } else {
                                    Text(
                                        text = if (editData == null) "KIRIM DATA" else "UPDATE DATA",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        fontFamily = OrbitronFontFamily
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
fun InfoItem(label: String, value: String) {
    Column {
        Text(label, fontSize = 10.sp, color = SciFiCyan, fontWeight = FontWeight.Bold)
        Text(value, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun GlassCard(
    borderColor: Color = SciFiBorderLight,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SciFiGlass,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Brush.verticalGradient(listOf(borderColor, Color.Transparent)))
    ) {
        content()
    }
}

@Composable
fun ModernDualEditField(label: String, prev: String, curr: String, onValueChange: (String) -> Unit) {
    Column {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SciFiCyan, modifier = Modifier.padding(bottom = 4.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, SciFiBorderLight),
            shape = RoundedCornerShape(16.dp),
            color = Color.White.copy(alpha = 0.03f)
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(prev, fontSize = 10.sp, color = SciFiTextMuted)
                BasicTextField(
                    value = curr,
                    onValueChange = onValueChange,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    cursorBrush = SolidColor(SciFiCyan),
                    decorationBox = { innerTextField: @Composable () -> Unit ->
                        if (curr.isEmpty()) Text("0.0", color = SciFiTextMuted.copy(alpha = 0.4f), fontSize = 15.sp)
                        innerTextField()
                    }
                )
            }
        }
    }
}

@Composable
fun CalculationRow(label: String, value: String, color: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = SciFiTextMuted, fontWeight = FontWeight.Medium)
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = color)
    }
}

private suspend fun submitData(tgl: String, jam: String, hH: Float, hHH: Float, aHEH: Float, aHH: Float, grid: Float, pv: Float, toG: Float, ptg: String, kwh: Float, kvar: Float, slsh: Float, kes: String, editRow: Int?): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val urlString = "$SCRIPT_URL?action=insert" +
                    "&tanggal=${java.net.URLEncoder.encode(tgl, "UTF-8")}" +
                    "&jam=${java.net.URLEncoder.encode(jam, "UTF-8")}" +
                    "&huhe_h=$hH&huhe_hh=$hHH&huar_heh=$aHEH&huar_hh=$aHH" +
                    "&grid_pln=$grid&pv_plts=$pv&to_grid=$toG" +
                    "&petugas=${java.net.URLEncoder.encode(ptg, "UTF-8")}" +
                    "&nilai_kwh=$kwh&nilai_kvar=$kvar&selisih=$slsh&kesimpulan=$kes" +
                    if (editRow != null) "&row=$editRow" else ""

            val conn = URL(urlString).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.instanceFollowRedirects = true
            val response = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            json.optString("status") == "success"
        } catch (e: Exception) {
            Log.e("CekListrik", "Submit Error", e)
            false
        }
    }
}
