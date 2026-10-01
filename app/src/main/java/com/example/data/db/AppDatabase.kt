package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.LineupEntity
import com.example.data.model.TossEntity
import com.example.data.model.PlayerEntity
import com.example.data.model.SessionEntity

@Database(entities = [LineupEntity::class, TossEntity::class, PlayerEntity::class, SessionEntity::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun lineupDao(): LineupDao
    abstract fun tossDao(): TossDao
    abstract fun playerDao(): PlayerDao
    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `players` (`id` TEXT NOT NULL, `displayName` TEXT NOT NULL, `nickname` TEXT, `createdAt` INTEGER NOT NULL, `lastUsedAt` INTEGER NOT NULL, `totalMatches` INTEGER NOT NULL, `totalTimesJoker` INTEGER NOT NULL, `isFavorite` INTEGER NOT NULL, `isArchived` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `playerIdsJson` TEXT NOT NULL, `teamCount` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL)")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `players` ADD COLUMN `skillRating` INTEGER NOT NULL DEFAULT 5")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `players` ADD COLUMN `matchesPlayed` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `players` ADD COLUMN `matchesAsJoker` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `players` ADD COLUMN `totalWins` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `players` ADD COLUMN `totalLosses` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `players` ADD COLUMN `longestGapSincePlayed` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `players` ADD COLUMN `currentPlayStreak` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `players` ADD COLUMN `totalTeammates` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `players` ADD COLUMN `totalOpponents` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `players` ADD COLUMN `favoriteTeammateId` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `players` ADD COLUMN `favoriteOpponentId` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `players` ADD COLUMN `lastPlayedAt` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `players` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")

                // Safely migrate existing match data
                db.execSQL("UPDATE `players` SET `matchesPlayed` = `totalMatches`, `matchesAsJoker` = `totalTimesJoker`, `lastPlayedAt` = `lastUsedAt`, `updatedAt` = `createdAt` WHERE `matchesPlayed` = 0")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `overallFairnessScore` REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `fairnessRating` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `teammateVarietyScore` REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `opponentVarietyScore` REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `teamStrengthScore` REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `jokerFairnessScore` REAL NOT NULL DEFAULT 0.0")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `fairnessProfile` TEXT NOT NULL DEFAULT 'Legacy Result'")
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `settingsSnapshotJson` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `fairnessThresholdUsed` REAL NOT NULL DEFAULT 70.0")
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `qualityGateOutcome` TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "missingxi_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                .fallbackToDestructiveMigration(dropAllTables = false)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
