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
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
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
import com.example.sitekiver01.components.SciFiBackground
import com.example.sitekiver01.components.DatabaseLoadingState
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
import kotlinx.coroutines.async
import kotlinx.coroutines.CancellationException

const val STANG_SCRIPT_URL = "https://script.google.com/macros/s/AKfycbyFRP2WDOj-Vi2v-aB73VGkSycD3KHbCwiMtoS7BnXpPcAX3_4-YyvczwYd4_vIxQQ/exec"
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            val preferences = remember {
                getSharedPreferences("siteki_appearance", MODE_PRIVATE)
            }
            var darkTheme by rememberSaveable {
                mutableStateOf(preferences.getBoolean("dark_theme", false))
            }
            SiTekiVer01Theme(darkTheme = darkTheme) {
                AppNavigation(
                    darkTheme = darkTheme,
                    onToggleTheme = {
                        darkTheme = !darkTheme
                        preferences.edit().putBoolean("dark_theme", darkTheme).apply()
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
    }
}

suspend fun fetchOpenOrders(url: String): List<OrderItem> {
    return withContext(Dispatchers.IO) {
        val result = mutableListOf<OrderItem>()
        try {
            val response = URL(url).readText().trim()
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
                        bagianOrder = obj.optString("bagianOrder", obj.optString("bagian_order", "")),
                        namaOrder = obj.optString("namaOrder", obj.optString("nama_order", "")),
                        bagianTujuan = obj.optString("bagianTujuan", obj.optString("bagian_tujuan", ""))
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
fun AppNavigation(
    darkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {}
) {

    val context = LocalContext.current
    val navController = androidx.navigation.compose.rememberNavController()
    // REVISI: Mengubah default screen awal menuju Screen.Login demi keamanan otentikasi
    var showSplash by remember { mutableStateOf(true) }
    var currentScreen by remember { mutableStateOf(Screen.Login) }
    var webUrl by remember { mutableStateOf("") }
    var webTitle by remember { mutableStateOf("") }
    var previousScreen by remember { mutableStateOf(Screen.Login) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showPerawatanPopup by remember { mutableStateOf(false) }
    var editDataListrik by remember { mutableStateOf<JSONObject?>(null) }

    var isiPerawatanMachine by remember { mutableStateOf("") }
    var isiPerawatanDate by remember { mutableStateOf("") }
    var isiPerawatanWaktu by remember { mutableStateOf("") }

    var currentOrderDetail by remember { mutableStateOf<OrderItem?>(null) }
    var currentOrderRowIndex by remember { mutableIntStateOf(-1) }
    var currentOrderMesin by remember { mutableStateOf("") }
    var currentOrderMesinFromQR by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val intent = (context as? ComponentActivity)?.intent
        val data = intent?.data
        val uriString = data?.toString() ?: ""

        when {
            data?.scheme == "siteki" && data.host == "buatorder" -> {
                val namaMesin = data.getQueryParameter("namaMesin") ?: ""
                if (namaMesin.isNotEmpty()) {
                    currentOrderMesinFromQR = namaMesin
                    currentScreen = Screen.BuatOrderKerja
                    Log.d("DeepLink", "Deep Link → $namaMesin")
                }
            }

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
            Screen.Login -> (context as? Activity)?.finish() // Keluar aplikasi jika menekan back di halaman login
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
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)),
            title = {
                Text(
                    text = "Konfirmasi Keluar",
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "Apakah Anda akan keluar dari aplikasi?",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = { (context as? Activity)?.finish() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Yes", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showExitDialog = false },
                    border = BorderStroke(1.dp, SciFiBorderLight)
                ) {
                    Text("Batal", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        )
    }

    if (showPerawatanPopup) {
        Dialog(onDismissRequest = { showPerawatanPopup = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    1.dp,
                    Brush.verticalGradient(listOf(MaterialTheme.colorScheme.outline, Color.Transparent))
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "PILIH HALAMAN",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = "Apakah Ingin Melihat Jadwal?",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                showPerawatanPopup = false
                                currentScreen = Screen.JadPerawatan
                            },
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SciFiCyan),
                            border = BorderStroke(1.dp, SciFiBorderMedium)
                        ) {
                            Text(
                                text = "JADWAL",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                fontFamily = OrbitronFontFamily
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                showPerawatanPopup = false
                                currentScreen = Screen.Perawatan
                            },
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Text(
                                text = "PERAWATAN",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                fontFamily = OrbitronFontFamily
                            )
                        }
                    }
                }
            }
        }
    }

    // REVISI: Bottom bar hanya dirender jika user sukses login
    val showBottomBar =
        currentScreen in listOf(Screen.Dashboard, Screen.QRScanner) && UserSession.isLoggedIn

    AnimatedContent(
        targetState = if (showSplash) Screen.Splash else currentScreen,
        transitionSpec = {
            (fadeIn(tween(260)) + slideInHorizontally(tween(360)) { it / 9 })
                .togetherWith(fadeOut(tween(180)) + slideOutHorizontally(tween(260)) { -it / 12 })
        },
        label = "workspaceScreenTransition"
    ) { targetScreen ->

        if (targetScreen == Screen.Splash) {
            SplashScreen(
                onTimeout = {
                    showSplash = false
                }
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                SciFiBackground()

                Scaffold(
                    containerColor = Color.Transparent,
                    bottomBar = {
                        if (showBottomBar) {
                            CustomBottomNavigation(
                                selectedIndex = selectedIndex,
                                darkTheme = darkTheme,
                                onItemSelected = { index ->
                                    if (index != 4) selectedIndex = index

                                    when (index) {
                                        0 -> currentScreen = Screen.Dashboard
                                        1 -> showPerawatanPopup = true
                                        2 -> currentScreen = Screen.QRScanner
                                        3 -> currentScreen = Screen.LapKerja
                                        4 -> onToggleTheme()
                                    }
                                }
                            )
                        }
                    }
                ) { innerPadding ->

                    Box(modifier = Modifier.fillMaxSize()) {

                        val modifierWithPadding = Modifier.padding(
                            bottom = if (showBottomBar) {
                                innerPadding.calculateBottomPadding()
                            } else {
                                0.dp
                            }
                        )

                        // Gunakan targetScreen, bukan currentScreen
                        when (targetScreen) {
                            Screen.Login -> {
                                LoginScreen(
                                    onLoginSuccess = {
                                        currentScreen = Screen.Dashboard
                                        selectedIndex = 0
                                    }
                                )
                            }

                            Screen.Dashboard -> {
                                DashboardScreen(
                                    modifier = modifierWithPadding,

                                    // Callback Anda tetap seperti semula
                                    onNavigateToPerawatan = {
                                        showPerawatanPopup = true
                                    },

                                    onNavigateToStang = {
                                        currentScreen = Screen.Stang
                                    },

                                    onNavigateToKPI = {
                                        currentScreen = Screen.KPI
                                    },

                                    onNavigateToListrik = {
                                        currentScreen = Screen.Listrik
                                    },

                                    onNavigateToLapKerja = {
                                        currentScreen = Screen.LapKerja
                                        selectedIndex = 3
                                    },
                                    onOrderKerjaClick = { order ->
                                        currentOrderDetail = order
                                        currentOrderRowIndex = order.rowIndex
                                        currentOrderMesin = order.namaMesin
                                        currentScreen = Screen.DetailOrder
                                    },
                                    onNavigateToStokPart = { currentScreen = Screen.StokPart },
                                    onNavigateToWebView = navigateToWebView,
                                    onNavigateToOrderKerjaList = {
                                        currentScreen = Screen.OrderKerjaList
                                    },
                                    onNavigateToIsiPerawatan = onNavigateToIsiPerawatan,
                                    onNavigateToLainnya = { currentScreen = Screen.Lainnya },
                                    onNavigateToManagementUser = {
                                        currentScreen = Screen.ManagementUser
                                    },
                                    darkTheme = darkTheme,
                                    onToggleTheme = onToggleTheme,
                                    onLogout = {
                                        UserSession.logout()
                                        currentScreen = Screen.Login
                                    }
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

                            Screen.ManagementUser -> {
                                UserManagementScreen(
                                    onBack = { currentScreen = Screen.Dashboard },
                                    onSuccess = {
                                        // Setelah sukses sinkronisasi data, kembalikan ke Dashboard atau beri aksi lain
                                        currentScreen = Screen.Dashboard
                                    }
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
                                    onBack = {
                                        currentScreen = Screen.Dashboard; selectedIndex = 0
                                    },
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
                                    WebViewScreen(
                                        url = webUrl,
                                        title = webTitle,
                                        onBack = { currentScreen = previousScreen })
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
                                    onNavigateToDetailPerawatan = {
                                        currentScreen = Screen.DetailPerawatan
                                    },
                                    onNavigateToDetailDowntime = {
                                        currentScreen = Screen.DetailDowntime
                                    }
                                )
                            }

                            Screen.DetailPerawatan -> {
                                DetailPerawatanScreen(onBack = { currentScreen = Screen.KPI })
                            }

                            Screen.DetailDowntime -> {
                                DetailDowntimeScreen(onBack = { currentScreen = Screen.KPI })
                            }

                            Screen.Listrik -> {
                                CekListrikScreen(
                                    onBack = {
                                        currentScreen = Screen.Dashboard; selectedIndex = 0
                                    },
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
                                    onBack = {
                                        currentScreen = Screen.Dashboard; selectedIndex = 0
                                    },
                                    onNavigateToWebView = navigateToWebView,
                                    onNavigateToIsiLaporan = { currentScreen = Screen.IsiLaporan }
                                )
                            }

                            Screen.IsiLaporan -> {
                                IsiLaporanScreen(onBack = { currentScreen = Screen.LapKerja })
                            }

                            Screen.JadPerawatan -> {
                                JadPerawatanScreen(
                                    onBack = { currentScreen = Screen.Dashboard },
                                    onNavigateToIsiPerawatan = onNavigateToIsiPerawatan
                                )
                            }

                            Screen.StokPart -> {
                                StokPartScreen(
                                    onBack = { currentScreen = Screen.Dashboard },
                                    onOrderPart = { currentScreen = Screen.OrderPart },
                                    onDaftarBon = { currentScreen = Screen.DaftarOrder }
                                )
                            }

                            Screen.OrderPart -> {
                                OrderPartScreen(onBack = { currentScreen = Screen.StokPart })
                            }

                            Screen.DaftarOrder -> {
                                DaftarOrderScreen(onBack = { currentScreen = Screen.StokPart })
                            }

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
                                    onNavigateToKatalog = { currentScreen = Screen.Katalog },
                                    onNavigateToTravo = { currentScreen = Screen.TravoMenu },
                                    onNavigateToLemburan = { currentScreen = Screen.IsiLemburan },
                                    // Tambahkan navigasi baru ke User Management
                                    onNavigateToUserMgmt = {
                                        if (UserSession.role == "Admin") {
                                            currentScreen = Screen.ManagementUser
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Hanya Admin yang memiliki akses!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                )
                            }

                            Screen.TravoMenu -> {
                                TravoMenuScreen(
                                    onBack = {
                                        currentScreen = Screen.Lainnya
                                    }, // Atau ke Screen.Dashboard
                                    onNavigateToInspeksi = {
                                        currentScreen = Screen.TravoInspeksi
                                    }, // Pastikan ini terdaftar
                                    onNavigateToDataTravo = {
                                        currentScreen = Screen.TravoData
                                    }      // Pastikan ini terdaftar
                                )
                            }

// Tambahkan juga layar tujuan agar tidak "nyangkut"
                            Screen.TravoInspeksi -> {
                                IsiInspeksiScreen(onBack = { currentScreen = Screen.TravoMenu })
                            }

                            Screen.TravoData -> {
                                DataTravoScreen(onBack = { currentScreen = Screen.TravoMenu })
                            }

                            Screen.ManagementUser -> {
                                UserManagementScreen(
                                    onBack = {
                                        currentScreen = Screen.Lainnya
                                    } // Memastikan state berubah ke 'Lainnya'
                                )
                            }

                            Screen.IsiLemburan -> {
                                IsiLemburanScreen(
                                    onBack = { currentScreen = Screen.Lainnya },
                                    onNavigateToRekap = {
                                        currentScreen = Screen.RekapAdmin
                                    } // Navigasi via state
                                )
                            }

                            Screen.Stang -> {
                                StangScreen(
                                    onBack = { currentScreen = Screen.Dashboard }
                                )
                            }

                            Screen.RekapAdmin -> {
                                RekapAdminScreen(
                                    onBack = { currentScreen = Screen.IsiLemburan }
                                )
                            }


                            // REVISI: Layar Manajemen Pendaftaran Karyawan Baru

                            else -> {
                                currentScreen = Screen.Dashboard
                            }

                        }
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

data class DashboardMaintenanceResult(
    val tasks: List<MaintenanceTask>,
    val isFromPreviousWeek: Boolean
)

private suspend fun fetchDashboardMaintenance(): DashboardMaintenanceResult {
    return withContext(Dispatchers.IO) {
        val masterText = URL(
            "https://script.google.com/macros/s/AKfycbwSnaaYVxXWVngeGQYU2im2G5FQ6L7WstjTkx7IW3jVYcuELECt0_cyvM0cFx4Uf8U/exec?action=getRawatMaster"
        ).readText()
        val actualText = URL(
            "https://script.google.com/macros/s/AKfycbwQ7ocBNsl4x5-rGLrSyvkyluhSRl3B_LvmkA3cFuvuL9pBbVAOUI3i_Vu6jwfkfOA/exec?action=getPerawatan"
        ).readText()

        val masterArray = JSONArray(masterText)
        val actualArray = JSONArray(actualText)
        val completedByMachine = mutableMapOf<String, MutableSet<String>>()

        for (index in 0 until actualArray.length()) {
            val item = actualArray.getJSONObject(index)
            val machineName = item.optString("nama_mesin").trim()
            val date = item.optString("tanggal").trim()
            if (machineName.isNotEmpty() && date.isNotEmpty()) {
                completedByMachine.getOrPut(machineName) { mutableSetOf() }.add(date)
            }
        }

        val today = Calendar.getInstance()
        val todayTasks = getPendingTasks(today, masterArray, completedByMachine)
        if (todayTasks.isNotEmpty()) {
            return@withContext DashboardMaintenanceResult(
                tasks = todayTasks.distinctBy { "${it.namaAsli}|${it.status}|${it.tanggal}" },
                isFromPreviousWeek = false
            )
        }

        val previousMonday = (today.clone() as Calendar).apply {
            firstDayOfWeek = Calendar.MONDAY
            minimalDaysInFirstWeek = 4
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            add(Calendar.WEEK_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val overdueTasks = buildList {
            repeat(7) { dayOffset ->
                val day = (previousMonday.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_MONTH, dayOffset)
                }
                addAll(getPendingTasks(day, masterArray, completedByMachine))
            }
        }
            .distinctBy { "${it.namaAsli}|${it.status}" }

        DashboardMaintenanceResult(
            tasks = overdueTasks,
            isFromPreviousWeek = overdueTasks.isNotEmpty()
        )
    }
}

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
    onNavigateToLainnya: () -> Unit,
    onNavigateToManagementUser: () -> Unit,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onLogout: () -> Unit
) {
    val apiUrl = "https://script.google.com/macros/s/AKfycbzbmKFheI55ccsJ_kLdOzy6VIdGpgKIy2s9pljrIM8sNbgJ_RLywnzF-Q2sJTslVQU/exec?action=getAllOrders"
    var openOrders by remember { mutableStateOf<List<OrderItem>>(emptyList()) }
    var isLoadingOrders by remember { mutableStateOf(true) }
    var maintenanceResult by remember { mutableStateOf<DashboardMaintenanceResult?>(null) }
    var isLoadingMaintenance by remember { mutableStateOf(true) }
    var maintenanceError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isLoadingOrders = true
        openOrders = fetchOpenOrders(apiUrl)
        isLoadingOrders = false
    }

    LaunchedEffect(Unit) {
        isLoadingMaintenance = true
        maintenanceError = null
        try {
            maintenanceResult = fetchDashboardMaintenance()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("DashboardMaintenance", "Gagal membaca jadwal: ${e.message}", e)
            maintenanceError = "Jadwal perawatan belum dapat dimuat"
        } finally {
            isLoadingMaintenance = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
    ) {
        WorkspaceHeader(
            openOrderCount = openOrders.size,
            darkTheme = darkTheme,
            onToggleTheme = onToggleTheme,
            onLogout = onLogout
        )

        Spacer(Modifier.height(26.dp))

        SectionEyebrow("PRIORITAS HARI INI")
        Spacer(Modifier.height(10.dp))

        if (isLoadingOrders) {
            Surface(
                modifier = Modifier.fillMaxWidth().height(174.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                DatabaseLoadingState(
                    label = "Menyinkronkan order kerja",
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            MachineCard(
                orders = if (openOrders.isEmpty()) {
                    listOf("Tidak ada order kerja terbuka")
                } else {
                    openOrders.mapIndexed { index, it ->
                        "${index + 1}. ${it.namaMesin.ifEmpty { "Tanpa nama mesin" }}\n${it.kerusakan}"
                    }
                },
                buttonText = "Buka order",
                onButtonClick = { index ->
                    if (openOrders.isNotEmpty()) onOrderKerjaClick(openOrders[index])
                }
            )
        }

        Spacer(Modifier.height(30.dp))
        SectionEyebrow("PERAWATAN")
        Spacer(Modifier.height(12.dp))

        DashboardMaintenanceSliderPanel(
            result = maintenanceResult,
            isLoading = isLoadingMaintenance,
            errorMessage = maintenanceError,
            onTaskClick = { task ->
                onNavigateToIsiPerawatan(task.namaAsli, task.tanggal, task.status)
            }
        )

        Spacer(Modifier.height(30.dp))
        SectionEyebrow("RUANG KERJA")
        Spacer(Modifier.height(12.dp))

        CategorySection(
            onPerawatanClick = onNavigateToPerawatan,
            onStangClick = onNavigateToStang,
            onKPIClick = onNavigateToKPI,
            onListrikClick = onNavigateToListrik,
            onLapKerjaClick = onNavigateToLapKerja,
            onStokPartClick = onNavigateToStokPart,
            onOrderKerjaClick = onNavigateToOrderKerjaList,
            onNavigateToLainnya = onNavigateToLainnya,
            onNavigateToManagementUser = onNavigateToManagementUser
        )

        Spacer(Modifier.height(110.dp))
    }
}

@Composable
private fun WorkspaceHeader(
    openOrderCount: Int,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onLogout: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "workspaceSignal")
    val signal by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "workspaceSignalAlpha"
    )

    Column(Modifier.fillMaxWidth().padding(top = 20.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("ST", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("SiTeki Workspace", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(UserSession.role.ifBlank { "Operational user" }, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onToggleTheme) {
                    Icon(
                        if (darkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                        "Ganti tema",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onLogout) {
                    Icon(Icons.Rounded.Logout, "Keluar", tint = MaterialTheme.colorScheme.error)
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        Text(
            "Selamat bekerja,\n${UserSession.namaFull.ifBlank { "Tim Teknik" }}",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 32.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-1).sp
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = signal), CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(
                "$openOrderCount order terbuka · sistem tersinkron",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun SectionEyebrow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(22.dp).height(3.dp).background(MaterialTheme.colorScheme.primary))
        Spacer(Modifier.width(8.dp))
        Text(
            text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.5.sp
        )
    }
}

@Composable
private fun DashboardMaintenanceSliderPanel(
    result: DashboardMaintenanceResult?,
    isLoading: Boolean,
    errorMessage: String?,
    onTaskClick: (MaintenanceTask) -> Unit
) {
    if (!isLoading && errorMessage == null && result != null && result.tasks.isNotEmpty()) {
        DashboardMaintenanceSlider(result = result, onTaskClick = onTaskClick)
    } else {
        DashboardMaintenancePanel(
            result = result,
            isLoading = isLoading,
            errorMessage = errorMessage,
            onTaskClick = onTaskClick
        )
    }
}

@Composable
private fun DashboardMaintenanceSlider(
    result: DashboardMaintenanceResult,
    onTaskClick: (MaintenanceTask) -> Unit
) {
    val tasks = result.tasks
    val pagerState = rememberPagerState(pageCount = { tasks.size })
    val isPreview = LocalInspectionMode.current

    LaunchedEffect(tasks) {
        if (!isPreview && tasks.size > 1) {
            while (true) {
                delay(5000)
                try {
                    pagerState.animateScrollToPage(
                        (pagerState.currentPage + 1) % tasks.size,
                        animationSpec = tween(650)
                    )
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth().height(174.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 4.dp
    ) {
        Column(Modifier.fillMaxSize().padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (result.isFromPreviousWeek) {
                        MaterialTheme.colorScheme.tertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    }
                ) {
                    Icon(
                        if (result.isFromPreviousWeek) Icons.Rounded.History else Icons.Rounded.CalendarToday,
                        null,
                        tint = if (result.isFromPreviousWeek) {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.padding(9.dp).size(19.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    if (result.isFromPreviousWeek) {
                        "${tasks.size} PERAWATAN TERTUNDA"
                    } else {
                        "${tasks.size} PERAWATAN HARI INI"
                    },
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp
                )
                Text(
                    "${pagerState.currentPage + 1}/${tasks.size}",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                val task = tasks[page]
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTaskClick(task) }
                        .padding(top = 13.dp)
                ) {
                    Text(
                        task.namaDisplay,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        lineHeight = 22.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(5.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(5.dp),
                            color = if (task.status == "B") {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            }
                        ) {
                            Text(
                                task.status.ifBlank { "M" },
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                color = if (task.status == "B") {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                },
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${if (task.status == "B") "Bulanan" else "Mingguan"} · ${task.tanggal}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    repeat(tasks.size.coerceAtMost(5)) { index ->
                        Box(
                            Modifier
                                .width(if (index == pagerState.currentPage) 18.dp else 5.dp)
                                .height(5.dp)
                                .background(
                                    if (index == pagerState.currentPage) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outline
                                    },
                                    CircleShape
                                )
                        )
                    }
                }
                TextButton(
                    onClick = {
                        onTaskClick(tasks[pagerState.currentPage.coerceAtMost(tasks.lastIndex)])
                    }
                ) {
                    Text("Isi perawatan", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Rounded.ArrowForward, null, Modifier.size(17.dp))
                }
            }
        }
    }
}

@Composable
private fun DashboardMaintenancePanel(
    result: DashboardMaintenanceResult?,
    isLoading: Boolean,
    errorMessage: String?,
    onTaskClick: (MaintenanceTask) -> Unit
) {
    when {
        isLoading -> {
            Surface(
                modifier = Modifier.fillMaxWidth().height(166.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                DatabaseLoadingState(
                    label = "Memeriksa jadwal perawatan",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        errorMessage != null -> {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f))
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.CloudOff, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        result == null || result.tasks.isEmpty() -> {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    Modifier.padding(17.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            Icons.Rounded.TaskAlt,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(9.dp).size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "Tidak ada perawatan tertunda",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Hari ini dan minggu sebelumnya sudah bersih.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        else -> {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shadowElevation = 3.dp
            ) {
                Column {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (result.isFromPreviousWeek) {
                                MaterialTheme.colorScheme.tertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            }
                        ) {
                            Icon(
                                if (result.isFromPreviousWeek) Icons.Rounded.History else Icons.Rounded.CalendarToday,
                                null,
                                tint = if (result.isFromPreviousWeek) {
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                                modifier = Modifier.padding(9.dp).size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (result.isFromPreviousWeek) {
                                    "Tertunda minggu sebelumnya"
                                } else {
                                    "Perawatan hari ini"
                                },
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                if (result.isFromPreviousWeek) {
                                    "Belum tercatat selesai dan perlu ditindaklanjuti."
                                } else {
                                    "${result.tasks.size} pekerjaan sesuai jadwal hari ini."
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }
                        Text(
                            result.tasks.size.toString().padStart(2, '0'),
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    result.tasks.forEachIndexed { index, task ->
                        var visible by remember(task.namaAsli, task.tanggal) { mutableStateOf(false) }
                        LaunchedEffect(task.namaAsli, task.tanggal) {
                            delay(index * 55L)
                            visible = true
                        }
                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn(tween(220)) + slideInHorizontally(tween(280)) { 24 }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onTaskClick(task) }
                                    .padding(horizontal = 16.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (task.status == "B") {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.primaryContainer
                                    }
                                ) {
                                    Text(
                                        task.status.ifBlank { "M" },
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                        color = if (task.status == "B") {
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        task.namaDisplay,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "${if (task.status == "B") "Bulanan" else "Mingguan"} · ${task.tanggal}",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp
                                    )
                                }
                                Icon(
                                    Icons.Rounded.ArrowForward,
                                    "Isi perawatan",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        if (index < result.tasks.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 64.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomBottomNavigation(
    modifier: Modifier = Modifier,
    selectedIndex: Int,
    darkTheme: Boolean,
    onItemSelected: (Int) -> Unit
) {
    val items = listOf(
        Triple("Beranda", Icons.Rounded.Home, 0),
        Triple("Rawat", Icons.Rounded.Build, 1),
        Triple("Pindai", Icons.Rounded.QrCodeScanner, 2),
        Triple("Laporan", Icons.AutoMirrored.Rounded.Assignment, 3),
        Triple(if (darkTheme) "Terang" else "Gelap", if (darkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode, 4)
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .height(68.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 16.dp
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 7.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            items.forEach { (label, icon, index) ->
                val selected = selectedIndex == index && index != 4
                val width by animateDpAsState(if (selected) 88.dp else 52.dp, tween(260), label = "dockWidth")
                val container by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    tween(220),
                    label = "dockColor"
                )
                Surface(
                    onClick = { onItemSelected(index) },
                    modifier = Modifier.width(width).fillMaxHeight(),
                    shape = RoundedCornerShape(13.dp),
                    color = container
                ) {
                    Row(
                        Modifier.fillMaxSize().padding(horizontal = if (selected) 11.dp else 0.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            icon,
                            label,
                            modifier = Modifier.size(if (index == 2) 25.dp else 21.dp),
                            tint = if (selected || index == 2) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        AnimatedVisibility(
                            visible = selected,
                            enter = fadeIn(tween(180)) + expandHorizontally(),
                            exit = fadeOut(tween(120)) + shrinkHorizontally()
                        ) {
                            Text(
                                label,
                                modifier = Modifier.padding(start = 7.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegacyCustomBottomNavigation(modifier: Modifier = Modifier, selectedIndex: Int, onItemSelected: (Int) -> Unit) {
    val density = LocalDensity.current

    val barHeight = 72.dp
    val fabSize = 64.dp

    val barHeightPx = with(density) { barHeight.toPx() }
    val cornerRadiusPx = with(density) { 24.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp, start = 12.dp, end = 12.dp)
            .height(barHeight + 16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {

        val cyberDockShape = GenericShape { size, _ ->
            val w = size.width
            val h = size.height
            val center = w / 2

            val notchW = with(density) { 46.dp.toPx() }
            val notchH = with(density) { 32.dp.toPx() }
            val r = cornerRadiusPx

            moveTo(r, 0f)
            lineTo(center - notchW, 0f)

            cubicTo(
                center - (notchW * 0.6f), 0f,
                center - (notchW * 0.7f), notchH,
                center, notchH
            )
            cubicTo(
                center + (notchW * 0.7f), notchH,
                center + (notchW * 0.6f), 0f,
                center + notchW, 0f
            )

            lineTo(w - r, 0f)

            arcTo(Rect(w - 2 * r, 0f, w, 2 * r), 270f, 90f, false)

            lineTo(w, h - r)
            arcTo(Rect(w - 2 * r, h - 2 * r, w, h), 0f, 90f, false)

            lineTo(r, h)
            arcTo(Rect(0f, h - 2 * r, 2 * r, h), 90f, 90f, false)

            lineTo(0f, r)
            arcTo(Rect(0f, 0f, 2 * r, 2 * r), 180f, 90f, false)
            close()
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .graphicsLayer {
                    shape = cyberDockShape
                    clip = true
                }
                .drawBehind {
                    val w = size.width
                    val center = w / 2
                    val notchW = with(density) { 46.dp.toPx() }
                    val notchH = with(density) { 32.dp.toPx() }

                    val strokeThickness = 3.5f
                    val offsetY = strokeThickness / 2

                    val borderPath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(center - notchW - 20f, offsetY)
                        lineTo(center - notchW, offsetY)
                        cubicTo(
                            center - (notchW * 0.6f), offsetY,
                            center - (notchW * 0.7f), notchH,
                            center, notchH
                        )
                        cubicTo(
                            center + (notchW * 0.7f), notchH,
                            center + (notchW * 0.6f), offsetY,
                            center + notchW, offsetY
                        )
                        lineTo(center + notchW + 20f, offsetY)
                    }

                    drawPath(
                        path = borderPath,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                SciFiCyan,
                                SciFiBlue,
                                SciFiCyan,
                                Color.Transparent
                            )
                        ),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeThickness)
                    )
                },
            color = Color(0xCC070D19),
            border = null
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavItem(Icons.Rounded.Dashboard, "DASHBOARD", selectedIndex == 0) { onItemSelected(0) }
                NavItem(Icons.Rounded.SettingsSuggest, "MAINT", selectedIndex == 1) { onItemSelected(1) }

                Spacer(modifier = Modifier.width(fabSize + 12.dp))

                NavItem(Icons.AutoMirrored.Rounded.Assignment, "LAPORAN", selectedIndex == 3) { onItemSelected(3) }
                NavItem(Icons.Rounded.ManageAccounts, "AKUN", selectedIndex == 4) { onItemSelected(4) }
            }
        }

        val infinitePulse = rememberInfiniteTransition(label = "reactorPulse")
        val glowRadius by infinitePulse.animateFloat(
            initialValue = 10f,
            targetValue = 20f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glow"
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-8).dp)
                .size(fabSize),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(fabSize - 8.dp)
                    .background(SciFiCyan.copy(alpha = 0.25f), CircleShape)
                    .blur(glowRadius.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(2.dp, Brush.sweepGradient(listOf(SciFiCyan, SciFiBlue, SciFiCyan)), CircleShape)
            )

            Box(
                modifier = Modifier
                    .size(fabSize - 8.dp)
                    .clip(CircleShape)
                    .background(brush = Brush.verticalGradient(colors = listOf(Color(0xFF0D192B), Color(0xFF040810))))
                    .border(1.dp, SciFiCyan.copy(alpha = 0.4f), CircleShape)
                    .clickable { onItemSelected(2) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.QrCodeScanner,
                    contentDescription = "Core Scanner",
                    tint = SciFiCyan,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
fun NavItem(icon: Any, label: String, isSelected: Boolean, onClick: () -> Unit) {
    val animatedColor by animateColorAsState(
        targetValue = if (isSelected) SciFiCyan else Color.White.copy(alpha = 0.35f),
        animationSpec = tween(250),
        label = "neonColor"
    )
    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.2f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "neonScale"
    )

    Column(
        modifier = Modifier
            .width(64.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.height(28.dp)) {
            if (isSelected) {
                Icon(
                    imageVector = icon as ImageVector,
                    contentDescription = null,
                    tint = SciFiCyan.copy(alpha = 0.4f),
                    modifier = Modifier
                        .size(24.dp)
                        .blur(6.dp)
                        .graphicsLayer {
                            scaleX = animatedScale
                            scaleY = animatedScale
                        }
                )
            }
            Icon(
                imageVector = icon as ImageVector,
                contentDescription = label,
                tint = animatedColor,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer {
                        scaleX = animatedScale
                        scaleY = animatedScale
                    }
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            color = animatedColor,
            fontSize = 8.5.sp,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
            fontFamily = OrbitronFontFamily,
            letterSpacing = 0.5.sp,
            textAlign = TextAlign.Center,
            maxLines = 1
        )

        AnimatedVisibility(
            visible = isSelected,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(width = 12.dp, height = 2.dp)
                    .background(SciFiCyan, RoundedCornerShape(1.dp))
            )
        }
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

            } catch (e: CancellationException) {
                // Jangan log apapun di sini, ini normal saat pindah halaman
                throw e
            } catch (e: Exception) {
                // Ini baru error yang sebenarnya (masalah internet/format data)
                Log.e("TopHeader", "Error fetching maintenance data: ${e.message}")
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
                        DatabaseLoadingState(
                            label = "Memeriksa jadwal perawatan",
                            modifier = Modifier.fillMaxWidth().height(150.dp)
                        )
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
                        DatabaseLoadingState(
                            label = "Menyinkronkan order perawatan",
                            modifier = Modifier.fillMaxSize()
                        )
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
                val rotation by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "rotation")
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
    val safePageCount = if (orders.isEmpty()) 1 else orders.size
    val pagerState = rememberPagerState(pageCount = { safePageCount })
    val isPreview = LocalInspectionMode.current

    LaunchedEffect(orders) {
        if (!isPreview && orders.size > 1) {
            while (true) {
                delay(5000)
                try {
                    val nextPage = (pagerState.currentPage + 1) % orders.size
                    pagerState.animateScrollToPage(nextPage, animationSpec = tween(650))
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth().height(174.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 4.dp
    ) {
        Column(Modifier.fillMaxSize().padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Icon(
                        Icons.Rounded.Engineering,
                        null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(9.dp).size(19.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    if (orders.size == 1 && orders.first().contains("Tidak ada")) "ANTRIAN BERSIH" else "${orders.size} ORDER PERLU DITANGANI",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp
                )
            }

            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                val text = orders.getOrElse(page) { "Tidak ada data" }
                Text(
                    text,
                    modifier = Modifier.fillMaxWidth().padding(top = 15.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    lineHeight = 23.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    repeat(orders.size.coerceAtMost(5)) { index ->
                        Box(
                            Modifier
                                .width(if (index == pagerState.currentPage) 18.dp else 5.dp)
                                .height(5.dp)
                                .background(
                                    if (index == pagerState.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    CircleShape
                                )
                        )
                    }
                }
                if (orders.isNotEmpty() && !orders[0].contains("Tidak ada order kerja terbuka")) {
                    TextButton(onClick = { onButtonClick(pagerState.currentPage.coerceAtMost(orders.lastIndex)) }) {
                        Text(buttonText, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Rounded.ArrowForward, null, Modifier.size(17.dp))
                    }
                }
            }
        }
    }
}

data class CategoryItem(val title: String, val icon: Any)

@Composable
fun CategorySection(
    onPerawatanClick: () -> Unit,
    onStangClick: () -> Unit,
    onKPIClick: () -> Unit,
    onListrikClick: () -> Unit,
    onLapKerjaClick: () -> Unit,
    onStokPartClick: () -> Unit,
    onOrderKerjaClick: () -> Unit,
    onNavigateToLainnya: () -> Unit,
    onNavigateToManagementUser: () -> Unit
) {
    val role = UserSession.role

    // REVISI: Mengubah list statis menjadi matriks dinamis yang menyaring tombol berdasarkan Role Karyawan
    val items = remember(role) {
        mutableListOf<CategoryItem>().apply {
            // 1. Perawatan (Akses: Admin, Teknik)
            if (role == "Admin" || role == "Teknik") {
                add(CategoryItem("Perawatan", R.drawable.perawatan))
            }
            // 2. Laporan Kerja (Akses: Admin, Teknik)
            if (role == "Admin" || role == "Teknik") {
                add(CategoryItem("Laporan Kerja", R.drawable.laporan))
            }
            // 3. KPI (Akses: Semua Role)
            add(CategoryItem("KPI", R.drawable.kpi))

            // 4. Listrik (Akses: Admin, Teknik)
            if (role == "Admin" || role == "Teknik") {
                add(CategoryItem("Listrik", Icons.Default.ElectricBolt))
            }
            // 5. Stang (Akses: Admin, Teknik, Gudang)
            if (role == "Admin" || role == "Teknik" || role == "Gudang") {
                add(CategoryItem("Stang", R.drawable.stang))
            }
            // 6. Order Kerja (Akses: Admin, Teknik, Operator)
            if (role == "Admin" || role == "Teknik" || role == "Operator") {
                add(CategoryItem("Order Kerja", R.drawable.wo))
            }
            // 7. Part (Akses: Admin, Teknik, Gudang)
            if (role == "Admin" || role == "Teknik" || role == "Gudang") {
                add(CategoryItem("Part", R.drawable.part))
            }
            // 8. Lainnya / Katalog (Akses: Semua Role)
            add(CategoryItem("Lainnya", Icons.Default.Apps))
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        items.chunked(2).forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { item ->
                    CategoryCard(item = item, index = rowIndex * 2 + row.indexOf(item), modifier = Modifier.weight(1f), onClick = {
                        when (item.title) {
                            "Perawatan" -> onPerawatanClick()
                            "Laporan Kerja" -> onLapKerjaClick()
                            "KPI" -> onKPIClick()
                            "Listrik" -> onListrikClick()
                            "Stang" -> onStangClick()
                            "Order Kerja" -> onOrderKerjaClick()
                            "Part" -> onStokPartClick()
                            "Lainnya" -> onNavigateToLainnya()
                            "User Admin" -> onNavigateToManagementUser()
                        }
                    })
                }
                if (row.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun CategoryCard(item: CategoryItem, index: Int = 0, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 55L)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(280)) + slideInVertically(tween(360)) { 24 }
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().height(92.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shadowElevation = 2.dp
        ) {
            Column(
                Modifier.fillMaxSize().padding(13.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    when (val icon = item.icon) {
                        is ImageVector -> Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(23.dp))
                        is Int -> Icon(painterResource(id = icon), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(23.dp))
                    }
                    Icon(Icons.Rounded.NorthEast, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(15.dp))
                }
                Text(
                    item.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun NewsSection() {
    val newsItems = remember { listOf(
        NewsItem("JADWAL LIBUR LEBARAN", "Semarang, 07 Maret 2026 - PT Pabrik Besi Beton Raja Besi mengumumk...", "07 Mar 2026", R.drawable.lebaran1),
        NewsItem("SELAMAT MERAYAKAN HARI RA...", "Semarang, 09 Maret 2026 - PT Pabrik Besi Beton Raja Besi mengucap...", "09 Mar 2026", R.drawable.lebaran1),
        NewsItem("PESAN K3 UNTUK KAR...", "Saat melakukan mudik lebaran pastikan kond...", "09 Mar 2026", R.drawable.pk3),
        NewsItem("PASTIKAN LOKASI KERJA DALAM KON...", "Before starting the long holiday, make sure the con...", "10 Mar 2026", R.drawable.lokerja),
        NewsItem("TIPS MERAWAT MESIN", "Simak tips singkat dari mekanik ahli kami agar mesin tetap awet...", "10 Mar 2026", R.drawable.servis)
    )}
    val pagerState = rememberPagerState(pageCount = { newsItems.size })
    val isPreview = LocalInspectionMode.current

    // REVISI PENYELAMAT: Amankan looping otomatis dengan pelindung try-catch siber
    LaunchedEffect(Unit) {
        if (!isPreview && newsItems.isNotEmpty()) {
            while (true) {
                delay(4000)
                try {
                    val nextPage = (pagerState.currentPage + 1) % newsItems.size
                    pagerState.animateScrollToPage(nextPage)
                } catch (e: Exception) {
                    // Blokir crash jika thread penataan ulang UI belum siap sempurna
                    e.printStackTrace()
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Text(text = "Berita", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp), color = Color.White)
        HorizontalPager(state = pagerState, contentPadding = PaddingValues(end = 64.dp), pageSpacing = 16.dp, modifier = Modifier.fillMaxWidth()) { page ->
            if (page < newsItems.size) {
                NewsCard(newsItems[page])
            }
        }
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
    LaunchedEffect(Unit) {
        if (!isPreview) {
            while (true) {
                delay(3000)
                try {
                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
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
