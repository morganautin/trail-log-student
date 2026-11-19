package fr.estiam.traillog.ui.visits

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import fr.estiam.traillog.R
import fr.estiam.traillog.databinding.ItemIssueBinding
import fr.estiam.traillog.model.Issue
import fr.estiam.traillog.model.IssueStatus
import fr.estiam.traillog.model.VisitFormatter

/** Liste des problèmes d'une visite : cocher = problème résolu, décocher = rouvert. */
class IssueAdapter(private val onToggle: (Issue, Boolean) -> Unit) :
    ListAdapter<Issue, IssueAdapter.Holder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(ItemIssueBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position), onToggle)

    class Holder(private val b: ItemIssueBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(issue: Issue, onToggle: (Issue, Boolean) -> Unit) {
            val ctx = b.root.context
            val resolved = issue.status == IssueStatus.RESOLVED
            // On retire l'écouteur avant de modifier la case (sinon le recyclage déclencherait un faux clic)
            b.issueResolved.setOnCheckedChangeListener(null)
            b.issueResolved.isChecked = resolved
            b.issueResolved.text = issue.description
            // Texte barré pour un problème résolu
            b.issueResolved.paintFlags = if (resolved) b.issueResolved.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                else b.issueResolved.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            b.issueDate.text = ctx.getString(R.string.issue_opened_on, VisitFormatter.dateTime(issue.createdAt))
            // Accessibilité : TalkBack lit la description + l'état ("résolu" / "ouvert")
            b.issueResolved.contentDescription = ctx.getString(
                if (resolved) R.string.issue_a11y_resolved else R.string.issue_a11y_open, issue.description)
            b.issueResolved.setOnCheckedChangeListener { _, checked -> onToggle(issue, checked) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Issue>() {
            override fun areItemsTheSame(old: Issue, new: Issue) = old.id == new.id
            override fun areContentsTheSame(old: Issue, new: Issue) = old == new
        }
    }
}
