package com.example.sitekiver01.model

data class ActualRecord(
    val nama_mesin: String = "",
    val tanggal: String = "",
    val jenis_perawatan: String = "" // "M" atau "B"
)

data class MaintenanceRule(
    val dayOfWeek: Int, // 1 (Senin) - 6 (Sabtu)
    val bWeeks: List<Int>
)
