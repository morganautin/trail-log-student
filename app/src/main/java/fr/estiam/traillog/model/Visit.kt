package fr.estiam.traillog.model

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Modèle métier du carnet de terrain Ridgeway Field Services.
 * Ces classes ne dépendent pas d'Android : elles sont testables en JUnit local.
 */

/** Types de visite d'entretien réalisés par les équipes. */
enum class VisitType { INSPECTION, SIGNAGE, DRAINAGE, EROSION, HAZARD_CHECK }

/** Statut d'un problème relevé pendant une visite. */
enum class IssueStatus { OPEN, RESOLVED }

/** Ordre d'affichage de la liste des visites (piloté par une préférence utilisateur). */
enum class VisitSortOrder { NEWEST_FIRST, OLDEST_FIRST }

/** Une visite d'entretien sur un sentier. id = 0 tant que la visite n'est pas enregistrée. */
data class Visit(
    val id: Long = 0,
    val trailId: String,
    val visitedAt: Long,
    val type: VisitType,
    val conditionScore: Int,   // note d'état du sentier, de 0 (très dégradé) à 100 (parfait)
    val notes: String = ""
) {
    fun isValid(): Boolean = trailId.isNotBlank() && conditionScore in 0..100 && visitedAt > 0
}

/** Ligne de la liste : la visite + le nom du sentier + le nombre de problèmes encore ouverts. */
data class VisitSummary(
    val visit: Visit,
    val trailName: String,
    val openIssues: Int
)

/** Problème relevé pendant une visite (panneau cassé, rigole bouchée…), à suivre jusqu'à résolution. */
data class Issue(
    val id: Long = 0,
    val visitId: Long,
    val description: String,
    val status: IssueStatus = IssueStatus.OPEN,
    val createdAt: Long
)

/** Niveau d'état dérivé de la note : utilisé par la vue personnalisée et par l'accessibilité. */
enum class ConditionLevel {
    POOR, FAIR, GOOD;

    companion object {
        /** < 40 → mauvais, 40 à 69 → moyen, ≥ 70 → bon. Les valeurs hors 0..100 sont ramenées dans l'intervalle. */
        fun fromScore(score: Int): ConditionLevel {
            val s = score.coerceIn(0, 100)
            return when {
                s >= 70 -> GOOD
                s >= 40 -> FAIR
                else -> POOR
            }
        }
    }
}

/** Règles de relance utilisées par la tâche de fond WorkManager. */
object FollowUpRules {
    const val DAY_MILLIS = 24L * 60 * 60 * 1000

    /**
     * Un problème est "en retard" s'il est encore OUVERT et a été créé il y a
     * au moins [thresholdDays] jours. Avec un seuil de 0 jour, tout problème ouvert est en retard.
     */
    fun isOverdue(issue: Issue, nowMillis: Long, thresholdDays: Int): Boolean =
        issue.status == IssueStatus.OPEN &&
            nowMillis - issue.createdAt >= thresholdDays.coerceAtLeast(0) * DAY_MILLIS

    /** Nombre de problèmes en retard dans la liste. */
    fun overdueCount(issues: List<Issue>, nowMillis: Long, thresholdDays: Int): Int =
        issues.count { isOverdue(it, nowMillis, thresholdDays) }
}

/** Tri de la liste des visites selon la préférence. À date égale, l'id départage (ordre stable). */
object VisitSorting {
    fun sort(visits: List<VisitSummary>, order: VisitSortOrder): List<VisitSummary> = when (order) {
        VisitSortOrder.NEWEST_FIRST -> visits.sortedWith(
            compareByDescending<VisitSummary> { it.visit.visitedAt }.thenByDescending { it.visit.id })
        VisitSortOrder.OLDEST_FIRST -> visits.sortedWith(
            compareBy<VisitSummary> { it.visit.visitedAt }.thenBy { it.visit.id })
    }
}

/** Formatage de la date de visite (Locale et fuseau passés en paramètre pour des tests déterministes). */
object VisitFormatter {
    fun dateTime(
        millis: Long,
        locale: Locale = Locale.getDefault(),
        zone: ZoneId = ZoneId.systemDefault()
    ): String = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", locale)
        .withZone(zone)
        .format(Instant.ofEpochMilli(millis))
}
