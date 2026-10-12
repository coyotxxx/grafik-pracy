package pl.grafik.pracy

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.grafik.pracy.domain.DayEntry
import pl.grafik.pracy.domain.Shift
import java.time.LocalDate

/**
 * Odpracowanie — pracuję w jednym dniu, żeby mieć wolne w innym.
 *
 * Maciej przepracował noc z 10 na 11.10.2026, a aplikacja zapisała to jako 8 h
 * nadgodzin po 100 %. To nie były nadgodziny: odrabiał 21 grudnia, który zakład
 * wyłącza z pracy. Nadgodziny to dodatkowa zapłata, odpracowanie to przesunięcie
 * dnia — pensja bez zmian.
 *
 * Rozpoznajemy to po kierunku powiązania: dzień PRACY wskazujący inny dzień
 * odrabia go, dzień WOLNY wskazujący inny — sam został odrobiony.
 */
class OdpracowanieTest {

    private val sobota = LocalDate.of(2026, 10, 10)
    private val grudzien = LocalDate.of(2026, 12, 21)

    @Test
    fun dzien_pracy_ze_wskazaniem_to_odpracowanie() {
        val d = DayEntry(date = sobota, shift = Shift.III, dwnFor = grudzien)
        assertTrue(d.czyOdpracowanie)
        assertFalse("to nie jest dzień odrobiony, tylko odrabiający", d.czyOdrobiony)
    }

    @Test
    fun dzien_wolny_ze_wskazaniem_to_dzien_odrobiony() {
        val d = DayEntry(date = grudzien, shift = Shift.W5, dwnFor = sobota)
        assertTrue(d.czyOdrobiony)
        assertFalse(d.czyOdpracowanie)
    }

    @Test
    fun zwykla_zmiana_bez_wskazania_to_nie_odpracowanie() {
        val d = DayEntry(date = sobota, shift = Shift.III)
        assertFalse(d.czyOdpracowanie)
        assertFalse(d.czyOdrobiony)
    }

    @Test
    fun zwykly_dzien_wolny_to_nie_dzien_odrobiony() {
        val d = DayEntry(date = grudzien, shift = Shift.W5)
        assertFalse(d.czyOdrobiony)
        assertFalse(d.czyOdpracowanie)
    }

    @Test
    fun urlop_ze_wskazaniem_nie_jest_odpracowaniem() {
        // Urlop nie jest dniem pracy, więc niczego nie odrabia.
        val d = DayEntry(date = grudzien, shift = Shift.URLOP, dwnFor = sobota)
        assertFalse(d.czyOdpracowanie)
        assertTrue("ale sam może być oznaczony jako odrobiony", d.czyOdrobiony)
    }

    @Test
    fun dzien_bez_zmiany_niczego_nie_odrabia() {
        // Pusty dzień: nie wiadomo nawet, czy to praca.
        val d = DayEntry(date = sobota, dwnFor = grudzien)
        assertFalse(d.czyOdpracowanie)
        assertFalse(d.czyOdrobiony)
    }

    @Test
    fun odpracowanie_liczy_sie_jak_zwykla_dniowka() {
        // Osiem godzin wchodzi do wymiaru, ale bez nadgodzin — pensja się nie zmienia.
        val d = DayEntry(date = sobota, shift = Shift.III, dwnFor = grudzien, otHours = 0)
        assertTrue(8 == d.workedHours)
    }

    @Test
    fun dzien_odrobiony_nie_wnosi_godzin() {
        val d = DayEntry(date = grudzien, shift = Shift.W5, dwnFor = sobota)
        assertTrue(0 == d.workedHours)
    }
}
