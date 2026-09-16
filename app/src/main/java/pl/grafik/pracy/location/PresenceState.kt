package pl.grafik.pracy.location

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime

private val Context.presenceDs by preferencesDataStore("presence_state")

/**
 * Stan „jestem w strefie". Trzymany poza pamięcią procesu, bo BroadcastReceiver
 * dostaje zdarzenia w różnych instancjach aplikacji.
 */
object PresenceState {

    private val kEnter = stringPreferencesKey("open_enter")
    private val kSeen = stringPreferencesKey("last_seen")

    suspend fun openEnter(ctx: Context): LocalDateTime? =
        ctx.presenceDs.data.first()[kEnter]?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }

    suspend fun lastSeen(ctx: Context): LocalDateTime? =
        ctx.presenceDs.data.first()[kSeen]?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }

    suspend fun markEnter(ctx: Context, at: LocalDateTime) {
        ctx.presenceDs.edit { p ->
            if (p[kEnter] == null) p[kEnter] = at.toString()
            p[kSeen] = at.toString()
        }
    }

    /** Odświeża „ostatnio widziany w pracy" — z tego odtwarzamy wyjście, gdy Android zgubi EXIT. */
    suspend fun touch(ctx: Context, at: LocalDateTime) {
        ctx.presenceDs.edit { p -> p[kSeen] = at.toString() }
    }

    suspend fun clear(ctx: Context) {
        ctx.presenceDs.edit { p -> p.remove(kEnter); p.remove(kSeen) }
    }
}
