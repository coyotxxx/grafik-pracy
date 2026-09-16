package pl.grafik.pracy.location

import android.annotation.SuppressLint
import android.content.Context
import android.os.SystemClock
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import pl.grafik.pracy.domain.WorkPlace
import kotlin.coroutines.resume

/** Odczyt bieżącej pozycji na potrzeby watchdoga. Null = nie wiadomo, gdzie jesteśmy. */
object LocationProbe {

    /** Zapas ponad promień, żeby drgania GPS nie wyrzucały ze strefy w kółko. */
    const val MARGIN_M = 100

    /** Pozycja starsza niż to jest bezużyteczna — mogłaby fałszywie potwierdzić obecność w pracy. */
    const val MAX_AGE_MIN = 15L

    const val TIMEOUT_BALANCED_MS = 8_000L
    const val TIMEOUT_HIGH_MS = 12_000L
    const val TIMEOUT_LAST_MS = 3_000L

    /**
     * Najpierw tanio (tryb oszczędny), potem dokładnie, a na końcu ostatnia znana —
     * ale tylko jeśli jest świeża. Pusty wynik znaczy „nie wiem", nie „nie ma mnie w pracy".
     */
    @SuppressLint("MissingPermission")
    suspend fun current(ctx: Context): Location? {
        if (!GeofenceManager.hasFineLocation(ctx)) return null
        val client = LocationServices.getFusedLocationProviderClient(ctx)

        // Najpierw ostatnia znana — jest darmowa i, co ważne, DZIAŁA W TLE.
        // Android dławi świeże zapytania aplikacjom w tle, a watchdog właśnie tam żyje.
        lastKnownFresh(client)?.let { return it }

        fixed(client, Priority.PRIORITY_BALANCED_POWER_ACCURACY, TIMEOUT_BALANCED_MS)?.let { return it }
        return fixed(client, Priority.PRIORITY_HIGH_ACCURACY, TIMEOUT_HIGH_MS)
    }

    @SuppressLint("MissingPermission")
    private suspend fun lastKnownFresh(
        client: com.google.android.gms.location.FusedLocationProviderClient
    ): Location? {
        val last = withTimeoutOrNull(TIMEOUT_LAST_MS) {
            suspendCancellableCoroutine { cont: kotlin.coroutines.Continuation<Location?> ->
                client.lastLocation
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resume(null) }
            }
        } ?: return null
        val ageMin = (SystemClock.elapsedRealtimeNanos() - last.elapsedRealtimeNanos) / 60_000_000_000L
        return if (ageMin in 0..MAX_AGE_MIN) last else null
    }

    /**
     * Zapytanie o pozycję MUSI mieć limit czasu. Bez niego apka wisi w nieskończoność
     * tam, gdzie GPS nie łapie — czyli dokładnie w hali zakładu.
     */
    @SuppressLint("MissingPermission")
    private suspend fun fixed(
        client: com.google.android.gms.location.FusedLocationProviderClient,
        priority: Int,
        timeoutMs: Long
    ): Location? {
        val cts = CancellationTokenSource()
        return try {
            withTimeoutOrNull(timeoutMs) {
                suspendCancellableCoroutine { cont: kotlin.coroutines.Continuation<Location?> ->
                    client.getCurrentLocation(priority, cts.token)
                        .addOnSuccessListener { cont.resume(it) }
                        .addOnFailureListener { cont.resume(null) }
                }
            }
        } catch (e: Exception) {
            null
        } finally {
            runCatching { cts.cancel() }
        }
    }

    fun isInside(loc: Location, wp: WorkPlace): Boolean {
        val out = FloatArray(1)
        Location.distanceBetween(loc.latitude, loc.longitude, wp.lat, wp.lon, out)
        return out[0] <= wp.radiusM + MARGIN_M
    }
}
