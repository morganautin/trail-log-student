package fr.estiam.traillog.ui.visits

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import fr.estiam.traillog.R
import fr.estiam.traillog.TrailLogApplication
import fr.estiam.traillog.databinding.ActivityEditVisitBinding
import fr.estiam.traillog.model.ConditionLevel
import fr.estiam.traillog.model.Trail
import fr.estiam.traillog.model.Visit
import fr.estiam.traillog.model.VisitType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Écran 3 : création d'une visite (sans extra) ou modification (extra EXTRA_VISIT_ID).
 * Après une création, on ouvre directement le détail de la nouvelle visite.
 */
class EditVisitActivity : AppCompatActivity() {
    private lateinit var b: ActivityEditVisitBinding
    private val app get() = application as TrailLogApplication
    private var trails: List<Trail> = emptyList()
    private var original: Visit? = null
    private val types = VisitType.entries

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityEditVisitBinding.inflate(layoutInflater); setContentView(b.root)
        val editId = intent.getLongExtra(VisitDetailActivity.EXTRA_VISIT_ID, 0L)
        b.editVisitTitle.setText(if (editId == 0L) R.string.new_visit else R.string.edit_visit)

        // Liste des types de visite (libellés traduits)
        b.typeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
            types.map { VisitLabels.type(this, it) })

        // Note d'état : la jauge de prévisualisation suit le curseur en direct
        b.conditionSeek.max = 100
        b.conditionSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) = showScore(progress)
            override fun onStartTrackingTouch(seekBar: SeekBar) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
        })
        b.conditionSeek.progress = DEFAULT_SCORE
        showScore(DEFAULT_SCORE)

        b.saveVisitButton.isEnabled = false   // activé une fois les sentiers chargés
        b.saveVisitButton.setOnClickListener { save() }

        lifecycleScope.launch {
            // 1. Sentiers disponibles (triés par nom par le DAO existant)
            trails = app.trails.observeTrails().first()
            if (trails.isEmpty()) {
                // État vide : impossible de créer une visite sans sentier
                b.trailEmptyText.visibility = android.view.View.VISIBLE
                return@launch
            }
            b.trailSpinner.adapter = ArrayAdapter(this@EditVisitActivity,
                android.R.layout.simple_spinner_dropdown_item, trails.map { it.name })

            // 2. En modification : pré-remplir le formulaire
            if (editId != 0L) {
                val visit = app.visits.getVisit(editId)
                if (visit == null) {
                    Snackbar.make(b.root, R.string.visit_missing, Snackbar.LENGTH_LONG).show()
                    return@launch
                }
                original = visit
                b.trailSpinner.setSelection(trails.indexOfFirst { it.id == visit.trailId }.coerceAtLeast(0))
                b.typeSpinner.setSelection(types.indexOf(visit.type).coerceAtLeast(0))
                b.conditionSeek.progress = visit.conditionScore
                b.visitNotesInput.setText(visit.notes)
            }
            b.saveVisitButton.isEnabled = true
        }
    }

    /** Met à jour le libellé et la jauge de prévisualisation. */
    private fun showScore(score: Int) {
        val level = ConditionLevel.fromScore(score)
        b.conditionValue.text = getString(R.string.condition_label, score, getString(ConditionIndicatorView.labelRes(level)))
        b.conditionPreview.setScore(score)
    }

    private fun save() {
        val trail = trails.getOrNull(b.trailSpinner.selectedItemPosition)
        if (trail == null) {
            Snackbar.make(b.root, R.string.visit_invalid, Snackbar.LENGTH_LONG).show(); return
        }
        val visit = Visit(
            id = original?.id ?: 0L,
            trailId = trail.id,
            // En création : date/heure actuelles ; en modification : on garde la date d'origine
            visitedAt = original?.visitedAt ?: System.currentTimeMillis(),
            type = types[b.typeSpinner.selectedItemPosition.coerceIn(0, types.size - 1)],
            conditionScore = b.conditionSeek.progress,
            notes = b.visitNotesInput.text?.toString()?.trim().orEmpty()
        )
        if (!visit.isValid()) {
            Snackbar.make(b.root, R.string.visit_invalid, Snackbar.LENGTH_LONG).show(); return
        }
        b.saveVisitButton.isEnabled = false   // évite un double enregistrement
        lifecycleScope.launch {
            val id = app.visits.saveVisit(visit)
            if (original == null) {
                // Création → on enchaîne sur le détail de la nouvelle visite
                startActivity(Intent(this@EditVisitActivity, VisitDetailActivity::class.java)
                    .putExtra(VisitDetailActivity.EXTRA_VISIT_ID, id))
            }
            finish()
        }
    }

    companion object { private const val DEFAULT_SCORE = 50 }
}
