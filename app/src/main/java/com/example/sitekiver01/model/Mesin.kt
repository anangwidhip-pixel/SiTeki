package com.example.sitekiver01.model

import com.google.firebase.firestore.PropertyName

data class Mesin(
    var id: String = "",
    @get:PropertyName("Kategori") @set:PropertyName("Kategori") var kategori: String = "",
    @get:PropertyName("Jenis") @set:PropertyName("Jenis") var jenis: String = "",
    @get:PropertyName("Nama") @set:PropertyName("Nama") var nama: String = ""
)
