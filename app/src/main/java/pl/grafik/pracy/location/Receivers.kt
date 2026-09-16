package pl.grafik.pracy.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import pl.grafik.pracy.data.SettingsStore
import java.time.LocalDateTime

/** Zdarzenia wejścia/wyjścia ze strefy pracy. */
class GeofenceReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return
        val app = ctx.applicationContext
        val now = LocalDateTime.now()
        val pending = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (!SettingsStore(app).workPlace.first().enabled) return@launch
                when (event.geofenceTransition) {
                    Geofence.GEOFENCE_TRANSITION_ENTER,
                    Geofence.GEOFENCE_TRANSITION_DWELL -> PresenceRepo.onEnter(app, now)
                    Geofence.GEOFENCE_TRANSITION_EXIT -> PresenceRepo.onExit(app, now)
                }
            } finally {
                pending.finish()
            }
        }
    }
}

/** Android kasuje strefy przy restarcie telefonu — odtwarzamy je. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val app = ctx.applicationContext
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val wp = SettingsStore(app).workPlace.first()
                if (wp.enabled && wp.isSet) {
                    GeofenceManager.register(app, wp)
                    PresenceWatchdog.schedule(app)
                }
            } finally {
                pending.finish()
            }
        }
    }
}

/** Przyciski „Zapisz" / „Odrzuć" z powiadomienia. */
class PresenceActionReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val id = intent.getLongExtra(PresenceNotif.EXTRA_ID, -1L)
        if (id < 0) return
        val app = ctx.applicationContext
        val accept = intent.action == PresenceNotif.ACTION_ACCEPT
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (accept) PresenceRepo.accept(app, id) else PresenceRepo.reject(app, id)
                PresenceNotif.dismiss(app, id)
            } finally {
                pending.finish()
            }
        }
    }
}
