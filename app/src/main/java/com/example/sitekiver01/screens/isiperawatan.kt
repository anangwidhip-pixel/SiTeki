package com.example.sitekiver01.screens

import android.app.DatePickerDialog
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.sitekiver01.components.GlassCard
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import androidx.compose.ui.text.style.TextAlign

data class MasterRawat(
    val kategori: String,
    val jenis: String,
    val nama: String,
    val waktu: String,
    val item: String,
    val label: String
)

data class FirebaseMesin(
    val nama: String = "",
    val kategori: String = "",
    val jenis: String = ""      // Armada
)

class PerawatanViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    var allData = mutableStateListOf<MasterRawat>()
    var categories = mutableStateListOf<String>()
    var filteredJenis = mutableStateListOf<String>()
    var filteredNama = mutableStateListOf<String>()
    var dynamicItems = mutableStateListOf<MasterRawat>()

    var isLoading by mutableStateOf(false)

    // ==================== FIREBASE LOOKUP (Cepat) ====================
    fun fetchFromFirebase(namaMesin: String, onResult: (FirebaseMesin?) -> Unit) {
        if (namaMesin.isBlank()) {
            onResult(null)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val snapshot = db.collection("master_mesin")
                    .whereEqualTo("nama", namaMesin)
                    .get()
                    .await()

                val mesin = if (snapshot.documents.isNotEmpty()) {
                    snapshot.documents[0].toObject(FirebaseMesin::class.java)
                } else null

                withContext(Dispatchers.Main) {
                    onResult(mesin)
                }
            } catch (e: Exception) {
                Log.e("FirebaseLookup", "Error: ${e.message}")
                withContext(Dispatchers.Main) { onResult(null) }
            }
        }
    }

    // ==================== APPS SCRIPT (Fungsi Lama) ====================
    fun fetchData(onComplete: () -> Unit = {}) {
        isLoading = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = URL("https://script.google.com/macros/s/AKfycbwSnaaYVxXWVngeGQYU2im2G5FQ6L7WstjTkx7IW3jVYcuELECt0_cyvM0cFx4Uf8U/exec?action=getRawatMaster")
                val conn = url.openConnection() as HttpURLConnection
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(text)

                allData.clear()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val nameValue = if (obj.has("nama_mesin")) obj.getString("nama_mesin") else obj.optString("nama")

                    allData.add(MasterRawat(
                        obj.optString("kategori"),
                        obj.optString("jenis"),
                        nameValue,
                        obj.optString("waktu"),
                        obj.optString("item"),
                        obj.optString("label")
                    ))
                }

                categories.clear()
                categories.addAll(allData.map { it.kategori }.distinct().sorted())

                withContext(Dispatchers.Main) {
                    isLoading = false
                    onComplete()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    Log.e("PerawatanVM", "Error: ${e.message}")
                }
            }
        }
    }

    fun filterJenis(kat: String) {
        filteredJenis.clear()
        filteredJenis.addAll(allData.filter { it.kategori == kat }.map { it.jenis }.distinct().sorted())
    }

    fun filterNama(jen: String) {
        filteredNama.clear()
        filteredNama.addAll(allData.filter { it.jenis == jen }.map { it.nama }.distinct().sorted())
    }

    fun loadDynamicItems(nama: String, waktu: String) {
        val w = if (waktu == "Mingguan") "M" else "B"
        dynamicItems.clear()
        val result = allData.filter {
            it.nama.trim().equals(nama.trim(), ignoreCase = true) && it.waktu == w
        }
        dynamicItems.addAll(result)

        Log.d("DynamicItems", "Loaded ${result.size} items for $nama ($waktu)")
    }
}

// ==================== SCREEN ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IsiPerawatanScreen(
    navController: NavController? = null,
    initialMachine: String = "",
    initialDate: String = "",
    initialWaktu: String = "",
    onBack: () -> Unit = {},
    vm: PerawatanViewModel = viewModel()
) {
    val context = LocalContext.current
    var isSubmitting by remember { mutableStateOf(false) }

    val mappedWaktu = remember(initialWaktu) {
        when (initialWaktu) {
            "M" -> "Mingguan"
            "B" -> "Bulanan"
            else -> initialWaktu
        }
    }

    var tanggal by remember { mutableStateOf(initialDate) }
    var selectedKategori by remember { mutableStateOf("") }
    var selectedJenis by remember { mutableStateOf("") }
    var selectedNama by remember { mutableStateOf(initialMachine) }
    var selectedWaktu by remember { mutableStateOf(mappedWaktu) }
    var keterangan by remember { mutableStateOf("") }
    val isianMap = remember { mutableStateMapOf<String, String>() }

    // ==================== AUTO FILL DARI FIREBASE ====================
    LaunchedEffect(initialMachine) {
        if (initialMachine.isNotEmpty()) {
            vm.fetchFromFirebase(initialMachine) { firebaseMesin ->
                firebaseMesin?.let {
                    selectedKategori = it.kategori
                    selectedJenis = it.jenis
                    selectedNama = it.nama
                }
            }
        }
    }

    LaunchedEffect(selectedNama, selectedWaktu) {
        if (selectedNama.isNotEmpty() && selectedWaktu.isNotEmpty()) {
            vm.loadDynamicItems(selectedNama, selectedWaktu)
        }
    }
    // Load full master data (tetap diperlukan untuk dropdown & dynamic items)
    LaunchedEffect(Unit) {
        vm.fetchData {
            // Setelah data master selesai load
            if (initialMachine.isNotEmpty()) {
                val machineData = vm.allData.find {
                    it.nama.trim().equals(initialMachine.trim(), ignoreCase = true)
                }
                machineData?.let {
                    selectedKategori = it.kategori
                    vm.filterJenis(it.kategori)
                    selectedJenis = it.jenis
                    vm.filterNama(it.jenis)
                    selectedNama = it.nama
                }
            }
        }
    }
    if (vm.isLoading) {
        Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) {
            CircularProgressIndicator(color = Color.Cyan)
        }
    } else if (vm.dynamicItems.isNotEmpty()) {
        // tampilan item dynamic seperti biasa
    } else if (selectedNama.isNotEmpty() && selectedWaktu.isNotEmpty()) {
        Text("Tidak ada item perawatan untuk mesin ini", color = Color.Gray)
    }
    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, y, m, d -> tanggal = String.format(Locale.US, "%02d/%02d/%d", d, m + 1, y) },
        calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
    )

    Scaffold(
        containerColor = Color(0xFF011619),
        topBar = {
            TopAppBar(
                title = { Text("ISI PERAWATAN", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBackIosNew, null, tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("TANGGAL DAN DATA", color = Color(0xFF00E5FF), fontWeight = FontWeight.SemiBold)

                    OutlinedButton(onClick = { datePickerDialog.show() }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Icon(Icons.Default.CalendarMonth, null, tint = Color.Cyan)
                        Spacer(Modifier.width(8.dp))
                        Text(if (tanggal.isEmpty()) "Pilih Tanggal" else tanggal, color = Color.White)
                    }

                    MyDropdown("Kategori", vm.categories, selectedKategori) {
                        selectedKategori = it; vm.filterJenis(it); selectedJenis = ""; selectedNama = ""
                    }

                    MyDropdown("Jenis", vm.filteredJenis, selectedJenis) {
                        selectedJenis = it; vm.filterNama(it); selectedNama = ""
                    }

                    MyDropdown("Nama Mesin", vm.filteredNama, selectedNama) {
                        selectedNama = it
                        if (selectedWaktu.isNotEmpty()) vm.loadDynamicItems(it, selectedWaktu)
                    }

                    MyDropdown("Waktu", listOf("Mingguan", "Bulanan"), selectedWaktu) {
                        selectedWaktu = it
                        if (selectedNama.isNotEmpty()) vm.loadDynamicItems(selectedNama, it)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (vm.dynamicItems.isNotEmpty()) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("YANG DIRAWAT", color = Color(0xFF00E5FF), fontWeight = FontWeight.SemiBold)

                        vm.dynamicItems.forEach { item ->
                            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                Text(item.label, color = Color.White, fontSize = 14.sp)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    listOf("Bagus", "Perbaikan", "T.A").forEach { opsi ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            RadioButton(
                                                selected = (isianMap[item.item] == opsi),
                                                onClick = { isianMap[item.item] = opsi },
                                                colors = RadioButtonDefaults.colors(selectedColor = Color.Cyan)
                                            )
                                            Text(opsi, color = Color.LightGray, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = keterangan,
                            onValueChange = { keterangan = it },
                            label = { Text("Keterangan") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }
                }
            } else if (selectedNama.isNotEmpty() && selectedWaktu.isNotEmpty() && !vm.isLoading) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier.padding(32.dp).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Tidak ada item perawatan untuk mesin ini",
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // --- SUBMIT BUTTON ---
            Button(
                onClick = {
                    if (tanggal.isEmpty() || selectedNama.isEmpty() || selectedKategori.isEmpty()) {
                        Toast.makeText(context, "Mohon lengkapi Tanggal, Kategori, dan Nama Mesin!", Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    isSubmitting = true

                    submitData(
                        ctx = context,
                        onSuccess = {
                            // Reset Form
                            tanggal = ""
                            selectedKategori = ""
                            selectedJenis = ""
                            selectedNama = ""
                            selectedWaktu = ""
                            keterangan = ""
                            isianMap.clear()

                            // Kembali ke JadPerawatan
                            onBack()           // ← Ini yang paling penting
                            isSubmitting = false
                        },
                        onError = {
                            isSubmitting = false
                        },
                        tgl = tanggal,
                        kat = selectedKategori,
                        jen = selectedJenis,
                        nama = selectedNama,
                        waktu = selectedWaktu,
                        dataItem = isianMap,
                        ket = keterangan
                    )
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BCD4)),
                shape = RoundedCornerShape(12.dp),
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("SUBMIT DATA", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun MyDropdown(label: String, items: List<String>, selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.padding(vertical = 4.dp)) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            label = { Text(label, color = Color.Gray) },
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, tint = Color.White) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )
        Box(modifier = Modifier.matchParentSize().clickable { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { text ->
                DropdownMenuItem(text = { Text(text) }, onClick = {
                    onSelect(text)
                    expanded = false
                })
            }
        }
    }
}

fun submitData(
    ctx: android.content.Context,
    onSuccess: () -> Unit,
    onError: (() -> Unit)? = null,
    tgl: String,
    kat: String,
    jen: String,
    nama: String,
    waktu: String,
    dataItem: Map<String, String>,
    ket: String
) {
    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
        try {
            val urlTarget = "https://script.google.com/macros/s/AKfycbwQ7ocBNsl4x5-rGLrSyvkyluhSRl3B_LvmkA3cFuvuL9pBbVAOUI3i_Vu6jwfkfOA/exec"

            val jsonObject = JSONObject().apply {
                put("tanggal", tgl)
                put("kategori", kat)
                put("jenis", jen)
                put("nama_mesin", nama)
                put("waktu", if (waktu == "Mingguan") "M" else "B")
                put("keterangan", ket)

                dataItem.forEach { (key, value) ->
                    put(key, value)
                }
            }

            val url = URL(urlTarget)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")

            conn.outputStream.use { os ->
                os.write(jsonObject.toString().toByteArray(Charsets.UTF_8))
            }

            val responseCode = conn.responseCode

            withContext(kotlinx.coroutines.Dispatchers.Main) {
                if (responseCode in 200..299) {
                    onSuccess()
                } else {
                    Toast.makeText(ctx, "❌ Gagal menyimpan data", Toast.LENGTH_LONG).show()
                    onError?.invoke()
                }
            }

        } catch (e: Exception) {
            withContext(kotlinx.coroutines.Dispatchers.Main) {
                Toast.makeText(ctx, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                onError?.invoke()
            }
        }
    }
}
