package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.LineupEntity
import com.example.data.model.TossEntity

@Database(entities = [LineupEntity::class, TossEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun lineupDao(): LineupDao
    abstract fun tossDao(): TossDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "missingxi_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
