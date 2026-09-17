package pl.grafik.pracy.nowy

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test
import pl.grafik.pracy.nowy.theme.Dim
import pl.grafik.pracy.nowy.ui.MIN_DOL
import pl.grafik.pracy.nowy.ui.ODDECH_GORA
import pl.grafik.pracy.nowy.ui.krawedz

/**
 * Makiety zakładają telefon z gestami: 44 dp u góry i 18 dp na dole akurat pokrywają
 * paski systemowe. Na telefonie z wyższym paskiem stanu i trzema przyciskami te liczby
 * są za małe i treść wchodzi pod zegarek albo pod przyciski systemowe — dlatego odstęp
 * przy krawędzi bierze się z insetu, a wartość z makiety jest tylko dolną granicą.
 */
class KrawedzieTest {

    @Test
    fun telefon_z_gestami_dostaje_odstepy_z_makiety() {
        // pasek stanu 24 dp — makietowe 44 dp i tak jest większe
        assertEquals(Dim.topSafe, krawedz(24.dp, Dim.topSafe, ODDECH_GORA))
        // gesty nie zajmują miejsca w tym emulatorze — zostaje 18 dp z makiety
        assertEquals(MIN_DOL, krawedz(0.dp, MIN_DOL))
    }

    @Test
    fun wysoki_pasek_stanu_odsuwa_tresc_nizej() {
        // telefon Macieja: pasek stanu 41 dp, więc 44 dp z makiety zostawiałoby 3 dp
        assertEquals(51.dp, krawedz(41.dp, Dim.topSafe, ODDECH_GORA))
    }

    @Test
    fun trzy_przyciski_dostaja_tyle_ile_zajmuja() {
        // pasek trzech przycisków to około 48 dp — musi zmieścić się w całości
        assertEquals(48.dp, krawedz(48.dp, MIN_DOL))
    }

    @Test
    fun panel_przy_krawedzi_nie_dodaje_nic_gdy_nawigacji_nie_ma() {
        // panel narzędzi i arkusz dnia mają własny odstęp, więc ich minimum to zero
        assertEquals(0.dp, krawedz(0.dp, 0.dp))
        assertEquals(48.dp, krawedz(48.dp, 0.dp))
    }
}
