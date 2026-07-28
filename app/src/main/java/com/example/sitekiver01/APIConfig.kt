package com.example.sitekiver01

object APIConfig {
    private const val BASE_URL = "https://script.google.com/macros/s/"

    // Endpoint berikut adalah salinan kontrak aktif Siteki_Web.
    const val USERS_URL = "${BASE_URL}AKfycbw2rSZ0GAJJv3PMtrXTeKktphfRYBHzQ4-2NJQ0vWU2_9k329UGC-2uancWWbgFnv4/exec"
    const val LOGIN_URL = USERS_URL
    const val USER_MANAGEMENT_URL = USERS_URL
    const val LEMBURAN_URL = USERS_URL
    const val REKAP_ADMIN_URL = USERS_URL

    const val ORDER_MAIN_URL = "${BASE_URL}AKfycbzbmKFheI55ccsJ_kLdOzy6VIdGpgKIy2s9pljrIM8sNbgJ_RLywnzF-Q2sJTslVQU/exec"
    const val BUAT_ORDER_URL = ORDER_MAIN_URL
    const val PENYELESAIAN_ORDER_URL = ORDER_MAIN_URL
    const val ORDER_LIST_URL = ORDER_MAIN_URL
    const val API_MAIN_URL = ORDER_MAIN_URL

    const val DOWNTIME_URL = "${BASE_URL}AKfycbwXEeFSt5dCP-gPUtSbLX1WCfvPfSe7wJGnMs4vwEt1djVQNVjXxUdv8_ly9uFvM4o/exec"
    const val ORDER_URL = "${BASE_URL}AKfycbxJOKT1yM71bQr1PbJSJ7X6q-RdJ1nmUpjvutRzkBvIYuPbZM2cGh3NuQ0X62GCJkVd/exec"
    const val ORDER_PART_URL = "${BASE_URL}AKfycbwHVQ2pB4rKZXuZTLcffgIAHiRgo4lP_wPCieNNOd2XFdOxhHehcoo5DgxSBd2wUl8/exec"
    const val LAPORAN_URL = "${BASE_URL}AKfycbyXwUhvZfhImtUBGpno8irGpYokkCnYmbx5HcOS88wogDpaUmAdOFKpv_wuoRKET6A/exec"
    const val PART_MASTER_URL = "${BASE_URL}AKfycbyLAKLUbUpzWwuR3KSet3pPyEQhV9d1pWuqduAToyYPeZpQm96AFJM7gPHaL5mTyeum/exec"
    const val MIGRASI_URL = PART_MASTER_URL
    const val MIGRASI_PART_URL = PART_MASTER_URL
    const val STOK_PART_URL = "${BASE_URL}AKfycbxHnZzPQ3jCrMU3tvRGTiQnBIZe7pETN7iWr8e4amU4cdgi22TVzEFjB84ZXUohBDvD/exec"
    const val RAWAT_MASTER_URL = "${BASE_URL}AKfycbwSnaaYVxXWVngeGQYU2im2G5FQ6L7WstjTkx7IW3jVYcuELECt0_cyvM0cFx4Uf8U/exec"
    const val PERAWATAN_URL = "${BASE_URL}AKfycbwQ7ocBNsl4x5-rGLrSyvkyluhSRl3B_LvmkA3cFuvuL9pBbVAOUI3i_Vu6jwfkfOA/exec"
    const val PENCAPAIAN_URL = "${BASE_URL}AKfycbyEO5MruO0r1StkK0iyEoQmfaa3iTZJDCAh4vg9-jdpqItGlt1yuPDe7orWDHwXRyU/exec"
    const val LISTRIK_URL = "${BASE_URL}AKfycbx4YbnLXFsnwDDV-Kso7Lx3Cu2R6tEYBkaEnRM_fnU-RBUoSWo-xZR9DIoHfzjwYd0/exec"
    const val KPI_COMBINED_URL = "${BASE_URL}AKfycbxWrt_-ItPd_61v0uLh1oLn1g0l3v5ov9ApsQFKoNuq8r7OGQIT8yXyRytgx7RSvbM/exec"
    const val TRAVO_URL = "${BASE_URL}AKfycbyaJ1oCCTfcti5u98MYyWP9OBA96SGPEmL_dchslJ9myC4dEv4ku8bZebYAxyqt0aA/exec"
    const val STANG_MAIN_URL = "${BASE_URL}AKfycbw2mqd4JU5ILu85ql2HrEmT4ksv0vR95bo9MqGWwRyXqOWUEdBWk3yYG9CTYXoTF9g/exec"
    const val DETAIL_PERAWATAN_URL = "${BASE_URL}AKfycbzxLaO2nEOxkUmBQnM0jAYzay7GGKBnOjhR3Afk9ZLUadK145ZdvOE-0NvIJ55EFKs/exec"
    const val INSPEKSI_URL = "${BASE_URL}AKfycbyX0U2MaTrjBTZjLkTH64E3bIXg2lyHhtPdTJ1QbEFco34m3FK18gDDE0Lqk7ja-k-C/exec"
    const val STANG_SCREEN_URL = STANG_MAIN_URL
}
