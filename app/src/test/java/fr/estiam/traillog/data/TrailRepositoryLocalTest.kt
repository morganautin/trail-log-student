package fr.estiam.traillog.data

import androidx.paging.PagingSource
import fr.estiam.traillog.model.Trail
import fr.estiam.traillog.model.TrailSortOrder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * M5 Lab 1 : tests JUnit 4 locaux (JVM) du vrai MemoryTrailRepository.
 *
 * - Aucun appareil Android, aucune base Room : le repository garde ses données en mémoire.
 * - Chaque test suit le schéma Arrange (préparer) → Act (agir) → Assert (vérifier).
 * - Le repository contient déjà 45 sentiers de démo ("sample_1" … "sample_45") :
 *   on ne suppose donc jamais qu'il est vide, on ne vérifie que NOS identifiants connus.
 */
class TrailRepositoryLocalTest {

    // Système testé : recréé avant CHAQUE test (voir setUp) pour que les tests
    // soient indépendants les uns des autres (aucune donnée ne "fuit" d'un test à l'autre).
    private lateinit var repository: MemoryTrailRepository

    // --- Fixtures déterministes : identifiants, noms et distances fixes ---
    // updatedAt est figé (au lieu de System.currentTimeMillis()) pour que les objets
    // soient strictement identiques à chaque exécution.
    private val pine = Trail(
        id = "pine-1", name = "Pine Loop", region = "Alpes",
        distanceKm = 3.2, elevationGainM = 250, updatedAt = 1L
    )
    private val ridge = Trail(
        id = "ridge-2", name = "Ridge Run", region = "Jura",
        distanceKm = 7.5, elevationGainM = 600, updatedAt = 2L
    )
    private val creek = Trail(
        id = "creek-3", name = "Creekside", region = "Vosges",
        distanceKm = 1.0, elevationGainM = 40, updatedAt = 3L
    )

    // Ensemble de nos identifiants, utilisé pour filtrer les résultats paginés
    private val knownIds = setOf("pine-1", "ridge-2", "creek-3")

    @Before
    fun setUp() {
        // Nouvelle instance = état de départ propre (uniquement les données de démo)
        repository = MemoryTrailRepository()
    }

    // ------------------------------------------------------------------
    // Outils internes
    // ------------------------------------------------------------------

    /** Enregistre nos 3 fixtures dans le repository (méthodes suspend → runBlocking). */
    private fun saveFixtures() = runBlocking {
        repository.save(pine)
        repository.save(ridge)
        repository.save(creek)
    }

    /**
     * Charge une page via le PagingSource, exactement comme le ferait la liste de l'app.
     * loadSize = 100 suffit pour tout récupérer en une seule page (45 démos + 3 fixtures).
     */
    private fun loadPage(sort: TrailSortOrder, favoritesOnly: Boolean): List<Trail> = runBlocking {
        val result = repository.pagingSource(sort, favoritesOnly).load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 100, placeholdersEnabled = false)
        )
        // Le chargement doit réussir : on vérifie que c'est bien une Page (et pas une erreur)
        assertTrue("Le PagingSource doit renvoyer une Page", result is PagingSource.LoadResult.Page)
        (result as PagingSource.LoadResult.Page).data
    }

    // ------------------------------------------------------------------
    // 1. save + getTrail, et mise à jour par identifiant String stable
    // ------------------------------------------------------------------

    @Test
    fun save_thenGetTrail_returnsSavedTrail() = runBlocking {
        // Arrange : rien de plus que le repository neuf + la fixture "pine"

        // Act : on enregistre puis on relit par son identifiant
        repository.save(pine)
        val loaded = repository.getTrail("pine-1")

        // Assert : on récupère exactement le même sentier (data class → comparaison champ par champ)
        assertEquals(pine, loaded)
    }

    @Test
    fun save_existingId_updatesDataWithoutDuplicate() = runBlocking {
        // Arrange : pine est déjà enregistré
        repository.save(pine)
        val renamed = pine.copy(name = "Pine Loop (modifié)", distanceKm = 4.0)

        // Act : on réenregistre avec le MÊME identifiant
        repository.save(renamed)

        // Assert 1 : les données ont bien été mises à jour
        assertEquals(renamed, repository.getTrail("pine-1"))
        // Assert 2 : il n'existe toujours qu'UN seul sentier "pine-1" (pas de doublon)
        val all = repository.observeTrails().first()
        assertEquals(1, all.count { it.id == "pine-1" })
    }

    @Test(expected = IllegalArgumentException::class)
    fun save_invalidTrail_isRejected() {
        // Arrange : un nom vide rend le sentier invalide (Trail.isValid() == false)
        val invalid = pine.copy(name = "   ")

        // Act + Assert : save() appelle require(trail.isValid()) → IllegalArgumentException attendue
        runBlocking { repository.save(invalid) }
    }

    // ------------------------------------------------------------------
    // 2. delete + observeTrails (Flow lu avec first())
    // ------------------------------------------------------------------

    @Test
    fun delete_removesTrailFromObservedList() = runBlocking {
        // Arrange : nos 3 sentiers sont présents
        saveFixtures()
        assertNotNull(repository.getTrail("ridge-2")) // précondition : il existe bien avant suppression

        // Act : on supprime ridge-2, puis on lit la valeur actuelle du Flow
        repository.delete("ridge-2")
        val observedIds = repository.observeTrails().first().map { it.id }

        // Assert : ridge-2 a disparu, les deux autres sont toujours là
        assertTrue("ridge-2 ne doit plus être observé", "ridge-2" !in observedIds)
        assertTrue(observedIds.containsAll(listOf("pine-1", "creek-3")))
        assertNull(repository.getTrail("ridge-2"))
    }

    // ------------------------------------------------------------------
    // 3. Tri NAME et DISTANCE via le PagingSource
    // ------------------------------------------------------------------

    @Test
    fun pagingSource_nameSort_ordersKnownTrailsAlphabetically() {
        // Arrange
        saveFixtures()

        // Act : on charge la page triée par nom, puis on ne garde que nos sentiers
        val ourOrder = loadPage(TrailSortOrder.NAME, favoritesOnly = false)
            .map { it.id }
            .filter { it in knownIds }

        // Assert : Creekside < Pine Loop < Ridge Run (ordre alphabétique)
        assertEquals(listOf("creek-3", "pine-1", "ridge-2"), ourOrder)
    }

    @Test
    fun pagingSource_distanceSort_ordersKnownTrailsLongestFirst() {
        // Arrange
        saveFixtures()

        // Act
        val ourOrder = loadPage(TrailSortOrder.DISTANCE, favoritesOnly = false)
            .map { it.id }
            .filter { it in knownIds }

        // Assert : le tri DISTANCE du repository est DÉCROISSANT (compareByDescending)
        // → 7.5 km, puis 3.2 km, puis 1.0 km
        assertEquals(listOf("ridge-2", "pine-1", "creek-3"), ourOrder)
    }

    // ------------------------------------------------------------------
    // 4. Cas limites
    // ------------------------------------------------------------------

    @Test
    fun pagingSource_favoritesOnly_withNoFavoriteFixture_excludesOurTrails() {
        // Arrange : aucune de nos fixtures n'est favorite (isFavorite = false par défaut)
        saveFixtures()

        // Act : on demande uniquement les favoris
        val favorites = loadPage(TrailSortOrder.NAME, favoritesOnly = true)

        // Assert 1 : aucun de nos sentiers n'apparaît
        assertTrue(favorites.none { it.id in knownIds })
        // Assert 2 : tout ce qui est renvoyé est bien marqué favori (le filtre fonctionne)
        assertTrue(favorites.all { it.isFavorite })
    }

    @Test
    fun getTrail_unknownId_returnsNull() = runBlocking {
        // Arrange : des données existent, mais pas cet identifiant
        saveFixtures()

        // Act
        val result = repository.getTrail("does-not-exist")

        // Assert : pas d'exception, simplement null
        assertNull(result)
    }
}
