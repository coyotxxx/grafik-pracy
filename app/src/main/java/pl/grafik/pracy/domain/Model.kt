package pl.grafik.pracy.domain

import java.time.LocalDate

/** Co robię w danym dniu — pierwszy wymiar modelu odczytanego z arkusza zakładowego. */
enum class Shift(val code: String, val label: String, val hours: Int, val from: String, val to: String) {
    I("I", "Zmiana I", 8, "6:00", "14:00"),
    II("II", "Zmiana II", 8, "14:00", "22:00"),
    III("III", "Zmiana III", 8, "22:00", "6:00"),
    W5("w5", "Wolne", 0, "", ""),
    WS("wś", "Wolne święto", 0, "", ""),
    DWN("DWN", "Dzień za pracującą niedzielę", 0, "", ""),
    BWN("bezw.", "Bezwzględnie wolna niedziela", 0, "", ""),
    URLOP("U", "Urlop", 0, "", ""),
    L4("L4", "Zwolnienie lekarskie", 0, "", "");

    val isWork: Boolean get() = this == I || this == II || this == III
}

/** Charakter dnia — drugi wymiar. Liczony z kalendarza, nie wpisywany ręcznie. */
enum class DayKind { ZWYKLY, SOBOTA, NIEDZIELA, SWIETO }

/** Stawka dodatku za nadgodziny. */
enum class OtRate(val percent: Int) { P50(50), P100(100) }

data class DayEntry(
    val date: LocalDate,
    val shift: Shift? = null,
    val otHours: Int = 0,
    val otRate: OtRate = OtRate.P100,
    /** Odbieg od stałego schematu — żółte tło w arkuszu zakładowym. */
    val deviation: Boolean = false,
    val note: String = "",
    /** Dla DWN: za którą pracującą niedzielę odbierany jest ten dzień. */
    val dwnFor: LocalDate? = null
) {
    val workedHours: Int get() = (if (shift?.isWork == true) 8 else 0) + otHours
}

/**
 * Urlop wypoczynkowy. Punkt odniesienia („na dzień X zostało mi Y") jest ważny,
 * bo prawdziwy stan trzyma kadrowa, a nie ta aplikacja — dzięki niemu można
 * zsynchronizować licznik z tym, co pokazuje zakład.
 */
data class VacationCfg(
    /** Wymiar roczny: 20 dni do 10 lat stażu, 26 powyżej. */
    val wymiar: Int = 26,
    /** Niewykorzystany z poprzedniego roku. Trzeba go wybrać do 30 września. */
    val zalegly: Int = 0,
    /** Dzień, na który znamy prawdziwy stan (null = liczymy od początku roku). */
    val stanData: LocalDate? = null,
    /** Ile zostało z puli bieżącego roku na ten dzień. */
    val stanBiezacy: Int = 0,
    /** Ile zostało z puli zaległej na ten dzień. */
    val stanZalegly: Int = 0
) {
    companion object {
        /** Art. 168 KP — urlopu zaległego udziela się najpóźniej do 30 września. */
        fun terminZaleglego(rok: Int): LocalDate = LocalDate.of(rok, 9, 30)
    }
}

/** Rozbicie urlopu na pulę zaległą i bieżącą — zużywamy najpierw zaległą. */
data class VacationBalance(
    val bazaZalegly: Int,
    val bazaBiezacy: Int,
    val zuzyte: Int,
    val rok: Int
) {
    val zZaleglego: Int get() = minOf(zuzyte, bazaZalegly)
    val zBiezacego: Int get() = zuzyte - zZaleglego
    val zostaloZaleglego: Int get() = bazaZalegly - zZaleglego
    val zostaloBiezacego: Int get() = bazaBiezacy - zBiezacego
    val zostalo: Int get() = zostaloZaleglego + zostaloBiezacego
    val baza: Int get() = bazaZalegly + bazaBiezacy

    val termin: LocalDate get() = VacationCfg.terminZaleglego(rok)
    /** Ile dni do 30 września; ujemne = termin minął. */
    fun dniDoTerminu(dzis: LocalDate): Long =
        java.time.temporal.ChronoUnit.DAYS.between(dzis, termin)
}

data class MonthStats(
    val worked: Int = 0,
    val norm: Int = 0,
    val ot100: Int = 0,
    val ot50: Int = 0,
    val daysWork: Int = 0,
    val daysFree: Int = 0,
    val sundayWork: Int = 0,
    val holidayWork: Int = 0,
    val saturdayWork: Int = 0,
    val byShift: Map<Shift, Int> = emptyMap(),
    /** Ile dni na każdej zmianie — obok godzin. */
    val daysByShift: Map<Shift, Int> = emptyMap(),
    /** Godziny pokryte urlopem — dzień urlopu pokrywa dzień roboczy, który wypadał wg grafiku. */
    val urlopH: Int = 0,
    /** Godziny rozliczone do dzisiaj włącznie — w trwającym miesiącu reszta jest dopiero planem. */
    val doDzis: Int = 0,
    /** Czy wyświetlany miesiąc to ten, w którym jesteśmy. */
    val biezacyMiesiac: Boolean = false
) {
    /** Co idzie do rozliczenia miesiąca: przepracowane + pokryte urlopem. */
    val rozliczone: Int get() = worked + urlopH
    val ot: Int get() = ot100 + ot50
    val diff: Int get() = rozliczone - norm
}
