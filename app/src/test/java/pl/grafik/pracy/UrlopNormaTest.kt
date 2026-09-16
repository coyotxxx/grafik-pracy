package pl.grafik.pracy

import org.junit.Assert.assertEquals
import org.junit.Test
import pl.grafik.pracy.domain.MonthStats
import pl.grafik.pracy.domain.Shift

/**
 * Reguła Macieja: „urlop pokrywa dzień roboczy".
 * Dzień urlopu wchodzi do rozliczenia miesiąca jak przepracowany, więc nie robi niedoboru.
 */
class UrlopNormaTest {

    @Test
    fun trzy_dni_urlopu_pokrywaja_dwadziescia_cztery_godziny() {
        val st = MonthStats(worked = 152, norm = 176, urlopH = 24)
        assertEquals("do rozliczenia idzie całość", 176, st.rozliczone)
        assertEquals("norma wykonana, zero niedoboru", 0, st.diff)
    }

    @Test
    fun bez_urlopu_nic_sie_nie_zmienia() {
        val st = MonthStats(worked = 176, norm = 176)
        assertEquals(176, st.rozliczone)
        assertEquals(0, st.diff)
    }

    @Test
    fun nadgodziny_liczone_ponad_pokrycie_urlopem() {
        val st = MonthStats(worked = 160, norm = 176, urlopH = 24, ot100 = 8)
        assertEquals(184, st.rozliczone)
        assertEquals("8 h ponad normę", 8, st.diff)
    }

    @Test
    fun realny_niedobor_nadal_widac() {
        val st = MonthStats(worked = 140, norm = 176, urlopH = 8)
        assertEquals(148, st.rozliczone)
        assertEquals(-28, st.diff)
    }

    @Test
    fun urlop_nie_zmienia_rozkladu_zmian() {
        val st = MonthStats(
            worked = 152, norm = 176, urlopH = 24,
            byShift = mapOf(Shift.I to 48, Shift.II to 56, Shift.III to 48)
        )
        assertEquals("urlop nie doklejał się do żadnej zmiany", 152, st.byShift.values.sum())
    }

    @Test
    fun urlop_liczy_sie_zawsze_gdy_dzien_jest_oznaczony() {
        // dwa dni urlopu = 16 h, niezależnie od tego, co mówił cykl —
        // skoro dzień znika z puli urlopu, musi też pokryć godziny
        val st = MonthStats(worked = 160, norm = 176, urlopH = 16)
        assertEquals(176, st.rozliczone)
        assertEquals(0, st.diff)
    }

    @Test
    fun w_trwajacym_miesiacu_liczymy_do_dzis() {
        val st = MonthStats(worked = 176, norm = 176, doDzis = 88, biezacyMiesiac = true)
        assertEquals("połowa miesiąca", 88, st.doDzis)
        assertEquals("cały miesiąc i tak wyjdzie na normę", 176, st.rozliczone)
        assertEquals(0, st.diff)
    }

    @Test
    fun w_zamknietym_miesiacu_do_dzis_nie_ma_znaczenia() {
        val st = MonthStats(worked = 176, norm = 176, doDzis = 176, biezacyMiesiac = false)
        assertEquals(176, st.rozliczone)
    }
}
