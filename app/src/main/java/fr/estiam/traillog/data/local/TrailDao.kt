package fr.estiam.traillog.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import fr.estiam.traillog.model.Trail
import kotlinx.coroutines.flow.Flow

/**
 * Lab 1 — Requêtes SQL de la table "trails".
 * Les fonctions `suspend` sont exécutées par Room hors du thread principal.
 * Les fonctions qui renvoient un Flow ou un PagingSource sont réactives :
 * Room les relance tout seul dès que la table change.
 */
@Dao
interface TrailDao {

    // --- Lecture paginée (liste principale) ---------------------------------
    // Room génère lui-même le PagingSource (dépendance room-paging).
    // `:favoritesOnly = 0 OR isFavorite = 1` : si le filtre est désactivé on
    // garde tout, sinon uniquement les favoris.
    // Les colonnes ayant les mêmes noms que Trail, Room construit directement
    // des objets Trail : pas de conversion à faire dans le Repository.

    /** Tri alphabétique, sans tenir compte des majuscules. */
    @Query(
        "SELECT * FROM trails WHERE (:favoritesOnly = 0 OR isFavorite = 1) " +
            "ORDER BY name COLLATE NOCASE ASC"
    )
    fun pagingByName(favoritesOnly: Boolean): PagingSource<Int, Trail>

    /** Tri par distance décroissante, puis par nom (même règle qu'en mémoire). */
    @Query(
        "SELECT * FROM trails WHERE (:favoritesOnly = 0 OR isFavorite = 1) " +
            "ORDER BY distanceKm DESC, name COLLATE NOCASE ASC"
    )
    fun pagingByDistance(favoritesOnly: Boolean): PagingSource<Int, Trail>

    // --- Lecture réactive et unitaire ---------------------------------------

    /** Flux de toutes les randonnées : une nouvelle liste est émise à chaque changement. */
    @Query("SELECT * FROM trails ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<TrailEntity>>

    /** Une randonnée par son id, ou null si elle n'existe pas (plus). */
    @Query("SELECT * FROM trails WHERE id = :id")
    suspend fun getById(id: String): TrailEntity?

    // --- Écriture -------------------------------------------------------------

    /** Ajout OU modification : insère si l'id est nouveau, met à jour sinon. */
    @Upsert
    suspend fun upsert(trail: TrailEntity)

    /** Suppression par id. */
    @Query("DELETE FROM trails WHERE id = :id")
    suspend fun deleteById(id: String)

    // --- Lab 2 : import initial ----------------------------------------------

    /** Insère la liste ; IGNORE = une ligne déjà présente n'est jamais écrasée. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnoringExisting(trails: List<TrailEntity>)

    /** Lit une valeur technique (ex. marqueur "import déjà fait"). */
    @Query("SELECT value FROM app_meta WHERE `key` = :key")
    suspend fun getMeta(key: String): String?

    /** Écrit une valeur technique. */
    @Upsert
    suspend fun putMeta(meta: MetaEntity)
}
