package pl.grafik.pracy.location

import android.content.Context
import android.util.Log
import android.location.Location
import pl.grafik.pracy.data.AppDb
import pl.grafik.pracy.data.DayRow
import pl.grafik.pracy.data.PresenceRow
import pl.grafik.pracy.data.SettingsStore
import pl.grafik.pracy.domain.*
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Serce automatycznego wykrywania.
 *
 * Zasada: nic nie trafia do grafiku samo. Wykryty pobyt ląduje w tabeli `presence`
 * ze statusem `pending` i czeka na jedno dotknięcie w powiadomieniu.
 */
object PresenceRepo {

    const val TAG = "GrafikPresence"

    suspend fun onEnter(ctx: Context, at: LocalDateTime = LocalDateTime.now()) {
        PresenceState.markEnter(ctx, at)
    }

    suspend fun onStillHere(ctx: Context, at: LocalDateTime = LocalDateTime.now()) {
        PresenceState.touch(ctx, at)
    }

    /**
     * Wyjście ze strefy. Jeśli telefon nadal wisi na firmowym Wi-Fi, wyjście jest
     * fałszywe (GPS zgubił sygnał w hali) i je ignorujemy.
     */
    suspend fun onExit(ctx: Context, at: LocalDateTime = LocalDateTime.now()): Long? {
        val wp = SettingsStore(ctx).workPlace.first()
        if (wp.hasWifi && WifiCheck.isAtWork(ctx, wp.ssid)) {
            PresenceState.touch(ctx, at)
            return null
        }
        return closeSpan(ctx, at, wp, source = if (wp.hasWifi) "geo+wifi" else "geo")
    }

    /**
     * Pobyt dłuższy niż to jest na pewno błędem (najdłuższa zmiana + nadgodziny to kilkanaście godzin).
     * Tylko w takim wypadku domykamy pobyt „na ślepo", bez potwierdzenia pozycją.
     */
    const val MAX_OPEN_HOURS = 16L

    /**
     * Ratunek na zgubione zdarzenie EXIT.
     *
     * Uwaga na pułapkę: brak zdarzeń NIE znaczy, że wyszedłem. Przy ośmiogodzinnej zmianie
     * Android nie przysyła nic od wejścia do wyjścia. Dlatego watchdog sam sprawdza pozycję
     * i zamyka pobyt dopiero wtedy, gdy naprawdę jestem poza strefą.
     */
    suspend fun watchdog(
        ctx: Context,
        now: LocalDateTime = LocalDateTime.now(),
        probe: suspend () -> Location? = { LocationProbe.current(ctx) }
    ) {
        val wp = SettingsStore(ctx).workPlace.first()
        if (!wp.enabled) { Log.i(TAG, "watchdog: wykrywanie wyłączone"); return }
        val enter = PresenceState.openEnter(ctx)
        if (enter == null) { Log.i(TAG, "watchdog: brak otwartego pobytu"); return }

        // 1. Firmowe Wi-Fi — najtańsze i najpewniejsze potwierdzenie.
        if (wp.hasWifi && WifiCheck.isAtWork(ctx, wp.ssid)) {
            Log.i(TAG, "watchdog: firmowe Wi-Fi — nadal w pracy")
            PresenceState.touch(ctx, now)
            return
        }

        // 2. Pytamy o pozycję.
        val loc = probe()
        if (loc != null) {
            val inside = LocationProbe.isInside(loc, wp)
            Log.i(TAG, "watchdog: pozycja ${"%.5f".format(loc.latitude)},${"%.5f".format(loc.longitude)} — ${if (inside) "w strefie" else "POZA strefą"}")
            if (inside) {
                PresenceState.touch(ctx, now)          // nadal w pracy — nic nie zamykamy
            } else {
                closeSpan(ctx, now, wp, source = "watchdog")   // wyjście, którego Android nie zgłosił
            }
            return
        }
        Log.w(TAG, "watchdog: brak pozycji — sprawdzam ciszę")

        // 3. Pozycji nie ma — GPS wyłączony, brak uprawnienia albo Android dławi lokalizację
        //    aplikacjom w tle. To znaczy „NIE WIEM", a nie „wyszedłem".
        //
        //    Nie wolno tu zamykać pobytu po krótkiej ciszy: przez całą ośmiogodzinną zmianę
        //    możemy nie dostać ani jednej pozycji, a zamknięcie zapisałoby godzinę zamiast ośmiu.
        //    Czekamy na zdarzenie EXIT z geofence. Domykamy dopiero, gdy pobyt jest
        //    absurdalnie długi — wtedy coś ewidentnie poszło nie tak i lepiej dać propozycję
        //    do poprawienia niż zostawić wiszący pobyt na zawsze.
        val seen = PresenceState.lastSeen(ctx) ?: enter
        val otwartyH = Duration.between(enter, now).toHours()
        Log.i(TAG, "watchdog: brak pozycji, pobyt otwarty $otwartyH h (limit $MAX_OPEN_HOURS)")
        if (otwartyH >= MAX_OPEN_HOURS) {
            Log.w(TAG, "watchdog: domykam awaryjnie ostatnim kontaktem $seen")
            closeSpan(ctx, seen, wp, source = "watchdog-awaryjnie")
        }
    }

    private suspend fun closeSpan(ctx: Context, exitAt: LocalDateTime, wp: WorkPlace, source: String): Long? {
        val enter = PresenceState.openEnter(ctx) ?: return null
        PresenceState.clear(ctx)
        if (!exitAt.isAfter(enter)) return null

        val span = PresenceSpan(enter, exitAt)
        if (span.minutes < wp.minStayMin) {
            Log.i(TAG, "pobyt ${span.minutes} min < próg ${wp.minStayMin} — pomijam")
            return null                                    // przejazd obok, nie dzień pracy
        }

        val db = AppDb.get(ctx)
        val cfg = SettingsStore(ctx).config.first()
        // Zmiany wyliczamy z góry — assignDate nie jest funkcją zawieszającą.
        val dEnter = span.enter.toLocalDate()
        val hour = PresenceEngine.roundUp(span.enter).hour
        val known = mapOf(
            dEnter.minusDays(1) to effectiveShift(ctx, cfg, dEnter.minusDays(1), hour),
            dEnter to effectiveShift(ctx, cfg, dEnter, hour)
        )
        val date = PresenceEngine.assignDate(span) { d -> known[d] }
        val shift = known[date] ?: effectiveShift(ctx, cfg, date, hour)

        // Czy normę tego dnia pokrył już wcześniejszy pobyt (dwie wizyty w jednym dniu).
        val planowane = PresenceEngine.shiftWindow(date, shift)
        val wczesniejsze = db.presenceDao().forDate(date.toString())
            .filter { it.status != "rejected" }
            .mapNotNull { r ->
                runCatching {
                    PresenceSpan(LocalDateTime.parse(r.countedFrom), LocalDateTime.parse(r.countedTo))
                }.getOrNull()
            }
        val normaZajeta = PresenceEngine.normAlreadyCounted(planowane, wczesniejsze)

        val analiza = PresenceEngine.analyze(date, span, shift, Holidays.kindOf(date), normaZajeta)

        // Art. 132 KP — czy do następnej zmiany zostaje wymagane 11 h odpoczynku.
        val nastepne = listOfNotNull(
            PresenceEngine.shiftWindow(date.plusDays(1), effectiveShift(ctx, cfg, date.plusDays(1)))?.first,
            PresenceEngine.shiftWindow(date.plusDays(2), effectiveShift(ctx, cfg, date.plusDays(2)))?.first
        )
        val result = analiza.copy(restHours = PresenceEngine.restBefore(analiza.countedTo, nastepne))

        val id = db.presenceDao().insert(
            PresenceRow(
                date = date.toString(),
                enterAt = enter.toString(),
                exitAt = exitAt.toString(),
                source = source,
                status = "pending",
                otHours = result.otHours,
                otRate = result.otRate.percent,
                countedFrom = result.countedFrom.toString(),
                countedTo = result.countedTo.toString(),
                createdAt = LocalDateTime.now().toString()
            )
        )
        Log.i(TAG, "zapisano propozycję #$id: ${result.date} ${result.otHours}h ${result.otRate.percent}% (${'$'}source)")
        PresenceNotif.propose(ctx, id, result)
        return id
    }

    /**
     * Zmiana obowiązująca w danym dniu.
     * Kolejność: to co wpisane ręcznie → cykl (jeśli włączony) → odczyt z godziny wejścia.
     * Ostatni wariant jest dla pustego grafiku: bez niego cała obecność poszłaby
     * jako nadgodziny 100 %, bo apka uznałaby dzień za wolny.
     */
    private suspend fun effectiveShift(
        ctx: Context, cfg: CycleConfig, d: LocalDate, entryHour: Int? = null
    ): Shift? {
        AppDb.get(ctx).dayDao().get(d.toString())?.toEntry()?.shift?.let { return it }
        if (cfg.generate) return CycleGenerator.shiftFor(cfg, d)
        return entryHour?.let { shiftFromHour(it) }
    }

    /** Przybliżenie zmiany po godzinie wejścia — używane tylko, gdy grafik jest pusty. */
    fun shiftFromHour(h: Int): Shift =
        PresenceEngine.shiftByEntry(LocalDateTime.of(2000, 1, 1, h.coerceIn(0, 23), 0))

    /** Zatwierdzenie propozycji — dopiero tu wpis trafia do grafiku. */
    suspend fun accept(ctx: Context, id: Long) {
        val db = AppDb.get(ctx)
        val row = db.presenceDao().byId(id) ?: return
        if (row.status == "accepted") return

        val date = LocalDate.parse(row.date)
        val cfg = SettingsStore(ctx).config.first()
        val hour = runCatching { LocalDateTime.parse(row.countedFrom).hour }.getOrNull()
        val cur = db.dayDao().get(row.date)?.toEntry()
            ?: DayEntry(
                date = date,
                shift = if (cfg.generate) CycleGenerator.shiftFor(cfg, date)
                        else hour?.let { shiftFromHour(it) }
            )

        // W dniu wolnym zostawiamy oznaczenie dnia (w5/wś) i całą obecność zapisujemy
        // jako nadgodziny — inaczej doliczylibyśmy 8 h normy, której tego dnia nie było.
        val next = cur.copy(
            otHours = row.otHours,
            otRate = if (row.otRate == 50) OtRate.P50 else OtRate.P100
        )
        db.dayDao().upsert(DayRow.from(next))
        db.presenceDao().setStatus(id, "accepted")
    }

    suspend fun reject(ctx: Context, id: Long) {
        AppDb.get(ctx).presenceDao().setStatus(id, "rejected")
    }
}
