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

    @Test fun godziny_z_zakladu_dotycza_calego_okresu() {
        val q3 = Settlement.periodOf(ym("2026-08"), kwartalny)
        assertEquals("2026-07", Settlement.key(q3))             // klucz to pierwszy miesiąc okresu
        val ustawowa = Settlement.statutoryNorm(q3)

        // wpisane, ale przełącznik wyłączony — nie rusza niczego
        val wylaczona = SettlementCfg(useCompanyNorm = false, companyNorms = mapOf("2026-07" to 540))
        assertEquals(ustawowa, Settlement.norm(q3, wylaczona))

        // włączone i wpisane
        val wlaczona = wylaczona.copy(useCompanyNorm = true)
        assertEquals(540, Settlement.norm(q3, wlaczona))

        // inny kwartał bez wpisu — zostaje ustawowa
        val q1 = Settlement.periodOf(ym("2026-02"), kwartalny)
        assertEquals(Settlement.statutoryNorm(q1), Settlement.norm(q1, wlaczona))

        // zero nie może wyzerować normy
        val zero = SettlementCfg(useCompanyNorm = true, companyNorms = mapOf("2026-07" to 0))
        assertEquals(ustawowa, Settlement.norm(q3, zero))
    }

    @Test fun rok_dzieli_sie_na_cztery_kwartaly_do_wpisania() {
        val okresy = Settlement.periodsOfYear(2026, kwartalny)
        assertEquals(4, okresy.size)
        assertEquals(listOf(1, 4, 7, 10), okresy.map { it.from.monthValue })
        assertEquals(listOf(3, 6, 9, 12), okresy.map { it.to.monthValue })
        // suma norm kwartałów musi dać cały rok
        assertEquals(
            (1..12).sumOf { Holidays.monthlyNorm(2026, it) },
            okresy.sumOf { Settlement.statutoryNorm(it) }
        )
    }

    @Test fun kazda_dlugosc_okresu_pokrywa_rok_bez_dziur() {
        Settlement.DLUGOSCI.forEach { dl ->
            val okresy = Settlement.periodsOfYear(2026, SettlementCfg(months = dl))
            assertEquals("długość $dl", 12 / dl, okresy.size)
            assertEquals("długość $dl", 1, okresy.first().from.monthValue)
            assertEquals("długość $dl", 12, okresy.last().to.monthValue)
        }
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
