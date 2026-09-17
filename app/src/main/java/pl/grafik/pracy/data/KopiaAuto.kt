package pl.grafik.pracy.data

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit

/**
 * Automatyczna kopia raz w tygodniu, w niedzielę w nocy, do pamięci aplikacji.
 *
 * Zadanie po wykonaniu planuje samo siebie na kolejną niedzielę — tak samo jak
 * przypomnienia o wydarzeniach. Mniej rzeczy do zgubienia przy restarcie telefonu.
 */
class KopiaWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext
        runCatching {
            if (SettingsStore(app).kopiaAuto.first()) {
                Kopia.zrobKopieLokalna(app)
            }
        }.onFailure { Log.e(KopiaAuto.TAG, "błąd kopii automatycznej", it) }
        KopiaAuto.zaplanuj(app)
        return Result.success()
    }
}

object KopiaAuto {

    const val TAG = "GrafikKopiaAuto"
    private const val WORK = "kopia_auto"

    /** Godzina, o której robi się kopia — noc z soboty na niedzielę już minęła. */
    private val GODZINA = LocalTime.of(3, 0)

    /** Ustawia zadanie na najbliższą niedzielę. */
    fun zaplanuj(ctx: Context) {
        val teraz = LocalDateTime.now()
        var kiedy = teraz.toLocalDate()
            .with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            .atTime(GODZINA)
        if (!kiedy.isAfter(teraz)) kiedy = kiedy.plusWeeks(1)
        val zaIle = Duration.between(teraz, kiedy).toMinutes().coerceAtLeast(1)

        val req = OneTimeWorkRequestBuilder<KopiaWorker>()
            .setInitialDelay(zaIle, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(ctx).enqueueUniqueWork(WORK, ExistingWorkPolicy.REPLACE, req)
        Log.i(TAG, "następna kopia automatyczna $kiedy (za $zaIle min)")
    }

    fun odwolaj(ctx: Context) {
        WorkManager.getInstance(ctx).cancelUniqueWork(WORK)
        Log.i(TAG, "kopia automatyczna wyłączona")
    }

    /** Włącza albo wyłącza zadanie zgodnie z ustawieniem. */
    fun ustaw(ctx: Context, wlaczona: Boolean) {
        if (wlaczona) zaplanuj(ctx) else odwolaj(ctx)
    }
}
