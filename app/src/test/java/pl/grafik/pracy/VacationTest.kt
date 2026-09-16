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
        val s = stan(VacationCfg(wymiar = 26, zalegly = 0), wrzesien)
        assertEquals(26, s.urlopBaza)
        assertEquals(4, s.urlopZuzyty)
        assertEquals(22, s.urlopPozostalo)
    }

    @Test
    fun zalegly_doliczany_do_wymiaru() {
        val s = stan(VacationCfg(wymiar = 20, zalegly = 6), wrzesien)
        assertEquals(26, s.urlopBaza)
        assertEquals(22, s.urlopPozostalo)
    }

    @Test
    fun punkt_odniesienia_liczy_tylko_dni_PO_nim() {
        val s = stan(
            VacationCfg(wymiar = 26, stanData = LocalDate.of(2026, 8, 31), stanDni = 12),
            wrzesien
        )
        assertEquals("baza to stan z zakładu", 12, s.urlopBaza)
        assertEquals("marzec i lipiec już się nie liczą", 2, s.urlopZuzyty)
        assertEquals(10, s.urlopPozostalo)
    }

    @Test
    fun dzien_punktu_odniesienia_nie_jest_odliczany() {
        val s = stan(
            VacationCfg(stanData = LocalDate.of(2026, 9, 7), stanDni = 10),
            wrzesien
        )
        assertEquals("7 września to dzień stanu, liczy się tylko 8", 1, s.urlopZuzyty)
        assertEquals(9, s.urlopPozostalo)
    }

    @Test
    fun punkt_odniesienia_z_innego_roku_jest_ignorowany() {
        val s = stan(
            VacationCfg(wymiar = 26, stanData = LocalDate.of(2025, 11, 1), stanDni = 3),
            wrzesien
        )
        assertEquals("nowy rok = nowy wymiar", 26, s.urlopBaza)
        assertEquals(4, s.urlopZuzyty)
    }

    @Test
    fun lista_miesiaca_to_tylko_dni_z_wyswietlanego_miesiaca() {
        val s = stan(VacationCfg(), wrzesien)
        assertEquals(listOf(LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 8)), s.urlopMiesiaca)
    }

    @Test
    fun przekroczony_wymiar_daje_ujemny_bilans() {
        val duzo = (1..30).map { LocalDate.of(2026, 1, 1).plusDays(it.toLong()) }
        val s = stan(VacationCfg(wymiar = 26), duzo)
        assertEquals(-4, s.urlopPozostalo)
    }
}
