package pl.grafik.pracy

import org.junit.Assert.assertEquals
import org.junit.Test
import pl.grafik.pracy.domain.DayKind
import pl.grafik.pracy.domain.OtRate
import pl.grafik.pracy.domain.PresenceEngine
import pl.grafik.pracy.domain.PresenceSpan
import pl.grafik.pracy.domain.Shift
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Stawka dodatku za nadgodziny. Zakład Macieja płaci 100 % za dni wolne, niedziele,
 * święta i za nadgodziny przy nocce — także te wypadające po 6:00 rano.
 */
class NadgodzinyNoceTest {

    private fun czas(dzien: Int, godz: Int, min: Int = 0) =
        LocalDateTime.of(2026, 9, dzien, godz, min)

    private fun analiza(dzien: Int, od: LocalDateTime, doKiedy: LocalDateTime, shift: Shift?) =
        PresenceEngine.analyze(
            LocalDate.of(2026, 9, dzien), PresenceSpan(od, doKiedy), shift, DayKind.ZWYKLY
        )

    @Test
    fun zostanie_po_nocce_daje_sto_procent() {
        // nocka 22:00–6:00, wyjście dopiero o 8:00 — dwie nadgodziny już za dnia
        val w = analiza(17, czas(17, 21, 55), czas(18, 8, 0), Shift.III)
        assertEquals(2, w.otHours)
        assertEquals(OtRate.P100, w.otRate)
    }

    @Test
    fun wczesniejsze_przyjscie_na_nocke_tez_daje_sto_procent() {
        val w = analiza(17, czas(17, 20, 0), czas(18, 6, 5), Shift.III)
        assertEquals(2, w.otHours)
        assertEquals(OtRate.P100, w.otRate)
    }

    @Test
    fun nadgodziny_na_dniowce_zostaja_przy_piecdziesieciu() {
        // zmiana I 6:00–14:00, zostaje do 16:00 — dzień jak dzień
        val w = analiza(7, czas(7, 5, 55), czas(7, 16, 0), Shift.I)
        assertEquals(2, w.otHours)
        assertEquals(OtRate.P50, w.otRate)
    }

    @Test
    fun popoludniowka_przedluzona_w_noc_daje_sto_procent() {
        // zmiana II 14:00–22:00, zostaje do 24:00 — te dwie godziny są już porą nocną
        val w = analiza(21, czas(21, 13, 55), czas(22, 0, 0), Shift.II)
        assertEquals(2, w.otHours)
        assertEquals(OtRate.P100, w.otRate)
    }

    @Test
    fun praca_w_dzien_wolny_to_nadal_sto_procent() {
        val w = analiza(19, czas(19, 6, 0), czas(19, 14, 0), Shift.W5)
        assertEquals(8, w.otHours)
        assertEquals(OtRate.P100, w.otRate)
    }

    @Test
    fun pora_nocna_to_przedzial_od_dwudziestej_drugiej_do_szostej() {
        assertEquals(true, PresenceEngine.wPorzeNocnej(czas(17, 23, 0), czas(18, 1, 0)))
        assertEquals(true, PresenceEngine.wPorzeNocnej(czas(17, 21, 0), czas(17, 22, 30)))
        assertEquals(true, PresenceEngine.wPorzeNocnej(czas(18, 5, 30), czas(18, 7, 0)))
        assertEquals(false, PresenceEngine.wPorzeNocnej(czas(18, 6, 0), czas(18, 14, 0)))
        assertEquals(false, PresenceEngine.wPorzeNocnej(czas(18, 14, 0), czas(18, 22, 0)))
    }
}
