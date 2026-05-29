package com.example.sitekiver01.screens

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.sitekiver01.OrbitronFontFamily
import com.example.sitekiver01.components.*
import com.example.sitekiver01.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OrderPartScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    // --- State Form ---
    var tglPesan by remember { mutableStateOf(sdf.format(Date())) }
    var kategori by remember { mutableStateOf("") }
    var namaPart by remember { mutableStateOf("") }
    var ukuranPart by remember { mutableStateOf("") }
    var kegunaan by remember { mutableStateOf("") }
    var jmlPesan by remember { mutableStateOf("") }
    var satuanLabel by remember { mutableStateOf("-") }
    var bagian by remember { mutableStateOf("") }
    var pemesan by remember { mutableStateOf("") }
    var mesinTerpilih by remember { mutableStateOf("") }

    // --- State Data ---
    var listStok by remember { mutableStateOf<List<List<String>>>(emptyList()) }
    var listTeknisi by remember { mutableStateOf<List<List<String>>>(emptyList()) }
    var listMesin by remember { mutableStateOf<List<List<String>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // --- Dialog Controls ---
    var showMachineDialog by remember { mutableStateOf(false) }
    var showPartSelection by remember { mutableStateOf(false) }
    var showAddNewPart by remember { mutableStateOf(false) }
    var showTeknisiDialog by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    val scriptUrl = "https://script.google.com/macros/s/AKfycbwHVQ2pB4rKZXuZTLcffgIAHiRgo4lP_wPCieNNOd2XFdOxhHehcoo5DgxSBd2wUl8/exec"

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val response = URL("$scriptUrl?action=getMetadataOrder").readText()
                val json = JSONObject(response)

                listStok = parseJsonArray(json.getJSONArray("stok"))
                listTeknisi = parseJsonArray(json.getJSONArray("teknisi"))
                listMesin = parseJsonArray(json.getJSONArray("mesin"))
            } catch (e: Exception) { e.printStackTrace() }
            finally { isLoading = false }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            containerColor = Color(0xFF0F172A),
            modifier = Modifier.border(1.dp, SciFiBorderLight, RoundedCornerShape(28.dp)),
            title = { Text("Konfirmasi Simpan", color = Color.White, fontFamily = OrbitronFontFamily) },
            text = { Text("Apakah data yang Anda masukkan sudah benar?", color = SciFiTextMuted) },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        isSubmitting = true
                        scope.launch {
                            val success = submitOrder(JSONObject().apply {
                                put("action", "submitOrder")
                                put("tglPesan", tglPesan)
                                put("kategori", kategori)
                                put("nama", namaPart)
                                put("ukuran", ukuranPart)
                                put("kegunaan", kegunaan)
                                put("jmlPesan", jmlPesan)
                                put("satuan", satuanLabel)
                                put("bagian", bagian)
                                put("pemesan", pemesan)
                                put("mesin", mesinTerpilih)
                                put("isNewData", !listStok.any { it[2] == namaPart })
                                put("status", "Open")
                            }, scriptUrl)
                            isSubmitting = false
                            if (success) {
                                Toast.makeText(context, "Data Tersimpan!", Toast.LENGTH_SHORT).show()
                                onBack()
                            } else {
                                Toast.makeText(context, "Gagal menyimpan data!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SciFiCyan)
                ) { Text("Ya", color = Color.Black, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text("Tidak", color = SciFiTextMuted) }
            }
        )
    }

    if (showPartSelection) {
        PartSelectionDialog(
            listStok = listStok,
            onDismiss = { showPartSelection = false },
            onTidakAdaData = { showAddNewPart = true },
            onSelected = { k, n, u, s ->
                kategori = k
                namaPart = n
                ukuranPart = u
                satuanLabel = s
                showPartSelection = false
            }
        )
    }

    if (showAddNewPart) {
        AddNewPartDialog(
            listStok = listStok,
            onDismiss = { showAddNewPart = false },
            onSave = { k, n, u ->
                kategori = k
                namaPart = n
                ukuranPart = u
                satuanLabel = "-"
                showAddNewPart = false
            }
        )
    }

    if (showMachineDialog) {
        MachineSelectionDialog(listMesin, onDismiss = { showMachineDialog = false }) {
            mesinTerpilih = it
            showMachineDialog = false
        }
    }

    if (showTeknisiDialog) {
        TeknisiSelectionDialog(listTeknisi, onDismiss = { showTeknisiDialog = false }) { b, p ->
            bagian = b
            pemesan = p
            showTeknisiDialog = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(GlassBase)) {
        // PONDASI UTAMA: Menggunakan Animasi Grid Siber Global
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
                            onClick = onBack,
                            modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                        }
                        Spacer(Modifier.width(16.dp))
                        Text(
                            "ORDER SPAREPART",
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
            if (isLoading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = SciFiCyan)
                }
            } else {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Spacer(Modifier.height(12.dp))

                    // SECTION 1: IDENTITAS
                    ModernSectionHeader("IDENTITAS PESANAN", Icons.Default.Info)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = SciFiGlass,
                        border = BorderStroke(1.dp, Brush.verticalGradient(listOf(SciFiBorderLight, Color.Transparent)))
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            FormLabel("TANGGAL PESAN")
                            ModernDatePickerField(tglPesan) { tglPesan = it }

                            Spacer(Modifier.height(14.dp))

                            FormLabel("SPAREPART")
                            ModernClickableField(
                                value = if (kategori.isEmpty() && namaPart.isEmpty()) ""
                                else "$kategori | $namaPart - $ukuranPart",
                                placeholder = "Pilih Kategori, Nama, & Ukuran...",
                                color = Color.White.copy(alpha = 0.03f)
                            ) { showPartSelection = true }
                        }
                    }

                    // SECTION 2: DETAIL
                    ModernSectionHeader("DETAIL PESANAN", Icons.Default.Inventory)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = SciFiGlass,
                        border = BorderStroke(1.dp, Brush.verticalGradient(listOf(SciFiBorderLight, Color.Transparent)))
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            FormLabel("KEGUNAAN")
                            ModernTextField(
                                value = kegunaan,
                                onValueChange = { kegunaan = it }, // <-- PERBAIKAN: Masukkan ke dalam kurung di urutan kedua
                                placeholder = "Part digunakan untuk...",
                                minLines = 2
                            )

                            Spacer(Modifier.height(14.dp))

                            FormLabel("JUMLAH PESAN")
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ModernTextField(
                                        value = jmlPesan,
                                        placeholder = "Jumlah...",
                                        onValueChange = { jmlPesan = it }
                                    )
                                }
                                Surface(
                                    color = Color.White.copy(alpha = 0.05f),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, SciFiBorderLight)
                                ) {
                                    Text(
                                        text = satuanLabel,
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    // SECTION 3: PENGGUNA
                    ModernSectionHeader("PENGGUNA & MESIN", Icons.Default.Person)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = SciFiGlass,
                        border = BorderStroke(1.dp, Brush.verticalGradient(listOf(SciFiBorderLight, Color.Transparent)))
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            FormLabel("BAGIAN & PEMESAN")
                            ModernClickableField(
                                value = if (bagian.isEmpty() && pemesan.isEmpty()) ""
                                else "$bagian | $pemesan",
                                placeholder = "Pilih Bagian & Pemesan...",
                                color = Color.White.copy(alpha = 0.03f)
                            ) { showTeknisiDialog = true }

                            Spacer(Modifier.height(14.dp))

                            FormLabel("MESIN")
                            ModernClickableField(
                                value = mesinTerpilih,
                                placeholder = "Pilih Mesin...",
                                color = Color.White.copy(alpha = 0.03f)
                            ) { showMachineDialog = true }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // CORE REACTOR SUBMIT BUTTON WITH GLOW
                    val infiniteTransitionPulse = rememberInfiniteTransition(label = "orderBtnGlow")
                    val glowBlurFloat by infiniteTransitionPulse.animateFloat(
                        initialValue = 6f,
                        targetValue = 14f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ), label = "orderBtnPulse"
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
                            onClick = { showConfirmDialog = true },
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Send, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("SUBMIT ORDER SPAREPART", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, fontFamily = OrbitronFontFamily)
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

// ==================== DIALOG COMPONENTS (CYBER THEME) ====================

@Composable
fun PartSelectionDialog(
    listStok: List<List<String>>,
    onDismiss: () -> Unit,
    onTidakAdaData: () -> Unit,
    onSelected: (kategori: String, nama: String, ukuran: String, satuan: String) -> Unit
) {
    val categories = remember(listStok) {
        listStok.map { it[1] }.distinct().filter { it.isNotBlank() && it != "Kategori" }
    }
    var step by remember { mutableStateOf(1) }
    var selKat by remember { mutableStateOf(categories.firstOrNull() ?: "") }
    var selNama by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            Modifier.fillMaxWidth().height(550.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.dp, SciFiBorderMedium)
        ) {
            Column {
                Box(
                    modifier = Modifier.fillMaxWidth().height(64.dp).background(Color.White.copy(alpha = 0.04f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (step == 1) "PILIH JENIS DAN NAMA" else "PILIH UKURAN",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        fontFamily = OrbitronFontFamily
                    )
                }

                if (step == 1) {
                    Row(modifier = Modifier.weight(1f)) {
                        LazyColumn(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF0B1220)).border(0.5.dp, SciFiBorderLight)) {
                            items(categories) { kat ->
                                val isSelected = selKat == kat
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selKat = kat }
                                        .background(if (isSelected) SciFiCyan.copy(alpha = 0.12f) else Color.Transparent)
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        kat,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) SciFiCyan else Color.White,
                                        fontSize = 13.sp
                                    )
                                    Icon(Icons.Default.ChevronRight, null, tint = if (isSelected) SciFiCyan else SciFiTextMuted, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        val names = remember(selKat, listStok) {
                            listStok.filter { it[1] == selKat }.map { it[2] }.distinct()
                        }
                        LazyColumn(modifier = Modifier.weight(1.2f).fillMaxHeight().background(Color(0xFF0F172A))) {
                            items(names) { name ->
                                Text(
                                    name,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selNama = name
                                            step = 2
                                        }
                                        .padding(14.dp),
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                                HorizontalDivider(color = SciFiBorderLight.copy(alpha = 0.4f))
                            }
                        }
                    }
                } else {
                    val sizes = remember(selNama, listStok) {
                        listStok.filter { it[1] == selKat && it[2] == selNama }.map { it[3] }.distinct()
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { step = 1 }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = SciFiCyan) }
                            Text("$selKat > $selNama", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                        }
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(sizes) { size ->
                                val satuan = listStok.find { it[1] == selKat && it[2] == selNama && it[3] == size }?.get(7) ?: "-"
                                Text(
                                    size,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelected(selKat, selNama, size, satuan) }
                                        .padding(14.dp),
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                                HorizontalDivider(color = SciFiBorderLight.copy(alpha = 0.4f))
                            }
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    OutlinedButton(
                        onClick = onTidakAdaData,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, SciFiCyan)
                    ) {
                        Text("TIDAK ADA DATA", color = SciFiCyan, fontWeight = FontWeight.Bold, fontFamily = OrbitronFontFamily, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AddNewPartDialog(
    listStok: List<List<String>>,
    onDismiss: () -> Unit,
    onSave: (kategori: String, nama: String, ukuran: String) -> Unit
) {
    var kat by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var size by remember { mutableStateOf("") }

    val katOptions = remember(listStok) {
        listStok.map { it[1] }.distinct().filter { it.isNotBlank() && it != "Kategori" }
    }
    val nameOptions = remember(kat, listStok) {
        listStok.filter { it[1] == kat }.map { it[2] }.distinct()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.dp, SciFiBorderMedium)
        ) {
            Column(Modifier.padding(24.dp)) {
                Text("TAMBAH DATA PART", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color.White, fontFamily = OrbitronFontFamily)
                Spacer(Modifier.height(20.dp))

                FormLabel("KATEGORI")
                ModernDropdownField(
                    selected = kat,
                    options = katOptions,
                    label = "Pilih Kategori...", // <-- Tambahkan parameter label ini
                    onSelected = { kat = it }
                )

                Spacer(Modifier.height(10.dp))

                FormLabel("NAMA PART")
                ModernDropdownField(
                    selected = name,
                    options = nameOptions,
                    label = "Pilih Nama Part...", // <-- Tambahkan parameter label ini
                    onSelected = { name = it }
                )

                Spacer(Modifier.height(10.dp))

                FormLabel("UKURAN")
                ModernTextField(
                    value = size,
                    onValueChange = { size = it }, // <-- PERBAIKAN: Pastikan menggunakan onValueChange eksplisit
                    placeholder = "Masukkan Ukuran"
                )

                Spacer(Modifier.height(28.dp))
                ModernButton(
                    text = "SIMPAN DATA",
                    onClick = {
                        if (kat.isNotBlank() && name.isNotBlank() && size.isNotBlank()) {
                            onSave(kat, name, size)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Batal", color = SciFiTextMuted)
                }
            }
        }
    }
}

@Composable
fun MachineSelectionDialog(listMesin: List<List<String>>, onDismiss: () -> Unit, onSelected: (String) -> Unit) {
    var step by remember { mutableStateOf(1) }
    var selKat by remember { mutableStateOf("") }
    var selJen by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            Modifier.fillMaxWidth().height(550.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.dp, SciFiBorderMedium)
        ) {
            Column {
                Box(
                    modifier = Modifier.fillMaxWidth().height(64.dp).background(Color.White.copy(alpha = 0.04f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        when(step) {
                            1 -> "PILIH KATEGORI & JENIS"
                            else -> "PILIH NAMA MESIN"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        fontFamily = OrbitronFontFamily
                    )
                }

                if (step == 1) {
                    val categories = remember(listMesin) {
                        listMesin.map { it[0] }.distinct().filter { it != "Kategori" }
                    }
                    if (selKat.isEmpty() && categories.isNotEmpty()) selKat = categories.first()

                    Row(modifier = Modifier.weight(1f)) {
                        LazyColumn(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF0B1220)).border(0.5.dp, SciFiBorderLight)) {
                            items(categories) { kat ->
                                val isSelected = selKat == kat
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selKat = kat }
                                        .background(if (isSelected) SciFiCyan.copy(alpha = 0.12f) else Color.Transparent)
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        kat,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) SciFiCyan else Color.White,
                                        fontSize = 13.sp
                                    )
                                    Icon(Icons.Default.ChevronRight, null, tint = if (isSelected) SciFiCyan else SciFiTextMuted, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        val types = remember(selKat, listMesin) {
                            listMesin.filter { it[0] == selKat }.map { it[1] }.distinct()
                        }
                        LazyColumn(modifier = Modifier.weight(1.2f).fillMaxHeight().background(Color(0xFF0F172A))) {
                            items(types) { type ->
                                Text(
                                    type,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selJen = type
                                            step = 2
                                        }
                                        .padding(14.dp),
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                                HorizontalDivider(color = SciFiBorderLight.copy(alpha = 0.4f))
                            }
                        }
                    }
                } else {
                    val names = remember(selJen, listMesin) {
                        listMesin.filter { it[1] == selJen }.map { it[2] }.distinct()
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { step = 1 }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = SciFiCyan) }
                            Text("$selKat > $selJen", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                        }
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(names) { name ->
                                Text(
                                    name,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelected(name) }
                                        .padding(14.dp),
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                                HorizontalDivider(color = SciFiBorderLight.copy(alpha = 0.4f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TeknisiSelectionDialog(
    listTeknisi: List<List<String>>,
    onDismiss: () -> Unit,
    onSelected: (bagian: String, pemesan: String) -> Unit
) {
    val sections = remember(listTeknisi) {
        listTeknisi.map { it[3] }.distinct().filter { it.isNotBlank() && it != "Bagian" }
    }
    var selSection by remember { mutableStateOf(sections.firstOrNull() ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            Modifier.fillMaxWidth().height(550.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.dp, SciFiBorderMedium)
        ) {
            Column {
                Box(
                    modifier = Modifier.fillMaxWidth().height(64.dp).background(Color.White.copy(alpha = 0.04f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "PILIH BAGIAN & PEMESAN",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        fontFamily = OrbitronFontFamily
                    )
                }

                Row(modifier = Modifier.weight(1f)) {
                    LazyColumn(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF0B1220)).border(0.5.dp, SciFiBorderLight)) {
                        items(sections) { section ->
                            val isSelected = selSection == section
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selSection = section }
                                    .background(if (isSelected) SciFiCyan.copy(alpha = 0.12f) else Color.Transparent)
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    section,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) SciFiCyan else Color.White,
                                    fontSize = 13.sp
                                )
                                Icon(Icons.Default.ChevronRight, null, tint = if (isSelected) SciFiCyan else SciFiTextMuted, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    val names = remember(selSection, listTeknisi) {
                        listTeknisi.filter { it[3] == selSection }.map { it[0] }.distinct()
                    }
                    LazyColumn(modifier = Modifier.weight(1.2f).fillMaxHeight().background(Color(0xFF0F172A))) {
                        items(names) { name ->
                            Text(
                                name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelected(selSection, name) }
                                    .padding(14.dp),
                                fontSize = 13.sp,
                                color = Color.White
                            )
                            HorizontalDivider(color = SciFiBorderLight.copy(alpha = 0.4f))
                        }
                    }
                }
            }
        }
    }
}

fun parseJsonArray(json: org.json.JSONArray): List<List<String>> {
    return (0 until json.length()).map { i ->
        val inner = json.getJSONArray(i)
        (0 until inner.length()).map { j -> inner.getString(j) }
    }
}

suspend fun submitOrder(data: JSONObject, scriptUrl: String): Boolean = withContext(Dispatchers.IO) {
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