package pl.grafik.pracy.domain

import java.time.YearMonth

/**
 * Szacunek wypłaty z grafiku — liczymy wyłącznie to, co wynika z umowy i godzin.
 *
 * Model wzięty z prawdziwych odcinków Macieja (Unilever, ruch ciągły):
 *
 * - wynagrodzenie jest **miesięczne**, a stawka za godzinę to zasadnicza ÷ **czas
 *   zaplanowany w grafiku** na ten miesiąc. W ruchu ciągłym plan bywa inny niż wymiar
 *   ustawowy: sierpień 2026 ma 160 h planu, lipiec 184 h, a stawki z odcinków
 *   (49,69 i 43,21 zł) wychodzą właśnie z tych liczb, nie z wymiaru z art. 130 KP;
 * - nadgodziny płatne są jak 1,5 albo 2 stawki godzinowej z zasadniczej, ale w różnych
 *   terminach: **50 % wpływa z wypłatą za ten sam miesiąc, a 100 % zbiorczo za cały
 *   okres rozliczeniowy, w wypłacie za jego ostatni miesiąc**. Potwierdzone odcinkami:
 *   pozycja „2545 NadlSred" pojawia się wyłącznie w grudniu, marcu i czerwcu;
 * - dodatek za III zmianę to **stała kwota za godzinę** z regulaminu, nie procent z Kodeksu.
 *
 * Świadomie NIE liczymy: premii uznaniowej, dodatku urlopowego ze średniej i nadgodzin
 * ze średniej — tych pozycji nie da się przewidzieć z grafiku, a zgadywanie robiłoby
 * z szacunku wróżenie.
 */

/** Stawki użytkownika — trzy liczby, wszystkie z umowy albo z regulaminu. */
data class StawkiCfg(
    /** Wynagrodzenie zasadnicze brutto za miesiąc. Zero = nie ustawiono. */
    val zasadnicza: Double = 0.0,
    /** Dodatek za godzinę pracy na zmianie nocnej — kwota z regulaminu zakładu. */
    val dodatekNocny: Double = 0.0,
    /** Czy pokazywać kwoty w Bilansie. */
    val pokazujWBilansie: Boolean = true
) {
    val ustawiona: Boolean get() = zasadnicza > 0.0
}

/** Jeden składnik wypłaty — nazwa, ile godzin, po ile i ile z tego wychodzi. */
data class SkladnikWyplaty(
    val nazwa: String,
    val godziny: Int,
    val stawka: Double,
    val kwota: Double
)

/** Wyliczona wypłata miesiąca. */
data class Wyplata(
    val zasadnicza: SkladnikWyplaty,
    val nadgodziny50: SkladnikWyplaty,
    /** Setki z całego okresu — wchodzą tylko w wypłacie za ostatni miesiąc okresu. */
    val nadgodziny100: SkladnikWyplaty,
    val nocny: SkladnikWyplaty,
    /**
     * Setki uzbierane do tej pory w okresie, które czekają na rozliczenie.
     * W ostatnim miesiącu okresu jest tu zero, bo wtedy wchodzą do wypłaty.
     */
    val nadgodziny100Czekaja: SkladnikWyplaty,
    /** Stawka za godzinę wyliczona z zasadniczej i planu — pokazujemy ją wprost. */
    val stawkaGodzinowa: Double,
    /** Czas zaplanowany w miesiącu, czyli dzielnik stawki. */
    val normaMiesiaca: Int
) {
    /** To, co wchodzi do tej wypłaty. Godziny czekające na koniec okresu są osobno. */
    val skladniki: List<SkladnikWyplaty>
        get() = listOf(zasadnicza, nadgodziny50, nadgodziny100, nocny).filter { it.kwota > 0.0 }

    val razem: Double get() = skladniki.sumOf { it.kwota }
}

object KalkulatorWyplaty {

    /** Dodatek za nadgodziny: +50 % albo +100 % do normalnego wynagrodzenia. */
    const val MNOZNIK_50 = 1.5
    const val MNOZNIK_100 = 2.0

    /** Ile godzin zmiany wypada w porze nocnej (22:00–6:00). */
    fun godzinyNocne(shift: Shift?): Int = when (shift) {
        Shift.III -> 8          // cała nocka mieści się w porze nocnej
        else -> 0
    }

    /** Stawka za godzinę: zasadnicza podzielona przez czas zaplanowany w miesiącu. */
    fun stawkaGodzinowa(zasadnicza: Double, planowaneH: Int): Double =
        if (planowaneH > 0) zasadnicza / planowaneH else 0.0

    /**
     * Czas zaplanowany: wszystkie zmiany z grafiku po 8 h, razem z dniami urlopu.
     * Tyle właśnie pokazuje odcinek w rubryce „Czas planowany" i przez tę liczbę
     * zakład dzieli zasadniczą, licząc stawkę za godzinę.
     */
    fun godzinyPlanowane(dni: Collection<DayEntry>): Int =
        dni.count { it.shift?.isWork == true || it.shift == Shift.URLOP } * 8

    /**
     * Wypłata z dni miesiąca.
     *
     * Zasadnicza wchodzi w całości — urlop jest płatny i już się w niej mieści,
     * dlatego nie liczymy go osobno.
     */
    /**
     * @param dni dni wyświetlanego miesiąca
     * @param ot100Okresu godziny po 100 % uzbierane w całym okresie rozliczeniowym
     * @param ostatniMiesiacOkresu czy ten miesiąc zamyka okres — wtedy setki są wypłacane
     */
    fun policz(
        dni: Collection<DayEntry>,
        cfg: StawkiCfg,
        ym: YearMonth,
        ot100Okresu: Int = 0,
        ostatniMiesiacOkresu: Boolean = false
    ): Wyplata {
        // Gdy miesiąc nie ma jeszcze wpisanego grafiku, bierzemy wymiar ustawowy —
        // inaczej stawka wychodziłaby zerowa i nadgodziny nic by nie kosztowały.
        val zGrafiku = godzinyPlanowane(dni)
        val planowane = if (zGrafiku > 0) zGrafiku else Settlement.statutoryNorm(ym)
        val stawka = stawkaGodzinowa(cfg.zasadnicza, planowane)

        var hNoc = 0
        var hOt50 = 0

        dni.forEach { e ->
            if (e.shift?.isWork == true) hNoc += godzinyNocne(e.shift)
            if (e.otHours > 0 && e.otRate == OtRate.P50) hOt50 += e.otHours
        }

        val hWyplacane = if (ostatniMiesiacOkresu) ot100Okresu else 0
        val hCzekajace = if (ostatniMiesiacOkresu) 0 else ot100Okresu

        return Wyplata(
            zasadnicza = SkladnikWyplaty("Zasadnicza", planowane, stawka, cfg.zasadnicza),
            nadgodziny50 = SkladnikWyplaty(
                "Nadgodziny 50 %", hOt50, stawka * MNOZNIK_50, hOt50 * stawka * MNOZNIK_50
            ),
            nadgodziny100 = SkladnikWyplaty(
                "Nadgodziny 100 % za okres", hWyplacane, stawka * MNOZNIK_100,
                hWyplacane * stawka * MNOZNIK_100
            ),
            nocny = SkladnikWyplaty(
                "Dodatek za nocki", hNoc, cfg.dodatekNocny, hNoc * cfg.dodatekNocny
            ),
            nadgodziny100Czekaja = SkladnikWyplaty(
                "Nadgodziny 100 % — czekają na koniec okresu", hCzekajace, stawka * MNOZNIK_100,
                hCzekajace * stawka * MNOZNIK_100
            ),
            stawkaGodzinowa = stawka,
            normaMiesiaca = planowane
        )
    }

    /** Godziny po 100 % w podanych dniach — do zsumowania z całego okresu. */
    fun godziny100(dni: Collection<DayEntry>): Int =
        dni.filter { it.otHours > 0 && it.otRate == OtRate.P100 }.sumOf { it.otHours }

    /** „5 667,00" — kwota po polsku, ze spacją co trzy cyfry i przecinkiem. */
    fun zlote(kwota: Double): String {
        val calosc = kwota.toLong()
        val grosze = Math.round((kwota - calosc) * 100).toInt()
        val cyfry = calosc.toString().reversed().chunked(3).joinToString(" ").reversed()
        return "%s,%02d".format(cyfry, grosze)
    }
}
