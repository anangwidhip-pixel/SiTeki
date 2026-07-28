package com.example.sitekiver01.screens

import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sitekiver01.OrbitronFontFamily
import com.example.sitekiver01.APIConfig
import com.example.sitekiver01.UserSession
import com.example.sitekiver01.components.GlassCard
import com.example.sitekiver01.components.ModernSectionHeader
import com.example.sitekiver01.components.SciFiBackground
import com.example.sitekiver01.ui.theme.GlassBase
import com.example.sitekiver01.ui.theme.SciFiBorderLight
import com.example.sitekiver01.ui.theme.SciFiCyan
import com.example.sitekiver01.ui.theme.SciFiTextMuted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var usernameInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isAuthenticating by remember { mutableStateOf(false) }

    val loadingMessages = remember {
        listOf(
            "Menghubungkan ke server SiTeki",
            "Memverifikasi username dan password",
            "Membaca profil dan database pengguna",
            "Menyiapkan sesi aplikasi"
        )
    }

    var loadingMessageIndex by remember {
        mutableIntStateOf(0)
    }

    /*
     * Status loading akan berganti selama request login masih berjalan.
     * LinearProgressIndicator tetap bersifat indeterminate karena durasi
     * respons server tidak dapat diketahui secara tepat.
     */
    LaunchedEffect(isAuthenticating) {
        if (isAuthenticating) {
            loadingMessageIndex = 0

            while (true) {
                delay(950)
                loadingMessageIndex =
                    (loadingMessageIndex + 1) % loadingMessages.size
            }
        } else {
            loadingMessageIndex = 0
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        SciFiBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary
            ) {
                Text(
                    "ST",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(34.dp))

            Text(
                text = "Masuk ke\nruang kerja.",
                fontSize = 40.sp,
                lineHeight = 43.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = (-1.2).sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Gunakan akun SiTeki untuk melanjutkan pekerjaan.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(30.dp))

            GlassCard {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Column {
                        Text(
                            text = "Username",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = usernameInput,
                            onValueChange = {
                                usernameInput = it
                            },
                            enabled = !isAuthenticating,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .height(50.dp),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledTextColor =
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                disabledBorderColor =
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                            )
                        )
                    }

                    Column {
                        Text(
                            text = "Password",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = {
                                passwordInput = it
                            },
                            enabled = !isAuthenticating,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = {
                                IconButton(
                                    enabled = !isAuthenticating,
                                    onClick = {
                                        passwordVisible = !passwordVisible
                                    }
                                ) {
                                    Icon(
                                        imageVector =
                                            if (passwordVisible) {
                                                Icons.Default.Visibility
                                            } else {
                                                Icons.Default.VisibilityOff
                                            },
                                        contentDescription =
                                            if (passwordVisible) {
                                                "Sembunyikan password"
                                            } else {
                                                "Tampilkan password"
                                            },
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            visualTransformation =
                                if (passwordVisible) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .height(50.dp),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledTextColor =
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                disabledBorderColor =
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                enabled = !isAuthenticating,
                onClick = {
                    val username = usernameInput.trim()
                    val password = passwordInput

                    if (username.isEmpty() || password.isEmpty()) {
                        Toast.makeText(
                            context,
                            "Username & Password wajib diisi!",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        scope.launch {
                            isAuthenticating = true

                            try {
                                val loginResult =
                                    melakukanProsesLogin(
                                        user = username,
                                        pass = password
                                    )

                                if (
                                    loginResult != null &&
                                    loginResult.optString("status")
                                        .equals("success", ignoreCase = true)
                                ) {
                                    val profile =
                                        loginResult.optJSONObject("profile")

                                    if (profile != null) {
                                        UserSession.updateSession(
                                            user = profile.optString(
                                                "username",
                                                username
                                            ),
                                            nama = profile.optString(
                                                "nama",
                                                "Karyawan Raja Besi"
                                            ),
                                            userRole = profile.optString(
                                                "role",
                                                "Operator"
                                            ),
                                            sessionToken = loginResult.optString("token", "")
                                        )

                                        Toast.makeText(
                                            context,
                                            "Akses Diberikan: " +
                                                    UserSession.namaFull,
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        onLoginSuccess()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Data profil tidak ditemukan dari server!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } else {
                                    val errorMessage =
                                        loginResult?.optString(
                                            "message"
                                        )?.takeIf {
                                            it.isNotBlank()
                                        } ?: "Terminal gagal terhubung ke server!"

                                    Toast.makeText(
                                        context,
                                        errorMessage,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } catch (exception: Exception) {
                                exception.printStackTrace()

                                Toast.makeText(
                                    context,
                                    "Terjadi kesalahan saat membaca data login.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } finally {
                                isAuthenticating = false
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor =
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                )
            ) {
                if (isAuthenticating) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(21.dp)
                    )

                    Spacer(modifier = Modifier.size(10.dp))

                    Text(
                        text = "Memverifikasi...",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                } else {
                    Text(
                        text = "Masuk ke SiTeki",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        if (isAuthenticating) {
            LoginLoadingOverlay(
                message = loadingMessages[loadingMessageIndex]
            )
        }
    }
}

/**
 * Overlay loading modern yang muncul selama proses login berlangsung.
 */
@Composable
private fun LoginLoadingOverlay(
    message: String
) {
    val infiniteTransition =
        rememberInfiniteTransition(label = "loginLoading")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850),
            repeatMode = RepeatMode.Reverse
        ),
        label = "loginPulse"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650),
            repeatMode = RepeatMode.Reverse
        ),
        label = "loginPulseAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 18.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = 22.dp,
                    vertical = 25.dp
                ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                        }
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                                    MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(58.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp
                    )

                    Text(
                        text = "DB",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = OrbitronFontFamily
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Memverifikasi akun",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = OrbitronFontFamily,
                    letterSpacing = 1.1.sp
                )

                Spacer(modifier = Modifier.height(9.dp))

                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(50)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha)
                            )
                    )

                    Spacer(modifier = Modifier.size(9.dp))

                    Text(
                        text = "KONEKSI SERVER AKTIF",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 8.sp,
                        fontFamily = OrbitronFontFamily,
                        letterSpacing = 0.7.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Mohon tunggu dan jangan menutup aplikasi",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Koneksi HTTP untuk memverifikasi kredensial melalui Google Apps Script.
 */
private suspend fun melakukanProsesLogin(
    user: String,
    pass: String
): JSONObject? {
    return withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null

        try {
            connection = URL(APIConfig.USER_MANAGEMENT_URL).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 20_000
            connection.readTimeout = 30_000
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            connection.setRequestProperty(
                "Accept",
                "application/json"
            )
            val payload = JSONObject()
                .put("action", "login")
                .put("username", user)
                .put("password", pass)
            connection.outputStream.use {
                it.write(payload.toString().toByteArray(Charsets.UTF_8))
            }

            val responseCode = connection.responseCode

            val responseText =
                if (responseCode in 200..299) {
                    connection.inputStream
                        .bufferedReader()
                        .use { it.readText() }
                } else {
                    connection.errorStream
                        ?.bufferedReader()
                        ?.use { it.readText() }
                        .orEmpty()
                }
                    .trim()

            if (responseText.isBlank()) {
                null
            } else {
                JSONObject(responseText)
            }
        } catch (exception: Exception) {
            println(
                "SiTekiLoginError: Gagal terhubung ke server: " +
                        exception.message
            )
            null
        } finally {
            connection?.disconnect()
        }
    }
}
