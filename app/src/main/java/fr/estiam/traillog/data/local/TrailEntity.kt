package fr.estiam.traillog.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import fr.estiam.traillog.model.Difficulty
import fr.estiam.traillog.model.Trail

/**
 * Lab 1 — Table Room "trails".
 * Une ligne de la table = une randonnée. Les colonnes portent le même nom
 * que les champs du modèle métier [Trail] pour garder un mapping simple.
 */
@Entity(tableName = "trails")
data class TrailEntity(
    // Clé primaire : l'identifiant de la randonnée (UUID ou id du JSON).
    @PrimaryKey val id: String,
    val name: String,
    val region: String,
    val distanceKm: Double,
    val elevationGainM: Int,
    // Room enregistre automatiquement un enum sous forme de texte (EASY, MODERATE, HARD).
    val difficulty: Difficulty,
    val notes: String,
    val isFavorite: Boolean,
    val updatedAt: Long
)

/**
 * Petite table technique clé/valeur.
 * Elle sert au Lab 2 pour mémoriser que l'import JSON initial a déjà été fait.
 */
@Entity(tableName = "app_meta")
data class MetaEntity(
    @PrimaryKey val key: String,
    val value: String
)

/** Conversion base de données -> modèle métier utilisé par les écrans. */
fun TrailEntity.toTrail(): Trail = Trail(
    id = id, name = name, region = region, distanceKm = distanceKm,
    elevationGainM = elevationGainM, difficulty = difficulty, notes = notes,
    isFavorite = isFavorite, updatedAt = updatedAt
)

/** Conversion modèle métier -> ligne de base de données. */
fun Trail.toEntity(): TrailEntity = TrailEntity(
    id = id, name = name, region = region, distanceKm = distanceKm,
    elevationGainM = elevationGainM, difficulty = difficulty, notes = notes,
    isFavorite = isFavorite, updatedAt = updatedAt
)
