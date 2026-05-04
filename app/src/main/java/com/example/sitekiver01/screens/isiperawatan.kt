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
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import java.util.Locale

// --- DATA MODEL ---
data class MasterRawat(
    val kategori: String,
    val jenis: String,
    val nama: String,
    val waktu: String,
    val item: String,
    val label: String
)

// --- VIEWMODEL ---
class PerawatanViewModel : ViewModel() {
    var allData = mutableStateListOf<MasterRawat>()
    var categories = mutableStateListOf<String>()
    var filteredJenis = mutableStateListOf<String>()
    var filteredNama = mutableStateListOf<String>()
    var dynamicItems = mutableStateListOf<MasterRawat>()

    var isLoading by mutableStateOf(false)


    fun fetchData(onComplete: () -> Unit = {}) {
        isLoading = true

        viewModelScope.launch(Dispatchers.IO) {   // ← Ganti GlobalScope
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
                    Log.e("PerawatanVM", "Error fetch: ${e.message}")
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
        dynamicItems.addAll(allData.filter { it.nama.trim().equals(nama.trim(), ignoreCase = true) && it.waktu == w })
    }
}

// --- UI SCREEN ---
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
    val scope = rememberCoroutineScope()
    var isSubmitting by remember { mutableStateOf(false) }
    // Map status "M" -> "Mingguan" or "B" -> "Bulanan"
    val mappedWaktu = remember(initialWaktu) {
        when (initialWaktu) {
            "M" -> "Mingguan"
            "B" -> "Bulanan"
            else -> initialWaktu
        }
    }

    // Form State
    var tanggal by remember { mutableStateOf(initialDate) }
    var selectedKategori by remember { mutableStateOf("") }
    var selectedJenis by remember { mutableStateOf("") }
    var selectedNama by remember { mutableStateOf(initialMachine) }
    var selectedWaktu by remember { mutableStateOf(mappedWaktu) }
    var keterangan by remember { mutableStateOf("") }
    val isianMap = remember { mutableStateMapOf<String, String>() }

    // Load Data and Auto-populate
    LaunchedEffect(Unit) { 
        vm.fetchData {
            if (initialMachine.isNotEmpty()) {
                val machineData = vm.allData.find { it.nama.trim().equals(initialMachine.trim(), ignoreCase = true) }
                if (machineData != null) {
                    selectedKategori = machineData.kategori
                    vm.filterJenis(selectedKategori)
                    selectedJenis = machineData.jenis
                    vm.filterNama(selectedJenis)
                    selectedNama = machineData.nama // use master data name
                    
                    if (selectedWaktu.isNotEmpty()) {
                        vm.loadDynamicItems(selectedNama, selectedWaktu)
                    }
                }
            }
        } 
    }

    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(context, { _, y, m, d ->
        tanggal = String.format(Locale.US, "%02d/%02d/%d", d, m + 1, y)
    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))

    Scaffold(
        containerColor = Color(0xFF011619),
        topBar = {
            TopAppBar(
                title = { Text("ISI PERAWATAN", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back", tint = Color.White)
                    }
                },
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
            // --- SECTION 1: TANGGAL & DATA ---
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("TANGGAL DAN DATA", color = Color(0xFF00E5FF), fontWeight = FontWeight.SemiBold)

                    // Input Tanggal
                    OutlinedButton(
                        onClick = { datePickerDialog.show() },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color.Cyan)
                        Spacer(Modifier.width(8.dp))
                        Text(if (tanggal.isEmpty()) "Pilih Tanggal" else tanggal, color = Color.White)
                    }

                    // Dropdown Kategori
                    MyDropdown("Kategori", vm.categories, selectedKategori) {
                        selectedKategori = it
                        vm.filterJenis(it)
                        selectedJenis = ""; selectedNama = ""
                    }

                    // Dropdown Jenis
                    MyDropdown("Jenis", vm.filteredJenis, selectedJenis) {
                        selectedJenis = it
                        vm.filterNama(it)
                        selectedNama = ""
                    }

                    // Dropdown Nama Mesin
                    MyDropdown("Nama Mesin", vm.filteredNama, selectedNama) {
                        selectedNama = it
                        if (selectedWaktu.isNotEmpty()) vm.loadDynamicItems(it, selectedWaktu)
                    }

                    // Dropdown Waktu
                    MyDropdown("Waktu", listOf("Mingguan", "Bulanan"), selectedWaktu) {
                        selectedWaktu = it
                        if (selectedNama.isNotEmpty()) vm.loadDynamicItems(selectedNama, it)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- SECTION 2: YANG DIRAWAT (DINAMIS) ---
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
                            label = { Text("Keterangan", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }
                }
            }

            // --- SUBMIT BUTTON ---
            Button(
                onClick = {
                    if (tanggal.isEmpty() || selectedNama.isEmpty()) {
                        Toast.makeText(context, "Data belum lengkap!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isSubmitting = true  // Pastikan ada state ini

                    submitData(
                        ctx = context,
                        onSuccess = {
                            if (navController != null) {
                                navController.popBackStack()
                            } else {
                                onBack()
                            }
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BCD4)),
                shape = RoundedCornerShape(12.dp),
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        "SUBMIT DATA",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
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
            conn.setRequestProperty("Accept", "application/json")

            conn.outputStream.use { os ->
                os.write(jsonObject.toString().toByteArray(Charsets.UTF_8))
            }

            val responseCode = conn.responseCode
            val responseBody = try {
                conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } catch (e: Exception) {
                conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            }

            withContext(kotlinx.coroutines.Dispatchers.Main) {
                if (responseCode in 200..299) {
                    Toast.makeText(ctx, "✅ Data Berhasil Disimpan!", Toast.LENGTH_LONG).show()

                    // KEMBALI KE HALAMAN SEBELUMNYA
                    onSuccess()

                } else {
                    Toast.makeText(ctx, "❌ Gagal: $responseBody", Toast.LENGTH_LONG).show()
                }
            }

        } catch (e: Exception) {
            withContext(kotlinx.coroutines.Dispatchers.Main) {
                Toast.makeText(ctx, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
