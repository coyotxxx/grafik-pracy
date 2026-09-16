package pl.grafik.pracy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.grafik.pracy.domain.*
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Reguła Macieja: wejście zaokrąglamy W GÓRĘ, wyjście W DÓŁ — do pełnych godzin.
 * Zaokrąglenie liczy tylko nadwyżkę poza zmianą.
 */
class PresenceEngineTest {

    private val d = LocalDate.of(2026, 9, 16)      // środa, dzień zwykły
    private fun at(h: Int, m: Int, day: LocalDate = d): LocalDateTime = day.atTime(h, m)
    private fun span(fromH: Int, fromM: Int, toH: Int, toM: Int) =
        PresenceSpan(at(fromH, fromM), at(toH, toM))

    // ---- przykłady podane wprost przez Macieja ----

    @Test
    fun `przyklad 1 — 6-00 do 15-15 na zmianie I daje 1 godzine nadgodzin`() {
        val r = PresenceEngine.analyze(d, span(6, 0, 15, 15), Shift.I, DayKind.ZWYKLY)
        assertEquals(9, r.countedHours)
        assertEquals(1, r.otHours)
        assertEquals(OtRate.P50, r.otRate)
        assertEquals(at(6, 0), r.countedFrom)
        assertEquals(at(15, 0), r.countedTo)
    }

    @Test
    fun `przyklad 2 — 4-30 do 14-15 na zmianie I daje 1 godzine nadgodzin`() {
        val r = PresenceEngine.analyze(d, span(4, 30, 14, 15), Shift.I, DayKind.ZWYKLY)
        assertEquals(at(5, 0), r.countedFrom)     // 4:30 -> 5:00 w górę
        assertEquals(at(14, 0), r.countedTo)      // 14:15 -> 14:00 w dół
        assertEquals(9, r.countedHours)
        assertEquals(1, r.otHours)
    }

    // ---- przypadki brzegowe ----

    @Test
    fun `40 minut wczesniej i 40 minut dluzej to zero nadgodzin`() {
        val r = PresenceEngine.analyze(d, span(5, 20, 14, 40), Shift.I, DayKind.ZWYKLY)
        assertEquals(8, r.countedHours)
        assertEquals(0, r.otHours)
    }

    @Test
    fun `spoznienie nie obcina normy`() {
        val r = PresenceEngine.analyze(d, span(6, 10, 14, 0), Shift.I, DayKind.ZWYKLY)
        assertEquals(at(6, 0), r.countedFrom)     // NIE 7:00
        assertEquals(8, r.countedHours)
        assertEquals(0, r.otHours)
    }

    @Test
    fun `wczesniejsze wyjscie nie obcina normy`() {
        val r = PresenceEngine.analyze(d, span(6, 0, 13, 20), Shift.I, DayKind.ZWYKLY)
        assertEquals(at(14, 0), r.countedTo)      // NIE 13:00
        assertEquals(8, r.countedHours)
        assertEquals(0, r.otHours)
    }

    @Test
    fun `rowne godziny zostaja bez zmian`() {
        val r = PresenceEngine.analyze(d, span(5, 0, 16, 0), Shift.I, DayKind.ZWYKLY)
        assertEquals(11, r.countedHours)
        assertEquals(3, r.otHours)
    }

    // ---- zmiana III przez północ ----

    @Test
    fun `zmiana III w normie — 21-40 do 6-30 nastepnego dnia`() {
        val s = PresenceSpan(at(21, 40), at(6, 30, d.plusDays(1)))
        val r = PresenceEngine.analyze(d, s, Shift.III, DayKind.ZWYKLY)
        assertEquals(at(22, 0), r.countedFrom)
        assertEquals(at(6, 0, d.plusDays(1)), r.countedTo)
        assertEquals(8, r.countedHours)
        assertEquals(0, r.otHours)
    }

    @Test
    fun `zmiana III z nadgodzinami — 21-00 do 7-20`() {
        val s = PresenceSpan(at(21, 0), at(7, 20, d.plusDays(1)))
        val r = PresenceEngine.analyze(d, s, Shift.III, DayKind.ZWYKLY)
        assertEquals(10, r.countedHours)
        assertEquals(2, r.otHours)
    }

    @Test
    fun `wejscie nad ranem przypisuje sie do wczorajszej zmiany III`() {
        val s = PresenceSpan(at(1, 30, d.plusDays(1)), at(6, 10, d.plusDays(1)))
        val assigned = PresenceEngine.assignDate(s) { day -> if (day == d) Shift.III else Shift.I }
        assertEquals(d, assigned)
    }

    @Test
    fun `wejscie rano w dzien bez nocki zostaje w swoim dniu`() {
        val s = PresenceSpan(at(5, 30), at(14, 10))
        val assigned = PresenceEngine.assignDate(s) { Shift.I }
        assertEquals(d, assigned)
    }

    // ---- dzień wolny / niedziela / święto ----

    @Test
    fun `praca w dniu wolnym to cala obecnosc jako nadgodziny 100 procent`() {
        val r = PresenceEngine.analyze(d, span(7, 40, 12, 50), Shift.W5, DayKind.ZWYKLY)
        assertEquals(at(8, 0), r.countedFrom)
        assertEquals(at(12, 0), r.countedTo)
        assertEquals(4, r.otHours)
        assertEquals(OtRate.P100, r.otRate)
        assertTrue(r.onFreeDay)
    }

    @Test
    fun `nadgodziny w niedziele sa po 100 procent mimo zaplanowanej zmiany`() {
        val sunday = LocalDate.of(2026, 9, 20)
        val s = PresenceSpan(sunday.atTime(6, 0), sunday.atTime(15, 30))
        val r = PresenceEngine.analyze(sunday, s, Shift.I, DayKind.NIEDZIELA)
        assertEquals(1, r.otHours)
        assertEquals(OtRate.P100, r.otRate)
    }

    @Test
    fun `nadgodziny w swieto sa po 100 procent`() {
        val r = PresenceEngine.analyze(d, span(6, 0, 16, 5), Shift.I, DayKind.SWIETO)
        assertEquals(2, r.otHours)
        assertEquals(OtRate.P100, r.otRate)
    }

    @Test
    fun `nadgodziny w zaplanowana sobote sa po 50 procent`() {
        val r = PresenceEngine.analyze(d, span(6, 0, 16, 5), Shift.I, DayKind.SOBOTA)
        assertEquals(OtRate.P50, r.otRate)
    }

    // ---- sklejanie i odrzucanie pobytów ----

    @Test
    fun `krotkie wyjscie w srodku dnia jest sklejane`() {
        val raw = listOf(span(6, 0, 11, 0), span(11, 20, 15, 15))
        val merged = PresenceEngine.mergeSpans(raw, 30)
        assertEquals(1, merged.size)
        val r = PresenceEngine.analyze(d, merged.first(), Shift.I, DayKind.ZWYKLY)
        assertEquals(1, r.otHours)
    }

    @Test
    fun `dluga przerwa nie jest sklejana`() {
        val raw = listOf(span(6, 0, 11, 0), span(13, 0, 15, 15))
        assertEquals(2, PresenceEngine.mergeSpans(raw, 30).size)
    }

    @Test
    fun `przejazd obok zakladu nie jest dniem pracy`() {
        val raw = listOf(span(6, 0, 6, 12))
        assertTrue(PresenceEngine.filterShort(raw, 30).isEmpty())
    }

    @Test
    fun `pobyt rowny progowi jest uznawany`() {
        val raw = listOf(span(6, 0, 6, 30))
        assertEquals(1, PresenceEngine.filterShort(raw, 30).size)
    }

    // ---- zaokrąglanie ----

    @Test
    fun `zaokraglanie w gore i w dol`() {
        assertEquals(at(5, 0), PresenceEngine.roundUp(at(4, 30)))
        assertEquals(at(5, 0), PresenceEngine.roundUp(at(4, 1)))
        assertEquals(at(4, 0), PresenceEngine.roundUp(at(4, 0)))
        assertEquals(at(15, 0), PresenceEngine.roundDown(at(15, 59)))
        assertEquals(at(15, 0), PresenceEngine.roundDown(at(15, 0)))
    }
}
