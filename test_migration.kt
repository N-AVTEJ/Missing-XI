import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE IF NOT EXISTS `players` (`id` TEXT NOT NULL, `displayName` TEXT NOT NULL, `nickname` TEXT, `createdAt` INTEGER NOT NULL, `lastUsedAt` INTEGER NOT NULL, `totalMatches` INTEGER NOT NULL, `totalTimesJoker` INTEGER NOT NULL, `isFavorite` INTEGER NOT NULL, `isArchived` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        database.execSQL("CREATE TABLE IF NOT EXISTS `sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `playerIdsJson` TEXT NOT NULL, `teamCount` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL)")
    }
}
