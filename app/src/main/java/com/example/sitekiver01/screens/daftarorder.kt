package com.example.sitekiver01.screens

import android.app.DatePickerDialog
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.sitekiver01.components.ModernFormCard
import com.example.sitekiver01.components.ModernSectionHeader
import com.example.sitekiver01.ui.theme.RajabesiDarkNavy
import com.example.sitekiver01.ui.theme.SiTekiVer01Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

// ... (Bagian import tetap sama seperti kode Anda sebelumnya)

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

    Scaffold(
        topBar = {
            Surface(
                color = RajabesiDarkNavy,
                shadowElevation = 8.dp,
                modifier = Modifier.clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            ) {
                Row(
                    modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
                        .fillMaxWidth().height(72.dp).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
                    Spacer(Modifier.width(8.dp))
                    Text("DAFTAR BON PESANAN", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding())
                .background(brush = Brush.verticalGradient(colors = listOf(Color(0xFFF8FAFC), Color(0xFFF1F5F9))))
                .padding(16.dp)
        ) {
            ModernSectionHeader("PENCARIAN BON", Icons.Default.Search)
            ModernFormCard {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari Nama atau Ukuran...", fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RajabesiDarkNavy, unfocusedBorderColor = Color(0xFFE2E8F0)),
                    singleLine = true
                )
            }
            Spacer(Modifier.height(24.dp))
            ModernSectionHeader("DATA BON OPEN", Icons.AutoMirrored.Filled.ListAlt)
            Spacer(Modifier.height(8.dp))

            ModernFormCard(modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = RajabesiDarkNavy)
                    } else if (filteredData.isEmpty()) {
                        Text("Tidak ada bon open", modifier = Modifier.align(Alignment.Center), color = Color.Gray)
                    } else {
                        val horizontalScrollState = rememberScrollState()
                        Box(modifier = Modifier.horizontalScroll(horizontalScrollState)) {
                            Column(modifier = Modifier.width(510.dp)) {
                                Row(modifier = Modifier.background(Color(0xFF15803D).copy(alpha = 0.1f)).padding(vertical = 10.dp)) {
                                    HeaderCell("Tgl Pesan", 100.dp)
                                    HeaderCell("Nama", 180.dp)
                                    HeaderCell("Ukuran", 130.dp)
                                    HeaderCell("Jml Pesan", 100.dp)
                                }
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    itemsIndexed(filteredData) { index, item ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth()
                                                .clickable { selectedOrder = item; showUpdateDialog = true }
                                                .background(if (index % 2 == 0) Color.White else Color(0xFFF8FAFC)),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            DataCell(item.optString("tanggal"), 100.dp)
                                            DataCell(item.optString("nama"), 180.dp, TextAlign.Start)
                                            DataCell(item.optString("ukuran"), 130.dp)
                                            DataCell("${item.optString("jumlah")}", 100.dp)
                                        }
                                        HorizontalDivider(color = Color(0xFFF1F5F9))
                                    }
                                }
                            }
                        }
                    }
                }
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
                        // REVISI: Kirim Kategori agar Triple Match berfungsi
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

// ... (UpdateOrderDialog, HeaderCell, DataCell, submitToDatabaseOrder tetap sama)

@Composable
fun UpdateOrderDialog(
    order: JSONObject,
    onDismiss: () -> Unit,
    onUpdate: (tglDatang: String, jmlDatang: String) -> Unit
) {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    var tglDatang by remember { mutableStateOf(sdf.format(Date())) }
    var jmlDatang by remember { mutableStateOf(order.optString("jumlah")) }
    var isUpdating by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("DETAIL & UPDATE KEDATANGAN", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = RajabesiDarkNavy)
                Spacer(Modifier.height(16.dp))

                DetailRowOrder("Nama Part", order.optString("nama"))
                DetailRowOrder("Ukuran", order.optString("ukuran"))
                DetailRowOrder("Jml Pesan", "${order.optString("jumlah")} unit")
                DetailRowOrder("Tgl Pesan", order.optString("tanggal"))
                DetailRowOrder("Mesin", order.optString("mesin"))

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Text("Input Kedatangan", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.DarkGray)
                Spacer(Modifier.height(8.dp))

                Text("Tanggal Datang", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                DatePickerFieldArrival(tglDatang) { tglDatang = it }

                Spacer(Modifier.height(12.dp))

                Text("Jumlah Datang", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = jmlDatang,
                    onValueChange = { jmlDatang = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        isUpdating = true
                        onUpdate(tglDatang, jmlDatang)
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RajabesiDarkNavy),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isUpdating
                ) {
                    if (isUpdating) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    else Text("UPDATE & CLOSE BON", fontWeight = FontWeight.Bold)
                }

                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Batal", color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun DetailRowOrder(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, modifier = Modifier.weight(1f), fontSize = 13.sp, color = Color.Gray)
        Text(value, modifier = Modifier.weight(1.5f), fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
    }
}

@Composable
fun HeaderCell(text: String, width: Dp) {
    Text(
        text = text,
        modifier = Modifier.width(width).padding(horizontal = 4.dp),
        color = Color(0xFF15803D),
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        textAlign = TextAlign.Center
    )
}

@Composable
fun DataCell(text: String, width: Dp, textAlign: TextAlign = TextAlign.Center) {
    Text(
        text = text.ifEmpty { "-" },
        modifier = Modifier.width(width).padding(vertical = 12.dp, horizontal = 8.dp),
        fontSize = 12.sp,
        color = Color(0xFF334155),
        textAlign = textAlign,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun DatePickerFieldArrival(value: String, onValueChange: (String) -> Unit) {
    val context = LocalContext.current
    OutlinedCard(
        onClick = {
            val cal = Calendar.getInstance()
            DatePickerDialog(context, { _, y, m, d ->
                onValueChange(String.format(Locale("id", "ID"), "%02d/%02d/%04d", d, m + 1, y))
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(value, modifier = Modifier.weight(1f), fontSize = 14.sp)
            Icon(Icons.Default.CalendarMonth, null, tint = RajabesiDarkNavy)
        }
    }
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