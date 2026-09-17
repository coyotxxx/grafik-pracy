package pl.grafik.pracy.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pl.grafik.pracy.data.*
import pl.grafik.pracy.domain.*
import pl.grafik.pracy.location.GeofenceManager
import pl.grafik.pracy.location.PresenceState
import pl.grafik.pracy.location.PresenceWatchdog
import pl.grafik.pracy.events.Reminders
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

enum class Tool { I, II, III, W5, WS, DWN, BWN, URLOP, L4, OT, DEV, ERASE }

/**
 * Obecność wykryta w danym dniu — to, co kalendarz wypisuje w rogu kafelka.
 * Liczba godzin bierze się z zaliczonego czasu, nie z samego pobytu w strefie.
 */
data class DayPresence(
    val hours: Int = 0,
    val trwa: Boolean = false,
    /** Czy wszystkie wpisy tego dnia są wpisane ręcznie, a nie wykryte. */
    val reczne: Boolean = false,
    val od: LocalDateTime? = null,
    val doKiedy: LocalDateTime? = null
)

data class UiState(
    val ym: YearMonth = YearMonth.now(),
    val entries: Map<LocalDate, DayEntry> = emptyMap(),
    /** Wydarzenia w całej widocznej siatce, także z sąsiednich miesięcy. */
    val events: Map<LocalDate, List<EventRow>> = emptyMap(),
    val cfg: CycleConfig = CycleConfig(),
    val colors: Map<String, String> = Palette0.defaults,
    val tool: Tool = Tool.OT,
    val otHours: Int = 8,
    val otRate: OtRate = OtRate.P100,
    val stats: MonthStats = MonthStats(),
    val remindOn: Boolean = true,
    val remindHour: Int = 18,
    /** Czy dotknięcie dnia zmienia grafik. Domyślnie NIE — żeby nie malować przez przypadek. */
    val painting: Boolean = false,
    val urlop: VacationCfg = VacationCfg(),
    /** Wszystkie dni urlopu w roku wyświetlanego miesiąca. */
    val urlopRok: List<LocalDate> = emptyList(),
    /** Dni urlopu w roku poprzednim — do podpowiedzi, ile przeszło na ten rok. */
    val urlopPoprzedniRok: List<LocalDate> = emptyList(),
    val motyw: pl.grafik.pracy.ui.theme.PaletteTheme = pl.grafik.pracy.ui.theme.PaletteTheme.OBECNA,
    /** Jak zakład rozlicza czas pracy: długość okresu i normy. */
    val okres: SettlementCfg = SettlementCfg(),
    /** Nadgodziny każdego okresu roku — po jednym pasku na kwartał. */
    /** Godziny wykrytej obecności w dniach widocznej siatki. */
    val obecnosc: Map<LocalDate, DayPresence> = emptyMap(),
    val okresy: List<PeriodStats> = emptyList(),
    /**
     * Dni wpisane ręcznie — reszta pochodzi z cyklu.
     * Tryb edycji w nowym wyglądzie zaznacza je pierścieniem, żeby było widać,
     * co już nadpisałeś, a czego cykl jeszcze pilnuje.
     */
    val reczne: Set<LocalDate> = emptySet(),
    /** Nadgodziny w całym roku i limit roczny, czyli suma limitów okresów. */
    val otRok: Int = 0,
    val otLimitRok: Int = 0,
    /** Kolizje odpoczynku dobowego w widocznej siatce — art. 132 KP. */
    val kolizje: List<KolizjaOdpoczynku> = emptyList(),
    /** Odpoczynek tygodniowy w miesiącu — art. 133 KP. */
    val tygodnieOdpoczynku: List<TydzienOdpoczynku> = emptyList(),
    /** Znacznik kolizji na kafelku dnia i ostrzeżenie przy malowaniu. */
    val restZnacznik: Boolean = true,
    val restOstrzegaj: Boolean = true
) {
    /**
     * Bilans urlopu w roku wyświetlanego miesiąca.
     * Zużywamy najpierw pulę zaległą — to ona ma termin 30 września.
     */
    val urlopBilans: VacationBalance
        get() {
            val stan = urlop.stanData
            return if (stan != null && stan.year == ym.year) VacationBalance(
                bazaZalegly = urlop.stanZalegly,
                bazaBiezacy = urlop.stanBiezacy,
                zuzyte = urlopRok.count { it.isAfter(stan) },
                rok = ym.year
            ) else VacationBalance(
                bazaZalegly = urlop.zalegly,
                bazaBiezacy = urlop.wymiar,
                zuzyte = urlopRok.size,
                rok = ym.year
            )
        }

    /** Ile dni urlopu zostało niewykorzystanych w poprzednim roku — podpowiedź do pola „zaległy". */
    val urlopSugestiaZaleglego: Int
        get() = (urlop.wymiar - urlopPoprzedniRok.size).coerceAtLeast(0)

    /** Urlop w wyświetlanym miesiącu — do listy z datami. */
    val urlopMiesiaca: List<LocalDate> get() = urlopRok.filter { YearMonth.from(it) == ym }
}

private object Palette0 { val defaults = pl.grafik.pracy.ui.theme.Palette.defaults }

class Vm(app: Application) : AndroidViewModel(app) {

    private val dao = AppDb.get(app).dayDao()
    private val events = AppDb.get(app).eventDao()
    private val settings = SettingsStore(app)

    private val _ym = MutableStateFlow(YearMonth.now())
    private val _tool = MutableStateFlow(Tool.OT)
    private val _otH = MutableStateFlow(8)
    private val _otR = MutableStateFlow(OtRate.P100)
    private val _maluj = MutableStateFlow(false)
    private val undoStack = ArrayDeque<Pair<LocalDate, DayEntry?>>()

    init {
        // Przypomnienia planujemy przy każdym starcie — WorkManager sam pilnuje, żeby był jeden.
        viewModelScope.launch {
            if (settings.reminders.first().first) Reminders.schedule(getApplication())
        }
    }

    /** Siatka miesiąca rozciągnięta do pełnych tygodni — stąd dni z sąsiednich miesięcy. */
    private fun gridRange(ym: YearMonth): Pair<LocalDate, LocalDate> {
        val first = ym.atDay(1)
        val start = first.minusDays(((first.dayOfWeek.value + 6) % 7).toLong())
        val last = ym.atEndOfMonth()
        val end = last.plusDays((7 - last.dayOfWeek.value).toLong())
        return start to end
    }

    val state: StateFlow<UiState> = combine(
        _ym, _tool, _otH, _otR, settings.config, settings.colors,
        _ym.flatMapLatest { ym ->
            val (a, b) = gridRange(ym)
            dao.observeRange(a.toString(), b.toString())
        },
        _ym.flatMapLatest { ym ->
            val (a, b) = gridRange(ym)
            events.observeRange(a.toString(), b.toString())
        },
        settings.reminders,
        _maluj,
        settings.vacation,
        _ym.flatMapLatest { ym ->
            dao.observeWithShift(Shift.URLOP.code, "${ym.year}-01-01", "${ym.year}-12-31")
        },
        _ym.flatMapLatest { ym ->
            dao.observeWithShift(Shift.URLOP.code, "${ym.year - 1}-01-01", "${ym.year - 1}-12-31")
        },
        settings.motyw,
        settings.settlement,
        _ym.flatMapLatest { ym ->
            val (a, b) = gridRange(ym)
            AppDb.get(getApplication()).presenceDao().observeRange(a.toString(), b.toString())
        },
        pl.grafik.pracy.location.PresenceState.openEnterFlow(getApplication()),
        // Cały rok — z tego liczymy i okres rozliczeniowy, i limit roczny nadgodzin.
        _ym.flatMapLatest { ym ->
            dao.observeRange("${ym.year}-01-01", "${ym.year}-12-31")
        },
        settings.odpoczynek
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        val ym = arr[0] as YearMonth
        val tool = arr[1] as Tool
        val otH = arr[2] as Int
        val otR = arr[3] as OtRate
        val cfg = arr[4] as CycleConfig
        val cols = (arr[5] as Map<String, String>).ifEmpty { pl.grafik.pracy.ui.theme.Palette.defaults }
        val rows = arr[6] as List<DayRow>
        val evRows = arr[7] as List<EventRow>
        @Suppress("UNCHECKED_CAST")
        val rem = arr[8] as Pair<Boolean, Int>
        val maluj = arr[9] as Boolean
        val url = arr[10] as VacationCfg
        @Suppress("UNCHECKED_CAST")
        val urlRok = (arr[11] as List<DayRow>).map { LocalDate.parse(it.date) }
        @Suppress("UNCHECKED_CAST")
        val urlPrev = (arr[12] as List<DayRow>).map { LocalDate.parse(it.date) }
        val motyw = arr[13] as pl.grafik.pracy.ui.theme.PaletteTheme
        val okres = arr[14] as SettlementCfg
        @Suppress("UNCHECKED_CAST")
        val presRows = arr[15] as List<PresenceRow>
        val otwartyPobyt = arr[16] as LocalDateTime?
        @Suppress("UNCHECKED_CAST")
        val rokRows = arr[17] as List<DayRow>
        @Suppress("UNCHECKED_CAST")
        val rest = arr[18] as Pair<Boolean, Boolean>

        val okresy = calcOkresy(rokRows, ym, cfg, okres)

        val (gStart, gEnd) = gridRange(ym)
        val saved = rows.associate { LocalDate.parse(it.date) to it.toEntry() }
        // Pusta aplikacja pokazuje tylko to, co użytkownik sam wpisał.
        val gen = CycleGenerator.range(cfg, gStart, gEnd)
        val merged = LinkedHashMap<LocalDate, DayEntry>()
        gen.forEach { (d, s) -> merged[d] = saved[d] ?: DayEntry(date = d, shift = s) }
        saved.forEach { (d, e) -> merged[d] = e }

        val ev = evRows.groupBy { LocalDate.parse(it.date) }
        // Statystyki liczymy TYLKO z bieżącego miesiąca, mimo że siatka pokazuje więcej.
        val wMiesiacu = merged.filterKeys { YearMonth.from(it) == ym }

        UiState(ym, merged, ev, cfg, cols, tool, otH, otR, calc(wMiesiacu, ym), rem.first, rem.second,
            maluj, url, urlRok, urlPrev, motyw, okres,
            obecnosc(presRows, otwartyPobyt, merged), okresy.first, saved.keys, okresy.second,
            Settlement.yearLimit(ym.year, okres),
            Odpoczynek.kolizjeDobowe(merged),
            Odpoczynek.tygodnie(merged, ym.atDay(1), ym.atEndOfMonth()),
            rest.first, rest.second)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState())

    private fun calc(m: Map<LocalDate, DayEntry>, ym: YearMonth): MonthStats {
        var worked = 0; var ot100 = 0; var ot50 = 0
        var dw = 0; var df = 0; var sun = 0; var hol = 0; var sat = 0
        val by = mutableMapOf<Shift, Int>()
        val dni = mutableMapOf<Shift, Int>()
        var urlopH = 0
        val dzis = LocalDate.now()
        var doDzis = 0
        m.values.forEach { e ->
            worked += e.workedHours
            // Do dziś włącznie — żeby w trwającym miesiącu nie liczyć dni, których jeszcze nie było.
            if (!e.date.isAfter(dzis)) doDzis += e.workedHours + (if (e.shift == Shift.URLOP) 8 else 0)
            if (e.otRate == OtRate.P100) ot100 += e.otHours else ot50 += e.otHours
            if (e.shift?.isWork == true) {
                dw++
                by[e.shift] = (by[e.shift] ?: 0) + 8
                dni[e.shift] = (dni[e.shift] ?: 0) + 1
                when (Holidays.kindOf(e.date)) {
                    DayKind.NIEDZIELA -> sun++
                    DayKind.SWIETO -> hol++
                    DayKind.SOBOTA -> sat++
                    else -> {}
                }
            } else {
                df++
                // Dzień urlopu zabiera dzień z puli, więc musi też pokryć 8 h.
                // Warunkowanie tego cyklem robiło niekonsekwencję: dzień znikał z puli,
                // ale nie pokrywał godzin.
                if (e.shift == Shift.URLOP) urlopH += 8
            }
        }
        // Miesiąc liczymy normą ustawową — zakład podaje godziny na cały okres,
        // a rozbijanie tej liczby na miesiące byłoby zmyślaniem.
        return MonthStats(worked, Settlement.statutoryNorm(ym),
            ot100, ot50, dw, df, sun, hol, sat, by, dni, urlopH, doDzis,
            biezacyMiesiac = YearMonth.from(dzis) == ym)
    }

    /**
     * Obecność do wypisania na kafelkach. Odrzucone wykrycia pomijamy — odrzucenie
     * znaczy „to nie była prawda", więc nie ma czego oznaczać.
     */
    private fun obecnosc(
        rows: List<PresenceRow>,
        otwarty: LocalDateTime?,
        dni: Map<LocalDate, DayEntry>
    ): Map<LocalDate, DayPresence> {
        val out = HashMap<LocalDate, DayPresence>()
        rows.forEach { r ->
            if (r.status == "rejected") return@forEach
            val d = runCatching { LocalDate.parse(r.date) }.getOrNull() ?: return@forEach
            val h = PresenceEngine.countedHours(
                runCatching { LocalDateTime.parse(r.countedFrom) }.getOrNull(),
                runCatching { LocalDateTime.parse(r.countedTo) }.getOrNull()
            )
            val od = runCatching { LocalDateTime.parse(r.countedFrom) }.getOrNull()
            val doK = runCatching { LocalDateTime.parse(r.countedTo) }.getOrNull()
            val było = out[d]
            out[d] = DayPresence(
                hours = (było?.hours ?: 0) + h,
                trwa = było?.trwa ?: false,
                reczne = (było?.reczne ?: true) && r.source == "manual",
                od = listOfNotNull(było?.od, od).minOrNull(),
                doKiedy = listOfNotNull(było?.doKiedy, doK).maxOrNull()
            )
        }
        // Trwający pobyt trafia na ten dzień grafiku, do którego należy — po nocce
        // wejście nad ranem to jeszcze dzień poprzedni.
        otwarty?.let { enter ->
            val d = PresenceEngine.assignDate(PresenceSpan(enter, enter)) { dzien -> dni[dzien]?.shift }
            out[d] = (out[d] ?: DayPresence()).copy(trwa = true)
        }
        return out
    }

    /**
     * Nadgodziny w rozbiciu na okresy roku plus suma roczna.
     * Cały rok bierzemy jednym zapytaniem — okres i tak nigdy nie przechodzi przez granicę roku.
     */
    private fun calcOkresy(
        rokRows: List<DayRow>, ym: YearMonth, cfg: CycleConfig, okres: SettlementCfg
    ): Pair<List<PeriodStats>, Int> {
        val rokOd = LocalDate.of(ym.year, 1, 1)
        val rokDo = LocalDate.of(ym.year, 12, 31)

        // Ten sam sposób scalania co w kalendarzu: cykl daje tło, wpisy ręczne mają pierwszeństwo.
        val zapisane = rokRows.associate { LocalDate.parse(it.date) to it.toEntry() }
        val dni = LinkedHashMap<LocalDate, DayEntry>()
        CycleGenerator.range(cfg, rokOd, rokDo).forEach { (d, sh) -> dni[d] = zapisane[d] ?: DayEntry(date = d, shift = sh) }
        zapisane.forEach { (d, e) -> dni[d] = e }

        val okresyRoku = Settlement.periodsOfYear(ym.year, okres)
        val biezacy = Settlement.periodOf(ym, okres)
        val nadgodziny = HashMap<YearMonth, Int>()
        var otRok = 0
        dni.forEach { (d, e) ->
            if (e.otHours == 0) return@forEach
            otRok += e.otHours
            okresyRoku.firstOrNull { d in it }?.let { p ->
                nadgodziny[p.from] = (nadgodziny[p.from] ?: 0) + e.otHours
            }
        }

        val lista = okresyRoku.map { p ->
            PeriodStats(
                period = p,
                ot = nadgodziny[p.from] ?: 0,
                otLimit = Settlement.otLimit(p),
                otLimitZakl = Settlement.otLimitCompany(p, okres),
                biezacy = p.from == biezacy.from
            )
        }
        return lista to otRok
    }

    /** „Byłem w pracy" z karty dnia — godziny bierzemy ze zmiany, bez wpisywania. */
    fun oznaczObecnosc(d: LocalDate, byl: Boolean) = viewModelScope.launch {
        val app = getApplication<android.app.Application>()
        if (byl) pl.grafik.pracy.location.PresenceRepo.markManual(app, d)
        else pl.grafik.pracy.location.PresenceRepo.clearManual(app, d)
    }

    fun saveSettlement(c: SettlementCfg) = viewModelScope.launch { settings.saveSettlement(c) }

    fun setMonth(ym: YearMonth) { _ym.value = ym }
    fun prevMonth() { _ym.value = _ym.value.minusMonths(1) }
    fun nextMonth() { _ym.value = _ym.value.plusMonths(1) }
    /** Wybór narzędzia to jasna intencja — od razu odblokowuje malowanie. */
    fun pick(t: Tool) { _tool.value = t; _maluj.value = true }

    fun setPainting(on: Boolean) { _maluj.value = on }

    // --- podpowiedzi, kiedy wziąć urlop ---

    private val _plan = MutableStateFlow<List<VacationSuggestion>>(emptyList())
    val plan: StateFlow<List<VacationSuggestion>> = _plan.asStateFlow()

    /**
     * Liczy propozycje na najbliższy rok. Wolne bierzemy z grafiku: ręczne wpisy mają
     * pierwszeństwo, reszta z cyklu. Bez włączonego cyklu nie ma z czego liczyć.
     */
    fun policzPlan() = viewModelScope.launch {
        val cfg = settings.config.first()
        if (!cfg.generate) { _plan.value = emptyList(); return@launch }

        val dzis = LocalDate.now()
        var od = maxOf(dzis, cfg.genFrom?.atDay(1) ?: dzis)
        val doDnia = minOf(dzis.plusYears(1), cfg.genTo?.atEndOfMonth() ?: dzis.plusYears(1))
        if (od.isAfter(doDnia)) { _plan.value = emptyList(); return@launch }

        val zapisane = dao.observeRange(od.toString(), doDnia.toString()).first()
            .associate { LocalDate.parse(it.date) to it.toEntry() }
        val st = state.value
        val bilans = st.urlopBilans

        _plan.value = VacationPlanner.zaproponuj(
            od = od,
            do_ = doDnia,
            wolny = { d ->
                val e = zapisane[d]
                if (e != null) e.shift?.isWork != true
                else !CycleGenerator.shiftFor(cfg, d).isWork
            },
            swieto = { Holidays.isHoliday(it) },
            budzet = bilans.zostalo
        )
    }

    fun saveVacation(v: VacationCfg) = viewModelScope.launch { settings.saveVacation(v) }

    /** Jak pokazywać kolizje odpoczynku — znacznik w kalendarzu i ostrzeżenie przy malowaniu. */
    fun saveOdpoczynek(znacznik: Boolean, ostrzegaj: Boolean) =
        viewModelScope.launch { settings.saveOdpoczynek(znacznik, ostrzegaj) }

    /**
     * Dni od wczoraj na trzy tygodnie w przód. Ekran „Teraz" w nowym wyglądzie odlicza
     * do startu najbliższej zmiany i pokazuje pięć kolejnych dni — a te potrafią wypaść
     * poza siatkę wyświetlanego miesiąca, której pilnuje `state`.
     *
     * Kolejność jak w kalendarzu: wpis własny wygrywa z cyklem.
     */
    val najblizszeDni: StateFlow<List<DayEntry>> = run {
        val od = LocalDate.now().minusDays(1)
        val doKiedy = od.plusDays(23)
        combine(
            settings.config,
            dao.observeRange(od.toString(), doKiedy.toString())
        ) { cfg, rows ->
            val saved = rows.associate { LocalDate.parse(it.date) to it.toEntry() }
            val merged = LinkedHashMap<LocalDate, DayEntry>()
            CycleGenerator.range(cfg, od, doKiedy).forEach { (d, s) ->
                merged[d] = saved[d] ?: DayEntry(date = d, shift = s)
            }
            saved.forEach { (d, e) -> merged[d] = e }
            merged.values.sortedBy { it.date }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    /** Wejście w podsumowanie — zawsze startujemy od bieżącego miesiąca. */
    fun onEnterSummary() { _ym.value = YearMonth.now() }

    /** Wejście na zakładkę Miesiąc: wracamy do dziś i blokujemy malowanie. */
    fun onEnterCalendar() {
        _ym.value = YearMonth.now()
        _maluj.value = false
    }
    fun otPlus() { _tool.value = Tool.OT; _otH.value = (_otH.value + 2).coerceAtMost(12) }
    fun otMinus() { _tool.value = Tool.OT; _otH.value = (_otH.value - 2).coerceAtLeast(2) }
    fun toggleRate() { _tool.value = Tool.OT; _otR.value = if (_otR.value == OtRate.P100) OtRate.P50 else OtRate.P100 }

    /** Jedno dotknięcie dnia — malowanie wybranym narzędziem. Nic nie robi przy blokadzie. */
    fun tap(d: LocalDate) = viewModelScope.launch {
        if (!_maluj.value) return@launch
        val cur = state.value.entries[d] ?: DayEntry(date = d)
        undoStack.addLast(d to dao.get(d.toString())?.toEntry())
        if (undoStack.size > 60) undoStack.removeFirst()

        val next = when (_tool.value) {
            Tool.ERASE -> DayEntry(date = d, shift = null)
            Tool.OT -> {
                val same = cur.otHours == _otH.value && cur.otRate == _otR.value
                cur.copy(otHours = if (same) 0 else _otH.value, otRate = _otR.value)
            }
            Tool.DEV -> cur.copy(deviation = !cur.deviation)
            Tool.I -> cur.copy(shift = Shift.I)
            Tool.II -> cur.copy(shift = Shift.II)
            Tool.III -> cur.copy(shift = Shift.III)
            Tool.W5 -> cur.copy(shift = Shift.W5)
            Tool.WS -> cur.copy(shift = Shift.WS)
            Tool.DWN -> cur.copy(shift = Shift.DWN)
            Tool.BWN -> cur.copy(shift = Shift.BWN)
            Tool.URLOP -> cur.copy(shift = Shift.URLOP)
            Tool.L4 -> cur.copy(shift = Shift.L4)
        }
        dao.upsert(DayRow.from(next))
    }

    /**
     * Ustawia zmianę wprost na wskazanym dniu — bez wybierania narzędzia i malowania.
     * Tego potrzebuje karta dnia w nowym wyglądzie: segmenty I/II/III/Urlop/Wolne
     * działają na jeden dotyk, a nie „wybierz pędzel, potem maluj".
     */
    fun setShift(d: LocalDate, shift: Shift?) = viewModelScope.launch {
        val cur = state.value.entries[d] ?: DayEntry(date = d)
        zapamietajDoCofniecia(d)
        dao.upsert(DayRow.from(cur.copy(shift = shift)))
    }

    /**
     * Ustawia dokładną liczbę nadgodzin i stawkę na dniu.
     * Stepper narzędzia chodzi co 2 h w zakresie 2–12, a karta dnia potrzebuje
     * kroku 1 od zera — stąd osobna funkcja zamiast obchodzenia tamtej.
     */
    fun setOvertime(d: LocalDate, godziny: Int, stawka: OtRate) = viewModelScope.launch {
        val cur = state.value.entries[d] ?: DayEntry(date = d)
        zapamietajDoCofniecia(d)
        dao.upsert(DayRow.from(cur.copy(otHours = godziny.coerceIn(0, 12), otRate = stawka)))
    }

    /**
     * Malowanie dnia w trybie edycji: typ dnia i — opcjonalnie — nadgodziny,
     * jednym zapisem i jednym krokiem cofania. `nadgodziny = null` zostawia je bez zmian.
     */
    fun paintDay(d: LocalDate, shift: Shift?, nadgodziny: Int? = null, stawka: OtRate = OtRate.P100) =
        viewModelScope.launch {
            val cur = state.value.entries[d] ?: DayEntry(date = d)
            zapamietajDoCofniecia(d)
            val next = cur.copy(
                shift = shift,
                otHours = nadgodziny?.coerceIn(0, 12) ?: cur.otHours,
                otRate = if (nadgodziny != null) stawka else cur.otRate
            )
            dao.upsert(DayRow.from(next))
        }

    /** Wspólny zapis stanu sprzed zmiany — żeby „Cofnij" działało tak samo dla każdej drogi. */
    private suspend fun zapamietajDoCofniecia(d: LocalDate) {
        undoStack.addLast(d to dao.get(d.toString())?.toEntry())
        if (undoStack.size > 60) undoStack.removeFirst()
    }

    fun undo() = viewModelScope.launch {
        val last = undoStack.removeLastOrNull() ?: return@launch
        val (d, prev) = last
        if (prev == null) dao.delete(d.toString()) else dao.upsert(DayRow.from(prev))
    }

    /** Przywraca cały miesiąc do grafiku wyliczonego z cyklu. */
    fun resetMonth() = viewModelScope.launch {
        val ym = _ym.value
        dao.clearRange(ym.atDay(1).toString(), ym.atEndOfMonth().toString())
    }

    /**
     * Czyści aplikację do stanu jak po instalacji: grafik, historię wykryć i wszystkie
     * ustawienia. Strefa wokół pracy jest zdejmowana, żeby nie zbierała danych po cichu.
     */
    fun clearEverything() = viewModelScope.launch {
        val app = getApplication<Application>()
        GeofenceManager.unregister(app)
        PresenceWatchdog.cancel(app)
        PresenceState.clear(app)
        val db = AppDb.get(app)
        db.presenceDao().clearAll()
        db.eventDao().clearAll()
        db.dayDao().clearAll()
        Reminders.cancel(app)
        settings.clearAll()
        undoStack.clear()
    }

    fun saveConfig(c: CycleConfig) = viewModelScope.launch { settings.saveConfig(c) }

    fun setReminders(on: Boolean, hour: Int) = viewModelScope.launch {
        settings.saveReminders(on, hour)
        if (on) Reminders.schedule(getApplication()) else Reminders.cancel(getApplication())
    }

    // --- wydarzenia ---

    fun addEvent(d: LocalDate, time: String, text: String, remind: Boolean) = viewModelScope.launch {
        if (text.isBlank()) return@launch
        events.upsert(EventRow(date = d.toString(), time = time.trim(), text = text.trim(), remind = remind))
        Reminders.schedule(getApplication())
    }

    fun updateEvent(e: EventRow) = viewModelScope.launch {
        events.update(e)
        Reminders.schedule(getApplication())
    }

    fun deleteEvent(id: Long) = viewModelScope.launch {
        events.delete(id)
        Reminders.schedule(getApplication())
    }
    fun saveMotyw(m: pl.grafik.pracy.ui.theme.PaletteTheme) = viewModelScope.launch { settings.saveMotyw(m) }

    fun saveColors(m: Map<String, String>) = viewModelScope.launch { settings.saveColors(m) }
}
