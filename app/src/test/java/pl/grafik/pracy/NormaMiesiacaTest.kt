package pl.grafik.pracy

import org.junit.Assert.assertEquals
import org.junit.Test
import pl.grafik.pracy.domain.Holidays

/**
 * Norma z art. 130 KP. Liczby sprawdzone wobec rubryki „Czas planowany" na paskach
 * wynagrodzenia i wobec oficjalnych wymiarów czasu pracy.
 */
class NormaMiesiacaTest {

    @Test
    fun swieto_w_sobote_obniza_wymiar() {
        // sierpień 2026: 21 dni roboczych = 168 h, Wniebowzięcie 15.08 wypada w sobotę
        assertEquals(160, Holidays.monthlyNorm(2026, 8))
    }

    @Test
    fun swieto_w_niedziele_nie_zmienia_wymiaru() {
        // maj 2026: 1.05 piątek (święto), 3.05 niedziela (święto — bez wpływu)
        // 21 dni roboczych = 168, minus 1.05 = 160
        assertEquals(160, Holidays.monthlyNorm(2026, 5))
    }

    @Test
    fun miesiac_bez_swiat_to_same_dni_robocze() {
        // lipiec 2026: 23 dni robocze, brak świąt
        assertEquals(184, Holidays.monthlyNorm(2026, 7))
        // marzec 2026: 22 dni robocze, brak świąt
        assertEquals(176, Holidays.monthlyNorm(2026, 3))
    }

    @Test
    fun styczen_2026_ma_dwa_swieta_w_dni_robocze() {
        // 22 dni robocze = 176, święta 1.01 (czwartek) i 6.01 (wtorek)
        assertEquals(160, Holidays.monthlyNorm(2026, 1))
    }

    @Test
    fun caly_rok_2026_to_2008_godzin() {
        // suma wymiarów wszystkich miesięcy, z Wigilią jako dniem wolnym
        assertEquals(2008, (1..12).sumOf { Holidays.monthlyNorm(2026, it) })
    }

    @Test
    fun grudzien_2026_ma_160_godzin_bo_wigilia_jest_wolna() {
        // 23 dni robocze = 184 h, minus Wigilia (czwartek), Boże Narodzenie (piątek)
        // i drugi dzień świąt (sobota) — każde po 8 h
        assertEquals(160, Holidays.monthlyNorm(2026, 12))
    }

    @Test
    fun wigilia_jest_swietem_dopiero_od_2025() {
        assertEquals("Wigilia", Holidays.nameOf(java.time.LocalDate.of(2026, 12, 24)))
        assertEquals("Wigilia", Holidays.nameOf(java.time.LocalDate.of(2025, 12, 24)))
        // w latach wcześniejszych Wigilia była zwykłym dniem pracy
        assertEquals(null, Holidays.nameOf(java.time.LocalDate.of(2024, 12, 24)))
    }
}
