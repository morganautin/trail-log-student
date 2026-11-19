package fr.estiam.traillog.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import fr.estiam.traillog.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.trailSettingsDataStore by preferencesDataStore(name = "traillog_settings")

/** M3 correction: durable, reactive user preferences. */
class DataStorePreferencesRepository(private val context: Context) : PreferencesRepository {
    private val unitKey = stringPreferencesKey("pref_distance_units")
    private val sortKey = stringPreferencesKey("pref_default_sort")
    // Projet final : clés des deux nouvelles préférences
    private val visitSortKey = stringPreferencesKey("pref_visit_sort")
    private val followUpDaysKey = intPreferencesKey("pref_follow_up_days")

    override val settings: Flow<UserSettings> = context.trailSettingsDataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            UserSettings(
                unit = DistanceUnit.entries.firstOrNull { it.name == prefs[unitKey] } ?: DistanceUnit.KM,
                sortOrder = TrailSortOrder.entries.firstOrNull { it.name == prefs[sortKey] } ?: TrailSortOrder.NAME,
                // Valeur absente ou inconnue → valeur par défaut (comportement déterministe)
                visitSort = VisitSortOrder.entries.firstOrNull { it.name == prefs[visitSortKey] }
                    ?: VisitSortOrder.NEWEST_FIRST,
                followUpDays = (prefs[followUpDaysKey] ?: UserSettings.DEFAULT_FOLLOW_UP_DAYS).coerceAtLeast(0)
            )
        }
    override suspend fun setUnit(unit: DistanceUnit) {
        context.trailSettingsDataStore.edit { it[unitKey] = unit.name }
    }
    override suspend fun setSort(sort: TrailSortOrder) {
        context.trailSettingsDataStore.edit { it[sortKey] = sort.name }
    }
    override suspend fun setVisitSort(sort: VisitSortOrder) {
        context.trailSettingsDataStore.edit { it[visitSortKey] = sort.name }
    }
    override suspend fun setFollowUpDays(days: Int) {
        context.trailSettingsDataStore.edit { it[followUpDaysKey] = days.coerceAtLeast(0) }
    }
}
