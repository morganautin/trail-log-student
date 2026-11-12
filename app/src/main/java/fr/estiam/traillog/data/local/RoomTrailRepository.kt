package fr.estiam.traillog.data.local

import android.content.Context
import androidx.paging.PagingSource
import androidx.room.withTransaction
import fr.estiam.traillog.data.TrailRepository
import fr.estiam.traillog.model.Trail
import fr.estiam.traillog.model.TrailSortOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Lab 1 + Lab 2 — Implémentation Room du contrat [TrailRepository].
 * Les écrans ne connaissent que l'interface : remplacer la version mémoire
 * par celle-ci ne demande aucun changement côté UI.
 */
class RoomTrailRepository(
    context: Context,
    private val database: TrailDatabase = TrailDatabase.getInstance(context)
) : TrailRepository {

    // Contexte application, utilisé uniquement pour lire le dossier assets.
    private val appContext = context.applicationContext
    private val dao = database.trailDao()

    /** Liste paginée : on choisit la requête selon le tri demandé. */
    override fun pagingSource(sort: TrailSortOrder, favoritesOnly: Boolean): PagingSource<Int, Trail> =
        when (sort) {
            TrailSortOrder.NAME -> dao.pagingByName(favoritesOnly)
            TrailSortOrder.DISTANCE -> dao.pagingByDistance(favoritesOnly)
        }

    /** Flux réactif : chaque changement en base émet une nouvelle liste convertie en Trail. */
    override fun observeTrails(): Flow<List<Trail>> =
        dao.observeAll().map { entities -> entities.map { it.toTrail() } }

    override suspend fun getTrail(id: String): Trail? = dao.getById(id)?.toTrail()

    /** Ajout ou modification (les favoris passent aussi par ici). */
    override suspend fun save(trail: Trail) {
        // Refuse les données invalides (nom vide, distance négative...) avant d'écrire.
        require(trail.isValid()) { "Randonnée invalide : ${trail.id}" }
        dao.upsert(trail.toEntity())
    }

    override suspend fun delete(id: String) = dao.deleteById(id)

    /**
     * Lab 2 — Import du JSON à la première installation uniquement.
     * Appelée à chaque ouverture de l'écran principal : elle doit donc être
     * rapide et sans effet quand l'import a déjà eu lieu.
     */
    override suspend fun seedIfNeeded() {
        // 1. Déjà importé ? On s'arrête tout de suite (cas de toutes les relances).
        if (dao.getMeta(KEY_SEEDED) != null) return

        // 2. Lecture + parsing du fichier sur un thread d'entrée/sortie,
        //    jamais sur le thread principal.
        val trails = withContext(Dispatchers.IO) {
            val json = appContext.assets.open(SEED_FILE).bufferedReader().use { it.readText() }
            TrailJsonParser.parse(json)
        }

        // 3. Transaction : l'insertion et le marqueur sont enregistrés ensemble.
        //    Si l'app est coupée au milieu, rien n'est écrit et on recommencera.
        database.withTransaction {
            // Nouvelle vérification dans la transaction, au cas où deux appels
            // auraient démarré en même temps : un seul fera l'import.
            if (dao.getMeta(KEY_SEEDED) == null) {
                // IGNORE : une randonnée déjà créée/modifiée par l'utilisateur n'est pas écrasée.
                dao.insertAllIgnoringExisting(trails.map { it.toEntity() })
                // Le marqueur empêche tout nouvel import, même si l'utilisateur
                // supprime ensuite des randonnées importées.
                dao.putMeta(MetaEntity(KEY_SEEDED, System.currentTimeMillis().toString()))
            }
        }
    }

    private companion object {
        const val SEED_FILE = "trails.json"
        const val KEY_SEEDED = "seed_trails_json_done"
    }
}
