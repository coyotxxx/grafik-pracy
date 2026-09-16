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
        // Pełne tygodnie, nie zaokrąglone: I kwartał ma 90 dni = 12 tygodni, nie 13.
        val limity = Settlement.periodsOfYear(2026, kwartalny).map { Settlement.otLimit(it) }
        assertEquals(listOf(96, 104, 104, 104), limity)

        val q1 = Settlement.periodOf(ym("2026-02"), kwartalny)
        assertEquals(90L, q1.days)
        assertEquals(12, q1.weeks)
    }

    @Test fun ten_sam_zakres_limitow_w_kazdym_roku() {
        listOf(2025, 2026, 2027, 2028).forEach { rok ->
            val limity = Settlement.periodsOfYear(rok, kwartalny).map { Settlement.otLimit(it) }
            assertTrue("rok $rok: $limity", limity.all { it in 96..104 })
        }
    }

    @Test fun limit_roczny_zakladowy_ma_pierwszenstwo_przed_ustawowym() {
        assertEquals(150, Settlement.otLimitYear(SettlementCfg()))
        assertEquals(150, Settlement.otLimitYear(SettlementCfg(otLimitYearCompany = 0)))
        assertEquals(416, Settlement.otLimitYear(SettlementCfg(otLimitYearCompany = 416)))
        // zakład może też ustawić NIŻSZY limit niż ustawowy
        assertEquals(100, Settlement.otLimitYear(SettlementCfg(otLimitYearCompany = 100)))
    }

    @Test fun limit_roczny_potrafi_zablokowac_sufit_techniczny() {
        // 130 h nadgodzin w roku przy limicie 150 — w okresie zostaje tylko 20 h,
        // mimo że technicznie wolno 104 h.
        val st = PeriodStats(ot = 10, otLimit = 104, otRok = 130, otLimitRok = 150)
        assertEquals(20, st.zostaloNadgodzinRok)
        assertEquals(30, st.otLimitEff)
        assertEquals(20, st.zostaloNadgodzin)
        assertTrue(st.blokujeRoczny)
    }

    @Test fun gdy_rok_nie_blokuje_obowiazuje_sufit_techniczny() {
        val st = PeriodStats(ot = 10, otLimit = 104, otRok = 10, otLimitRok = 150)
        assertEquals(104, st.otLimitEff)
        assertEquals(94, st.zostaloNadgodzin)
        assertFalse(st.blokujeRoczny)
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
