package pl.grafik.pracy.domain

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * Okres rozliczeniowy czasu pracy.
 *
 * Zakład Macieja rozlicza kwartałami, więc pojedynczy miesiąc niczego nie zamyka —
 * niedobór ze stycznia można odrobić w marcu. Norma bywa dwojaka: ustawowa z art. 130 KP
 * i ta, którą podaje zakład (w ruchu ciągłym wychodzi inna). Trzymamy obie i pokazujemy obok siebie.
 */
data class SettlementCfg(
    /** Długość okresu w miesiącach. Kwartał = 3. */
    val months: Int = 3,
    /** Czy rozliczamy się normą zakładową zamiast ustawowej. */
    val useCompanyNorm: Boolean = false,
    /** Norma podana przez zakład, klucz „yyyy-MM". Brak wpisu = zostaje ustawowa. */
    val companyNorms: Map<String, Int> = emptyMap(),
    /** Art. 151 § 3 KP — 150 h rocznie, chyba że regulamin zakładu daje więcej. */
    val otLimitYear: Int = 150
)

/** Okres rozliczeniowy — od pierwszego do ostatniego miesiąca włącznie. */
data class Period(val from: YearMonth, val to: YearMonth) {
    val months: List<YearMonth>
        get() = generateSequence(from) { it.plusMonths(1) }.takeWhile { !it.isAfter(to) }.toList()
    val start: LocalDate get() = from.atDay(1)
    val end: LocalDate get() = to.atEndOfMonth()
    /** Tygodnie okresu — do limitu z art. 131, liczonego „przeciętnie w tygodniu". */
    val weeks: Int get() = Math.round(ChronoUnit.DAYS.between(start, end.plusDays(1)) / 7.0).toInt()

    operator fun contains(d: LocalDate): Boolean = !d.isBefore(start) && !d.isAfter(end)
}

object Settlement {

    /** Ustawowa norma tygodniowa. */
    const val NORM_WEEK = 40

    /** Art. 131 § 1 KP — z nadgodzinami przeciętnie najwyżej tyle godzin tygodniowo. */
    const val MAX_WEEK_WITH_OT = 48

    /** Dopuszczalne długości okresu rozliczeniowego (art. 129 KP dopuszcza do 12 miesięcy). */
    val DLUGOSCI = listOf(1, 3, 4, 6, 12)

    /**
     * Okresy liczymy od stycznia — kwartały kalendarzowe I–III, IV–VI, VII–IX, X–XII.
     * Dzięki temu okres nigdy nie przechodzi przez granicę roku i limit roczny
     * da się policzyć z tych samych danych co okres.
     */
    fun periodOf(ym: YearMonth, cfg: SettlementCfg): Period {
        val len = cfg.months.coerceIn(1, 12)
        val idx = (ym.monthValue - 1) / len
        val from = YearMonth.of(ym.year, idx * len + 1)
        return Period(from, from.plusMonths((len - 1).toLong()))
    }

    fun key(ym: YearMonth): String = ym.toString()          // „2026-09"

    /** Norma ustawowa miesiąca — art. 130 KP. */
    fun statutoryNorm(ym: YearMonth): Int = Holidays.monthlyNorm(ym.year, ym.monthValue)

    fun statutoryNorm(p: Period): Int = p.months.sumOf { statutoryNorm(it) }

    /**
     * Norma obowiązująca w miesiącu. Zakładowa wchodzi tylko wtedy, gdy jest włączona
     * i faktycznie wpisana — pusty wpis nie może wyzerować normy.
     */
    fun normOfMonth(ym: YearMonth, cfg: SettlementCfg): Int {
        val ust = statutoryNorm(ym)
        if (!cfg.useCompanyNorm) return ust
        return cfg.companyNorms[key(ym)]?.takeIf { it > 0 } ?: ust
    }

    fun norm(p: Period, cfg: SettlementCfg): Int = p.months.sumOf { normOfMonth(it, cfg) }

    /**
     * Ile nadgodzin wolno w okresie. Art. 131: łącznie z nadgodzinami przeciętnie 48 h
     * tygodniowo, czyli 8 h nadgodzin na tydzień okresu.
     */
    fun otLimit(p: Period): Int = (MAX_WEEK_WITH_OT - NORM_WEEK) * p.weeks
}

/** Rozliczenie całego okresu — to, co widać w podsumowaniu nad kartą miesiąca. */
data class PeriodStats(
    val period: Period = Period(YearMonth.now(), YearMonth.now()),
    /** Przepracowane + pokryte urlopem w całym okresie. */
    val rozliczone: Int = 0,
    /** To samo, ale tylko do dzisiaj — w trwającym okresie reszta jest dopiero planem. */
    val doDzis: Int = 0,
    val norm: Int = 0,
    val normUstawowa: Int = 0,
    val ot: Int = 0,
    val otLimit: Int = 0,
    val otRok: Int = 0,
    val otLimitRok: Int = 150,
    val biezacy: Boolean = false
) {
    val diff: Int get() = rozliczone - norm
    /** Czy norma zakładowa różni się od ustawowej — wtedy pokazujemy obie. */
    val normaInna: Boolean get() = norm != normUstawowa
    val zostaloNadgodzin: Int get() = (otLimit - ot).coerceAtLeast(0)
    val zostaloNadgodzinRok: Int get() = (otLimitRok - otRok).coerceAtLeast(0)
}
