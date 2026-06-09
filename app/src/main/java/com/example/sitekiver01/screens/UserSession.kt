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

    // Tambahkan fungsi untuk update session setelah login
    fun updateSession(user: String, nama: String, userRole: String) {
        isLoggedIn = true
        username = user
        namaFull = nama
        role = userRole
    }

    // Fungsi untuk membersihkan session saat user keluar dari aplikasi
    fun logout() {
        isLoggedIn = false
        username = ""
        namaFull = ""
        role = "Lainnya"
    }
}