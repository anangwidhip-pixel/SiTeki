package com.example.sitekiver01.repository

import android.util.Log
import com.example.sitekiver01.model.Part
import com.google.firebase.firestore.FirebaseFirestore
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
                
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d("PartRepository", "Response from spreadsheet: $response")
                
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
        // Berdasarkan struktur umum spreadsheet: [index, Kategori, Nama, Ukuran, ..., Jenis Komponen]
        // Jika spreadsheet mengembalikan baris sebagai array
        return Part(
            kategori = array.optString(1, ""),
            nama = array.optString(2, ""),
            ukuran = array.optString(3, ""),
            jenisKomponen = array.optString(array.length() - 1, "")
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
            Log.d("PartRepository", "Migration successful: ${spreadsheetData.size} documents written")
            Result.success(spreadsheetData.size)
        } catch (e: Exception) {
            Log.e("PartRepository", "Error migrateAll: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getAllPart(): List<Part> {
        return try {
            val snapshot = partCollection.get().await()
            Log.d("PartRepository", "Fetched ${snapshot.size()} documents from master_part")
            
            snapshot.documents.mapNotNull { doc ->
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
        } catch (e: Exception) {
            Log.e("PartRepository", "Error getAllPart: ${e.message}")
            emptyList()
        }
    }

    suspend fun addPart(part: Part): Boolean {
        return try {
            val docRef = partCollection.document()
            part.id = docRef.id
            val data = mapOf(
                "Kategori" to part.kategori,
                "Nama" to part.nama,
                "Ukuran" to part.ukuran,
                "Jenis Komponen" to part.jenisKomponen
            )
            docRef.set(data).await()
            true
        } catch (e: Exception) {
            Log.e("PartRepository", "Error addPart: ${e.message}")
            false
        }
    }
}
