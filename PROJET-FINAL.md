# Projet final – TrailLog → carnet de terrain Ridgeway Field Services

Branche : `final-ridgeway` (construite sur `m5-student`).

## Où trouver chaque exigence

| Exigence | Implémentation |
|---|---|
| Liste des visites (RecyclerView) : sentier, date/heure, type, problèmes ouverts | `ui/visits/VisitListActivity`, `VisitAdapter`, `res/layout/item_visit.xml` |
| Écran de détail accessible depuis la liste | `ui/visits/VisitDetailActivity` (clic sur une ligne) |
| Vue personnalisée « indicateur d'état » | `ui/visits/ConditionIndicatorView` : jauge colorée selon la note (rouge < 40, orange 40–69, vert ≥ 70), repères des seuils, repère rond |
| Accessibilité | descriptions des boutons (FAB, cases à cocher), description complète de chaque ligne, indicateur exposé à TalkBack comme une barre de progression 0–100 avec annonce automatique (live region), titres marqués comme en-têtes, libellés `labelFor` |
| Room (entités + DAO) + Repository, note d'état par visite | `data/local/VisitEntities.kt` (tables `visits`, `issues`, clés étrangères), `VisitDao`, `RoomVisitRepository`, interface `data/VisitRepository` ; migration 1 → 2 dans `TrailDatabase` |
| Données de départ (≥ 10 sentiers) | `assets/trails.json` (30 sentiers, déjà importés au premier lancement) + `assets/field_visits.json` (5 visites de démonstration avec problèmes) |
| Préférence qui change le comportement | Paramètres → « Ordre de la liste des visites » (récentes/anciennes d'abord) et « Délai de relance » (0, 3 ou 7 jours), stockés dans DataStore |
| WorkManager périodique + notification | `worker/OverdueFollowUpWorker` : toutes les 6 h, compte les problèmes ouverts depuis ≥ N jours et notifie ; menu « Vérifier les relances maintenant » pour une exécution immédiate |
| Test JUnit local | `test/.../model/VisitRulesTest.kt`, `test/.../data/local/VisitMappingTest.kt` |
| Test Espresso | `androidTest/.../VisitFlowEspressoTest.kt` (liste → détail, création → détail) |

## Lancer les tests

```bash
.\gradlew.bat :app:testDebugUnitTest            # tests locaux
.\gradlew.bat :app:connectedDebugAndroidTest    # tests Espresso (émulateur démarré)
```

## Parcours de démonstration (captures pour le document de procédure)

1. Ouvrir l'app → menu latéral (☰) → **Visites d'entretien** : les 5 visites de démonstration apparaissent (données importées).
2. Toucher une visite → écran de détail : indicateur d'état, notes, liste des problèmes.
3. Cocher un problème → il passe en « résolu » et le compteur de problèmes ouverts diminue.
4. Ajouter un problème avec le champ en bas du détail.
5. Revenir à la liste → bouton **+** → choisir sentier, type, note, notes → **Enregistrer** → le détail de la nouvelle visite s'ouvre.
6. **Modifier la visite** → changer la note → l'indicateur change de couleur.
7. Menu ⋮ → **Paramètres** → « Visites les plus anciennes d'abord » → revenir : l'ordre de la liste est inversé (le sous-titre l'indique).
8. Paramètres → Délai de relance **7 jours** → liste → menu ⋮ → **Vérifier les relances maintenant** → notification « 3 problèmes sont ouverts depuis au moins 7 jours ». Avec **0 jour**, tous les problèmes ouverts sont comptés.
