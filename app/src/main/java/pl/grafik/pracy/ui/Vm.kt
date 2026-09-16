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
import java.time.LocalDate
import java.time.YearMonth

enum class Tool { I, II, III, W5, WS, DWN, BWN, URLOP, L4, OT, DEV, ERASE }

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
    val motyw: pl.grafik.pracy.ui.theme.PaletteTheme = pl.grafik.pracy.ui.theme.PaletteTheme.OBECNA
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
        settings.motyw
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

        UiState(ym, merged, ev, cfg, cols, tool, otH, otR, calc(wMiesiacu, ym, cfg), rem.first, rem.second, maluj, url, urlRok, urlPrev, motyw)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState())

    private fun calc(m: Map<LocalDate, DayEntry>, ym: YearMonth, cfg: CycleConfig): MonthStats {
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
        return MonthStats(worked, Holidays.monthlyNorm(ym.year, ym.monthValue),
            ot100, ot50, dw, df, sun, hol, sat, by, dni, urlopH, doDzis,
            biezacyMiesiac = YearMonth.from(dzis) == ym)
    }

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

    /** Wejście na zakładkę Miesiąc: wracamy do dziś i blokujemy malowanie. */
    /** Wejście w podsumowanie — zawsze startujemy od bieżącego miesiąca. */
    fun onEnterSummary() { _ym.value = YearMonth.now() }

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
