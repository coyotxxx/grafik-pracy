package pl.grafik.pracy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.grafik.pracy.domain.DayKind
import pl.grafik.pracy.domain.Holidays
import java.time.LocalDate

/**
 * Dni ustawowo wolne od pracy w Polsce — to one są w kalendarzu zaznaczone na czerwono
 * i one obniżają wymiar czasu pracy.
 *
 * Połowa z nich chodzi za Wielkanocą, więc lista musi się zgadzać w każdym roku,
 * a nie tylko w tym jednym, na którym akurat patrzyliśmy.
 */
class SwietaTest {

    @Test
    fun rok_ma_czternascie_dni_ustawowo_wolnych() {
        // 13 świąt „od zawsze" plus Wigilia, wolna od 2025
        assertEquals(14, Holidays.all(2026).size)
        assertEquals(14, Holidays.all(2027).size)
    }

    @Test
    fun przed_2025_wigilia_nie_byla_wolna() {
        assertEquals(13, Holidays.all(2024).size)
        assertFalse(Holidays.isHoliday(LocalDate.of(2024, 12, 24)))
        assertTrue(Holidays.isHoliday(LocalDate.of(2025, 12, 24)))
    }

    @Test
    fun swieta_stale_sa_na_swoich_datach() {
        val rok = 2026
        listOf(
            LocalDate.of(rok, 1, 1) to "Nowy Rok",
            LocalDate.of(rok, 1, 6) to "Trzech Króli",
            LocalDate.of(rok, 5, 1) to "Święto Pracy",
            LocalDate.of(rok, 5, 3) to "Święto Konstytucji 3 Maja",
            LocalDate.of(rok, 8, 15) to "Wniebowzięcie NMP",
            LocalDate.of(rok, 11, 1) to "Wszystkich Świętych",
            LocalDate.of(rok, 11, 11) to "Święto Niepodległości",
            LocalDate.of(rok, 12, 24) to "Wigilia",
            LocalDate.of(rok, 12, 25) to "Boże Narodzenie",
            LocalDate.of(rok, 12, 26) to "Drugi dzień Świąt"
        ).forEach { (data, nazwa) ->
            assertEquals(nazwa, Holidays.nameOf(data))
        }
    }

    @Test
    fun wielkanoc_wypada_tam_gdzie_powinna() {
        // daty sprawdzone z kalendarzem
        assertEquals(LocalDate.of(2026, 4, 5), niedzielaWielkanocna(2026))
        assertEquals(LocalDate.of(2027, 3, 28), niedzielaWielkanocna(2027))
        assertEquals(LocalDate.of(2025, 4, 20), niedzielaWielkanocna(2025))
        assertEquals(LocalDate.of(2024, 3, 31), niedzielaWielkanocna(2024))
    }

    @Test
    fun swieta_ruchome_chodza_za_wielkanoca() {
        val w = niedzielaWielkanocna(2026)
        assertEquals("Poniedziałek Wielkanocny", Holidays.nameOf(w.plusDays(1)))
        assertEquals("Zesłanie Ducha Świętego", Holidays.nameOf(w.plusDays(49)))
        assertEquals("Boże Ciało", Holidays.nameOf(w.plusDays(60)))
    }

    @Test
    fun zwykly_dzien_nie_jest_swietem() {
        assertNull(Holidays.nameOf(LocalDate.of(2026, 9, 18)))
        assertFalse(Holidays.isHoliday(LocalDate.of(2026, 9, 18)))
        assertEquals(DayKind.ZWYKLY, Holidays.kindOf(LocalDate.of(2026, 9, 18)))
    }

    @Test
    fun swieto_ma_pierwszenstwo_przed_dniem_tygodnia() {
        // 1 listopada 2026 to niedziela — ale liczy się jako święto
        val wszystkichSwietych = LocalDate.of(2026, 11, 1)
        assertEquals(7, wszystkichSwietych.dayOfWeek.value)
        assertEquals(DayKind.SWIETO, Holidays.kindOf(wszystkichSwietych))
    }

    @Test
    fun wrzesien_nie_ma_zadnego_swieta() {
        // stąd wzięło się zgłoszenie: we wrześniu nie było czego pokazać
        val wrzesien = (1..30).map { LocalDate.of(2026, 9, it) }
        assertTrue(wrzesien.none { Holidays.isHoliday(it) })
    }

    /** Niedziela wielkanocna z listy świąt — sama w sobie też jest dniem wolnym. */
    private fun niedzielaWielkanocna(rok: Int): LocalDate =
        Holidays.all(rok).entries.first { it.value == "Wielkanoc" }.key
}
