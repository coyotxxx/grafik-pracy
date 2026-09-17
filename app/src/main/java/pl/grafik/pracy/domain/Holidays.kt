package pl.grafik.pracy.domain

import java.time.LocalDate

/**
 * Polskie dni ustawowo wolne od pracy — stałe oraz ruchome liczone z daty Wielkanocy.
 * Norma miesięczna: (dni robocze pon–pt × 8) − (święta wypadające w dni robocze × 8).
 */
object Holidays {

    /** Wielkanoc metodą Meeusa/Jonesa/Butchera (kalendarz gregoriański). */
    fun easter(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = ((h + l - 7 * m + 114) % 31) + 1
        return LocalDate.of(year, month, day)
    }

    /** Od 2025 roku Wigilia jest dniem ustawowo wolnym od pracy. */
    private const val PIERWSZY_ROK_WIGILII = 2025

    fun all(year: Int): Map<LocalDate, String> {
        val e = easter(year)
        val dni = linkedMapOf(
            LocalDate.of(year, 1, 1) to "Nowy Rok",
            LocalDate.of(year, 1, 6) to "Trzech Króli",
            e to "Wielkanoc",
            e.plusDays(1) to "Poniedziałek Wielkanocny",
            LocalDate.of(year, 5, 1) to "Święto Pracy",
            LocalDate.of(year, 5, 3) to "Święto Konstytucji 3 Maja",
            e.plusDays(49) to "Zesłanie Ducha Świętego",
            e.plusDays(60) to "Boże Ciało",
            LocalDate.of(year, 8, 15) to "Wniebowzięcie NMP",
            LocalDate.of(year, 11, 1) to "Wszystkich Świętych",
            LocalDate.of(year, 11, 11) to "Święto Niepodległości",
            LocalDate.of(year, 12, 25) to "Boże Narodzenie",
            LocalDate.of(year, 12, 26) to "Drugi dzień Świąt"
        )
        if (year >= PIERWSZY_ROK_WIGILII) dni[LocalDate.of(year, 12, 24)] = "Wigilia"
        return dni
    }

    fun nameOf(d: LocalDate): String? = all(d.year)[d]
    fun isHoliday(d: LocalDate): Boolean = all(d.year).containsKey(d)

    fun kindOf(d: LocalDate): DayKind = when {
        isHoliday(d) -> DayKind.SWIETO
        d.dayOfWeek.value == 7 -> DayKind.NIEDZIELA
        d.dayOfWeek.value == 6 -> DayKind.SOBOTA
        else -> DayKind.ZWYKLY
    }

    /**
     * Norma godzin wg art. 130 KP: 8 h za każdy dzień od poniedziałku do piątku,
     * minus 8 h za **każde** święto przypadające w innym dniu niż niedziela.
     *
     * Święto w sobotę też obniża wymiar (art. 130 § 2) — sierpień 2026 ma przez to
     * 160 h, nie 168. Wcześniej liczyliśmy tylko święta z poniedziałku–piątku,
     * co zawyżało normę w miesiącach ze świętem wypadającym w sobotę.
     */
    fun monthlyNorm(year: Int, month: Int): Int {
        var d = LocalDate.of(year, month, 1)
        val end = d.plusMonths(1)
        var workdays = 0
        var holidays = 0
        val hol = all(year)
        while (d.isBefore(end)) {
            val dow = d.dayOfWeek.value
            if (dow <= 5) workdays++
            if (dow != 7 && hol.containsKey(d)) holidays++
            d = d.plusDays(1)
        }
        return (workdays - holidays) * 8
    }
}
