package fr.estiam.traillog.data.local

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import fr.estiam.traillog.data.VisitRepository
import fr.estiam.traillog.model.FollowUpRules
import fr.estiam.traillog.model.Issue
import fr.estiam.traillog.model.IssueStatus
import fr.estiam.traillog.model.Visit
import fr.estiam.traillog.model.VisitSortOrder
import fr.estiam.traillog.model.VisitSorting
import fr.estiam.traillog.model.VisitSummary
import fr.estiam.traillog.model.VisitType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray

/** Implémentation Room du carnet de visites. Room reste la seule source de vérité. */
class RoomVisitRepository(
    private val context: Context,
    private val db: TrailDatabase
) : VisitRepository {

    private val dao = db.visitDao()
    private val seedLock = Mutex()

    override fun observeVisits(order: VisitSortOrder): Flow<List<VisitSummary>> =
        dao.observeVisitRows().map { rows ->
            // Conversion Room → modèle métier, puis tri par la règle pure (testée en JUnit)
            VisitSorting.sort(rows.map { it.toSummary() }, order)
        }

    override fun observeVisit(visitId: Long): Flow<VisitSummary?> =
        dao.observeVisitRow(visitId).map { it?.toSummary() }

    override fun observeIssues(visitId: Long): Flow<List<Issue>> =
        dao.observeIssues(visitId).map { list -> list.map { it.toDomain() } }

    override suspend fun getVisit(visitId: Long): Visit? = dao.visitById(visitId)?.toDomain()

    override suspend fun saveVisit(visit: Visit): Long {
        require(visit.isValid()) { "Visite invalide : $visit" }
        return if (visit.id == 0L) {
            dao.insertVisit(visit.toEntity()).also { Log.d(TAG, "Visite créée id=$it sentier=${visit.trailId}") }
        } else {
            dao.updateVisit(visit.toEntity())
            Log.d(TAG, "Visite mise à jour id=${visit.id} score=${visit.conditionScore}")
            visit.id
        }
    }

    override suspend fun deleteVisit(visitId: Long) = dao.deleteVisit(visitId)

    override suspend fun addIssue(visitId: Long, description: String, createdAt: Long): Long {
        require(description.isNotBlank()) { "Description vide" }
        return dao.insertIssue(IssueEntity(visitId = visitId, description = description.trim(),
            status = IssueStatus.OPEN.name, createdAt = createdAt))
    }

    override suspend fun setIssueResolved(issueId: Long, resolved: Boolean) {
        val status = if (resolved) IssueStatus.RESOLVED else IssueStatus.OPEN
        dao.setIssueStatus(issueId, status.name)
        Log.d(TAG, "Problème id=$issueId → $status")
    }

    override suspend fun openIssues(): List<Issue> = dao.openIssues().map { it.toDomain() }

    /**
     * Seed des visites de démonstration depuis assets/field_visits.json.
     * - à appeler APRÈS le seed des sentiers (les visites référencent des sentiers) ;
     * - un drapeau durable évite de réimporter si l'utilisateur supprime ses visites ;
     * - une visite dont le sentier n'existe pas est ignorée (au lieu de faire échouer la clé étrangère).
     */
    override suspend fun seedIfNeeded() {
        seedLock.withLock {
            val flags = context.getSharedPreferences("traillog_seed", Context.MODE_PRIVATE)
            if (flags.getBoolean(SEED_FLAG, false)) return@withLock
            if (dao.countVisits() > 0) {
                flags.edit().putBoolean(SEED_FLAG, true).apply()
                return@withLock
            }
            val now = System.currentTimeMillis()
            val array = JSONArray(context.assets.open("field_visits.json").bufferedReader().use { it.readText() })
            var imported = 0
            db.withTransaction {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val trailId = obj.optString("trailId").trim()
                    if (trailId.isBlank() || dao.countTrail(trailId) == 0) {
                        Log.w(TAG, "Seed : sentier '$trailId' introuvable, visite ignorée")
                        continue
                    }
                    val visit = Visit(
                        trailId = trailId,
                        visitedAt = now - obj.optInt("daysAgo", 0) * FollowUpRules.DAY_MILLIS,
                        type = VisitType.entries.firstOrNull { it.name == obj.optString("type") } ?: VisitType.INSPECTION,
                        conditionScore = obj.optInt("conditionScore", 50).coerceIn(0, 100),
                        notes = obj.optString("notes")
                    )
                    val visitId = dao.insertVisit(visit.toEntity())
                    val issues = obj.optJSONArray("issues") ?: JSONArray()
                    for (j in 0 until issues.length()) {
                        val issue = issues.getJSONObject(j)
                        val description = issue.optString("description").trim()
                        if (description.isBlank()) continue
                        dao.insertIssue(IssueEntity(
                            visitId = visitId,
                            description = description,
                            status = IssueStatus.entries.firstOrNull { it.name == issue.optString("status") }?.name
                                ?: IssueStatus.OPEN.name,
                            createdAt = now - issue.optInt("daysAgo", 0) * FollowUpRules.DAY_MILLIS
                        ))
                    }
                    imported++
                }
            }
            Log.i(TAG, "Seed des visites terminé : $imported visite(s) importée(s)")
            flags.edit().putBoolean(SEED_FLAG, true).apply()
        }
    }

    companion object {
        const val TAG = "VisitRepository"
        private const val SEED_FLAG = "visits_seed_complete"
    }
}
