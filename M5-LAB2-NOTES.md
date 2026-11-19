# M5 – Lab 2 : tests d'interface Espresso

**Lancer** (émulateur démarré, `adb devices` doit l'afficher comme `device`) :
`.\gradlew.bat :app:connectedDebugAndroidTest`

## Tests écrits

Fichier : `app/src/androidTest/java/fr/estiam/traillog/TrailListEspressoTest.kt`. Le test fictif marqué `@Ignore` a été remplacé.

| Test | Comportement vérifié |
|---|---|
| `trailList_isDisplayedOnLaunch` | `R.id.trailList` est visible au lancement |
| `addButton_opensEditForm` | `addButton` ouvre le formulaire (`nameInput`, `distanceInput`, `saveButton` visibles) |
| `addTrail_savesAndShowsNewRowInList` | remplir le formulaire, enregistrer, revenir sur la liste, retrouver la nouvelle ligne par son texte |

## Synchronisation (sans Thread.sleep)

- L'enregistrement Room et le rechargement Paging 3 se font en arrière-plan.
- Un `IdlingResource` maison (`TrailVisibleIdlingResource`) n'est « idle » que lorsque le sentier créé est présent dans les données de l'adapter paginé (`snapshot()`).
- Il est réveillé par `addOnPagesUpdatedListener`, donc par un événement réel de Paging 3 et non par une attente fixe.
- Il est enregistré juste après le clic sur Enregistrer, puis retiré dans `@After`.

## Isolation

- Le nom du sentier est unique à chaque exécution (`AAA Espresso <horodatage>`) : un sentier laissé par une exécution précédente ne peut pas faire réussir le test par erreur.
- Dans `@After`, on supprime uniquement les sentiers portant ce nom, sans toucher aux autres données.
- Le nom commence par « AAA » et la distance est de 999 km : le sentier apparaît en tête de liste, quel que soit le tri choisi dans les préférences (NAME ou DISTANCE).
- La ligne est retrouvée par son contenu (`RecyclerViewActions.scrollTo` avec `hasDescendant(withText(...))`), jamais par une position dans l'adapter.

## Limites

- Les tests utilisent la vraie base de l'application, pas une base en mémoire. Un arrêt brutal pendant un test peut donc laisser le sentier de test en base, même s'il ne fausse pas les exécutions suivantes.
- Le test d'ajout suppose que le filtre « favoris seulement » n'est pas actif au lancement (c'est le comportement par défaut).
- Le formulaire invalide (Snackbar d'erreur) et la modification d'un sentier existant ne sont pas testés.
