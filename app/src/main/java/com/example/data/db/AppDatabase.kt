package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.LineupEntity
import com.example.data.model.TossEntity

import com.example.data.model.PlayerEntity
import com.example.data.model.SessionEntity

@Database(entities = [LineupEntity::class, TossEntity::class, PlayerEntity::class, SessionEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun lineupDao(): LineupDao
    abstract fun tossDao(): TossDao
    abstract fun playerDao(): PlayerDao
    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "missingxi_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
