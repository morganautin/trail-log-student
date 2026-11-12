package fr.estiam.traillog

import android.app.Application
import fr.estiam.traillog.data.*
import fr.estiam.traillog.data.local.RoomTrailRepository
import fr.estiam.traillog.worker.TrailReminderWorker

/** M3 : la version mémoire des randonnées est remplacée par Room (Lab 1). */
class TrailLogApplication : Application() {
    // Lab 1 : les randonnées sont lues/écrites dans la base Room.
    // `by lazy` : la base n'est ouverte qu'à la première utilisation.
    val trails: TrailRepository by lazy { RoomTrailRepository(this) }

    val settings: PreferencesRepository by lazy { MemoryPreferencesRepository() } // TODO M3-LAB3: DataStore
    override fun onCreate() { super.onCreate(); TrailReminderWorker.ensureChannel(this) }
}
