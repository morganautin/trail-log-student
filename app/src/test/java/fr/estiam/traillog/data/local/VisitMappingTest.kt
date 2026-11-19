package fr.estiam.traillog.data.local

import fr.estiam.traillog.model.Issue
import fr.estiam.traillog.model.IssueStatus
import fr.estiam.traillog.model.Visit
import fr.estiam.traillog.model.VisitType
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Projet final – conversion entre les entités Room et le modèle métier.
 * Si un champ est perdu ou mal converti dans le Repository, ces tests échouent.
 */
class VisitMappingTest {

    @Test
    fun visit_roundTrip_keepsAllFields() {
        // Arrange
        val visit = Visit(id = 12, trailId = "trail_003", visitedAt = 1_700_000_000_000L,
            type = VisitType.EROSION, conditionScore = 48, notes = "Talus fragile")

        // Act : modèle → entité Room → modèle
        val back = visit.toEntity().toDomain()

        // Assert
        assertEquals(visit, back)
    }

    @Test
    fun visitEntity_unknownTypeAndOutOfRangeScore_areSanitized() {
        // Arrange : données corrompues ou d'une ancienne version
        val entity = VisitEntity(id = 1, trailId = "trail_001", visitedAt = 1L,
            type = "TYPE_SUPPRIME", conditionScore = 140, notes = "")

        // Act
        val visit = entity.toDomain()

        // Assert : valeurs de repli au lieu d'un plantage
        assertEquals(VisitType.INSPECTION, visit.type)
        assertEquals(100, visit.conditionScore)
    }

    @Test
    fun issue_roundTrip_keepsStatus() {
        val issue = Issue(id = 5, visitId = 12, description = "Panneau tombé",
            status = IssueStatus.RESOLVED, createdAt = 42L)
        assertEquals(issue, issue.toEntity().toDomain())
    }

    @Test
    fun visitRow_toSummary_mapsJoinColumns() {
        // Arrange : une ligne telle que renvoyée par la requête de liste (jointure + comptage)
        val row = VisitRow(id = 3, trailId = "trail_002", trailName = "Sentier 02", visitedAt = 99L,
            type = "SIGNAGE", conditionScore = 62, notes = "Balisage", openIssues = 2)

        // Act
        val summary = row.toSummary()

        // Assert : les 4 informations affichées dans la liste sont présentes
        assertEquals("Sentier 02", summary.trailName)
        assertEquals(99L, summary.visit.visitedAt)
        assertEquals(VisitType.SIGNAGE, summary.visit.type)
        assertEquals(2, summary.openIssues)
    }
}
