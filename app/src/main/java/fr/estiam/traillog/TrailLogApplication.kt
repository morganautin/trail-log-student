package fr.estiam.traillog

import android.app.Application
import fr.estiam.traillog.data.PreferencesRepository
import fr.estiam.traillog.data.TrailRepository
import fr.estiam.traillog.data.DataStorePreferencesRepository
import fr.estiam.traillog.data.VisitRepository
import fr.estiam.traillog.data.local.RoomTrailRepository
import fr.estiam.traillog.data.local.RoomVisitRepository
import fr.estiam.traillog.data.local.TrailDatabase
import fr.estiam.traillog.worker.OverdueFollowUpWorker
import fr.estiam.traillog.worker.TrailReminderWorker

class TrailLogApplication : Application() {
    val trails: TrailRepository by lazy {
        RoomTrailRepository(this, TrailDatabase.get(this).trailDao())
    }
    // Projet final : carnet de visites d'entretien (même base Room que les sentiers)
    val visits: VisitRepository by lazy { RoomVisitRepository(this, TrailDatabase.get(this)) }
    val settings: PreferencesRepository by lazy { DataStorePreferencesRepository(this) }

    override fun onCreate() {
        super.onCreate()
        TrailReminderWorker.ensureChannel(this)
        // Projet final : canal des relances + planification périodique de la vérification
        OverdueFollowUpWorker.ensureChannel(this)
        OverdueFollowUpWorker.schedulePeriodic(this)
    }
}
