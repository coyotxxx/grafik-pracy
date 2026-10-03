package pl.grafik.pracy

import org.junit.Assert.assertEquals
import org.junit.Test
import pl.grafik.pracy.domain.DayKind
import pl.grafik.pracy.domain.OtRate
import pl.grafik.pracy.domain.PresenceEngine
import pl.grafik.pracy.domain.PresenceSpan
import pl.grafik.pracy.domain.Shift
import java.time.LocalDate

/**
 * Praca w dniu wolnym — sobota 3.10.2026.
 *
 * Maciej przyszedł do pracy w dniu, który w grafiku jest wolny, i zapytał, co
 * aplikacja zrobi po jego wyjściu. Normy tego dnia nie ma, więc cała obecność
 * to nadgodziny po 100 % (art. 151-1 § 1 pkt 1 lit. b KP), a etykieta dnia
 * zostaje „wolne" — aplikacja nie zgaduje, że to była zmiana I.
 */
class PracaWDniuWolnymTest {

    private val sobota = LocalDate.of(2026, 10, 3)

    private fun wynik(odG: Int, odM: Int, doG: Int, doM: Int) = PresenceEngine.analyze(
        date = sobota,
        span = PresenceSpan(sobota.atTime(odG, odM), sobota.atTime(doG, doM)),
        shift = Shift.W5,
        kind = DayKind.ZWYKLY
    )

    @Test
    fun wyjscie_po_czternastej_daje_osiem_nadgodzin_po_sto_procent() {
        // Wejście jak zwykle na pierwszą zmianę: 5:40, wyjście 14:05.
        val r = wynik(5, 40, 14, 5)
        assertEquals("zaokrąglenie do pełnych godzin: 6:00–14:00", 8, r.countedHours)
        assertEquals("normy w dniu wolnym nie ma, więc wszystko to nadgodziny", 8, r.otHours)
        assertEquals(OtRate.P100, r.otRate)
    }

    @Test
    fun etykieta_dnia_zostaje_wolna() {
        // Aplikacja nie podmienia „wolne" na „zmiana I" — grafik mówi, że dzień jest wolny,
        // a to, że ktoś przyszedł, nie zmienia rozkładu czasu pracy.
        val r = wynik(5, 40, 14, 5)
        assertEquals(Shift.W5, r.shift)
    }

    @Test
    fun krotsze_wyjscie_liczy_tylko_pelne_godziny() {
        assertEquals(6, wynik(5, 40, 12, 30).otHours)   // 6:00–12:00
        assertEquals(4, wynik(6, 0, 10, 0).otHours)
    }

    @Test
    fun wyjscie_po_dwudziestej_drugiej_dalej_sto_procent() {
        // Dzień wolny przebija wszystko inne — nie trzeba pory nocnej.
        val r = wynik(5, 40, 22, 30)
        assertEquals(16, r.otHours)
        assertEquals(OtRate.P100, r.otRate)
    }

    @Test
    fun w_dniu_roboczym_te_same_godziny_dalyby_zero_nadgodzin() {
        // Dla porównania: ta sama obecność w dniu ze zmianą I to zwykła dniówka.
        val r = PresenceEngine.analyze(
            date = sobota,
            span = PresenceSpan(sobota.atTime(5, 40), sobota.atTime(14, 5)),
            shift = Shift.I,
            kind = DayKind.ZWYKLY
        )
        assertEquals(8, r.countedHours)
        assertEquals(0, r.otHours)
    }
}
