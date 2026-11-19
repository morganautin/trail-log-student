package fr.estiam.traillog.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import fr.estiam.traillog.model.Issue
import fr.estiam.traillog.model.IssueStatus
import fr.estiam.traillog.model.Visit
import fr.estiam.traillog.model.VisitSummary
import fr.estiam.traillog.model.VisitType

/**
 * Table des visites d'entretien.
 * Clé étrangère vers "trails" : supprimer un sentier supprime ses visites (CASCADE).
 */
@Entity(
    tableName = "visits",
    foreignKeys = [ForeignKey(
        entity = TrailEntity::class,
        parentColumns = ["id"],
        childColumns = ["trailId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["trailId"])]
)
data class VisitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trailId: String,
    val visitedAt: Long,
    val type: String,          // nom de l'enum VisitType, stocké en texte (lisible et robuste)
    val conditionScore: Int,   // 0..100
    val notes: String
) {
    fun toDomain(): Visit = Visit(
        id = id, trailId = trailId, visitedAt = visitedAt,
        // Valeur inconnue en base → INSPECTION plutôt qu'un plantage
        type = VisitType.entries.firstOrNull { it.name == type } ?: VisitType.INSPECTION,
        conditionScore = conditionScore.coerceIn(0, 100),
        notes = notes
    )
}

fun Visit.toEntity() = VisitEntity(id, trailId, visitedAt, type.name, conditionScore, notes)

/**
 * Table des problèmes relevés pendant une visite.
 * Clé étrangère vers "visits" : supprimer une visite supprime ses problèmes (CASCADE).
 */
@Entity(
    tableName = "issues",
    foreignKeys = [ForeignKey(
        entity = VisitEntity::class,
        parentColumns = ["id"],
        childColumns = ["visitId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["visitId"])]
)
data class IssueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val visitId: Long,
    val description: String,
    val status: String,   // nom de l'enum IssueStatus
    val createdAt: Long
) {
    fun toDomain(): Issue = Issue(
        id = id, visitId = visitId, description = description,
        status = IssueStatus.entries.firstOrNull { it.name == status } ?: IssueStatus.OPEN,
        createdAt = createdAt
    )
}

fun Issue.toEntity() = IssueEntity(id, visitId, description, status.name, createdAt)

/**
 * Résultat de la requête de liste (jointure visites + sentiers + comptage des problèmes ouverts).
 * Simple classe de données : Room remplit chaque propriété avec la colonne du même nom.
 */
data class VisitRow(
    val id: Long,
    val trailId: String,
    val trailName: String,
    val visitedAt: Long,
    val type: String,
    val conditionScore: Int,
    val notes: String,
    val openIssues: Int
) {
    fun toSummary(): VisitSummary = VisitSummary(
        visit = VisitEntity(id, trailId, visitedAt, type, conditionScore, notes).toDomain(),
        trailName = trailName,
        openIssues = openIssues
    )
}
