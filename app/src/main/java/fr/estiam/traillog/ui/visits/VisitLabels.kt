package fr.estiam.traillog.ui.visits

import android.content.Context
import fr.estiam.traillog.R
import fr.estiam.traillog.model.VisitType

/** Libellés traduits des types de visite (les enums restent indépendants d'Android). */
object VisitLabels {
    fun typeRes(type: VisitType): Int = when (type) {
        VisitType.INSPECTION -> R.string.visit_type_inspection
        VisitType.SIGNAGE -> R.string.visit_type_signage
        VisitType.DRAINAGE -> R.string.visit_type_drainage
        VisitType.EROSION -> R.string.visit_type_erosion
        VisitType.HAZARD_CHECK -> R.string.visit_type_hazard
    }

    fun type(context: Context, type: VisitType): String = context.getString(typeRes(type))

    /** "Aucun problème ouvert" / "1 problème ouvert" / "3 problèmes ouverts". */
    fun openIssues(context: Context, count: Int): String =
        if (count == 0) context.getString(R.string.open_issues_none)
        else context.resources.getQuantityString(R.plurals.open_issues, count, count)
}
