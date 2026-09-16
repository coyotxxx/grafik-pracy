package pl.grafik.pracy

import org.junit.Assert.*
import org.junit.Test
import pl.grafik.pracy.domain.CycleConfig
import pl.grafik.pracy.domain.CycleGenerator
import java.time.LocalDate
import java.time.YearMonth

/** Cykl ma wypełniać kalendarz tylko w okresie, który wybrał użytkownik. */
class CycleRangeTest {

    private val cfg = CycleConfig(
        anchorDate = LocalDate.of(2026, 1, 1),
        generate = true,
        genFrom = YearMonth.of(2026, 9),
        genTo = YearMonth.of(2027, 8)
    )

    @Test
    fun wylaczony_cykl_nie_wypelnia_niczego() {
        val off = cfg.copy(generate = false)
        assertFalse(off.covers(LocalDate.of(2026, 10, 5)))
        assertTrue(CycleGenerator.range(off, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)).isEmpty())
    }

    @Test
    fun dni_przed_poczatkiem_zakresu_zostaja_puste() {
        assertFalse(cfg.covers(LocalDate.of(2026, 8, 31)))
        assertTrue(cfg.covers(LocalDate.of(2026, 9, 1)))
    }

    @Test
    fun dni_po_koncu_zakresu_zostaja_puste() {
        assertTrue(cfg.covers(LocalDate.of(2027, 8, 31)))
        assertFalse(cfg.covers(LocalDate.of(2027, 9, 1)))
    }

    @Test
    fun bez_konca_wypelnia_wszystko_od_poczatku() {
        val bezKonca = cfg.copy(genTo = null)
        assertTrue(bezKonca.covers(LocalDate.of(2030, 12, 31)))
        assertFalse(bezKonca.covers(LocalDate.of(2026, 8, 1)))
    }

    @Test
    fun jeden_miesiac_to_dokladnie_ten_miesiac() {
        val jeden = cfg.copy(genFrom = YearMonth.of(2026, 9), genTo = YearMonth.of(2026, 9))
        assertEquals(30, CycleGenerator.range(jeden, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)).size)
        assertTrue(CycleGenerator.range(jeden, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)).isEmpty())
    }

    @Test
    fun siatka_na_przelomie_miesiecy_wypelnia_tylko_objete_dni() {
        // siatka września zahacza o sierpień (31.08) i październik
        val wrzesien = CycleGenerator.range(cfg, LocalDate.of(2026, 8, 31), LocalDate.of(2026, 10, 4))
        assertFalse("31 sierpnia jest poza zakresem", wrzesien.containsKey(LocalDate.of(2026, 8, 31)))
        assertTrue("1 października jest w zakresie", wrzesien.containsKey(LocalDate.of(2026, 10, 1)))
    }
}
