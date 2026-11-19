package fr.estiam.traillog.ui.visits

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import fr.estiam.traillog.R
import fr.estiam.traillog.TrailLogApplication
import fr.estiam.traillog.databinding.ActivityVisitListBinding
import fr.estiam.traillog.model.VisitSortOrder
import fr.estiam.traillog.ui.SettingsActivity
import fr.estiam.traillog.worker.OverdueFollowUpWorker
import kotlinx.coroutines.launch

/** Écran 1 du carnet de terrain : liste des visites d'entretien. */
class VisitListActivity : AppCompatActivity() {
    private lateinit var b: ActivityVisitListBinding
    private val app get() = application as TrailLogApplication
    private val vm: VisitListViewModel by viewModels { VisitListViewModel.Factory(app.visits, app.settings) }
    private val adapter = VisitAdapter { summary ->
        // Navigation liste → détail
        startActivity(Intent(this, VisitDetailActivity::class.java)
            .putExtra(VisitDetailActivity.EXTRA_VISIT_ID, summary.visit.id))
    }

    // Android 13+ : demande de la permission de notification (pour la tâche de fond)
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) Snackbar.make(b.root, R.string.followup_permission_denied, Snackbar.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityVisitListBinding.inflate(layoutInflater); setContentView(b.root)
        setSupportActionBar(b.visitToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        b.visitList.layoutManager = LinearLayoutManager(this)
        b.visitList.adapter = adapter
        b.addVisitButton.setOnClickListener { startActivity(Intent(this, EditVisitActivity::class.java)) }

        askNotificationPermissionIfNeeded()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Seed : d'abord les sentiers (déjà existant), puis les visites de démonstration
                launch {
                    try {
                        app.trails.seedIfNeeded()
                        app.visits.seedIfNeeded()
                    } catch (e: Exception) {
                        Log.e(TAG, "Échec du seed", e)
                        showError()
                    }
                }
                // Liste triée selon la préférence ; état vide si aucune visite
                launch {
                    vm.visits.collect { list ->
                        b.visitProgress.visibility = View.GONE
                        adapter.submitList(list)
                        b.visitEmptyText.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
                // Sous-titre : rappelle l'ordre choisi dans les Paramètres
                launch {
                    vm.sortOrder.collect { order ->
                        supportActionBar?.subtitle = getString(
                            if (order == VisitSortOrder.NEWEST_FIRST) R.string.visit_sort_newest else R.string.visit_sort_oldest)
                    }
                }
            }
        }
    }

    private fun showError() {
        b.visitProgress.visibility = View.GONE
        b.visitEmptyText.visibility = View.VISIBLE
        b.visitEmptyText.setText(R.string.visits_load_error)
    }

    private fun askNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_visits, menu); return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_check_followups -> {
            // Lance immédiatement le même Worker que la tâche périodique (démonstration)
            OverdueFollowUpWorker.runNow(this)
            Snackbar.make(b.root, R.string.followup_check_started, Snackbar.LENGTH_SHORT).show()
            true
        }
        R.id.action_visit_settings -> { startActivity(Intent(this, SettingsActivity::class.java)); true }
        android.R.id.home -> { finish(); true }
        else -> super.onOptionsItemSelected(item)
    }

    companion object { private const val TAG = "VisitListActivity" }
}
