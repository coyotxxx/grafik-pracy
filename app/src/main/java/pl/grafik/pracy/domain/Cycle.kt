package pl.grafik.pracy.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Wzorce grafików zmianowych. Domyślny odwzorowany 1:1 z arkusza zakładowego Macieja
 * (analiza roku 2024: trafność ~94%, reszta to celowe korekty „odbieg od schematu").
 */
enum class CyclePattern(val label: String, val desc: String, val days: List<String>) {

    B4_16D(
        "4-brygadowy, cykl 16-dniowy",
        "4×I → 1 wolne → 4×III → 2 wolne → 4×II → 1 wolne",
        listOf("I","I","I","I","w","III","III","III","III","w","w","II","II","II","II","w")
    ),

    TYGODNIOWY(
        "3-zmianowy, rotacja tygodniowa",
        "5 dni na zmianie, weekend wolny, co tydzień inna zmiana",
        listOf("I","I","I","I","I","w","w","II","II","II","II","II","w","w","III","III","III","III","III","w","w")
    ),

    B4_CIAGLY(
        "4-brygadowy ciągły",
        "2×I → 2×II → 2×III → 2 wolne (praca także w weekendy)",
        listOf("I","I","II","II","III","III","w","w")
    ),

    B3_CIAGLY(
        "3-zmianowy ciągły",
        "4×I → 4×II → 4×III → 4 wolne",
        listOf("I","I","I","I","II","II","II","II","III","III","III","III","w","w","w","w")
    );

    val length: Int get() = days.size
}

data class CycleConfig(
    val pattern: CyclePattern = CyclePattern.B4_16D,
    /** Dzień odniesienia — od niego liczony jest cykl. */
    val anchorDate: LocalDate = LocalDate.of(2024, 3, 1),
    /** Pozycja w cyklu, na której użytkownik był w dniu odniesienia (0-based). */
    val anchorIndex: Int = 0,
    val brigade: String = "A",
    /**
     * Czy kalendarz ma być wypełniany z cyklu. Domyślnie NIE — po instalacji aplikacja
     * jest pusta i pokazuje wyłącznie to, co użytkownik sam wpisze.
     */
    val generate: Boolean = false
)

object CycleGenerator {

    fun shiftFor(cfg: CycleConfig, date: LocalDate): Shift {
        val delta = ChronoUnit.DAYS.between(cfg.anchorDate, date)
        val len = cfg.pattern.length
        var idx = ((delta + cfg.anchorIndex) % len).toInt()
        if (idx < 0) idx += len
        return when (cfg.pattern.days[idx]) {
            "I" -> Shift.I
            "II" -> Shift.II
            "III" -> Shift.III
            else -> Shift.W5
        }
    }

    /** Grafik na cały miesiąc, wyliczony z cyklu. */
    fun month(cfg: CycleConfig, year: Int, month: Int): Map<LocalDate, Shift> {
        val out = LinkedHashMap<LocalDate, Shift>()
        var d = LocalDate.of(year, month, 1)
        val end = d.plusMonths(1)
        while (d.isBefore(end)) {
            out[d] = shiftFor(cfg, d)
            d = d.plusDays(1)
        }
        return out
    }
}
