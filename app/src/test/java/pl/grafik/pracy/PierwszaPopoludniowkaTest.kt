package pl.grafik.pracy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.grafik.pracy.domain.DayKind
import pl.grafik.pracy.domain.OtRate
import pl.grafik.pracy.domain.PresenceEngine
import pl.grafik.pracy.domain.PresenceSpan
import pl.grafik.pracy.domain.Shift
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Nadgodziny przy drugiej zmianie, tak jak płaci je zakład Macieja:
 *
 * - po 22:00, czyli po końcu popołudniówki — 100 % (pora nocna);
 * - przed 14:00, czyli wcześniejsze przyjście — 50 %;
 * - chyba że to PIERWSZA popołudniówka w bloku — wtedy te wcześniejsze godziny
 *   idą po 100 %. Kolejne dni bloku mają już 50 %.
 */
class PierwszaPopoludniowkaTest {

    private val poniedzialek = LocalDate.of(2026, 9, 21)
    private val start = poniedzialek.atTime(14, 0)

    private fun przedzial(od: LocalDateTime, doKiedy: LocalDateTime) = listOf(od to doKiedy)

    // ─── sama reguła ─────────────────────────────────────────────

    @Test
    fun pierwsza_popoludniowka_po_wolnem_daje_sto_procent() {
        val cztery = przedzial(poniedzialek.atTime(10, 0), start)
        assertTrue(
            PresenceEngine.przedPierwszaPopoludniowka(Shift.II, null, cztery, start)
        )
    }

    @Test
    fun pierwsza_popoludniowka_po_nockach_tez() {
        val cztery = przedzial(poniedzialek.atTime(10, 0), start)
        assertTrue(
            PresenceEngine.przedPierwszaPopoludniowka(Shift.II, Shift.III, cztery, start)
        )
        assertTrue(
            PresenceEngine.przedPierwszaPopoludniowka(Shift.II, Shift.I, cztery, start)
        )
    }

    @Test
    fun kolejny_dzien_bloku_to_juz_piecdziesiat() {
        val cztery = przedzial(poniedzialek.atTime(10, 0), start)
        assertFalse(
            "wtorek po poniedziałkowej popołudniówce",
            PresenceEngine.przedPierwszaPopoludniowka(Shift.II, Shift.II, cztery, start)
        )
    }

    @Test
    fun godziny_po_zmianie_nie_korzystaja_z_tej_reguly() {
        // zostanie po 22:00 — reguła pierwszej popołudniówki tu nie działa,
        // te godziny i tak mają 100 % z tytułu pory nocnej
        val po = przedzial(poniedzialek.atTime(22, 0), poniedzialek.atTime(23, 30))
        assertFalse(
            PresenceEngine.przedPierwszaPopoludniowka(Shift.II, null, po, start)
        )
    }

    @Test
    fun inne_zmiany_nie_podlegaja_tej_regule() {
        val rano = przedzial(poniedzialek.atTime(4, 0), poniedzialek.atTime(6, 0))
        assertFalse(PresenceEngine.przedPierwszaPopoludniowka(Shift.I, null, rano, poniedzialek.atTime(6, 0)))
        assertFalse(PresenceEngine.przedPierwszaPopoludniowka(Shift.III, null, rano, poniedzialek.atTime(22, 0)))
    }

    // ─── stawka wyliczana z całości ──────────────────────────────

    @Test
    fun stawka_uwzglednia_pierwsza_popoludniowke() {
        assertEquals(
            OtRate.P100,
            PresenceEngine.rateFor(
                DayKind.ZWYKLY, onFreeDay = false, przedPierwszaPopoludniowka = true
            )
        )
        assertEquals(
            OtRate.P50,
            PresenceEngine.rateFor(DayKind.ZWYKLY, onFreeDay = false)
        )
    }

    // ─── cała analiza pobytu ─────────────────────────────────────

    @Test
    fun przyjscie_o_dziesiatej_na_pierwsza_popoludniowke_to_sto_procent() {
        val pobyt = PresenceSpan(poniedzialek.atTime(10, 0), poniedzialek.atTime(22, 0))
        val wynik = PresenceEngine.analyze(
            poniedzialek, pobyt, Shift.II, DayKind.ZWYKLY,
            poprzedniaZmiana = null            // w niedzielę wolne
        )

        assertEquals(4, wynik.otHours)
        assertEquals(OtRate.P100, wynik.otRate)
    }

    @Test
    fun to_samo_przyjscie_w_kolejnym_dniu_bloku_to_piecdziesiat() {
        val wtorek = poniedzialek.plusDays(1)
        val pobyt = PresenceSpan(wtorek.atTime(10, 0), wtorek.atTime(22, 0))
        val wynik = PresenceEngine.analyze(
            wtorek, pobyt, Shift.II, DayKind.ZWYKLY,
            poprzedniaZmiana = Shift.II
        )

        assertEquals(4, wynik.otHours)
        assertEquals(OtRate.P50, wynik.otRate)
    }

    @Test
    fun zostanie_po_dwudziestej_drugiej_to_zawsze_sto_procent() {
        val wtorek = poniedzialek.plusDays(1)
        val pobyt = PresenceSpan(wtorek.atTime(14, 0), wtorek.atTime(23, 30))
        val wynik = PresenceEngine.analyze(
            wtorek, pobyt, Shift.II, DayKind.ZWYKLY,
            poprzedniaZmiana = Shift.II        // środek bloku, a mimo to setka
        )

        assertTrue(wynik.otHours >= 1)
        assertEquals(OtRate.P100, wynik.otRate)
    }

    @Test
    fun zwykla_popoludniowka_bez_nadgodzin_nie_dostaje_stawki() {
        val wtorek = poniedzialek.plusDays(1)
        val pobyt = PresenceSpan(wtorek.atTime(14, 0), wtorek.atTime(22, 0))
        val wynik = PresenceEngine.analyze(
            wtorek, pobyt, Shift.II, DayKind.ZWYKLY, poprzedniaZmiana = Shift.II
        )
        assertEquals(0, wynik.otHours)
    }
}
