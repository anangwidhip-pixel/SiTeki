package com.example.sitekiver01.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
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
import androidx.compose.ui.window.Dialog
import com.example.sitekiver01.OrbitronFontFamily
import com.example.sitekiver01.components.*
import com.example.sitekiver01.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
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
fun PenyelesaianOrderScreen(
    orderRowIndex: Int,
    namaMesin: String,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = FirebaseFirestore.getInstance()
    var isSubmitting by remember { mutableStateOf(false) }

    // PERBAIKAN LOCALE WARNING: Membungkus formatter ke dalam remember
    val sdfDate = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val sdfTime = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    // Form State
    var perbaikanDilakukan by remember { mutableStateOf("") }

    var tglMulai by remember { mutableStateOf(sdfDate.format(Date())) }
    var waktuMulai by remember { mutableStateOf(sdfTime.format(Date())) }

    var tglSelesai by remember { mutableStateOf(sdfDate.format(Date())) }
    var waktuSelesai by remember { mutableStateOf(sdfTime.format(Date())) }

    var statusMesin by remember { mutableStateOf("") }
    var nilaiPerbaikan by remember { mutableStateOf("Bagus") }
    var sparePart by remember { mutableStateOf("Tidak Pakai") }
    var ukuranSparePart by remember { mutableStateOf("Tidak Pakai") }
    var keterangan by remember { mutableStateOf("") }

    val SCRIPT_URL = "https://script.google.com/macros/s/AKfycbzbmKFheI55ccsJ_kLdOzy6VIdGpgKIy2s9pljrIM8sNbgJ_RLywnzF-Q2sJTslVQU/exec"
    val statusOptions = listOf("Repair", "Breakdown", "Tunggu Part", "Overhoul Mesin", "Yang Lain")

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
                        ) { Icon(Icons.Default.ArrowBackIosNew, "Back", tint = MaterialTheme.colorScheme.onSurface) }
                        Spacer(Modifier.width(16.dp))
                        Text(
                            "Penyelesaian Order",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            fontFamily = OrbitronFontFamily,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(Modifier.height(12.dp))

                // INFORMASI MESIN
                ModernSectionHeader("INFORMASI MESIN", Icons.Default.PrecisionManufacturing)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = SciFiGlass,
                    border = BorderStroke(1.dp, Brush.verticalGradient(listOf(SciFiBorderLight, Color.Transparent)))
                ) {
                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Settings, null, tint = SciFiCyan, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("NAMA MESIN", fontSize = 10.sp, color = SciFiTextMuted)
                            Text(namaMesin, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }

                // DETAIL PERBAIKAN
                ModernSectionHeader("DETAIL PERBAIKAN", Icons.Default.Engineering)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = SciFiGlass,
                    border = BorderStroke(1.dp, Brush.verticalGradient(listOf(SciFiBorderLight, Color.Transparent)))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        FormLabel("PERBAIKAN YANG DILAKUKAN")
                        ModernTextField(
                            value = perbaikanDilakukan,
                            onValueChange = { perbaikanDilakukan = it }, // PERBAIKAN: Posisi argumen dibenarkan
                            placeholder = "Uraikan perbaikan...",
                            minLines = 3
                        )

                        Spacer(Modifier.height(14.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(Modifier.weight(1f)) {
                                FormLabel("MULAI")
                                ModernDatePickerField(tglMulai) { tglMulai = it }
                                Spacer(modifier = Modifier.height(6.dp))
                                ModernTimePickerField(waktuMulai) { waktuMulai = it }
                            }
                            Column(Modifier.weight(1f)) {
                                FormLabel("SELESAI")
                                ModernDatePickerField(tglSelesai) { tglSelesai = it }
                                Spacer(modifier = Modifier.height(6.dp))
                                ModernTimePickerField(waktuSelesai) { waktuSelesai = it }
                            }
                        }
                    }
                }

                // HASIL & SPAREPART
                ModernSectionHeader("HASIL & SPAREPART", Icons.Default.Inventory)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = SciFiGlass,
                    border = BorderStroke(1.dp, Brush.verticalGradient(listOf(SciFiBorderLight, Color.Transparent)))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        FormLabel("STATUS MESIN")
                        ModernDropdownField(
                            selected = statusMesin,
                            options = statusOptions,
                            label = "Pilih Status...", // PERBAIKAN: Ditambahkan label pengenal
                            onSelected = { statusMesin = it }
                        )

                        Spacer(Modifier.height(14.dp))

                        FormLabel("SPAREPART DIGUNAKAN")
                        SparePartSelectorModern(
                            sparePart = sparePart,
                            ukuran = ukuranSparePart,
                            onSparePartChange = { sparePart = it },
                            onUkuranChange = { ukuranSparePart = it }
                        )

                        Spacer(Modifier.height(14.dp))

                        FormLabel("NILAI PERBAIKAN")
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Bagus", "Cukup", "Tidak Bagus").forEach { opt ->
                                ChoiceChip(opt, nilaiPerbaikan == opt) { nilaiPerbaikan = opt }
                            }
                        }
                    }
                }

                // KETERANGAN
                ModernSectionHeader("KETERANGAN", Icons.AutoMirrored.Filled.Assignment)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = SciFiGlass,
                    border = BorderStroke(1.dp, Brush.verticalGradient(listOf(SciFiBorderLight, Color.Transparent)))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        FormLabel("CATATAN TAMBAHAN")
                        ModernTextField(
                            value = keterangan,
                            onValueChange = { keterangan = it }, // PERBAIKAN: Posisi argumen dibenarkan
                            placeholder = "Keterangan tambahan...",
                            minLines = 3
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // BUTTON SUBMIT DENGAN GLOW RADIUS REACTOR PULSE
                val infiniteTransitionPulse = rememberInfiniteTransition(label = "completeBtnGlow")
                val glowBlurFloat by infiniteTransitionPulse.animateFloat(
                    initialValue = 6f,
                    targetValue = 14f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ), label = "completeBtnPulse"
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
                            if (perbaikanDilakukan.isEmpty() || statusMesin.isEmpty()) {
                                Toast.makeText(context, "Perbaikan dan Status Mesin wajib diisi!", Toast.LENGTH_LONG).show()
                                return@Button
                            }
                            isSubmitting = true
                            submitPenyelesaianData(
                                scriptUrl = SCRIPT_URL,
                                rowIndex = orderRowIndex,
                                perbaikan = perbaikanDilakukan,
                                jamMulai = "$tglMulai $waktuMulai",
                                jamSelesai = "$tglSelesai $waktuSelesai",
                                statusMesin = statusMesin,
                                nilaiPerbaikan = nilaiPerbaikan,
                                sparePart = sparePart,
                                ukuranSparePart = ukuranSparePart,
                                keterangan = keterangan,
                                context = context,
                                onSuccess = {
                                    isSubmitting = false
                                    onSuccess()
                                },
                                onError = { isSubmitting = false }
                            )
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
                                    Text("SELESAIKAN & TUTUP ORDER", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, fontFamily = OrbitronFontFamily)
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

@Composable
private fun SparePartSelectorModern(
    sparePart: String,
    ukuran: String,
    onSparePartChange: (String) -> Unit,
    onUkuranChange: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var isManualMode by remember { mutableStateOf(false) }
    val db = FirebaseFirestore.getInstance()

    var kategoriList by remember { mutableStateOf<List<String>>(emptyList()) }
    var namaList by remember { mutableStateOf<List<String>>(emptyList()) }
    var ukuranList by remember { mutableStateOf<List<String>>(emptyList()) }

    var selectedKategori by remember { mutableStateOf("") }
    var selectedNama by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(showDialog) {
        if (showDialog && kategoriList.isEmpty()) {
            isLoading = true
            db.collection("master_part").get().addOnSuccessListener { docs ->
                kategoriList = docs.mapNotNull { it.getString("Kategori") ?: it.getString("kategori") }.distinct().sorted()
                isLoading = false
            }.addOnFailureListener { isLoading = false }
        }
    }

    LaunchedEffect(selectedKategori) {
        if (selectedKategori.isNotEmpty()) {
            db.collection("master_part").whereEqualTo("Kategori", selectedKategori).get().addOnSuccessListener { docs ->
                namaList = docs.mapNotNull { it.getString("Nama") ?: it.getString("nama") }.distinct().sorted()
                selectedNama = ""
                ukuranList = emptyList()
            }
        }
    }

    LaunchedEffect(selectedNama) {
        if (selectedNama.isNotEmpty()) {
            db.collection("master_part")
                .whereEqualTo("Kategori", selectedKategori)
                .whereEqualTo("Nama", selectedNama)
                .get().addOnSuccessListener { docs ->
                    ukuranList = docs.mapNotNull { it.getString("Ukuran") ?: it.getString("ukuran") }.distinct().sorted()
                }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ModernClickableField(
            value = if (sparePart.isEmpty()) "Pilih Sparepart..." else sparePart,
            placeholder = "Pilih Sparepart...",
            color = Color.White.copy(alpha = 0.03f)
        ) {
            isManualMode = false
            showDialog = true
        }

        if (isManualMode || sparePart != "Tidak Pakai") {
            ModernTextField(
                value = ukuran,
                onValueChange = { onUkuranChange(it) }, // PERBAIKAN: Posisi argumen dibenarkan
                placeholder = "Ukuran..."
            )
        }
    }

    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth(0.95f),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF0F172A),
                border = BorderStroke(1.dp, SciFiBorderMedium)
            ) {
                Column(Modifier.padding(24.dp)) {
                    Text("PILIH SPARE PART", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, fontFamily = OrbitronFontFamily)
                    Spacer(Modifier.height(20.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                onSparePartChange("Tidak Pakai")
                                onUkuranChange("Tidak Pakai")
                                showDialog = false
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("TIDAK PAKAI", fontSize = 11.sp, color = Color.White) }

                        Button(
                            onClick = {
                                isManualMode = true
                                onSparePartChange("")
                                onUkuranChange("")
                                showDialog = false
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = SciFiCyan),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("MANUAL", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold) }
                    }

                    Spacer(Modifier.height(16.dp))

                    if (isLoading) {
                        CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally), color = SciFiCyan)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            FormLabel("KATEGORI")
                            ModernDropdownField(
                                selected = selectedKategori,
                                options = kategoriList,
                                label = "Pilih Kategori...",
                                onSelected = { selectedKategori = it }
                            )

                            if (namaList.isNotEmpty()) {
                                FormLabel("NAMA PART")
                                ModernDropdownField(
                                    selected = selectedNama,
                                    options = namaList,
                                    label = "Pilih Nama...",
                                    onSelected = {
                                        selectedNama = it
                                        onSparePartChange(it)
                                    }
                                )
                            }

                            if (ukuranList.isNotEmpty()) {
                                FormLabel("UKURAN")
                                ModernDropdownField(
                                    selected = ukuran,
                                    options = ukuranList,
                                    label = "Pilih Ukuran...",
                                    onSelected = { onUkuranChange(it) }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showDialog = false }) {
                            Text("TUTUP", color = SciFiTextMuted)
                        }
                    }
                }
            }
        }
    }
}

private fun submitPenyelesaianData(
    scriptUrl: String, rowIndex: Int, perbaikan: String, jamMulai: String, jamSelesai: String,
    statusMesin: String, nilaiPerbaikan: String, sparePart: String, ukuranSparePart: String,
    keterangan: String, context: Context, onSuccess: () -> Unit, onError: () -> Unit
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val json = JSONObject().apply {
                put("action", "complete")
                put("rowIndex", rowIndex)
                put("perbaikanDilakukan", perbaikan)
                put("jamMulai", jamMulai)
                put("jamSelesai", jamSelesai)
                put("statusMesin", statusMesin)
                put("nilaiPerbaikan", nilaiPerbaikan)
                put("sparePart", sparePart)
                put("ukuranSparePart", ukuranSparePart)
                put("keterangan", keterangan)
            }
            val conn = URL(scriptUrl).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.outputStream.use { it.write(json.toString().toByteArray()) }

            val code = conn.responseCode
            withContext(Dispatchers.Main) {
                if (code in 200..299) {
                    Toast.makeText(context, "✅ Berhasil disimpan!", Toast.LENGTH_SHORT).show()
                    onSuccess()
                } else {
                    Toast.makeText(context, "Gagal: $code", Toast.LENGTH_LONG).show()
                    onError()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                onError()
            }
        }
    }
}
