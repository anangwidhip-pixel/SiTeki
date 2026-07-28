package com.example.sitekiver01.screens

import android.app.DatePickerDialog
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sitekiver01.OrbitronFontFamily
import com.example.sitekiver01.components.*
import com.example.sitekiver01.ui.theme.*
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
import java.util.concurrent.TimeUnit

const val STANG_SCRIPT_URL = "https://script.google.com/macros/s/AKfycbw2mqd4JU5ILu85ql2HrEmT4ksv0vR95bo9MqGWwRyXqOWUEdBWk3yYG9CTYXoTF9g/exec"

data class StangBelumKembaliRecord(
    val kode: String,
    val keluar: String,
    val namaGroup: String,
    val digunakan: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StangScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    // --- State Dropdown & Auto Kode Database ---
    var listMerk by remember { mutableStateOf<List<String>>(emptyList()) }
    var listGroup by remember { mutableStateOf<List<String>>(emptyList()) }
    var listLokasi by remember { mutableStateOf<List<String>>(emptyList()) }
    var nextKodeOtomatis by remember { mutableStateOf("Memuat...") }

    // --- State Penampung Data Rangkuman Logistik ---
    var sumTotalDikeluarkan by remember { mutableStateOf("0") }
    var sumTotalSudahKembali by remember { mutableStateOf("0") }
    var sumTotalBelumKembali by remember { mutableStateOf("0") }
    var sumDurasiTerlama by remember { mutableStateOf("0 Hari") }
    var sumDurasiTersingkat by remember { mutableStateOf("0 Hari") }
    var sumRataRataDurasi by remember { mutableStateOf("0 Hari") }
    var sumMerkTerlama by remember { mutableStateOf("-") }

    // --- State Daftar Tabel Stang Belum Kembali ---
    var listStangBelumKembali by remember { mutableStateOf<List<StangBelumKembaliRecord>>(emptyList()) }
    var isLoadingTabel by remember { mutableStateOf(false) }

    // --- State Pilihan Mode Tab ---
    var selectedTabMode by remember { mutableIntStateOf(0) }

    // --- State Form Bersama / Form Pinjam ---
    var tglKeluar by remember { mutableStateOf(sdf.format(Date())) }
    var groupKeluar by remember { mutableStateOf("Pilih Group") }
    var lokasiGuna by remember { mutableStateOf("Pilih Lokasi") }
    var merkStang by remember { mutableStateOf("Pilih Merk") }
    var keteranganText by remember { mutableStateOf("") }

    // --- State Form Kembali ---
    var inputKodeKembali by remember { mutableStateOf("") }
    var isSearchingKode by remember { mutableStateOf(false) }
    var tglKembali by remember { mutableStateOf(sdf.format(Date())) }
    var groupKembali by remember { mutableStateOf("Pilih Group") }
    var lokasiDari by remember { mutableStateOf("Pilih Lokasi") }

    var foundDataJson by remember { mutableStateOf<JSONObject?>(null) }
    var durasiPinjamHari by remember { mutableIntStateOf(0) }

    var showGroupDropdown by remember { mutableStateOf(false) }
    var showLokasiDropdown by remember { mutableStateOf(false) }
    var showMerkDropdown by remember { mutableStateOf(false) }
    var isLoadingGlobal by remember { mutableStateOf(false) }

    // --- Ambil Database Master, Next Nomor Kode, & Rangkuman Analitik ---
    val fetchDatabaseStang = suspend {
        isLoadingGlobal = true
        isLoadingTabel = true
        withContext(Dispatchers.IO) {
            try {
                val response = URL("$STANG_SCRIPT_URL?action=getDatabaseStang").readText()
                val json = JSONObject(response)

                listMerk = (json.optJSONArray("merk") ?: JSONArray()).let { arr -> List(arr.length()) { arr.getString(it) } }
                listGroup = (json.optJSONArray("group") ?: JSONArray()).let { arr -> List(arr.length()) { arr.getString(it) } }
                listLokasi = (json.optJSONArray("lokasi") ?: JSONArray()).let { arr -> List(arr.length()) { arr.getString(it) } }
                nextKodeOtomatis = json.optString("nextKode", "1")

                // Parsing Paket Objek Rangkuman dari Server
                json.optJSONObject("rangkuman")?.let { r ->
                    sumTotalDikeluarkan = r.optString("totalDikeluarkan", "0")
                    sumTotalSudahKembali = r.optString("totalSudahKembali", "0")
                    sumTotalBelumKembali = r.optString("totalBelumKembali", "0")
                    sumDurasiTerlama = r.optString("durasiTerlama", "0 Hari")
                    sumDurasiTersingkat = r.optString("durasiTersingkat", "0 Hari")
                    sumRataRataDurasi = r.optString("rataRataDurasi", "0 Hari")
                    sumMerkTerlama = r.optString("merkTerlama", "-")
                }

                // Parsing Data Tabel Belum Kembali
                val arrBelumKembali = json.optJSONArray("belumKembali") ?: JSONArray()
                listStangBelumKembali = List(arrBelumKembali.length()) {
                    val obj = arrBelumKembali.getJSONObject(it)
                    StangBelumKembaliRecord(obj.optString("kode"), obj.optString("keluar"), obj.optString("namaKeluar"), obj.optString("digunakan"))
                }

            } catch (e: Exception) {
                Log.e("StangScreen", "Fetch DB Error: ${e.message}")
                nextKodeOtomatis = "1"
            } finally {
                isLoadingGlobal = false
                isLoadingTabel = false
            }
        }
    }

    LaunchedEffect(Unit) { fetchDatabaseStang() }

    LaunchedEffect(tglKeluar, tglKembali, foundDataJson) {
        if (foundDataJson != null) {
            try {
                val tglAwalStr = foundDataJson?.optString("keluar") ?: tglKeluar
                val date1 = sdf.parse(tglAwalStr)
                val date2 = sdf.parse(tglKembali)
                if (date1 != null && date2 != null) {
                    val diff = date2.time - date1.time
                    val days = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS).toInt()
                    durasiPinjamHari = if (days < 0) 0 else days
                }
            } catch (e: Exception) { durasiPinjamHari = 0 }
        }
    }

    val aksiPencarianDataKode = { kodeTarget: String ->
        if (kodeTarget.isEmpty()) {
            Toast.makeText(context, "Isi Kode Terlebih Dahulu!", Toast.LENGTH_SHORT).show()
        } else {
            scope.launch {
                isSearchingKode = true
                foundDataJson = mencariDataStangByKode(kodeTarget)
                if (foundDataJson != null) {
                    Toast.makeText(context, "Data Peminjaman Ditemukan!", Toast.LENGTH_SHORT).show()
                    keteranganText = foundDataJson!!.optString("keterangan", "")
                } else {
                    Toast.makeText(context, "Kode Tidak Valid!", Toast.LENGTH_SHORT).show()
                    keteranganText = ""
                }
                isSearchingKode = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        SciFiBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Surface(color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
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
                            "Logistik Stang Las",
                            color = MaterialTheme.colorScheme.onBackground,
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
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(4.dp))

                // ================= RANGKUMAN STATISTIK TERMINAL GLOBAL (PALING ATAS) =================
                if (foundDataJson == null) {
                    ModernSectionHeader("RANGKUMAN STATISTIK TERMINAL", Icons.Default.Analytics)
                    GlassCard {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Menggunakan RapiRowStatistik pembagi bobot 65% dan 35% agar teks presisi
                            RapiRowStatistik("Jumlah Semua Stang Yang Telah Dikeluarkan", sumTotalDikeluarkan, Color.White)
                            RapiRowStatistik("Jumlah Stang Yang Sudah Kembali", sumTotalSudahKembali, SciFiStatusM)
                            RapiRowStatistik("Jumlah Stang Yang Belum Kembali", sumTotalBelumKembali, SciFiSaturday)
                            HorizontalDivider(color = SciFiBorderLight.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 4.dp))
                            RapiRowStatistik("Durasi Pemakaian Terlama", sumDurasiTerlama, Color.White)
                            RapiRowStatistik("Durasi Pemakaian Tersingkat", sumDurasiTersingkat, Color.White)
                            RapiRowStatistik("Rata - Rata Durasi Pemakaian", sumRataRataDurasi, SciFiCyan)
                            RapiRowStatistik("Merk Dengan Rata - Rata Pemakaian Terlama", sumMerkTerlama, SciFiSaturday)
                        }
                    }
                }

                // --- TAB SELECTOR MODE ---
                Surface(
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(23.dp),
                    color = Color.White.copy(alpha = 0.03f),
                    border = BorderStroke(1.dp, SciFiBorderLight)
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxHeight()
                                .background(if (selectedTabMode == 0) SciFiCyan else Color.Transparent, RoundedCornerShape(23.dp))
                                .clickable {
                                    selectedTabMode = 0
                                    keteranganText = ""
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("PINJAM STANG", color = if (selectedTabMode == 0) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = OrbitronFontFamily)
                        }
                        Box(
                            modifier = Modifier.weight(1f).fillMaxHeight()
                                .background(if (selectedTabMode == 1) SciFiCyan else Color.Transparent, RoundedCornerShape(23.dp))
                                .clickable {
                                    selectedTabMode = 1
                                    keteranganText = ""
                                    foundDataJson = null
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("KEMBALI STANG", color = if (selectedTabMode == 1) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = OrbitronFontFamily)
                        }
                    }
                }

                // ================= TAB 0: MODE PINJAM STANG =================
                if (selectedTabMode == 0) {
                    ModernSectionHeader("REGISTRASI PEMINJAMAN BARU", Icons.Default.Output)

                    GlassCard {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Column {
                                Text("KODE TRANSAKSI OTOMATIS (READ-ONLY)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SciFiSaturday, fontFamily = OrbitronFontFamily)

                                BasicTextField(
                                    value = nextKodeOtomatis,
                                    onValueChange = {},
                                    enabled = false,
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        color = SciFiSaturday, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = OrbitronFontFamily
                                    ),
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(44.dp),
                                    singleLine = true,
                                    decorationBox = { innerTextField ->
                                        TextFieldDefaults.DecorationBox(
                                            value = nextKodeOtomatis, innerTextField = innerTextField, enabled = false, singleLine = true,
                                            visualTransformation = VisualTransformation.None,
                                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                            leadingIcon = { Icon(Icons.Default.Lock, null, tint = SciFiSaturday, modifier = Modifier.size(16.dp)) },
                                            container = { Surface(shape = RoundedCornerShape(12.dp), color = Color.White.copy(alpha = 0.02f), border = BorderStroke(1.dp, SciFiBorderLight)) {} },
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                        )
                                    }
                                )
                            }

                            Column {
                                Text("TANGGAL KELUAR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SciFiCyan, fontFamily = OrbitronFontFamily)
                                OutlinedButton(
                                    onClick = {
                                        val c = Calendar.getInstance()
                                        DatePickerDialog(context, { _, y, m, d ->
                                            tglKeluar = String.format("%02d/%02d/%04d", d, m + 1, y)
                                        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(44.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, SciFiBorderLight),
                                    colors = ButtonDefaults.outlinedButtonColors(disabledContentColor = Color.White)
                                ) {
                                    Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(16.dp), tint = SciFiCyan)
                                    Spacer(Modifier.width(8.dp))
                                    Text(tglKeluar, fontSize = 13.sp, color = Color.White)
                                    Spacer(Modifier.weight(1f))
                                }
                            }

                            Column {
                                Text("NAMA GROUP PEMINJAM", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SciFiCyan, fontFamily = OrbitronFontFamily)
                                Box(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                                    OutlinedButton(
                                        onClick = { showGroupDropdown = true },
                                        modifier = Modifier.fillMaxWidth().height(44.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, SciFiBorderLight),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                    ) {
                                        Text(groupKeluar, fontSize = 13.sp)
                                        Spacer(Modifier.weight(1f))
                                        Icon(Icons.Default.ArrowDropDown, null, tint = SciFiTextMuted)
                                    }
                                    DropdownMenu(
                                        expanded = showGroupDropdown,
                                        onDismissRequest = { showGroupDropdown = false },
                                        modifier = Modifier.background(Color(0xFF0F172A)).border(1.dp, SciFiBorderLight)
                                    ) {
                                        listGroup.forEach { g ->
                                            DropdownMenuItem(
                                                text = { Text(g, color = Color.White) },
                                                onClick = { groupKeluar = g; showGroupDropdown = false }
                                            )
                                        }
                                    }
                                }
                            }

                            Column {
                                Text("LOKASI PENEMPATAN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SciFiCyan, fontFamily = OrbitronFontFamily)
                                Box(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                                    OutlinedButton(
                                        onClick = { showLokasiDropdown = true },
                                        modifier = Modifier.fillMaxWidth().height(44.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, SciFiBorderLight),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                    ) {
                                        Text(lokasiGuna, fontSize = 13.sp)
                                        Spacer(Modifier.weight(1f))
                                        Icon(Icons.Default.ArrowDropDown, null, tint = SciFiTextMuted)
                                    }
                                    DropdownMenu(
                                        expanded = showLokasiDropdown,
                                        onDismissRequest = { showLokasiDropdown = false },
                                        modifier = Modifier.background(Color(0xFF0F172A)).border(1.dp, SciFiBorderLight)
                                    ) {
                                        // MENYEMBUHKAN BARIS 248: Menghapus baris 'listLocations ->' typo bawaan log lama
                                        listLokasi.forEach { l ->
                                            DropdownMenuItem(
                                                text = { Text(l, color = Color.White) },
                                                onClick = { lokasiGuna = l; showLokasiDropdown = false }
                                            )
                                        }
                                    }
                                }
                            }

                            Column {
                                Text("MERK STANG LAS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SciFiCyan, fontFamily = OrbitronFontFamily)
                                Box(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                                    OutlinedButton(
                                        onClick = { showMerkDropdown = true },
                                        modifier = Modifier.fillMaxWidth().height(44.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, SciFiBorderLight),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                    ) {
                                        Text(merkStang, fontSize = 13.sp)
                                        Spacer(Modifier.weight(1f))
                                        Icon(Icons.Default.ArrowDropDown, null, tint = SciFiTextMuted)
                                    }
                                    DropdownMenu(
                                        expanded = showMerkDropdown,
                                        onDismissRequest = { showMerkDropdown = false },
                                        modifier = Modifier.background(Color(0xFF0F172A)).border(1.dp, SciFiBorderLight)
                                    ) {
                                        listMerk.forEach { m ->
                                            DropdownMenuItem(
                                                text = { Text(m, color = Color.White) },
                                                onClick = { merkStang = m; showMerkDropdown = false }
                                            )
                                        }
                                    }
                                }
                            }

                            Column {
                                Text("KETERANGAN TAMBAHAN PINJAMAN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SciFiCyan, fontFamily = OrbitronFontFamily)
                                Spacer(modifier = Modifier.height(4.dp))
                                ModernTextField(
                                    value = keteranganText,
                                    onValueChange = { keteranganText = it },
                                    placeholder = "Isi catatan kondisi stang las saat dipinjam...",
                                    icon = Icons.Default.Edit
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (groupKeluar == "Pilih Group" || lokasiGuna == "Pilih Lokasi" || merkStang == "Pilih Merk") {
                                Toast.makeText(context, "Lengkapi Parameter Validasi!", Toast.LENGTH_SHORT).show()
                            } else {
                                scope.launch {
                                    isLoadingGlobal = true
                                    val success = submitTransaksiStang(
                                        action = "pinjam", kode = "", keluar = tglKeluar, namaKeluar = groupKeluar,
                                        digunakan = lokasiGuna, kembali = "", namaKembali = "", dari = "",
                                        merk = merkStang, durasi = "0", keterangan = keteranganText
                                    )
                                    if (success) {
                                        Toast.makeText(context, "Sukses Terdaftar!", Toast.LENGTH_LONG).show()
                                        groupKeluar = "Pilih Group"; lokasiGuna = "Pilih Lokasi"; merkStang = "Pilih Merk"; keteranganText = ""
                                        fetchDatabaseStang()
                                    } else {
                                        Toast.makeText(context, "Koneksi Terminal Database Gagal!", Toast.LENGTH_SHORT).show()
                                    }
                                    isLoadingGlobal = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SciFiCyan)
                    ) {
                        if (isLoadingGlobal) CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(22.dp))
                        else Text("KIRIM DATA PINJAMAN", color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = OrbitronFontFamily, fontSize = 13.sp)
                    }
                }

                // ================= TAB 1: MODE KEMBALI STANG =================
                if (selectedTabMode == 1) {
                    ModernSectionHeader("INPUT KODE PEMINJAMAN KEMBALI", Icons.Default.Input)

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = SciFiGlass,
                        border = BorderStroke(1.dp, SciFiBorderLight)
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = inputKodeKembali,
                                    onValueChange = { inputKodeKembali = it },
                                    placeholder = { Text("Masukkan Kode Unik Pinjam...", fontSize = 12.sp, color = SciFiTextMuted) },
                                    modifier = Modifier.fillMaxWidth().height(46.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = SciFiCyan, unfocusedBorderColor = SciFiBorderLight,
                                        focusedTextColor = Color.White, unfocusedTextColor = Color.White
                                    ),
                                    singleLine = true
                                )
                            }
                            Button(
                                onClick = { aksiPencarianDataKode(inputKodeKembali) },
                                modifier = Modifier.height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SciFiCyan)
                            ) {
                                if (isSearchingKode) CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(18.dp))
                                else Icon(Icons.Default.Search, null, tint = Color.Black)
                            }
                        }
                    }

                    // --- INTEGRASI STRUKTUR TABEL LOG ANTRIAN ---
                    if (foundDataJson == null) {
                        Spacer(Modifier.height(4.dp))
                        ModernSectionHeader("DAFTAR ANTRIAN LOG BELUM KEMBALI", Icons.Default.TableChart)

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 200.dp, max = 500.dp)
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = SciFiGlass,
                            border = BorderStroke(1.dp, SciFiBorderLight)
                        ) {
                            if (isLoadingTabel) {
                                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = SciFiCyan) }
                            } else if (listStangBelumKembali.isEmpty()) {
                                Box(Modifier.fillMaxSize().padding(16.dp), Alignment.Center) {
                                    Text("Semua Stang Las Sudah Dikembalikan (Clear)!", color = SciFiStatusM, fontSize = 12.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Column(Modifier.fillMaxSize()) {
                                    Row(
                                        Modifier.fillMaxWidth().background(SciFiCyan.copy(alpha = 0.08f)).padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("KD", Modifier.width(36.dp), color = SciFiCyan, fontWeight = FontWeight.Black, fontSize = 11.sp, fontFamily = OrbitronFontFamily)
                                        Text("KELUAR", Modifier.width(75.dp), color = SciFiCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Text("NAMA", Modifier.weight(1f), color = SciFiCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Text("LOKASI", Modifier.weight(1f), color = SciFiCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Text("AKSI", Modifier.width(80.dp), color = SciFiCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center)
                                    }

                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(bottom = 8.dp)
                                    ) {
                                        itemsIndexed(listStangBelumKembali) { idx, item ->
                                            Row(
                                                Modifier
                                                    .fillMaxWidth()
                                                    .background(if (idx % 2 == 0) Color.White.copy(alpha = 0.01f) else Color.Transparent)
                                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(item.kode, Modifier.width(36.dp), color = SciFiSaturday, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = OrbitronFontFamily)
                                                Text(item.keluar, Modifier.width(75.dp), color = Color.White, fontSize = 12.sp)
                                                Text(item.namaGroup, Modifier.weight(1f), color = Color.White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text(item.digunakan, Modifier.weight(1f), color = Color.White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)

                                                Button(
                                                    onClick = {
                                                        inputKodeKembali = item.kode
                                                        aksiPencarianDataKode(item.kode)
                                                    },
                                                    modifier = Modifier.width(80.dp).height(30.dp),
                                                    contentPadding = PaddingValues(0.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = SciFiCyan),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text("KEMBALI", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 10.sp, fontFamily = OrbitronFontFamily)
                                                }
                                            }
                                            HorizontalDivider(color = SciFiBorderLight.copy(alpha = 0.15f), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 10.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Panel Tampil Form Pengembalian saat Kode Dipilih
                    AnimatedVisibility(
                        visible = foundDataJson != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        foundDataJson?.let { data ->
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                ModernSectionHeader("INFORMASI AWAL STANG PINJAMAN", Icons.Default.Info)
                                GlassCard {
                                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        CalculationRow("GROUP PEMINJAM", data.optString("namaKeluar"), Color.White)
                                        CalculationRow("TANGGAL KELUAR", data.optString("keluar"), Color.White)
                                        CalculationRow("LOKASI AWAL", data.optString("digunakan"), Color.White)
                                        CalculationRow("MERK STANG", data.optString("merk"), SciFiCyan)
                                        CalculationRow("KET. AWAL", data.optString("keterangan").ifEmpty { "-" }, SciFiSaturday)
                                    }
                                }

                                ModernSectionHeader("FORM PENGEMBALIAN DATA", Icons.Default.AssignmentReturn)
                                GlassCard {
                                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                        Column {
                                            Text("TANGGAL KEMBALI", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SciFiCyan, fontFamily = OrbitronFontFamily)
                                            OutlinedButton(
                                                onClick = {
                                                    val c = Calendar.getInstance()
                                                    DatePickerDialog(context, { _, y, m, d ->
                                                        tglKembali = String.format("%02d/%02d/%04d", d, m + 1, y)
                                                    }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                                                },
                                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(44.dp),
                                                shape = RoundedCornerShape(12.dp),
                                                border = BorderStroke(1.dp, SciFiBorderLight),
                                                colors = ButtonDefaults.outlinedButtonColors(disabledContentColor = Color.White)
                                            ) {
                                                Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(16.dp), tint = SciFiCyan)
                                                Spacer(Modifier.width(8.dp))
                                                Text(tglKembali, fontSize = 13.sp, color = Color.White)
                                                Spacer(Modifier.weight(1f))
                                            }
                                        }

                                        Column {
                                            Text("GROUP YANG MENGEMBALIKAN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SciFiCyan, fontFamily = OrbitronFontFamily)
                                            Box(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                                                var showGroupDropdownKembali by remember { mutableStateOf(false) }
                                                OutlinedButton(
                                                    onClick = { showGroupDropdownKembali = true },
                                                    modifier = Modifier.fillMaxWidth().height(44.dp),
                                                    shape = RoundedCornerShape(12.dp),
                                                    border = BorderStroke(1.dp, SciFiBorderLight),
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                                ) {
                                                    Text(groupKembali, fontSize = 13.sp)
                                                    Spacer(Modifier.weight(1f))
                                                    Icon(Icons.Default.ArrowDropDown, null, tint = SciFiTextMuted)
                                                }
                                                DropdownMenu(
                                                    expanded = showGroupDropdownKembali,
                                                    onDismissRequest = { showGroupDropdownKembali = false },
                                                    modifier = Modifier.background(Color(0xFF0F172A)).border(1.dp, SciFiBorderLight)
                                                ) {
                                                    listGroup.forEach { g ->
                                                        DropdownMenuItem(
                                                            text = { Text(g, color = Color.White) },
                                                            onClick = { groupKembali = g; showGroupDropdownKembali = false }
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Column {
                                            Text("LOKASI ASAL (DARI)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SciFiCyan, fontFamily = OrbitronFontFamily)
                                            Box(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                                                var showLokasiDropdownKembali by remember { mutableStateOf(false) }
                                                OutlinedButton(
                                                    onClick = { showLokasiDropdownKembali = true },
                                                    modifier = Modifier.fillMaxWidth().height(44.dp),
                                                    shape = RoundedCornerShape(12.dp),
                                                    border = BorderStroke(1.dp, SciFiBorderLight),
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                                ) {
                                                    Text(lokasiDari, fontSize = 13.sp)
                                                    Spacer(Modifier.weight(1f))
                                                    Icon(Icons.Default.ArrowDropDown, null, tint = SciFiTextMuted)
                                                }
                                                DropdownMenu(
                                                    expanded = showLokasiDropdownKembali,
                                                    onDismissRequest = { showLokasiDropdownKembali = false },
                                                    modifier = Modifier.background(Color(0xFF0F172A)).border(1.dp, SciFiBorderLight)
                                                ) {
                                                    listLokasi.forEach { l ->
                                                        DropdownMenuItem(
                                                            text = { Text(l, color = Color.White) },
                                                            onClick = { lokasiDari = l; showLokasiDropdownKembali = false }
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        CalculationRow("DURASI PEMINJAMAN", "$durasiPinjamHari Hari", SciFiSaturday)

                                        Column {
                                            Text("KETERANGAN TAMBAHAN / PERUBAHAN STATUS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SciFiCyan, fontFamily = OrbitronFontFamily)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            ModernTextField(
                                                value = keteranganText,
                                                onValueChange = { keteranganText = it },
                                                placeholder = "Ubah catatan jika stang mengalami kerusakan/perubahan kondisi...",
                                                icon = Icons.Default.Edit
                                            )
                                        }
                                    }
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedButton(
                                        onClick = { foundDataJson = null; inputKodeKembali = "" },
                                        modifier = Modifier.weight(0.4f).height(48.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, SciFiBorderLight)
                                    ) { Text("BATAL", color = Color.White, fontFamily = OrbitronFontFamily, fontWeight = FontWeight.Bold) }

                                    Button(
                                        onClick = {
                                            if (groupKembali == "Pilih Group" || lokasiDari == "Pilih Lokasi") {
                                                Toast.makeText(context, "Lengkapi Parameter Pengembalian!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                scope.launch {
                                                    isLoadingGlobal = true
                                                    val success = submitTransaksiStang(
                                                        action = "kembali", kode = inputKodeKembali, keluar = data.optString("keluar"),
                                                        namaKeluar = data.optString("namaKeluar"), digunakan = data.optString("digunakan"),
                                                        kembali = tglKembali, namaKembali = groupKembali, dari = lokasiDari,
                                                        merk = data.optString("merk"), durasi = durasiPinjamHari.toString(),
                                                        keterangan = keteranganText
                                                    )
                                                    if (success) {
                                                        Toast.makeText(context, "Barang Berhasil Dikembalikan!", Toast.LENGTH_SHORT).show()
                                                        inputKodeKembali = ""; foundDataJson = null; keteranganText = ""
                                                        groupKembali = "Pilih Group"; lokasiDari = "Pilih Lokasi"
                                                        fetchDatabaseStang()
                                                    } else {
                                                        Toast.makeText(context, "Gagal Mengupdate Data!", Toast.LENGTH_SHORT).show()
                                                    }
                                                    isLoadingGlobal = false
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(0.6f).height(48.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = SciFiStatusM)
                                    ) {
                                        if (isLoadingGlobal) CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(22.dp))
                                        else Text("PROSES PENGEMBALIAN", color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = OrbitronFontFamily, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// KOMPONEN DATA STATISTIK DENGAN BOBOT AMAN (ANTI TEKS PECAH / TURUN BAWAH)
@Composable
fun RapiRowStatistik(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp,
            modifier = Modifier.weight(0.65f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = OrbitronFontFamily,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.35f)
        )
    }
}

private suspend fun mencariDataStangByKode(kode: String): JSONObject? {
    return withContext(Dispatchers.IO) {
        try {
            val urlString = "$STANG_SCRIPT_URL?action=getSingleStok&kode=${URLEncoder.encode(kode, "UTF-8")}"
            val response = URL(urlString).readText()
            val json = JSONObject(response)
            if (json.optString("status") == "success") {
                json.getJSONObject("data")
            } else null
        } catch (e: Exception) { null }
    }
}

private suspend fun submitTransaksiStang(
    action: String, kode: String, keluar: String, namaKeluar: String, digunakan: String,
    kembali: String, namaKembali: String, dari: String, merk: String, durasi: String, keterangan: String
): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val urlString = "$STANG_SCRIPT_URL?action=$action" +
                    "&kode=${URLEncoder.encode(kode, "UTF-8")}" +
                    "&keluar=${URLEncoder.encode(keluar, "UTF-8")}" +
                    "&namaKeluar=${URLEncoder.encode(namaKeluar, "UTF-8")}" +
                    "&digunakan=${URLEncoder.encode(digunakan, "UTF-8")}" +
                    "&kembali=${URLEncoder.encode(kembali, "UTF-8")}" +
                    "&namaKembali=${URLEncoder.encode(namaKembali, "UTF-8")}" +
                    "&dari=${URLEncoder.encode(dari, "UTF-8")}" +
                    "&merk=${URLEncoder.encode(merk, "UTF-8")}" +
                    "&durasi=$durasi" +
                    "&keterangan=${URLEncoder.encode(keterangan, "UTF-8")}"

            val conn = URL(urlString).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.instanceFollowRedirects = true
            val response = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            json.optString("status") == "success"
        } catch (e: Exception) { false }
    }
}
