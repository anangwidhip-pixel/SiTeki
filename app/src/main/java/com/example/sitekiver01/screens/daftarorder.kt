package com.example.sitekiver01.screens

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DaftarOrderScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Masukkan URL Deployment Terbaru Anda di sini
    val scriptUrl = "https://script.google.com/macros/s/AKfycbxJOKT1yM71bQr1PbJSJ7X6q-RdJ1nmUpjvutRzkBvIYuPbZM2cGh3NuQ0X62GCJkVd/exec"

    var dataList by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    var selectedOrder by remember { mutableStateOf<JSONObject?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    val fetchData = suspend {
        isLoading = true
        withContext(Dispatchers.IO) {
            try {
                val response = URL("$scriptUrl?action=getDaftarBon").readText()
                val jsonArray = JSONArray(response)
                val list = mutableListOf<JSONObject>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    if (obj.optString("status", "").lowercase() == "open") {
                        list.add(obj)
                    }
                }
                dataList = list
            } catch (e: Exception) { e.printStackTrace() }
            finally { isLoading = false }
        }
    }

    LaunchedEffect(Unit) { fetchData() }

    val filteredData = remember(dataList, searchQuery) {
        dataList.filter {
            val nama = it.optString("nama", "").lowercase()
            val ukuran = it.optString("ukuran", "").lowercase()
            nama.contains(searchQuery.lowercase()) || ukuran.contains(searchQuery.lowercase())
        }
    }

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
                        ) { Icon(Icons.Default.ArrowBackIosNew, "Back", tint = Color.White) }
                        Spacer(Modifier.width(16.dp))
                        Text(
                            "DAFTAR BON PESANAN",
                            color = Color.White,
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
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(Modifier.height(12.dp))
                ModernSectionHeader("PENCARIAN DATA BON", Icons.Default.Search)

                // PANEL PENCARIAN (GLASSMORPHIC)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = SciFiGlass,
                    border = BorderStroke(1.dp, Brush.verticalGradient(listOf(SciFiBorderLight, Color.Transparent)))
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        ModernTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = "Cari Nama atau Ukuran...",
                            icon = Icons.Default.Search
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))
                ModernSectionHeader("DATA REKAMAN BON OPEN", Icons.AutoMirrored.Filled.ListAlt)

                // TABEL DATA UTAMA (GLASSMORPHIC)
                Surface(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = SciFiGlass,
                    border = BorderStroke(1.dp, Brush.verticalGradient(listOf(SciFiBorderLight, Color.Transparent)))
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = SciFiCyan)
                        } else if (filteredData.isEmpty()) {
                            Text("Tidak ada bon open", modifier = Modifier.align(Alignment.Center), color = SciFiTextMuted, fontSize = 14.sp)
                        } else {
                            val horizontalScrollState = rememberScrollState()
                            Box(modifier = Modifier.horizontalScroll(horizontalScrollState)) {
                                Column(modifier = Modifier.width(550.dp)) {
                                    Row(
                                        modifier = Modifier
                                            .background(SciFiCyan.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                            .padding(vertical = 10.dp)
                                    ) {
                                        HeaderCellOrder("Tgl Pesan", 110.dp)
                                        HeaderCellOrder("Nama", 200.dp)
                                        HeaderCellOrder("Ukuran", 140.dp)
                                        HeaderCellOrder("Jml Pesan", 100.dp)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                                        itemsIndexed(filteredData) { index, item ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth()
                                                    .clickable { selectedOrder = item; showUpdateDialog = true }
                                                    .background(if (index % 2 == 0) Color.Transparent else Color.White.copy(alpha = 0.01f)),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                DataCellOrder(item.optString("tanggal"), 110.dp)
                                                DataCellOrder(item.optString("nama"), 200.dp, TextAlign.Start)
                                                DataCellOrder(item.optString("ukuran"), 140.dp, TextAlign.Start)
                                                DataCellOrder("${item.optString("jumlah")}", 100.dp)
                                            }
                                            HorizontalDivider(color = SciFiBorderLight.copy(alpha = 0.3f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (showUpdateDialog && selectedOrder != null) {
        UpdateOrderDialog(
            order = selectedOrder!!,
            onDismiss = { showUpdateDialog = false },
            onUpdate = { tglDatang, jmlDatang ->
                scope.launch {
                    val updateData = JSONObject().apply {
                        put("action", "updateArrival")
                        put("no", selectedOrder!!.optString("no"))
                        put("kategori", selectedOrder!!.optString("kategori"))
                        put("nama", selectedOrder!!.optString("nama"))
                        put("ukuran", selectedOrder!!.optString("ukuran"))
                        put("tglDatang", tglDatang)
                        put("jmlDatang", jmlDatang)
                        put("status", "Close")
                    }
                    val success = submitToDatabaseOrder(updateData, scriptUrl)
                    if (success) {
                        Toast.makeText(context, "Data Terupdate & Stok Bertambah!", Toast.LENGTH_SHORT).show()
                        showUpdateDialog = false
                        fetchData()
                    } else {
                        Toast.makeText(context, "Gagal Update", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

@Composable
fun UpdateOrderDialog(
    order: JSONObject,
    onDismiss: () -> Unit,
    onUpdate: (tglDatang: String, jmlDatang: String) -> Unit
) {
    // PERBAIKAN LOCALE WARNING: Membungkus Formatter ke dalam remember
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    var tglDatang by remember { mutableStateOf(sdf.format(Date())) }
    var jmlDatang by remember { mutableStateOf(order.optString("jumlah")) }
    var isUpdating by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A), // SciFiBrandCard Dark Panel
            border = BorderStroke(1.dp, SciFiBorderMedium)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("DETAIL & UPDATE KEDATANGAN", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Color.White, fontFamily = OrbitronFontFamily)
                Spacer(Modifier.height(20.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailRowOrder("Nama Part", order.optString("nama"))
                    DetailRowOrder("Ukuran", order.optString("ukuran"))
                    DetailRowOrder("Jml Pesan", "${order.optString("jumlah")} unit")
                    DetailRowOrder("Tgl Pesan", order.optString("tanggal"))
                    DetailRowOrder("Mesin", order.optString("mesin"))
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = SciFiBorderLight)

                FormLabel("TANGGAL DATANG")
                ModernDatePickerField(tglDatang) { tglDatang = it }

                Spacer(Modifier.height(14.dp))

                FormLabel("JUMLAH DATANG")
                ModernTextField(
                    value = jmlDatang,
                    onValueChange = { jmlDatang = it }, // PERBAIKAN: Parameter diurutkan secara benar
                    placeholder = "Jumlah...",
                    icon = Icons.Default.Edit
                )

                Spacer(Modifier.height(28.dp))

                ModernButton(
                    text = "UPDATE & CLOSE BON",
                    isLoading = isUpdating,
                    onClick = {
                        isUpdating = true
                        onUpdate(tglDatang, jmlDatang)
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
fun DetailRowOrder(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, modifier = Modifier.weight(1f), fontSize = 13.sp, color = SciFiTextMuted)
        Text(value, modifier = Modifier.weight(1.5f), fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, color = Color.White)
    }
}

@Composable
fun HeaderCellOrder(text: String, width: Dp) {
    Text(
        text = text,
        modifier = Modifier.width(width).padding(horizontal = 4.dp),
        color = SciFiCyan,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        textAlign = TextAlign.Center,
        fontFamily = OrbitronFontFamily
    )
}

@Composable
fun DataCellOrder(text: String, width: Dp, textAlign: TextAlign = TextAlign.Center) {
    Text(
        text = text.ifEmpty { "-" },
        modifier = Modifier.width(width).padding(vertical = 12.dp, horizontal = 8.dp),
        fontSize = 11.sp,
        color = Color.White,
        textAlign = textAlign,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

suspend fun submitToDatabaseOrder(data: JSONObject, scriptUrl: String): Boolean = withContext(Dispatchers.IO) {
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
