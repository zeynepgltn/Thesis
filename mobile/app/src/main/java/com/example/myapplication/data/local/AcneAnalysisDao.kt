package com.example.myapplication.data.local

import androidx.room.*
import com.example.myapplication.data.model.AcneAnalysis
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO (Data Access Object)
 * Veritabanı işlemlerini tanımlar
 */
@Dao
interface AcneAnalysisDao {

    /**
     * Yeni analiz sonucu ekler
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalysis(analysis: AcneAnalysis): Long

    /**
     * Tüm analiz sonuçlarını getirir (tarihe göre azalan)
     */
    @Query("SELECT * FROM acne_analysis ORDER BY timestamp DESC")
    fun getAllAnalyses(): Flow<List<AcneAnalysis>>

    /**
     * Son N günlük analizleri getirir
     */
    @Query("SELECT * FROM acne_analysis WHERE timestamp >= :daysBefore ORDER BY timestamp DESC")
    fun getRecentAnalyses(daysBefore: Long): Flow<List<AcneAnalysis>>

    /**
     * ID'ye göre tek bir analiz getirir
     */
    @Query("SELECT * FROM acne_analysis WHERE id = :id")
    suspend fun getAnalysisById(id: Long): AcneAnalysis?

    /**
     * Toplam analiz sayısını döndürür
     */
    @Query("SELECT COUNT(*) FROM acne_analysis")
    fun getTotalCount(): Flow<Int>

    /**
     * Belirli bir tahmine sahip kayıt sayısını döndürür
     */
    @Query("SELECT COUNT(*) FROM acne_analysis WHERE prediction = :prediction")
    fun getCountByPrediction(prediction: Int): Flow<Int>

    /**
     * Analiz siler
     */
    @Delete
    suspend fun deleteAnalysis(analysis: AcneAnalysis)

    /**
     * ID'ye göre analiz siler
     */
    @Query("DELETE FROM acne_analysis WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * Tüm analizleri siler (GİZLİLİK özelliği)
     */
    @Query("DELETE FROM acne_analysis")
    suspend fun deleteAll()
}