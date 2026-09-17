package pl.grafik.pracy.nowy.widzety

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Odświeżanie widżetów.
 *
 * Widżet „DO STARTU" pokazuje odliczanie, więc rzadkie odświeżanie robiłoby z niego
 * kłamcę. Zamiast budzić telefon co kwadrans przez całą dobę, zagęszczamy odświeżanie
 * w miarę zbliżania się zmiany: blisko startu co pięć minut, w ciągu dnia co pół godziny,
 * a gdy do zmiany daleko — co dwie godziny.
 *
 * Zadanie planuje samo siebie, tak jak przypomnienia i kopia automatyczna.
 */
class WidzetyWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        runCatching { OdswiezanieWidzetow.odswiez(applicationContext) }
            .onFailure { Log.e(OdswiezanieWidzetow.TAG, "nie udało się odświeżyć widżetów", it) }
        OdswiezanieWidzetow.zaplanuj(applicationContext)
        return Result.success()
    }
}

object OdswiezanieWidzetow {

    const val TAG = "GrafikWidzety"
    private const val WORK = "widzety_odswiezanie"

    /** Odświeża wszystkie trzy widżety naraz — dane i tak czytamy jednym zapytaniem. */
    suspend fun odswiez(ctx: Context) {
        WidzetDzis().updateAll(ctx)
        WidzetDoStartu().updateAll(ctx)
        WidzetTydzien().updateAll(ctx)
    }

    /** Jak często zaglądać, zależnie od tego, ile zostało do zmiany. */
    fun odstepMinut(doStartuMin: Long): Long = when {
        doStartuMin <= 120 -> 5
        doStartuMin <= 12 * 60 -> 30
        else -> 120
    }

    fun zaplanuj(ctx: Context) {
        val zaIle = runCatching {
            kotlinx.coroutines.runBlocking { odstepMinut(DaneWidzetu.wczytaj(ctx).doStartuMin) }
        }.getOrDefault(30L)

        val req = OneTimeWorkRequestBuilder<WidzetyWorker>()
            .setInitialDelay(zaIle, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(ctx).enqueueUniqueWork(WORK, ExistingWorkPolicy.REPLACE, req)
        Log.i(TAG, "następne odświeżenie widżetów za $zaIle min")
    }

    fun odwolaj(ctx: Context) {
        WorkManager.getInstance(ctx).cancelUniqueWork(WORK)
    }
}
