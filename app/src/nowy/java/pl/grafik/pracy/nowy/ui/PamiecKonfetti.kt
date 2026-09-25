package pl.grafik.pracy.nowy.ui

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.konfettiDs by preferencesDataStore("konfetti")

/**
 * Które uroczystości już przywitaliśmy konfetti.
 *
 * Wysyp ma być miłym zaskoczeniem raz, a nie za każdym otwarciem tego samego dnia.
 * Pamiętamy więc same daty — bez imion, bez niczego więcej.
 *
 * Zapis siedzi w osobnej kartotece niż ustawienia aplikacji, bo to drobiazg wyglądu:
 * skasowanie go nie rusza grafiku i nie ma czego przenosić przy zmianie telefonu.
 */
object PamiecKonfetti {

    /** Ile dat trzymamy. Starsze wypadają — rok uroczystości to góra kilkanaście wpisów. */
    private const val LIMIT = 60

    private val kPokazane = stringSetPreferencesKey("pokazane")

    suspend fun czyPokazano(ctx: Context, data: String): Boolean =
        ctx.konfettiDs.data.first()[kPokazane].orEmpty().contains(data)

    suspend fun zapamietaj(ctx: Context, data: String) {
        ctx.konfettiDs.edit { p ->
            val byly = p[kPokazane].orEmpty()
            if (data in byly) return@edit
            // Daty są w formacie ISO, więc sortują się chronologicznie jak tekst.
            p[kPokazane] = (byly + data).sortedDescending().take(LIMIT).toSet()
        }
    }

    /** Po przywróceniu kopii albo wyczyszczeniu danych zaczynamy od nowa. */
    suspend fun zapomnij(ctx: Context) {
        ctx.konfettiDs.edit { it.remove(kPokazane) }
    }
}
