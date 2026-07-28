package com.example.sitekiver01.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import java.util.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BuatOrderKerjaScreen(
    machineNameFromQR: String = "",
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    // State Form
    var bagianOrder by remember { mutableStateOf("") }
    var namaOrder by remember { mutableStateOf("") }
    var bagianTujuan by remember { mutableStateOf("") }
    var jenisPekerjaan by remember { mutableStateOf("") }
    var kerusakan by remember { mutableStateOf("") }
    var urgensi by remember { mutableStateOf("Biasa") }

    // Cascading Firebase
    var kategoriMesin by remember { mutableStateOf("Mesin") }
    var jenisList by remember { mutableStateOf<List<String>>(emptyList()) }
    var namaMesinList by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedJenis by remember { mutableStateOf("") }
    var selectedNamaMesin by remember { mutableStateOf(machineNameFromQR) }

    var isSubmitting by remember { mutableStateOf(false) }

    // Auto fill dari QR Code / Deep Link
    LaunchedEffect(machineNameFromQR) {
        if (machineNameFromQR.isNotEmpty()) {
            selectedNamaMesin = machineNameFromQR
            Log.d("QRDebug", "🔍 Mencari Nama: '$machineNameFromQR'")

            db.collection("master_mesin")
                .whereEqualTo("Nama", machineNameFromQR)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        val doc = documents.documents[0]
                        val kategoriDariDB = doc.getString("Kategori") ?: "Mesin"
                        val jenisDariDB = doc.getString("Jenis") ?: ""

                        kategoriMesin = kategoriDariDB
                        selectedJenis = jenisDariDB
                        Log.d("QRDebug", "✅ BERHASIL AUTO-FILL!")
                    }
                }
        }
    }

    // Load data dari Firebase
    LaunchedEffect(bagianTujuan) {
        kategoriMesin = if (bagianTujuan == "Bengkel") "Armada" else "Mesin"

        db.collection("master_mesin")
            .whereEqualTo("Kategori", kategoriMesin)
            .get()
            .addOnSuccessListener { documents ->
                jenisList = documents.mapNotNull { it.getString("Jenis") }.distinct().sorted()
            }
    }

    LaunchedEffect(selectedJenis) {
        if (selectedJenis.isNotEmpty()) {
            db.collection("master_mesin")
                .whereEqualTo("Kategori", kategoriMesin)
                .whereEqualTo("Jenis", selectedJenis)
                .get()
                .addOnSuccessListener { documents ->
                    namaMesinList = documents.mapNotNull { it.getString("Nama") }.sorted()
                    Log.d("FirebaseDebug", "Nama Mesin ditemukan: ${namaMesinList.size}")
                }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // PONDASI UTAMA: Menggunakan Background Mesh Animasi Sinkron dengan Dashboard
        SciFiBackground()

        Scaffold(
            containerColor = Color.Transparent, // Wajib transparan penuh agar mesh canvas terlihat
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
                            modifier = Modifier.background(Color.White.copy(alpha = 0.08f), CircleShape)
                        ) { Icon(Icons.Default.ArrowBackIosNew, "Back", tint = MaterialTheme.colorScheme.onSurface) }
                        Spacer(Modifier.width(16.dp))
                        Text(
                            "Buat Order Kerja",
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

                // HEADER INFORMASI PEMBUAT
                ModernSectionHeader("INFORMASI PEMBUAT", Icons.Default.Person)

                // KARTU FORM 1: Gaya Glassmorphism Industri
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = SciFiGlass,
                    border = BorderStroke(1.dp, Brush.verticalGradient(listOf(SciFiBorderLight, Color.Transparent)))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        FormLabel("BAGIAN ORDER")
                        ModernDropdownField(
                            selected = bagianOrder,
                            options = listOf("Bahan Baku","Finishgood","Gudang Part","HRD","Muat","Opt. Crane","Pengecatan","Perakitan","Pipa ERW","PPID","QC","Slitting","Tek. Shift A","Tek. Shift B","Bengkel","Umum","Konstruksi"),
                            label = "Pilih Bagian Order...",
                            onSelected = { bagianOrder = it }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        FormLabel("NAMA PEMBUAT ORDER")
                        ModernTextField(
                            value = namaOrder,
                            onValueChange = { namaOrder = it },
                            placeholder = "Masukkan Nama Anda...",
                            icon = Icons.Default.Edit
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // HEADER TUJUAN PERANGKAT
                ModernSectionHeader("TUJUAN & PERANGKAT", Icons.Default.PrecisionManufacturing)

                // KARTU FORM 2: Glassmorphic Perangkat
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = SciFiGlass,
                    border = BorderStroke(1.dp, Brush.verticalGradient(listOf(SciFiBorderLight, Color.Transparent)))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        FormLabel("BAGIAN TUJUAN")
                        ModernDropdownField(
                            selected = bagianTujuan,
                            options = listOf("Tek. Shift A","Tek. Shift B","Bengkel","Umum","Konstruksi"),
                            label = "Pilih Bagian Tujuan...",
                            onSelected = { bagianTujuan = it }
                        )

                        Spacer(Modifier.height(14.dp))

                        val labelJenisDinamis = if (bagianTujuan == "Bengkel") "JENIS ARMADA" else "JENIS MESIN"
                        FormLabel(labelJenisDinamis)
                        ModernDropdownField(
                            selected = selectedJenis,
                            options = jenisList,
                            label = "Pilih Jenis...",
                            onSelected = { selectedJenis = it }
                        )

                        Spacer(Modifier.height(14.dp))

                        val labelNamaDinamis = if (bagianTujuan == "Bengkel") "NAMA ARMADA" else "NAMA MESIN"
                        FormLabel(labelNamaDinamis)
                        ModernDropdownField(
                            selected = selectedNamaMesin,
                            options = namaMesinList,
                            label = "Pilih Nama...",
                            onSelected = { selectedNamaMesin = it }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // HEADER DETAIL PEKERJAAN
                ModernSectionHeader("DETAIL PEKERJAAN", Icons.AutoMirrored.Filled.Assignment)

                // KARTU FORM 3: Glassmorphic Detail Masalah
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = SciFiGlass,
                    border = BorderStroke(1.dp, Brush.verticalGradient(listOf(SciFiBorderLight, Color.Transparent)))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        FormLabel("JENIS PEKERJAAN")
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Perbaikan","Pemeriksaan","Pemasangan","Pemindahan","Pembuatan","Setting","Kalibrasi").forEach { opt ->
                                ChoiceChip(
                                    text = opt,
                                    selected = jenisPekerjaan == opt,
                                    onClick = { jenisPekerjaan = opt }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        FormLabel("KERUSAKAN / PERMASALAHAN")
                        ModernTextField(
                            value = kerusakan,
                            onValueChange = { kerusakan = it },
                            placeholder = "Jelaskan detail kerusakan...",
                            minLines = 3
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        FormLabel("TINGKAT URGENSI")
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Biasa","Penting","Penting Sekali").forEach { opt ->
                                ChoiceChip(
                                    text = opt,
                                    selected = urgensi == opt,
                                    onClick = { urgensi = opt }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // TOMBOL SUBMIT DENGAN GLOW PULSE EFFECT BERGAYA CORE REACTOR
                val infiniteTransitionPulse = rememberInfiniteTransition(label = "btnGlow")
                val glowBlurFloat by infiniteTransitionPulse.animateFloat(
                    initialValue = 6f,
                    targetValue = 14f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ), label = "btnGlowPulse"
                )

                // Pendaran cahaya di belakang tombol submit
                Box(
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Kalkulasi konversi Float ke Dp secara langsung dan aman
                    val blurRadiusDp = (glowBlurFloat / LocalDensity.current.density).dp

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .fillMaxHeight(0.8f)
                            .background(SciFiCyan.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            .blur(blurRadiusDp) // Sekarang bertipe Dp murni dan aman
                    )

                    Button(
                        onClick = {
                            if (bagianOrder.isEmpty() || bagianTujuan.isEmpty() || selectedNamaMesin.isEmpty() || kerusakan.isEmpty()) {
                                Toast.makeText(context, "Mohon isi semua field wajib!", Toast.LENGTH_LONG).show()
                                return@Button
                            }

                            isSubmitting = true
                            submitBuatOrderKerja(
                                context = context,
                                bagianOrder = bagianOrder,
                                namaOrder = namaOrder,
                                bagianTujuan = bagianTujuan,
                                kategoriMesin = kategoriMesin,
                                jenis = selectedJenis,
                                namaMesin = selectedNamaMesin,
                                jenisPekerjaan = jenisPekerjaan,
                                kerusakan = kerusakan,
                                urgensi = urgensi,
                                onSuccess = {
                                    isSubmitting = false
                                    onSuccess()
                                },
                                onError = {
                                    isSubmitting = false
                                }
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
                                .background(
                                    brush = Brush.horizontalGradient(colors = listOf(SciFiCyan, SciFiBlue)),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .border(1.dp, SciFiBorderMedium, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("SUBMIT ORDER KERJA", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, fontFamily = OrbitronFontFamily)
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

// ==================== SUBMIT FUNCTION ====================
private fun submitBuatOrderKerja(
    context: android.content.Context,
    bagianOrder: String,
    namaOrder: String,
    bagianTujuan: String,
    kategoriMesin: String,
    jenis: String,
    namaMesin: String,
    jenisPekerjaan: String,
    kerusakan: String,
    urgensi: String,
    onSuccess: () -> Unit,
    onError: () -> Unit = {}
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val url = URL("https://script.google.com/macros/s/AKfycbzbmKFheI55ccsJ_kLdOzy6VIdGpgKIy2s9pljrIM8sNbgJ_RLywnzF-Q2sJTslVQU/exec")

            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")

            val json = JSONObject().apply {
                put("action", "create")
                put("bagianOrder", bagianOrder)
                put("namaOrder", namaOrder)
                put("kategoriMesin", kategoriMesin)
                put("bagianTujuan", bagianTujuan)
                put("jenis", jenis)
                put("namaMesin", namaMesin)
                put("jenisPekerjaan", jenisPekerjaan)
                put("kerusakan", kerusakan)
                put("urgensi", urgensi)
            }

            conn.outputStream.use { it.write(json.toString().toByteArray()) }

            val responseCode = conn.responseCode

            withContext(Dispatchers.Main) {
                if (responseCode in 200..299) {
                    Toast.makeText(context, "✅ Order Kerja Berhasil Dikirim!", Toast.LENGTH_LONG).show()
                    onSuccess()
                } else {
                    Toast.makeText(context, "❌ Gagal: Code $responseCode", Toast.LENGTH_LONG).show()
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
