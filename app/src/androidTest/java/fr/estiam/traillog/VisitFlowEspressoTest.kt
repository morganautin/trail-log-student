package fr.estiam.traillog

import android.Manifest
import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import fr.estiam.traillog.model.VisitSortOrder
import fr.estiam.traillog.ui.visits.ConditionIndicatorView
import fr.estiam.traillog.ui.visits.VisitDetailActivity
import fr.estiam.traillog.ui.visits.VisitListActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Projet final – tests Espresso des parcours critiques du carnet de visites :
 * 1. liste (données de démonstration importées) → détail avec l'indicateur d'état ;
 * 2. création d'une visite → ouverture automatique de son détail.
 *
 * Synchronisation sans Thread.sleep : on attend des CONDITIONS réelles
 * (liste remplie, bouton activé, texte affiché) via un IdlingResource.
 */
@RunWith(AndroidJUnit4::class)
class VisitFlowEspressoTest {

    // 1. Accorde la permission de notification AVANT le lancement : pas de boîte de dialogue système
    @get:Rule(order = 0)
    val notificationPermission: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    // 2. Lance l'écran de liste des visites
    @get:Rule(order = 1)
    val activityRule = ActivityScenarioRule(VisitListActivity::class.java)

    // Notes uniques : la visite créée par le test est identifiable et supprimée ensuite
    private val uniqueNotes = "Espresso ${System.currentTimeMillis()}"

    @After
    fun cleanUp() {
        // On supprime UNIQUEMENT la visite créée par ce test
        val app = ApplicationProvider.getApplicationContext<TrailLogApplication>()
        runBlocking {
            app.visits.observeVisits(VisitSortOrder.NEWEST_FIRST).first()
                .filter { it.visit.notes == uniqueNotes }
                .forEach { app.visits.deleteVisit(it.visit.id) }
        }
    }

    @Test
    fun visitList_showsSeededVisits_andOpensDetailWithConditionIndicator() {
        // Arrange : attendre que les visites de démonstration soient importées et affichées
        awaitCondition("liste remplie") {
            (resumedActivity()?.findViewById<RecyclerView>(R.id.visitList)?.adapter?.itemCount ?: 0) > 0
        }
        onView(withId(R.id.visitList)).check(matches(isDisplayed()))

        // Act : toucher la première visite de la liste
        onView(withId(R.id.visitList)).perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

        // Assert : l'écran de détail est chargé (nom du sentier affiché)…
        awaitCondition("détail chargé") {
            // Le titre (nom du sentier) n'est rempli qu'une fois la visite lue dans Room
            val activity: VisitDetailActivity? = (resumedActivity() as? VisitDetailActivity)
            val title: CharSequence? = activity?.findViewById<TextView>(R.id.visitDetailTrail)?.text
            title != null && title.isNotBlank()
        }
        onView(withId(R.id.visitDetailTrail)).check(matches(isDisplayed()))
        // …avec la vue personnalisée visible et une description accessible renseignée
        onView(withId(R.id.conditionIndicator)).check(matches(isDisplayed()))
        val indicator = (resumedActivityOnMain() as VisitDetailActivity)
            .findViewById<ConditionIndicatorView>(R.id.conditionIndicator)
        assertTrue("L'indicateur doit avoir une description accessible",
            !indicator.contentDescription.isNullOrBlank())
    }

    @Test
    fun createVisit_savesAndOpensItsDetail() {
        // Act 1 : ouvrir le formulaire de création
        onView(withId(R.id.addVisitButton)).perform(click())

        // Attendre que les sentiers soient chargés (le bouton Enregistrer s'active alors)
        awaitCondition("formulaire prêt") {
            resumedActivity()?.findViewById<Button>(R.id.saveVisitButton)?.isEnabled == true
        }

        // Act 2 : saisir des notes uniques puis enregistrer (sentier et type : valeurs par défaut)
        onView(withId(R.id.visitNotesInput)).perform(scrollTo(), replaceText(uniqueNotes), closeSoftKeyboard())
        onView(withId(R.id.saveVisitButton)).perform(scrollTo(), click())

        // Assert : après l'enregistrement Room, le détail de la NOUVELLE visite s'ouvre avec nos notes
        awaitCondition("détail de la visite créée") {
            val activity: VisitDetailActivity? = (resumedActivity() as? VisitDetailActivity)
            val notes: String? = activity?.findViewById<TextView>(R.id.visitDetailNotes)?.text?.toString()
            notes == uniqueNotes
        }
        onView(withId(R.id.visitDetailNotes)).check(matches(withText(uniqueNotes)))
        onView(withId(R.id.conditionIndicator)).check(matches(isDisplayed()))
    }

    // ------------------------------------------------------------------
    // Outils de synchronisation
    // ------------------------------------------------------------------

    /** Activité au premier plan (à appeler sur le thread principal). */
    private fun resumedActivity(): Activity? =
        ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).firstOrNull()

    /** Même chose, depuis le thread du test. */
    private fun resumedActivityOnMain(): Activity? {
        var result: Activity? = null
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync { result = resumedActivity() }
        return result
    }

    /**
     * Bloque le test (via Espresso) jusqu'à ce que [condition] soit vraie, puis retire l'IdlingResource.
     * Si la condition ne devient jamais vraie, Espresso échoue avec un timeout explicite.
     */
    private fun awaitCondition(name: String, condition: () -> Boolean) {
        val resource = ConditionIdlingResource(name, condition)
        IdlingRegistry.getInstance().register(resource)
        try {
            onIdle()   // Espresso attend que toutes les ressources soient "idle"
        } finally {
            IdlingRegistry.getInstance().unregister(resource)
        }
    }
}

/**
 * IdlingResource basé sur une condition : "idle" quand la condition est vraie.
 * La condition est réévaluée sur le thread principal toutes les 50 ms jusqu'à ce qu'elle soit remplie,
 * puis Espresso est prévenu (onTransitionToIdle). Ce n'est pas une attente fixe : on avance dès que c'est prêt.
 */
private class ConditionIdlingResource(
    private val label: String,
    private val condition: () -> Boolean
) : IdlingResource {
    private val handler = Handler(Looper.getMainLooper())
    @Volatile private var callback: IdlingResource.ResourceCallback? = null

    private val poll = object : Runnable {
        override fun run() {
            if (condition()) callback?.onTransitionToIdle() else handler.postDelayed(this, 50)
        }
    }

    override fun getName(): String = "Condition($label)"

    override fun isIdleNow(): Boolean = condition()

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback) {
        this.callback = callback
        handler.post(poll)
    }
}
