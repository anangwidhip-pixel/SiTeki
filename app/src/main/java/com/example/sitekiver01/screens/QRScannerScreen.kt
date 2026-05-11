package com.example.sitekiver01.screens

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

@Composable
fun QRScannerScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ScanContract()
    ) { result ->
        if (result.contents != null) {
            val scannedText = result.contents
            Log.d("QRScanner", "Scanned: $scannedText")

            // Handle Deep Link atau Web Link
            if (scannedText.startsWith("siteki://") || scannedText.contains("script.google.com")) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(scannedText)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                context.startActivity(intent)
            } else {
                // Jika scan biasa
                Log.d("QRScanner", "Hasil scan biasa: $scannedText")
            }
        } else {
            onBack()
        }
    }

    // Mulai scan otomatis
    LaunchedEffect(Unit) {
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt("Arahkan ke QR Code")
            setCameraId(0)
            setBeepEnabled(true)
            setBarcodeImageEnabled(true)
            setOrientationLocked(false)
        }
        scannerLauncher.launch(options)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Sedang memindai QR Code...")
    }
}