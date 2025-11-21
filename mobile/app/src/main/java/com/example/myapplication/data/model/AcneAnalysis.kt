package com.example.myapplication.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.*

/**
 * Akne analiz sonuçlarını saklayan veri modeli
 * Room database entity'si
 */
@Entity(tableName = "acne_analysis")
data class AcneAnalysis(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Analiz tarihi (milisaniye)
    val timestamp: Long = System.currentTimeMillis(),

    // Fotoğraf yolu (yerel depolama)
    val imagePath: String,

    // Model tahmini (0: Akne Yok, 1: Akne Var)
    val prediction: Int,

    // Güven skoru (0.0 - 1.0)
    val confidence: Float,

    // Kullanıcı notu (opsiyonel)
    val userNote: String? = null
) {
    /**
     * Güven skorunu yüzdelik formata çevirir
     */
    fun getConfidencePercentage(): Int = (confidence * 100).toInt()

    /**
     * Tahmin sonucunu okunabilir string'e çevirir
     */
    fun getPredictionText(): String = when (prediction) {
        0 -> "Akne Tespit Edilmedi "
        1 -> "Akne Tespit Edildi "
        else -> "Bilinmiyor"
    }

    /**
     * Tarihi formatlar
     */
    fun getFormattedDate(): String {
        val date = Date(timestamp)
        val format = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("tr", "TR"))
        return format.format(date)
    }
}