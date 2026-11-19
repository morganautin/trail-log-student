package fr.estiam.traillog.ui.visits

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import fr.estiam.traillog.R
import fr.estiam.traillog.TrailLogApplication
import fr.estiam.traillog.databinding.ActivityVisitDetailBinding
import fr.estiam.traillog.model.ConditionLevel
import fr.estiam.traillog.model.VisitFormatter
import kotlinx.coroutines.launch

/** Écran 2 du carnet de terrain : détail d'une visite, indicateur d'état et suivi des problèmes. */
class VisitDetailActivity : AppCompatActivity() {
    private lateinit var b: ActivityVisitDetailBinding
    private val app get() = application as TrailLogApplication
    private var visitId: Long = 0

    private val issueAdapter = IssueAdapter { issue, resolved ->
        // Mise à jour du statut : la liste et le compteur se rafraîchissent via les Flow Room
        lifecycleScope.launch { app.visits.setIssueResolved(issue.id, resolved) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityVisitDetailBinding.inflate(layoutInflater); setContentView(b.root)
        visitId = intent.getLongExtra(EXTRA_VISIT_ID, 0L)
        if (visitId == 0L) { finish(); return }   // identifiant absent : rien à afficher

        b.issueList.layoutManager = LinearLayoutManager(this)
        b.issueList.adapter = issueAdapter
        b.issueList.isNestedScrollingEnabled = false

        b.editVisitButton.setOnClickListener {
            startActivity(Intent(this, EditVisitActivity::class.java).putExtra(EXTRA_VISIT_ID, visitId))
        }
        b.addIssueButton.setOnClickListener { addIssue() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Visite (avec nom du sentier et nombre de problèmes ouverts)
                launch {
                    app.visits.observeVisit(visitId).collect { summary ->
                        if (summary == null) {
                            // État d'erreur : la visite a été supprimée ou n'existe pas
                            b.visitDetailTrail.setText(R.string.visit_missing)
                            b.visitDetailContent.visibility = View.GONE
                            return@collect
                        }
                        b.visitDetailContent.visibility = View.VISIBLE
                        val visit = summary.visit
                        val level = ConditionLevel.fromScore(visit.conditionScore)
                        b.visitDetailTrail.text = summary.trailName
                        b.visitDetailDate.text = VisitFormatter.dateTime(visit.visitedAt)
                        b.visitDetailType.text = VisitLabels.type(this@VisitDetailActivity, visit.type)
                        b.conditionIndicator.setScore(visit.conditionScore)
                        b.conditionLabel.text = getString(R.string.condition_label, visit.conditionScore,
                            getString(ConditionIndicatorView.labelRes(level)))
                        b.visitDetailNotes.text = visit.notes.ifBlank { getString(R.string.no_notes) }
                        b.issuesHeader.text = VisitLabels.openIssues(this@VisitDetailActivity, summary.openIssues)
                    }
                }
                // Problèmes de la visite ; message si la liste est vide
                launch {
                    app.visits.observeIssues(visitId).collect { issues ->
                        issueAdapter.submitList(issues)
                        b.noIssuesText.visibility = if (issues.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
            }
        }
    }

    private fun addIssue() {
        val text = b.newIssueInput.text?.toString()?.trim().orEmpty()
        if (text.isBlank()) {
            // Erreur de saisie : message sous le champ, lu par TalkBack
            b.newIssueLayout.error = getString(R.string.issue_empty_error)
            return
        }
        b.newIssueLayout.error = null
        lifecycleScope.launch {
            app.visits.addIssue(visitId, text)
            b.newIssueInput.setText("")
            Snackbar.make(b.root, R.string.issue_added, Snackbar.LENGTH_SHORT).show()
        }
    }

    companion object { const val EXTRA_VISIT_ID = "visit_id" }
}
