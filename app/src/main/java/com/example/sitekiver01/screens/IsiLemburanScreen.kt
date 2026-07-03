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
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log

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

    // Bulan yang dipilih adalah bulan awal cutoff.
    // Contoh: Juli 2026 = 22 Juli 2026 sampai 21 Agustus 2026.
    val initialCutoffCalendar = remember { Calendar.getInstance() }
    var selectedCutoffMonth by remember {
        mutableIntStateOf(initialCutoffCalendar.get(Calendar.MONTH))
    }
    var selectedCutoffYear by remember {
        mutableIntStateOf(initialCutoffCalendar.get(Calendar.YEAR))
    }
    var showCutoffMonthPicker by remember { mutableStateOf(false) }

    val webAppUrl = "https://script.google.com/macros/s/AKfycbzwB_hSKNsldfym-QNOBfo7QhvBzsjqyg_MkHcoRdK9BYx6UAgOFAybejeA_tAb_QLy/exec"

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

    // FILTER BERDASARKAN BULAN CUTOFF YANG DIPILIH:
    // tanggal 22 bulan terpilih sampai tanggal 21 bulan berikutnya.
    val rekapData = remember(
        listLemburRaw,
        selectedCutoffMonth,
        selectedCutoffYear
    ) {
        val localeIndonesia = Locale("id", "ID")
        val dateFormatter = SimpleDateFormat("dd MMMM yyyy", localeIndonesia)

        val startCalendar = Calendar.getInstance().apply {
            clear()
            set(selectedCutoffYear, selectedCutoffMonth, 22, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val endCalendar = (startCalendar.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
            set(Calendar.DAY_OF_MONTH, 21)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        val tanggalAwal = startCalendar.time
        val tanggalAkhir = endCalendar.time

        val filtered = listLemburRaw
            .filter { item ->
                val itemDate = parseLemburDate(item.tanggal)
                val namaSheetLower = item.nama.trim().lowercase(localeIndonesia)
                val namaLoginLower = UserSession.namaFull.trim().lowercase(localeIndonesia)

                val matchesUser =
                    namaSheetLower.contains(namaLoginLower) ||
                            namaLoginLower.contains(namaSheetLower)

                val inRange = itemDate != null &&
                        !itemDate.before(tanggalAwal) &&
                        !itemDate.after(tanggalAkhir)

                matchesUser && inRange
            }
            .sortedByDescending { parseLemburDate(it.tanggal)?.time ?: 0L }

        val totalUpah = filtered.sumOf { item -> item.upah }
        val labelPeriode =
            "${dateFormatter.format(tanggalAwal)} - ${dateFormatter.format(tanggalAkhir)}"

        Triple(filtered, totalUpah, labelPeriode)
    }

    val filteredLembur = rekapData.first
    val totalUpahLembur = rekapData.second
    val labelPeriodeCutoff = rekapData.third

    val selectedCutoffMonthLabel = remember(
        selectedCutoffMonth,
        selectedCutoffYear
    ) {
        val calendar = Calendar.getInstance().apply {
            clear()
            set(selectedCutoffYear, selectedCutoffMonth, 1)
        }
        SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
            .format(calendar.time)
            .replaceFirstChar { it.uppercase() }
    }

    // DatePicker Logic
    val calendarPicker = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, day -> tanggal = "$day/${month + 1}/$year" },
        calendarPicker.get(Calendar.YEAR),
        calendarPicker.get(Calendar.MONTH),
        calendarPicker.get(Calendar.DAY_OF_MONTH)
    )

    // Function untuk refresh data
    fun refreshData() {
        isLoadingRekap = true
        scope.launch {
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
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Tombol Refresh Manual
                                IconButton(
                                    onClick = { refreshData() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = "Refresh",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .background(GlassAccentCyan.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(if (isLoadingRekap) "LOADING" else "AKTIF", fontSize = 9.sp, color = GlassAccentCyan, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "CARI BULAN CUTOFF",
                                fontSize = 10.sp,
                                color = GlassTextMuted,
                                fontWeight = FontWeight.Bold,
                                fontFamily = OrbitronFontFamily
                            )

                            OutlinedButton(
                                onClick = { showCutoffMonthPicker = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, GlassAccentCyan.copy(alpha = 0.55f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = GlassAccentCyan.copy(alpha = 0.06f),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = GlassAccentCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = selectedCutoffMonthLabel,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Start,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Cari bulan",
                                    tint = GlassAccentCyan,
                                    modifier = Modifier.size(18.dp)
                                )
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

                        // Daftar lemburan dengan tombol edit dan hapus
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.heightIn(max = 200.dp).verticalScroll(rememberScrollState())
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
                                    // State untuk dialog
                                    var showDeleteDialog by remember { mutableStateOf(false) }
                                    var showEditDialog by remember { mutableStateOf(false) }
                                    var isDeleting by remember { mutableStateOf(false) }
                                    var isEditing by remember { mutableStateOf(false) }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(8.dp))
                                            .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Informasi lemburan
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.tanggal, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Medium)
                                            Text("${item.jam} Jam - ${item.jenis}", fontSize = 10.sp, color = GlassTextMuted)
                                        }

                                        // Upah
                                        Text(
                                            text = "Rp ${String.format("%,d", item.upah.toLong())}",
                                            fontSize = 12.sp,
                                            color = GlassAccentCyan,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = OrbitronFontFamily
                                        )

                                        Spacer(Modifier.width(4.dp))

                                        // Tombol Edit
                                        IconButton(
                                            onClick = { showEditDialog = true },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Edit",
                                                tint = Color(0xFF4CAF50),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        // Tombol Hapus
                                        IconButton(
                                            onClick = { showDeleteDialog = true },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            if (isDeleting) {
                                                CircularProgressIndicator(color = Color(0xFFC23B22), modifier = Modifier.size(14.dp))
                                            } else {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Hapus",
                                                    tint = Color(0xFFC23B22),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Dialog Konfirmasi Hapus
                                    if (showDeleteDialog) {
                                        AlertDialog(
                                            onDismissRequest = { showDeleteDialog = false },
                                            containerColor = Color(0xFF1A1A2E),
                                            title = {
                                                Text(
                                                    "HAPUS DATA LEMBURAN",
                                                    fontFamily = OrbitronFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp,
                                                    color = Color.White
                                                )
                                            },
                                            text = {
                                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Text("Anda yakin ingin menghapus data berikut?", color = Color.White)
                                                    Spacer(Modifier.height(8.dp))
                                                    Text("Tanggal: ${item.tanggal}", color = Color.White, fontWeight = FontWeight.Bold)
                                                    Text("Jam: ${item.jam} Jam", color = Color.White)
                                                    Text("Jenis: ${item.jenis}", color = Color.White)
                                                    Text("Upah: Rp ${String.format("%,d", item.upah.toLong())}", color = GlassAccentCyan)
                                                }
                                            },
                                            confirmButton = {
                                                Button(
                                                    onClick = {
                                                        isDeleting = true
                                                        scope.launch {
                                                            // Kirim dengan format yang sudah pasti benar
                                                            val (isSuccess, pesan) = hapusLemburan(
                                                                webAppUrl = webAppUrl,
                                                                tanggal = item.tanggal, // Dari LemburData
                                                                nama = UserSession.namaFull // Dari session
                                                            )
                                                            isDeleting = false
                                                            showDeleteDialog = false

                                                            if (isSuccess) {
                                                                Toast.makeText(context, "Data berhasil dihapus!", Toast.LENGTH_SHORT).show()
                                                                refreshData()
                                                            } else {
                                                                Toast.makeText(context, "Gagal hapus: $pesan", Toast.LENGTH_LONG).show()
                                                            }
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC23B22))
                                                ) {
                                                    if (isDeleting) {
                                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                                                    } else {
                                                        Text("HAPUS", color = Color.White, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            },
                                            dismissButton = {
                                                OutlinedButton(onClick = { showDeleteDialog = false }) {
                                                    Text("BATAL", color = GlassAccentCyan)
                                                }
                                            }
                                        )
                                    }

                                    // Dialog Edit
                                    if (showEditDialog) {
                                        EditLemburDialog(
                                            itemToEdit = item,
                                            onDismiss = { showEditDialog = false },
                                            onSave = { tanggalBaru, jamBaru, isHariBesarBaru ->
                                                isEditing = true
                                                scope.launch {
                                                    val (isSuccess, pesan) = updateLemburan(
                                                        webAppUrl = webAppUrl,
                                                        tanggalLama = item.tanggal,
                                                        tanggalBaru = tanggalBaru,
                                                        nama = UserSession.namaFull,
                                                        role = UserSession.role,
                                                        jam = jamBaru,
                                                        isHariBesar = isHariBesarBaru
                                                    )
                                                    isEditing = false
                                                    showEditDialog = false

                                                    if (isSuccess) {
                                                        Toast.makeText(context, "Data berhasil diupdate!", Toast.LENGTH_SHORT).show()
                                                        refreshData()
                                                    } else {
                                                        Toast.makeText(context, "Gagal update: $pesan", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            },
                                            isSaving = isEditing
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
                                        val (isSuccess, pesan) = kirimLemburan(
                                            webAppUrl = webAppUrl,
                                            tgl = tanggal,
                                            nama = UserSession.namaFull,
                                            role = UserSession.role,
                                            jam = jam.toDoubleOrNull() ?: 0.0,
                                            isHariBesar = isHariBesar
                                        )

                                        isUploading = false

                                        if (isSuccess) {
                                            Toast.makeText(context, "Data Berhasil Masuk!", Toast.LENGTH_SHORT).show()
                                            refreshData()
                                            // Reset form
                                            tanggal = ""
                                            jam = ""
                                            isHariBesar = false
                                            onBack()
                                        } else {
                                            // Tampilkan pesan error spesifik dari server
                                            Toast.makeText(context, pesan, Toast.LENGTH_LONG).show()
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

    if (showCutoffMonthPicker) {
        CutoffMonthPickerDialog(
            selectedMonth = selectedCutoffMonth,
            selectedYear = selectedCutoffYear,
            onDismiss = { showCutoffMonthPicker = false },
            onMonthSelected = { month, year ->
                selectedCutoffMonth = month
                selectedCutoffYear = year
                showCutoffMonthPicker = false
            }
        )
    }
}

// =================================================================
// DIALOG PILIH BULAN CUTOFF
// =================================================================
@Composable
private fun CutoffMonthPickerDialog(
    selectedMonth: Int,
    selectedYear: Int,
    onDismiss: () -> Unit,
    onMonthSelected: (month: Int, year: Int) -> Unit
) {
    val localeIndonesia = remember { Locale("id", "ID") }
    val monthNames = remember {
        DateFormatSymbols(localeIndonesia).months.take(12)
    }

    var temporaryMonth by remember(selectedMonth) {
        mutableIntStateOf(selectedMonth)
    }
    var temporaryYear by remember(selectedYear) {
        mutableIntStateOf(selectedYear)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A2E),
        title = {
            Text(
                text = "PILIH BULAN CUTOFF",
                color = Color.White,
                fontFamily = OrbitronFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { temporaryYear-- }) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowLeft,
                            contentDescription = "Tahun sebelumnya",
                            tint = GlassAccentCyan
                        )
                    }

                    Text(
                        text = temporaryYear.toString(),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = OrbitronFontFamily
                    )

                    IconButton(onClick = { temporaryYear++ }) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "Tahun berikutnya",
                            tint = GlassAccentCyan
                        )
                    }
                }

                monthNames.chunked(3).forEachIndexed { rowIndex, monthsInRow ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        monthsInRow.forEachIndexed { columnIndex, monthName ->
                            val monthIndex = rowIndex * 3 + columnIndex
                            val isSelected = monthIndex == temporaryMonth

                            OutlinedButton(
                                onClick = { temporaryMonth = monthIndex },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) GlassAccentCyan else GlassBorder
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) {
                                        GlassAccentCyan
                                    } else {
                                        Color.Transparent
                                    },
                                    contentColor = if (isSelected) Color.Black else Color.White
                                )
                            ) {
                                Text(
                                    text = monthName.take(3).uppercase(localeIndonesia),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                val previewStart = Calendar.getInstance().apply {
                    clear()
                    set(temporaryYear, temporaryMonth, 22)
                }
                val previewEnd = (previewStart.clone() as Calendar).apply {
                    add(Calendar.MONTH, 1)
                    set(Calendar.DAY_OF_MONTH, 21)
                }
                val previewFormatter = SimpleDateFormat("dd MMM yyyy", localeIndonesia)

                Text(
                    text = "Periode: ${previewFormatter.format(previewStart.time)} - " +
                            previewFormatter.format(previewEnd.time),
                    color = GlassTextMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onMonthSelected(temporaryMonth, temporaryYear)
                },
                colors = ButtonDefaults.buttonColors(containerColor = GlassAccentCyan)
            ) {
                Text(
                    text = "TERAPKAN",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("BATAL", color = GlassAccentCyan)
            }
        }
    )
}

// =================================================================
// DIALOG EDIT LEMBUR
// =================================================================
@Composable
fun EditLemburDialog(
    itemToEdit: LemburData,
    onDismiss: () -> Unit,
    onSave: (String, Double, Boolean) -> Unit,
    isSaving: Boolean = false
) {
    val context = LocalContext.current
    var editTanggal by remember { mutableStateOf(itemToEdit.tanggal) }
    var editJam by remember { mutableStateOf(itemToEdit.jam.toString()) }
    var editHariBesar by remember { mutableStateOf(itemToEdit.jenis == "HariBesar") }

    val calendarPicker = remember { Calendar.getInstance() }

    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, day ->
                editTanggal = "$day/${month + 1}/$year"
            },
            calendarPicker.get(Calendar.YEAR),
            calendarPicker.get(Calendar.MONTH),
            calendarPicker.get(Calendar.DAY_OF_MONTH)
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A2E),
        title = {
            Text(
                "EDIT DATA LEMBURAN",
                fontFamily = OrbitronFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Field Tanggal
                Column {
                    Text("Tanggal", fontSize = 11.sp, color = GlassAccentCyan, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = editTanggal,
                        onValueChange = { },
                        readOnly = true,
                        enabled = true,
                        placeholder = { Text("Pilih Tanggal...", color = GlassTextMuted) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { datePickerDialog.show() },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GlassAccentCyan,
                            unfocusedBorderColor = GlassBorder,
                            focusedContainerColor = Color.White.copy(alpha = 0.03f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.03f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            disabledTextColor = Color.White,
                            disabledBorderColor = GlassBorder,
                            disabledContainerColor = Color.White.copy(alpha = 0.03f)
                        ),
                        trailingIcon = {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = "Pilih Tanggal",
                                tint = GlassAccentCyan
                            )
                        }
                    )
                }

                // Field Jam
                Column {
                    Text("Jumlah Jam", fontSize = 11.sp, color = GlassAccentCyan, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = editJam,
                        onValueChange = { editJam = it },
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

                // Toggle Hari Besar
                Surface(
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
                            checked = editHariBesar,
                            onCheckedChange = { editHariBesar = it },
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
        },
        confirmButton = {
            Button(
                onClick = {
                    if (editTanggal.isEmpty() || editJam.isEmpty()) {
                        Toast.makeText(context, "Lengkapi data!", Toast.LENGTH_SHORT).show()
                    } else {
                        onSave(
                            editTanggal,
                            editJam.toDoubleOrNull() ?: 0.0,
                            editHariBesar
                        )
                    }
                },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = GlassAccentCyan)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(18.dp))
                } else {
                    Text("SIMPAN", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("BATAL", color = GlassAccentCyan)
            }
        }
    )
}

// =================================================================
// PARSER TANGGAL DATA LEMBUR
// =================================================================
private fun parseLemburDate(rawDate: String): Date? {
    val value = rawDate.trim()
    if (value.isEmpty()) return null

    val formats = listOf(
        "dd/MM/yyyy",
        "d/M/yyyy",
        "yyyy-MM-dd",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "EEE MMM dd HH:mm:ss zzz yyyy"
    )

    for (pattern in formats) {
        try {
            return SimpleDateFormat(pattern, Locale.US).apply {
                isLenient = false
                if (pattern.contains("'Z'")) {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
            }.parse(value)
        } catch (_: Exception) {
            // Coba format berikutnya.
        }
    }

    return null
}

// =================================================================
// FUNGSI-FUNGSI API
// =================================================================

// FUNGSI PENGIRIM DATA POST KE GOOGLE SHEETS (TAMBAH DATA BARU)
suspend fun kirimLemburan(
    webAppUrl: String,
    tgl: String,
    nama: String,
    role: String,
    jam: Double,
    isHariBesar: Boolean
): Pair<Boolean, String> {
    return withContext(Dispatchers.IO) {
        try {
            val url = URL(webAppUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")

            val json = JSONObject().apply {
                put("action", "add")
                put("tanggal", tgl)
                put("nama", nama)
                put("role", role)
                put("jam", jam)
                put("jenis", if (isHariBesar) "HariBesar" else if (jam < 2.0) "Normal_Kecil" else "Normal_Besar")
            }

            conn.outputStream.use { it.write(json.toString().toByteArray()) }

            // BACA RESPONS SERVER
            val response = conn.inputStream.bufferedReader().use { it.readText() }

            // JIKA RESPONS ADALAH "Success", KEMBALIKAN TRUE
            if (response.contains("Success")) {
                Pair(true, "Success")
            } else {
                // JIKA RESPONS ADALAH "Error: ...", KEMBALIKAN FALSE + PESANNYA
                Pair(false, response)
            }
        } catch (e: Exception) {
            Pair(false, "Error: Koneksi Gagal")
        }
    }
}

// FUNGSI MENGHAPUS DATA LEMBURAN
suspend fun hapusLemburan(
    webAppUrl: String,
    tanggal: String,
    nama: String
): Pair<Boolean, String> {
    return withContext(Dispatchers.IO) {
        try {
            val url = URL(webAppUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 30000 // Tambah timeout jadi 30 detik
            conn.readTimeout = 30000

            val json = JSONObject().apply {
                put("action", "delete")
                put("tanggal", tanggal)
                put("nama", nama)
            }

            Log.d("HapusLemburan", "Mengirim request: " + json.toString())

            conn.outputStream.use { it.write(json.toString().toByteArray()) }

            // BACA RESPONS SERVER
            val responseCode = conn.responseCode
            Log.d("HapusLemburan", "Response Code: $responseCode")

            val response = if (responseCode == HttpURLConnection.HTTP_OK) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown Error"
            }

            Log.d("HapusLemburan", "Response: $response")

            if (response.contains("Success")) {
                Pair(true, "Success")
            } else {
                Pair(false, response)
            }
        } catch (e: Exception) {
            Log.e("HapusLemburan", "Error: ${e.message}", e)
            Pair(false, "Error: ${e.message}")
        }
    }
}

// FUNGSI UPDATE DATA LEMBURAN
suspend fun updateLemburan(
    webAppUrl: String,
    tanggalLama: String,
    tanggalBaru: String,
    nama: String,
    role: String,
    jam: Double,
    isHariBesar: Boolean
): Pair<Boolean, String> {
    return withContext(Dispatchers.IO) {
        try {
            val url = URL(webAppUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")

            val json = JSONObject().apply {
                put("action", "update")
                put("tanggalLama", tanggalLama)
                put("tanggalBaru", tanggalBaru)
                put("nama", nama)
                put("role", role)
                put("jam", jam)
                put("jenis", if (isHariBesar) "HariBesar" else if (jam < 2.0) "Normal_Kecil" else "Normal_Besar")
            }

            conn.outputStream.use { it.write(json.toString().toByteArray()) }

            val response = conn.inputStream.bufferedReader().use { it.readText() }

            if (response.contains("Success")) {
                Pair(true, "Success")
            } else {
                Pair(false, response)
            }
        } catch (e: Exception) {
            Pair(false, "Error: Koneksi Gagal")
        }
    }
}