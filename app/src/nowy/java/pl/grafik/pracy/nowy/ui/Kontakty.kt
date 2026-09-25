package pl.grafik.pracy.nowy.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Kontakt znaleziony w książce adresowej — tyle, ile potrzeba do uroczystości. */
data class Kontakt(
    val id: Long,
    val imie: String,
    /** Dzień i miesiąc urodzin, jeśli kontakt je ma. */
    val dzien: Int? = null,
    val miesiac: Int? = null
) {
    val maUrodziny: Boolean get() = dzien != null && miesiac != null
}

/**
 * Szukanie w kontaktach telefonu.
 *
 * Czytamy dwie rzeczy: imię, żeby nie trzeba było go przepisywać, i datę urodzin,
 * żeby nie trzeba było jej pamiętać. Nic stąd nie wychodzi poza telefon i nic nie
 * jest zapisywane poza tym, co sam wybierzesz.
 *
 * Bez zgody na kontakty wyszukiwanie po prostu nie zwraca nic — formularz dalej
 * działa, a kontakt da się wskazać systemowym oknem wyboru.
 */
object Kontakty {

    /** Ile podpowiedzi pokazujemy. Dłuższa lista i tak nie mieści się pod polem. */
    private const val ILE = 6

    /** Od tylu znaków zaczynamy szukać — przy jednej literze pasuje pół książki. */
    const val MIN_ZNAKOW = 2

    fun czyMaZgode(ctx: Context): Boolean =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED

    suspend fun szukaj(ctx: Context, fraza: String): List<Kontakt> =
        withContext(Dispatchers.IO) {
            if (fraza.trim().length < MIN_ZNAKOW || !czyMaZgode(ctx)) return@withContext emptyList()
            runCatching {
                val znalezione = poNazwie(ctx, fraza.trim())
                if (znalezione.isEmpty()) return@runCatching znalezione
                val urodziny = urodzinyDla(ctx, znalezione.map { it.id })
                znalezione.map { k ->
                    urodziny[k.id]?.let { (m, d) -> k.copy(dzien = d, miesiac = m) } ?: k
                }
            }.getOrDefault(emptyList())
        }

    private fun poNazwie(ctx: Context, fraza: String): List<Kontakt> {
        val uri = Uri.withAppendedPath(
            ContactsContract.Contacts.CONTENT_FILTER_URI, Uri.encode(fraza)
        )
        val wynik = LinkedHashMap<Long, Kontakt>()
        ctx.contentResolver.query(
            uri,
            arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME),
            null, null,
            "${ContactsContract.Contacts.DISPLAY_NAME} ASC"
        )?.use { c ->
            while (c.moveToNext() && wynik.size < ILE) {
                val id = c.getLong(0)
                val imie = c.getString(1)?.trim().orEmpty()
                // Kontakty bywają zdublowane między kontami — pokazujemy każdą osobę raz.
                if (imie.isNotBlank() && wynik.values.none { it.imie == imie }) {
                    wynik[id] = Kontakt(id, imie)
                }
            }
        }
        return wynik.values.toList()
    }

    /** Urodziny wszystkich znalezionych naraz — jedno zapytanie zamiast sześciu. */
    private fun urodzinyDla(ctx: Context, idki: List<Long>): Map<Long, Pair<Int, Int>> {
        if (idki.isEmpty()) return emptyMap()
        val miejsca = idki.joinToString(",") { "?" }
        val wynik = HashMap<Long, Pair<Int, Int>>()
        ctx.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(
                ContactsContract.Data.CONTACT_ID,
                ContactsContract.CommonDataKinds.Event.START_DATE
            ),
            "${ContactsContract.Data.MIMETYPE} = ? AND " +
                "${ContactsContract.CommonDataKinds.Event.TYPE} = ? AND " +
                "${ContactsContract.Data.CONTACT_ID} IN ($miejsca)",
            arrayOf(
                ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE,
                ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY.toString()
            ) + idki.map { it.toString() },
            null
        )?.use { c ->
            while (c.moveToNext()) {
                miesiacIDzien(c.getString(1))?.let { wynik[c.getLong(0)] = it }
            }
        }
        return wynik
    }
}

/**
 * Miesiąc i dzień z daty urodzin zapisanej w kontaktach.
 *
 * Android trzyma ją jako zwykły tekst i nie narzuca jednego zapisu. Spotykane są
 * trzy: pełna data, data bez roku (`--03-24`) oraz pełna ze znacznikiem czasu.
 * Rok nas nie interesuje — uroczystość wraca co roku.
 */
internal fun miesiacIDzien(tekst: String?): Pair<Int, Int>? {
    val t = tekst?.trim().orEmpty()
    if (t.isEmpty()) return null
    val bezCzasu = t.substringBefore('T')
    val czesci = bezCzasu.removePrefix("--").split("-")
    val liczby = czesci.mapNotNull { it.toIntOrNull() }
    val (m, d) = when {
        // „--03-24" → po zdjęciu myślników zostają dwie liczby: miesiąc i dzień.
        czesci.size == 2 && liczby.size == 2 -> liczby[0] to liczby[1]
        czesci.size == 3 && liczby.size == 3 -> liczby[1] to liczby[2]
        else -> return null
    }
    if (m !in 1..12 || d !in 1..31) return null
    return m to d
}
