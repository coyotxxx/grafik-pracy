package pl.grafik.pracy.nowy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pl.grafik.pracy.nowy.ekrany.dataUroczystosci
import java.time.LocalDate

/**
 * Data uroczystości wpisywana dwoma polami.
 *
 * Pola przyjmują dwie cyfry, więc da się wystukać 55 albo 31 lutego. Wcześniej
 * przycisk „Dodaj" po prostu nic nie robił — bez słowa wyjaśnienia. Teraz
 * niemożliwa data daje `null`, a formularz mówi o tym wprost.
 */
class DataUroczystosciTest {

    @Test
    fun zwykla_data_przechodzi() {
        assertEquals(LocalDate.of(2026, 9, 24), dataUroczystosci("24", "9", 2026))
        assertEquals(LocalDate.of(2026, 12, 5), dataUroczystosci("5", "12", 2026))
    }

    @Test
    fun zera_wiodace_nie_przeszkadzaja() {
        assertEquals(LocalDate.of(2026, 3, 8), dataUroczystosci("08", "03", 2026))
    }

    @Test
    fun dzien_ktorego_nie_ma_w_miesiacu_odpada() {
        assertNull("31 lutego nie istnieje", dataUroczystosci("31", "2", 2026))
        assertNull("31 kwietnia też nie", dataUroczystosci("31", "4", 2026))
    }

    @Test
    fun liczby_poza_kalendarzem_odpadaja() {
        assertNull(dataUroczystosci("55", "9", 2026))
        assertNull(dataUroczystosci("24", "13", 2026))
        assertNull(dataUroczystosci("0", "9", 2026))
        assertNull(dataUroczystosci("24", "0", 2026))
    }

    @Test
    fun puste_pola_to_jeszcze_nie_blad() {
        assertNull(dataUroczystosci("", "9", 2026))
        assertNull(dataUroczystosci("24", "", 2026))
        assertNull(dataUroczystosci("", "", 2026))
    }

    @Test
    fun dwudziesty_dziewiaty_lutego_trafia_w_rok_przestepny() {
        // 2026 nie jest przestępny — data ma wskoczyć na najbliższy, który jest.
        assertEquals(LocalDate.of(2028, 2, 29), dataUroczystosci("29", "2", 2026))
        assertEquals(LocalDate.of(2028, 2, 29), dataUroczystosci("29", "2", 2028))
    }

    @Test
    fun luty_28_dziala_w_kazdym_roku() {
        assertEquals(LocalDate.of(2026, 2, 28), dataUroczystosci("28", "2", 2026))
    }
}
