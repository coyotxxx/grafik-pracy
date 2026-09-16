package pl.grafik.pracy

import org.junit.Assert.*
import org.junit.Test
import pl.grafik.pracy.domain.*
import java.time.LocalDate
import java.time.YearMonth

/** Okres rozliczeniowy: kwartały kalendarzowe, dwie normy i limity nadgodzin z KP. */
class OkresTest {

    private val kwartalny = SettlementCfg(months = 3)
    private fun ym(s: String) = YearMonth.parse(s)

    @Test fun kwartaly_sa_kalendarzowe() {
        assertEquals(Period(ym("2026-07"), ym("2026-09")), Settlement.periodOf(ym("2026-09"), kwartalny))
        assertEquals(Period(ym("2026-01"), ym("2026-03")), Settlement.periodOf(ym("2026-01"), kwartalny))
        assertEquals(Period(ym("2026-10"), ym("2026-12")), Settlement.periodOf(ym("2026-12"), kwartalny))
    }

    @Test fun okres_nigdy_nie_przechodzi_przez_granice_roku() {
        Settlement.DLUGOSCI.forEach { dl ->
            (1..12).forEach { m ->
                val p = Settlement.periodOf(YearMonth.of(2026, m), SettlementCfg(months = dl))
                assertEquals("długość $dl, miesiąc $m", 2026, p.from.year)
                assertEquals("długość $dl, miesiąc $m", 2026, p.to.year)
                assertEquals("długość $dl, miesiąc $m", dl, p.months.size)
                assertTrue("miesiąc $m poza okresem", YearMonth.of(2026, m) in p.months)
            }
        }
    }

    @Test fun norma_okresu_to_suma_miesiecy() {
        val p = Settlement.periodOf(ym("2026-08"), kwartalny)
        val recznie = listOf(7, 8, 9).sumOf { Holidays.monthlyNorm(2026, it) }
        assertEquals(recznie, Settlement.statutoryNorm(p))
    }

    @Test fun norma_zakladowa_wchodzi_tylko_gdy_wlaczona_i_wpisana() {
        val wrzesien = ym("2026-09")
        val ustawowa = Holidays.monthlyNorm(2026, 9)

        // wpisana, ale przełącznik wyłączony — nie rusza niczego
        val wylaczona = SettlementCfg(useCompanyNorm = false, companyNorms = mapOf("2026-09" to 184))
        assertEquals(ustawowa, Settlement.normOfMonth(wrzesien, wylaczona))

        // włączona i wpisana
        val wlaczona = wylaczona.copy(useCompanyNorm = true)
        assertEquals(184, Settlement.normOfMonth(wrzesien, wlaczona))

        // włączona, ale bez wpisu na ten miesiąc — zostaje ustawowa
        assertEquals(Holidays.monthlyNorm(2026, 8), Settlement.normOfMonth(ym("2026-08"), wlaczona))

        // zero nie może wyzerować normy
        val zero = SettlementCfg(useCompanyNorm = true, companyNorms = mapOf("2026-09" to 0))
        assertEquals(ustawowa, Settlement.normOfMonth(wrzesien, zero))
    }

    @Test fun limit_nadgodzin_w_kwartale_wynika_z_art_131() {
        val p = Settlement.periodOf(ym("2026-09"), kwartalny)
        assertEquals(92, java.time.temporal.ChronoUnit.DAYS.between(p.start, p.end.plusDays(1)))
        assertEquals(13, p.weeks)                       // 92 dni to 13 tygodni
        assertEquals(8 * 13, Settlement.otLimit(p))     // 48 h - 40 h = 8 h nadgodzin na tydzień
    }

    @Test fun okres_zawiera_swoje_dni() {
        val p = Settlement.periodOf(ym("2026-09"), kwartalny)
        assertTrue(LocalDate.parse("2026-07-01") in p)
        assertTrue(LocalDate.parse("2026-09-30") in p)
        assertFalse(LocalDate.parse("2026-06-30") in p)
        assertFalse(LocalDate.parse("2026-10-01") in p)
    }

    @Test fun rozliczenie_pokazuje_obie_normy_gdy_sie_roznia() {
        val p = Settlement.periodOf(ym("2026-09"), kwartalny)
        val ust = Settlement.statutoryNorm(p)
        assertFalse(PeriodStats(period = p, norm = ust, normUstawowa = ust).normaInna)
        assertTrue(PeriodStats(period = p, norm = ust + 8, normUstawowa = ust).normaInna)
    }

    @Test fun zostalo_nadgodzin_nie_schodzi_ponizej_zera() {
        val st = PeriodStats(ot = 120, otLimit = 104, otRok = 200, otLimitRok = 150)
        assertEquals(0, st.zostaloNadgodzin)
        assertEquals(0, st.zostaloNadgodzinRok)
    }
}
