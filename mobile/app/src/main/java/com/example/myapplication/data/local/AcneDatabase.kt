package com.example.myapplication.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.myapplication.data.model.AcneAnalysis

/**
 * Room Database
 */
@Database(
    entities = [AcneAnalysis::class],
    version = 1,
    exportSchema = false
)
abstract class AcneDatabase : RoomDatabase() {

    abstract fun acneAnalysisDao(): AcneAnalysisDao

    companion object {
        @Volatile
        private var INSTANCE: AcneDatabase? = null

        private const val DATABASE_NAME = "acne_detection_db"

        /**
         * Singleton pattern ile database instance döndürür
         */
        fun getDatabase(context: Context): AcneDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AcneDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}