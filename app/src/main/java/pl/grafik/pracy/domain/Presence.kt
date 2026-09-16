package pl.grafik.pracy.domain

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/**
 * Miejsce pracy — strefa wykrywania obecności.
 * Trzymane w DataStore (jeden rekord), nie w Room.
 */
data class WorkPlace(
    val enabled: Boolean = false,
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val radiusM: Int = 200,
    /** SSID firmowego Wi-Fi — potwierdza obecność tam, gdzie GPS słabo łapie. */
    val ssid: String = "",
    /** Krótszy pobyt nie jest uznawany za dzień pracy (przejazd obok). */
    val minStayMin: Int = 30,
    /** Wyjście krótsze niż to sklejamy — skok do sklepu nie tnie dnia na dwa. */
    val mergeGapMin: Int = 30
) {
    val isSet: Boolean get() = lat != 0.0 || lon != 0.0
    val hasWifi: Boolean get() = ssid.isNotBlank()
}

/** Pojedynczy pobyt w strefie: wejście i wyjście. */
data class PresenceSpan(val enter: LocalDateTime, val exit: LocalDateTime) {
    val minutes: Long get() = Duration.between(enter, exit).toMinutes()
}

/** Wynik analizy jednego pobytu — to trafia do powiadomienia z propozycją. */
data class PresenceResult(
    val date: LocalDate,
    val span: PresenceSpan,
    /** Godziny po zaokrągleniu brzegów — to pokazujemy w powiadomieniu. */
    val countedFrom: LocalDateTime,
    val countedTo: LocalDateTime,
    val countedHours: Int,
    val normHours: Int,
    val otHours: Int,
    val otRate: OtRate,
    val shift: Shift?,
    /** Praca w dniu, który wg grafiku był wolny — całość jest nadgodziną. */
    val onFreeDay: Boolean
)

/**
 * Reguła Macieja (2026-09-16):
 *   „jeżeli jestem w pracy od 4:30 to znaczy że zaczynam pracę o 5:00,
 *    jeżeli byłem w pracy do 15:15 to znaczy że pracowałem do 15:00"
 *
 * Wejście zaokrąglamy W GÓRĘ, wyjście W DÓŁ — zawsze do pełnej godziny.
 * Zaokrąglenie liczy wyłącznie NADWYŻKĘ poza zmianą: spóźnienie nie obcina normy.
 */
object PresenceEngine {

    fun roundUp(t: LocalDateTime): LocalDateTime {
        val floor = t.truncatedTo(ChronoUnit.HOURS)
        return if (floor == t) t else floor.plusHours(1)
    }

    fun roundDown(t: LocalDateTime): LocalDateTime = t.truncatedTo(ChronoUnit.HOURS)

    /** Okno zmiany w danym dniu; III przechodzi przez północ. */
    fun shiftWindow(date: LocalDate, shift: Shift?): Pair<LocalDateTime, LocalDateTime>? {
        if (shift == null || !shift.isWork) return null
        val from = parseTime(shift.from) ?: return null
        val to = parseTime(shift.to) ?: return null
        val start = date.atTime(from)
        val end = if (to > from) date.atTime(to) else date.plusDays(1).atTime(to)
        return start to end
    }

    private fun parseTime(s: String): LocalTime? {
        val p = s.split(":")
        if (p.size != 2) return null
        val h = p[0].trim().toIntOrNull() ?: return null
        val m = p[1].trim().toIntOrNull() ?: return null
        return runCatching { LocalTime.of(h, m) }.getOrNull()
    }

    /**
     * Do którego dnia grafiku należy pobyt.
     * Wejście przed 6:00 przy zmianie III z poprzedniego dnia → to jeszcze tamten dzień.
     */
    fun assignDate(span: PresenceSpan, shiftOf: (LocalDate) -> Shift?): LocalDate {
        val d = span.enter.toLocalDate()
        if (span.enter.toLocalTime() < LocalTime.of(6, 0) && shiftOf(d.minusDays(1)) == Shift.III) {
            return d.minusDays(1)
        }
        return d
    }

    /** Skleja pobyty rozdzielone krótką przerwą (wyjście po zakupy, na papierosa). */
    fun mergeSpans(spans: List<PresenceSpan>, gapMinutes: Int): List<PresenceSpan> {
        if (spans.isEmpty()) return emptyList()
        val sorted = spans.sortedBy { it.enter }
        val out = mutableListOf(sorted.first())
        sorted.drop(1).forEach { s ->
            val last = out.last()
            val gap = Duration.between(last.exit, s.enter).toMinutes()
            if (gap <= gapMinutes) out[out.size - 1] = last.copy(exit = maxOf(last.exit, s.exit))
            else out.add(s)
        }
        return out
    }

    /** Odrzuca pobyty za krótkie, by uznać je za dzień pracy. */
    fun filterShort(spans: List<PresenceSpan>, minMinutes: Int): List<PresenceSpan> =
        spans.filter { it.minutes >= minMinutes }

    /** Stawka dodatku wg charakteru dnia. Dzień wolny / niedziela / święto → 100 %. */
    fun rateFor(kind: DayKind, onFreeDay: Boolean): OtRate =
        if (onFreeDay || kind == DayKind.NIEDZIELA || kind == DayKind.SWIETO) OtRate.P100 else OtRate.P50

    /**
     * Analiza jednego pobytu.
     * @param shift zmiana wynikająca z grafiku dla [date]; null lub dzień wolny → cała obecność to nadgodziny.
     */
    fun analyze(date: LocalDate, span: PresenceSpan, shift: Shift?, kind: DayKind): PresenceResult {
        val window = shiftWindow(date, shift)
        val upIn = roundUp(span.enter)
        val downOut = roundDown(span.exit)

        val from: LocalDateTime
        val to: LocalDateTime
        val norm: Int
        if (window != null) {
            // Spóźnienie nie obcina normy, wcześniejsze wyjście też nie — grafik zostaje grafikiem.
            from = minOf(window.first, upIn)
            to = maxOf(window.second, downOut)
            norm = shift!!.hours
        } else {
            from = upIn
            to = downOut
            norm = 0
        }

        val counted = if (to.isAfter(from)) Duration.between(from, to).toHours().toInt() else 0
        val ot = (counted - norm).coerceAtLeast(0)
        val onFree = window == null
        return PresenceResult(
            date = date,
            span = span,
            countedFrom = from,
            countedTo = to,
            countedHours = counted,
            normHours = norm,
            otHours = ot,
            otRate = rateFor(kind, onFree),
            shift = shift,
            onFreeDay = onFree
        )
    }
}
