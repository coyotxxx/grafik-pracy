package pl.grafik.pracy.events

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import kotlinx.coroutines.flow.first
import pl.grafik.pracy.MainActivity
import pl.grafik.pracy.R
import pl.grafik.pracy.data.AppDb
import pl.grafik.pracy.data.EventRow
import pl.grafik.pracy.data.SettingsStore
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * Przypomnienie o jutrzejszych wydarzeniach, wysyłane dnia poprzedniego
 * o godzinie ustawionej przez użytkownika (domyślnie 18:00).
 *
 * Zamiast budzika na każde wydarzenie chodzi JEDNO zadanie dziennie, które po
 * wykonaniu planuje samo siebie na kolejny dzień. Mniej rzeczy do zgubienia
 * przy restarcie telefonu i mniej pracy dla baterii.
 */
class ReminderWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext
        runCatching {
            val (on, _) = SettingsStore(app).reminders.first()
            if (on) {
                val jutro = LocalDate.now().plusDays(1)
                val ev = AppDb.get(app).eventDao().remindersFor(jutro.toString())
                if (ev.isNotEmpty()) Reminders.notify(app, jutro, ev)
                Log.i(Reminders.TAG, "przypomnienia na $jutro: ${ev.size}")
            }
        }.onFailure { Log.e(Reminders.TAG, "błąd przypomnień", it) }
        Reminders.schedule(applicationContext)          // planujemy kolejny dzień
        return Result.success()
    }
}

object Reminders {

    const val TAG = "GrafikReminder"
    const val CHANNEL = "wydarzenia"
    private const val WORK = "reminder_daily"
    private const val NOTIF_ID = 90210

    /** Ustawia zadanie na najbliższą godzinę przypomnień. */
    fun schedule(ctx: Context) {
        val hour = runCatching {
            kotlinx.coroutines.runBlocking { SettingsStore(ctx).reminders.first().second }
        }.getOrDefault(18)

        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(LocalTime.of(hour, 0))
        if (!next.isAfter(now)) next = next.plusDays(1)
        val delay = Duration.between(now, next).toMinutes().coerceAtLeast(1)

        val req = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(ctx).enqueueUniqueWork(WORK, ExistingWorkPolicy.REPLACE, req)
        Log.i(TAG, "następne przypomnienie za $delay min ($next)")
    }

    fun cancel(ctx: Context) {
        WorkManager.getInstance(ctx).cancelUniqueWork(WORK)
    }

    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) != null) return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Przypomnienia o wydarzeniach", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Wydarzenia zaplanowane na jutro" }
        )
    }

    fun notify(ctx: Context, dzien: LocalDate, ev: List<EventRow>) {
        ensureChannel(ctx)
        if (!NotificationManagerCompat.from(ctx).areNotificationsEnabled()) return

        val tytul = if (ev.size == 1) "Jutro: ${opis(ev.first())}" else "Jutro masz ${ev.size} wydarzenia"
        val tresc = ev.joinToString("\n") { "• ${opis(it)}" }

        val open = PendingIntent.getActivity(
            ctx, NOTIF_ID, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_grafik)
            .setContentTitle(tytul)
            .setContentText(tresc.lineSequence().first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(tresc))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(ctx).notify(NOTIF_ID, n) }
    }

    private fun opis(e: EventRow) = if (e.time.isBlank()) e.text else "${e.time} ${e.text}"
}
