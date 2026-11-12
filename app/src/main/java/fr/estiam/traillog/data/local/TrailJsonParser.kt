package fr.estiam.traillog.data.local

import fr.estiam.traillog.model.Difficulty
import fr.estiam.traillog.model.Trail
import org.json.JSONArray

/**
 * Lab 2 — Lecture du fichier assets/trails.json.
 * Transforme le texte JSON en objets [Trail] et écarte les entrées invalides.
 */
object TrailJsonParser {

    fun parse(json: String): List<Trail> {
        // 1. Le fichier est un tableau JSON : [ { ... }, { ... } ]
        val array = JSONArray(json)
        val result = mutableListOf<Trail>()
        // On garde la trace des ids déjà vus pour ignorer les doublons du fichier.
        val seenIds = mutableSetOf<String>()

        for (i in 0 until array.length()) {
            // 2. Une entrée qui n'est pas un objet est simplement ignorée.
            val item = array.optJSONObject(i) ?: continue

            // 3. Mapping champ par champ. Les "opt..." évitent un crash si un
            //    champ manque : une valeur par défaut est utilisée à la place.
            val trail = Trail(
                id = item.optString("id").trim(),
                name = item.optString("name").trim(),
                region = item.optString("region").trim(),
                distanceKm = item.optDouble("distanceKm", -1.0), // -1 => rejeté par isValid()
                elevationGainM = item.optInt("elevationGainM", -1),
                difficulty = parseDifficulty(item.optString("difficulty")),
                notes = item.optString("notes"),
                isFavorite = item.optBoolean("isFavorite", false)
            )

            // 4. Validation : id et nom non vides, distance et dénivelé non négatifs.
            //    add() renvoie false si l'id a déjà été rencontré.
            if (trail.isValid() && seenIds.add(trail.id)) result += trail
        }
        return result
    }

    /** Texte -> enum ; une valeur inconnue retombe sur MODERATE au lieu de planter. */
    private fun parseDifficulty(raw: String): Difficulty =
        Difficulty.entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) }
            ?: Difficulty.MODERATE
}
