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
    val onFreeDay: Boolean,
    /** Zmiana rozpoznana po godzinie przyjazdu — do opisu w powiadomieniu. */
    val recognized: Shift = Shift.I,
    /** Ile odpoczynku zostaje przed następną zmianą, gdy jest go za mało. null = w porządku. */
    val restHours: Long? = null
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

    /** Art. 132 KP — dobowy odpoczynek. Nowa zmiana nie może zacząć się wcześniej. */
    const val MIN_REST_HOURS = 11L

    /**
     * Zamianę zmian uznajemy dopiero przy pobycie tej długości. Krótsza wizyta
     * w dniu, w którym grafik przewidywał inną zmianę, to nadgodziny, a nie cała zmiana.
     */
    const val MIN_SWAP_MIN = 240L

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
     *
     * Rozstrzyga o tym dobowy odpoczynek (art. 132 KP). Po zmianie kończącej się o 6:00
     * następna nie może ruszyć przed 17:00, więc każde wejście w tym oknie to nie nowa
     * zmiana, tylko przedłużenie poprzedniej — i liczy się do TAMTEGO dnia.
     */
    fun assignDate(span: PresenceSpan, shiftOf: (LocalDate) -> Shift?): LocalDate {
        val d = span.enter.toLocalDate()
        val prev = d.minusDays(1)
        val prevEnd = shiftWindow(prev, shiftOf(prev))?.second ?: return d
        return if (span.enter.isBefore(prevEnd.plusHours(MIN_REST_HOURS))) prev else d
    }

    /**
     * Na którą zmianę przyjechałem — po godzinie wejścia, już zaokrąglonej w górę.
     * 21:30 → 22:00 → nocka. Używane, gdy pobyt nie pasuje do zmiany z grafiku.
     */
    fun shiftByEntry(enter: LocalDateTime): Shift = when (roundUp(enter).hour) {
        in 5..11 -> Shift.I
        in 12..19 -> Shift.II
        else -> Shift.III
    }

    /**
     * Godziny zaliczone w zapisanym pobycie. Liczymy z zaokrąglonych brzegów,
     * bo to one trafiają do grafiku — nie z surowego czasu w strefie.
     */
    fun countedHours(from: LocalDateTime?, to: LocalDateTime?): Int {
        if (from == null || to == null || !to.isAfter(from)) return 0
        return Duration.between(from, to).toHours().toInt()
    }

    /** Czy pobyt w ogóle zahacza o okno zmiany. */
    fun overlaps(span: PresenceSpan, w: Pair<LocalDateTime, LocalDateTime>): Boolean =
        span.enter.isBefore(w.second) && span.exit.isAfter(w.first)

    /**
     * Czy normę tego dnia pokrył już wcześniejszy pobyt. Liczymy po godzinach
     * zaliczonych, nie po samym istnieniu wpisu — krótkie zajrzenie do pracy
     * niczego nie pokrywa i nie może zabrać normy właściwej zmianie.
     */
    fun normAlreadyCounted(planned: Pair<LocalDateTime, LocalDateTime>?, previous: List<PresenceSpan>): Boolean {
        if (planned == null) return false
        return previous.any { it.enter.isBefore(planned.second) && it.exit.isAfter(planned.first) }
    }

    /**
     * Ile odpoczynku zostaje do najbliższej zaplanowanej zmiany.
     * Zwraca liczbę godzin tylko wtedy, gdy jest ich mniej niż wymagane 11 — inaczej null.
     */
    fun restBefore(countedTo: LocalDateTime, nextStarts: List<LocalDateTime>): Long? {
        val next = nextStarts.filter { it.isAfter(countedTo) }.minOrNull() ?: return null
        val h = Duration.between(countedTo, next).toHours()
        return if (h < MIN_REST_HOURS) h else null
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
     *
     * @param shift zmiana wynikająca z grafiku dla [date]; dzień wolny → cała obecność to nadgodziny.
     * @param normUsed czy normę tego dnia pokrył już wcześniejszy pobyt — wtedy to, co teraz, jest nadgodziną.
     */
    fun analyze(
        date: LocalDate,
        span: PresenceSpan,
        shift: Shift?,
        kind: DayKind,
        normUsed: Boolean = false
    ): PresenceResult {
        val upIn = roundUp(span.enter)
        val downOut = roundDown(span.exit)

        val planned = shiftWindow(date, shift)
        val onFree = planned == null
        val rozpoznana = shiftByEntry(span.enter)

        // Okno zmiany z grafiku stosujemy TYLKO wtedy, gdy pobyt faktycznie w nie trafia.
        // Bez tego przyjazd o 21:30 w dniu ze zmianą I rozciągał liczenie od 6:00 do 6:00
        // następnego dnia — 24 h i 16 h wymyślonych nadgodzin.
        val planPasuje = planned != null && overlaps(span, planned)

        // Zamiana zmian: grafik mówi jedno, przyjechałem na inną zmianę. Uznajemy to
        // dopiero przy odpowiednio długim pobycie i tylko gdy norma dnia jest jeszcze wolna —
        // inaczej krótkie zajrzenie do pracy zaliczyłoby się jako cała zmiana.
        val szukajZamiany = !onFree && !planPasuje && !normUsed && span.minutes >= MIN_SWAP_MIN
        val oknoZamiany = if (szukajZamiany) shiftWindow(date, rozpoznana) else null
        val zamianaPasuje = oknoZamiany != null && overlaps(span, oknoZamiany)

        // Gdy norma tego dnia jest już policzona, nie rozciągamy niczego na okno zmiany —
        // liczą się wyłącznie godziny faktycznie spędzone w pracy.
        val window = when {
            normUsed -> null
            planPasuje -> planned
            zamianaPasuje -> oknoZamiany
            else -> null
        }
        val zmiana = when {
            planPasuje -> shift
            zamianaPasuje -> rozpoznana
            else -> shift
        }

        val from: LocalDateTime
        val to: LocalDateTime
        if (window != null) {
            // Spóźnienie nie obcina normy, wcześniejsze wyjście też nie — grafik zostaje grafikiem.
            from = minOf(window.first, upIn)
            to = maxOf(window.second, downOut)
        } else {
            from = upIn
            to = downOut
        }
        val norm = if (window != null) (zmiana?.hours ?: 0) else 0

        val counted = if (to.isAfter(from)) Duration.between(from, to).toHours().toInt() else 0
        val ot = (counted - norm).coerceAtLeast(0)
        return PresenceResult(
            date = date,
            span = span,
            countedFrom = from,
            countedTo = to,
            countedHours = counted,
            normHours = norm,
            otHours = ot,
            otRate = rateFor(kind, onFree),
            shift = zmiana,
            onFreeDay = onFree,
            recognized = rozpoznana
        )
    }
}
