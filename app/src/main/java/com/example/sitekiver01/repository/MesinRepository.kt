package com.example.sitekiver01.repository

import android.util.Log
import com.example.sitekiver01.model.Mesin
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

class MesinRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val mesinCollection = firestore.collection("master_mesin")

    suspend fun fetchFromSpreadsheet(scriptUrl: String): List<Mesin> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(scriptUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10_000
                connection.readTimeout = 30_000
                
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(response)
                val list = mutableListOf<Mesin>()
                
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(Mesin(
                        kategori = obj.optString("Kategori", obj.optString("kategori", "Mesin")),
                        jenis = obj.optString("Jenis", obj.optString("jenis", "")),
                        nama = obj.optString("Nama", obj.optString("nama_mesin", obj.optString("nama", "")))
                    ))
                }
                list
            } catch (e: Exception) {
                Log.e("MesinRepository", "Error fetch: ${e.message}")
                emptyList()
            }
        }
    }

    suspend fun migrateAll(scriptUrl: String): Result<Int> {
        return try {
            val spreadsheetData = fetchFromSpreadsheet(scriptUrl)
            if (spreadsheetData.isEmpty()) return Result.failure(Exception("Tidak ada data di spreadsheet"))
            
            val batch = firestore.batch()
            spreadsheetData.forEach { mesin ->
                val docRef = mesinCollection.document()
                val data = mapOf(
                    "id" to docRef.id,
                    "Kategori" to mesin.kategori,
                    "Jenis" to mesin.jenis,
                    "Nama" to mesin.nama
                )
                batch.set(docRef, data)
            }
            batch.commit().await()
            memoryCache = emptyList()
            cacheTime = 0L
            Result.success(spreadsheetData.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllMesin(forceRefresh: Boolean = false): List<Mesin> {
        val now = System.currentTimeMillis()
        if (!forceRefresh && memoryCache.isNotEmpty() && now - cacheTime < CACHE_TTL_MS) {
            return memoryCache
        }
        return try {
            val cachedSnapshot = if (!forceRefresh) runCatching {
                mesinCollection.get(Source.CACHE).await()
            }.getOrNull() else null
            val snapshot = if (cachedSnapshot != null && !cachedSnapshot.isEmpty) {
                cachedSnapshot
            } else {
                mesinCollection.get(Source.SERVER).await()
            }
            val result = snapshot.documents.mapNotNull { doc ->
                Mesin(
                    id = doc.id,
                    kategori = doc.getString("Kategori") ?: doc.getString("kategori") ?: "",
                    jenis = doc.getString("Jenis") ?: doc.getString("jenis") ?: "",
                    nama = doc.getString("Nama") ?: doc.getString("nama") ?: ""
                )
            }
            if (result.isNotEmpty()) {
                memoryCache = result
                cacheTime = now
            }
            result
        } catch (e: Exception) {
            memoryCache
        }
    }

    companion object {
        private const val CACHE_TTL_MS = 15 * 60 * 1000L
        @Volatile private var cacheTime = 0L
        @Volatile private var memoryCache: List<Mesin> = emptyList()
    }
}
