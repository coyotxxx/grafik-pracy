package pl.grafik.pracy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.grafik.pracy.domain.CycleConfig
import pl.grafik.pracy.domain.CycleGenerator
import pl.grafik.pracy.domain.CyclePattern
import pl.grafik.pracy.domain.Shift
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Rotacja tygodniowa: pracujemy pon–pt, sobota i niedziela ZAWSZE wolne.
 * Przesunięcie może być tylko o całe tygodnie.
 */
class WeeklyCycleTest {

    private fun cfg(index: Int = 0, rev: Boolean = false) = CycleConfig(
        pattern = CyclePattern.TYGODNIOWY,
        anchorDate = LocalDate.of(2024, 3, 1),      // piątek — celowo NIE poniedziałek
        anchorIndex = index,
        generate = true,
        reverse = rev
    )

    private fun rok(c: CycleConfig) =
        CycleGenerator.range(c, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))

    @Test
    fun wzorzec_tygodniowy_jest_oznaczony_jako_przywiazany_do_tygodnia() {
        assertTrue(CyclePattern.TYGODNIOWY.weekAligned)
        assertEquals(3, CyclePattern.TYGODNIOWY.weeks)
        listOf(CyclePattern.B4_16D, CyclePattern.B4_CIAGLY, CyclePattern.B3_CIAGLY)
            .forEach { assertTrue("${it.name} nie jest tygodniowy", !it.weekAligned) }
    }

    @Test
    fun soboty_i_niedziele_sa_zawsze_wolne() {
        rok(cfg()).forEach { (d, sh) ->
            if (d.dayOfWeek == DayOfWeek.SATURDAY || d.dayOfWeek == DayOfWeek.SUNDAY) {
                assertEquals("$d (${d.dayOfWeek}) musi być wolne", Shift.W5, sh)
            }
        }
    }

    @Test
    fun od_poniedzialku_do_piatku_zawsze_pracujemy() {
        rok(cfg()).forEach { (d, sh) ->
            if (d.dayOfWeek.value <= 5) {
                assertTrue("$d (${d.dayOfWeek}) powinien być dniem pracy, jest $sh", sh.isWork)
            }
        }
    }

    @Test
    fun przesuniecie_o_tydzien_nie_rusza_weekendu() {
        (0..2).forEach { i ->
            rok(cfg(index = i)).forEach { (d, sh) ->
                if (d.dayOfWeek.value >= 6) assertEquals("przesunięcie $i, $d", Shift.W5, sh)
            }
        }
    }

    @Test
    fun przesuniecie_zmienia_ktora_zmiane_mam_w_tygodniu() {
        val pon = LocalDate.of(2026, 1, 5)          // poniedziałek
        val a = CycleGenerator.shiftFor(cfg(index = 0), pon)
        val b = CycleGenerator.shiftFor(cfg(index = 1), pon)
        val c = CycleGenerator.shiftFor(cfg(index = 2), pon)
        assertEquals(setOf(Shift.I, Shift.II, Shift.III), setOf(a, b, c))
    }

    @Test
    fun caly_tydzien_to_ta_sama_zmiana() {
        val pon = LocalDate.of(2026, 1, 5)
        val zmiana = CycleGenerator.shiftFor(cfg(), pon)
        (0..4).forEach { i ->
            assertEquals(zmiana, CycleGenerator.shiftFor(cfg(), pon.plusDays(i.toLong())))
        }
    }

    @Test
    fun odwrocenie_rotacji_tez_zachowuje_wolne_weekendy() {
        rok(cfg(rev = true)).forEach { (d, sh) ->
            if (d.dayOfWeek.value >= 6) assertEquals("odwrotnie, $d", Shift.W5, sh)
        }
        assertEquals("III → II → I", CycleGenerator.rotationLabel(cfg(rev = true)))
    }

    @Test
    fun normalna_kolejnosc_to_I_II_III() {
        assertEquals("I → II → III", CycleGenerator.rotationLabel(cfg()))
    }
}
