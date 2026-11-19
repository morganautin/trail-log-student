package fr.estiam.traillog.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Version 2 (projet final) : ajout des tables "visits" et "issues"
@Database(entities = [TrailEntity::class, VisitEntity::class, IssueEntity::class], version = 2, exportSchema = false)
abstract class TrailDatabase : RoomDatabase() {
    abstract fun trailDao(): TrailDao
    abstract fun visitDao(): VisitDao

    companion object {
        @Volatile private var instance: TrailDatabase? = null

        /**
         * Migration 1 → 2 : on CRÉE les nouvelles tables sans toucher à "trails".
         * Les sentiers déjà importés (et le drapeau de seed) sont conservés.
         * Une migration destructive aurait vidé "trails" sans que le seed ne se relance,
         * car son drapeau "seed_complete" est stocké à part dans les SharedPreferences.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `visits` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `trailId` TEXT NOT NULL, " +
                        "`visitedAt` INTEGER NOT NULL, `type` TEXT NOT NULL, `conditionScore` INTEGER NOT NULL, " +
                        "`notes` TEXT NOT NULL, " +
                        "FOREIGN KEY(`trailId`) REFERENCES `trails`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_visits_trailId` ON `visits` (`trailId`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `issues` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `visitId` INTEGER NOT NULL, " +
                        "`description` TEXT NOT NULL, `status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`visitId`) REFERENCES `visits`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_issues_visitId` ON `issues` (`visitId`)")
            }
        }

        fun get(context: Context): TrailDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, TrailDatabase::class.java,
                "traillog.db")
                .addMigrations(MIGRATION_1_2)
                .build().also { instance = it }
        }
    }
}
