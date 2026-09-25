package pl.grafik.pracy.nowy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pl.grafik.pracy.nowy.ui.miesiacIDzien

/**
 * Data urodzin zapisana w kontaktach telefonu.
 *
 * Android trzyma ją jako zwykły tekst i nie narzuca jednego zapisu — zależy od
 * konta, z którego kontakt pochodzi. Rok pomijamy: uroczystość wraca co roku.
 */
class UrodzinyZKontaktowTest {

    @Test
    fun pelna_data_z_rokiem() {
        assertEquals(3 to 24, miesiacIDzien("1985-03-24"))
        assertEquals(12 to 5, miesiacIDzien("2001-12-05"))
    }

    @Test
    fun data_bez_roku() {
        // Kontakt bez podanego rocznika — Google zapisuje go z dwoma myślnikami.
        assertEquals(3 to 24, miesiacIDzien("--03-24"))
        assertEquals(1 to 1, miesiacIDzien("--01-01"))
    }

    @Test
    fun data_ze_znacznikiem_czasu() {
        assertEquals(3 to 24, miesiacIDzien("1985-03-24T00:00:00.000Z"))
    }

    @Test
    fun biale_znaki_nie_przeszkadzaja() {
        assertEquals(3 to 24, miesiacIDzien("  1985-03-24  "))
    }

    @Test
    fun brak_daty_to_brak_urodzin() {
        assertNull(miesiacIDzien(null))
        assertNull(miesiacIDzien(""))
        assertNull(miesiacIDzien("   "))
    }

    @Test
    fun zapis_ktorego_nie_rozumiemy_odpada_po_cichu() {
        assertNull("nie zgadujemy przy zapisie amerykańskim", miesiacIDzien("03/24/1985"))
        assertNull(miesiacIDzien("kiedyś w marcu"))
        assertNull(miesiacIDzien("1985"))
    }

    @Test
    fun liczby_poza_kalendarzem_odpadaja() {
        assertNull(miesiacIDzien("1985-13-24"))
        assertNull(miesiacIDzien("1985-03-45"))
        assertNull(miesiacIDzien("--00-10"))
    }
}
