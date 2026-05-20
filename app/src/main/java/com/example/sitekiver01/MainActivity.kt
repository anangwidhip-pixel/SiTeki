package com.example.sitekiver01

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.Dialog
import com.example.sitekiver01.screens.*
import com.example.sitekiver01.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.util.*
import kotlin.math.absoluteValue
import androidx.compose.ui.tooling.preview.Preview as ComposePreview
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.async

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            SiTekiVer01Theme {
                AppNavigation()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Deep Link dari QR Code akan ditangani di AppNavigation
    }
}

suspend fun fetchOpenOrders(url: String): List<OrderItem> {
    return withContext(Dispatchers.IO) {
        val result = mutableListOf<OrderItem>()
        try {
            val response = URL(url).readText().trim()
            Log.d("SiTekiData", "Response: $response")

            // CEK: Jika respon bukan JSON Array (tidak diawali '['), maka berhenti
            if (!response.startsWith("[")) {
                Log.e("SiTekiData", "Format Salah: Respon bukan JSON Array")
                return@withContext emptyList()
            }

            val jsonArray = JSONArray(response)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val status = obj.optString("status", "")

                if (status.equals("Open", ignoreCase = true)) {
                    result.add(OrderItem(
                        rowIndex = obj.optInt("rowIndex", i + 2),
                        tanggal = obj.optString("tanggal", ""),
                        namaMesin = obj.optString("namaMesin", ""),
                        kerusakan = obj.optString("kerusakan", ""),
                        urgensi = obj.optString("urgensi", ""),
                        status = status,
                        bagianOrder = obj.optString("bagian_order", ""),
                        namaOrder = obj.optString("nama_order", ""),
                        bagianTujuan = obj.optString("bagian_tujuan", "")
                    ))
                }
            }
        } catch (e: Exception) {
            Log.e("SiTekiData", "Error Parsing: ${e.message}")
        }
        result
    }
}

@Composable
fun AppNavigation() {
    val context = LocalContext.current

    var currentScreen by remember { mutableStateOf(Screen.Dashboard) }
    var webUrl by remember { mutableStateOf("") }
    var webTitle by remember { mutableStateOf("") }
    var previousScreen by remember { mutableStateOf(Screen.Dashboard) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showPerawatanPopup by remember { mutableStateOf(false) }

    var editDataListrik by remember { mutableStateOf<JSONObject?>(null) }

    // IsiPerawatan State
    var isiPerawatanMachine by remember { mutableStateOf("") }
    var isiPerawatanDate by remember { mutableStateOf("") }
    var isiPerawatanWaktu by remember { mutableStateOf("") }

    // Order Kerja State
    var currentOrderDetail by remember { mutableStateOf<OrderItem?>(null) }
    var currentOrderRowIndex by remember { mutableIntStateOf(-1) }
    var currentOrderMesin by remember { mutableStateOf("") }
    var currentOrderMesinFromQR by remember { mutableStateOf("") }   // ← Deep Link QR Code
    var openOrders by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var isLoadingOrders by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val intent = (context as? ComponentActivity)?.intent
        val data = intent?.data
        val uriString = data?.toString() ?: ""

        when {
            // Deep Link (siteki://buatorder?...)
            data?.scheme == "siteki" && data.host == "buatorder" -> {
                val namaMesin = data.getQueryParameter("namaMesin") ?: ""
                if (namaMesin.isNotEmpty()) {
                    currentOrderMesinFromQR = namaMesin
                    currentScreen = Screen.BuatOrderKerja
                    Log.d("DeepLink", "Deep Link → $namaMesin")
                }
            }

            // Web Link dari QR Code (script.google.com)
            uriString.contains("script.google.com") -> {
                val namaMesin = data?.getQueryParameter("mesin") ?: ""
                if (namaMesin.isNotEmpty()) {
                    currentOrderMesinFromQR = namaMesin
                    currentScreen = Screen.BuatOrderKerja
                    Log.d("WebLink", "Web Link → $namaMesin")
                }
            }
        }
    }

    val navigateToWebView: (String, String) -> Unit = { url, title ->
        webUrl = url
        webTitle = title
        previousScreen = currentScreen
        currentScreen = Screen.WebView
    }

    val onNavigateToIsiPerawatan: (String, String, String) -> Unit = { machine, date, waktu ->
        isiPerawatanMachine = machine
        isiPerawatanDate = date
        isiPerawatanWaktu = waktu
        previousScreen = currentScreen
        currentScreen = Screen.IsiPerawatan
    }

    BackHandler {
        when (currentScreen) {
            Screen.Dashboard -> showExitDialog = true
            Screen.WebView -> currentScreen = previousScreen
            Screen.DetailOrder, Screen.PenyelesaianOrder, Screen.BuatOrderKerja,
            Screen.OrderKerjaList, Screen.IsiPerawatan -> currentScreen = previousScreen
            else -> {
                currentScreen = Screen.Dashboard
                selectedIndex = 0
            }
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            containerColor = Color(0xFF1A1A1A),
            title = { Text(text = "Konfirmasi Keluar", color = Color.White) },
            text = { Text(text = "Apakah Anda akan keluar dari aplikasi?", color = GlassTextMuted) },
            confirmButton = {
                Button(onClick = { (context as? Activity)?.finish() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                    Text("Yes", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showExitDialog = false }, border = BorderStroke(1.dp, GlassBorder)) {
                    Text("No", color = Color.White)
                }
            }
        )
    }

    if (showPerawatanPopup) {
        AlertDialog(
            onDismissRequest = { showPerawatanPopup = false },
            containerColor = Color(0xFF1A1A1A),
            title = { Text(text = "Pilih Halaman", color = Color.White) },
            text = { Text(text = "Apakah Ingin Melihat Jadwal?", color = GlassTextMuted) },
            confirmButton = {
                Button(onClick = {
                    showPerawatanPopup = false
                    currentScreen = Screen.JadPerawatan
                }, colors = ButtonDefaults.buttonColors(containerColor = GlassAccentCyan)) {
                    Text("Ya", color = Color.Black)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showPerawatanPopup = false
                    currentScreen = Screen.Perawatan
                }, border = BorderStroke(1.dp, GlassBorder)) {
                    Text("Tidak", color = Color.White)
                }
            }
        )
    }

    val showBottomBar = currentScreen in listOf(Screen.Dashboard, Screen.QRScanner)

    Box(modifier = Modifier.fillMaxSize().background(GlassBase)) {
        // Orbs Background
        val infiniteTransition = rememberInfiniteTransition(label = "orbs")
        val orbOffset by infiniteTransition.animateFloat(
            initialValue = 0f, targetValue = 100f,
            animationSpec = infiniteRepeatable(animation = tween(8000, easing = LinearEasing), repeatMode = RepeatMode.Reverse)
        )
        Box(modifier = Modifier.size(400.dp).offset(x = (-100).dp + orbOffset.dp, y = (-100).dp + (orbOffset/2).dp)
            .background(GlassAccentPurple.copy(alpha = 0.15f), CircleShape).blur(100.dp))
        Box(modifier = Modifier.size(350.dp).align(Alignment.BottomEnd).offset(x = 100.dp - orbOffset.dp, y = 100.dp - (orbOffset/3).dp)
            .background(GlassAccentCyan.copy(alpha = 0.12f), CircleShape).blur(80.dp))

        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                if (showBottomBar) {
                    CustomBottomNavigation(
                        selectedIndex = selectedIndex,
                        onItemSelected = { index ->
                            selectedIndex = index
                            when (index) {
                                0 -> currentScreen = Screen.Dashboard
                                1 -> showPerawatanPopup = true
                                2 -> currentScreen = Screen.QRScanner
                                3 -> currentScreen = Screen.LapKerja
                                4 -> { /* Akun - bisa ditambah nanti */ }
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                val modifierWithPadding = Modifier.padding(bottom = if (showBottomBar) innerPadding.calculateBottomPadding() else 0.dp)

                when (currentScreen) {
                    Screen.Dashboard -> {
                        DashboardScreen(
                            modifier = modifierWithPadding,
                            onNavigateToPerawatan = { showPerawatanPopup = true },
                            onNavigateToKatalog = { currentScreen = Screen.Katalog },
                            onNavigateToKPI = { currentScreen = Screen.KPI },
                            onNavigateToListrik = { currentScreen = Screen.Listrik },
                            onNavigateToLapKerja = { currentScreen = Screen.LapKerja; selectedIndex = 3 },
                            onOrderKerjaClick = { order ->
                                currentOrderDetail = order        // Simpan data order yang diklik
                                currentOrderRowIndex = order.rowIndex
                                currentOrderMesin = order.namaMesin
                                currentScreen = Screen.DetailOrder // Pindah ke halaman detail atau penyelesaian
                            },   // ← Harus ada
                            onNavigateToStokPart = { currentScreen = Screen.StokPart }, // <--- TAMBAHKAN INI
                            onNavigateToWebView = navigateToWebView,
                            onNavigateToOrderKerjaList = { currentScreen = Screen.OrderKerjaList }, // Navigasi kategori
                            onNavigateToIsiPerawatan = onNavigateToIsiPerawatan,
                            onNavigateToLainnya = { currentScreen = Screen.Lainnya }
                        )
                    }

                    Screen.OrderKerjaList -> {
                        OrderKerjaListScreen(
                            onBack = { currentScreen = Screen.Dashboard },
                            onOrderClick = { order ->
                                currentOrderDetail = order
                                currentScreen = Screen.DetailOrder
                            },
                            onBuatOrderClick = { currentScreen = Screen.BuatOrderKerja }   // ← Tambahkan ini
                        )
                    }

                    Screen.DetailOrder -> {
                        currentOrderDetail?.let { order ->
                            DetailOrderScreen(
                                order = order,                    // OrderItem
                                onBack = { currentScreen = Screen.Dashboard },
                                onLakukanPerbaikan = { selectedOrder ->
                                    currentOrderRowIndex = selectedOrder.rowIndex
                                    currentOrderMesin = selectedOrder.namaMesin
                                    currentScreen = Screen.PenyelesaianOrder
                                }
                            )
                        } ?: run {
                            currentScreen = Screen.Dashboard
                        }
                    }

                    Screen.PenyelesaianOrder -> {
                        PenyelesaianOrderScreen(
                            orderRowIndex = currentOrderRowIndex,
                            namaMesin = currentOrderMesin,
                            onBack = { currentScreen = Screen.Dashboard },
                            onSuccess = { currentScreen = Screen.Dashboard }
                        )
                    }

                    Screen.BuatOrderKerja -> {
                        BuatOrderKerjaScreen(
                            machineNameFromQR = currentOrderMesinFromQR,
                            onBack = {
                                currentScreen = Screen.Dashboard
                                currentOrderMesinFromQR = ""
                            },
                            onSuccess = {
                                currentScreen = Screen.Dashboard
                                currentOrderMesinFromQR = ""
                            }
                        )
                    }
                    Screen.Perawatan -> {
                        PerawatanScreen(
                            modifier = modifierWithPadding.padding(top = innerPadding.calculateTopPadding()),
                            onBack = { currentScreen = Screen.Dashboard; selectedIndex = 0 },
                            onNavigateToWebView = navigateToWebView,
                            onNavigateToIsiPerawatan = { machineName, tanggal, jenis ->
                                isiPerawatanMachine = machineName
                                isiPerawatanDate = tanggal
                                isiPerawatanWaktu = jenis
                                previousScreen = Screen.Perawatan      // Kembali ke PerawatanScreen
                                currentScreen = Screen.IsiPerawatan
                            },
                            onNavigateToDetail = { currentScreen = Screen.DetailPerawatan }
                        )
                    }
                    Screen.Katalog -> {
                        KatalogScreen(
                            modifier = modifierWithPadding.padding(top = innerPadding.calculateTopPadding()),
                            onBack = { currentScreen = Screen.Dashboard },
                            onNavigateToWebView = navigateToWebView
                        )
                    }
                    Screen.WebView -> {
                        Box(modifier = Modifier.padding(innerPadding)) {
                            WebViewScreen(url = webUrl, title = webTitle, onBack = { currentScreen = previousScreen })
                        }
                    }
                    Screen.QRScanner -> {
                        QRScannerScreen(
                            onBack = {
                                currentScreen = Screen.Dashboard
                                selectedIndex = 0
                            }
                        )
                    }
                    Screen.KPI -> {
                        KPIScreen(
                            onBack = { currentScreen = Screen.Dashboard },
                            onNavigateToDetailPerawatan = { currentScreen = Screen.DetailPerawatan },
                            onNavigateToDetailDowntime = { currentScreen = Screen.DetailDowntime }
                        )
                    }
                    Screen.DetailPerawatan -> { DetailPerawatanScreen(onBack = { currentScreen = Screen.KPI }) }
                    Screen.DetailDowntime -> { DetailDowntimeScreen(onBack = { currentScreen = Screen.KPI }) }
                    Screen.Listrik -> {
                        CekListrikScreen(
                            onBack = { currentScreen = Screen.Dashboard; selectedIndex = 0 },
                            onNavigateToDetail = { currentScreen = Screen.Detaillistrik },
                            editData = editDataListrik,
                            onEditFinished = { editDataListrik = null }
                        )
                    }
                    Screen.Detaillistrik -> {
                        DataListrikScreen(
                            onBack = { currentScreen = Screen.Listrik },
                            onEditData = { data ->
                                editDataListrik = data
                                currentScreen = Screen.Listrik
                            }
                        )
                    }
                    Screen.LapKerja -> {
                        LapKerjaScreen(
                            onBack = { currentScreen = Screen.Dashboard; selectedIndex = 0 },
                            onNavigateToWebView = navigateToWebView,
                            onNavigateToIsiLaporan = { currentScreen = Screen.IsiLaporan }
                        )
                    }
                    Screen.IsiLaporan -> { IsiLaporanScreen(onBack = { currentScreen = Screen.LapKerja }) }
                    Screen.JadPerawatan -> { JadPerawatanScreen(onBack = { currentScreen = Screen.Dashboard },
                        onNavigateToIsiPerawatan = onNavigateToIsiPerawatan
                    ) }
                    Screen.StokPart -> {
                        StokPartScreen(
                            onBack = { currentScreen = Screen.Dashboard },
                            onOrderPart = { currentScreen = Screen.OrderPart },
                            onDaftarBon = { currentScreen = Screen.DaftarOrder }
                        )
                    }
                    Screen.OrderPart -> { OrderPartScreen(onBack = { currentScreen = Screen.StokPart }) }
                    Screen.DaftarOrder -> { DaftarOrderScreen(onBack = { currentScreen = Screen.StokPart }) }
                    Screen.IsiPerawatan -> {
                        IsiPerawatanScreen(
                            initialMachine = isiPerawatanMachine,
                            initialDate = isiPerawatanDate,
                            initialWaktu = isiPerawatanWaktu,
                            onBack = { currentScreen = previousScreen }   // ← Ini yang penting
                        )
                    }
                    Screen.Lainnya -> {
                        LainnyaScreen(
                            onBack = { currentScreen = Screen.Dashboard }
                        )
                    }

                    // Safety net
                    else -> {
                        currentScreen = Screen.Dashboard
                    }

                }
            }
        }
    }
}

data class CategoryItem(val title: String, val icon: Any)
data class NewsItem(val title: String, val description: String, val date: String, val image: Int)
val OrbitronFontFamily = FontFamily.SansSerif

data class MaintenanceTask(
    val jenis: String,
    val namaDisplay: String,
    val namaAsli: String,
    val status: String,
    val tanggal: String
)

fun getPendingTasks(
    cal: Calendar,
    masterArray: JSONArray,
    doneMap: Map<String, Set<String>>
): List<MaintenanceTask> {
    val d = cal.get(Calendar.DAY_OF_MONTH)
    val m = cal.get(Calendar.MONTH)
    val y = cal.get(Calendar.YEAR)
    val dateStr = String.format(Locale.US, "%02d/%02d/%d", d, m + 1, y)

    val result = mutableListOf<MaintenanceTask>()

    for (i in 0 until masterArray.length()) {
        val obj = masterArray.getJSONObject(i)
        val nama = if (obj.has("nama_mesin")) obj.getString("nama_mesin") else obj.optString("nama")
        val jenis = obj.optString("jenis")

        val status = getMaintenanceStatus(nama, d, m, y)

        if ((status == "M" || status == "B") && doneMap[nama]?.contains(dateStr) != true) {
            result.add(MaintenanceTask(jenis, nama, nama, status, dateStr))
        }
    }
    return result
}

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    onNavigateToPerawatan: () -> Unit,
    onNavigateToKatalog: () -> Unit,
    onNavigateToKPI: () -> Unit,
    onNavigateToListrik: () -> Unit,
    onNavigateToLapKerja: () -> Unit,
    onOrderKerjaClick: (OrderItem) -> Unit, // Mengirim data OrderItem saat diklik
    onNavigateToOrderKerjaList: () -> Unit, // Tambahkan parameter ini
    onNavigateToStokPart: () -> Unit,
    onNavigateToWebView: (String, String) -> Unit,
    onNavigateToIsiPerawatan: (String, String, String) -> Unit,
    onNavigateToLainnya: () -> Unit
) {
    // 1. Inisialisasi wadah data
    val apiUrl = "https://script.google.com/macros/s/AKfycbyP84TUvoujsa0uuCYLR172Ft7EHzY_ofH_XkmJnYh1Y3qDICdSnlBBkGf9VU1WivQ/exec?action=getAllOrders"
    var openOrders by remember { mutableStateOf<List<OrderItem>>(emptyList()) }
    var isLoadingOrders by remember { mutableStateOf(true) }

    // 2. Ambil data dari Google Sheets saat layar dibuka
    LaunchedEffect(Unit) {
        isLoadingOrders = true
        openOrders = fetchOpenOrders(apiUrl)
        isLoadingOrders = false
    }

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // Tampilan Header
        TopHeader(
            openOrders = openOrders,
            onOrderKerjaClick = onOrderKerjaClick,
            onNavigateToWebView = onNavigateToWebView,
            onNavigateToIsiPerawatan = onNavigateToIsiPerawatan
        )
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(text = "Order Kerja", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Spacer(modifier = Modifier.height(16.dp))

            if (isLoadingOrders) {
                // Skeleton untuk Order Kerja
                Surface(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    shape = RoundedCornerShape(32.dp),
                    color = GlassSurface
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = GlassAccentCyan)
                    }
                }
            } else {
                MachineCard(
                    orders = if (openOrders.isEmpty()) {
                        listOf("Tidak ada order kerja terbuka")
                    } else {
                        openOrders.mapIndexed { index, it ->
                            "Order ${index + 1}: ${it.kerusakan}\n${it.namaMesin.ifEmpty { "Data Kosong" }}"
                        }
                    },
                    buttonText = "Lakukan Perbaikan",
                    onButtonClick = { index ->
                        if (openOrders.isNotEmpty()) onOrderKerjaClick(openOrders[index])
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Bagian Menu Kategori
        CategorySection(
            onPerawatanClick = onNavigateToPerawatan,
            onKatalogClick = onNavigateToKatalog,
            onKPIClick = onNavigateToKPI,
            onListrikClick = onNavigateToListrik,
            onLapKerjaClick = onNavigateToLapKerja,
            onStokPartClick = onNavigateToStokPart,
            onOrderKerjaClick = onNavigateToOrderKerjaList, // Gunakan parameter navigasi list
            onNavigateToWebView = onNavigateToWebView,
            onNavigateToLainnya = onNavigateToLainnya
        )

        // Bagian Berita
        NewsSection()

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun CustomBottomNavigation(
    modifier: Modifier = Modifier,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp, start = 24.dp, end = 24.dp)
            .height(88.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(68.dp)) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 0: Home
                NavItem(Icons.Rounded.Dashboard, "Home", selectedIndex == 0) { onItemSelected(0) }

                // 1: Maintenance (Perawatan)
                NavItem(Icons.Rounded.SettingsSuggest, "Maint", selectedIndex == 1) { onItemSelected(1) }

                // Spacer untuk FAB QR
                Spacer(modifier = Modifier.width(60.dp))

                // 3: Laporan Kerja
                NavItem(Icons.AutoMirrored.Rounded.Assignment, "Lap", selectedIndex == 3) { onItemSelected(3) }

                // 4: Akun
                NavItem(Icons.Rounded.ManageAccounts, "Akun", selectedIndex == 4) { onItemSelected(4) }
            }
        }

        // Floating QR Button
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-8).dp)
                .size(72.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.size(60.dp).background(GlassAccentCyan.copy(alpha = 0.3f), CircleShape).blur(15.dp))

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(GlassAccentCyan, Color(0xFF00B8D4), Color(0xFF0097A7))
                        ),
                        shape = CircleShape
                    )
                    .clickable { onItemSelected(2) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.QrCodeScanner,
                    contentDescription = "QR Scanner",
                    tint = Color.Black,
                    modifier = Modifier.size(34.dp)
                )
            }
        }
    }
}

@Composable
fun NavItem(icon: Any, label: String, isSelected: Boolean, hasBadge: Boolean = false, onClick: () -> Unit) {
    val animatedColor by animateColorAsState(targetValue = if (isSelected) GlassAccentCyan else Color.White.copy(alpha = 0.4f), animationSpec = tween(300), label = "color")
    val animatedScale by animateFloatAsState(targetValue = if (isSelected) 1.2f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow), label = "scale")
    Column(modifier = Modifier.width(64.dp).clip(RoundedCornerShape(20.dp)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        BadgedBox(badge = { if (hasBadge) { Box(modifier = Modifier.size(8.dp).background(Color.Red, CircleShape)) } }) {
            Icon(imageVector = when (icon) { is ImageVector -> icon else -> Icons.AutoMirrored.Rounded.HelpOutline }, contentDescription = label, tint = animatedColor, modifier = Modifier.size(26.dp).graphicsLayer { scaleX = animatedScale; scaleY = animatedScale })
        }
        Spacer(modifier = Modifier.height(4.dp))
        AnimatedVisibility(visible = isSelected, enter = fadeIn() + expandVertically(expandFrom = Alignment.Top), exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)) {
            Text(text = label, color = GlassAccentCyan, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, fontFamily = OrbitronFontFamily)
        }
        if (!isSelected) { Box(modifier = Modifier.height(14.dp)) }
    }
}

@Composable
fun TopHeader(
    onNavigateToWebView: (String, String) -> Unit,
    onNavigateToIsiPerawatan: (String, String, String) -> Unit,
    openOrders: List<OrderItem>,
    onOrderKerjaClick: (OrderItem) -> Unit
) {
    var notificationData by remember { mutableStateOf<List<MaintenanceTask>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }           // default true
    var showNotificationDialog by remember { mutableStateOf(false) }

    // Caching sederhana
    var cachedMaster by remember { mutableStateOf<JSONArray?>(null) }
    var cachedDone by remember { mutableStateOf<Map<String, Set<String>>?>(null) }
    LaunchedEffect(Unit) {
        isLoading = true
        withContext(Dispatchers.IO) {
            try {
                // === PARALLEL FETCH ===
                val masterDeferred = async {
                    val url = URL("https://script.google.com/macros/s/AKfycbwSnaaYVxXWVngeGQYU2im2G5FQ6L7WstjTkx7IW3jVYcuELECt0_cyvM0cFx4Uf8U/exec?action=getRawatMaster")
                    val text = url.openConnection().inputStream.bufferedReader().use { it.readText() }
                    JSONArray(text)
                }

                val doneDeferred = async {
                    val url = URL("https://script.google.com/macros/s/AKfycbwQ7ocBNsl4x5-rGLrSyvkyluhSRl3B_LvmkA3cFuvuL9pBbVAOUI3i_Vu6jwfkfOA/exec?action=getPerawatan")
                    val text = url.openConnection().inputStream.bufferedReader().use { it.readText() }
                    val array = JSONArray(text)
                    val map = mutableMapOf<String, MutableSet<String>>()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val name = obj.optString("nama_mesin")
                        val date = obj.optString("tanggal").trim()
                        if (name.isNotEmpty() && date.isNotEmpty()) {
                            map.getOrPut(name) { mutableSetOf() }.add(date)
                        }
                    }
                    map
                }

                val masterArray = masterDeferred.await()
                val doneMap = doneDeferred.await()

                cachedMaster = masterArray
                cachedDone = doneMap

                val today = Calendar.getInstance()
                val list = mutableListOf<MaintenanceTask>()

                // 1. Prioritas Utama: Jadwal HARI INI
                val todayTasks = getPendingTasks(today, masterArray, doneMap)
                list.addAll(todayTasks)

                // 2. Jika tidak ada jadwal hari ini → Ambil pending bulan ini
                if (todayTasks.isEmpty()) {
                    val currentMonth = today.get(Calendar.MONTH)
                    val currentYear = today.get(Calendar.YEAR)
                    val monthCal = today.clone() as Calendar
                    val seenMachines = mutableSetOf<String>()

                    for (day in 1..31) {
                        monthCal.set(currentYear, currentMonth, day)
                        if (monthCal.get(Calendar.MONTH) != currentMonth) break

                        val monthTasks = getPendingTasks(monthCal, masterArray, doneMap)
                        monthTasks.forEach { task ->
                            if (!seenMachines.contains(task.namaAsli)) {
                                list.add(task)
                                seenMachines.add(task.namaAsli)
                            }
                        }
                    }
                }

                notificationData = list.distinctBy { it.namaAsli }

            } catch (e: Exception) {
                Log.e("TopHeader", "Error fetching maintenance data", e)
            } finally {
                isLoading = false
            }
        }
    }

    if (showNotificationDialog) {
        Dialog(onDismissRequest = { showNotificationDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF1A1A1A),
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                border = BorderStroke(1.dp, GlassBorder)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp)
                                .background(GlassAccentPurple.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.NotificationsActive,
                                null,
                                tint = GlassAccentPurple,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp)); Text(
                        "Jadwal Perawatan",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = Color.White
                    )
                    }
                    Spacer(Modifier.height(20.dp))
                    if (isLoading) {
                        Box(
                            Modifier.fillMaxWidth().height(150.dp),
                            Alignment.Center
                        ) { CircularProgressIndicator(color = GlassAccentCyan) }
                    } else if (notificationData.isEmpty()) {
                        Column(
                            Modifier.fillMaxWidth().padding(vertical = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.DoneAll,
                                null,
                                tint = GlassAccentGreen,
                                modifier = Modifier.size(48.dp)
                            ); Spacer(Modifier.height(12.dp)); Text(
                            "Semua Beres!",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            notificationData.take(5).forEach { task ->
                                Surface(
                                    onClick = {
                                        showNotificationDialog = false; onNavigateToIsiPerawatan(
                                        task.namaAsli,
                                        task.tanggal,
                                        task.status
                                    )
                                    },
                                    color = Color.White.copy(alpha = 0.05f),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, GlassBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier.size(36.dp).background(
                                                Color.White.copy(alpha = 0.1f),
                                                CircleShape
                                            ), contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Build,
                                                null,
                                                tint = GlassAccentCyan,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                task.namaDisplay,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    task.jenis,
                                                    fontSize = 11.sp,
                                                    color = GlassTextMuted
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                // === KETERANGAN BULANAN / MINGGUAN ===
                                                val periodeLabel = when (task.status) {
                                                    "B" -> "Bulanan (B)"
                                                    "M" -> "Mingguan (M)"
                                                    else -> "Bulanan (B)"
                                                }
                                                Text(
                                                    periodeLabel,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = GlassAccentCyan,
                                                    modifier = Modifier
                                                        .background(
                                                            GlassAccentCyan.copy(alpha = 0.15f),
                                                            RoundedCornerShape(4.dp)
                                                        )
                                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        Icon(
                                            Icons.AutoMirrored.Filled.OpenInNew,
                                            null,
                                            tint = Color.White.copy(alpha = 0.3f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { showNotificationDialog = false },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GlassAccentCyan)
                    ) { Text("MENGERTI", fontWeight = FontWeight.Bold, color = Color.Black) }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SISTEM INFORMASI TEKNIK",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    fontFamily = OrbitronFontFamily
                )
                Spacer(Modifier.weight(1f))
                Box {
                    IconButton(
                        onClick = { showNotificationDialog = true },
                        modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    if (notificationData.isNotEmpty()) {
                        Box(
                            modifier = Modifier.size(10.dp).background(Color.Red, CircleShape)
                                .align(Alignment.TopEnd).offset(x = (-2).dp, y = 2.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            CarouselBanner()
            Spacer(Modifier.height(12.dp))

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Spacer(modifier = Modifier.height(24.dp))
                Text("Order Perawatan", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Spacer(modifier = Modifier.height(16.dp))

                // Tabel/Konten Perawatan sekarang akan sejajar dengan teks di atasnya
                if (isLoading) {
                    // Skeleton Loading
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = GlassSurface,
                        border = BorderStroke(1.dp, GlassBorder)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = GlassAccentCyan)
                        }
                    }
                } else {
                    MaintenanceTable(
                        data = notificationData,
                        onActionClick = { task ->
                            onNavigateToIsiPerawatan(task.namaAsli, task.tanggal, task.status)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MaintenanceTable(data: List<MaintenanceTask>, onActionClick: (MaintenanceTask) -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded },
            shape = if (isExpanded) RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            else RoundedCornerShape(24.dp),
            color = GlassSurface,
            border = BorderStroke(1.dp, GlassBorder)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Rounded.ListAlt, null, tint = GlassAccentCyan, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = if (data.isEmpty()) "Tidak ada jadwal perawatan"
                        else "${data.size} Mesin Perlu Dicek",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                val rotation by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "arrow")
                Icon(Icons.Default.KeyboardArrowDown, null, tint = GlassAccentCyan, modifier = Modifier.graphicsLayer { rotationZ = rotation })
            }
        }

        AnimatedVisibility(
            visible = isExpanded && data.isNotEmpty(),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp),
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                color = Color.White.copy(alpha = 0.03f),
                border = BorderStroke(1.dp, GlassBorder)
            ) {
                Column(Modifier.fillMaxSize()) {
                    // Header Tabel
                    Row(
                        Modifier.fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.08f))
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            "IDENTITAS MESIN",
                            Modifier.weight(1f),
                            color = GlassAccentCyan,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            fontFamily = OrbitronFontFamily
                        )
                        Text(
                            "AKSI",
                            Modifier.width(90.dp),
                            color = GlassAccentCyan,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            fontSize = 11.sp,
                            fontFamily = OrbitronFontFamily
                        )
                    }

                    LazyColumn(Modifier.fillMaxWidth(), contentPadding = PaddingValues(bottom = 12.dp)) {
                        itemsIndexed(data) { index, task ->
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = task.namaDisplay,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Jenis Perawatan
                                        Surface(
                                            color = GlassAccentCyan.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = task.jenis,
                                                color = GlassAccentCyan,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        Spacer(Modifier.width(8.dp))

                                        // === LABEL BULANAN / MINGGUAN ===
                                        val periodeLabel = when (task.status) {
                                            "B" -> "Bulanan (B)"
                                            "M" -> "Mingguan (M)"
                                            else -> "Bulanan (B)"
                                        }

                                        Text(
                                            text = periodeLabel,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (task.status == "M") Color(0xFFFFC107) else GlassAccentCyan,
                                            modifier = Modifier
                                                .background(
                                                    (if (task.status == "M") Color(0xFFFFC107) else GlassAccentCyan)
                                                        .copy(alpha = 0.15f),
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 1.dp)
                                        )
                                    }
                                }

                                Button(
                                    onClick = { onActionClick(task) },
                                    modifier = Modifier.width(90.dp).height(36.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = GlassAccentCyan),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Build, null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("BUKA", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp)
                                    }
                                }
                            }

                            if (index < data.size - 1) {
                                HorizontalDivider(
                                    Modifier.padding(horizontal = 16.dp),
                                    thickness = 0.5.dp,
                                    color = GlassBorder
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MachineCard(orders: List<String>, buttonText: String, onButtonClick: (Int) -> Unit = {}) {
    val pagerState = rememberPagerState(pageCount = { orders.size })
    val isPreview = LocalInspectionMode.current
    LaunchedEffect(Unit) { if (!isPreview) { while (true) { delay(5000); pagerState.animateScrollToPage((pagerState.currentPage + 1) % orders.size, animationSpec = tween(1000)) } } }
    val density = LocalDensity.current
    val btnW = 158.dp; val btnH = 29.dp; val cardR = 32.dp
    val btnWidthPx = with(density) { btnW.toPx() }; val btnHeightPx = with(density) { btnH.toPx() }; val cardRadiusPx = with(density) { cardR.toPx() }; val gapPx = with(density) { 8.dp.toPx() }; val br = btnHeightPx / 2
    Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
        val notchedShape = GenericShape { size, _ ->
            if (size.width > 0 && size.height > 0) {
                val w = size.width; val h = size.height; val r = cardRadiusPx; val notchTop = h - btnHeightPx - gapPx; val notchLeft = w - btnWidthPx - gapPx
                moveTo(r, 0f); lineTo(w - r, 0f); arcTo(Rect(w - 2 * r, 0f, w, 2 * r), 270f, 90f, false); lineTo(w, notchTop - br); arcTo(Rect(w - 2 * br, notchTop - 2 * br, w, notchTop), 0f, 90f, false); lineTo(notchLeft + br, notchTop); arcTo(Rect(notchLeft, notchTop, notchLeft + 2 * br, notchTop + 2 * br), 270f, -90f, false); lineTo(notchLeft, h - br); arcTo(Rect(notchLeft - 2 * br, h - 2 * br, notchLeft, h), 0f, 90f, false); lineTo(r, h); arcTo(Rect(0f, h - 2 * r, 2 * r, h), 90f, 90f, false); lineTo(0f, r); arcTo(Rect(0f, 0f, 2 * r, 2 * r), 180f, 90f, false); close()
            }
        }
        Surface(modifier = Modifier.fillMaxSize(), shape = notchedShape, color = GlassSurface) {
            Row(modifier = Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(painter = painterResource(id = R.drawable.mesin), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(86.dp))
                Spacer(modifier = Modifier.width(20.dp)); Box(modifier = Modifier.width(1.5.dp).height(64.dp).background(Color.White.copy(alpha = 0.2f))); Spacer(modifier = Modifier.width(20.dp))
                HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) { Text(text = orders[page], fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                }
            }
        }
        Button(onClick = { onButtonClick(pagerState.currentPage) }, modifier = Modifier.align(Alignment.BottomEnd).width(btnW).height(btnH), shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent), contentPadding = PaddingValues(0.dp)) {
            Box(modifier = Modifier.fillMaxSize().background(brush = Brush.horizontalGradient(colors = listOf(GlassAccentCyan, Color(0xFF0054B2))), shape = CircleShape), contentAlignment = Alignment.Center) { Text(text = buttonText, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp) }
        }
    }
}

@Composable
fun CategorySection(
    onPerawatanClick: () -> Unit,
    onKatalogClick: () -> Unit,
    onKPIClick: () -> Unit,
    onListrikClick: () -> Unit,
    onLapKerjaClick: () -> Unit,
    onStokPartClick: () -> Unit,           // <--- Tambahkan ini
    onOrderKerjaClick: () -> Unit,           // ← Harus ada
    onNavigateToLainnya: () -> Unit,   // ← Tambahkan
    onNavigateToWebView: (String, String) -> Unit
) {
    val items = remember { listOf(
        CategoryItem("Perawatan", R.drawable.perawatan),
        CategoryItem("Laporan Kerja", R.drawable.laporan),
        CategoryItem("KPI", R.drawable.kpi),
        CategoryItem("Listrik", Icons.Default.ElectricBolt),
        CategoryItem("Katalog", R.drawable.catalog),
        CategoryItem("Order Kerja", R.drawable.wo),
        CategoryItem("Part", R.drawable.part),
        CategoryItem("Lainnya", Icons.Default.Apps)
    )}

    Column(modifier = Modifier.fillMaxWidth().background(Color.Transparent)
        .padding(start = 24.dp, end = 24.dp, top = 0.dp, bottom = 20.dp)
    ) {
        Text(text = "Kategori", fontSize = 24.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 20.dp), color = Color.White)

        items.chunked(4).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { item ->
                    CategoryCard(item = item, modifier = Modifier.weight(1f), onClick = {
                        when (item.title) {
                            "Perawatan" -> onPerawatanClick()
                            "Laporan Kerja" -> onLapKerjaClick()
                            "KPI" -> onKPIClick()
                            "Listrik" -> onListrikClick()
                            "Katalog" -> onKatalogClick()
                            "Order Kerja" -> onOrderKerjaClick()
                            "Part" -> onStokPartClick() // <--- Ubah bagian ini
                            "Lainnya" -> onNavigateToLainnya()   // ← TAMBAHKAN
                        }
                    })
                }
            }
        }
    }
}

@Composable
fun CategoryCard(item: CategoryItem, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Column(modifier = modifier.clickable { onClick() }, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.aspectRatio(1f).fillMaxWidth().background(brush = Brush.verticalGradient(colors = listOf(Color.White.copy(alpha = 0.05f), Color.White.copy(alpha = 0.1f))), shape = RoundedCornerShape(28.dp)).padding(12.dp), contentAlignment = Alignment.Center) { when (val icon = item.icon) { is ImageVector -> Icon(icon, null, tint = GlassAccentCyan, modifier = Modifier.size(36.dp)); is Int -> Icon(painterResource(id = icon), null, tint = Color.Unspecified, modifier = Modifier.size(36.dp)) } }
        Text(text = item.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp), color = Color.White)
    }
}

@Composable
fun NewsSection() {
    val newsItems = remember { listOf(NewsItem("JADWAL LIBUR LEBARAN", "Semarang, 07 Maret 2026 - PT Pabrik Besi Beton Raja Besi mengumumk...", "07 Mar 2026", R.drawable.lebaran1), NewsItem("SELAMAT MERAYAKAN HARI RA...", "Semarang, 09 Maret 2026 - PT Pabrik Besi Beton Raja Besi mengucap...", "09 Mar 2026", R.drawable.lebaran1), NewsItem("PESAN K3 UNTUK KAR...", "Saat melakukan mudik lebaran pastikan kond...", "09 Mar 2026", R.drawable.pk3), NewsItem("PASTIKAN LOKASI KERJA DALAM KON...", "Before starting the long holiday, make sure the con...", "10 Mar 2026", R.drawable.lokerja), NewsItem("TIPS MERAWAT MESIN", "Simak tips singkat dari mekanik ahli kami agar mesin tetap awet...", "10 Mar 2026", R.drawable.servis)) }
    val pagerState = rememberPagerState(pageCount = { newsItems.size })
    val isPreview = LocalInspectionMode.current
    LaunchedEffect(Unit) { if (!isPreview) { while (true) { delay(4000); pagerState.animateScrollToPage((pagerState.currentPage + 1) % newsItems.size) } } }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Text(text = "Berita", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp), color = Color.White)
        HorizontalPager(state = pagerState, contentPadding = PaddingValues(end = 64.dp), pageSpacing = 16.dp, modifier = Modifier.fillMaxWidth()) { page -> NewsCard(newsItems[page]) }
    }
}

@Composable
fun NewsCard(item: NewsItem) {
    Card(modifier = Modifier.fillMaxWidth().height(340.dp), shape = RoundedCornerShape(32.dp), colors = CardDefaults.cardColors(containerColor = GlassSurface)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth().height(180.dp).padding(12.dp).clip(RoundedCornerShape(24.dp)).background(Color.White.copy(alpha = 0.1f))) { Image(painter = painterResource(id = item.image), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) { Text(text = item.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.White); Spacer(modifier = Modifier.height(8.dp)); Text(text = item.description, fontSize = 13.sp, color = GlassTextMuted, maxLines = 2, overflow = TextOverflow.Ellipsis); Spacer(modifier = Modifier.weight(1f)); Text(text = item.date, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White); Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun CarouselBanner() {
    val imageResources = listOf(R.drawable.slide1, R.drawable.slide2, R.drawable.slide3, R.drawable.slide4, R.drawable.slide5)
    val actualImageCount = imageResources.size
    val pagerState = rememberPagerState(initialPage = Int.MAX_VALUE / 2, pageCount = { Int.MAX_VALUE })
    val isPreview = LocalInspectionMode.current
    LaunchedEffect(Unit) { if (!isPreview) { while (true) { delay(3000); pagerState.animateScrollToPage(pagerState.currentPage + 1) } } }
    Box(contentAlignment = Alignment.BottomCenter) {
        HorizontalPager(state = pagerState, contentPadding = PaddingValues(horizontal = 16.dp), pageSpacing = (-14).dp) { page ->
            val actualIndex = page % actualImageCount; val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
            Card(modifier = Modifier.fillMaxWidth().height(218.dp).graphicsLayer { val scale = lerp(0.85f, 1f, 1f - pageOffset.coerceIn(0f, 1f)); scaleX = scale; scaleY = scale; alpha = lerp(0.6f, 1f, 1f - pageOffset.coerceIn(0f, 1f)) }, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))) { Image(painter = painterResource(id = imageResources[actualIndex]), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        }
        Row(modifier = Modifier.padding(bottom = 12.dp)) { repeat(actualImageCount) { i -> val active = (pagerState.currentPage % actualImageCount) == i; Box(modifier = Modifier.padding(4.dp).height(6.dp).width(if (active) 24.dp else 12.dp).clip(CircleShape).background(if (active) GlassAccentCyan else Color.White.copy(alpha = 0.3f))) } }
    }
}

@ComposePreview(showBackground = true)
@Composable
fun DashboardScreenPreview() { SiTekiVer01Theme { AppNavigation() } }
