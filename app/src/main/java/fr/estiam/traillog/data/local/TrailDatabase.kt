package fr.estiam.traillog.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Lab 1 — Base de données Room de l'application (fichier "traillog.db").
 * On déclare ici les tables (entities) et on expose le DAO.
 */
@Database(
    entities = [TrailEntity::class, MetaEntity::class],
    version = 1,
    exportSchema = false
)
abstract class TrailDatabase : RoomDatabase() {

    abstract fun trailDao(): TrailDao

    companion object {
        // Une seule instance pour toute l'application (ouvrir une base coûte cher).
        @Volatile private var instance: TrailDatabase? = null

        fun getInstance(context: Context): TrailDatabase =
            // Double vérification : on ne crée la base qu'une fois, même si
            // deux threads la demandent en même temps.
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext, // contexte application : pas de fuite d'Activity
                    TrailDatabase::class.java,
                    "traillog.db"
                ).build().also { instance = it }
            }
    }
}
