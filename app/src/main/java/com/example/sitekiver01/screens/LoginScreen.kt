package com.example.sitekiver01.screens

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sitekiver01.OrbitronFontFamily
import com.example.sitekiver01.UserSession
import com.example.sitekiver01.components.*
import com.example.sitekiver01.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var usernameInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isAuthenticating by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(GlassBase)) {
        // Efek background mesh grid bawaan SiTeki
        SciFiBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "SITEKI SYSTEM",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = SciFiCyan,
                fontFamily = OrbitronFontFamily,
                letterSpacing = 2.sp
            )
            Text(
                text = "PT. PABRIK BESI BETON RAJA BESI",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.6f),
                fontFamily = OrbitronFontFamily
            )

            Spacer(modifier = Modifier.height(32.dp))

            ModernSectionHeader("OTENTIKASI TERMINAL ACCESS", Icons.Default.VerifiedUser)

            GlassCard {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    // FIELD INPUT USERNAME
                    Column {
                        Text("ID USERNAME INTERFACES", fontSize = 10.sp, color = SciFiCyan, fontWeight = FontWeight.Bold, fontFamily = OrbitronFontFamily)
                        OutlinedTextField(
                            value = usernameInput,
                            onValueChange = { usernameInput = it },
                            leadingIcon = { Icon(Icons.Default.Person, null, tint = SciFiCyan) },
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SciFiCyan,
                                unfocusedBorderColor = SciFiBorderLight,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }

                    // FIELD INPUT PASSWORD
                    Column {
                        Text("SECURE KEYCODE PASSWORD", fontSize = 10.sp, color = SciFiCyan, fontWeight = FontWeight.Bold, fontFamily = OrbitronFontFamily)
                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            leadingIcon = { Icon(Icons.Default.Lock, null, tint = SciFiCyan) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        tint = SciFiTextMuted
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SciFiCyan,
                                unfocusedBorderColor = SciFiBorderLight,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // TOMBOL LOGIN INITIALIZATION
            Button(
                onClick = {
                    if (usernameInput.isEmpty() || passwordInput.isEmpty()) {
                        Toast.makeText(context, "Username & Password wajib diisi!", Toast.LENGTH_SHORT).show()
                    } else {
                        scope.launch {
                            isAuthenticating = true
                            val loginResult = melakukanProsesLogin(usernameInput, passwordInput)
                            isAuthenticating = false

                            if (loginResult != null && loginResult.optString("status") == "success") {
                                try {
                                    // REVISI PENYELAMAT: Gunakan optJSONObject agar anti-crash jika properti null
                                    val profile = loginResult.optJSONObject("profile")

                                    if (profile != null) {
                                        // Amankan data ke Session Manager Lokal Android
                                        UserSession.updateSession(
                                            user = profile.optString("username", usernameInput),
                                            nama = profile.optString("nama", "Karyawan Raja Besi"),
                                            userRole = profile.optString("role", "Operator")
                                        ) // Menangkap: Admin, Teknik, dll.

                                        Toast.makeText(context, "Akses Diberikan: ${UserSession.namaFull}", Toast.LENGTH_SHORT).show()

                                        // Trigger callback untuk masuk ke halaman Dashboard Utama
                                        onLoginSuccess()
                                    } else {
                                        Toast.makeText(context, "Data profil tidak ditemukan dari server!", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (jsonException: Exception) {
                                    jsonException.printStackTrace()
                                    Toast.makeText(context, "Format Data Profil Rusak!", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                // Jika loginResult null atau status != success
                                val errorMsg = loginResult?.optString("message") ?: "Terminal Gagal Terhubung ke Server!"
                                Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SciFiCyan)
            ) {
                if (isAuthenticating) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(22.dp))
                } else {
                    Text(
                        text = "INITIALIZE CONNECTION",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontFamily = OrbitronFontFamily,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

/**
 * Engine Koneksi HTTP untuk Verifikasi Kredensial ke Google Apps Script
 */
private suspend fun melakukanProsesLogin(user: String, pass: String): JSONObject? {
    return withContext(Dispatchers.IO) {
        try {
            // REVISI LANGSUNG: Tempel URL Web App Apps Script Anda di sini agar tidak memanggil variabel luar
            val urlUtama = "https://script.google.com/macros/s/AKfycbzzdAkBlB9PVR3aAtPmkuZ5OCO9Cz31S-zlGiQfPcPsUkhTfjsHQt6gasEO4qNvSFU/exec"

            val urlString = "$urlUtama?action=login" +
                    "&username=${URLEncoder.encode(user, "UTF-8")}" +
                    "&password=${URLEncoder.encode(pass, "UTF-8")}"

            val response = URL(urlString).readText().trim()
            JSONObject(response)
        } catch (e: Exception) {
            // Menggunakan fungsi bawaan standar Kotlin, dijamin 100% bebas error 'Unresolved reference'
            println("SiTekiLoginError: Gagal menembak server: ${e.message}")
            null
        }
    }
}