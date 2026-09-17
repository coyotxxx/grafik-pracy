package pl.grafik.pracy.location

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import pl.grafik.pracy.MainActivity
import pl.grafik.pracy.R
import pl.grafik.pracy.domain.PresenceEngine
import pl.grafik.pracy.domain.PresenceResult
import pl.grafik.pracy.domain.Shift
import java.time.format.DateTimeFormatter

/** Powiadomienie z propozycją. Nic nie trafia do grafiku bez dotknięcia „Zapisz". */
object PresenceNotif {

    const val CHANNEL = "praca_wykryta"
    const val ACTION_ACCEPT = "pl.grafik.pracy.ACCEPT"
    const val ACTION_REJECT = "pl.grafik.pracy.REJECT"
    const val EXTRA_ID = "presence_id"

    /** Apka jest po polsku — nazwa miesiąca nie może zależeć od języka systemu. */
    private val PL = java.util.Locale.forLanguageTag("pl-PL")
    private val hm = DateTimeFormatter.ofPattern("HH:mm", PL)
    private val dm = DateTimeFormatter.ofPattern("d MMMM", PL)

    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) != null) return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Wykryta praca", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Propozycje wpisów wykrytych po lokalizacji"
            }
        )
    }

    fun propose(ctx: Context, id: Long, r: PresenceResult, zapisane: Boolean = false) {
        ensureChannel(ctx)
        if (NotificationManagerCompat.from(ctx).areNotificationsEnabled().not()) return

        val godz = when (r.otHours) {
            0 -> "bez nadgodzin"
            1 -> "1 godz. nadgodzin (${r.otRate.percent}%)"
            else -> "${r.otHours} godz. nadgodzin (${r.otRate.percent}%)"
        }
        val title = (if (zapisane) "Zapisano: " else "") + "Praca ${r.date.format(dm)} · $godz"
        val body = buildString {
            append("W pracy ${r.span.enter.format(hm)}–${r.span.exit.format(hm)}")
            append(" · liczone ${r.countedFrom.format(hm)}–${r.countedTo.format(hm)}")
            append(" = ${r.countedHours} h")
            append("\n${opisZmiany(r)}")
            if (r.onFreeDay) append(" · dzień wolny — cała obecność jako nadgodziny.")
            r.restHours?.let {
                append("\n⚠ Do następnej zmiany tylko $it h odpoczynku (wymagane ${PresenceEngine.MIN_REST_HOURS} h).")
            }
        }

        val open = PendingIntent.getActivity(
            ctx, id.toInt(), Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_grafik)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(open)
            .setAutoCancel(true)
            .apply {
                // Przy automatycznym zapisie nie ma czego zatwierdzać — zostaje cofnięcie.
                if (!zapisane) addAction(0, "Zapisz", action(ctx, id, ACTION_ACCEPT))
                addAction(0, if (zapisane) "Cofnij" else "Odrzuć", action(ctx, id, ACTION_REJECT))
            }
            .build()

        runCatching { NotificationManagerCompat.from(ctx).notify(id.toInt(), n) }
    }

    /** Na którą zmianę wyszło — po godzinie przyjazdu, nawet gdy grafik mówił co innego. */
    private fun opisZmiany(r: PresenceResult): String {
        val z = r.recognized
        val nazwa = if (z == Shift.III) "Zmiana III (nocka)" else "Zmiana ${z.code}"
        return "$nazwa ${z.from}–${z.to}"
    }

    private fun action(ctx: Context, id: Long, act: String): PendingIntent {
        val i = Intent(ctx, PresenceActionReceiver::class.java).setAction(act).putExtra(EXTRA_ID, id)
        return PendingIntent.getBroadcast(
            ctx, (id * 10 + if (act == ACTION_ACCEPT) 1 else 2).toInt(), i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun dismiss(ctx: Context, id: Long) =
        runCatching { NotificationManagerCompat.from(ctx).cancel(id.toInt()) }
}
