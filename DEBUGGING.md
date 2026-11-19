# Preuves de debugging

Le sujet demande au moins **deux artefacts concrets** : extraits Logcat, notes de points d'arrêt, ou description avant/après d'un bug précis. Ils doivent venir de **vraies exécutions** sur ton émulateur.

## Journaux ajoutés dans le code (filtres Logcat)

| Tag Logcat | Où | Ce qui est journalisé |
|---|---|---|
| `VisitRepository` | `RoomVisitRepository` | seed des visites (nombre importé, sentier introuvable), création/mise à jour de visite, changement de statut d'un problème |
| `OverdueFollowUp` | `OverdueFollowUpWorker` | nombre de problèmes ouverts, seuil en jours, nombre en retard ; permission de notification refusée ; erreurs |
| `VisitListActivity` | `VisitListActivity` | échec éventuel du seed |

Dans Android Studio : onglet **Logcat**, filtre `tag:OverdueFollowUp | tag:VisitRepository`.

## Artefact 1 – Logcat : diagnostic de la relance (à capturer)

1. Paramètres → Délai de relance **7 jours** → liste → ⋮ → **Vérifier les relances maintenant**.
2. Capture de la ligne Logcat `Vérification : X problème(s) ouvert(s), seuil=7 j, en retard=Y`.
3. Passer le délai à **0 jour**, relancer, capturer la nouvelle ligne : le nombre en retard augmente.
4. Expliquer : le seuil de la préférence est bien lu par le Worker, et le calcul (`FollowUpRules`) se comporte comme attendu.

## Artefact 2 – Point d'arrêt et inspection des variables (à capturer)

1. Ouvrir `model/Visit.kt`, poser un point d'arrêt dans `FollowUpRules.isOverdue`.
2. Lancer l'app en mode **Debug** (icône 🐞), déclencher « Vérifier les relances maintenant ».
3. Au point d'arrêt, capturer le panneau **Variables** : `issue.status`, `issue.createdAt`, `nowMillis`, `thresholdDays`.
4. Noter pourquoi un problème donné est ou n'est pas en retard.

## Artefact 3 – Problème diagnostiqué pendant l'adaptation (avant / après)

**Symptôme anticipé :** l'ajout des tables `visits` et `issues` impose de passer la base Room de la version 1 à la version 2. Avec une migration destructive (`fallbackToDestructiveMigration`), la table `trails` aurait été vidée. Le seed des sentiers ne se serait pas relancé, car son drapeau `seed_complete` est stocké à part dans les SharedPreferences : la liste des sentiers serait restée vide.

**Diagnostic :** lecture de `RoomTrailRepository.seedIfNeeded()`. Le drapeau n'est pas lié au contenu de la base.

**Correction :** migration explicite `MIGRATION_1_2` dans `TrailDatabase`. Elle crée uniquement les nouvelles tables et conserve les sentiers existants.

**Vérification :** après mise à jour de l'app sur l'émulateur (qui contenait déjà la base v1), les sentiers modifiés pendant les labs sont toujours présents et les visites de démonstration apparaissent.

> Si d'autres erreurs apparaissent pendant la compilation ou les tests, ajoute-les ici avec l'erreur (Logcat ou Build), la cause trouvée et la correction : ce sont d'excellents artefacts.
