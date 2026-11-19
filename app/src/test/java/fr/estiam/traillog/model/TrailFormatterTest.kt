package fr.estiam.traillog.model

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

/**
 * Tests ciblés de TrailFormatter.distance(km, unit).
 * Le formatage dépend de la Locale (point ou virgule décimale) : on la fixe
 * explicitement avant chaque test et on restaure l'ancienne après.
 */
class TrailFormatterTest {

    private lateinit var previous: Locale

    @Before
    fun fixLocale() {
        previous = Locale.getDefault()   // on mémorise la Locale de la machine
        Locale.setDefault(Locale.US)     // Locale.US → point décimal, résultat prévisible
    }

    @After
    fun restoreLocale() {
        Locale.setDefault(previous)      // on remet la Locale d'origine (isolation des tests)
    }

    @Test fun kilometerDisplayKeepsSourceValue() {
        // Arrange / Act / Assert : en KM, la valeur est affichée telle quelle avec 1 décimale
        assertEquals("10.0 km", TrailFormatter.distance(10.0, DistanceUnit.KM))
    }

    @Test fun milesConversionUsesCorrectFactor() {
        // 10 km × 0.621371 = 6.21 → arrondi à 1 décimale
        assertEquals("6.2 mi", TrailFormatter.distance(10.0, DistanceUnit.MI))
    }

    @Test fun zeroDistanceInKilometers() {
        // Cas limite : distance nulle
        assertEquals("0.0 km", TrailFormatter.distance(0.0, DistanceUnit.KM))
    }

    @Test fun zeroDistanceInMiles() {
        // Cas limite : 0 km reste 0 mi après conversion
        assertEquals("0.0 mi", TrailFormatter.distance(0.0, DistanceUnit.MI))
    }

    @Test fun fixtureDistanceInMilesIsRounded() {
        // Pine Loop : 3.2 km × 0.621371 = 1.988 → arrondi à "2.0 mi"
        assertEquals("2.0 mi", TrailFormatter.distance(3.2, DistanceUnit.MI))
    }

    @Test fun frenchLocaleUsesDecimalComma() {
        // On change la Locale DANS le test : @After la restaurera de toute façon
        Locale.setDefault(Locale.FRANCE)
        assertEquals("3,2 km", TrailFormatter.distance(3.2, DistanceUnit.KM))
    }
}
