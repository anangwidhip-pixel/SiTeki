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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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

    val sdfDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())

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

    val SCRIPT_URL = "https://script.google.com/macros/s/AKfycbyEz9WL-SsHlsl8nROHs28HVijqVu_UptKDuLMBI3z2mo6pDH_vIxOVM9B_fjmNow/exec"
    val statusOptions = listOf("Repair", "Breakdown", "Tunggu Part", "Overhoul Mesin", "Yang Lain")

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
                            "PENYELESAIAN ORDER",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            fontFamily = OrbitronFontFamily
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(12.dp))

                ModernSectionHeader("INFORMASI MESIN", Icons.Default.PrecisionManufacturing)
                ModernFormCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Settings, null, tint = GlassAccentCyan, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("NAMA MESIN", fontSize = 10.sp, color = GlassTextMuted)
                            Text(namaMesin, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }

                ModernSectionHeader("DETAIL PERBAIKAN", Icons.Default.Engineering)
                ModernFormCard {
                    Column {
                        FormLabel("PERBAIKAN YANG DILAKUKAN")
                        ModernTextField(perbaikanDilakukan, "Uraikan perbaikan...", minLines = 3) { perbaikanDilakukan = it }

                        Spacer(Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(Modifier.weight(1f)) {
                                FormLabel("MULAI")
                                ModernDatePickerField(tglMulai) { tglMulai = it }
                                ModernTimePickerField(waktuMulai) { waktuMulai = it }
                            }
                            Column(Modifier.weight(1f)) {
                                FormLabel("SELESAI")
                                ModernDatePickerField(tglSelesai) { tglSelesai = it }
                                ModernTimePickerField(waktuSelesai) { waktuSelesai = it }
                            }
                        }
                    }
                }

                ModernSectionHeader("HASIL & SPAREPART", Icons.Default.Inventory)
                ModernFormCard {
                    Column {
                        FormLabel("STATUS MESIN")
                        ModernDropdownField(statusMesin, statusOptions, "Pilih Status...") { statusMesin = it }

                        FormLabel("SPAREPART DIGUNAKAN")
                        SparePartSelectorModern(
                            sparePart = sparePart,
                            ukuran = ukuranSparePart,
                            onSparePartChange = { sparePart = it },
                            onUkuranChange = { ukuranSparePart = it }
                        )

                        FormLabel("NILAI PERBAIKAN")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Bagus", "Cukup", "Tidak Bagus").forEach {
                                ChoiceChip(it, nilaiPerbaikan == it) { nilaiPerbaikan = it }
                            }
                        }
                    }
                }

                ModernSectionHeader("KETERANGAN", Icons.AutoMirrored.Filled.Assignment)
                ModernFormCard {
                    Column {
                        FormLabel("CATATAN TAMBAHAN")
                        ModernTextField(keterangan, "Keterangan tambahan...", minLines = 3) { keterangan = it }
                    }
                }

                Spacer(Modifier.height(24.dp))

                ModernButton(
                    text = "SELESAIKAN & TUTUP ORDER",
                    isLoading = isSubmitting,
                    icon = Icons.Default.CloudUpload,
                    onClick = {
                        if (perbaikanDilakukan.isEmpty() || statusMesin.isEmpty()) {
                            Toast.makeText(context, "Perbaikan dan Status Mesin wajib diisi!", Toast.LENGTH_LONG).show()
                            return@ModernButton
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
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(50.dp))
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
            placeholder = "Pilih Sparepart..."
        ) {
            isManualMode = false
            showDialog = true
        }

        if (isManualMode || sparePart != "Tidak Pakai") {
            ModernTextField(
                value = ukuran,
                placeholder = "Ukuran...",
                onValueChange = { onUkuranChange(it) }
            )
        }
    }

    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth(0.95f),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF1A1A1A),
                border = BorderStroke(1.dp, GlassBorder)
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
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
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
                            colors = ButtonDefaults.buttonColors(containerColor = GlassAccentCyan),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("MANUAL", fontSize = 11.sp, color = Color.Black) }
                    }

                    Spacer(Modifier.height(16.dp))

                    if (isLoading) {
                        CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally), color = GlassAccentCyan)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            FormLabel("KATEGORI")
                            ModernDropdownField(selectedKategori, kategoriList, "Pilih Kategori...") { selectedKategori = it }
                            
                            if (namaList.isNotEmpty()) {
                                FormLabel("NAMA PART")
                                ModernDropdownField(selectedNama, namaList, "Pilih Nama...") { 
                                    selectedNama = it
                                    onSparePartChange(it)
                                }
                            }
                            
                            if (ukuranList.isNotEmpty()) {
                                FormLabel("UKURAN")
                                ModernDropdownField(ukuran, ukuranList, "Pilih Ukuran...") { onUkuranChange(it) }
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(24.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showDialog = false }) {
                            Text("TUTUP", color = GlassTextMuted)
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
