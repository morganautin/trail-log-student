package fr.estiam.traillog.ui.visits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import fr.estiam.traillog.data.PreferencesRepository
import fr.estiam.traillog.data.VisitRepository
import fr.estiam.traillog.model.VisitSortOrder
import fr.estiam.traillog.model.VisitSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/**
 * ViewModel de la liste : combine la préférence "ordre des visites" et le Repository.
 * Quand l'utilisateur change la préférence, la liste est automatiquement re-triée.
 */
class VisitListViewModel(
    private val repo: VisitRepository,
    prefs: PreferencesRepository
) : ViewModel() {

    /** Ordre courant (pour l'afficher en sous-titre). */
    val sortOrder: Flow<VisitSortOrder> = prefs.settings.map { it.visitSort }.distinctUntilChanged()

    /** Visites triées selon la préférence. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val visits: Flow<List<VisitSummary>> = sortOrder.flatMapLatest { order -> repo.observeVisits(order) }

    class Factory(private val repo: VisitRepository, private val prefs: PreferencesRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = VisitListViewModel(repo, prefs) as T
    }
}
