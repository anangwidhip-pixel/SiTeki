package com.example.sitekiver01

/**
 * Session Manager Global untuk Aplikasi SiTeki
 * Menyimpan data sirkulasi hak akses siber pengguna aktif
 */
object UserSession {
    var isLoggedIn: Boolean = false; private set
    var username: String = "" ; private set
    var namaFull: String = "" ; private set
    var role: String = "Lainnya" ; private set
    var token: String = "" ; private set

    // Tambahkan fungsi untuk update session setelah login
    fun updateSession(user: String, nama: String, userRole: String, sessionToken: String = "") {
        isLoggedIn = true
        username = user
        namaFull = nama
        role = userRole
        token = sessionToken
    }

    // Fungsi untuk membersihkan session saat user keluar dari aplikasi
    fun logout() {
        isLoggedIn = false
        username = ""
        namaFull = ""
        role = "Lainnya"
        token = ""
    }
}
