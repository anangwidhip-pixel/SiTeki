package com.example.sitekiver01.screens

import android.widget.Toast
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sitekiver01.DataTravo
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

@Composable
fun IsiInspeksiScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val webAppUrl = "https://script.google.com/macros/s/AKfycbyX0U2MaTrjBTZjLkTH64E3bIXg2lyHhtPdTJ1QbEFco34m3FK18gDDE0Lqk7ja-k-C/exec"

    var listDataTravo by remember { mutableStateOf<List<DataTravo>>(emptyList()) }
    var listLokasi by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // State Form
    var selectedKode by remember { mutableStateOf("") }
    var selectedLokasi by remember { mutableStateOf("") }
    var kondisi by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var stang by remember { mutableStateOf("Ada") }
    var kabel by remember { mutableStateOf("Ada") }
    var masa by remember { mutableStateOf("Ada") }
    var keterangan by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    val namaTravoOtomatis = listDataTravo.find { it.kode == selectedKode }?.nama ?: "-"

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val resTravo = URL("$webAppUrl?action=getDataTravo").readText()
                val arrTravo = JSONArray(resTravo)
                val tempTravo = mutableListOf<DataTravo>()
                for (i in 0 until arrTravo.length()) {
                    val obj = arrTravo.getJSONObject(i)
                    tempTravo.add(DataTravo(obj.optString("kode"), obj.optString("nama"), "", "", "", ""))
                }
                listDataTravo = tempTravo

                val resRef = URL("$webAppUrl?action=getReferensi").readText()
                val objRef = JSONObject(resRef)
                val arrLokasi = objRef.getJSONArray("lokasi")
                listLokasi = List(arrLokasi.length()) { arrLokasi.getString(it) }
            } catch (e: Exception) { e.printStackTrace() }
            finally { isLoading = false }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Row(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars).fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.background(Color.White.copy(0.1f), CircleShape)) {
                    Icon(Icons.Default.ArrowBackIosNew, "Back", tint = Color.White)
                }
                Spacer(Modifier.width(16.dp))
                Text("INSPEKSI TRAVO", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, fontFamily = OrbitronFontFamily)
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = GlassAccentCyan) }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item { ModernSectionHeader("FORMULIR INSPEKSI", Icons.Default.Checklist) }

                item {
                    ModernFormCard {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // 1. KODE
                            ModernDropdownField(
                                selected = selectedKode,
                                options = listDataTravo.map { it.kode },
                                label = "Pilih Kode Travo",
                                onSelected = { selectedKode = it }
                            )

                            // 2. NAMA (Otomatis menggunakan ModernSearchField agar desain kotak & font serasi)
                            ModernSearchField(
                                if (selectedKode.isEmpty()) "" else namaTravoOtomatis, // Menghapus string "Nama: "
                                "Nama Travo",                                         // Placeholder / Hint saat kosong
                                { /* Kosongkan karena hanya untuk tampil data read-only */ },
                                false                                                 // isSearchable = false
                            )

                            // 3. LOKASI
                            ModernDropdownField(
                                selected = selectedLokasi,
                                options = listLokasi,
                                label = "Pilih Lokasi",
                                onSelected = { selectedLokasi = it }
                            )

                            // 4. KONDISI
                            ModernDropdownField(
                                selected = kondisi,
                                options = listOf("Bagus", "Rusak", "N/A"),
                                label = "Kondisi",
                                onSelected = { kondisi = it }
                            )

// 5. STATUS
                            ModernDropdownField(
                                selected = status,
                                options = listOf("Digunakan", "Standby", "Rusak", "Servis"),
                                label = "Status",
                                onSelected = { status = it }
                            )

                            Divider(color = GlassBorder)

                            Text("KELENGKAPAN AKSESORIS", fontSize = 10.sp, color = GlassAccentCyan, fontWeight = FontWeight.Bold)

                            // Pastikan RowOption didefinisikan di bawah file ini
                            RowOption("Stang Las", stang) { stang = it }
                            RowOption("Kabel Las", kabel) { kabel = it }
                            RowOption("Masa Las", masa) { masa = it }

                            ModernTextField(keterangan, { keterangan = it }, "Keterangan Tambahan")
                        }
                    }
                }

                item {
                    Button(
                        onClick = {
                            isSending = true
                            scope.launch {
                                // Panggil fungsi kirim di sini
                                val success = kirimInspeksi(webAppUrl, selectedKode, namaTravoOtomatis, selectedLokasi, kondisi, status, stang, kabel, masa, keterangan)
                                isSending = false
                                if (success) {
                                    Toast.makeText(context, "Data Terkirim", Toast.LENGTH_SHORT).show()
                                    onBack()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GlassAccentCyan)
                    ) {
                        Text(if (isSending) "MENGIRIM..." else "SUBMIT INSPEKSI", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RowOption(label: String, current: String, onSelect: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
        listOf("Ada", "Tidak", "Rusak").forEach { option ->
            FilterChip(
                selected = current == option,
                onClick = { onSelect(option) },
                label = { Text(option, fontSize = 10.sp) },
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}
suspend fun kirimInspeksi(
    url: String,
    kode: String,
    nama: String,
    lokasi: String,
    kondisi: String,
    status: String,
    stang: String,
    kabel: String,
    masa: String,
    ket: String
): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true

            // Membuat JSON payload
            val json = JSONObject().apply {
                put("action", "simpanInspeksi")
                put("kode", kode)
                put("nama", nama)
                put("lokasi", lokasi)
                put("kondisi", kondisi)
                put("status", status)
                put("stang", stang)
                put("kabel", kabel)
                put("masa", masa)
                put("keterangan", ket)
            }

            // Kirim data
            conn.outputStream.use { it.write(json.toString().toByteArray()) }

            // Cek respon
            val response = conn.inputStream.bufferedReader().readText()
            return@withContext response.contains("Success")
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }
}