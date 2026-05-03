package com.example.sitekiver01.repository

import android.util.Log
import com.example.sitekiver01.model.Mesin
import com.google.firebase.firestore.FirebaseFirestore
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
            Result.success(spreadsheetData.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllMesin(): List<Mesin> {
        return try {
            val snapshot = mesinCollection.get().await()
            snapshot.documents.mapNotNull { doc ->
                Mesin(
                    id = doc.id,
                    kategori = doc.getString("Kategori") ?: doc.getString("kategori") ?: "",
                    jenis = doc.getString("Jenis") ?: doc.getString("jenis") ?: "",
                    nama = doc.getString("Nama") ?: doc.getString("nama") ?: ""
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
