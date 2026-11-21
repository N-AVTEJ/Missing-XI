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

@Database(entities = [LineupEntity::class, TossEntity::class, PlayerEntity::class, SessionEntity::class], version = 11, exportSchema = false)
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

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `lockedPlayerIdsJson` TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `fixedGroupsJson` TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `excludedPlayerIdsJson` TEXT NOT NULL DEFAULT '[]'")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `wasManuallyEdited` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `originalArrangementJson` TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `tosses` ADD COLUMN `tossUuid` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `tosses` ADD COLUMN `matchId` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `tosses` ADD COLUMN `attemptNumber` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `tosses` ADD COLUMN `selectedBy` TEXT NOT NULL DEFAULT 'USER'")
                db.execSQL("ALTER TABLE `tosses` ADD COLUMN `status` TEXT NOT NULL DEFAULT 'COMPLETED'")
                db.execSQL("UPDATE `tosses` SET `tossUuid` = 'legacy_toss_' || `id` WHERE `tossUuid` = ''")

                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `matchId` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `matchStatus` TEXT NOT NULL DEFAULT 'TEAMS_GENERATED'")
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `finalTeamsJson` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `completedAt` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `latestTossChoice` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `latestTossResult` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `latestTossWon` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE `sessions` SET `matchId` = 'legacy_match_' || `id` WHERE `matchId` = ''")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `matchNumber` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `totalEligiblePlayers` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `teammatePenalty` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `opponentPenalty` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `teamStrengthDifference` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `excludedPlayerNamesJson` TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `hasFairnessBreakdown` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE `sessions` SET `hasFairnessBreakdown` = 1 WHERE `overallFairnessScore` > 0.0 AND `fairnessRating` != ''")

                // Performance indexes for Match History pagination, filtering, sorting, and toss joins
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sessions_timestamp` ON `sessions` (`timestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sessions_matchStatus` ON `sessions` (`matchStatus`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sessions_completedAt` ON `sessions` (`completedAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sessions_matchId` ON `sessions` (`matchId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tosses_matchId` ON `tosses` (`matchId`)")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Performance indexes for Player Directory filtering & sorting
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_players_isArchived` ON `players` (`isArchived`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_players_displayName` ON `players` (`displayName`)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "missingxi_database"
                )
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10,
                    MIGRATION_10_11
                )
                .fallbackToDestructiveMigration(dropAllTables = false)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
