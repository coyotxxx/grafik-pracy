package pl.grafik.pracy

import org.junit.Assert.assertEquals
import org.junit.Test
import pl.grafik.pracy.domain.VacationCfg
import pl.grafik.pracy.ui.UiState
import java.time.LocalDate
import java.time.YearMonth

/** Bilans urlopu — z wymiaru albo od stanu podanego przez kadrową. */
class VacationTest {

    private fun stan(cfg: VacationCfg, dni: List<LocalDate>, ym: YearMonth = YearMonth.of(2026, 9)) =
        UiState(ym = ym, urlop = cfg, urlopRok = dni)

    private val wrzesien = listOf(
        LocalDate.of(2026, 3, 2),
        LocalDate.of(2026, 7, 20),
        LocalDate.of(2026, 9, 7),
        LocalDate.of(2026, 9, 8)
    )

    @Test
    fun bez_punktu_odniesienia_liczymy_od_wymiaru() {
        val b = stan(VacationCfg(wymiar = 26, zalegly = 0), wrzesien).urlopBilans
        assertEquals(26, b.baza)
        assertEquals(4, b.zuzyte)
        assertEquals(22, b.zostalo)
    }

    @Test
    fun zalegly_jest_osobna_pula_i_zuzywa_sie_PIERWSZY() {
        val b = stan(VacationCfg(wymiar = 26, zalegly = 5), wrzesien).urlopBilans
        assertEquals("5 zaległego + 26 bieżącego", 31, b.baza)
        assertEquals("cztery dni poszły z zaległego", 4, b.zZaleglego)
        assertEquals(0, b.zBiezacego)
        assertEquals(1, b.zostaloZaleglego)
        assertEquals(26, b.zostaloBiezacego)
        assertEquals(27, b.zostalo)
    }

    @Test
    fun po_wyczerpaniu_zaleglego_schodzimy_na_biezacy() {
        val b = stan(VacationCfg(wymiar = 26, zalegly = 2), wrzesien).urlopBilans
        assertEquals(2, b.zZaleglego)
        assertEquals(2, b.zBiezacego)
        assertEquals(0, b.zostaloZaleglego)
        assertEquals(24, b.zostaloBiezacego)
    }

    @Test
    fun termin_zaleglego_to_30_wrzesnia() {
        val b = stan(VacationCfg(zalegly = 5), wrzesien).urlopBilans
        assertEquals(LocalDate.of(2026, 9, 30), b.termin)
        assertEquals(29, b.dniDoTerminu(LocalDate.of(2026, 9, 1)))
        assertEquals("po terminie wartość jest ujemna", -1, b.dniDoTerminu(LocalDate.of(2026, 10, 1)).toInt())
    }

    @Test
    fun punkt_odniesienia_liczy_tylko_dni_PO_nim() {
        val b = stan(
            VacationCfg(wymiar = 26, stanData = LocalDate.of(2026, 8, 31), stanBiezacy = 12, stanZalegly = 0),
            wrzesien
        ).urlopBilans
        assertEquals("baza to stan z zakładu", 12, b.baza)
        assertEquals("marzec i lipiec już się nie liczą", 2, b.zuzyte)
        assertEquals(10, b.zostalo)
    }

    @Test
    fun punkt_odniesienia_tez_zdejmuje_najpierw_zalegly() {
        val b = stan(
            VacationCfg(stanData = LocalDate.of(2026, 8, 31), stanBiezacy = 12, stanZalegly = 1),
            wrzesien
        ).urlopBilans
        assertEquals(1, b.zZaleglego)
        assertEquals(1, b.zBiezacego)
        assertEquals(0, b.zostaloZaleglego)
        assertEquals(11, b.zostaloBiezacego)
    }

    @Test
    fun dzien_punktu_odniesienia_nie_jest_odliczany() {
        val b = stan(
            VacationCfg(stanData = LocalDate.of(2026, 9, 7), stanBiezacy = 10),
            wrzesien
        ).urlopBilans
        assertEquals("7 września to dzień stanu, liczy się tylko 8", 1, b.zuzyte)
        assertEquals(9, b.zostalo)
    }

    @Test
    fun punkt_odniesienia_z_innego_roku_jest_ignorowany() {
        val b = stan(
            VacationCfg(wymiar = 26, stanData = LocalDate.of(2025, 11, 1), stanBiezacy = 3),
            wrzesien
        ).urlopBilans
        assertEquals("nowy rok = nowy wymiar", 26, b.baza)
        assertEquals(4, b.zuzyte)
    }

    @Test
    fun lista_miesiaca_to_tylko_dni_z_wyswietlanego_miesiaca() {
        val s = stan(VacationCfg(), wrzesien)
        assertEquals(listOf(LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 8)), s.urlopMiesiaca)
    }

    @Test
    fun przekroczony_wymiar_daje_ujemny_bilans() {
        val duzo = (1..30).map { LocalDate.of(2026, 1, 1).plusDays(it.toLong()) }
        assertEquals(-4, stan(VacationCfg(wymiar = 26), duzo).urlopBilans.zostalo)
    }

    /** Scenariusz Macieja: w styczniu nowy wymiar 26 i jeszcze 5 dni z poprzedniego roku. */
    @Test
    fun styczen_pokazuje_nowy_wymiar_obok_zaleglego() {
        val s = stan(VacationCfg(wymiar = 26, zalegly = 5), emptyList(), YearMonth.of(2026, 1))
        val b = s.urlopBilans
        assertEquals(5, b.zostaloZaleglego)
        assertEquals(26, b.zostaloBiezacego)
        assertEquals(31, b.zostalo)
        assertEquals(LocalDate.of(2026, 9, 30), b.termin)
    }

    @Test
    fun podpowiedz_ile_przeszlo_z_poprzedniego_roku() {
        val poprzedni = (1..21).map { LocalDate.of(2025, 2, 1).plusDays(it.toLong()) }
        val s = UiState(
            ym = YearMonth.of(2026, 1),
            urlop = VacationCfg(wymiar = 26),
            urlopPoprzedniRok = poprzedni
        )
        assertEquals("26 − 21 wykorzystanych", 5, s.urlopSugestiaZaleglego)
    }
}
