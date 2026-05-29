package com.example.sitekiver01.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.sitekiver01.ui.theme.*
import com.example.sitekiver01.OrbitronFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import com.example.sitekiver01.components.*
import com.example.sitekiver01.model.Part
import com.example.sitekiver01.repository.MesinRepository
import com.example.sitekiver01.repository.PartRepository
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun IsiLaporanScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // PERBAIKAN LOCALE WARNING: Membungkus Date Formatter ke dalam remember
    val sdfDate = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val sdfFull = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    val mesinRepo = remember { MesinRepository() }
    val partRepo = remember { PartRepository() }
    val db = remember { FirebaseFirestore.getInstance() }

    // --- Database States ---
    var isLoadingData by remember { mutableStateOf(false) }

    // --- Form States ---
    var tanggal by remember { mutableStateOf(sdfDate.format(Date())) }
    var bagian by remember { mutableStateOf("") }
    var jenisMesin by remember { mutableStateOf("") }
    var namaMesin by remember { mutableStateOf("") }
    var jenisPekerjaan by remember { mutableStateOf("") }
    var laporanPekerjaan by remember { mutableStateOf("") }
    var jenisKomponen by remember { mutableStateOf("") }

    var tglMulai by remember { mutableStateOf(tanggal) }
    var jamMulai by remember { mutableStateOf("08:00") }
    var tglSelesai by remember { mutableStateOf(tanggal) }
    var jamSelesai by remember { mutableStateOf("09:00") }

    var definisi by remember { mutableStateOf("") }
    var sparepartSelected by remember { mutableStateOf("") }
    var sparepartKategori by remember { mutableStateOf("") }
    var ukuranPart by remember { mutableStateOf("") }
    var orderValue by remember { mutableStateOf("") }
    var statusOrderValue by remember { mutableStateOf("") }
    var nilaiPerbaikan by remember { mutableStateOf("") }
    var keterangan by remember { mutableStateOf("") }

    // Flags
    var isNewMachine by remember { mutableStateOf(false) }
    var isNewPart by remember { mutableStateOf(false) }
    var isMigrating by remember { mutableStateOf(false) }
    var isMigratingPart by remember { mutableStateOf(false) }

    // Dialog Controls
    var showSparepartDialog by remember { mutableStateOf(false) }
    var showMachineDialog by remember { mutableStateOf(false) }
    var showAddMachineDialog by remember { mutableStateOf(false) }
    var showAddPartDialog by remember { mutableStateOf(false) }
    var showAddSizeDialog by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    val scriptUrl = "https://script.google.com/macros/s/AKfycbwYHHf8ONKbs9m5CppnzUuo067CBvrqRRfLYzl5ABwOH81sVWnFD8AyPx6F6Vf3uC4/exec"
    val migrationScriptUrl = "https://script.google.com/macros/s/AKfycbyLAKLUbUpzWwuR3KSet3pPyEQhV9d1pWubackduAToyYPeZpQm96AFJM7gPHaL5mTyeum/exec"
    val migrationPartScriptUrl = "https://script.google.com/macros/s/AKfycbyLAKLUbUpzWwuR3KSet3pPyEQhV9d1pWuqduAToyYPeZpQm96AFJM7gPHaL5mTyeum/exec?action=getPart"

    val kategoriMesin = if (bagian == "Bengkel") "Armada" else "Mesin"
    val labelSuffix = if (bagian == "Bengkel") "ARMADA" else "MESIN"

    var listMesinFirebase by remember { mutableStateOf<List<List<String>>>(emptyList()) }
    var isLoadingMesin by remember { mutableStateOf(true) }

    var listPartFirebase by remember { mutableStateOf<List<Part>>(emptyList()) }
    var isLoadingPart by remember { mutableStateOf(true) }

    // Hitung Total Jam
    val totalJam = remember(tglMulai, jamMulai, tglSelesai, jamSelesai) {
        try {
            val d1 = sdfFull.parse("$tglMulai $jamMulai")
            val d2 = sdfFull.parse("$tglSelesai $jamSelesai")
            val diff = d2!!.time - d1!!.time
            val hours = diff.toFloat() / (1000 * 60 * 60)
            if (hours < 0) "0,00" else String.format(Locale("id", "ID"), "%.2f", hours)
        } catch (e: Exception) { "0,00" }
    }

    val loadMesinData = {
        isLoadingMesin = true
        db.collection("master_mesin")
            .get()
            .addOnSuccessListener { result ->
                val mesinList = result.map { doc ->
                    listOf(
                        doc.getString("Kategori") ?: doc.getString("kategori") ?: "Mesin",
                        doc.getString("Jenis") ?: doc.getString("jenis") ?: "",
                        doc.getString("Nama") ?: doc.getString("nama") ?: ""
                    )
                }
                listMesinFirebase = mesinList
                isLoadingMesin = false
            }
            .addOnFailureListener {
                isLoadingMesin = false
                Toast.makeText(context, "Gagal memuat data mesin", Toast.LENGTH_SHORT).show()
            }
    }

    val loadPartData = {
        isLoadingPart = true
        scope.launch {
            listPartFirebase = partRepo.getAllPart()
            isLoadingPart = false
        }
    }

    LaunchedEffect(Unit) {
        loadMesinData()
        loadPartData()
    }

    Box(modifier = Modifier.fillMaxSize().background(GlassBase)) {

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
                        ) { Icon(Icons.Default.ArrowBackIosNew, "", tint = Color.White) }
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = "ISI LAPORAN PEKERJAAN",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            fontFamily = OrbitronFontFamily,
                            letterSpacing = 1.sp
                        )
                        Spacer(Modifier.weight(1f))

                        if (isMigrating || isMigratingPart) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = SciFiCyan, strokeWidth = 2.dp)
                        } else {
                            IconButton(onClick = {
                                isMigrating = true
                                scope.launch {
                                    val result = mesinRepo.migrateAll(migrationScriptUrl)
                                    isMigrating = false
                                    if (result.isSuccess) {
                                        Toast.makeText(context, "Migrasi Mesin Berhasil", Toast.LENGTH_SHORT).show()
                                        loadMesinData()
                                    } else {
                                        Toast.makeText(context, "Gagal Migrasi Mesin", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }, modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)) {
                                Icon(Icons.Default.SettingsSuggest, "Migrasi Mesin", tint = Color.White)
                            }
                            Spacer(Modifier.width(8.dp))
                            IconButton(onClick = {
                                isMigratingPart = true
                                scope.launch {
                                    val result = partRepo.migrateAll(migrationPartScriptUrl)
                                    isMigratingPart = false
                                    if (result.isSuccess) {
                                        Toast.makeText(context, "Migrasi Part Berhasil", Toast.LENGTH_SHORT).show()
                                        loadPartData()
                                    } else {
                                        Toast.makeText(context, "Gagal Migrasi Part: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }, modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)) {
                                Icon(Icons.Default.Inventory, "Migrasi Part", tint = Color.White)
                            }
                        }
                    }
                }
            }
        ) { p ->
            Column(
                modifier = Modifier
                    .padding(p)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(Modifier.height(12.dp))

                // SECTION 1: IDENTITAS PEKERJAAN
                ModernSectionHeader("IDENTITAS PEKERJAAN", Icons.Default.Info)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = SciFiGlass,
                    border = BorderStroke(1.dp, Brush.verticalGradient(listOf(SciFiBorderLight, Color.Transparent)))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        FormLabel("TANGGAL")
                        ModernDatePickerField(tanggal) { tanggal = it }

                        Spacer(Modifier.height(14.dp))

                        FormLabel("BAGIAN")
                        FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Teknik", "Teknik A", "Teknik B", "Umum", "Bengkel", "Konstruksi").forEach {
                                ChoiceChip(it, bagian == it) { bagian = it; jenisMesin = ""; namaMesin = "" }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        FormLabel("JENIS $labelSuffix")
                        Box(modifier = Modifier.fillMaxWidth()) {
                            ModernClickableField(
                                value = if (jenisMesin.isEmpty()) "Pilih Jenis $labelSuffix..." else jenisMesin,
                                placeholder = "Pilih Jenis $labelSuffix...",
                                color = Color.White.copy(alpha = 0.03f)
                            ) {
                                if(bagian.isNotEmpty()) showMachineDialog = true
                                else Toast.makeText(context, "Pilih Bagian Terlebih Dahulu", Toast.LENGTH_SHORT).show()
                            }
                            if (isLoadingMesin) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp).align(Alignment.CenterEnd).padding(end = 16.dp), strokeWidth = 2.dp, color = SciFiCyan)
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        FormLabel("NAMA $labelSuffix")
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, SciFiBorderLight),
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White.copy(alpha = 0.03f)
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (namaMesin.isEmpty()) "Nama $labelSuffix otomatis terisi..." else namaMesin,
                                    color = if (namaMesin.isEmpty()) SciFiTextMuted else Color.White,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(Icons.Default.Lock, null, tint = SciFiTextMuted, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                // SECTION 2: DETAIL PEKERJAAN
                ModernSectionHeader("DETAIL PEKERJAAN", Icons.Default.Engineering)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = SciFiGlass,
                    border = BorderStroke(1.dp, Brush.verticalGradient(listOf(SciFiBorderLight, Color.Transparent)))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        FormLabel("JENIS PEKERJAAN")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Perbaikan", "Pemeriksaan", "Pemasangan", "Pemindahan", "Pembuatan", "Setting").forEach {
                                ChoiceChip(it, jenisPekerjaan == it) { jenisPekerjaan = it }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        FormLabel("LAPORAN PEKERJAAN")
                        // PERBAIKAN: Parameter onValueChange dibenarkan posisi argumennya
                        ModernTextField(
                            value = laporanPekerjaan,
                            onValueChange = { laporanPekerjaan = it },
                            placeholder = "Uraikan pekerjaan...",
                            minLines = 3
                        )

                        Spacer(Modifier.height(14.dp))

                        FormLabel("JENIS KOMPONEN")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Mekanikal", "Elektrikal", "Konstruksi").forEach { ChoiceChip(it, jenisKomponen == it) { jenisKomponen = it } }
                        }
                    }
                }

                // SECTION 3: WAKTU & DURASI
                ModernSectionHeader("WAKTU & DURASI", Icons.Default.Timer)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = SciFiGlass,
                    border = BorderStroke(1.dp, Brush.verticalGradient(listOf(SciFiBorderLight, Color.Transparent)))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(Modifier.weight(1f)) {
                                FormLabel("MULAI")
                                ModernDatePickerField(tglMulai, "Tgl") { tglMulai = it }
                                Spacer(Modifier.height(8.dp))
                                ModernTimePickerField(jamMulai, "Jam") { jamMulai = it }
                            }
                            Column(Modifier.weight(1f)) {
                                FormLabel("SELESAI")
                                ModernDatePickerField(tglSelesai, "Tgl") { tglSelesai = it }
                                Spacer(Modifier.height(8.dp))
                                ModernTimePickerField(jamSelesai, "Jam") { jamSelesai = it }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .background(SciFiCyan.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .border(1.dp, SciFiCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                            contentAlignment = Alignment.Center) {
                            Text("Total Durasi: $totalJam Jam", color = SciFiCyan, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }

                // SECTION 4: MATERIAL & HASIL
                ModernSectionHeader("MATERIAL & HASIL", Icons.Default.Inventory)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = SciFiGlass,
                    border = BorderStroke(1.dp, Brush.verticalGradient(listOf(SciFiBorderLight, Color.Transparent)))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        FormLabel("DEFINISI")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Tunggu Part", "Overhaul", "Kirim Luar").forEach { ChoiceChip(it, definisi == it) { definisi = it } }
                        }

                        Spacer(Modifier.height(14.dp))

                        FormLabel("SPAREPART DIPAKAI")
                        Box(modifier = Modifier.fillMaxWidth()) {
                            ModernClickableField(
                                value = if (sparepartSelected.isEmpty()) "Klik pilih Sparepart..." else sparepartSelected,
                                placeholder = "Klik pilih Sparepart...",
                                color = Color.White.copy(alpha = 0.03f)
                            ) { showSparepartDialog = true }
                            if (isLoadingPart) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp).align(Alignment.CenterEnd).padding(end = 16.dp), strokeWidth = 2.dp, color = SciFiCyan)
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        FormLabel("KODE/JENIS/UKURAN PART")
                        val ukuranOptions = remember(sparepartSelected, listPartFirebase) {
                            if (sparepartSelected == "Klik pilih Sparepart..." || sparepartSelected == "Tidak Pakai") {
                                mutableListOf()
                            } else {
                                listPartFirebase
                                    .filter { it.nama.trim().equals(sparepartSelected.trim(), ignoreCase = true) }
                                    .map { it.ukuran.trim() }
                                    .filter { it.isNotEmpty() }
                                    .distinct()
                                    .sorted()
                                    .toMutableList()
                            }
                        }

                        if (sparepartSelected.isNotEmpty() &&
                            sparepartSelected != "Klik pilih Sparepart..." &&
                            sparepartSelected != "Tidak Pakai") {
                            if (!ukuranOptions.contains("Lainnya : ...")) ukuranOptions.add("Lainnya : ...")
                        }

                        ModernDropdownField(
                            selected = ukuranPart,
                            options = ukuranOptions,
                            label = "Pilih Ukuran...", // PERBAIKAN: Ditambahkan parameter label
                            onSelected = {
                                if (it == "Lainnya : ...") showAddSizeDialog = true
                                else { ukuranPart = it; isNewPart = false }
                            }
                        )

                        Spacer(Modifier.height(14.dp))

                        FormLabel("NILAI PERBAIKAN")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Bagus", "Cukup", "Tidak Bagus").forEach { ChoiceChip(it, nilaiPerbaikan == it) { nilaiPerbaikan = it } }
                        }
                    }
                }

                // SECTION 5: STATUS & ORDER
                ModernSectionHeader("STATUS & ORDER", Icons.Default.Assignment)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = SciFiGlass,
                    border = BorderStroke(1.dp, Brush.verticalGradient(listOf(SciFiBorderLight, Color.Transparent)))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        FormLabel("ORDER SPAREPART")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Order", "Tanpa Order").forEach { choice ->
                                ChoiceChip(choice, orderValue == choice) { orderValue = choice }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        FormLabel("STATUS PEKERJAAN")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Open", "Close").forEach { choice ->
                                ChoiceChip(choice, statusOrderValue == choice) { statusOrderValue = choice }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        FormLabel("KETERANGAN")
                        // PERBAIKAN: Parameter onValueChange dibenarkan posisi argumennya
                        ModernTextField(
                            value = keterangan,
                            onValueChange = { keterangan = it },
                            placeholder = "Catatan tambahan...",
                            minLines = 4
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // BUTTON SUBMIT DENGAN GLOW EFFECTS REACTOR PULSE
                val infiniteTransitionPulse = rememberInfiniteTransition(label = "reportBtnGlow")
                val glowBlurFloat by infiniteTransitionPulse.animateFloat(
                    initialValue = 6f,
                    targetValue = 14f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ), label = "reportBtnPulse"
                )

                Box(
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val blurRadiusDp = (glowBlurFloat / LocalDensity.current.density).dp
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .fillMaxHeight(0.8f)
                            .background(SciFiCyan.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            .blur(blurRadiusDp)
                    )

                    Button(
                        onClick = {
                            isSubmitting = true
                            scope.launch {
                                val mappedBagian = when (bagian) {
                                    "Teknik A" -> "Tek. Shift A"
                                    "Teknik B" -> "Tek. Shift B"
                                    else -> bagian
                                }
                                val data = JSONObject().apply {
                                    put("tanggal", tanggal); put("bagian", mappedBagian); put("kategoriMesin", kategoriMesin)
                                    put("jenis", jenisMesin); put("namaMesin", namaMesin); put("jenisPekerjaan", jenisPekerjaan)
                                    put("laporan", laporanPekerjaan); put("jenisKomponen", jenisKomponen)
                                    put("jamMulai", "$tglMulai $jamMulai"); put("jamSelesai", "$tglSelesai $jamSelesai")
                                    put("totalJam", totalJam); put("definisi", definisi); put("sparepart", sparepartSelected)
                                    put("ukuranPart", ukuranPart); put("order", orderValue); put("statusOrder", statusOrderValue)
                                    put("nilaiPerbaikan", nilaiPerbaikan); put("keterangan", keterangan)
                                    put("isNewMachine", isNewMachine); put("isNewPart", isNewPart)
                                    put("partKategori", sparepartKategori); put("partNama", sparepartSelected); put("partUkuran", ukuranPart)
                                }
                                val success = submitToDatabase(data, scriptUrl)
                                isSubmitting = false
                                if (success) {
                                    Toast.makeText(context, "Data Berhasil Disimpan", Toast.LENGTH_SHORT).show()
                                    onBack()
                                } else Toast.makeText(context, "Gagal Simpan", Toast.LENGTH_SHORT).show()
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
                            if (isSubmitting) {
                                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CloudUpload, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("SIMPAN DATA LAPORAN", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, fontFamily = OrbitronFontFamily)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(40.dp))
            }
        }
    }

    if (showMachineDialog) {
        MachineDialog(
            listMesin = listMesinFirebase,
            kategori = kategoriMesin,
            onDismiss = { showMachineDialog = false },
            onSelected = { jenis, nama ->
                jenisMesin = jenis
                namaMesin = nama
                isNewMachine = false
                showMachineDialog = false
            },
            onNoData = {
                showMachineDialog = false
                showAddMachineDialog = true
            }
        )
    }

    if (showSparepartDialog) {
        SparepartDialog(listPart = listPartFirebase,
            onDismiss = { showSparepartDialog = false },
            onSelected = { cat, name, size ->
                sparepartKategori = cat
                sparepartSelected = name
                ukuranPart = size
                isNewPart = false
                showSparepartDialog = false
            },
            onNoData = {
                showSparepartDialog = false
                showAddPartDialog = true
            },
            onTidakPakai = {
                sparepartSelected = "Tidak Pakai"
                sparepartKategori = "-"
                ukuranPart = ""
                isNewPart = false
                showSparepartDialog = false
            }
        )
    }

    if (showAddMachineDialog) {
        var nJenis by remember { mutableStateOf("") }; var nNama by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { showAddMachineDialog = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF0F172A),
                border = BorderStroke(1.dp, SciFiBorderMedium)
            ) {
                Column(Modifier.padding(24.dp)) {
                    Text("TAMBAH $labelSuffix BARU", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold, fontFamily = OrbitronFontFamily)
                    Spacer(Modifier.height(16.dp))
                    FormLabel("JENIS $labelSuffix")
                    ModernTextField(value = nJenis, onValueChange = { nJenis = it }, placeholder = "Masukkan jenis...")
                    FormLabel("NAMA $labelSuffix")
                    ModernTextField(value = nNama, onValueChange = { nNama = it }, placeholder = "Masukkan nama...")
                    Spacer(Modifier.height(24.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showAddMachineDialog = false }) { Text("BATAL", color = SciFiTextMuted) }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = {
                            if(nJenis.isNotEmpty() && nNama.isNotEmpty()){
                                jenisMesin = nJenis; namaMesin = nNama; isNewMachine = true; showAddMachineDialog = false
                            } else Toast.makeText(context, "Isi Semua Data", Toast.LENGTH_SHORT).show()
                        }, colors = ButtonDefaults.buttonColors(containerColor = SciFiCyan), shape = RoundedCornerShape(12.dp)) {
                            Text("SIMPAN", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showAddPartDialog) {
        var nCat by remember { mutableStateOf("") }; var nName by remember { mutableStateOf("") }; var nSize by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { showAddPartDialog = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF0F172A),
                border = BorderStroke(1.dp, SciFiBorderMedium)
            ) {
                Column(Modifier.padding(24.dp)) {
                    Text("TAMBAH SPAREPART BARU", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold, fontFamily = OrbitronFontFamily)
                    Spacer(Modifier.height(16.dp))
                    FormLabel("KATEGORI")
                    ModernTextField(value = nCat, onValueChange = { nCat = it }, placeholder = "Kategori...")
                    FormLabel("NAMA PART")
                    ModernTextField(value = nName, onValueChange = { nName = it }, placeholder = "Nama part...")
                    FormLabel("UKURAN/KODE")
                    ModernTextField(value = nSize, onValueChange = { nSize = it }, placeholder = "Ukuran/kode...")
                    Spacer(Modifier.height(24.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showAddPartDialog = false }) { Text("BATAL", color = SciFiTextMuted) }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = {
                            sparepartKategori = nCat; sparepartSelected = nName; ukuranPart = nSize; isNewPart = true; showAddPartDialog = false
                        }, colors = ButtonDefaults.buttonColors(containerColor = SciFiCyan), shape = RoundedCornerShape(12.dp)) {
                            Text("SIMPAN", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showAddSizeDialog) {
        var nSize by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { showAddSizeDialog = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF0F172A),
                border = BorderStroke(1.dp, SciFiBorderMedium)
            ) {
                Column(Modifier.padding(24.dp)) {
                    Text("TAMBAH UKURAN BARU", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold, fontFamily = OrbitronFontFamily)
                    Spacer(Modifier.height(16.dp))
                    FormLabel("UKURAN BARU")
                    ModernTextField(value = nSize, onValueChange = { nSize = it }, placeholder = "Masukkan ukuran...")
                    Spacer(Modifier.height(24.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showAddSizeDialog = false }) { Text("BATAL", color = SciFiTextMuted) }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = {
                            ukuranPart = nSize; isNewPart = true; showAddSizeDialog = false
                        }, colors = ButtonDefaults.buttonColors(containerColor = SciFiCyan), shape = RoundedCornerShape(12.dp)) {
                            Text("SIMPAN", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MachineDialog(listMesin: List<List<String>>, kategori: String, onDismiss: () -> Unit, onSelected: (String, String) -> Unit, onNoData: () -> Unit) {
    var selectedJenis by remember { mutableStateOf("") }
    val filteredByKategori = listMesin.filter { it.size > 0 && it[0].trim().equals(kategori, ignoreCase = true) }
    val jenisList = filteredByKategori.map { it[1] }.distinct().sorted()
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(540.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.dp, SciFiBorderMedium)
        ) {
            Column {
                Box(Modifier.fillMaxWidth().height(64.dp).background(Color.White.copy(alpha = 0.04f)), Alignment.Center) {
                    Text("PILIH JENIS DAN NAMA", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = OrbitronFontFamily)
                }
                Row(Modifier.weight(1f)) {
                    LazyColumn(Modifier.weight(1f).fillMaxHeight().background(Color(0xFF0B1220)).border(0.5.dp, SciFiBorderLight)) {
                        items(jenisList) { jenis ->
                            Row(modifier = Modifier.fillMaxWidth().clickable { selectedJenis = jenis }.background(if (selectedJenis == jenis) SciFiCyan.copy(alpha = 0.12f) else Color.Transparent).padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(jenis, fontSize = 13.sp, color = if (selectedJenis == jenis) SciFiCyan else Color.White, fontWeight = if (selectedJenis == jenis) FontWeight.Bold else FontWeight.Normal);
                                Icon(Icons.Default.ChevronRight, "", tint = if (selectedJenis == jenis) SciFiCyan else SciFiTextMuted, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    LazyColumn(Modifier.weight(1.2f).fillMaxHeight().background(Color(0xFF0F172A))) {
                        items(filteredByKategori.filter { it.size > 1 && it[1].trim().equals(selectedJenis, ignoreCase = true) }.map { it[2] }.distinct().sorted()) { name ->
                            Text(text = name, modifier = Modifier.fillMaxWidth().clickable { onSelected(selectedJenis, name) }.padding(14.dp), fontSize = 13.sp, color = Color.White)
                        }
                    }
                }
                Box(Modifier.fillMaxWidth().padding(16.dp), Alignment.Center) {
                    OutlinedButton(onClick = onNoData, modifier = Modifier.fillMaxWidth().height(46.dp), shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, SciFiCyan)) {
                        Text("TIDAK ADA DATA", color = SciFiCyan, fontWeight = FontWeight.Bold, fontFamily = OrbitronFontFamily, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SparepartDialog(listPart: List<Part>,
                    onDismiss: () -> Unit,
                    onSelected: (String, String, String) -> Unit,
                    onNoData: () -> Unit,
                    onTidakPakai: () -> Unit
) {
    val kategoris = remember(listPart) { listPart.map { it.kategori }.distinct().sorted() }
    var selectedKategori by remember { mutableStateOf(if (kategoris.isNotEmpty()) kategoris[0] else "") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(500.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.dp, SciFiBorderMedium)
        ) {
            Column {
                Box(modifier = Modifier.fillMaxWidth().height(56.dp).background(Color.White.copy(alpha = 0.04f)), contentAlignment = Alignment.Center) {
                    Text("PILIH SPAREPART", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = OrbitronFontFamily)
                }
                Row(modifier = Modifier.weight(1f)) {
                    LazyColumn(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF0B1220)).border(0.5.dp, SciFiBorderLight)) {
                        items(kategoris) { cat ->
                            val isSelected = selectedKategori == cat
                            Text(text = cat, modifier = Modifier.fillMaxWidth().background(if (isSelected) SciFiCyan.copy(alpha = 0.12f) else Color.Transparent).clickable { selectedKategori = cat }.padding(16.dp), fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) SciFiCyan else Color.White)
                            HorizontalDivider(color = SciFiBorderLight.copy(alpha = 0.3f))
                        }
                    }
                    LazyColumn(modifier = Modifier.weight(1.2f).fillMaxHeight().background(Color(0xFF0F172A))) {
                        val filteredParts = listPart.filter { it.kategori == selectedKategori }.map { it.nama }.distinct().sorted()
                        items(filteredParts) { partName ->
                            val firstPart = listPart.find { it.nama == partName && it.kategori == selectedKategori }
                            Text(text = partName, modifier = Modifier.fillMaxWidth().clickable { onSelected(selectedKategori, partName, firstPart?.ukuran ?: "") }.padding(16.dp), fontSize = 13.sp, color = Color.White)
                            HorizontalDivider(color = SciFiBorderLight.copy(alpha = 0.3f))
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onTidakPakai, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red), border = BorderStroke(1.dp, Color.Red)) { Text("TIDAK PAKAI", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = OrbitronFontFamily) }
                    Button(onClick = onNoData, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) { Text("TIDAK ADA DATA", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = OrbitronFontFamily, color = Color.White) }
                }
            }
        }
    }
}

suspend fun submitToDatabase(data: JSONObject, scriptUrl: String): Boolean = withContext(Dispatchers.IO) {
    try {
        val conn = URL(scriptUrl).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json")
        conn.outputStream.write(data.toString().toByteArray())
        val responseCode = conn.responseCode
        responseCode == 200 || responseCode == 302
    } catch (e: Exception) { false }
}