package com.example.sitekiver01.screens

// Import Komponen UI & Material
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Import Tema & Komponen Custom Anda
import com.example.sitekiver01.OrbitronFontFamily
import com.example.sitekiver01.APIConfig
import com.example.sitekiver01.UserSession
import com.example.sitekiver01.components.*
import com.example.sitekiver01.ui.theme.*

// Import Coroutines & Networking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar

data class RekapUser(
    val nama: String,
    val role: String,
    val jumlahData: Int,
    val totalJam: Double,
    val totalUpah: Double
)

@Composable
fun RekapAdminScreen(onBack: () -> Unit) {
    var rekapList by remember { mutableStateOf<List<RekapUser>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val connection = URL(APIConfig.USER_MANAGEMENT_URL).openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.instanceFollowRedirects = true
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                val payload = JSONObject()
                    .put("action", "getOvertimeAdmin")
                    .put("token", UserSession.token)
                    .put("year", Calendar.getInstance().get(Calendar.YEAR))
                    .put("month", 0)
                connection.outputStream.use {
                    it.write(payload.toString().toByteArray(Charsets.UTF_8))
                }
                val response = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                if (!response.optString("status").equals("success", ignoreCase = true)) {
                    throw IllegalStateException(response.optString("message", "Akses rekap ditolak"))
                }
                val arr = response.optJSONArray("data") ?: org.json.JSONArray()
                val list = mutableListOf<RekapUser>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(RekapUser(
                        obj.optString("nama", "Tanpa Nama"),
                        obj.optString("role", "-"),
                        obj.optInt("jumlahData", 0),
                        obj.optDouble("totalJam", 0.0),
                        obj.optDouble("totalUpah", 0.0)
                    ))
                }
                withContext(Dispatchers.Main) {
                    rekapList = list
                    isLoading = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { isLoading = false }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Background Global yang sama dengan halaman order
        SciFiBackground()

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
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)
                    ) { Icon(Icons.Default.ArrowBackIosNew, "Back", tint = Color.White) }
                    Spacer(Modifier.width(16.dp))
                    Text(
                        "REKAP TOTAL UPAH",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        fontFamily = OrbitronFontFamily,
                        letterSpacing = 1.sp
                    )
                }
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(12.dp))
                ModernSectionHeader("DATA AKTIF KARYAWAN", Icons.AutoMirrored.Filled.List)
                Spacer(Modifier.height(12.dp))

                if (isLoading) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator(color = SciFiCyan)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 20.dp)
                    ) {
                        items(rekapList) { user ->
                            // Glass Card yang seragam
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                color = SciFiGlass,
                                border = BorderStroke(1.dp, Brush.verticalGradient(listOf(SciFiBorderLight, Color.Transparent)))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Ikon Profil
                                    Box(Modifier.size(40.dp).background(SciFiCyan.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = SciFiCyan)
                                    }

                                    Spacer(Modifier.width(16.dp))

                                    Column(Modifier.weight(1f)) {
                                        Text(user.nama, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                                        Text(
                                            "${user.role} · ${user.jumlahData} kali · ${String.format("%.2f", user.totalJam)} jam",
                                            color = SciFiTextMuted,
                                            fontSize = 11.sp,
                                            fontFamily = OrbitronFontFamily
                                        )
                                    }

                                    Text(
                                        "Rp ${String.format("%,d", user.totalUpah.toLong())}",
                                        color = SciFiCyan,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        fontFamily = OrbitronFontFamily
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
