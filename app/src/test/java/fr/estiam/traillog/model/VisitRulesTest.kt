package fr.estiam.traillog.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.util.Locale

/**
 * Projet final – tests JUnit locaux de la logique métier du carnet de visites.
 * Aucune dépendance Android : exécution rapide sur la JVM, résultats déterministes
 * (l'heure "maintenant" est fixée, jamais lue sur l'horloge système).
 */
class VisitRulesTest {

    // Instant de référence fixe pour tous les calculs de retard
    private val now = 1_800_000_000_000L
    private val day = FollowUpRules.DAY_MILLIS

    private fun issue(id: Long, daysAgo: Long, status: IssueStatus = IssueStatus.OPEN) =
        Issue(id = id, visitId = 1, description = "Problème $id", status = status, createdAt = now - daysAgo * day)

    // ---------------------------------------------------------------------
    // FollowUpRules : calcul des relances en retard (utilisé par le Worker)
    // ---------------------------------------------------------------------

    @Test
    fun overdueCount_countsOnlyOpenIssuesOlderThanThreshold() {
        // Arrange : 2 ouverts anciens, 1 ouvert récent, 1 résolu ancien
        val issues = listOf(
            issue(1, daysAgo = 10),
            issue(2, daysAgo = 8),
            issue(3, daysAgo = 2),
            issue(4, daysAgo = 30, status = IssueStatus.RESOLVED)
        )

        // Act
        val overdue = FollowUpRules.overdueCount(issues, now, thresholdDays = 7)

        // Assert : seuls 1 et 2 sont en retard (le 3 est trop récent, le 4 est résolu)
        assertEquals(2, overdue)
    }

    @Test
    fun isOverdue_exactlyAtThreshold_isOverdue() {
        // Cas limite : créé il y a exactement 7 jours → en retard (comparaison >=)
        assertTrue(FollowUpRules.isOverdue(issue(1, daysAgo = 7), now, thresholdDays = 7))
        // Une milliseconde plus tard → pas encore en retard
        val almost = issue(2, daysAgo = 7).copy(createdAt = now - 7 * day + 1)
        assertFalse(FollowUpRules.isOverdue(almost, now, thresholdDays = 7))
    }

    @Test
    fun overdueCount_zeroThreshold_countsEveryOpenIssue() {
        // Préférence "Immédiatement (0 jour)" : tout problème ouvert compte, même créé à l'instant
        val issues = listOf(issue(1, daysAgo = 0), issue(2, daysAgo = 0, status = IssueStatus.RESOLVED))
        assertEquals(1, FollowUpRules.overdueCount(issues, now, thresholdDays = 0))
    }

    @Test
    fun overdueCount_emptyList_isZero() {
        assertEquals(0, FollowUpRules.overdueCount(emptyList(), now, thresholdDays = 7))
    }

    // ---------------------------------------------------------------------
    // ConditionLevel : niveaux de l'indicateur d'état
    // ---------------------------------------------------------------------

    @Test
    fun conditionLevel_boundaries() {
        assertEquals(ConditionLevel.POOR, ConditionLevel.fromScore(0))
        assertEquals(ConditionLevel.POOR, ConditionLevel.fromScore(39))
        assertEquals(ConditionLevel.FAIR, ConditionLevel.fromScore(40))
        assertEquals(ConditionLevel.FAIR, ConditionLevel.fromScore(69))
        assertEquals(ConditionLevel.GOOD, ConditionLevel.fromScore(70))
        assertEquals(ConditionLevel.GOOD, ConditionLevel.fromScore(100))
    }

    @Test
    fun conditionLevel_outOfRangeScores_areClamped() {
        assertEquals(ConditionLevel.POOR, ConditionLevel.fromScore(-20))
        assertEquals(ConditionLevel.GOOD, ConditionLevel.fromScore(150))
    }

    // ---------------------------------------------------------------------
    // VisitSorting : effet de la préférence "ordre des visites"
    // ---------------------------------------------------------------------

    private fun summary(id: Long, visitedAt: Long) = VisitSummary(
        visit = Visit(id = id, trailId = "trail_$id", visitedAt = visitedAt, type = VisitType.INSPECTION, conditionScore = 50),
        trailName = "Sentier $id",
        openIssues = 0
    )

    @Test
    fun visitSorting_newestFirst_and_oldestFirst() {
        // Arrange : ordre d'entrée volontairement mélangé, et deux visites à la même date (ids 2 et 3)
        val input = listOf(summary(1, 1_000), summary(2, 3_000), summary(3, 3_000), summary(4, 2_000))

        // Act
        val newest = VisitSorting.sort(input, VisitSortOrder.NEWEST_FIRST).map { it.visit.id }
        val oldest = VisitSorting.sort(input, VisitSortOrder.OLDEST_FIRST).map { it.visit.id }

        // Assert : ordre déterministe, l'id départage les visites de même date
        assertEquals(listOf(3L, 2L, 4L, 1L), newest)
        assertEquals(listOf(1L, 4L, 2L, 3L), oldest)
    }

    // ---------------------------------------------------------------------
    // Visit / VisitFormatter
    // ---------------------------------------------------------------------

    @Test
    fun visitValidation_rejectsBlankTrailAndOutOfRangeScore() {
        val valid = Visit(trailId = "trail_001", visitedAt = now, type = VisitType.DRAINAGE, conditionScore = 80)
        assertTrue(valid.isValid())
        assertFalse(valid.copy(trailId = " ").isValid())
        assertFalse(valid.copy(conditionScore = 101).isValid())
        assertFalse(valid.copy(conditionScore = -1).isValid())
    }

    @Test
    fun visitFormatter_usesGivenLocaleAndZone() {
        // 0 ms = 1er janvier 1970 à 00:00 UTC ; fuseau et Locale fixés → texte exact
        assertEquals("01/01/1970 00:00", VisitFormatter.dateTime(0L, Locale.FRANCE, ZoneId.of("UTC")))
        // Paris est en UTC+1 à cette date
        assertEquals("01/01/1970 01:00", VisitFormatter.dateTime(0L, Locale.FRANCE, ZoneId.of("Europe/Paris")))
    }
}
