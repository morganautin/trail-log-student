package fr.estiam.traillog.data

import fr.estiam.traillog.model.Issue
import fr.estiam.traillog.model.Visit
import fr.estiam.traillog.model.VisitSortOrder
import fr.estiam.traillog.model.VisitSummary
import kotlinx.coroutines.flow.Flow

/**
 * Contrat du carnet de visites. L'interface (UI, Worker) ne parle qu'à ce Repository,
 * jamais directement à Room : la persistance peut changer sans toucher aux écrans.
 */
interface VisitRepository {
    /** Liste réactive des visites, triée selon la préférence. */
    fun observeVisits(order: VisitSortOrder): Flow<List<VisitSummary>>
    /** Une visite (avec nom du sentier et nombre de problèmes ouverts), ou null. */
    fun observeVisit(visitId: Long): Flow<VisitSummary?>
    /** Problèmes d'une visite. */
    fun observeIssues(visitId: Long): Flow<List<Issue>>

    suspend fun getVisit(visitId: Long): Visit?
    /** Crée la visite si id == 0, sinon la met à jour. Renvoie l'id de la visite. */
    suspend fun saveVisit(visit: Visit): Long
    suspend fun deleteVisit(visitId: Long)

    /** Ajoute un problème OUVERT à une visite. Renvoie son id. */
    suspend fun addIssue(visitId: Long, description: String, createdAt: Long = System.currentTimeMillis()): Long
    suspend fun setIssueResolved(issueId: Long, resolved: Boolean)
    /** Tous les problèmes encore ouverts (pour la tâche de fond). */
    suspend fun openIssues(): List<Issue>

    /** Importe les visites de démonstration au premier lancement. */
    suspend fun seedIfNeeded()
}
