package com.example.sitekiver01.model

import com.google.firebase.firestore.PropertyName

data class Part(
    var id: String = "",
    @get:PropertyName("Kategori") @set:PropertyName("Kategori") var kategori: String = "",
    @get:PropertyName("Nama") @set:PropertyName("Nama") var nama: String = "",
    @get:PropertyName("Ukuran") @set:PropertyName("Ukuran") var ukuran: String = "",
    @get:PropertyName("Jenis Komponen") @set:PropertyName("Jenis Komponen") var jenisKomponen: String = ""
)
