package com.example.sitekiver01.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sitekiver01.OrbitronFontFamily
import com.example.sitekiver01.components.*
import com.example.sitekiver01.ui.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderKerjaListScreen(
    onBack: () -> Unit,
    onOrderClick: (OrderItem) -> Unit,
    onBuatOrderClick: () -> Unit
) {
    var allOrders by remember { mutableStateOf<List<OrderItem>>(emptyList()) }
    var filteredOrders by remember { mutableStateOf<List<OrderItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedMonth by remember { mutableStateOf("Semua Bulan") }

    val months = listOf(
        "Semua Bulan", "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )

    // Fetch Data
    LaunchedEffect(Unit) {
        fetchAllOrders { orders ->
            allOrders = orders
            filteredOrders = orders
            isLoading = false
        }
    }

    // Filter Logic
    LaunchedEffect(searchQuery, selectedMonth) {
        filteredOrders = allOrders.filter { order ->
            val matchSearch = order.namaMesin.contains(searchQuery, ignoreCase = true) ||
                    order.kerusakan.contains(searchQuery, ignoreCase = true) ||
                    order.bagianOrder.contains(searchQuery, ignoreCase = true)

            val matchMonth = selectedMonth == "Semua Bulan" ||
                    order.tanggal.contains(selectedMonth.take(3), ignoreCase = true)

            matchSearch && matchMonth
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
                            "DAFTAR ORDER KERJA",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            fontFamily = OrbitronFontFamily
                        )
                    }
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onBuatOrderClick,
                    containerColor = GlassAccentCyan,
                    contentColor = Color.Black,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Buat Order Baru")
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp)
            ) {
                ModernSectionHeader("PENCARIAN", Icons.Default.Search)
                
                ModernFormCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ModernTextField(
                            value = searchQuery,
                            placeholder = "Cari Mesin / Kerusakan...",
                            icon = Icons.Default.Search,
                            onValueChange = { searchQuery = it }
                        )
                        ModernDropdownField(
                            selected = selectedMonth,
                            options = months,
                            label = "Filter Bulan...",
                            onSelected = { selectedMonth = it }
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
                ModernSectionHeader("LIST ORDER KERJA", Icons.AutoMirrored.Filled.List)

                if (isLoading) {
                    Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                        CircularProgressIndicator(color = GlassAccentCyan)
                    }
                } else if (filteredOrders.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                        Text("Tidak ada data order", color = GlassTextMuted, fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 20.dp)
                    ) {
                        items(filteredOrders) { order ->
                            OrderCardModern(order = order, onClick = { onOrderClick(order) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrderCardModern(order: OrderItem, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = GlassSurface,
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(
                        when (order.urgensi) {
                            "Penting Sekali" -> Color.Red
                            "Penting" -> GlassAccentAmber
                            else -> GlassAccentCyan
                        },
                        CircleShape
                    )
            )

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = order.namaMesin,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = order.kerusakan,
                    color = GlassTextMuted,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${order.bagianOrder} • ${order.tanggal}",
                    color = GlassAccentCyan.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    color = if (order.status.equals("Open", true)) GlassAccentGreen.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(0.5.dp, if (order.status.equals("Open", true)) GlassAccentGreen.copy(alpha = 0.5f) else GlassBorder)
                ) {
                    Text(
                        text = order.status.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = if (order.status.equals("Open", true)) GlassAccentGreen else GlassTextMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = order.urgensi,
                    color = when(order.urgensi) {
                        "Penting Sekali" -> Color.Red
                        "Penting" -> GlassAccentAmber
                        else -> GlassAccentCyan
                    },
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 10.sp
                )
            }
        }
    }
}

// ==================== FETCH DATA ====================
private fun fetchAllOrders(onResult: (List<OrderItem>) -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val url = URL("https://script.google.com/macros/s/AKfycbw5xLyV1iIkfNofQsJC87fYscocDAJ8GU5iQh1WunHjYy8zS-T6sFkb77z79AptiTY/exec?action=getAllOrders")

            val text = url.readText()
            val array = JSONArray(text)

            val list = mutableListOf<OrderItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    OrderItem(
                        rowIndex = obj.optInt("rowIndex"),
                        tanggal = obj.optString("tanggal"),
                        namaMesin = obj.optString("namaMesin"),
                        kerusakan = obj.optString("kerusakan"),
                        urgensi = obj.optString("urgensi"),
                        status = obj.optString("status"),
                        bagianOrder = obj.optString("bagianOrder"),
                        namaOrder = obj.optString("namaOrder"),
                        bagianTujuan = obj.optString("bagianTujuan")
                    )
                )
            }
            withContext(Dispatchers.Main) {
                onResult(list)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                onResult(emptyList())
            }
        }
    }
}
