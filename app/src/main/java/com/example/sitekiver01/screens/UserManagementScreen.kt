package com.example.sitekiver01.screens

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sitekiver01.OrbitronFontFamily
import com.example.sitekiver01.UserSession
import com.example.sitekiver01.components.*
import com.example.sitekiver01.ui.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import androidx.activity.compose.BackHandler

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun UserManagementScreen(
    onBack: () -> Unit, // Callback untuk kembali ke 'Lainnya'
    onSuccess: () -> Unit = {}
) {
    // Ini akan menangkap tombol back fisik maupun tombol back di UI
    BackHandler(enabled = true) {
        onBack() // Memanggil fungsi navigasi ke Screen.Lainnya
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isSubmitting by remember { mutableStateOf(false) }
    var isLoadingData by remember { mutableStateOf(false) }

    // ================== CONTROL LAYER (NAVIGASI INTERNAL) ==================
    // Jika Admin -> Tampilkan List Dulu (true). Jika selain Admin -> Langsung Form (false)
    val isAdmin = UserSession.role.equals("Admin", ignoreCase = true)
    var showList by remember { mutableStateOf(isAdmin) }

    // State Penampung Daftar Semua Karyawan (Khusus Admin)
    var listTeknisiMurni by remember { mutableStateOf(listOf<JSONObject>()) }

    // ================== STATE DATABASE TEKNISI A-Z (26 KOLOM) ==================
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var nama by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("Lainnya") }
    var fungsi by remember { mutableStateOf("") }
    var nik by remember { mutableStateOf("") }
    var jabatan by remember { mutableStateOf("") }
    var bagian by remember { mutableStateOf("") }
    var regu by remember { mutableStateOf("") }
    var tglMasuk by remember { mutableStateOf("") }
    var kontrakTerakhir by remember { mutableStateOf("") }
    var pendidikan by remember { mutableStateOf("") }
    var jurusan by remember { mutableStateOf("") }
    var statusPegawai by remember { mutableStateOf("Kontrak") }
    var statusGaji by remember { mutableStateOf("Harian") }
    var tunjangan by remember { mutableStateOf("") }
    var tLahir by remember { mutableStateOf("") }
    var tglLahir by remember { mutableStateOf("") }
    var alamat by remember { mutableStateOf("") }
    var noTelp by remember { mutableStateOf("") }
    var noTelpDarurat by remember { mutableStateOf("") }
    var gajiPokok by remember { mutableStateOf("") }
    var gajiHarian by remember { mutableStateOf("") }
    var keterangan by remember { mutableStateOf("") }

    // Fungsi Pengisian Otomatis State Form dari Objek JSON
    fun isiFormDariJson(obj: JSONObject) {
        username = obj.optString("username", "")
        password = obj.optString("password", "")
        nama = obj.optString("nama", "")
        role = obj.optString("role", "Lainnya")
        fungsi = obj.optString("fungsi", "")
        nik = obj.optString("nik", "")
        jabatan = obj.optString("jabatan", "")
        bagian = obj.optString("bagian", "")
        regu = obj.optString("regu", "")
        tglMasuk = obj.optString("tglMasuk", "")
        kontrakTerakhir = obj.optString("kontrakTerakhir", "")
        pendidikan = obj.optString("pendidikan", "")
        jurusan = obj.optString("jurusan", "")
        statusPegawai = obj.optString("statusPegawai", "Kontrak")
        statusGaji = obj.optString("statusGaji", "Harian")
        tunjangan = obj.optString("tunjangan", "")
        tLahir = obj.optString("tLahir", "")
        tglLahir = obj.optString("tglLahir", "")
        alamat = obj.optString("alamat", "")
        noTelp = obj.optString("noTelp", "")
        noTelpDarurat = obj.optString("noTelpDarurat", "")
        gajiPokok = obj.optString("gajiPokok", "0")
        gajiHarian = obj.optString("gajiHarian", "0")
        keterangan = obj.optString("keterangan", "")
    }

    // ================== SINKRONISASI AWAL (LOAD DATA FROM SPREADSHEET (2)) ==================
    LaunchedEffect(Unit) {
        isLoadingData = true
        scope.launch(Dispatchers.IO) {
            try {
                // Panggil doGet dari Spreadsheet (2) Data User
                val url = URL("https://script.google.com/macros/s/AKfycbzOrWNyuZsJ6K7L5icJU_BNZhznRbYYiK4-3ssqq0_69CkXhF6OVwOjvlPp-Qtvw4Q/exec?action=getAllUser")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"

                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(responseText)
                val tempList = mutableListOf<JSONObject>()

                for (i in 0 until jsonArray.length()) {
                    tempList.add(jsonArray.getJSONObject(i))
                }

                withContext(Dispatchers.Main) {
                    listTeknisiMurni = tempList
                    isLoadingData = false

                    // JIKA BUKAN ADMIN: Langsung cari datanya sendiri dan kunci layar ke Form Edit
                    if (!isAdmin) {
                        val dataSendiri = tempList.find {
                            it.optString("username").trim().lowercase() == UserSession.username.trim().lowercase()
                        }
                        if (dataSendiri != null) {
                            isiFormDariJson(dataSendiri)
                        } else {
                            // Jika data profil belum ada di spreadsheet, isi default data session login
                            username = UserSession.username
                            nama = UserSession.namaFull
                            role = UserSession.role
                        }
                        showList = false
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoadingData = false
                    Toast.makeText(context, "Gagal sinkronisasi data master: ${e.message}", Toast.LENGTH_LONG).show()
                    if (!isAdmin) onBack() // Jika user biasa gagal ambil data, paksa mundur
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(GlassBase)) {
        SciFiBackground()

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
                            onClick = {
                                if (isAdmin && !showList) {
                                    showList = true // Admin kembali ke daftar nama dulu
                                } else {
                                    onBack() // Keluar ke menu Utama/Lainnya
                                }
                            },
                            modifier = Modifier.background(Color.White.copy(alpha = 0.08f), CircleShape)
                        ) { Icon(Icons.Default.ArrowBackIosNew, "Back", tint = Color.White) }
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = if (showList) "DAFTAR SISTEM TEKNISI" else "EDIT PROFILE SIBER",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            fontFamily = OrbitronFontFamily,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        ) { padding ->
            if (isLoadingData) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SciFiCyan)
                }
            } else if (showList && isAdmin) {
                // =========================================================================
                // KONDISI A: TAMPILAN SELEKTOR DAFTAR NAMA (HANYA BISA DIAKSES ADMIN)
                // =========================================================================
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(10.dp)) }
                    items(listTeknisiMurni) { teknisi ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable {
                                isiFormDariJson(teknisi)
                                showList = false // Masuk ke mode Edit Form
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = SciFiGlass,
                            border = BorderStroke(1.dp, SciFiBorderLight)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(45.dp).background(SciFiCyan.copy(alpha = 0.1f), CircleShape).border(1.dp, SciFiCyan, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) { Icon(Icons.Default.Person, contentDescription = null, tint = SciFiCyan) }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = teknisi.optString("nama").uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontFamily = OrbitronFontFamily, fontSize = 14.sp)
                                    Text(text = "NIK: ${teknisi.optString("nik", "-")} | Bagian: ${teknisi.optString("bagian", "-")}", color = SciFiTextMuted, fontSize = 12.sp)
                                }
                                Icon(Icons.Default.ArrowForwardIos, null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            } else {
                // =========================================================================
                // KONDISI B: TAMPILAN CORE FORM EDIT 26 KOLOM (UNTUK SEMUA USER)
                // =========================================================================
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Spacer(Modifier.height(12.dp))

                    // ================== SEKTOR 1: KREDENSIAL AKUN ==================
                    ModernSectionHeader("OTENTIKASI TERMINAL", Icons.Default.Lock)
                    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = SciFiGlass, border = BorderStroke(1.dp, Brush.verticalGradient(listOf(SciFiBorderLight, Color.Transparent)))) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            FormLabel("USERNAME")
                            ModernTextField(value = username, onValueChange = { if (isAdmin) username = it }, placeholder = "Masukkan Username...", icon = Icons.Default.Person)
                            Spacer(modifier = Modifier.height(14.dp))
                            FormLabel("PASSWORD TERMINAL")
                            ModernTextField(value = password, onValueChange = { password = it }, placeholder = "Masukkan Password baru...", icon = Icons.Default.VpnKey)
                            Spacer(modifier = Modifier.height(14.dp))
                            FormLabel("ROLE SISTEM")
                            ModernDropdownField(selected = role, options = listOf("Admin", "Teknik", "Operator", "Lainnya"), label = "Pilih Role...", onSelected = { if (isAdmin) role = it })
                        }
                    }

                    // ================== SEKTOR 2: PROFIL DASAR PEGAWAI ==================
                    ModernSectionHeader("PROFIL DASAR TEKNISI", Icons.Default.Badge)
                    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = SciFiGlass, border = BorderStroke(1.dp, Brush.verticalGradient(listOf(SciFiBorderLight, Color.Transparent)))) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            FormLabel("NAMA LENGKAP")
                            ModernTextField(value = nama, onValueChange = { if (isAdmin) nama = it }, placeholder = "Nama Lengkap...", icon = Icons.Default.Edit)
                            Spacer(modifier = Modifier.height(14.dp))
                            FormLabel("NIK (NOMOR INDUK KARYAWAN)")
                            ModernTextField(value = nik, onValueChange = { nik = it }, placeholder = "Masukkan NIK...", icon = Icons.Default.Fingerprint)
                            Spacer(modifier = Modifier.height(14.dp))
                            FormLabel("JABATAN")
                            ModernDropdownField(selected = jabatan, options = listOf("Kabag", "Kasubag", "Karu", "Anggota"), label = "Pilih Jabatan...", onSelected = { jabatan = it })
                            Spacer(modifier = Modifier.height(14.dp))
                            FormLabel("BAGIAN")
                            ModernDropdownField(selected = bagian, options = listOf("Teknik", "Mekanik Shift A", "Mekanik Shift B", "Umum", "Bengkel", "Konstruksi", "Crane", "Admin"), label = "Pilih Bagian...", onSelected = { bagian = it })
                            Spacer(modifier = Modifier.height(14.dp))
                            FormLabel("REGU KINERJA")
                            ModernDropdownField(selected = regu, options = listOf("Teknik", "Mekanik", "Listrik", "Utilitas", "AC", "Bengkel", "Konstruksi", "Mek. Crane", "Admin"), label = "Pilih Regu...", onSelected = { regu = it })
                            Spacer(modifier = Modifier.height(14.dp))
                            FormLabel("FUNGSI TUGAS")
                            ModernTextField(value = fungsi, onValueChange = { fungsi = it }, placeholder = "Fungsi spesifik tugas...", icon = Icons.Default.Assignment)
                        }
                    }

                    // ================== SEKTOR 3: RIWAYAT & KONTRAK ==================
                    ModernSectionHeader("STRUKTUR KONTRAK & PENDIDIKAN", Icons.Default.CalendarMonth)
                    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = SciFiGlass, border = BorderStroke(1.dp, Brush.verticalGradient(listOf(SciFiBorderLight, Color.Transparent)))) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            FormLabel("TANGGAL MASUK KERJA (DD/MM/YYYY)")
                            ModernTextField(value = tglMasuk, onValueChange = { tglMasuk = it }, placeholder = "Contoh: 15/04/2018", icon = Icons.Default.DateRange)
                            Spacer(modifier = Modifier.height(14.dp))
                            FormLabel("KONTRAK TERAKHIR (DD/MM/YYYY)")
                            ModernTextField(value = kontrakTerakhir, onValueChange = { kontrakTerakhir = it }, placeholder = "Isi tanggal kontrak...", icon = Icons.Default.EventAvailable)
                            Spacer(modifier = Modifier.height(14.dp))
                            FormLabel("PENDIDIKAN TERAKHIR")
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("SMP", "SMA", "SMK", "D1", "D3", "S1").forEach { opt -> ChoiceChip(text = opt, selected = pendidikan == opt, onClick = { pendidikan = opt }) }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            FormLabel("JURUSAN")
                            ModernTextField(value = jurusan, onValueChange = { jurusan = it }, placeholder = "Contoh: Teknik Elektro", icon = Icons.Default.School)
                        }
                    }

                    // ================== SEKTOR 4: STATUS & PENGUPAHAN (PROTEKSI SENSITIF) ==================
                    ModernSectionHeader("SISTEM PENGUPAHAN & TUNJANGAN", Icons.Default.Payments)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = SciFiGlass,
                        border = BorderStroke(1.dp, Brush.verticalGradient(listOf(SciFiBorderLight, Color.Transparent)))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            FormLabel("STATUS PEGAWAI")
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Tetap", "Kontrak").forEach { opt ->
                                    ChoiceChip(text = opt, selected = statusPegawai == opt, onClick = { if (isAdmin) statusPegawai = opt })
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            FormLabel("STATUS GAJI")
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Bulanan", "Harian").forEach { opt ->
                                    ChoiceChip(text = opt, selected = statusGaji == opt, onClick = { if (isAdmin) statusGaji = opt })
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            FormLabel("TUNJANGAN KEAHLIAN / JABATAN")
                            ModernDropdownField(
                                selected = tunjangan,
                                options = listOf("Kabag", "Kasubag", "Karu", "Keahlian", "Tidak Ada"),
                                label = "Pilih Tunjangan...",
                                onSelected = { tunjangan = it }
                            )

                            // PROTEKSI GAJI MURNI & VISIBILITY DINAMIS
                            if (isAdmin) {
                                if (statusGaji.equals("Bulanan", ignoreCase = true)) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    FormLabel("GAJI POKOK BULANAN")
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Rp. ", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = OrbitronFontFamily)
                                        Box(modifier = Modifier.weight(1f)) {
                                            ModernTextField(
                                                value = gajiPokok,
                                                onValueChange = { gajiPokok = it },
                                                placeholder = "Contoh: 4500000",
                                                icon = Icons.Default.Payments
                                            )
                                        }
                                    }
                                }

                                // Kondisi B: Jika Status Gaji bertuliskan "Harian"
                                if (statusGaji.equals("Harian", ignoreCase = true)) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    FormLabel("GAJI HARIAN")
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            "Rp. ",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = OrbitronFontFamily
                                        )
                                        Box(modifier = Modifier.weight(1f)) {
                                            ModernTextField(
                                                value = gajiHarian,
                                                onValueChange = { gajiHarian = it },
                                                placeholder = "Contoh: 150000",
                                                icon = Icons.Default.Payments
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ================== SEKTOR 5: DATA PRIBADI & KONTAK ==================
                    ModernSectionHeader("DATA PRIBADI & KONTAK DARURAT", Icons.Default.ContactPage)
                    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = SciFiGlass, border = BorderStroke(1.dp, Brush.verticalGradient(listOf(SciFiBorderLight, Color.Transparent)))) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            FormLabel("TEMPAT LAHIR")
                            ModernTextField(value = tLahir, onValueChange = { tLahir = it }, placeholder = "Kota Lahir...", icon = Icons.Default.LocationOn)
                            Spacer(modifier = Modifier.height(14.dp))
                            FormLabel("TANGGAL LAHIR (DD/MM/YYYY)")
                            ModernTextField(value = tglLahir, onValueChange = { tglLahir = it }, placeholder = "Contoh: 20/04/1987", icon = Icons.Default.Cake)
                            Spacer(modifier = Modifier.height(14.dp))
                            FormLabel("ALAMAT RUMAH")
                            ModernTextField(value = alamat, onValueChange = { alamat = it }, placeholder = "Alamat domisili...", minLines = 2)
                            Spacer(modifier = Modifier.height(14.dp))
                            FormLabel("NOMOR TELEPON / WA")
                            ModernTextField(value = noTelp, onValueChange = { noTelp = it }, placeholder = "08xxxxxxxxxx", icon = Icons.Default.Phone)
                            Spacer(modifier = Modifier.height(14.dp))
                            FormLabel("NOMOR TELEPON DARURAT")
                            ModernTextField(value = noTelpDarurat, onValueChange = { noTelpDarurat = it }, placeholder = "Kontak keluarga...", icon = Icons.Default.ContactPhone)
                            Spacer(modifier = Modifier.height(14.dp))
                            FormLabel("KETERANGAN TAMBAHAN")
                            ModernTextField(value = keterangan, onValueChange = { keterangan = it }, placeholder = "Catatan tambahan...", minLines = 2)
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // ================== TOMBOL SUBMIT CORE PULSE EFFECT ==================
                    val infiniteTransitionPulse = rememberInfiniteTransition(label = "btnGlow")
                    val glowBlurFloat by infiniteTransitionPulse.animateFloat(
                        initialValue = 6f, targetValue = 14f,
                        animationSpec = infiniteRepeatable(animation = tween(1000, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
                        label = "btnGlowPulse"
                    )

                    Box(modifier = Modifier.fillMaxWidth().height(54.dp), contentAlignment = Alignment.Center) {
                        val blurRadiusDp = (glowBlurFloat / LocalDensity.current.density).dp
                        Box(modifier = Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.8f).background(SciFiPurple.copy(alpha = 0.3f), RoundedCornerShape(16.dp)).blur(blurRadiusDp))

                        Button(
                            onClick = {
                                if (username.isEmpty() || nama.isEmpty() || nik.isEmpty()) {
                                    Toast.makeText(context, "Username, Nama, dan NIK wajib diisi!", Toast.LENGTH_LONG).show()
                                    return@Button
                                }

                                isSubmitting = true
                                submitDataTeknisi(
                                    context = context,
                                    payload = JSONObject().apply {
                                        put("action", "updateOrCreateUser")
                                        put("username", username)
                                        put("password", password)
                                        put("nama", nama)
                                        put("role", role)
                                        put("fungsi", fungsi)
                                        put("nik", nik)
                                        put("jabatan", jabatan)
                                        put("bagian", bagian)
                                        put("regu", regu)
                                        put("tglMasuk", tglMasuk)
                                        put("kontrakTerakhir", kontrakTerakhir)
                                        put("pendidikan", pendidikan)
                                        put("jurusan", jurusan)
                                        put("statusPegawai", statusPegawai)
                                        put("statusGaji", statusGaji)
                                        put("tunjangan", tunjangan)
                                        put("tLahir", tLahir)
                                        put("tglLahir", tglLahir)
                                        put("alamat", alamat)
                                        put("noTelp", noTelp)
                                        put("noTelpDarurat", noTelpDarurat)
                                        put("gajiPokok", if (gajiPokok.isEmpty()) "0" else gajiPokok)
                                        put("gajiHarian", if (gajiHarian.isEmpty()) "0" else gajiHarian)
                                        put("keterangan", keterangan)
                                    }
                                ) {
                                    isSubmitting = false
                                    if (isAdmin) {
                                        showList = true // Kembalikan Admin ke list agar data ter-refresh
                                    } else {
                                        onSuccess() // Lempar user harian/bulanan ke Dashboard
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize().background(brush = Brush.horizontalGradient(colors = listOf(SciFiPurple, SciFiBlue)), shape = RoundedCornerShape(16.dp)).border(1.dp, SciFiBorderMedium, RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSubmitting) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("SINKRONISASI DATA TEKNISI", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, fontFamily = OrbitronFontFamily)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(40.dp))
                }
            }
        }
    }
}

// ==================== CORESCRIPT SINKRONISASI DATABASE ====================
private fun submitDataTeknisi(context: android.content.Context, payload: JSONObject, onComplete: () -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val url = URL("https://script.google.com/macros/s/AKfycbzOrWNyuZsJ6K7L5icJU_BNZhznRbYYiK4-3ssqq0_69CkXhF6OVwOjvlPp-Qtvw4Q/exec")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")

            conn.outputStream.use { it.write(payload.toString().toByteArray()) }
            val responseCode = conn.responseCode

            withContext(Dispatchers.Main) {
                if (responseCode in 200..299) {
                    Toast.makeText(context, "✅ Sinkronisasi Database Berhasil!", Toast.LENGTH_LONG).show()
                    onComplete()
                } else {
                    Toast.makeText(context, "❌ Sinkronisasi Gagal! Code: $responseCode", Toast.LENGTH_LONG).show()
                    onComplete()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error Jaringan: ${e.message}", Toast.LENGTH_LONG).show()
                onComplete()
            }
        }
    }
}