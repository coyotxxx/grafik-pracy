package pl.grafik.pracy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pl.grafik.pracy.domain.PlanPowiadomien
import pl.grafik.pracy.domain.Shift
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Kiedy aplikacja ma się odezwać — i kiedy ma siedzieć cicho, bo Maciej śpi po nocce.
 */
class PowiadomieniaTest {

    private fun d(dzien: Int) = LocalDate.of(2026, 9, dzien)

    @Test
    fun przypomnienie_o_wydarzeniu_wypada_przed_godzina() {
        assertEquals(
            LocalDateTime.of(2026, 9, 18, 8, 20),
            PlanPowiadomien.przedWydarzeniem(d(18), "9:20", 60)
        )
        assertEquals(
            LocalDateTime.of(2026, 9, 18, 9, 5),
            PlanPowiadomien.przedWydarzeniem(d(18), "09:20", 15)
        )
    }

    @Test
    fun wydarzenie_bez_godziny_nie_ma_przypomnienia_krotko_przed() {
        assertNull(PlanPowiadomien.przedWydarzeniem(d(18), "", 60))
        assertNull(PlanPowiadomien.przedWydarzeniem(d(18), "bzdura", 60))
    }

    @Test
    fun przypomnienie_o_zmianie_godzine_przed_startem() {
        assertEquals(
            LocalDateTime.of(2026, 9, 18, 21, 0),      // nocka startuje 22:00
            PlanPowiadomien.przedZmiana(d(18), Shift.III, 60)
        )
        assertEquals(
            LocalDateTime.of(2026, 9, 18, 5, 0),       // ranna startuje 6:00
            PlanPowiadomien.przedZmiana(d(18), Shift.I, 60)
        )
    }

    @Test
    fun dzien_wolny_nie_ma_startu_zmiany() {
        assertNull(PlanPowiadomien.przedZmiana(d(19), Shift.W5, 60))
        assertNull(PlanPowiadomien.przedZmiana(d(19), Shift.URLOP, 60))
        assertNull(PlanPowiadomien.przedZmiana(d(19), null, 60))
    }

    @Test
    fun powiadomienie_w_trakcie_nocki_ladzie_po_niej() {
        // nocka z 18 na 19: 22:00 → 06:00, cisza do 12:00
        val grafik = { dzien: LocalDate -> if (dzien == d(18)) Shift.III else null }
        val wTrakcie = LocalDateTime.of(2026, 9, 19, 2, 0)
        assertEquals(
            LocalDateTime.of(2026, 9, 19, 12, 0),
            PlanPowiadomien.zCiszaNaNocce(wTrakcie, grafik)
        )
    }

    @Test
    fun powiadomienie_zaraz_po_nocce_tez_czeka() {
        val grafik = { dzien: LocalDate -> if (dzien == d(18)) Shift.III else null }
        val tuzPo = LocalDateTime.of(2026, 9, 19, 8, 30)
        assertEquals(
            LocalDateTime.of(2026, 9, 19, 12, 0),
            PlanPowiadomien.zCiszaNaNocce(tuzPo, grafik)
        )
    }

    @Test
    fun po_ciszy_powiadomienie_idzie_bez_zmian() {
        val grafik = { dzien: LocalDate -> if (dzien == d(18)) Shift.III else null }
        val popoludnie = LocalDateTime.of(2026, 9, 19, 15, 0)
        assertEquals(popoludnie, PlanPowiadomien.zCiszaNaNocce(popoludnie, grafik))
    }

    @Test
    fun bez_nocki_nic_nie_przesuwamy() {
        val grafik = { _: LocalDate -> Shift.I }
        val rano = LocalDateTime.of(2026, 9, 19, 5, 0)
        assertEquals(rano, PlanPowiadomien.zCiszaNaNocce(rano, grafik))
    }

    @Test
    fun etykiety_wyprzedzenia_sa_czytelne() {
        assertEquals("15 min", PlanPowiadomien.etykietaWyprzedzenia(15))
        assertEquals("30 min", PlanPowiadomien.etykietaWyprzedzenia(30))
        assertEquals("1 h", PlanPowiadomien.etykietaWyprzedzenia(60))
        assertEquals("2 h", PlanPowiadomien.etykietaWyprzedzenia(120))
    }
}
