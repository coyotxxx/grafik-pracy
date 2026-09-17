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
import pl.grafik.pracy.domain.PlanPowiadomien
import pl.grafik.pracy.domain.PowiadomieniaCfg
import pl.grafik.pracy.domain.PresenceEngine
import pl.grafik.pracy.domain.Shift
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
        Reminders.planujNajblizsze(applicationContext)  // i odświeżamy punktowe na dwie doby
        return Result.success()
    }
}

/**
 * Pojedyncze powiadomienie zaplanowane na konkretną godzinę: krótko przed wydarzeniem
 * albo przed startem zmiany. Treść przychodzi w danych zadania, żeby worker nie musiał
 * znów pytać bazy — w międzyczasie wpis mógł zniknąć, a wtedy po prostu nic nie wysyłamy.
 */
class PunktowyWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val tytul = inputData.getString("tytul") ?: return Result.success()
        val tresc = inputData.getString("tresc").orEmpty()
        val id = inputData.getInt("id", 0)
        runCatching { Reminders.notifyRaw(applicationContext, id, tytul, tresc) }
            .onFailure { Log.e(Reminders.TAG, "błąd powiadomienia punktowego", it) }
        return Result.success()
    }
}

object Reminders {

    const val TAG = "GrafikReminder"
    const val CHANNEL = "wydarzenia"
    private const val WORK = "reminder_daily"
    private const val NOTIF_ID = 90210
    private const val TAG_PUNKTOWE = "grafik_punktowe"
    private const val ID_WYDARZENIE = 91000
    private const val ID_ZMIANA = 92000

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

    /** Wysyła gotowe powiadomienie — treść przygotował ten, kto je zaplanował. */
    fun notifyRaw(ctx: Context, id: Int, tytul: String, tresc: String) {
        ensureChannel(ctx)
        if (!NotificationManagerCompat.from(ctx).areNotificationsEnabled()) return
        val open = PendingIntent.getActivity(
            ctx, id, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_grafik)
            .setContentTitle(tytul)
            .setContentText(tresc)
            .setStyle(NotificationCompat.BigTextStyle().bigText(tresc))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(ctx).notify(id, n) }
    }

    /**
     * Planuje powiadomienia punktowe na najbliższe dwie doby: krótko przed wydarzeniem
     * i przed startem zmiany. Wywołujemy przy starcie aplikacji i raz dziennie z workera —
     * dzięki temu nie trzeba pilnować każdego zapisu z osobna, a plan zawsze jest świeży.
     *
     * Godziny, które wypadłyby na nocce albo zaraz po niej, przesuwa `PlanPowiadomien`.
     */
    fun planujNajblizsze(ctx: Context) {
        val wm = WorkManager.getInstance(ctx)
        wm.cancelAllWorkByTag(TAG_PUNKTOWE)
        runCatching {
            kotlinx.coroutines.runBlocking {
                val cfg = SettingsStore(ctx).powiadomienia.first()
                if (!cfg.wydarzenia && !cfg.przedZmiana) return@runBlocking

                val teraz = LocalDateTime.now()
                val od = teraz.toLocalDate()
                val doKiedy = od.plusDays(2)
                val dni = AppDb.get(ctx).dayDao().rangeOnce(od.toString(), doKiedy.toString())
                    .associate { LocalDate.parse(it.date) to it.toEntry().shift }
                val zmianaDnia: (LocalDate) -> Shift? = { dni[it] }

                if (cfg.wydarzenia && cfg.wDniu) {
                    AppDb.get(ctx).eventDao().rangeOnce(od.toString(), doKiedy.toString())
                        .forEach { e ->
                            val kiedy = PlanPowiadomien.przedWydarzeniem(
                                LocalDate.parse(e.date), e.time, cfg.wyprzedzenieMin
                            ) ?: return@forEach
                            zaplanuj(
                                wm, teraz, kiedy, cfg, zmianaDnia,
                                id = (ID_WYDARZENIE + e.id).toInt(),
                                tytul = "${e.time} — ${e.text}",
                                tresc = "Za ${PlanPowiadomien.etykietaWyprzedzenia(cfg.wyprzedzenieMin)}: ${e.text}"
                            )
                        }
                }

                if (cfg.przedZmiana) {
                    dni.forEach { (data, zmiana) ->
                        val kiedy = PlanPowiadomien.przedZmiana(data, zmiana, cfg.przedZmianaMin)
                            ?: return@forEach
                        val okno = PresenceEngine.shiftWindow(data, zmiana) ?: return@forEach
                        zaplanuj(
                            wm, teraz, kiedy, cfg, zmianaDnia,
                            id = ID_ZMIANA + data.dayOfMonth,
                            tytul = "Za ${PlanPowiadomien.etykietaWyprzedzenia(cfg.przedZmianaMin)} zmiana ${zmiana?.code}",
                            tresc = "Start ${okno.first.toLocalTime()} · koniec ${okno.second.toLocalTime()}"
                        )
                    }
                }
            }
        }.onFailure { Log.e(TAG, "nie udało się zaplanować powiadomień", it) }
    }

    private fun zaplanuj(
        wm: WorkManager,
        teraz: LocalDateTime,
        kiedy: LocalDateTime,
        cfg: PowiadomieniaCfg,
        zmianaDnia: (LocalDate) -> Shift?,
        id: Int,
        tytul: String,
        tresc: String
    ) {
        val moment = if (cfg.ciszaNaNocce) PlanPowiadomien.zCiszaNaNocce(kiedy, zmianaDnia) else kiedy
        if (!moment.isAfter(teraz)) return
        val zaIle = Duration.between(teraz, moment).toMinutes().coerceAtLeast(1)
        val req = OneTimeWorkRequestBuilder<PunktowyWorker>()
            .setInitialDelay(zaIle, TimeUnit.MINUTES)
            .addTag(TAG_PUNKTOWE)
            .setInputData(
                Data.Builder()
                    .putString("tytul", tytul)
                    .putString("tresc", tresc)
                    .putInt("id", id)
                    .build()
            )
            .build()
        wm.enqueue(req)
        Log.i(TAG, "powiadomienie [$tytul] za $zaIle min ($moment)")
    }
}
