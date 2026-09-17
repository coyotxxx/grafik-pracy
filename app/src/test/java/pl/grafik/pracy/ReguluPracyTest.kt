package pl.grafik.pracy

import org.junit.Assert.*
import org.junit.Test
import pl.grafik.pracy.domain.*
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Zasady, które apka musi znać: dobowy odpoczynek (art. 132 KP), rozpoznanie zmiany
 * po godzinie przyjazdu i zakaz rozciągania okna zmiany na pobyt, który w nie nie trafia.
 */
class ReguluPracyTest {

    private fun dt(d: String, t: String) = LocalDateTime.parse("${d}T$t")
    private fun span(d1: String, t1: String, d2: String, t2: String) =
        PresenceSpan(dt(d1, t1), dt(d2, t2))

    // ——— przypadek Macieja: sobota wolna, przyjazd na nockę ———

    @Test fun nocka_w_wolna_sobote_to_osiem_nadgodzin_sto_procent() {
        val d = LocalDate.parse("2026-09-19")                       // sobota, w grafiku w5
        val r = PresenceEngine.analyze(d, span("2026-09-19", "21:30", "2026-09-20", "06:15"),
            Shift.W5, Holidays.kindOf(d))

        assertEquals(dt("2026-09-19", "22:00"), r.countedFrom)
        assertEquals(dt("2026-09-20", "06:00"), r.countedTo)
        assertEquals(8, r.otHours)
        assertEquals(OtRate.P100, r.otRate)
        assertTrue(r.onFreeDay)
        assertEquals(Shift.III, r.recognized)                       // apka wie, że to nocka
    }

    // ——— A: okno zmiany tylko wtedy, gdy pobyt w nie trafia ———

    @Test fun przyjazd_poza_zmiana_nie_rozciaga_liczenia_na_dobe() {
        val d = LocalDate.parse("2026-09-21")                       // poniedziałek, grafik: zmiana I
        val r = PresenceEngine.analyze(d, span("2026-09-21", "21:30", "2026-09-22", "06:15"),
            Shift.I, DayKind.ZWYKLY)

        assertEquals(8, r.countedHours)                             // przed naprawą wychodziły 24 h
        assertNotEquals(16, r.otHours)
    }

    // ——— B: rozpoznanie zmiany po godzinie przyjazdu ———

    @Test fun zamiana_zmian_liczy_norme_tej_zmiany_na_ktora_przyszedlem() {
        val d = LocalDate.parse("2026-09-21")
        val r = PresenceEngine.analyze(d, span("2026-09-21", "21:30", "2026-09-22", "06:15"),
            Shift.I, DayKind.ZWYKLY)

        assertEquals(Shift.III, r.shift)                            // grafik mówił I, przyszedłem na III
        assertEquals(8, r.normHours)
        assertEquals(0, r.otHours)                                  // zamiana zmian to nie nadgodziny
    }

    @Test fun krotka_wizyta_nie_udaje_calej_zmiany() {
        val d = LocalDate.parse("2026-09-21")
        val r = PresenceEngine.analyze(d, span("2026-09-21", "16:00", "2026-09-21", "18:30"),
            Shift.I, DayKind.ZWYKLY)

        assertEquals(0, r.normHours)                                // 2,5 h to nie zmiana II
        assertEquals(2, r.otHours)
        assertEquals(OtRate.P50, r.otRate)
    }

    @Test fun rozpoznanie_zmiany_po_godzinie_wejscia() {
        assertEquals(Shift.I, PresenceEngine.shiftByEntry(dt("2026-09-21", "05:40")))
        assertEquals(Shift.II, PresenceEngine.shiftByEntry(dt("2026-09-21", "13:40")))
        assertEquals(Shift.III, PresenceEngine.shiftByEntry(dt("2026-09-21", "21:30")))
    }

    // ——— druga wizyta w tym samym dniu ———

    @Test fun gdy_norma_juz_policzona_liczymy_tylko_faktyczne_godziny() {
        val d = LocalDate.parse("2026-09-21")
        val planowane = PresenceEngine.shiftWindow(d, Shift.I)
        val wczesniej = listOf(span("2026-09-21", "06:00", "2026-09-21", "14:00"))
        assertTrue(PresenceEngine.normAlreadyCounted(planowane, wczesniej))

        val r = PresenceEngine.analyze(d, span("2026-09-21", "21:30", "2026-09-22", "06:15"),
            Shift.I, DayKind.ZWYKLY, normUsed = true)
        assertEquals(0, r.normHours)
        assertEquals(8, r.otHours)                                  // druga zmiana tego dnia = nadgodziny
    }

    @Test fun krotkie_zajrzenie_nie_zabiera_normy_wlasciwej_zmianie() {
        val planowane = PresenceEngine.shiftWindow(LocalDate.parse("2026-09-21"), Shift.I)
        val zajrzenie = listOf(span("2026-09-21", "05:00", "2026-09-21", "05:00"))
        assertFalse(PresenceEngine.normAlreadyCounted(planowane, zajrzenie))
    }

    // ——— C: dobowy odpoczynek decyduje, do którego dnia należy pobyt ———

    @Test fun po_nocce_wejscie_przed_uplywem_odpoczynku_to_wciaz_tamten_dzien() {
        val nocka = { _: LocalDate -> Shift.III }                   // 18.09 zmiana III, koniec 19.09 o 6:00

        // zaraz po nocce — zostałem dłużej
        assertEquals(LocalDate.parse("2026-09-18"),
            PresenceEngine.assignDate(span("2026-09-19", "05:40", "2026-09-19", "08:00"), nocka))

        // wezwany w środku dnia — nowa zmiana jest niemożliwa, brak 11 h odpoczynku
        assertEquals(LocalDate.parse("2026-09-18"),
            PresenceEngine.assignDate(span("2026-09-19", "10:00", "2026-09-19", "12:00"), nocka))

        // 21:30 to już po 17:00, czyli po wymaganym odpoczynku — nowa zmiana, nowy dzień
        assertEquals(LocalDate.parse("2026-09-19"),
            PresenceEngine.assignDate(span("2026-09-19", "21:30", "2026-09-20", "06:15"), nocka))
    }

    @Test fun po_dniu_wolnym_zawsze_biezacy_dzien() {
        assertEquals(LocalDate.parse("2026-09-21"),
            PresenceEngine.assignDate(span("2026-09-21", "05:40", "2026-09-21", "14:10")) { Shift.W5 })
    }

    @Test fun normalna_rotacja_nie_wpada_w_regule_odpoczynku() {
        // dzień po dniu ta sama zmiana — odstęp zawsze większy niż 11 h
        assertEquals(LocalDate.parse("2026-09-22"),
            PresenceEngine.assignDate(span("2026-09-22", "13:40", "2026-09-22", "22:10")) { Shift.II })
        assertEquals(LocalDate.parse("2026-09-08"),
            PresenceEngine.assignDate(span("2026-09-08", "05:40", "2026-09-08", "14:10")) { Shift.I })
        assertEquals(LocalDate.parse("2026-09-16"),
            PresenceEngine.assignDate(span("2026-09-16", "21:40", "2026-09-17", "06:10")) { Shift.III })
    }

    // ——— C: ostrzeżenie o zbyt krótkim odpoczynku ———

    @Test fun ostrzega_gdy_do_nastepnej_zmiany_mniej_niz_jedenascie_godzin() {
        val koniec = dt("2026-09-19", "12:00")                      // zostałem po nocce do 12:00
        val nastepna = listOf(dt("2026-09-19", "22:00"))            // wieczorem znowu na zmianę
        assertEquals(10L, PresenceEngine.restBefore(koniec, nastepna))
    }

    @Test fun nie_ostrzega_gdy_odpoczynku_jest_dosc() {
        val koniec = dt("2026-09-20", "06:00")
        assertNull(PresenceEngine.restBefore(koniec, listOf(dt("2026-09-21", "22:00"))))
        assertNull(PresenceEngine.restBefore(koniec, emptyList()))
    }

    // ——— godziny wypisywane na kafelku kalendarza ———

    @Test fun godziny_pobytu_licza_sie_z_zaokraglonych_brzegow() {
        assertEquals(8, PresenceEngine.countedHours(dt("2026-09-14", "22:00"), dt("2026-09-15", "06:00")))
        assertEquals(10, PresenceEngine.countedHours(dt("2026-09-15", "22:00"), dt("2026-09-16", "08:00")))
    }

    @Test fun niekompletny_albo_pusty_pobyt_daje_zero() {
        assertEquals(0, PresenceEngine.countedHours(null, dt("2026-09-15", "06:00")))
        assertEquals(0, PresenceEngine.countedHours(dt("2026-09-15", "06:00"), null))
        assertEquals(0, PresenceEngine.countedHours(dt("2026-09-15", "06:00"), dt("2026-09-15", "06:00")))
        // wyjście przed wejściem — uszkodzony wpis nie może dać ujemnych godzin
        assertEquals(0, PresenceEngine.countedHours(dt("2026-09-15", "08:00"), dt("2026-09-15", "06:00")))
    }
}
