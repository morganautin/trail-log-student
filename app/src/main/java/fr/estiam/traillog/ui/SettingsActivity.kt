package fr.estiam.traillog.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import fr.estiam.traillog.TrailLogApplication
import fr.estiam.traillog.databinding.ActivitySettingsBinding
import fr.estiam.traillog.model.DistanceUnit
import fr.estiam.traillog.model.TrailSortOrder
import fr.estiam.traillog.model.VisitSortOrder
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {
    private lateinit var b: ActivitySettingsBinding
    private val prefs get() = (application as TrailLogApplication).settings
    // Évite de réécrire la préférence pendant qu'on coche les boutons depuis les valeurs enregistrées
    private var loading = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivitySettingsBinding.inflate(layoutInflater); setContentView(b.root)
        b.unitsToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked && !loading) lifecycleScope.launch {
                prefs.setUnit(if (checkedId == b.miButton.id) DistanceUnit.MI else DistanceUnit.KM)
            }
        }
        b.sortRadio.setOnCheckedChangeListener { _, id ->
            if (!loading) lifecycleScope.launch { prefs.setSort(if (id == b.sortDistance.id) TrailSortOrder.DISTANCE else TrailSortOrder.NAME) }
        }
        // Projet final : ordre des visites
        b.visitSortRadio.setOnCheckedChangeListener { _, id ->
            if (!loading) lifecycleScope.launch {
                prefs.setVisitSort(if (id == b.visitSortOldest.id) VisitSortOrder.OLDEST_FIRST else VisitSortOrder.NEWEST_FIRST)
            }
        }
        // Projet final : délai de relance (0, 3 ou 7 jours)
        b.followUpRadio.setOnCheckedChangeListener { _, id ->
            if (!loading) lifecycleScope.launch {
                prefs.setFollowUpDays(when (id) { b.followUp0.id -> 0; b.followUp3.id -> 3; else -> 7 })
            }
        }
        lifecycleScope.launch {
            prefs.settings.collect { settings ->
                loading = true
                b.unitsToggle.check(if (settings.unit == DistanceUnit.KM) b.kmButton.id else b.miButton.id)
                b.sortRadio.check(if (settings.sortOrder == TrailSortOrder.NAME) b.sortName.id else b.sortDistance.id)
                b.visitSortRadio.check(if (settings.visitSort == VisitSortOrder.NEWEST_FIRST) b.visitSortNewest.id else b.visitSortOldest.id)
                b.followUpRadio.check(when (settings.followUpDays) { 0 -> b.followUp0.id; 3 -> b.followUp3.id; else -> b.followUp7.id })
                loading = false
                b.settingsStatus.text = "${settings.unit.name} · ${settings.sortOrder.name} · ${settings.visitSort.name} · ${settings.followUpDays} j"
            }
        }
    }
}
