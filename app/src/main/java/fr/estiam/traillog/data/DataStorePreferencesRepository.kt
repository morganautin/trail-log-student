package fr.estiam.traillog.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import fr.estiam.traillog.model.DistanceUnit
import fr.estiam.traillog.model.TrailSortOrder
import fr.estiam.traillog.model.UserSettings
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

// Un seul DataStore par fichier pour toute l'application : on le déclare
// au niveau du fichier, attaché au Context (fichier "user_settings").
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

/**
 * Lab 3 — Préférences enregistrées sur l'appareil avec Preferences DataStore.
 * Contrairement à la version mémoire, les choix survivent à une relance.
 */
class DataStorePreferencesRepository(context: Context) : PreferencesRepository {

    private val dataStore = context.applicationContext.settingsDataStore

    /** Flux observé par les écrans : émet à chaque changement de préférence. */
    override val settings: Flow<UserSettings> = dataStore.data
        // Fichier illisible : on repart des valeurs par défaut au lieu de planter.
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        // Conversion des valeurs texte stockées en objet UserSettings.
        .map { prefs ->
            UserSettings(
                unit = prefs[KEY_UNITS].toEnumOr(DistanceUnit.KM),
                sortOrder = prefs[KEY_SORT].toEnumOr(TrailSortOrder.NAME)
            )
        }
        // Évite de redessiner l'interface si rien n'a réellement changé.
        .distinctUntilChanged()

    /** Enregistre l'unité (KM ou MI). `edit` écrit hors du thread principal. */
    override suspend fun setUnit(unit: DistanceUnit) {
        dataStore.edit { prefs -> prefs[KEY_UNITS] = unit.name }
    }

    /** Enregistre le tri par défaut (NAME ou DISTANCE). */
    override suspend fun setSort(sort: TrailSortOrder) {
        dataStore.edit { prefs -> prefs[KEY_SORT] = sort.name }
    }

    private companion object {
        // Les deux clés demandées par l'énoncé.
        val KEY_UNITS = stringPreferencesKey("pref_distance_units")
        val KEY_SORT = stringPreferencesKey("pref_default_sort")
    }
}

/** Texte -> enum ; valeur absente ou inconnue => valeur par défaut. */
private inline fun <reified T : Enum<T>> String?.toEnumOr(default: T): T =
    enumValues<T>().firstOrNull { it.name == this } ?: default
