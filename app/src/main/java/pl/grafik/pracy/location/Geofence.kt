package pl.grafik.pracy.location

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import pl.grafik.pracy.domain.WorkPlace

/** Rejestracja i zdejmowanie strefy wokół miejsca pracy. */
object GeofenceManager {

    const val ID = "miejsce_pracy"
    const val ACTION = "pl.grafik.pracy.GEOFENCE"

    fun hasFineLocation(ctx: Context): Boolean =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    fun hasBackgroundLocation(ctx: Context): Boolean =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) hasFineLocation(ctx)
        else ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private fun pendingIntent(ctx: Context): PendingIntent {
        val i = Intent(ctx, GeofenceReceiver::class.java).setAction(ACTION)
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) flags = flags or PendingIntent.FLAG_MUTABLE
        return PendingIntent.getBroadcast(ctx, 1001, i, flags)
    }

    /** @return komunikat błędu albo null, gdy się udało. */
    @SuppressLint("MissingPermission")
    fun register(ctx: Context, wp: WorkPlace): String? {
        if (!wp.enabled || !wp.isSet) return "Miejsce pracy nie jest ustawione"
        if (!hasBackgroundLocation(ctx)) return "Brak uprawnienia do lokalizacji w tle"

        val fence = Geofence.Builder()
            .setRequestId(ID)
            .setCircularRegion(wp.lat, wp.lon, wp.radiusM.toFloat())
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(
                Geofence.GEOFENCE_TRANSITION_ENTER or
                    Geofence.GEOFENCE_TRANSITION_EXIT or
                    Geofence.GEOFENCE_TRANSITION_DWELL
            )
            // Zwłoka na DWELL — przejazd obok zakładu nie ma zrobić dnia pracy.
            .setLoiteringDelay(wp.minStayMin * 60_000)
            .setNotificationResponsiveness(60_000)
            .build()

        val req = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER or GeofencingRequest.INITIAL_TRIGGER_DWELL)
            .addGeofence(fence)
            .build()

        return runCatching {
            LocationServices.getGeofencingClient(ctx).addGeofences(req, pendingIntent(ctx))
            null
        }.getOrElse { it.message ?: "Nie udało się włączyć strefy" }
    }

    fun unregister(ctx: Context) {
        runCatching { LocationServices.getGeofencingClient(ctx).removeGeofences(pendingIntent(ctx)) }
    }
}
