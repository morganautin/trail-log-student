package fr.estiam.traillog.ui.visits

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import fr.estiam.traillog.R
import fr.estiam.traillog.databinding.ItemVisitBinding
import fr.estiam.traillog.model.ConditionLevel
import fr.estiam.traillog.model.VisitFormatter
import fr.estiam.traillog.model.VisitSummary

/** Adapter de la liste des visites (ListAdapter + DiffUtil : seules les lignes modifiées sont redessinées). */
class VisitAdapter(private val onClick: (VisitSummary) -> Unit) :
    ListAdapter<VisitSummary, VisitAdapter.Holder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(ItemVisitBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position), onClick)

    class Holder(private val b: ItemVisitBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: VisitSummary, onClick: (VisitSummary) -> Unit) {
            val ctx = b.root.context
            val date = VisitFormatter.dateTime(item.visit.visitedAt)
            val type = VisitLabels.type(ctx, item.visit.type)
            val issues = VisitLabels.openIssues(ctx, item.openIssues)

            // Les 4 informations demandées : sentier, date/heure, type, nombre de problèmes ouverts
            b.visitTrailName.text = item.trailName
            b.visitDate.text = date
            b.visitType.text = type
            b.visitOpenIssues.text = issues
            // Pastille colorée = niveau d'état (même code couleur que l'indicateur du détail)
            b.visitConditionDot.setColorFilter(
                ConditionIndicatorView.colorFor(ConditionLevel.fromScore(item.visit.conditionScore)))

            // Accessibilité : une seule phrase complète lue par TalkBack pour toute la ligne
            b.root.contentDescription = ctx.getString(R.string.visit_item_a11y,
                item.trailName, date, type, issues, item.visit.conditionScore)
            b.root.setOnClickListener { onClick(item) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<VisitSummary>() {
            override fun areItemsTheSame(old: VisitSummary, new: VisitSummary) = old.visit.id == new.visit.id
            override fun areContentsTheSame(old: VisitSummary, new: VisitSummary) = old == new
        }
    }
}
