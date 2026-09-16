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
    /**
     * Limit nadgodzin podany przez zakład na CAŁY okres — ten, którego nie wolno przekroczyć.
     * Klucz jak przy godzinach: pierwszy miesiąc okresu. Brak wpisu = obowiązuje
     * sufit techniczny z art. 131 KP.
     */
    val otLimitPeriods: Map<String, Int> = emptyMap()
)

/** Okres rozliczeniowy — od pierwszego do ostatniego miesiąca włącznie. */
data class Period(val from: YearMonth, val to: YearMonth) {
    val months: List<YearMonth>
        get() = generateSequence(from) { it.plusMonths(1) }.takeWhile { !it.isAfter(to) }.toList()
    val start: LocalDate get() = from.atDay(1)
    val end: LocalDate get() = to.atEndOfMonth()
    val days: Long get() = ChronoUnit.DAYS.between(start, end.plusDays(1))

    /**
     * PEŁNE tygodnie okresu — tak liczy się czas pracy w KP i tylko tak wychodzą
     * prawdziwe liczby: I kwartał ma 90 dni, czyli 12 pełnych tygodni, a nie 13.
     * Zaokrąglanie zawyżało limit nadgodzin I kwartału o 8 h.
     */
    val weeks: Int get() = (days / 7).toInt()

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

    /** Klucz normy zakładowej — pierwszy miesiąc okresu. */
    fun key(p: Period): String = p.from.toString()          // „2026-07" dla III kwartału

    /** Wszystkie okresy danego roku — do wpisania godzin z zakładu za jednym razem. */
    fun periodsOfYear(year: Int, cfg: SettlementCfg): List<Period> {
        val len = cfg.months.coerceIn(1, 12)
        return (1..12 step len).map { m ->
            val from = YearMonth.of(year, m)
            Period(from, from.plusMonths((len - 1).toLong()))
        }
    }

    /** Norma ustawowa miesiąca — art. 130 KP. */
    fun statutoryNorm(ym: YearMonth): Int = Holidays.monthlyNorm(ym.year, ym.monthValue)

    fun statutoryNorm(p: Period): Int = p.months.sumOf { statutoryNorm(it) }

    /**
     * Ustawowy limit nadgodzin okresu. Art. 131: łącznie z nadgodzinami przeciętnie
     * 48 h tygodniowo, czyli 8 h nadgodzin na każdy pełny tydzień okresu.
     * Dla kwartałów wychodzi 96 h (I) albo 104 h (pozostałe).
     */
    fun otLimit(p: Period): Int = (MAX_WEEK_WITH_OT - NORM_WEEK) * p.weeks

    /** Limit okresu podany przez zakład; null = zakład nic nie narzucił. */
    fun otLimitCompany(p: Period, cfg: SettlementCfg): Int? =
        cfg.otLimitPeriods[key(p)]?.takeIf { it > 0 }

    /** Limit okresu, który obowiązuje: zakładowy, gdy wpisany, inaczej ustawowy. */
    fun periodLimit(p: Period, cfg: SettlementCfg): Int =
        otLimitCompany(p, cfg) ?: otLimit(p)

    /**
     * Roczny limit nadgodzin = SUMA limitów okresów.
     * Zakład Macieja tak to ustala: rok to tyle, ile dają poszczególne kwartały.
     */
    fun yearLimit(year: Int, cfg: SettlementCfg): Int =
        periodsOfYear(year, cfg).sumOf { periodLimit(it, cfg) }

    /**
     * Górna granica roku z art. 131 KP — 8 h nadgodzin na każdy pełny tydzień roku,
     * czyli 416 h. Suma limitów kwartalnych nie powinna jej przekraczać.
     */
    fun yearCeiling(year: Int): Int {
        val dni = ChronoUnit.DAYS.between(LocalDate.of(year, 1, 1), LocalDate.of(year + 1, 1, 1))
        return (MAX_WEEK_WITH_OT - NORM_WEEK) * (dni / 7).toInt()
    }
}

/** Nadgodziny jednego okresu względem limitu — jeden pasek w podsumowaniu. */
data class PeriodStats(
    val period: Period = Period(YearMonth.now(), YearMonth.now()),
    /** Nadgodziny zrobione w tym okresie. */
    val ot: Int = 0,
    /** Limit ustawowy z art. 131 KP. */
    val otLimit: Int = 0,
    /** Limit podany przez zakład; null = zakład nic nie narzucił. */
    val otLimitZakl: Int? = null,
    val biezacy: Boolean = false
) {
    /**
     * Limit, który obowiązuje: zakładowy, gdy wpisany, inaczej ustawowy.
     * Zakład zwykle obniża ustawowy o kilka godzin, więc jego liczba zastępuje ustawową,
     * a nie jest z nią porównywana.
     */
    val limit: Int get() = otLimitZakl ?: otLimit

    val zakladowyObowiazuje: Boolean get() = otLimitZakl != null
    val zostalo: Int get() = (limit - ot).coerceAtLeast(0)
    val wyczerpany: Boolean get() = limit > 0 && ot >= limit
}
