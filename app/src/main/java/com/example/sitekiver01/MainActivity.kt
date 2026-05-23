package com.example.sitekiver01

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
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
import com.example.sitekiver01.ui.components.SciFiBackground
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

    // ==================== SCIFI BACKGROUND (NEW) ====================
    Box(modifier = Modifier.fillMaxSize()) {
        // Replace Orbs Background dengan SciFiBackground Component
        SciFiBackground()

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
                            onNavigateToStang = { currentScreen = Screen.Stang },
                            onNavigateToKPI = { currentScreen = Screen.KPI },
                            onNavigateToListrik = { currentScreen = Screen.Listrik },
                            onNavigateToLapKerja = { currentScreen = Screen.LapKerja; selectedIndex = 3 },
                            onOrderKerjaClick = { order ->
                                currentOrderDetail = order
                                currentOrderRowIndex = order.rowIndex
                                currentOrderMesin = order.namaMesin
                                currentScreen = Screen.DetailOrder
                            },
                            onNavigateToStokPart = { currentScreen = Screen.StokPart },
                            onNavigateToWebView = navigateToWebView,
                            onNavigateToOrderKerjaList = { currentScreen = Screen.OrderKerjaList },
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
                            onBuatOrderClick = { currentScreen = Screen.BuatOrderKerja }
                        )
                    }

                    Screen.DetailOrder -> {
                        currentOrderDetail?.let { order ->
                            DetailOrderScreen(
                                order = order,
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
                                previousScreen = Screen.Perawatan
                                currentScreen = Screen.IsiPerawatan
                            },
                            onNavigateToDetail = { currentScreen = Screen.DetailPerawatan }
                        )
                    }
                    Screen.Katalog -> {
                        KatalogScreen(
                            modifier = modifierWithPadding.padding(top = innerPadding.calculateTopPadding()),
                            onBack = { currentScreen = Screen.Lainnya },
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
                            onBack = { currentScreen = previousScreen }
                        )
                    }
                    Screen.Lainnya -> {
                        LainnyaScreen(
                            onBack = { currentScreen = Screen.Dashboard },
                            onNavigateToKatalog = { currentScreen = Screen.Katalog }
                        )
                    }
                    Screen.Stang -> {
                        StangScreen(
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
    onNavigateToStang: () -> Unit,
    onNavigateToKPI: () -> Unit,
    onNavigateToListrik: () -> Unit,
    onNavigateToLapKerja: () -> Unit,
    onOrderKerjaClick: (OrderItem) -> Unit,
    onNavigateToOrderKerjaList: () -> Unit,
    onNavigateToStokPart: () -> Unit,
    onNavigateToWebView: (String, String) -> Unit,
    onNavigateToIsiPerawatan: (String, String, String) -> Unit,
    onNavigateToLainnya: () -> Unit
) {
    val apiUrl = "https://script.google.com/macros/s/AKfycbyP84TUvoujsa0uuCYLR172Ft7EHzY_ofH_XkmJnYh1Y3qDICdSnlBBkGf9VU1WivQ/exec?action=getAllOrders"
    var openOrders by remember { mutableStateOf<List<OrderItem>>(emptyList()) }
    var isLoadingOrders by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isLoadingOrders = true
        openOrders = fetchOpenOrders(apiUrl)
        isLoadingOrders = false
    }

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
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

        CategorySection(
            onPerawatanClick = onNavigateToPerawatan,
            onStangClick = onNavigateToStang,
            onKPIClick = onNavigateToKPI,
            onListrikClick = onNavigateToListrik,
            onLapKerjaClick = onNavigateToLapKerja,
            onStokPartClick = onNavigateToStokPart,
            onOrderKerjaClick = onNavigateToOrderKerjaList,
            onNavigateToLainnya = onNavigateToLainnya
        )

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
                NavItem(Icons.Rounded.Dashboard, "Home", selectedIndex == 0) { onItemSelected(0) }
                NavItem(Icons.Rounded.SettingsSuggest, "Maint", selectedIndex == 1) { onItemSelected(1) }
                Spacer(modifier = Modifier.width(60.dp))
                NavItem(Icons.AutoMirrored.Rounded.Assignment, "Lap", selectedIndex == 3) { onItemSelected(3) }
                NavItem(Icons.Rounded.ManageAccounts, "Akun", selectedIndex == 4) { onItemSelected(4) }
            }
        }

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
    Column(modifier = Modifier.width(64.dp).clip(RoundedCornerShape(20.dp)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        BadgedBox(badge = { if (hasBadge) { Box(modifier = Modifier.size(8.dp).background(Color.Red, CircleShape)) } }) {
            Icon(imageVector = when (icon) { is ImageVector -> icon else -> Icons.AutoMirrored.Rounded.HelpOutline }, contentDescription = label, tint = animatedColor, modifier = Modifier.size(26.dp).graphicsLayer(scaleX = animatedScale, scaleY = animatedScale))
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
    var isLoading by remember { mutableStateOf(true) }
    var showNotificationDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLoading = true
        withContext(Dispatchers.IO) {
            try {
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

                val today = Calendar.getInstance()
                val list = mutableListOf<MaintenanceTask>()

                val todayTasks = getPendingTasks(today, masterArray, doneMap)
                list.addAll(todayTasks)

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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "SISTEM INFORMASI TEKNIK",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        fontFamily = OrbitronFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "PT. PABRIK BESI BETON RAJA BESI - KIC",
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = OrbitronFontFamily,
                        maxLines = 1
                    )
                }

                Box(
                    modifier = Modifier.padding(start = 8.dp)
                ) {
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
                            modifier = Modifier
                                .size(10.dp)
                                .background(Color.Red, CircleShape)
                                .align(Alignment.TopEnd)
                                .offset(x = (-2).dp, y = 2.dp)
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

                if (isLoading) {
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
                Column {
                    Text(
                        text = if (data.isEmpty()) "Tidak ada jadwal" else "Jadwal Perawatan",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                    if (data.isNotEmpty()) {
                        Text(
                            text = "${data.size} items",
                            fontSize = 11.sp,
                            color = GlassTextMuted
                        )
                    }
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = GlassAccentCyan,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        if (isExpanded && data.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                color = GlassSurface,
                border = BorderStroke(1.dp, GlassBorder)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    data.forEach { task ->
                        Surface(
                            onClick = { onActionClick(task) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.05f),
                            border = BorderStroke(1.dp, GlassBorder)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        task.namaAsli,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        task.tanggal,
                                        fontSize = 10.sp,
                                        color = GlassTextMuted
                                    )
                                }
                                Text(
                                    task.status,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GlassAccentCyan
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
