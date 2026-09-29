package pl.grafik.pracy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.grafik.pracy.domain.Waznosc
import java.time.LocalDate

/**
 * Termin ważności notatki — kara porządkowa, badania, szkolenie.
 *
 * Kara zaciera się po roku nienagannej pracy (art. 113 § 1 KP). Maciej chciał
 * widzieć, do kiedy kara obowiązuje i ile jeszcze zostało.
 */
class WaznoscTest {

    private val dzis = LocalDate.of(2026, 9, 29)

    // ─── stan ────────────────────────────────────────────

    @Test
    fun termin_w_przyszlosci_biegnie() {
        val s = Waznosc.stan(LocalDate.of(2027, 9, 29), dzis)
        assertTrue(s is Waznosc.Stan.Biegnie)
        assertEquals(365L, (s as Waznosc.Stan.Biegnie).dni)
    }

    @Test
    fun ostatni_dzien_jeszcze_sie_liczy() {
        // Kara obowiązuje do końca swojego ostatniego dnia, nie do jego początku.
        val s = Waznosc.stan(dzis, dzis)
        assertTrue(s is Waznosc.Stan.Biegnie)
        assertEquals(0L, (s as Waznosc.Stan.Biegnie).dni)
    }

    @Test
    fun dzien_po_terminie_to_juz_zatarcie() {
        assertTrue(Waznosc.stan(dzis.minusDays(1), dzis) is Waznosc.Stan.Minal)
    }

    @Test
    fun blisko_konca_od_szescdziesieciu_dni() {
        val daleko = Waznosc.stan(dzis.plusDays(61), dzis) as Waznosc.Stan.Biegnie
        val blisko = Waznosc.stan(dzis.plusDays(60), dzis) as Waznosc.Stan.Biegnie
        assertEquals(false, daleko.blisko)
        assertEquals(true, blisko.blisko)
    }

    // ─── ile zostało, po ludzku ─────────────────────────

    @Test
    fun daleki_termin_liczymy_w_miesiacach() {
        // „365 dni" czyta się gorzej niż „12 miesięcy".
        assertEquals("zostało 12 miesięcy", Waznosc.ileZostalo(LocalDate.of(2027, 9, 29), dzis))
        assertEquals("zostało 6 miesięcy", Waznosc.ileZostalo(LocalDate.of(2027, 3, 29), dzis))
    }

    @Test
    fun bliski_termin_liczymy_w_dniach() {
        assertEquals("zostało 36 dni", Waznosc.ileZostalo(dzis.plusDays(36), dzis))
        assertEquals("zostało 60 dni", Waznosc.ileZostalo(dzis.plusDays(60), dzis))
    }

    @Test
    fun ostatnie_dni_mowimy_osobno() {
        assertEquals("ostatni dzień", Waznosc.ileZostalo(dzis, dzis))
        assertEquals("został 1 dzień", Waznosc.ileZostalo(dzis.plusDays(1), dzis))
    }

    @Test
    fun po_terminie_nie_ma_czego_odliczac() {
        assertNull(Waznosc.ileZostalo(dzis.minusDays(1), dzis))
    }

    @Test
    fun odmiana_miesiecy_po_polsku() {
        // Jeden miesiąc to już poniżej progu 60 dni, więc liczy się w dniach —
        // „zostało 31 dni" jest tu uczciwsze niż „zostało 1 miesiąc".
        assertEquals("zostało 31 dni", Waznosc.ileZostalo(dzis.plusMonths(1).plusDays(1), dzis))
        assertEquals("zostało 3 miesiące", Waznosc.ileZostalo(dzis.plusMonths(3), dzis))
        assertEquals("zostało 5 miesięcy", Waznosc.ileZostalo(dzis.plusMonths(5), dzis))
        assertEquals("zostało 12 miesięcy", Waznosc.ileZostalo(dzis.plusMonths(12), dzis))
        assertEquals("zostało 22 miesiące", Waznosc.ileZostalo(dzis.plusMonths(22), dzis))
    }

    @Test
    fun domyslny_termin_kary_to_rok() {
        // art. 113 § 1 KP — po roku nienagannej pracy kara jest niebyła.
        assertEquals(12, Waznosc.MIESIECY_KARA)
        val termin = dzis.plusMonths(Waznosc.MIESIECY_KARA.toLong())
        assertEquals(LocalDate.of(2027, 9, 29), termin)
    }

    @Test
    fun skrot_pokazuje_dokladnie_to_co_wybrano() {
        // Zgłoszone przez test: 29.09 + 5 miesięcy to 28.02, czyli o dzień za mało
        // do pełnych pięciu. Odliczanie zaokrąglamy z dni, żeby zaraz po wybraniu
        // „6 miesięcy" nie pisało „zostało 5 miesięcy".
        listOf(6, 12, 24).forEach { miesiecy ->
            val termin = dzis.plusMonths(miesiecy.toLong())
            assertEquals(
                "skrót $miesiecy mies. ma pokazać tyle samo",
                "zostało $miesiecy ${if (miesiecy == 24) "miesiące" else "miesięcy"}",
                Waznosc.ileZostalo(termin, dzis)
            )
        }
    }

    @Test
    fun rok_przestepny_nie_psuje_terminu() {
        // 29 lutego + 12 miesięcy w roku nieprzestępnym — java cofa na 28.
        val kara = LocalDate.of(2028, 2, 29)
        assertEquals(LocalDate.of(2029, 2, 28), kara.plusMonths(12))
    }
}
