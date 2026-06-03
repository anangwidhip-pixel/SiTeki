package com.example.sitekiver01.screens

import android.app.Activity
import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.sitekiver01.components.*
import com.example.sitekiver01.ui.theme.*
import com.example.sitekiver01.OrbitronFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StokPartScreen(
    onBack: () -> Unit,
    onOrderPart: () -> Unit = {},
    onDaftarBon: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val calendar = Calendar.getInstance()
    val localeId = remember { Locale("id", "ID") }
    val currentMonthName = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, localeId) ?: ""
    val currentYear = calendar.get(Calendar.YEAR).toString()

    val selectedBulans by remember { mutableStateOf(listOf(currentMonthName)) }
    var searchNama by remember { mutableStateOf("") }

    var dataList by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    val fetchData = suspend {
        if (!isRefreshing) isLoading = true

        withContext(Dispatchers.IO) {
            try {
                val filterBulan = selectedBulans.joinToString(", ") { "$it $currentYear" }
                val encodedBulan = URLEncoder.encode(filterBulan, "UTF-8")

                val scriptUrl = "https://script.google.com/macros/s/AKfycbxHnZzPQ3jCrMU3tvRGTiQnBIZe7pETN7iWr8e4amU4cdgi22TVzEFjB84ZXUohBDvD/exec"
                val urlString = "$scriptUrl?action=getStokPart&bulan=$encodedBulan"

                val response = URL(urlString).readText()
                if (response.trim().startsWith("[")) {
                    val jsonArray = JSONArray(response)
                    val list = mutableListOf<JSONObject>()
                    for (i in 0 until jsonArray.length()) {
                        list.add(jsonArray.getJSONObject(i))
                    }
                    dataList = list
                } else {
                    dataList = emptyList()
                }
            } catch (e: Exception) {
                Log.e("StokPart", "Error fetch: ${e.message}")
                dataList = emptyList()
            }
        }
        isLoading = false
        isRefreshing = false
    }

    LaunchedEffect(Unit) { fetchData() }

    val filteredData = remember(dataList, searchNama) {
        val query = searchNama.lowercase()
        dataList.filter {
            val nama = it.optString("nama", "").lowercase()
            val ukuran = it.optString("ukuran", "").lowercase()
            nama.contains(query) || ukuran.contains(query)
        }
    }

    val suggestions = remember(dataList, searchNama) {
        if (searchNama.length < 2) emptyList<String>()
        else {
            val query = searchNama.lowercase()
            dataList.mapNotNull {
                val n = it.optString("nama", "")
                val u = it.optString("ukuran", "")
                val full = if (u.isNotEmpty()) "$n ($u)" else n
                if (full.lowercase().contains(query)) full else null
            }.distinct().take(5)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(GlassBase)) {

        // PONDASI UTAMA: Background Mesh Grid Animasi Global
        SciFiBackground()

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                scope.launch { fetchData() }
            },
            modifier = Modifier.fillMaxSize()
        ) {
            StokPartScreenContent(
                searchNama = searchNama,
                onNamaChange = { searchNama = it },
                dataList = filteredData,
                suggestions = suggestions,
                isLoading = isLoading,
                onBack = onBack,
                onSearchClick = { scope.launch { fetchData() } },
                onOrderPartClick = onOrderPart,
                onDaftarBonClick = onDaftarBon
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StokPartScreenContent(
    searchNama: String,
    onNamaChange: (String) -> Unit,
    dataList: List<JSONObject>,
    suggestions: List<String>,
    isLoading: Boolean,
    onBack: () -> Unit,
    onSearchClick: () -> Unit,
    onOrderPartClick: () -> Unit,
    onDaftarBonClick: () -> Unit
) {
    val horizontalScrollState = rememberScrollState()
    val tableWidth = 430.dp

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
                    verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)
                    ) { Icon(Icons.Default.ArrowBackIosNew, "Back", tint = Color.White) }
                    Spacer(Modifier.width(16.dp))
                    Text(
                        "STOK SPAREPART",
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
            ModernSectionHeader("PENCARIAN INSTRUMEN", Icons.Default.Search)

            // PANEL FILTER PENCARIAN (GLASSMORPHIC)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = SciFiGlass,
                border = BorderStroke(1.dp, Brush.verticalGradient(listOf(SciFiBorderLight, Color.Transparent)))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = searchNama,
                                onValueChange = onNamaChange,
                                placeholder = { Text("Nama/Ukuran Sparepart...", fontSize = 13.sp, color = SciFiTextMuted) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SciFiCyan,
                                    unfocusedBorderColor = SciFiBorderLight,
                                    focusedContainerColor = Color.White.copy(alpha = 0.03f),
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.03f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                singleLine = true
                            )
                        }
                        IconButton(
                            onClick = onSearchClick,
                            modifier = Modifier
                                .size(44.dp)
                                .background(SciFiCyan, RoundedCornerShape(12.dp))
                        ) {
                            Icon(Icons.Default.Search, null, tint = Color.Black)
                        }
                    }

                    if (searchNama.isNotEmpty() && dataList.isEmpty() && suggestions.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Mungkin maksud Anda:",
                            fontSize = 11.sp,
                            color = SciFiTextMuted,
                            fontWeight = FontWeight.Bold,
                            fontFamily = OrbitronFontFamily
                        )
                        FlowRow(
                            modifier = Modifier.padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            suggestions.forEach { suggestion ->
                                SuggestionChip(
                                    onClick = {
                                        val cleanText = if (suggestion.contains("(")) suggestion.substringBefore(" (") else suggestion
                                        onNamaChange(cleanText)
                                    },
                                    label = { Text(suggestion, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = SciFiCyan.copy(alpha = 0.1f),
                                        labelColor = SciFiCyan
                                    ),
                                    border = BorderStroke(1.dp, SciFiCyan.copy(alpha = 0.3f)),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                ModernSectionHeader("DAFTAR STOK PART", Icons.Default.Inventory2)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onOrderPartClick,
                        colors = ButtonDefaults.buttonColors(containerColor = SciFiCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(Icons.Default.ShoppingCart, null, modifier = Modifier.size(14.dp), tint = Color.Black)
                        Spacer(Modifier.width(4.dp))
                        Text("Order Part", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black, fontFamily = OrbitronFontFamily)
                    }
                    Button(
                        onClick = onDaftarBonClick,
                        colors = ButtonDefaults.buttonColors(containerColor = SciFiStatusM),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(Icons.Default.ListAlt, null, modifier = Modifier.size(14.dp), tint = Color.Black)
                        Spacer(Modifier.width(4.dp))
                        Text("Daftar Bon", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black, fontFamily = OrbitronFontFamily)
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

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
                    } else if (dataList.isEmpty()) {
                        Text("Tidak ada data", modifier = Modifier.align(Alignment.Center), color = SciFiTextMuted, fontSize = 14.sp)
                    } else {
                        Box(modifier = Modifier.horizontalScroll(horizontalScrollState)) {
                            Column(modifier = Modifier.width(tableWidth)) {
                                Row(modifier = Modifier
                                    .background(SciFiCyan.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                    .padding(vertical = 10.dp)) {
                                    HeaderCellStok("Nama Sparepart", 120.dp)
                                    HeaderCellStok("Ukuran/Jenis", 150.dp)
                                    HeaderCellStok("Stok", 80.dp)
                                    HeaderCellStok("Satuan", 80.dp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    itemsIndexed(dataList) { index, item ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    if (index % 2 == 0) Color.Transparent else Color.White.copy(alpha = 0.01f)
                                                ),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            DataCellStok(item.optString("nama"), 120.dp, TextAlign.Start)
                                            DataCellStok(item.optString("ukuran"), 150.dp, TextAlign.Left)
                                            DataCellStok(item.optString("stok"), 80.dp, TextAlign.Left)
                                            DataCellStok(item.optString("satuan"), 80.dp, TextAlign.Left)
                                        }
                                        HorizontalDivider(color = SciFiBorderLight.copy(alpha = 0.3f))
                                    }
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
fun HeaderCellStok(text: String, width: Dp) {
    Text(
        text = text,
        modifier = Modifier
            .width(width)
            .padding(horizontal = 8.dp),
        color = SciFiCyan,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        textAlign = TextAlign.Left,
        fontFamily = OrbitronFontFamily
    )
}

@Composable
fun DataCellStok(text: String, width: Dp, textAlign: TextAlign = TextAlign.Center, maxLines: Int = 2) {
    Text(
        text = if (text == "null" || text.isEmpty()) "-" else text,
        modifier = Modifier
            .width(width)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        fontSize = 11.sp,
        color = Color.White,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Preview(showBackground = true)
@Composable
fun StokPartScreenPreview() {
    SiTekiVer01Theme {
        StokPartScreenContent(
            searchNama = "xyz",
            onNamaChange = {},
            dataList = emptyList(),
            suggestions = listOf("Bearing 6204", "V-Belt A-42"),
            isLoading = false,
            onBack = {},
            onSearchClick = {},
            onOrderPartClick = {},
            onDaftarBonClick = {}
        )
    }
}