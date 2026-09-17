package pl.grafik.pracy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.grafik.pracy.domain.PlanPowiadomien
import pl.grafik.pracy.domain.Shift
import java.time.LocalDate

/**
 * Powiadomienia warunkowe: odzywają się tylko wtedy, gdy grafik albo kalendarz
 * dają po temu powód. Reszta czasu ma być cisza.
 */
class WarunkoweTest {

    // ─── zmiana brygady ──────────────────────────────────────────

    private val niedziela = LocalDate.of(2026, 9, 20)

    @Test
    fun w_niedziele_mowimy_o_zmianie_dopiero_gdy_jutro_jest_inna() {
        val tydzienNaDrugiej = List(7) { Shift.II }
        val komunikat = PlanPowiadomien.zmianaBrygady(niedziela, tydzienNaDrugiej, Shift.III)
        assertNotNull(komunikat)
        assertTrue(komunikat!!, komunikat.contains("nocna"))
        assertTrue(komunikat, komunikat.contains("II zmiana"))
    }

    @Test
    fun ta_sama_zmiana_od_jutra_nie_jest_zadna_wiadomoscia() {
        assertNull(PlanPowiadomien.zmianaBrygady(niedziela, List(7) { Shift.I }, Shift.I))
    }

    @Test
    fun w_inny_dzien_tygodnia_milczymy() {
        val poniedzialek = LocalDate.of(2026, 9, 21)
        assertNull(PlanPowiadomien.zmianaBrygady(poniedzialek, List(7) { Shift.I }, Shift.III))
    }

    @Test
    fun dzien_wolny_od_jutra_to_nie_zmiana_brygady() {
        assertNull(PlanPowiadomien.zmianaBrygady(niedziela, List(7) { Shift.I }, Shift.W5))
        assertNull(PlanPowiadomien.zmianaBrygady(niedziela, List(7) { Shift.I }, null))
    }

    @Test
    fun po_samych_dniach_wolnych_nie_ma_z_czym_porownac() {
        assertNull(PlanPowiadomien.zmianaBrygady(niedziela, List(7) { Shift.W5 }, Shift.I))
    }

    @Test
    fun porownujemy_z_ostatnia_zmiana_a_nie_z_pierwsza() {
        // tydzień zaczął się rannymi, skończył nockami — jutro ranna, więc jest o czym mówić
        val tydzien = listOf(Shift.I, Shift.I, Shift.W5, Shift.III, Shift.III, Shift.III, Shift.W5)
        val komunikat = PlanPowiadomien.zmianaBrygady(niedziela, tydzien, Shift.I)
        assertNotNull(komunikat)
        assertTrue(komunikat!!, komunikat.contains("nocna"))
    }

    // ─── limit nadgodzin ─────────────────────────────────────────

    @Test
    fun ostrzegamy_po_osiemdziesieciu_procentach_limitu() {
        assertNull("79 % to jeszcze za wcześnie", PlanPowiadomien.limitNadgodzin(79, 100))
        val komunikat = PlanPowiadomien.limitNadgodzin(80, 100)
        assertNotNull(komunikat)
        assertTrue(komunikat!!, komunikat.contains("zostało 20 h"))
    }

    @Test
    fun po_wyczerpaniu_limitu_mowimy_to_wprost() {
        val komunikat = PlanPowiadomien.limitNadgodzin(100, 100)
        assertNotNull(komunikat)
        assertTrue(komunikat!!, komunikat.contains("wyczerpany"))
        val ponad = PlanPowiadomien.limitNadgodzin(112, 100)
        assertNotNull(ponad)
        assertTrue(ponad!!, ponad.contains("wyczerpany"))
    }

    @Test
    fun bez_limitu_nie_ma_o_czym_ostrzegac() {
        assertNull(PlanPowiadomien.limitNadgodzin(50, 0))
        assertNull(PlanPowiadomien.limitNadgodzin(0, 0))
    }

    @Test
    fun prog_liczy_sie_z_prawdziwego_limitu_okresu() {
        // limit kwartału Macieja to 94 h — próg wypada na 76 h
        assertNull(PlanPowiadomien.limitNadgodzin(75, 94))
        assertNotNull(PlanPowiadomien.limitNadgodzin(76, 94))
    }

    // ─── zaległy urlop ───────────────────────────────────────────

    @Test
    fun o_zaleglym_urlopie_przypominamy_w_sierpniu() {
        val sierpien = LocalDate.of(2026, 8, 3)
        val komunikat = PlanPowiadomien.zaleglyUrlop(sierpien, 4)
        assertNotNull(komunikat)
        assertTrue(komunikat!!, komunikat.contains("4 dni"))
        assertTrue(komunikat, komunikat.contains("30 września"))
    }

    @Test
    fun w_innych_miesiacach_i_bez_zaleglosci_milczymy() {
        assertNull(PlanPowiadomien.zaleglyUrlop(LocalDate.of(2026, 7, 31), 4))
        assertNull(PlanPowiadomien.zaleglyUrlop(LocalDate.of(2026, 9, 1), 4))
        assertNull(PlanPowiadomien.zaleglyUrlop(LocalDate.of(2026, 8, 3), 0))
    }

    @Test
    fun liczymy_ile_dni_zostalo_do_terminu() {
        // 1 sierpnia do 30 września to 60 dni
        val komunikat = PlanPowiadomien.zaleglyUrlop(LocalDate.of(2026, 8, 1), 2)
        assertNotNull(komunikat)
        assertTrue(komunikat!!, komunikat.contains("60 dni"))
    }

    @Test
    fun jeden_dzien_odmienia_sie_poprawnie() {
        val komunikat = PlanPowiadomien.zaleglyUrlop(LocalDate.of(2026, 8, 10), 1)
        assertNotNull(komunikat)
        assertTrue(komunikat!!, komunikat.contains("1 dzień urlopu"))
    }

    @Test
    fun prog_limitu_i_miesiac_zaleglego_sa_tam_gdzie_mowi_makieta() {
        assertEquals(0.80, PlanPowiadomien.PROG_LIMITU, 0.001)
        assertEquals(8, PlanPowiadomien.MIESIAC_ZALEGLEGO)
    }
}
