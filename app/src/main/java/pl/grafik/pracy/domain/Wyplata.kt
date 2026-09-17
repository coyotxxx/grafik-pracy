package pl.grafik.pracy.domain

/**
 * Szacunek wypłaty z grafiku. Liczymy brutto — kwoty do ręki nie policzymy,
 * bo aplikacja nie zna składek, ulg ani premii regulaminowej.
 *
 * Silnik jest czysty: dostaje dni i stawki, oddaje liczby. Bez bazy i bez Androida.
 */

/** Stawki użytkownika — wszystko, czego potrzeba do wyliczenia. */
data class StawkiCfg(
    /** Brutto za godzinę, z umowy. Zero = nie ustawiono. */
    val stawka: Double = 0.0,
    /**
     * Podstawa dodatku za porę nocną. Kodeks (art. 151⁸ § 1) liczy 20 % stawki
     * wynikającej z płacy minimalnej, ale wiele zakładów liczy z własnej stawki
     * pracownika — stąd wybór.
     */
    val nocnyZMinimalnej: Boolean = true,
    /** Stawka godzinowa z płacy minimalnej — zmienia się co roku, więc jest do wpisania. */
    val stawkaMinimalna: Double = 30.50,
    /** Premia procentowa od podstawy. */
    val premiaProc: Int = 0,
    /** Czy pokazywać kwoty w Bilansie. */
    val pokazujWBilansie: Boolean = true
) {
    val ustawiona: Boolean get() = stawka > 0.0
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
    val podstawa: SkladnikWyplaty,
    val urlop: SkladnikWyplaty,
    val nadgodziny50: SkladnikWyplaty,
    val nadgodziny100: SkladnikWyplaty,
    val nocny: SkladnikWyplaty,
    val premia: SkladnikWyplaty
) {
    val skladniki: List<SkladnikWyplaty>
        get() = listOf(podstawa, urlop, nadgodziny50, nadgodziny100, nocny, premia)
            .filter { it.kwota > 0.0 }

    val razem: Double get() = skladniki.sumOf { it.kwota }
}

object KalkulatorWyplaty {

    /** Art. 151⁸ KP — dodatek za każdą godzinę pracy w porze nocnej. */
    const val DODATEK_NOCNY = 0.20

    /** Dodatek za nadgodziny: +50 % albo +100 % do normalnego wynagrodzenia. */
    const val MNOZNIK_50 = 1.5
    const val MNOZNIK_100 = 2.0

    /** Ile godzin zmiany wypada w porze nocnej (22:00–6:00). */
    fun godzinyNocne(shift: Shift?): Int = when (shift) {
        Shift.III -> 8          // cała nocka mieści się w porze nocnej
        else -> 0
    }

    /**
     * Wypłata z podanych dni.
     *
     * Dzień urlopu liczymy jak przepracowany — wynagrodzenie urlopowe w stałej stawce
     * wychodzi tak samo, a rozbicie i tak pokazujemy osobno.
     */
    fun policz(dni: Collection<DayEntry>, cfg: StawkiCfg): Wyplata {
        val stawkaNocna =
            (if (cfg.nocnyZMinimalnej) cfg.stawkaMinimalna else cfg.stawka) * DODATEK_NOCNY

        var hPraca = 0
        var hUrlop = 0
        var hNoc = 0
        var hOt50 = 0
        var hOt100 = 0

        dni.forEach { e ->
            if (e.shift?.isWork == true) {
                hPraca += 8
                hNoc += godzinyNocne(e.shift)
            }
            if (e.shift == Shift.URLOP) hUrlop += 8
            if (e.otHours > 0) {
                if (e.otRate == OtRate.P50) hOt50 += e.otHours else hOt100 += e.otHours
            }
        }

        val podstawa = hPraca * cfg.stawka
        val premia = podstawa * (cfg.premiaProc / 100.0)

        return Wyplata(
            podstawa = SkladnikWyplaty("Podstawa", hPraca, cfg.stawka, podstawa),
            urlop = SkladnikWyplaty("Urlop", hUrlop, cfg.stawka, hUrlop * cfg.stawka),
            nadgodziny50 = SkladnikWyplaty(
                "Nadgodziny 50 %", hOt50, cfg.stawka * MNOZNIK_50, hOt50 * cfg.stawka * MNOZNIK_50
            ),
            nadgodziny100 = SkladnikWyplaty(
                "Nadgodziny 100 %", hOt100, cfg.stawka * MNOZNIK_100, hOt100 * cfg.stawka * MNOZNIK_100
            ),
            nocny = SkladnikWyplaty("Dodatek nocny", hNoc, stawkaNocna, hNoc * stawkaNocna),
            premia = SkladnikWyplaty("Premia ${cfg.premiaProc} %", 0, 0.0, premia)
        )
    }

    /** „5 667,00" — kwota po polsku, ze spacją co trzy cyfry i przecinkiem. */
    fun zlote(kwota: Double): String {
        val calosc = kwota.toLong()
        val grosze = Math.round((kwota - calosc) * 100).toInt()
        val cyfry = calosc.toString().reversed().chunked(3).joinToString(" ").reversed()
        return "%s,%02d".format(cyfry, grosze)
    }
}
