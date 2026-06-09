package com.example.sitekiver01.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sitekiver01.OrbitronFontFamily
import com.example.sitekiver01.UserSession
import com.example.sitekiver01.ui.theme.*
import com.example.sitekiver01.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

// Model data internal riwayat lemburan siber
data class LemburData(
    val tanggal: String,
    val nama: String,
    val jam: Double,
    val jenis: String,
    val upah: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IsiLemburanScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onNavigateToRekap: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State Form Utama
    var tanggal by remember { mutableStateOf("") }
    var jam by remember { mutableStateOf("") }
    var isHariBesar by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }

    // State Internal Pemuatan Data Rekapitulasi Siber
    var listLemburRaw by remember { mutableStateOf<List<LemburData>>(emptyList()) }
    var isLoadingRekap by remember { mutableStateOf(false) }
    var isRekapVisible by remember { mutableStateOf(false) }

    val webAppUrl = "https://script.google.com/macros/s/AKfycbzt4A4mmIfd-PRv5j40SkOEtKRmXjM70MCS3axCB8WBEYc-V10z7ur_SWXlX5wkqriI/exec"

    // AMBIL DATA OTOMATIS BERDASARKAN KEY UNIT
    LaunchedEffect(Unit) {
        isLoadingRekap = true
        withContext(Dispatchers.IO) {
            try {
                val namaClean = UserSession.namaFull.trim()
                val namaEncoded = URLEncoder.encode(namaClean, "UTF-8")
                val urlAmbilData = "$webAppUrl?nama=$namaEncoded"

                val url = URL(urlAmbilData)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 15000
                conn.readTimeout = 15000

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val resText = conn.inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONArray(resText)
                    val tempList = mutableListOf<LemburData>()

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        tempList.add(
                            LemburData(
                                tanggal = obj.optString("tanggal", ""),
                                nama = obj.optString("nama", ""),
                                jam = obj.optDouble("jam", 0.0),
                                jenis = obj.optString("jenis", ""),
                                upah = obj.optDouble("upah", 0.0)
                            )
                        )
                    }
                    listLemburRaw = tempList
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoadingRekap = false
            }
        }
    }

    // LOGIKA FILTER AMAN DI SISI KOTLIN (22 BULAN LALU s/d 21 BULAN SEKARANG)
    val rekapData = remember(listLemburRaw, isRekapVisible) {
        val calendar = Calendar.getInstance()
        val sdfApp = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val currentMonthStr = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)

        calendar.set(Calendar.DAY_OF_MONTH, 21)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        val tglAkhir = calendar.time
        val tglAkhirStr = sdfApp.format(tglAkhir)

        calendar.add(Calendar.MONTH, -1)
        calendar.set(Calendar.DAY_OF_MONTH, 22)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        val tglAwal = calendar.time
        val tglAwalStr = sdfApp.format(tglAwal)

        val filtered = listLemburRaw.filter { item ->
            try {
                val tanggalNormal = item.tanggal.replace("-", "/")
                val itemDate = try {
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(tanggalNormal)
                } catch (e: Exception) {
                    SimpleDateFormat("d/M/yyyy", Locale.getDefault()).parse(tanggalNormal)
                }

                val namaSheetLower = item.nama.trim().lowercase()
                val namaLoginLower = UserSession.namaFull.trim().lowercase()
                val matchesUser = namaSheetLower.contains(namaLoginLower) || namaLoginLower.contains(namaSheetLower)

                val inRange = itemDate != null && !itemDate.before(tglAwal) && !itemDate.after(tglAkhir)
                matchesUser && inRange
            } catch (e: Exception) {
                false
            }
        }

        val totalUpah = filtered.sumOf { item -> item.upah }
        Triple(filtered, totalUpah, "$tglAwalStr - $tglAkhirStr")
    }

    val filteredLembur = rekapData.first
    val totalUpahLembur = rekapData.second
    val labelPeriodeCutoff = rekapData.third

    // DatePicker Logic
    val calendarPicker = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, day -> tanggal = "$day/${month + 1}/$year" },
        calendarPicker.get(Calendar.YEAR),
        calendarPicker.get(Calendar.MONTH),
        calendarPicker.get(Calendar.DAY_OF_MONTH)
    )

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
                        "ISI LEMBURAN",
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

            // TOMBOL UTAMA: TOGGLE REKAP LEMBURAN
            Button(
                onClick = { isRekapVisible = !isRekapVisible },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRekapVisible) Color(0xFFC23B22) else GlassAccentCyan
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isLoadingRekap) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(18.dp))
                    } else {
                        Icon(
                            imageVector = if (isRekapVisible) Icons.Default.Close else Icons.Default.Calculate,
                            contentDescription = null,
                            tint = Color.Black
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isRekapVisible) "TUTUP REKAP" else "REKAP LEMBURAN SAYA",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontFamily = OrbitronFontFamily,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // PANEL VISUAL REKAPITULASI DUA-DIGIT CUTOFF
            AnimatedVisibility(
                visible = isRekapVisible,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                ModernFormCard(modifier = Modifier.padding(bottom = 16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("PERIODE CUTOFF", fontSize = 10.sp, color = GlassAccentCyan, fontWeight = FontWeight.Bold, fontFamily = OrbitronFontFamily)
                            Box(
                                modifier = Modifier
                                    .background(GlassAccentCyan.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(if (isLoadingRekap) "LOADING" else "AKTIF", fontSize = 9.sp, color = GlassAccentCyan, fontWeight = FontWeight.Bold)
                            }
                        }

                        Text(
                            text = labelPeriodeCutoff,
                            fontSize = 13.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )

                        HorizontalDivider(color = GlassBorder, thickness = 1.dp)

                        Text("Rincian Kehadiran (${filteredLembur.size}x):", fontSize = 11.sp, color = GlassTextMuted)

                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.heightIn(max = 160.dp).verticalScroll(rememberScrollState())
                        ) {
                            if (filteredLembur.isEmpty()) {
                                Text(
                                    text = if (isLoadingRekap) "Menghubungi Server..." else "Belum ada rekaman lembur di periode ini.",
                                    color = GlassTextMuted,
                                    fontSize = 12.sp,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                filteredLembur.forEach { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(8.dp))
                                            .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(item.tanggal, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Medium)
                                            Text("${item.jam} Jam - ${item.jenis}", fontSize = 10.sp, color = GlassTextMuted)
                                        }
                                        Text(
                                            text = "Rp ${String.format("%,d", item.upah.toLong())}",
                                            fontSize = 12.sp,
                                            color = GlassAccentCyan,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = OrbitronFontFamily
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = GlassBorder, thickness = 1.dp)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(GlassAccentCyan.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                .border(1.dp, GlassAccentCyan.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("TOTAL ESTIMASI UPAH", fontSize = 11.sp, color = Color.White, fontFamily = OrbitronFontFamily, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Rp ${String.format("%,d", totalUpahLembur.toLong())}",
                                fontSize = 15.sp,
                                color = GlassAccentCyan,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = OrbitronFontFamily
                            )
                        }
                    }
                }
            }

            // FORM ENTRI INPUT DATA BARU
            ModernSectionHeader("INPUT DATA LEMBUR", Icons.Default.PostAdd)

            ModernFormCard(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // FIELD TANGGAL
                    item {
                        Column {
                            Text("Tanggal Kerja", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GlassAccentCyan, modifier = Modifier.padding(start = 2.dp, bottom = 6.dp))
                            ModernSearchField(
                                tanggal,
                                "Pilih Tanggal...",
                                { datePickerDialog.show() },
                                true
                            )
                        }
                    }

                    // FIELD JAM LEMBUR
                    item {
                        Column {
                            Text("Jumlah Jam Kerja (Durasi)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GlassAccentCyan, modifier = Modifier.padding(start = 2.dp, bottom = 6.dp))
                            OutlinedTextField(
                                value = jam,
                                onValueChange = { jam = it },
                                placeholder = { Text("Contoh: 3.5", color = GlassTextMuted) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GlassAccentCyan,
                                    unfocusedBorderColor = GlassBorder,
                                    focusedContainerColor = Color.White.copy(alpha = 0.03f),
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.03f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                        }
                    }

                    // SWITCH TOGGLE HARI LIBUR / BESAR
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.02f),
                            border = BorderStroke(1.dp, GlassBorder)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Hari Libur / Besar", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text("Aktifkan jika lembur pada tanggal merah", color = GlassTextMuted, fontSize = 10.sp)
                                }
                                Switch(
                                    checked = isHariBesar,
                                    onCheckedChange = { isHariBesar = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.Black,
                                        checkedTrackColor = GlassAccentCyan,
                                        uncheckedThumbColor = GlassTextMuted,
                                        uncheckedTrackColor = Color.Transparent
                                    )
                                )
                            }
                        }
                    }

                    // TOMBOL SIMPAN DATA LEMBURAN KE SERVER
                    item {
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (tanggal.isEmpty() || jam.isEmpty()) {
                                    Toast.makeText(context, "Lengkapi data!", Toast.LENGTH_SHORT).show()
                                } else {
                                    isUploading = true
                                    scope.launch {
                                        val success = kirimLemburan(
                                            webAppUrl = webAppUrl,
                                            tgl = tanggal,
                                            nama = UserSession.namaFull,
                                            role = UserSession.role,
                                            jam = jam.toDoubleOrNull() ?: 0.0,
                                            isHariBesar = isHariBesar
                                        )
                                        isUploading = false
                                        if (success) {
                                            Toast.makeText(context, "Data Berhasil Masuk!", Toast.LENGTH_SHORT).show()
                                            onBack()
                                        } else {
                                            Toast.makeText(context, "Gagal Terhubung!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            enabled = !isUploading,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GlassAccentCyan)
                        ) {
                            if (isUploading) {
                                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                            } else {
                                Text("SIMPAN LEMBURAN", color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = OrbitronFontFamily, fontSize = 13.sp)
                            }
                        }
                    }

                    // HAK AKSES ADMIN
                    item {
                        if (UserSession.role == "Admin") {
                            HorizontalDivider(color = GlassBorder, modifier = Modifier.padding(vertical = 4.dp))
                            OutlinedButton(
                                onClick = { onNavigateToRekap() },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, GlassAccentCyan),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = GlassAccentCyan)
                            ) {
                                Icon(Icons.Default.AdminPanelSettings, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("REKAP SELURUH USER (ADMIN)", fontFamily = OrbitronFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// FUNGSI PENGIRIM DATA POST KE GOOGLE SHEETS
suspend fun kirimLemburan(webAppUrl: String, tgl: String, nama: String, role: String, jam: Double, isHariBesar: Boolean): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val url = URL(webAppUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")

            val json = JSONObject().apply {
                put("tanggal", tgl)
                put("nama", nama)
                put("role", role)
                put("jam", jam)
                put("jenis", if (isHariBesar) "HariBesar" else if (jam < 2.0) "Normal_Kecil" else "Normal_Besar")
            }

            conn.outputStream.use { it.write(json.toString().toByteArray()) }
            conn.inputStream.bufferedReader().use { it.readText() }
            true
        } catch (e: Exception) {
            false
        }
    }
}