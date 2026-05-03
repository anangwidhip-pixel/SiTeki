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
    val localeId = Locale("id", "ID")
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

                val scriptUrl = "https://script.google.com/macros/s/AKfycbxJOKT1yM71bQr1PbJSJ7X6q-RdJ1nmUpjvutRzkBvIYuPbZM2cGh3NuQ0X62GCJkVd/exec"
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
                        fontFamily = OrbitronFontFamily
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(20.dp))
            ModernSectionHeader("PENCARIAN DATA", Icons.Default.Search)

            ModernFormCard {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = searchNama,
                                onValueChange = onNamaChange,
                                placeholder = { Text("Nama/Ukuran Sparepart...", fontSize = 13.sp, color = GlassTextMuted) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GlassAccentCyan,
                                    unfocusedBorderColor = GlassBorder,
                                    focusedContainerColor = Color.White.copy(alpha = 0.05f),
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                singleLine = true
                            )
                        }
                        IconButton(
                            onClick = onSearchClick,
                            modifier = Modifier
                                .size(48.dp)
                                .background(GlassAccentCyan, RoundedCornerShape(12.dp))
                        ) {
                            Icon(Icons.Default.Search, null, tint = Color.Black)
                        }
                    }

                    if (searchNama.isNotEmpty() && dataList.isEmpty() && suggestions.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Mungkin maksud Anda:",
                            fontSize = 11.sp,
                            color = GlassTextMuted,
                            fontWeight = FontWeight.Bold
                        )
                        FlowRow(
                            modifier = Modifier.padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            suggestions.forEach { suggestion ->
                                SuggestionChip(
                                    onClick = { 
                                        val cleanText = if (suggestion.contains("(")) suggestion.substringBefore(" (") else suggestion
                                        onNamaChange(cleanText) 
                                    },
                                    label = { Text(suggestion, fontSize = 10.sp) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = GlassAccentCyan.copy(alpha = 0.1f),
                                        labelColor = GlassAccentCyan
                                    ),
                                    border = BorderStroke(1.dp, GlassAccentCyan.copy(alpha = 0.3f)),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                ModernSectionHeader("DAFTAR STOK PART", Icons.Default.Inventory2)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onOrderPartClick,
                        colors = ButtonDefaults.buttonColors(containerColor = GlassAccentCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(Icons.Default.ShoppingCart, null, modifier = Modifier.size(16.dp), tint = Color.Black)
                        Spacer(Modifier.width(4.dp))
                        Text("Order Part", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                    Button(
                        onClick = onDaftarBonClick,
                        colors = ButtonDefaults.buttonColors(containerColor = GlassAccentGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(Icons.Default.ListAlt, null, modifier = Modifier.size(16.dp), tint = Color.Black)
                        Spacer(Modifier.width(4.dp))
                        Text("Daftar Bon", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            ModernFormCard(modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = GlassAccentCyan)
                    } else if (dataList.isEmpty()) {
                        Text("Tidak ada data", modifier = Modifier.align(Alignment.Center), color = GlassTextMuted, fontSize = 14.sp)
                    } else {
                        Box(modifier = Modifier.horizontalScroll(horizontalScrollState)) {
                            Column(modifier = Modifier.width(tableWidth)) {
                                Row(modifier = Modifier
                                    .background(GlassAccentCyan.copy(alpha = 0.1f))
                                    .padding(vertical = 10.dp)) {
                                    HeaderCellStok("Nama Sparepart", 120.dp)
                                    HeaderCellStok("Ukuran/Jenis", 150.dp)
                                    HeaderCellStok("Stok", 80.dp)
                                    HeaderCellStok("Satuan", 80.dp)
                                }
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    itemsIndexed(dataList) { index, item ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    if (index % 2 == 0) Color.Transparent else Color.White.copy(alpha = 0.02f)
                                                ),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            DataCellStok(item.optString("nama"), 120.dp, TextAlign.Start)
                                            DataCellStok(item.optString("ukuran"), 150.dp, TextAlign.Left)
                                            DataCellStok(item.optString("stok"), 80.dp, TextAlign.Left)
                                            DataCellStok(item.optString("satuan"), 80.dp, TextAlign.Left)
                                        }
                                        HorizontalDivider(color = GlassBorder)
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
        color = GlassAccentCyan,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        textAlign = TextAlign.Left
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
