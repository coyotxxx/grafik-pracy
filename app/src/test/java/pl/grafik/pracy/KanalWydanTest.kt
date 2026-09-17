package pl.grafik.pracy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pl.grafik.pracy.update.Updater

/**
 * Nowy wygląd ma własny kanał wydań — tagi `nowy-vN`, gdzie N to numer paczki.
 * Dzięki temu klasyczna aplikacja (która pyta o `latest`) nigdy nie dostanie APK
 * nowego wyglądu, a nowy wygląd wie, czy na GitHubie leży coś nowszego.
 */
class KanalWydanTest {

    @Test
    fun numer_paczki_wychodzi_z_tagu() {
        assertEquals(45, Updater.numerPaczki("nowy-v45", "nowy-"))
        assertEquals(7, Updater.numerPaczki("nowy-v7", "nowy-"))
        // bez „v" też ma działać — tag można kiedyś zapisać inaczej
        assertEquals(12, Updater.numerPaczki("nowy-12", "nowy-"))
    }

    @Test
    fun tag_bez_numeru_nie_jest_paczka() {
        assertNull(Updater.numerPaczki("nowy-wyglad", "nowy-"))
        assertNull(Updater.numerPaczki("nowy-v", "nowy-"))
    }

    @Test
    fun wydania_klasycznej_nie_trafiaja_do_kanalu_nowego() {
        // tag klasycznego wydania nie zaczyna się od prefiksu, więc kanał go pominie
        assertEquals(false, "v1.29.0".startsWith("nowy-"))
    }
}
