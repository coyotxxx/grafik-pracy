package pl.grafik.pracy

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pl.grafik.pracy.data.AppDb
import pl.grafik.pracy.data.DayRow
import pl.grafik.pracy.data.EventRow
import pl.grafik.pracy.data.Kopia
import pl.grafik.pracy.data.PresenceRow
import pl.grafik.pracy.data.SettingsStore
import pl.grafik.pracy.domain.CycleConfig
import pl.grafik.pracy.domain.CyclePattern
import pl.grafik.pracy.domain.StawkiCfg
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * Kopia zapasowa na prawdziwym urządzeniu: zrzut → czyszczenie → wczytanie.
 *
 * Testujemy to instrumentacyjnie, bo kopia dotyka bazy, ustawień i plików —
 * na samej maszynie wirtualnej sprawdzalibyśmy atrapy zamiast prawdziwej ścieżki.
 */
@RunWith(AndroidJUnit4::class)
class KopiaTest {

    private val ctx: Context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun czysto() = runBlocking { Kopia.wyczyscWszystko(ctx) }

    @After
    fun posprzataj() = runBlocking { Kopia.wyczyscWszystko(ctx) }

    private suspend fun wstawPrzykladoweDane() {
        val db = AppDb.get(ctx)
        db.dayDao().upsertAll(
            listOf(
                DayRow(date = "2026-09-17", shift = "III", otHours = 3, otRate = 100),
                DayRow(date = "2026-09-18", shift = "U"),
                DayRow(date = "2026-09-19", shift = "I", note = "zamiana z Krzyśkiem")
            )
        )
        db.eventDao().upsert(EventRow(date = "2026-09-20", time = "10:00", text = "fryzjer"))
        db.presenceDao().insert(
            PresenceRow(
                date = "2026-09-17",
                enterAt = "2026-09-17T21:50",
                exitAt = "2026-09-18T06:10",
                source = "geo", status = "accepted", shiftCode = "III"
            )
        )
        SettingsStore(ctx).saveStawki(StawkiCfg(zasadnicza = 7950.0, dodatekNocny = 10.01))
        SettingsStore(ctx).saveConfig(
            CycleConfig(
                pattern = CyclePattern.B4_16D,
                anchorDate = LocalDate.of(2024, 3, 1),
                anchorIndex = 0,
                brigade = "A"
            )
        )
    }

    @Test
    fun kopia_odtwarza_dni_wydarzenia_wykrycia_i_ustawienia() = runBlocking {
        wstawPrzykladoweDane()

        val json = Kopia.zrzut(ctx, "test")
        assertTrue("kopia nie może być pusta", json.length > 100)

        Kopia.wyczyscWszystko(ctx)
        assertEquals(0, Kopia.stan(ctx).dni)

        val podsumowanie = Kopia.wczytaj(ctx, json).getOrThrow()
        assertEquals(3, podsumowanie.dni)
        assertEquals(1, podsumowanie.wydarzenia)
        assertEquals(1, podsumowanie.wykrycia)

        val db = AppDb.get(ctx)
        val dzien = db.dayDao().get("2026-09-17")
        assertNotNull(dzien)
        assertEquals("III", dzien!!.shift)
        assertEquals(3, dzien.otHours)
        assertEquals("zamiana z Krzyśkiem", db.dayDao().get("2026-09-19")?.note)

        val stawki = SettingsStore(ctx).stawki.first()
        assertEquals(7950.0, stawki.zasadnicza, 0.01)
        assertEquals(10.01, stawki.dodatekNocny, 0.01)
        assertEquals("A", SettingsStore(ctx).config.first().brigade)
    }

    @Test
    fun liczby_na_karcie_zgadzaja_sie_z_zawartoscia() = runBlocking {
        wstawPrzykladoweDane()
        val stan = Kopia.stan(ctx)
        assertEquals(3, stan.dni)
        assertEquals(1, stan.wydarzenia)
        assertEquals(1, stan.wykrycia)
    }

    @Test
    fun przywrocenie_do_cyklu_kasuje_tylko_dni() = runBlocking {
        wstawPrzykladoweDane()
        Kopia.przywrocGrafikDoCyklu(ctx)

        val stan = Kopia.stan(ctx)
        assertEquals(0, stan.dni)
        assertEquals("wydarzenia zostają", 1, stan.wydarzenia)
        assertEquals("ustawienia zostają", 7950.0,
            SettingsStore(ctx).stawki.first().zasadnicza, 0.01)
    }

    @Test
    fun czyszczenie_zabiera_wszystko_razem_z_ustawieniami() = runBlocking {
        wstawPrzykladoweDane()
        Kopia.wyczyscWszystko(ctx)

        val stan = Kopia.stan(ctx)
        assertEquals(0, stan.dni)
        assertEquals(0, stan.wydarzenia)
        assertEquals(0, stan.wykrycia)
        assertEquals(0.0, SettingsStore(ctx).stawki.first().zasadnicza, 0.01)
    }

    @Test
    fun obcy_plik_nie_rusza_danych() = runBlocking {
        wstawPrzykladoweDane()

        val wynik = Kopia.wczytaj(ctx, """{"aplikacja":"cos-innego","format":1}""")
        assertTrue("obcy plik ma się nie wczytać", wynik.isFailure)
        assertEquals("dane zostają nietknięte", 3, Kopia.stan(ctx).dni)

        val smieci = Kopia.wczytaj(ctx, "to nie jest json")
        assertTrue(smieci.isFailure)
        assertEquals(3, Kopia.stan(ctx).dni)
    }

    @Test
    fun kopia_lokalna_powstaje_i_daje_sie_przywrocic() = runBlocking {
        wstawPrzykladoweDane()

        val plik = Kopia.zrobKopieLokalna(ctx, "test").getOrThrow()
        assertTrue("plik kopii ma mieć treść", plik.bajty > 100)
        assertTrue(plik.nazwa.startsWith("grafik-") && plik.nazwa.endsWith(".json"))
        assertTrue(Kopia.kopieLokalne(ctx).any { it.nazwa == plik.nazwa })

        Kopia.wyczyscWszystko(ctx)
        val podsumowanie = Kopia.przywroc(ctx, plik).getOrThrow()
        assertEquals(3, podsumowanie.dni)
    }

    @Test
    fun zdjecie_notatki_wchodzi_do_kopii_i_wraca() = runBlocking {
        // notatka ze zdjęciem — plik musi przeżyć zmianę telefonu razem z tekstem
        val plik = pl.grafik.pracy.data.ZdjeciaNotatek.plik(ctx, "2026-09-24.jpg")
        plik.parentFile?.mkdirs()
        plik.writeBytes(byteArrayOf(1, 2, 3, 4, 5))
        AppDb.get(ctx).dayDao().upsert(
            DayRow(date = "2026-09-24", shift = "II", note = "zamiana", notePhoto = "2026-09-24.jpg")
        )

        val json = Kopia.zrzut(ctx, "test")
        Kopia.wyczyscWszystko(ctx)
        assertFalse("czyszczenie kasuje też zdjęcia", plik.exists())

        Kopia.wczytaj(ctx, json).getOrThrow()

        val dzien = AppDb.get(ctx).dayDao().get("2026-09-24")
        assertEquals("zamiana", dzien?.note)
        assertEquals("2026-09-24.jpg", dzien?.notePhoto)
        assertTrue("sam plik też musi wrócić", plik.exists())
        assertArrayEquals(byteArrayOf(1, 2, 3, 4, 5), plik.readBytes())
    }

    @Test
    fun uroczystosc_wraca_z_kopii_kompletna() = runBlocking {
        // Uroczystość różni się od wydarzenia trzema polami. Bez nich wróciłaby
        // jako zwykły wpis z napisem „urodziny — Krzysiek" i przestałaby wracać co roku.
        AppDb.get(ctx).eventDao().upsert(
            EventRow(
                date = "2026-12-05", time = "", text = "urodziny — Krzysiek Nowak",
                remind = true, rodzaj = "urodziny", osoba = "Krzysiek Nowak", coroczne = true
            )
        )

        val json = Kopia.zrzut(ctx, "test")
        Kopia.wyczyscWszystko(ctx)
        Kopia.wczytaj(ctx, json).getOrThrow()

        val wpis = AppDb.get(ctx).eventDao().rangeOnce("2026-12-01", "2026-12-31").single()
        assertEquals("urodziny", wpis.rodzaj)
        assertEquals("Krzysiek Nowak", wpis.osoba)
        assertTrue("uroczystość ma dalej wracać co roku", wpis.coroczne)
        assertEquals("", wpis.time)
    }

    @Test
    fun zwykle_wydarzenie_nie_zaczyna_udawac_uroczystosci() = runBlocking {
        AppDb.get(ctx).eventDao().upsert(
            EventRow(date = "2026-09-20", time = "10:00", text = "fryzjer")
        )

        val json = Kopia.zrzut(ctx, "test")
        Kopia.wyczyscWszystko(ctx)
        Kopia.wczytaj(ctx, json).getOrThrow()

        val wpis = AppDb.get(ctx).eventDao().rangeOnce("2026-09-01", "2026-09-30").single()
        assertEquals("", wpis.rodzaj)
        assertEquals("", wpis.osoba)
        assertFalse(wpis.coroczne)
        assertEquals("10:00", wpis.time)
    }

    @Test
    fun kopia_sprzed_uroczystosci_dalej_sie_wczytuje() = runBlocking {
        // Plik zrobiony starszą wersją aplikacji nie zna trzech nowych pól.
        val stara = """
            {"aplikacja":"grafik-pracy","format":1,
             "wydarzenia":[{"date":"2026-09-20","time":"10:00","text":"fryzjer","remind":true}]}
        """.trimIndent()

        val podsumowanie = Kopia.wczytaj(ctx, stara).getOrThrow()
        assertEquals(1, podsumowanie.wydarzenia)

        val wpis = AppDb.get(ctx).eventDao().rangeOnce("2026-09-01", "2026-09-30").single()
        assertEquals("fryzjer", wpis.text)
        assertEquals("stary wpis to zwykłe wydarzenie", "", wpis.rodzaj)
        assertFalse(wpis.coroczne)
    }

    @Test
    fun rozmiar_pisze_sie_po_ludzku() {
        assertEquals("512 B", Kopia.PlikKopii("a", 512, java.io.File("a")).rozmiar)
        assertEquals("34 kB", Kopia.PlikKopii("a", 34_500, java.io.File("a")).rozmiar)
    }
}
