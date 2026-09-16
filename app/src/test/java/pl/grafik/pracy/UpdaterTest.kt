package pl.grafik.pracy

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.grafik.pracy.update.Updater

/** Porównanie wersji decyduje, czy zobaczysz pasek aktualizacji — musi być bezbłędne. */
class UpdaterTest {

    @Test
    fun nowsza_wersja_jest_rozpoznawana() {
        assertTrue(Updater.isNewer("1.1.1", "1.1.0"))
        assertTrue(Updater.isNewer("1.2.0", "1.1.9"))
        assertTrue(Updater.isNewer("2.0.0", "1.9.9"))
    }

    @Test
    fun ta_sama_wersja_nie_proponuje_aktualizacji() {
        assertFalse(Updater.isNewer("1.1.0", "1.1.0"))
    }

    @Test
    fun starsza_wersja_nie_proponuje_aktualizacji() {
        assertFalse(Updater.isNewer("1.0.9", "1.1.0"))
        assertFalse(Updater.isNewer("1.1.0", "2.0.0"))
    }

    @Test
    fun dziesiatki_porownywane_liczbowo_a_nie_tekstowo() {
        // tekstowo "1.10.0" < "1.9.0", liczbowo odwrotnie
        assertTrue(Updater.isNewer("1.10.0", "1.9.0"))
        assertFalse(Updater.isNewer("1.9.0", "1.10.0"))
    }

    @Test
    fun brakujace_czlony_traktowane_jak_zero() {
        assertTrue(Updater.isNewer("1.2", "1.1.9"))
        assertFalse(Updater.isNewer("1.1", "1.1.0"))
        assertTrue(Updater.isNewer("1.1.1", "1.1"))
    }

    @Test
    fun przedrostek_v_i_sufiksy_nie_psuja_porownania() {
        assertTrue(Updater.isNewer("1.1.1-debug", "1.1.0"))
        assertFalse(Updater.isNewer("1.1.0-rc1", "1.1.0"))
    }

    @Test
    fun smieci_nie_wywolaja_falszywej_aktualizacji() {
        assertFalse(Updater.isNewer("", "1.1.0"))
        assertFalse(Updater.isNewer("latest", "1.1.0"))
    }
}
