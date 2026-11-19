# M5 – Lab 1 : tests JUnit 4 locaux

**Lancer :** `.\gradlew.bat :app:testDebugUnitTest`

## Tests écrits

`app/src/test/java/fr/estiam/traillog/data/TrailRepositoryLocalTest.kt` (système testé : `MemoryTrailRepository`)

| Test | Comportement vérifié |
|---|---|
| `save_thenGetTrail_returnsSavedTrail` | save + getTrail |
| `save_existingId_updatesDataWithoutDuplicate` | mise à jour par ID, pas de doublon |
| `save_invalidTrail_isRejected` | un sentier invalide est refusé |
| `delete_removesTrailFromObservedList` | delete + observeTrails (`Flow.first()`) |
| `pagingSource_nameSort_ordersKnownTrailsAlphabetically` | tri NAME via PagingSource |
| `pagingSource_distanceSort_ordersKnownTrailsLongestFirst` | tri DISTANCE (décroissant) via PagingSource |
| `pagingSource_favoritesOnly_withNoFavoriteFixture_excludesOurTrails` | cas limite : favoris seulement |
| `getTrail_unknownId_returnsNull` | cas limite : ID inconnu |

`app/src/test/java/fr/estiam/traillog/model/TrailFormatterTest.kt` : `TrailFormatter.distance` en KM, MI, distance nulle, arrondi, et virgule décimale en Locale.FRANCE.

## Isolation des fixtures

- Un `MemoryTrailRepository` neuf est créé avant chaque test (`@Before`) : aucun état partagé entre les tests.
- Fixtures fixes (`pine-1`, `ridge-2`, `creek-3`) avec un `updatedAt` figé pour des résultats identiques à chaque exécution.
- Le repository contient 45 sentiers de démo : les assertions de tri et de filtre ne portent que sur nos identifiants connus.
- La `Locale` est fixée avant chaque test de formatage et restaurée après (`@After`).

## Gestion de l'asynchrone

- Les méthodes `suspend` (save, delete, getTrail, load) sont appelées dans `runBlocking`, qui attend leur fin avant les assertions.
- `observeTrails()` est un `StateFlow` : `first()` lit sa valeur actuelle, sans attente ni `Thread.sleep`.
- Le `PagingSource` est chargé directement avec `LoadParams.Refresh(key = null, loadSize = 100, placeholdersEnabled = false)`, ce qui renvoie toute la liste en une seule page.

## Limites

- Ces tests ne vérifient pas `RoomTrailRepository`, ni le SQL Room réel, ni l'intégration Paging 3 avec le `RecyclerView` : c'est le rôle des tests instrumentés (`androidTest`).
- On ne teste qu'une seule page de chargement, pas la pagination sur plusieurs pages ni l'invalidation du PagingSource après une écriture.
- Pas de test de concurrence (plusieurs écritures simultanées).
