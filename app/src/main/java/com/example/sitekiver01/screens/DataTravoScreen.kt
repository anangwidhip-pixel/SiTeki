package com.example.sitekiver01.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sitekiver01.DataTravo
import com.example.sitekiver01.OrbitronFontFamily
import com.example.sitekiver01.components.*
import com.example.sitekiver01.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.URL

@Composable
fun DataTravoScreen(onBack: () -> Unit) {
    var dataList by remember { mutableStateOf<List<DataTravo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val url = URL("https://script.google.com/macros/s/AKfycbyaJ1oCCTfcti5u98MYyWP9OBA96SGPEmL_dchslJ9myC4dEv4ku8bZebYAxyqt0aA/exec?action=getDataTravo")
                val res = url.readText()
                val arr = JSONArray(res)
                val list = mutableListOf<DataTravo>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(DataTravo(
                        kode = obj.optString("kode"),
                        nama = obj.optString("nama"),
                        merk = obj.optString("merk"),
                        tipe = obj.optString("tipe"),
                        tegangan = obj.optString("tegangan"),
                        pengadaan = obj.optString("pengadaan")
                    ))
                }
                dataList = list
            } catch (e: Exception) { e.printStackTrace() }
            finally { isLoading = false }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Row(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .fillMaxWidth()
                    .height(72.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.background(Color.White.copy(0.1f), CircleShape)) {
                    Icon(Icons.Default.ArrowBackIosNew, "Back", tint = Color.White)
                }
                Spacer(Modifier.width(16.dp))
                Text("DATA TRAVO", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, fontFamily = OrbitronFontFamily)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(horizontal = 20.dp).fillMaxSize()) {
            ModernSectionHeader("INVENTARIS MESIN TRAVO", Icons.Default.SettingsInputComponent)
            Spacer(Modifier.height(16.dp))

            ModernFormCard(modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = GlassAccentCyan)
                    } else if (dataList.isEmpty()) {
                        Text("Tidak ada data", modifier = Modifier.align(Alignment.Center), color = GlassTextMuted, fontSize = 14.sp)
                    } else {
                        Column {
                            // Header Tabel
                            Row(modifier = Modifier.fillMaxWidth().background(GlassAccentCyan.copy(alpha = 0.1f)).padding(vertical = 10.dp)) {
                                HeaderCellTravo("Kode", 1f)
                                HeaderCellTravo("Nama", 1.5f)
                                HeaderCellTravo("Merk", 1f)
                                HeaderCellTravo("Teg", 0.8f)
                            }

                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                itemsIndexed(dataList) { index, item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                            .background(if (index % 2 == 0) Color.Transparent else Color.White.copy(alpha = 0.02f))
                                    ) {
                                        DataCellTravo(item.kode, 1f, fontWeight = FontWeight.Bold)
                                        DataCellTravo(item.nama, 1.5f)
                                        DataCellTravo(item.merk, 1f)
                                        DataCellTravo(item.tegangan, 0.8f, fontSize = 7.sp)
                                    }
                                    HorizontalDivider(color = GlassBorder)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
fun RowScope.HeaderCellTravo(text: String, weight: Float) {
    Text(text, Modifier.weight(weight), color = GlassAccentCyan, fontWeight = FontWeight.Bold, fontSize = 10.sp, textAlign = TextAlign.Center)
}

@Composable
fun RowScope.DataCellTravo(text: String, weight: Float, textAlign: TextAlign = TextAlign.Center, fontSize: androidx.compose.ui.unit.TextUnit = 9.sp, fontWeight: FontWeight = FontWeight.Normal) {
    Text(text, Modifier.weight(weight), fontSize = fontSize, color = Color.White, textAlign = textAlign, fontWeight = fontWeight)
}