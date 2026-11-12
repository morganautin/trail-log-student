package fr.estiam.traillog

import android.app.Application
import fr.estiam.traillog.data.*
import fr.estiam.traillog.data.local.RoomTrailRepository
import fr.estiam.traillog.worker.TrailReminderWorker

/** M3 : les implémentations mémoire sont remplacées par Room (Lab 1) et DataStore (Lab 3). */
class TrailLogApplication : Application() {
    // Lab 1 : les randonnées sont lues/écrites dans la base Room.
    // `by lazy` : la base n'est ouverte qu'à la première utilisation.
    val trails: TrailRepository by lazy { RoomTrailRepository(this) }
    // Lab 3 : les préférences sont enregistrées avec DataStore.
    val settings: PreferencesRepository by lazy { DataStorePreferencesRepository(this) }
    override fun onCreate() { super.onCreate(); TrailReminderWorker.ensureChannel(this) }
}
