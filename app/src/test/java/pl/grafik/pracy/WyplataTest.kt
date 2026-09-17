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
import java.time.YearMonth

/**
 * Szacunek wypłaty brutto. Liczby sprawdzone na prawdziwych odcinkach: wynagrodzenie
 * jest miesięczne, stawka za godzinę to zasadnicza ÷ norma miesiąca, nadgodziny idą
 * po 1,5 albo 2 stawki, a dodatek za nocki to kwota z regulaminu.
 */
class WyplataTest {

    private val stawki = StawkiCfg(zasadnicza = 7950.0, dodatekNocny = 10.01)

    private fun dzien(nr: Int, s: Shift?, ot: Int = 0, rate: OtRate = OtRate.P100, miesiac: Int = 8) =
        DayEntry(date = LocalDate.of(2026, miesiac, nr), shift = s, otHours = ot, otRate = rate)

    @Test
    fun stawka_godzinowa_wychodzi_z_zasadniczej_i_planu() {
        // z odcinków: sierpień 160 h planu, lipiec 184 h, czerwiec 176 h
        assertEquals(49.69, KalkulatorWyplaty.stawkaGodzinowa(7950.0, 160), 0.01)
        assertEquals(43.21, KalkulatorWyplaty.stawkaGodzinowa(7950.0, 184), 0.01)
        assertEquals(45.17, KalkulatorWyplaty.stawkaGodzinowa(7950.0, 176), 0.01)
    }

    @Test
    fun czas_planowany_to_zmiany_i_urlop_po_osiem_godzin() {
        val dni = listOf(
            dzien(1, Shift.I), dzien(2, Shift.III), dzien(3, Shift.URLOP),
            dzien(4, Shift.W5), dzien(5, null), dzien(6, Shift.L4)
        )
        // trzy dni liczone (zmiana, zmiana, urlop), reszta nie
        assertEquals(24, KalkulatorWyplaty.godzinyPlanowane(dni))
    }

    @Test
    fun sierpien_2026_zgadza_sie_z_odcinkiem() {
        // z odcinka: 56 h nocek, nadgodziny 12 h po 50 % i 8 h po 100 %
        // 15 dni pracy i 5 dni urlopu = 160 h planu, tyle co na odcinku
        val dni = buildList {
            repeat(7) { add(dzien(it + 1, Shift.III)) }          // 7 × 8 h = 56 h nocek
            add(dzien(10, Shift.I, ot = 12, rate = OtRate.P50))
            add(dzien(11, Shift.II, ot = 8, rate = OtRate.P100))
            repeat(6) { add(dzien(12 + it, Shift.II)) }
            repeat(5) { add(dzien(20 + it, Shift.URLOP)) }
        }
        val w = KalkulatorWyplaty.policz(dni, stawki, YearMonth.of(2026, 8))

        assertEquals(160, w.normaMiesiaca)
        assertEquals(7950.0, w.zasadnicza.kwota, 0.01)
        assertEquals(56, w.nocny.godziny)
        assertEquals(560.56, w.nocny.kwota, 0.01)               // 56 × 10,01
        assertEquals(894.38, w.nadgodziny50.kwota, 0.05)        // 12 × 49,69 × 1,5
        assertEquals(795.0, w.nadgodziny100.kwota, 0.05)        // 8 × 49,69 × 2
        // odcinek: 993,80 + 298,20 + 397,52 (nadgodziny) + 560,56 (nocki) + 7950
        assertEquals(10200.0, w.razem, 0.2)
    }

    @Test
    fun krotszy_plan_daje_wyzsza_stawke_za_godzine() {
        val krotki = (1..20).map { dzien(it, Shift.I) }                   // 160 h planu
        val dlugi = (1..23).map { dzien(it, Shift.I) }                    // 184 h planu
        val a = KalkulatorWyplaty.policz(krotki, stawki, YearMonth.of(2026, 8))
        val b = KalkulatorWyplaty.policz(dlugi, stawki, YearMonth.of(2026, 7))
        assertTrue(a.stawkaGodzinowa > b.stawkaGodzinowa)
        assertEquals(49.69, a.stawkaGodzinowa, 0.01)
        assertEquals(43.21, b.stawkaGodzinowa, 0.01)
    }

    @Test
    fun nocki_licza_sie_po_kwocie_z_regulaminu() {
        val dni = (1..10).map { dzien(it, Shift.III) }
        val w = KalkulatorWyplaty.policz(dni, stawki, YearMonth.of(2026, 8))
        assertEquals(80, w.nocny.godziny)
        assertEquals(800.80, w.nocny.kwota, 0.01)               // 80 × 10,01
    }

    @Test
    fun bez_zaplanowanych_dni_nie_ma_stawki() {
        val w = KalkulatorWyplaty.policz(emptyList(), stawki, YearMonth.of(2026, 8))
        assertEquals(0.0, w.stawkaGodzinowa, 0.01)
    }

    @Test
    fun ranna_i_popoludniowka_nie_maja_dodatku_za_nocki() {
        val dni = listOf(dzien(1, Shift.I), dzien(2, Shift.II))
        val w = KalkulatorWyplaty.policz(dni, stawki, YearMonth.of(2026, 8))
        assertEquals(0, w.nocny.godziny)
        assertEquals(0.0, w.nocny.kwota, 0.01)
    }

    @Test
    fun nadgodziny_sto_procent_sa_dwa_razy_drozsze_niz_normalna_godzina() {
        val dni = (1..20).map { dzien(it, Shift.I) } +
            listOf(dzien(21, Shift.I, ot = 4, rate = OtRate.P100))
        val w = KalkulatorWyplaty.policz(dni, stawki, YearMonth.of(2026, 8))
        assertEquals(4, w.nadgodziny100.godziny)
        // 21 dni planu = 168 h, stawka 47,32
        assertEquals(4 * (7950.0 / 168) * 2, w.nadgodziny100.kwota, 0.01)
    }

    @Test
    fun urlop_nie_jest_osobnym_skladnikiem_bo_siedzi_w_zasadniczej() {
        val dni = listOf(dzien(1, Shift.URLOP), dzien(2, Shift.URLOP))
        val w = KalkulatorWyplaty.policz(dni, stawki, YearMonth.of(2026, 8))
        assertEquals(7950.0, w.razem, 0.01)
        assertEquals(1, w.skladniki.size)
    }

    @Test
    fun bez_zasadniczej_nie_ma_czego_liczyc() {
        val dni = (1..10).map { dzien(it, Shift.III) }
        val w = KalkulatorWyplaty.policz(dni, StawkiCfg(), YearMonth.of(2026, 8))
        assertEquals(0.0, w.razem, 0.01)
        assertTrue(!StawkiCfg().ustawiona)
    }

    @Test
    fun kwoty_pisza_sie_po_polsku() {
        assertEquals("5 667,00", KalkulatorWyplaty.zlote(5667.0))
        assertEquals("10 200,02", KalkulatorWyplaty.zlote(10200.02))
        assertEquals("49,69", KalkulatorWyplaty.zlote(49.6875))
        assertEquals("0,00", KalkulatorWyplaty.zlote(0.0))
    }
}
