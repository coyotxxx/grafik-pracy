package pl.grafik.pracy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.grafik.pracy.domain.DayEntry
import pl.grafik.pracy.domain.KalkulatorWyplaty
import pl.grafik.pracy.domain.OtRate
import pl.grafik.pracy.domain.Shift
import pl.grafik.pracy.domain.StawkiCfg
import java.time.LocalDate

/**
 * Szacunek wypłaty brutto: podstawa, urlop, nadgodziny 50/100 %, dodatek nocny, premia.
 * Liczby dobrane tak, żeby dało się je sprawdzić w pamięci.
 */
class WyplataTest {

    private val stawki = StawkiCfg(stawka = 30.0, nocnyZMinimalnej = true, stawkaMinimalna = 30.0)

    private fun dzien(nr: Int, s: Shift?, ot: Int = 0, rate: OtRate = OtRate.P100) =
        DayEntry(date = LocalDate.of(2026, 9, nr), shift = s, otHours = ot, otRate = rate)

    @Test
    fun dwadziescia_dniowek_to_sto_szescdziesiat_godzin() {
        val dni = (1..20).map { dzien(it, Shift.I) }
        val w = KalkulatorWyplaty.policz(dni, stawki)
        assertEquals(160, w.podstawa.godziny)
        assertEquals(4800.0, w.podstawa.kwota, 0.01)      // 160 × 30
        assertEquals(4800.0, w.razem, 0.01)
    }

    @Test
    fun nocka_dodaje_dwadziescia_procent_za_kazda_godzine() {
        val dni = (1..10).map { dzien(it, Shift.III) }
        val w = KalkulatorWyplaty.policz(dni, stawki)
        assertEquals(80, w.podstawa.godziny)
        assertEquals(80, w.nocny.godziny)                 // cała nocka w porze nocnej
        assertEquals(6.0, w.nocny.stawka, 0.01)           // 20 % z 30 zł
        assertEquals(480.0, w.nocny.kwota, 0.01)          // 80 × 6
        assertEquals(2400.0 + 480.0, w.razem, 0.01)
    }

    @Test
    fun popoludniowka_i_ranna_nie_maja_dodatku_nocnego() {
        val dni = listOf(dzien(1, Shift.I), dzien(2, Shift.II))
        val w = KalkulatorWyplaty.policz(dni, stawki)
        assertEquals(0, w.nocny.godziny)
        assertEquals(0.0, w.nocny.kwota, 0.01)
    }

    @Test
    fun nadgodziny_sto_procent_licza_sie_podwojnie() {
        val dni = listOf(dzien(1, Shift.I, ot = 4, rate = OtRate.P100))
        val w = KalkulatorWyplaty.policz(dni, stawki)
        assertEquals(4, w.nadgodziny100.godziny)
        assertEquals(240.0, w.nadgodziny100.kwota, 0.01)  // 4 × 30 × 2
    }

    @Test
    fun nadgodziny_piecdziesiat_procent_to_poltora_stawki() {
        val dni = listOf(dzien(1, Shift.II, ot = 4, rate = OtRate.P50))
        val w = KalkulatorWyplaty.policz(dni, stawki)
        assertEquals(180.0, w.nadgodziny50.kwota, 0.01)   // 4 × 30 × 1,5
    }

    @Test
    fun urlop_jest_osobnym_skladnikiem() {
        val dni = listOf(dzien(1, Shift.URLOP), dzien(2, Shift.URLOP))
        val w = KalkulatorWyplaty.policz(dni, stawki)
        assertEquals(16, w.urlop.godziny)
        assertEquals(480.0, w.urlop.kwota, 0.01)
        assertEquals(0, w.podstawa.godziny)               // urlop to nie praca
    }

    @Test
    fun premia_liczy_sie_od_podstawy_a_nie_od_calosci() {
        val dni = (1..20).map { dzien(it, Shift.III) } + dzien(21, Shift.I, ot = 8)
        val zPremia = KalkulatorWyplaty.policz(dni, stawki.copy(premiaProc = 10))
        val bezPremii = KalkulatorWyplaty.policz(dni, stawki)
        // 21 dni pracy × 8 h × 30 zł = 5040; premia 10 % = 504
        assertEquals(504.0, zPremia.premia.kwota, 0.01)
        assertEquals(bezPremii.razem + 504.0, zPremia.razem, 0.01)
    }

    @Test
    fun dodatek_nocny_z_wlasnej_stawki_jest_wyzszy() {
        val dni = (1..10).map { dzien(it, Shift.III) }
        val zMinimalnej = KalkulatorWyplaty.policz(dni, stawki.copy(stawka = 45.0))
        val zWlasnej = KalkulatorWyplaty.policz(
            dni, stawki.copy(stawka = 45.0, nocnyZMinimalnej = false)
        )
        assertEquals(6.0, zMinimalnej.nocny.stawka, 0.01)   // 20 % z 30 zł minimalnej
        assertEquals(9.0, zWlasnej.nocny.stawka, 0.01)      // 20 % z 45 zł własnej
        assertTrue(zWlasnej.razem > zMinimalnej.razem)
    }

    @Test
    fun dni_wolne_nie_daja_nic() {
        val dni = listOf(dzien(1, Shift.W5), dzien(2, Shift.WS), dzien(3, Shift.L4), dzien(4, null))
        val w = KalkulatorWyplaty.policz(dni, stawki)
        assertEquals(0.0, w.razem, 0.01)
        assertTrue(w.skladniki.isEmpty())
    }

    @Test
    fun kwoty_pisza_sie_po_polsku() {
        assertEquals("5 667,00", KalkulatorWyplaty.zlote(5667.0))
        assertEquals("1 234,56", KalkulatorWyplaty.zlote(1234.56))
        assertEquals("30,50", KalkulatorWyplaty.zlote(30.5))
        assertEquals("0,00", KalkulatorWyplaty.zlote(0.0))
    }
}
