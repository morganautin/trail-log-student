package fr.estiam.traillog

import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.estiam.traillog.ui.MainActivity
import fr.estiam.traillog.ui.TrailPagingAdapter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * M5 Lab 2 : tests d'interface Espresso sur l'écran principal (catalogue des sentiers).
 *
 * Ces tests tournent sur l'émulateur, avec la VRAIE base Room de l'application.
 * Règles respectées :
 * - pas de Thread.sleep : on attend la mise à jour de la liste via un IdlingResource ;
 * - pas de position d'adapter : on retrouve la ligne par son contenu (le nom du sentier) ;
 * - isolation : nom unique à chaque exécution + suppression du seul sentier créé par le test.
 */
@RunWith(AndroidJUnit4::class)
class TrailListEspressoTest {

    // Lance MainActivity avant chaque test et la ferme après
    @get:Rule val activityRule = ActivityScenarioRule(MainActivity::class.java)

    // Nom UNIQUE pour chaque exécution : un sentier laissé par une exécution précédente
    // ne peut donc jamais faire passer le test par erreur (pas de faux positif).
    // "AAA" + 999 km → le sentier arrive en tête de liste, que le tri soit NAME (A→Z)
    // ou DISTANCE (du plus long au plus court) : il est dans la première page chargée.
    private val uniqueName = "AAA Espresso ${System.currentTimeMillis()}"

    // IdlingResource enregistré pendant le test (pour pouvoir le retirer dans @After)
    private var idlingResource: IdlingResource? = null

    @After
    fun tearDown() {
        // 1. On retire l'IdlingResource pour ne pas bloquer les tests suivants
        idlingResource?.let { IdlingRegistry.getInstance().unregister(it) }

        // 2. Nettoyage : on supprime UNIQUEMENT le(s) sentier(s) portant notre nom unique,
        //    sans toucher aux autres données de l'application.
        val app = ApplicationProvider.getApplicationContext<TrailLogApplication>()
        runBlocking {
            app.trails.observeTrails().first()
                .filter { it.name == uniqueName }
                .forEach { app.trails.delete(it.id) }
        }
    }

    // ------------------------------------------------------------------
    // Test 1 : la liste des sentiers est affichée au lancement
    // ------------------------------------------------------------------
    @Test
    fun trailList_isDisplayedOnLaunch() {
        // Arrange : MainActivity est lancée par la règle
        // Act + Assert : le RecyclerView est visible à l'écran
        onView(withId(R.id.trailList)).check(matches(isDisplayed()))
    }

    // ------------------------------------------------------------------
    // Test 2 : le bouton "+" ouvre le formulaire d'ajout
    // ------------------------------------------------------------------
    @Test
    fun addButton_opensEditForm() {
        // Act : clic sur le bouton flottant d'ajout
        onView(withId(R.id.addButton)).perform(click())

        // Assert : les champs du formulaire et le bouton Enregistrer sont affichés
        onView(withId(R.id.nameInput)).check(matches(isDisplayed()))
        onView(withId(R.id.distanceInput)).check(matches(isDisplayed()))
        onView(withId(R.id.saveButton)).check(matches(isDisplayed()))
    }

    // ------------------------------------------------------------------
    // Test 3 : parcours complet d'ajout puis vérification dans la liste
    // ------------------------------------------------------------------
    @Test
    fun addTrail_savesAndShowsNewRowInList() {
        // Arrange : on récupère le RecyclerView de MainActivity pour pouvoir
        // surveiller ses données paginées pendant le test
        lateinit var list: RecyclerView
        activityRule.scenario.onActivity { activity ->
            list = activity.findViewById(R.id.trailList)
        }

        // Act 1 : ouvrir le formulaire
        onView(withId(R.id.addButton)).perform(click())

        // Act 2 : remplir le formulaire (replaceText = saisie directe, plus fiable que typeText)
        onView(withId(R.id.nameInput)).perform(replaceText(uniqueName), closeSoftKeyboard())
        onView(withId(R.id.regionInput)).perform(replaceText("Alpes"), closeSoftKeyboard())
        onView(withId(R.id.distanceInput)).perform(replaceText("999"), closeSoftKeyboard())
        onView(withId(R.id.elevationInput)).perform(replaceText("150"), closeSoftKeyboard())

        // Act 3 : enregistrer (scrollTo car le formulaire est dans un ScrollView)
        onView(withId(R.id.saveButton)).perform(scrollTo(), click())

        // Synchronisation SANS Thread.sleep :
        // l'enregistrement Room et le rechargement Paging 3 se font en arrière-plan.
        // On enregistre un IdlingResource qui ne devient "idle" que lorsque le nouveau
        // sentier est présent dans les données de la liste. Espresso attend
        // automatiquement cet état avant l'interaction suivante.
        // (Enregistré APRÈS le clic, sinon Espresso attendrait aussi avant le clic → blocage.)
        idlingResource = TrailVisibleIdlingResource(list, uniqueName).also {
            IdlingRegistry.getInstance().register(it)
        }

        // Assert 1 : on est revenu sur la liste
        onView(withId(R.id.trailList)).check(matches(isDisplayed()))

        // Assert 2 : on fait défiler jusqu'à la ligne qui CONTIENT notre nom (par contenu,
        // pas par position), puis on vérifie que ce texte est affiché
        onView(withId(R.id.trailList)).perform(
            RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(hasDescendant(withText(uniqueName)))
        )
        onView(withText(uniqueName)).check(matches(isDisplayed()))
    }
}

/**
 * IdlingResource maison : "idle" (= Espresso peut continuer) seulement quand
 * l'adapter paginé de la liste contient un sentier portant le nom attendu.
 *
 * Il est réveillé par addOnPagesUpdatedListener, appelé par Paging 3 à chaque
 * nouvelle page affichée : aucune attente arbitraire, uniquement des événements réels.
 */
private class TrailVisibleIdlingResource(
    recyclerView: RecyclerView,
    private val expectedName: String
) : IdlingResource {

    private val adapter = recyclerView.adapter as TrailPagingAdapter
    private var callback: IdlingResource.ResourceCallback? = null

    // Écouteur appelé par Paging 3 quand les données affichées changent
    private val pagesListener: () -> Unit = {
        if (containsExpected()) callback?.onTransitionToIdle()
    }

    override fun getName(): String = "TrailVisible($expectedName)"

    // Appelé par Espresso sur le thread principal
    override fun isIdleNow(): Boolean = containsExpected()

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback) {
        this.callback = callback
        adapter.addOnPagesUpdatedListener(pagesListener)
    }

    // snapshot() = les éléments actuellement chargés dans l'adapter paginé
    private fun containsExpected(): Boolean =
        adapter.snapshot().items.any { it.name == expectedName }
}
