package fr.estiam.traillog.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Accès Room aux visites et aux problèmes. Les Flow se mettent à jour automatiquement à chaque écriture. */
@Dao
interface VisitDao {

    // --- Lecture (réactive) ---------------------------------------------------

    /** Toutes les visites avec le nom du sentier et le nombre de problèmes OUVERTS (le tri est fait dans le Repository). */
    @Query(
        """
        SELECT v.id AS id, v.trailId AS trailId, t.name AS trailName, v.visitedAt AS visitedAt,
               v.type AS type, v.conditionScore AS conditionScore, v.notes AS notes,
               (SELECT COUNT(*) FROM issues i WHERE i.visitId = v.id AND i.status = 'OPEN') AS openIssues
        FROM visits v INNER JOIN trails t ON t.id = v.trailId
        """
    )
    fun observeVisitRows(): Flow<List<VisitRow>>

    /** Une seule visite (écran de détail). null si elle n'existe pas ou plus. */
    @Query(
        """
        SELECT v.id AS id, v.trailId AS trailId, t.name AS trailName, v.visitedAt AS visitedAt,
               v.type AS type, v.conditionScore AS conditionScore, v.notes AS notes,
               (SELECT COUNT(*) FROM issues i WHERE i.visitId = v.id AND i.status = 'OPEN') AS openIssues
        FROM visits v INNER JOIN trails t ON t.id = v.trailId
        WHERE v.id = :visitId
        """
    )
    fun observeVisitRow(visitId: Long): Flow<VisitRow?>

    /** Problèmes d'une visite : ouverts d'abord, puis du plus ancien au plus récent. */
    @Query("SELECT * FROM issues WHERE visitId = :visitId ORDER BY status = 'RESOLVED', createdAt ASC, id ASC")
    fun observeIssues(visitId: Long): Flow<List<IssueEntity>>

    // --- Lecture ponctuelle -------------------------------------------------------

    @Query("SELECT * FROM visits WHERE id = :visitId LIMIT 1")
    suspend fun visitById(visitId: Long): VisitEntity?

    /** Utilisé par la tâche de fond pour calculer les relances. */
    @Query("SELECT * FROM issues WHERE status = 'OPEN'")
    suspend fun openIssues(): List<IssueEntity>

    @Query("SELECT COUNT(*) FROM visits")
    suspend fun countVisits(): Int

    /** Vérifie qu'un sentier existe avant d'y rattacher une visite (sinon la clé étrangère échouerait). */
    @Query("SELECT COUNT(*) FROM trails WHERE id = :trailId")
    suspend fun countTrail(trailId: String): Int

    // --- Écriture -------------------------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertVisit(visit: VisitEntity): Long

    @Update
    suspend fun updateVisit(visit: VisitEntity)

    @Query("DELETE FROM visits WHERE id = :visitId")
    suspend fun deleteVisit(visitId: Long)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertIssue(issue: IssueEntity): Long

    @Query("UPDATE issues SET status = :status WHERE id = :issueId")
    suspend fun setIssueStatus(issueId: Long, status: String)
}
