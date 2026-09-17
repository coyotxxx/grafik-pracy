package pl.grafik.pracy.events

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import pl.grafik.pracy.data.AppDb
import pl.grafik.pracy.data.DayRow
import pl.grafik.pracy.data.SettingsStore
import pl.grafik.pracy.domain.CycleConfig
import pl.grafik.pracy.domain.CycleGenerator
import pl.grafik.pracy.domain.DayEntry
import pl.grafik.pracy.domain.PlanPowiadomien
import pl.grafik.pracy.domain.Settlement
import pl.grafik.pracy.domain.SettlementCfg
import pl.grafik.pracy.domain.Shift
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.util.concurrent.TimeUnit

/**
 * Powiadomienia, które odzywają się tylko wtedy, gdy jest po temu powód:
 * zmiana brygady w cyklu, zbliżający się limit nadgodzin i zaległy urlop.
 *
 * Jedno zadanie dziennie sprawdza wszystkie trzy warunki i planuje samo siebie
 * na kolejny dzień — tak samo jak przypomnienia o wydarzeniach.
 *
 * Zadanie uruchamia wyłącznie nowy wygląd. Klasyczna aplikacja nigdy go nie planuje,
 * więc jej zachowanie zostaje dokładnie takie, jakie było.
 */
class WarunkoweWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        runCatching { PowiadomieniaWarunkowe.sprawdz(applicationContext) }
            .onFailure { Log.e(PowiadomieniaWarunkowe.TAG, "błąd powiadomień warunkowych", it) }
        PowiadomieniaWarunkowe.zaplanuj(applicationContext)
        return Result.success()
    }
}

object PowiadomieniaWarunkowe {

    const val TAG = "GrafikWarunkowe"
    private const val WORK = "warunkowe_dzienne"

    /** Godzina sprawdzenia — wieczór, żeby „od jutra inna zmiana" trafiało w niedzielę. */
    private val GODZINA = LocalTime.of(19, 0)

    private const val ID_BRYGADA = 93001
    private const val ID_LIMIT = 93002
    private const val ID_URLOP = 93003

    /** Ustawia sprawdzenie na dziś wieczór albo, gdy pora minęła, na jutro. */
    fun zaplanuj(ctx: Context) {
        val teraz = LocalDateTime.now()
        var kiedy = teraz.toLocalDate().atTime(GODZINA)
        if (!kiedy.isAfter(teraz)) kiedy = kiedy.plusDays(1)
        val zaIle = Duration.between(teraz, kiedy).toMinutes().coerceAtLeast(1)

        val req = OneTimeWorkRequestBuilder<WarunkoweWorker>()
            .setInitialDelay(zaIle, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(ctx).enqueueUniqueWork(WORK, ExistingWorkPolicy.REPLACE, req)
        Log.i(TAG, "następne sprawdzenie warunków $kiedy (za $zaIle min)")
    }

    fun odwolaj(ctx: Context) {
        WorkManager.getInstance(ctx).cancelUniqueWork(WORK)
    }

    /** Włącza sprawdzanie, gdy choć jeden warunek jest zaznaczony, i wyłącza, gdy żaden. */
    fun ustaw(ctx: Context, cfg: pl.grafik.pracy.domain.PowiadomieniaCfg) {
        if (cfg.zmianaBrygady || cfg.limitNadgodzin || cfg.zaleglyUrlop) zaplanuj(ctx)
        else odwolaj(ctx)
    }

    /**
     * Sprawdza trzy warunki i wysyła te powiadomienia, które mają o czym mówić.
     * Wszystko liczy się z grafiku i ustawień — nic nie wychodzi z telefonu.
     */
    suspend fun sprawdz(ctx: Context, dzis: LocalDate = LocalDate.now()) {
        val ustawienia = SettingsStore(ctx)
        val cfg = ustawienia.powiadomienia.first()
        if (!cfg.zmianaBrygady && !cfg.limitNadgodzin && !cfg.zaleglyUrlop) return

        if (cfg.zmianaBrygady) {
            val cykl = ustawienia.config.first()
            val dni = grafik(ctx, cykl, dzis.minusDays(6), dzis.plusDays(1))
            PlanPowiadomien.zmianaBrygady(
                dzis = dzis,
                mijajacyTydzien = (0..6).map { dni[dzis.minusDays((6 - it).toLong())] },
                jutrzejsza = dni[dzis.plusDays(1)]
            )?.let {
                Reminders.notifyRaw(ctx, ID_BRYGADA, "Od jutra inna zmiana", it)
                Log.i(TAG, "zmiana brygady: $it")
            }
        }

        if (cfg.limitNadgodzin) {
            val okres = ustawienia.settlement.first()
            val (wykorzystane, limit) = nadgodzinyOkresu(ctx, YearMonth.from(dzis), okres)
            PlanPowiadomien.limitNadgodzin(wykorzystane, limit)?.let {
                Reminders.notifyRaw(ctx, ID_LIMIT, "Limit nadgodzin blisko", it)
                Log.i(TAG, "limit nadgodzin: $it")
            }
        }

        if (cfg.zaleglyUrlop) {
            val urlop = ustawienia.vacation.first()
            PlanPowiadomien.zaleglyUrlop(dzis, urlop.stanZalegly.takeIf { it > 0 } ?: urlop.zalegly)
                ?.let {
                    Reminders.notifyRaw(ctx, ID_URLOP, "Zaległy urlop", it)
                    Log.i(TAG, "zaległy urlop: $it")
                }
        }
    }

    /** Grafik w zakresie: cykl daje tło, ręczne wpisy mają pierwszeństwo — jak w kalendarzu. */
    private suspend fun grafik(
        ctx: Context, cykl: CycleConfig, od: LocalDate, doKiedy: LocalDate
    ): Map<LocalDate, Shift?> {
        val zapisane = AppDb.get(ctx).dayDao().rangeOnce(od.toString(), doKiedy.toString())
            .associate { LocalDate.parse(it.date) to it.toEntry() }
        val out = LinkedHashMap<LocalDate, Shift?>()
        CycleGenerator.range(cykl, od, doKiedy).forEach { (d, sh) -> out[d] = sh }
        zapisane.forEach { (d, e) -> out[d] = e.shift }
        return out
    }

    /** Nadgodziny bieżącego okresu i jego limit — te same liczby co w Bilansie. */
    private suspend fun nadgodzinyOkresu(
        ctx: Context, ym: YearMonth, okres: SettlementCfg
    ): Pair<Int, Int> {
        val biezacy = Settlement.periodOf(ym, okres)
        val rows: List<DayRow> = AppDb.get(ctx).dayDao()
            .rangeOnce(biezacy.start.toString(), biezacy.end.toString())
        val ot = rows.map { it.toEntry() }
            .filter { it.date in biezacy }
            .sumOf(DayEntry::otHours)
        return ot to Settlement.periodLimit(biezacy, okres)
    }
}
