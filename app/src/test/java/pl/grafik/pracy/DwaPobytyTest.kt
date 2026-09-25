package pl.grafik.pracy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test
import pl.grafik.pracy.domain.DayKind
import pl.grafik.pracy.domain.PresenceEngine
import pl.grafik.pracy.domain.PresenceSpan
import pl.grafik.pracy.domain.Shift
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Dwie wizyty w pracy tego samego dnia.
 *
 * Maciej zgłosił 25.09.2026: wykrycie zapisało 11 h i dorzuciło „+3 h po 50 %",
 * choć zmiana trwała 14:00–22:00. Druga wizyta mieściła się w godzinach zmiany,
 * ale norma była już „zajęta" przez pierwszą, więc jej godziny poleciały w całości
 * jako nadgodziny. Czas raz policzony nie może się liczyć drugi raz.
 */
class DwaPobytyTest {

    private val dzien = LocalDate.of(2026, 9, 24)
    private fun o(g: Int, m: Int = 0) = dzien.atTime(g, m)
    private fun pobyt(odG: Int, doG: Int) = PresenceSpan(o(odG), o(doG))

    /** Północ następnego dnia — koniec pobytu, który przechodzi przez dobę. */
    private val polnoc = dzien.plusDays(1).atTime(0, 0)

    // ─── sam rachunek nakładania ─────────────────────────────────

    @Test
    fun bez_wczesniejszych_pobytow_liczy_sie_calosc() {
        val nowe = PresenceEngine.nowaCzesc(o(14), o(22), emptyList())
        assertEquals(o(14) to o(22), nowe)
    }

    @Test
    fun pobyt_w_srodku_juz_policzonego_nie_wnosi_nic() {
        val nowe = PresenceEngine.nowaCzesc(o(16), o(19), listOf(pobyt(14, 22)))
        assertNull("te godziny już policzono", nowe)
    }

    @Test
    fun liczy_sie_tylko_ogon_wystajacy_poza_policzone() {
        val nowe = PresenceEngine.nowaCzesc(o(21), o(23), listOf(pobyt(14, 22)))
        assertEquals(o(22) to o(23), nowe)
    }

    @Test
    fun liczy_sie_tez_poczatek_przed_policzonym() {
        val nowe = PresenceEngine.nowaCzesc(o(12), o(16), listOf(pobyt(14, 22)))
        assertEquals(o(12) to o(14), nowe)
    }

    @Test
    fun pobyt_rozlaczny_liczy_sie_w_calosci() {
        val nowe = PresenceEngine.nowaCzesc(o(6), o(10), listOf(pobyt(14, 22)))
        assertEquals(o(6) to o(10), nowe)
    }

    @Test
    fun przy_dziurze_bierzemy_najdluzszy_wolny_kawalek() {
        val nowe = PresenceEngine.nowaCzesc(
            o(8), o(20), listOf(pobyt(9, 11), pobyt(12, 13))
        )
        // wolne kawałki: 8–9 (1 h), 11–12 (1 h), 13–20 (7 h)
        assertEquals(o(13) to o(20), nowe)
    }

    // ─── cała analiza pobytu ─────────────────────────────────────

    @Test
    fun druga_wizyta_w_godzinach_zmiany_nie_daje_nadgodzin() {
        // przypadek Macieja: pierwszy pobyt pokrył całą zmianę
        val pierwszy = PresenceEngine.analyze(
            dzien, pobyt(14, 22), Shift.II, DayKind.ZWYKLY
        )
        assertEquals(8, pierwszy.countedHours)
        assertEquals(0, pierwszy.otHours)

        val drugi = PresenceEngine.analyze(
            dzien, pobyt(16, 19), Shift.II, DayKind.ZWYKLY,
            normUsed = true,
            policzone = listOf(PresenceSpan(pierwszy.countedFrom, pierwszy.countedTo))
        )
        assertEquals("nic nowego nie przybyło", 0, drugi.countedHours)
        assertEquals("i żadnych nadgodzin", 0, drugi.otHours)
    }

    @Test
    fun zostanie_po_zmianie_nadal_daje_nadgodziny() {
        val pierwszy = PresenceEngine.analyze(
            dzien, pobyt(14, 22), Shift.II, DayKind.ZWYKLY
        )
        val drugi = PresenceEngine.analyze(
            dzien, PresenceSpan(o(22), polnoc), Shift.II, DayKind.ZWYKLY,
            normUsed = true,
            policzone = listOf(PresenceSpan(pierwszy.countedFrom, pierwszy.countedTo))
        )
        assertEquals("dwie godziny po zmianie to prawdziwe nadgodziny", 2, drugi.otHours)
    }

    @Test
    fun czesciowe_nalozenie_liczy_tylko_nowa_czesc() {
        val pierwszy = PresenceEngine.analyze(
            dzien, pobyt(14, 22), Shift.II, DayKind.ZWYKLY
        )
        val drugi = PresenceEngine.analyze(
            dzien, pobyt(21, 23), Shift.II, DayKind.ZWYKLY,
            normUsed = true,
            policzone = listOf(PresenceSpan(pierwszy.countedFrom, pierwszy.countedTo))
        )
        assertEquals("tylko godzina po 22:00", 1, drugi.otHours)
    }

    @Test
    fun suma_dnia_zgadza_sie_z_rzeczywistoscia() {
        // 14:00–22:00, potem wyjście i powrót 16:00–19:00 (np. mignięcie geofence)
        val pierwszy = PresenceEngine.analyze(dzien, pobyt(14, 22), Shift.II, DayKind.ZWYKLY)
        val drugi = PresenceEngine.analyze(
            dzien, pobyt(16, 19), Shift.II, DayKind.ZWYKLY,
            normUsed = true,
            policzone = listOf(PresenceSpan(pierwszy.countedFrom, pierwszy.countedTo))
        )
        assertEquals("razem osiem godzin, nie jedenaście",
            8, pierwszy.countedHours + drugi.countedHours)
    }

    // ─── sklejanie krótkiego wyjścia ────────────────────────────

    @Test
    fun minutowa_przerwa_to_ten_sam_pobyt() {
        // dokładnie przypadek Macieja: wpisy 13:45–18:25 i 18:26–22:05
        assertTrue(
            PresenceEngine.czyScalic(o(18, 25), o(18, 26), 30)
        )
    }

    @Test
    fun przerwa_w_granicach_ustawienia_sie_sklei() {
        assertTrue(PresenceEngine.czyScalic(o(18), o(18, 30), 30))
        assertTrue(PresenceEngine.czyScalic(o(18), o(18), 30))
    }

    @Test
    fun dluzsza_przerwa_to_juz_osobny_pobyt() {
        assertFalse("31 minut przy progu 30", PresenceEngine.czyScalic(o(18), o(18, 31), 30))
        assertFalse("kilka godzin to na pewno osobny pobyt",
            PresenceEngine.czyScalic(o(10), o(14), 30))
    }

    @Test
    fun wejscie_przed_koncem_poprzedniego_nie_jest_powrotem() {
        assertFalse(PresenceEngine.czyScalic(o(18), o(17), 30))
    }

    @Test
    fun sklejony_pobyt_liczy_sie_jak_jeden() {
        // 13:45–18:25 + 18:26–22:05 sklejone w 13:45–22:05 to jedna zmiana, bez nadgodzin
        val sklejony = PresenceEngine.analyze(
            dzien, PresenceSpan(o(13, 45), o(22, 5)), Shift.II, DayKind.ZWYKLY
        )
        assertEquals(8, sklejony.countedHours)
        assertEquals("żadnych wymyślonych nadgodzin", 0, sklejony.otHours)
    }

    @Test
    fun pierwszy_pobyt_dalej_dziala_jak_dotad() {
        val wynik = PresenceEngine.analyze(dzien, pobyt(14, 23), Shift.II, DayKind.ZWYKLY)
        assertEquals(9, wynik.countedHours)
        assertEquals(1, wynik.otHours)
    }
}
