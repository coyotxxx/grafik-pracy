package pl.grafik.pracy.nowy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.grafik.pracy.domain.DayEntry
import pl.grafik.pracy.domain.Shift
import pl.grafik.pracy.nowy.ekrany.Pozycja
import pl.grafik.pracy.nowy.ekrany.odmianaZapisanych
import pl.grafik.pracy.nowy.ekrany.opisZbioru
import pl.grafik.pracy.nowy.ekrany.zPodzialemNaMiesiace
import java.time.LocalDate
import java.time.YearMonth

/**
 * Lista notatek z lat: podział na miesiące i podpis nad listą.
 *
 * Po kilku latach wpisów nagłówek miesiąca musi trafiać dokładnie tam, gdzie zmienia
 * się miesiąc, a licznik przy nim pokazywać liczbę wpisów w TYM miesiącu, nie w całości.
 */
class ListaNotatekTest {

    private fun notatka(data: String, tekst: String = "coś", zdjecie: String? = null) =
        DayEntry(
            date = LocalDate.parse(data), shift = Shift.I,
            note = tekst, notePhoto = zdjecie
        )

    // ─── podział na miesiące ─────────────────────────────

    @Test
    fun pusta_lista_nie_ma_zadnych_naglowkow() {
        assertTrue(zPodzialemNaMiesiace(emptyList()).isEmpty())
    }

    @Test
    fun jeden_miesiac_to_jeden_naglowek() {
        val p = zPodzialemNaMiesiace(
            listOf(notatka("2026-09-29"), notatka("2026-09-24"), notatka("2026-09-07"))
        )
        assertEquals("nagłówek plus trzy wpisy", 4, p.size)
        assertEquals(YearMonth.of(2026, 9), (p[0] as Pozycja.Miesiac).ym)
        assertEquals("licznik liczy wpisy miesiąca", 3, (p[0] as Pozycja.Miesiac).ile)
        assertTrue(p.drop(1).all { it is Pozycja.Dzien })
    }

    @Test
    fun naglowek_wchodzi_przy_kazdej_zmianie_miesiaca() {
        val p = zPodzialemNaMiesiace(
            listOf(
                notatka("2026-09-29"), notatka("2026-09-01"),
                notatka("2026-08-22"),
                notatka("2025-12-24"), notatka("2025-12-02")
            )
        )
        val naglowki = p.filterIsInstance<Pozycja.Miesiac>()
        assertEquals(3, naglowki.size)
        assertEquals(YearMonth.of(2026, 9) to 2, naglowki[0].ym to naglowki[0].ile)
        assertEquals(YearMonth.of(2026, 8) to 1, naglowki[1].ym to naglowki[1].ile)
        assertEquals(YearMonth.of(2025, 12) to 2, naglowki[2].ym to naglowki[2].ile)
    }

    @Test
    fun ten_sam_miesiac_w_roznych_latach_to_osobne_naglowki() {
        val p = zPodzialemNaMiesiace(listOf(notatka("2026-09-10"), notatka("2025-09-10")))
        val naglowki = p.filterIsInstance<Pozycja.Miesiac>()
        assertEquals(2, naglowki.size)
        assertEquals(YearMonth.of(2026, 9), naglowki[0].ym)
        assertEquals(YearMonth.of(2025, 9), naglowki[1].ym)
        assertEquals(1, naglowki[0].ile)
    }

    @Test
    fun kolejnosc_wpisow_zostaje_taka_jak_przyszla() {
        // Sortowanie robi ekran (od najnowszych albo od najstarszych) — podział go nie zmienia.
        val p = zPodzialemNaMiesiace(
            listOf(notatka("2024-03-15"), notatka("2025-12-02"), notatka("2026-09-29"))
        )
        val daty = p.filterIsInstance<Pozycja.Dzien>().map { it.wpis.date.toString() }
        assertEquals(listOf("2024-03-15", "2025-12-02", "2026-09-29"), daty)
    }

    // ─── podpis nad listą ────────────────────────────────

    @Test
    fun odmiana_po_polsku() {
        assertEquals("zapisana", odmianaZapisanych(1))
        assertEquals("zapisane", odmianaZapisanych(2))
        assertEquals("zapisane", odmianaZapisanych(4))
        assertEquals("zapisanych", odmianaZapisanych(5))
        assertEquals("zapisanych", odmianaZapisanych(11))
        // 12, 13, 14 to wyjątek — mimo końcówki 2-4 mówi się „zapisanych"
        assertEquals("zapisanych", odmianaZapisanych(12))
        assertEquals("zapisanych", odmianaZapisanych(13))
        assertEquals("zapisanych", odmianaZapisanych(14))
        assertEquals("zapisane", odmianaZapisanych(22))
        assertEquals("zapisane", odmianaZapisanych(23))
        assertEquals("zapisanych", odmianaZapisanych(25))
        assertEquals("zapisanych", odmianaZapisanych(111))
        assertEquals("zapisane", odmianaZapisanych(122))
    }

    @Test
    fun podpis_mowi_ile_i_ile_ze_zdjeciem() {
        assertEquals("jeszcze nic nie zapisano", opisZbioru(0, 0))
        assertEquals("1 zapisana", opisZbioru(1, 0))
        assertEquals("10 zapisanych · 2 ze zdjęciem", opisZbioru(10, 2))
        assertEquals("3 zapisane", opisZbioru(3, 0))
    }
}
