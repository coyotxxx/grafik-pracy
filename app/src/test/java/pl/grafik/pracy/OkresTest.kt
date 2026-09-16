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

    @Test fun norma_ustawowa_okresu_to_suma_miesiecy() {
        val p = Settlement.periodOf(ym("2026-08"), kwartalny)
        val recznie = listOf(7, 8, 9).sumOf { Holidays.monthlyNorm(2026, it) }
        assertEquals(recznie, Settlement.statutoryNorm(p))
    }

    @Test fun rok_dzieli_sie_na_cztery_kwartaly() {
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

    @Test fun limit_zakladowy_podawany_jest_na_kazdy_kwartal_osobno() {
        val cfg = SettlementCfg(
            months = 3,
            otLimitPeriods = mapOf("2026-01" to 80, "2026-07" to 90)
        )
        val okresy = Settlement.periodsOfYear(2026, cfg)
        assertEquals(80, Settlement.otLimitCompany(okresy[0], cfg))     // I kwartał
        assertNull(Settlement.otLimitCompany(okresy[1], cfg))           // II — zakład nic nie podał
        assertEquals(90, Settlement.otLimitCompany(okresy[2], cfg))     // III kwartał
        assertNull(Settlement.otLimitCompany(okresy[3], cfg))

        // zero to brak wpisu, nie limit zerowy
        val zero = SettlementCfg(otLimitPeriods = mapOf("2026-01" to 0))
        assertNull(Settlement.otLimitCompany(okresy[0], zero))
    }

    @Test fun limit_zakladu_zastepuje_ustawowy() {
        // Zakład Macieja zwykle obniża ustawowy o kilka godzin.
        val nizszy = PeriodStats(ot = 10, otLimit = 104, otLimitZakl = 94)
        assertEquals(94, nizszy.limit)
        assertEquals(84, nizszy.zostalo)
        assertTrue(nizszy.zakladowyObowiazuje)
        assertFalse(nizszy.wyczerpany)

        // Zastępuje, a nie ogranicza — wpisana liczba obowiązuje także wtedy, gdy jest wyższa.
        assertEquals(120, PeriodStats(otLimit = 96, otLimitZakl = 120).limit)

        // Bez wpisu zostaje ustawowy.
        val bezWpisu = PeriodStats(otLimit = 96)
        assertEquals(96, bezWpisu.limit)
        assertFalse(bezWpisu.zakladowyObowiazuje)
    }

    @Test fun wyczerpany_limit_nie_schodzi_ponizej_zera() {
        val st = PeriodStats(ot = 120, otLimit = 104, otLimitZakl = 94)
        assertTrue(st.wyczerpany)
        assertEquals(0, st.zostalo)
    }

    @Test fun limit_roczny_to_suma_kwartalow() {
        // Bez wpisów zakładu: 96 + 104 + 104 + 104
        assertEquals(408, Settlement.yearLimit(2026, kwartalny))

        // Zakład obniża II i III kwartał o 10 h
        val zZakladem = kwartalny.copy(otLimitPeriods = mapOf("2026-04" to 94, "2026-07" to 94))
        assertEquals(96 + 94 + 94 + 104, Settlement.yearLimit(2026, zZakladem))
    }

    @Test fun suma_kwartalow_miesci_sie_w_rocznej_granicy_z_art_131() {
        listOf(2025, 2026, 2027, 2028).forEach { rok ->
            val sufit = Settlement.yearCeiling(rok)
            assertEquals("rok $rok", 416, sufit)
            assertTrue(
                "rok $rok: ${Settlement.yearLimit(rok, kwartalny)} > $sufit",
                Settlement.yearLimit(rok, kwartalny) <= sufit
            )
        }
    }

    @Test fun zawyzone_limity_zakladu_przekraczaja_granice_roczna() {
        val zaDuzo = kwartalny.copy(
            otLimitPeriods = mapOf("2026-01" to 120, "2026-04" to 120, "2026-07" to 120, "2026-10" to 120)
        )
        assertEquals(480, Settlement.yearLimit(2026, zaDuzo))
        assertTrue(Settlement.yearLimit(2026, zaDuzo) > Settlement.yearCeiling(2026))
    }

    @Test fun limit_okresu_bierze_zakladowy_gdy_jest() {
        val p = Settlement.periodOf(ym("2026-09"), kwartalny)
        assertEquals(104, Settlement.periodLimit(p, kwartalny))
        assertEquals(94, Settlement.periodLimit(p, kwartalny.copy(otLimitPeriods = mapOf("2026-07" to 94))))
    }

    @Test fun okres_zawiera_swoje_dni() {
        val p = Settlement.periodOf(ym("2026-09"), kwartalny)
        assertTrue(LocalDate.parse("2026-07-01") in p)
        assertTrue(LocalDate.parse("2026-09-30") in p)
        assertFalse(LocalDate.parse("2026-06-30") in p)
        assertFalse(LocalDate.parse("2026-10-01") in p)
    }

}
