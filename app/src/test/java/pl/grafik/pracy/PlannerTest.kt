package pl.grafik.pracy

import org.junit.Assert.*
import org.junit.Test
import pl.grafik.pracy.domain.*
import java.time.DayOfWeek
import java.time.LocalDate

class PlannerTest {

    /** Klasyczny układ biurowy: pon–pt praca, weekend wolny. */
    private fun biuro(d: LocalDate) = d.dayOfWeek == DayOfWeek.SATURDAY || d.dayOfWeek == DayOfWeek.SUNDAY

    @Test
    fun jeden_dzien_urlopu_w_piatek_daje_trzy_dni_wolnego() {
        val p = VacationPlanner.zaproponuj(
            LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 8),
            wolny = { biuro(it) || it == LocalDate.of(2026, 3, 5) },   // czwartek wolny
            swieto = { false }, budzet = 26
        )
        val piatek = p.firstOrNull { it.urlop == listOf(LocalDate.of(2026, 3, 6)) }
        assertNotNull("piątek między czwartkiem a weekendem musi być propozycją", piatek)
        assertEquals(4, piatek!!.dlugosc)     // czw + pt + sob + nd
        assertEquals(1, piatek.koszt)
    }

    @Test
    fun swieto_w_srodku_tygodnia_daje_najlepszy_interes() {
        // 3 maja 2026 to niedziela; bierzemy 1 maja 2026 (piątek, święto)
        val swieta = setOf(LocalDate.of(2026, 5, 1))
        val p = VacationPlanner.zaproponuj(
            LocalDate.of(2026, 4, 25), LocalDate.of(2026, 5, 10),
            wolny = { biuro(it) || it in swieta },
            swieto = { it in swieta }, budzet = 26
        )
        assertTrue("powinna być jakaś propozycja", p.isNotEmpty())
        assertTrue("najlepsza musi się wyraźnie opłacać", p.first().oplacalnosc >= 1.6)
    }

    @Test
    fun nie_proponujemy_wiecej_niz_zostalo_urlopu() {
        val p = VacationPlanner.zaproponuj(
            LocalDate.of(2026, 3, 1), LocalDate.of(2026, 6, 30),
            wolny = { biuro(it) }, swieto = { false }, budzet = 2
        )
        assertTrue(p.all { it.koszt <= 2 })
    }

    @Test
    fun przy_zerowym_budzecie_nie_ma_propozycji() {
        val p = VacationPlanner.zaproponuj(
            LocalDate.of(2026, 3, 1), LocalDate.of(2026, 6, 30),
            wolny = { biuro(it) }, swieto = { false }, budzet = 0
        )
        assertTrue(p.isEmpty())
    }

    @Test
    fun odrzucamy_propozycje_ktore_sie_nie_oplacaja() {
        // same dni pracy bez wolnego dookoła — nie ma czego łączyć
        val p = VacationPlanner.zaproponuj(
            LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 6),
            wolny = { false }, swieto = { false }, budzet = 26
        )
        assertTrue(p.isEmpty())
    }

    @Test
    fun najwyzej_jedna_propozycja_na_miesiac() {
        val p = VacationPlanner.zaproponuj(
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
            wolny = { biuro(it) }, swieto = { false }, budzet = 26
        )
        val miesiace = p.map { java.time.YearMonth.from(it.wolneOd) }
        assertEquals("lista nie może być kopiami z jednego miesiąca", miesiace.size, miesiace.distinct().size)
    }

    @Test
    fun propozycje_sa_posortowane_od_najbardziej_oplacalnych() {
        val p = VacationPlanner.zaproponuj(
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
            wolny = { biuro(it) }, swieto = { false }, budzet = 26
        )
        assertTrue(p.isNotEmpty())
        p.zipWithNext().forEach { (a, b) -> assertTrue(a.oplacalnosc >= b.oplacalnosc) }
    }

    @Test
    fun blok_obejmuje_swieta_ktore_go_wydluzaja() {
        val swieta = setOf(LocalDate.of(2026, 12, 25), LocalDate.of(2026, 12, 26))
        val p = VacationPlanner.zaproponuj(
            LocalDate.of(2026, 12, 20), LocalDate.of(2027, 1, 4),
            wolny = { biuro(it) || it in swieta },
            swieto = { it in swieta }, budzet = 26
        )
        assertTrue(p.isNotEmpty())
        assertTrue("propozycja świąteczna powinna wymieniać święta", p.any { it.swieta.isNotEmpty() })
    }

    /** Cykl 4-brygadowy: wolne są krótkie, więc podpowiedzi muszą działać i tam. */
    @Test
    fun dziala_takze_dla_cyklu_zmianowego() {
        val cfg = CycleConfig(
            pattern = CyclePattern.B4_16D, anchorDate = LocalDate.of(2026, 1, 1),
            generate = true, genFrom = java.time.YearMonth.of(2026, 1), genTo = java.time.YearMonth.of(2026, 12)
        )
        val p = VacationPlanner.zaproponuj(
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
            wolny = { !CycleGenerator.shiftFor(cfg, it).isWork },
            swieto = { Holidays.isHoliday(it) }, budzet = 26
        )
        assertTrue("dla cyklu zmianowego też powinny być propozycje", p.isNotEmpty())
        assertTrue(p.all { it.koszt in 1..VacationPlanner.MAX_DNI_URLOPU })
    }

    /** Propozycje muszą wynikać z AKTUALNIE ustawionego cyklu, nie z jakiegoś domyślnego. */
    @Test
    fun zmiana_cyklu_zmienia_propozycje() {
        fun dla(p: CyclePattern): List<VacationSuggestion> {
            val cfg = CycleConfig(
                pattern = p, anchorDate = LocalDate.of(2026, 1, 1), generate = true,
                genFrom = java.time.YearMonth.of(2026, 1), genTo = java.time.YearMonth.of(2026, 12)
            )
            return VacationPlanner.zaproponuj(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                wolny = { !CycleGenerator.shiftFor(cfg, it).isWork },
                swieto = { Holidays.isHoliday(it) }, budzet = 26
            )
        }
        val szesnastka = dla(CyclePattern.B4_16D)
        val tygodniowy = dla(CyclePattern.TYGODNIOWY)
        assertTrue(szesnastka.isNotEmpty())
        assertTrue(tygodniowy.isNotEmpty())
        assertNotEquals(
            "inny cykl musi dać inne propozycje",
            szesnastka.map { it.urlop }, tygodniowy.map { it.urlop }
        )
        // w cyklu tygodniowym weekend daje tanie okazje: 2 dni urlopu za 4 wolne
        assertTrue("tygodniowy powinien mieć tanią propozycję", tygodniowy.any { it.koszt <= 2 })
        // w 16-dniowym wolne bloki są krótkie, więc mosty są dłuższe
        assertTrue("w 16-dniowym mosty są dłuższe", szesnastka.all { it.koszt >= 2 })
    }
}
