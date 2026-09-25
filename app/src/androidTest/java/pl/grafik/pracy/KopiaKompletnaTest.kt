package pl.grafik.pracy

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pl.grafik.pracy.data.AppDb
import pl.grafik.pracy.data.DayRow
import pl.grafik.pracy.data.EventRow
import pl.grafik.pracy.data.Kopia
import pl.grafik.pracy.data.PayslipRow
import pl.grafik.pracy.data.PresenceRow

/**
 * Straż nad kompletnością kopii zapasowej.
 *
 * Kopia wylicza pola ręcznie, więc dołożenie kolumny do tabeli nie trafia do niej
 * samo. Tak przepadły uroczystości: `rodzaj`, `osoba` i `coroczne` zostały dodane
 * do wydarzeń, ale nie do zrzutu — po przywróceniu urodziny wróciłyby jako zwykły
 * wpis i przestałyby wracać co roku (zgłoszenie Macieja z 25.09.2026).
 *
 * Ten test porównuje pola encji z kluczami zapisanymi w pliku. Następna nowa kolumna
 * zapomniana w kopii wywali się tutaj, zanim ktokolwiek straci dane.
 */
@RunWith(AndroidJUnit4::class)
class KopiaKompletnaTest {

    private val ctx: Context get() = InstrumentationRegistry.getInstrumentation().targetContext

    /** `id` jest nadawane przez bazę przy wstawianiu, więc nie ma go po co przenosić. */
    private val pomijane = setOf("id")

    @Before fun czysto(): Unit = runBlocking { Kopia.wyczyscWszystko(ctx) }

    @After fun posprzataj(): Unit = runBlocking { Kopia.wyczyscWszystko(ctx) }

    private fun polaEncji(klasa: Class<*>): Set<String> =
        klasa.declaredFields
            .map { it.name }
            .filterNot { it in pomijane || it.startsWith("$") || it == "Companion" }
            .toSet()

    private fun kluczeWKopii(json: String, tablica: String): Set<String> {
        val tab = JSONObject(json).getJSONArray(tablica)
        assertTrue("kopia ma zawierać przykładowy wpis w „$tablica”", tab.length() > 0)
        val o = tab.getJSONObject(0)
        return o.keys().asSequence().toSet()
    }

    private suspend fun kopiaZPrzykladami(): String {
        val db = AppDb.get(ctx)
        db.dayDao().upsert(
            DayRow(
                date = "2026-09-24", shift = "II", otHours = 2, otRate = 50,
                deviation = true, note = "zamiana", notePhoto = "2026-09-24.jpg",
                dwnFor = "2026-09-20"
            )
        )
        db.eventDao().upsert(
            EventRow(
                date = "2026-12-05", time = "", text = "urodziny — Krzysiek",
                remind = true, rodzaj = "urodziny", osoba = "Krzysiek", coroczne = true
            )
        )
        db.presenceDao().insert(
            PresenceRow(
                date = "2026-09-24", enterAt = "2026-09-24T13:45",
                exitAt = "2026-09-24T22:05", source = "geo", status = "accepted",
                otHours = 1, otRate = 100, countedFrom = "2026-09-24T14:00",
                countedTo = "2026-09-24T22:00", createdAt = "2026-09-24T22:06",
                shiftCode = "II"
            )
        )
        db.payslipDao().upsert(
            PayslipRow(
                ym = "2026-08", fileName = "2026-08.pdf",
                originalName = "odcinek.pdf", addedAt = "2026-09-01T10:00"
            )
        )
        return Kopia.zrzut(ctx, "test")
    }

    @Test
    fun kopia_zapisuje_kazde_pole_dnia() = runBlocking {
        sprawdz(kopiaZPrzykladami(), "dni", DayRow::class.java)
    }

    @Test
    fun kopia_zapisuje_kazde_pole_wydarzenia() = runBlocking {
        // To jest test, którego zabrakło przy uroczystościach.
        sprawdz(kopiaZPrzykladami(), "wydarzenia", EventRow::class.java)
    }

    @Test
    fun kopia_zapisuje_kazde_pole_wykrycia() = runBlocking {
        sprawdz(kopiaZPrzykladami(), "wykrycia", PresenceRow::class.java)
    }

    @Test
    fun kopia_zapisuje_kazde_pole_odcinka() = runBlocking {
        // W odcinkach dochodzi jeszcze „plik" z treścią — stąd tylko sprawdzenie w jedną stronę.
        sprawdz(kopiaZPrzykladami(), "odcinki", PayslipRow::class.java)
    }

    private fun sprawdz(json: String, tablica: String, klasa: Class<*>) {
        val pola = polaEncji(klasa)
        val klucze = kluczeWKopii(json, tablica)
        val brakujace = pola - klucze
        assertTrue(
            "Kopia gubi pola encji ${klasa.simpleName}: $brakujace. " +
                "Dopisz je w Kopia.zrzut i Kopia.wczytaj, inaczej przepadną przy zmianie telefonu.",
            brakujace.isEmpty()
        )
    }
}
