package pl.grafik.pracy

import org.junit.Assert.assertEquals
import org.junit.Test
import pl.grafik.pracy.ui.screens.sformatujCzas

/** Wpisujesz same cyfry — apka układa z nich godzinę. */
class CzasTest {

    @Test
    fun cztery_cyfry_to_godzina_i_minuty() {
        assertEquals("09:20", sformatujCzas("0920"))
        assertEquals("14:45", sformatujCzas("1445"))
        assertEquals("23:59", sformatujCzas("2359"))
    }

    @Test
    fun trzy_cyfry_to_jednocyfrowa_godzina() {
        assertEquals("09:20", sformatujCzas("920"))
        assertEquals("07:05", sformatujCzas("705"))
    }

    @Test
    fun jedna_lub_dwie_cyfry_to_pelna_godzina() {
        assertEquals("09:00", sformatujCzas("9"))
        assertEquals("14:00", sformatujCzas("14"))
    }

    @Test
    fun kropka_przecinek_i_dwukropek_sa_ignorowane() {
        assertEquals("09:20", sformatujCzas("9.20"))
        assertEquals("09:20", sformatujCzas("9,20"))
        assertEquals("09:20", sformatujCzas("9:20"))
        assertEquals("09:20", sformatujCzas("9 20"))
    }

    @Test
    fun puste_zostaje_puste() {
        assertEquals("", sformatujCzas(""))
        assertEquals("", sformatujCzas("abc"))
    }

    @Test
    fun bzdurne_wartosci_sa_przycinane_do_sensownych() {
        assertEquals("23:59", sformatujCzas("9999"))
        assertEquals("23:00", sformatujCzas("99"))
    }

    @Test
    fun nadmiarowe_cyfry_sa_obcinane() {
        assertEquals("12:34", sformatujCzas("123456"))
    }
}
