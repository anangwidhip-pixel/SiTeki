package com.example.sitekiver01

data class DataTravo(
    val kode: String,
    val nama: String,
    val merk: String,
    val tipe: String,
    val tegangan: String,
    val pengadaan: String
)

// Model untuk data hasil inspeksi
data class DataInspeksi(
    val no: String,
    val tanggal: String,
    val kode: String,
    val nama: String,
    val lokasi: String,
    val kondisi: String,
    val status: String,
    val stang: String,
    val kabel: String,
    val masa: String,
    val keterangan: String
)