package pl.grafik.pracy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.grafik.pracy.domain.KalkulatorWyplaty
import pl.grafik.pracy.domain.StawkiCfg

/**
 * Droga od brutto do kwoty na koncie. Parametry z odcinka Macieja:
 * próg 12 %, koszty 300 zł, ulga 300 zł.
 */
class NettoTest {

    private val stawki = StawkiCfg(
        zasadnicza = 7950.0, dodatekNocny = 10.01,
        kosztyUzyskania = 300.0, ulgaPodatkowa = 300.0
    )

    @Test
    fun sierpien_2026_zgadza_sie_z_odcinkiem_w_granicach_procenta() {
        // odcinek: naliczenia 11 300,48; składki 1 561,57; zdrowotna 884,56; zaliczka 898
        val n = KalkulatorWyplaty.netto(11300.48, stawki)
        assertEquals(1549.29, n.spoleczne, 1.0)
        assertEquals(877.61, n.zdrowotna, 1.0)
        assertEquals(834.0, n.zaliczka, 1.0)
        // na odcinku po samych składkach i podatku zostaje 7 956 zł
        assertTrue("na rękę ${n.naReke}", Math.abs(n.naReke - 7956.35) < 90.0)
    }

    @Test
    fun stale_potracenia_schodza_na_koncu() {
        val bez = KalkulatorWyplaty.netto(11300.48, stawki)
        val z = KalkulatorWyplaty.netto(11300.48, stawki.copy(stalePotracenia = 490.66))
        assertEquals(bez.naReke - 490.66, z.naReke, 0.01)
    }

    @Test
    fun ulga_obniza_zaliczke_o_swoja_wartosc() {
        val zUlga = KalkulatorWyplaty.netto(8000.0, stawki)
        val bezUlgi = KalkulatorWyplaty.netto(8000.0, stawki.copy(ulgaPodatkowa = 0.0))
        assertEquals(300.0, bezUlgi.zaliczka - zUlga.zaliczka, 1.0)
    }

    @Test
    fun zaliczka_nie_schodzi_ponizej_zera() {
        // przy niskiej podstawie ulga zjada cały podatek
        val n = KalkulatorWyplaty.netto(1000.0, stawki)
        assertEquals(0.0, n.zaliczka, 0.01)
    }

    @Test
    fun skladki_licza_sie_od_pelnego_brutto() {
        val n = KalkulatorWyplaty.netto(10000.0, stawki)
        assertEquals(1371.0, n.spoleczne, 0.01)          // 13,71 %
        assertEquals(776.61, n.zdrowotna, 0.01)          // 9 % z 8 629
    }
}
