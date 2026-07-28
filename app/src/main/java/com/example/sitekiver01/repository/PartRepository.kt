package com.example.sitekiver01.repository

import android.util.Log
import com.example.sitekiver01.APIConfig
import com.example.sitekiver01.UserSession
import com.example.sitekiver01.model.Part
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class PartRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val partCollection = firestore.collection("master_part")

    suspend fun fetchFromSpreadsheet(scriptUrl: String): List<Part> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(scriptUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10_000
                connection.readTimeout = 30_000
                
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                
                val list = mutableListOf<Part>()
                
                if (response.trim().startsWith("[")) {
                    val jsonArray = JSONArray(response)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.get(i)
                        if (obj is JSONObject) {
                            list.add(parsePartFromObject(obj))
                        } else if (obj is JSONArray) {
                            list.add(parsePartFromArray(obj))
                        }
                    }
                } else if (response.trim().startsWith("{")) {
                    val jsonObject = JSONObject(response)
                    // Mendukung format { "stok": [...] } atau { "data": [...] }
                    val array = jsonObject.optJSONArray("stok") ?: jsonObject.optJSONArray("data") ?: jsonObject.optJSONArray("parts")
                    if (array != null) {
                        for (i in 0 until array.length()) {
                            val obj = array.get(i)
                            if (obj is JSONObject) {
                                list.add(parsePartFromObject(obj))
                            } else if (obj is JSONArray) {
                                list.add(parsePartFromArray(obj))
                            }
                        }
                    }
                }

                val filteredList = list.filter { it.kategori.isNotEmpty() || it.nama.isNotEmpty() }
                Log.d("PartRepository", "Parsed ${filteredList.size} parts from spreadsheet")
                filteredList
            } catch (e: Exception) {
                Log.e("PartRepository", "Error fetchFromSpreadsheet: ${e.message}")
                emptyList()
            }
        }
    }

    private fun parsePartFromObject(obj: JSONObject): Part {
        return Part(
            kategori = obj.optString("Kategori", obj.optString("kategori", "")),
            nama = obj.optString("Nama", obj.optString("nama", obj.optString("nama_part", obj.optString("Nama Part", "")))),
            ukuran = obj.optString("Ukuran", obj.optString("ukuran", obj.optString("spesifikasi", obj.optString("Ukuran/Kode", "")))),
            jenisKomponen = obj.optString("Jenis Komponen", obj.optString("jenis_komponen", obj.optString("jenisKomponen", "")))
        )
    }

    private fun parsePartFromArray(array: JSONArray): Part {
        // Struktur sheet Part: [Kategori, Nama, Ukuran/Kode, Jenis Komponen]
        return Part(
            kategori = array.optString(0, ""),
            nama = array.optString(1, ""),
            ukuran = array.optString(2, ""),
            jenisKomponen = array.optString(3, "")
        )
    }

    suspend fun migrateAll(scriptUrl: String): Result<Int> {
        return try {
            val spreadsheetData = fetchFromSpreadsheet(scriptUrl)
            if (spreadsheetData.isEmpty()) {
                Log.e("PartRepository", "Migration failed: Spreadsheet data is empty")
                return Result.failure(Exception("Tidak ada data di spreadsheet atau format JSON salah. Cek Logcat!"))
            }
            
            // Opsional: Hapus data lama agar sinkron total
            try {
                val oldDocs = partCollection.get().await()
                if (!oldDocs.isEmpty) {
                    val batchDelete = firestore.batch()
                    oldDocs.documents.forEach { batchDelete.delete(it.reference) }
                    batchDelete.commit().await()
                }
            } catch (e: Exception) { Log.e("PartRepository", "Delete old docs failed: ${e.message}") }

            val batch = firestore.batch()
            spreadsheetData.forEach { part ->
                val docRef = partCollection.document()
                val data = mapOf(
                    "id" to docRef.id,
                    "Kategori" to part.kategori,
                    "Nama" to part.nama,
                    "Ukuran" to part.ukuran,
                    "Jenis Komponen" to part.jenisKomponen
                )
                batch.set(docRef, data)
            }
            batch.commit().await()
            memoryCache = emptyList()
            cacheTime = 0L
            Log.d("PartRepository", "Migration successful: ${spreadsheetData.size} documents written")
            Result.success(spreadsheetData.size)
        } catch (e: Exception) {
            Log.e("PartRepository", "Error migrateAll: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getAllPart(forceRefresh: Boolean = false): List<Part> {
        val now = System.currentTimeMillis()
        if (!forceRefresh && memoryCache.isNotEmpty() && now - cacheTime < CACHE_TTL_MS) {
            return memoryCache
        }
        return try {
            val cachedSnapshot = if (!forceRefresh) runCatching {
                partCollection.get(Source.CACHE).await()
            }.getOrNull() else null
            val snapshot = if (cachedSnapshot != null && !cachedSnapshot.isEmpty) {
                cachedSnapshot
            } else {
                partCollection.get(Source.SERVER).await()
            }
            Log.d("PartRepository", "Fetched ${snapshot.size()} documents from master_part")
            
            val result = snapshot.documents.mapNotNull { doc ->
                try {
                    Part(
                        id = doc.id,
                        kategori = doc.getString("Kategori") ?: doc.getString("kategori") ?: "",
                        nama = doc.getString("Nama") ?: doc.getString("nama") ?: "",
                        ukuran = doc.getString("Ukuran") ?: doc.getString("ukuran") ?: "",
                        jenisKomponen = doc.getString("Jenis Komponen") ?: doc.getString("jenis_komponen") ?: doc.getString("jenisKomponen") ?: ""
                    )
                } catch (e: Exception) {
                    null
                }
            }
            if (result.isNotEmpty()) {
                memoryCache = result
                cacheTime = now
            }
            result
        } catch (e: Exception) {
            Log.e("PartRepository", "Error getAllPart: ${e.message}")
            memoryCache
        }
    }

    suspend fun addPart(part: Part): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                require(UserSession.token.isNotBlank()) {
                    "Sesi Admin tidak tersedia. Silakan masuk ulang."
                }
                val payload = JSONObject().apply {
                    put("action", "addMasterPart")
                    put("token", UserSession.token)
                    put("kategori", part.kategori.trim())
                    put("nama", part.nama.trim())
                    put("ukuran", part.ukuran.trim())
                    put("jenisKomponen", part.jenisKomponen.trim())
                    put("satuan", "Pcs")
                    put("stokAwal", 0)
                }
                val connection = (URL(APIConfig.LAPORAN_URL).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 15_000
                    readTimeout = 90_000
                    doOutput = true
                    setRequestProperty("Content-Type", "text/plain;charset=UTF-8")
                }
                connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(payload.toString()) }
                val responseCode = connection.responseCode
                val responseText = (if (responseCode in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                val result = JSONObject(responseText.ifBlank { "{}" })
                val success = responseCode in 200..299 &&
                    (result.optBoolean("success") || result.optString("status").equals("success", true)) &&
                    result.optBoolean("firestoreSynced") &&
                    result.optBoolean("stockSynced")
                if (!success) {
                    throw IllegalStateException(
                        result.optString("message").ifBlank {
                            "Part belum tersinkron ke Firestore dan tab Stok."
                        }
                    )
                }
                memoryCache = emptyList()
                cacheTime = 0L
                true
            } catch (e: Exception) {
                Log.e("PartRepository", "Error addPart via backend: ${e.message}", e)
                false
            }
        }
    }

    companion object {
        private const val CACHE_TTL_MS = 15 * 60 * 1000L
        @Volatile private var cacheTime = 0L
        @Volatile private var memoryCache: List<Part> = emptyList()
    }
}
