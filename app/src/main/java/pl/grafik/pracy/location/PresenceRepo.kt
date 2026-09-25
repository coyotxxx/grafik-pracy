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
        /** Podmieniane w testach — prawdziwe SSID bywa niedostępne w tle. */
        wifi: (String) -> Boolean = { ssid -> WifiCheck.isAtWork(ctx, ssid) },
        // probe zostaje ostatni, żeby wywołania z nawiasem klamrowym dalej działały.
        probe: suspend () -> Location? = { LocationProbe.current(ctx) }
    ) {
        val wp = SettingsStore(ctx).workPlace.first()
        if (!wp.enabled) { Log.i(TAG, "watchdog: wykrywanie wyłączone"); return }
        val enter = PresenceState.openEnter(ctx)
        if (enter == null) {
            // Firmowa sieć jest samodzielnym dowodem obecności. Wcześniej Wi-Fi tylko
            // potwierdzało pobyt otwarty przez geofence — gdy Android zgubił wejście
            // albo strefy w ogóle nie było, telefon wisiał na firmowym Wi-Fi,
            // a apka twierdziła, że nie ma Cię w pracy.
            if (wp.hasWifi && wifi(wp.ssid)) {
                Log.i(TAG, "watchdog: firmowe Wi-Fi bez otwartego pobytu — otwieram")
                PresenceState.markEnter(ctx, now)
            } else {
                Log.i(TAG, "watchdog: brak otwartego pobytu")
            }
            return
        }

        // 1. Firmowe Wi-Fi — najtańsze i najpewniejsze potwierdzenie.
        if (wp.hasWifi && wifi(wp.ssid)) {
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

        // Krótkie wyjście sklejamy z poprzednim pobytem. Bez tego mignięcie geofence
        // rozcinało jedną obecność na dwie — Maciej dostał wpisy 13:45–18:25
        // i 18:26–22:05, czyli rozdzielone JEDNĄ minutą (zgłoszenie z 25.09.2026).
        // Ustawienie „krótkie wyjście sklejane do X min" istniało od początku,
        // ale nic go nie czytało.
        val doScalenia = znajdzDoScalenia(ctx, enter, wp)
        if (doScalenia != null) {
            val odKiedy = runCatching { LocalDateTime.parse(doScalenia.enterAt) }.getOrNull()
            if (odKiedy != null) {
                AppDb.get(ctx).presenceDao().delete(doScalenia.id)
                Log.i(TAG, "sklejam z pobytem #${doScalenia.id} (przerwa ${
                    java.time.Duration.between(
                        LocalDateTime.parse(doScalenia.exitAt), enter
                    ).toMinutes()
                } min)")
                return closeSpanZ(ctx, odKiedy, exitAt, wp, source)
            }
        }
        return closeSpanZ(ctx, enter, exitAt, wp, source)
    }

    /**
     * Ostatni pobyt, z którym nowy powinien się skleić: zakończony nie dawniej
     * niż `mergeGapMin` minut przed nowym wejściem.
     */
    private suspend fun znajdzDoScalenia(
        ctx: Context, enter: LocalDateTime, wp: WorkPlace
    ): pl.grafik.pracy.data.PresenceRow? {
        val dao = AppDb.get(ctx).presenceDao()
        val kandydaci = (dao.forDate(enter.toLocalDate().toString()) +
            dao.forDate(enter.toLocalDate().minusDays(1).toString()))
            .filter { it.status != "rejected" }
        return kandydaci
            .mapNotNull { r ->
                val koniec = runCatching { LocalDateTime.parse(r.exitAt) }.getOrNull()
                if (koniec == null || koniec.isAfter(enter)) null else r to koniec
            }
            .filter { (_, koniec) -> PresenceEngine.czyScalic(koniec, enter, wp.mergeGapMin) }
            .maxByOrNull { (_, koniec) -> koniec }
            ?.first
    }

    /** Właściwy zapis pobytu — po ewentualnym sklejeniu z poprzednim. */
    private suspend fun closeSpanZ(
        ctx: Context, enter: LocalDateTime, exitAt: LocalDateTime, wp: WorkPlace, source: String
    ): Long? {
        val span = PresenceSpan(enter, exitAt)
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

        // Zmiana dnia poprzedniego rozstrzyga, czy to pierwsza popołudniówka w bloku —
        // wtedy godziny przed jej startem idą po 100 %.
        val wczoraj = effectiveShift(ctx, cfg, date.minusDays(1))

        val analiza = PresenceEngine.analyze(
            date, span, shift, Holidays.kindOf(date), normaZajeta, wczoraj,
            policzone = wczesniejsze
        )

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
                createdAt = LocalDateTime.now().toString(),
                shiftCode = result.shift?.code ?: ""
            )
        )
        Log.i(TAG, "zapisano propozycję #$id: ${result.date} ${result.otHours}h ${result.otRate.percent}% (${'$'}source)")

        // Automatyczny zapis: wpis od razu wchodzi do grafiku, a powiadomienie
        // tylko informuje. Nadal da się go cofnąć jednym dotknięciem.
        if (wp.autoSave) {
            accept(ctx, id)
            Log.i(TAG, "automatyczny zapis #$id")
        }
        PresenceNotif.propose(ctx, id, result, zapisane = wp.autoSave)
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

    /**
     * Ręczne „byłem w pracy" dla dnia, który apka przegapiła — na przykład sprzed
     * uruchomienia wykrywania. Godzin nie wpisujemy: dzień ma zmianę, a zmiana ma okno.
     * Nadgodzin nie ruszamy — te wpisuje się osobno w grafiku.
     */
    suspend fun markManual(ctx: Context, date: LocalDate) {
        val db = AppDb.get(ctx)
        val cfg = SettingsStore(ctx).config.first()
        val shift = effectiveShift(ctx, cfg, date) ?: return
        val okno = PresenceEngine.shiftWindow(date, shift) ?: return

        clearManual(ctx, date)
        db.presenceDao().insert(
            PresenceRow(
                date = date.toString(),
                enterAt = okno.first.toString(),
                exitAt = okno.second.toString(),
                source = "manual",
                status = "accepted",
                otHours = 0,
                otRate = 100,
                countedFrom = okno.first.toString(),
                countedTo = okno.second.toString(),
                createdAt = LocalDateTime.now().toString(),
                shiftCode = shift.code
            )
        )
    }

    /** Cofnięcie ręcznego oznaczenia. Wykrytych wpisów nie tyka. */
    suspend fun clearManual(ctx: Context, date: LocalDate) {
        val dao = AppDb.get(ctx).presenceDao()
        dao.forDate(date.toString()).filter { it.source == "manual" }.forEach { dao.delete(it.id) }
    }

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

        // Zapisujemy dokładnie to, co pokazało powiadomienie: rozpoznaną zmianę i nadgodziny.
        // W dniu wolnym rozpoznana zmiana to samo oznaczenie dnia (w5/wś), więc etykieta
        // zostaje, a cała obecność idzie jako nadgodziny — normy tego dnia nie było.
        // Stare wpisy nie mają zapisanej zmiany (pusty kod) — wtedy nie ruszamy tego, co jest.
        val next = cur.copy(
            shift = row.shift ?: cur.shift,
            otHours = row.otHours,
            otRate = if (row.otRate == 50) OtRate.P50 else OtRate.P100
        )
        db.dayDao().upsert(DayRow.from(next))
        db.presenceDao().setStatus(id, "accepted")
    }

    /**
     * Odrzucenie. Gdy wpis zdążył już wejść do grafiku — bo tak działa automatyczny
     * zapis — zdejmujemy z dnia dokładnie te nadgodziny, które sam dopisał.
     * Etykiety zmiany nie ruszamy: nie wiemy, co było wcześniej, a zgadywanie
     * kasowałoby cudzą pracę.
     */
    suspend fun reject(ctx: Context, id: Long) {
        val db = AppDb.get(ctx)
        val row = db.presenceDao().byId(id)
        if (row != null && row.status == "accepted" && row.otHours > 0) {
            db.dayDao().get(row.date)?.toEntry()?.let { cur ->
                db.dayDao().upsert(
                    DayRow.from(cur.copy(otHours = (cur.otHours - row.otHours).coerceAtLeast(0)))
                )
            }
        }
        db.presenceDao().setStatus(id, "rejected")
    }
}
