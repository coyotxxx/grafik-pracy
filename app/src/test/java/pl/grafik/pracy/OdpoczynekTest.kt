package pl.grafik.pracy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.grafik.pracy.domain.DayEntry
import pl.grafik.pracy.domain.Odpoczynek
import pl.grafik.pracy.domain.Shift
import java.time.LocalDate

/**
 * Odpoczynek dobowy (art. 132 KP — 11 h) i tygodniowy (art. 133 KP — 35 h).
 * Liczby biorą się z godzin zmian: I 6–14, II 14–22, III 22–6.
 */
class OdpoczynekTest {

    private fun dni(vararg pary: Pair<LocalDate, Shift?>): Map<LocalDate, DayEntry> =
        pary.associate { (d, s) -> d to DayEntry(date = d, shift = s) }

    private fun d(dzien: Int) = LocalDate.of(2026, 9, dzien)

    @Test
    fun z_nocki_na_popoludniowke_to_kolizja() {
        // nocka z 17 na 18 kończy się 18.09 o 6:00, popołudniówka 18.09 zaczyna się o 14:00
        val kolizje = Odpoczynek.kolizjeDobowe(dni(d(17) to Shift.III, d(18) to Shift.II))
        assertEquals(1, kolizje.size)
        assertEquals(8L, kolizje[0].przerwaH)
        assertEquals(3L, kolizje[0].brakujeH)
        assertEquals(d(18), kolizje[0].date)
        assertEquals(Shift.III, kolizje[0].poprzednia)
        assertEquals(Shift.II, kolizje[0].nastepna)
    }

    @Test
    fun cztery_nocki_pod_rzad_sa_w_porzadku() {
        // nocka kończy się o 6:00, następna zaczyna o 22:00 — 16 h przerwy
        val kolizje = Odpoczynek.kolizjeDobowe(
            dni(d(15) to Shift.III, d(16) to Shift.III, d(17) to Shift.III, d(18) to Shift.III)
        )
        assertTrue(kolizje.isEmpty())
    }

    @Test
    fun ranna_po_rannej_ma_szesnascie_godzin_przerwy() {
        val kolizje = Odpoczynek.kolizjeDobowe(dni(d(7) to Shift.I, d(8) to Shift.I))
        assertTrue(kolizje.isEmpty())
    }

    @Test
    fun z_popoludniowki_na_ranna_to_kolizja() {
        // II kończy się 22:00, I następnego dnia startuje 6:00 — 8 h
        val kolizje = Odpoczynek.kolizjeDobowe(dni(d(21) to Shift.II, d(22) to Shift.I))
        assertEquals(1, kolizje.size)
        assertEquals(8L, kolizje[0].przerwaH)
    }

    @Test
    fun dzien_wolny_miedzy_zmianami_kasuje_kolizje() {
        // nocka 17, wolne 18, popołudniówka 19 — przerwa 6:00 → 14:00 następnego dnia = 32 h
        val kolizje = Odpoczynek.kolizjeDobowe(
            dni(d(17) to Shift.III, d(18) to Shift.W5, d(19) to Shift.II)
        )
        assertTrue(kolizje.isEmpty())
    }

    @Test
    fun urlop_i_zwolnienie_nie_sa_praca() {
        val kolizje = Odpoczynek.kolizjeDobowe(
            dni(d(10) to Shift.II, d(11) to Shift.URLOP, d(12) to Shift.L4, d(13) to Shift.I)
        )
        assertTrue(kolizje.isEmpty())
    }

    @Test
    fun tydzien_z_wolnym_weekendem_spelnia_norme() {
        // pn–pt na pierwszej zmianie, weekend wolny
        val tydzien = (7..11).associate { d(it) to DayEntry(date = d(it), shift = Shift.I) }
        val wynik = Odpoczynek.tygodnie(tydzien, d(7), d(13))
        assertEquals(1, wynik.size)
        // od piątku 14:00 do końca niedzieli = 58 h
        assertTrue("najdłuższa przerwa ${wynik[0].najdluzszaPrzerwaH} h", wynik[0].spelnia)
    }

    @Test
    fun tydzien_bez_dnia_wolnego_nie_spelnia_normy() {
        // siedem popołudniówek z rzędu — między nimi zawsze 16 h
        val tydzien = (7..13).associate { d(it) to DayEntry(date = d(it), shift = Shift.II) }
        val wynik = Odpoczynek.tygodnie(tydzien, d(7), d(13))
        assertEquals(1, wynik.size)
        assertEquals(16L, wynik[0].najdluzszaPrzerwaH)
        assertTrue(!wynik[0].spelnia)
    }

    @Test
    fun znacznik_w_kalendarzu_stoi_na_dniu_z_kolizja() {
        val dni = dni(d(17) to Shift.III, d(18) to Shift.II, d(19) to Shift.W5)
        assertEquals(setOf(d(18)), Odpoczynek.dniZKolizja(dni))
    }

    @Test
    fun pusty_grafik_nie_ma_kolizji_ani_tygodni_bez_odpoczynku() {
        assertTrue(Odpoczynek.kolizjeDobowe(emptyMap()).isEmpty())
        val wynik = Odpoczynek.tygodnie(emptyMap(), d(1), d(30))
        assertTrue(wynik.all { it.spelnia })
    }
}
