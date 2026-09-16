package pl.grafik.pracy.location

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * Siatka bezpieczeństwa. Geofence Androida potrafi zgubić moment wyjścia —
 * ten worker co ~15 min sprawdza, czy naprawdę jeszcze jesteśmy w pracy,
 * i domyka pobyt ostatnim potwierdzonym kontaktem.
 */
class PresenceWatchdogWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        android.util.Log.i(PresenceRepo.TAG, "watchdog: start")
        runCatching { PresenceRepo.watchdog(applicationContext) }
            .onFailure { android.util.Log.e(PresenceRepo.TAG, "watchdog: błąd", it) }
        return Result.success()
    }
}

object PresenceWatchdog {
    private const val NAME = "presence_watchdog"

    fun schedule(ctx: Context) {
        val req = PeriodicWorkRequestBuilder<PresenceWatchdogWorker>(15, TimeUnit.MINUTES)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(ctx)
            .enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.UPDATE, req)
    }

    fun cancel(ctx: Context) {
        WorkManager.getInstance(ctx).cancelUniqueWork(NAME)
    }
}
