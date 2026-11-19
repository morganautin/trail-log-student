package fr.estiam.traillog.model

enum class DistanceUnit { KM, MI }
enum class TrailSortOrder { NAME, DISTANCE }
data class UserSettings(
    val unit: DistanceUnit = DistanceUnit.KM,
    val sortOrder: TrailSortOrder = TrailSortOrder.NAME,
    // Projet final : ordre de la liste des visites (préférence qui change l'affichage)
    val visitSort: VisitSortOrder = VisitSortOrder.NEWEST_FIRST,
    // Projet final : délai (en jours) au-delà duquel un problème ouvert est "en retard"
    val followUpDays: Int = DEFAULT_FOLLOW_UP_DAYS
) {
    companion object {
        const val DEFAULT_FOLLOW_UP_DAYS = 7
        /** Valeurs proposées dans l'écran Paramètres. */
        val FOLLOW_UP_CHOICES = listOf(0, 3, 7)
    }
}

object TrailFormatter {
    fun distance(km: Double, unit: DistanceUnit): String =
        if (unit == DistanceUnit.KM) "%.1f km".format(java.util.Locale.getDefault(), km)
        else "%.1f mi".format(java.util.Locale.getDefault(), km * 0.621371)
}
