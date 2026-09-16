package pl.grafik.pracy

import org.junit.Assert.assertEquals
import org.junit.Test
import pl.grafik.pracy.domain.CycleConfig
import pl.grafik.pracy.domain.CycleGenerator
import pl.grafik.pracy.domain.CyclePattern
import pl.grafik.pracy.domain.Shift
import java.time.LocalDate

/** Maciej chodzi cykl w drugą stronę niż zapisany wzorzec — musi dać się odwrócić. */
class RotationTest {

    private val start = LocalDate.of(2026, 1, 1)

    private fun cfg(p: CyclePattern, rev: Boolean) =
        CycleConfig(pattern = p, anchorDate = start, anchorIndex = 0, generate = true, reverse = rev)

    @Test
    fun cykl_16_dniowy_ma_normalnie_kolejnosc_I_III_II() {
        assertEquals("I → III → II", CycleGenerator.rotationLabel(cfg(CyclePattern.B4_16D, false)))
    }

    @Test
    fun odwrocenie_daje_kolejnosc_przeciwna() {
        assertEquals("II → III → I", CycleGenerator.rotationLabel(cfg(CyclePattern.B4_16D, true)))
    }

    @Test
    fun trzyzmianowy_ciagly_odwrocony_to_III_II_I() {
        assertEquals("I → II → III", CycleGenerator.rotationLabel(cfg(CyclePattern.B3_CIAGLY, false)))
        assertEquals("III → II → I", CycleGenerator.rotationLabel(cfg(CyclePattern.B3_CIAGLY, true)))
    }

    @Test
    fun odwrocenie_zmienia_przypisanie_zmian_do_dni() {
        val normalnie = CycleGenerator.range(cfg(CyclePattern.B3_CIAGLY, false), start, start.plusDays(15))
        val odwrotnie = CycleGenerator.range(cfg(CyclePattern.B3_CIAGLY, true), start, start.plusDays(15))
        assertEquals(Shift.I, normalnie[start])
        assertEquals(Shift.W5, odwrotnie[start])          // odwrócony cykl zaczyna się od wolnych
        assertEquals(Shift.III, odwrotnie[start.plusDays(4)])
    }

    @Test
    fun odwrocenie_nie_zmienia_liczby_dni_pracy_w_cyklu() {
        listOf(CyclePattern.B4_16D, CyclePattern.B3_CIAGLY, CyclePattern.B4_CIAGLY, CyclePattern.TYGODNIOWY)
            .forEach { p ->
                val a = CycleGenerator.days(cfg(p, false)).count { it != "w" }
                val b = CycleGenerator.days(cfg(p, true)).count { it != "w" }
                assertEquals("wzorzec ${p.name}", a, b)
            }
    }
}
