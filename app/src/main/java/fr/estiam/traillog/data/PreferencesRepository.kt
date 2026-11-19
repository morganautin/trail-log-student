package fr.estiam.traillog.data

import fr.estiam.traillog.model.UserSettings
import fr.estiam.traillog.model.DistanceUnit
import fr.estiam.traillog.model.TrailSortOrder
import fr.estiam.traillog.model.VisitSortOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

interface PreferencesRepository {
    val settings: Flow<UserSettings>
    suspend fun setUnit(unit: DistanceUnit)
    suspend fun setSort(sort: TrailSortOrder)
    // Projet final : préférences du carnet de visites
    suspend fun setVisitSort(sort: VisitSortOrder)
    suspend fun setFollowUpDays(days: Int)
}

/** Before M3 settings exist in memory only. */
class MemoryPreferencesRepository : PreferencesRepository {
    private val state = MutableStateFlow(UserSettings())
    override val settings: Flow<UserSettings> = state
    override suspend fun setUnit(unit: DistanceUnit) { state.value = state.value.copy(unit = unit) }
    override suspend fun setSort(sort: TrailSortOrder) { state.value = state.value.copy(sortOrder = sort) }
    override suspend fun setVisitSort(sort: VisitSortOrder) { state.value = state.value.copy(visitSort = sort) }
    override suspend fun setFollowUpDays(days: Int) { state.value = state.value.copy(followUpDays = days.coerceAtLeast(0)) }
}
