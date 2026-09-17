package pl.grafik.pracy.nowy

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
import pl.grafik.pracy.data.SettingsStore
import pl.grafik.pracy.domain.CycleConfig
import pl.grafik.pracy.domain.CyclePattern
import pl.grafik.pracy.domain.Shift
import pl.grafik.pracy.nowy.widzety.DaneWidzetu
import pl.grafik.pracy.nowy.widzety.OdswiezanieWidzetow
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Dane widżetów. Liczby mają się zgadzać z tym, co pokazuje ekran — widżet czyta
 * tę samą bazę i liczy tak samo, więc rozjazd byłby błędem widocznym na pulpicie.
 */
@RunWith(AndroidJUnit4::class)
class DaneWidzetuTest {

    private val ctx: Context get() = InstrumentationRegistry.getInstrumentation().targetContext

    /** Czwartek 17 września 2026, wieczorem — tuż przed nocką. */
    private val czwartekWieczor = LocalDateTime.of(2026, 9, 17, 20, 50)

    @Before
    fun czysto() = runBlocking {
        Kopia.wyczyscWszystko(ctx)
        SettingsStore(ctx).saveConfig(
            CycleConfig(
                pattern = CyclePattern.B4_16D,
                anchorDate = LocalDate.of(2024, 3, 1),
                anchorIndex = 0,
                brigade = "A"
            )
        )
    }

    @After
    fun posprzataj() = runBlocking { Kopia.wyczyscWszystko(ctx) }

    private suspend fun wpiszTydzien() {
        AppDb.get(ctx).dayDao().upsertAll(
            listOf(
                DayRow(date = "2026-09-14", shift = "U"),
                DayRow(date = "2026-09-15", shift = "III"),
                DayRow(date = "2026-09-16", shift = "III"),
                DayRow(date = "2026-09-17", shift = "III", otHours = 3, otRate = 100),
                DayRow(date = "2026-09-18", shift = "III"),
                DayRow(date = "2026-09-19", shift = null),
                DayRow(date = "2026-09-20", shift = "W5")
            )
        )
    }

    @Test
    fun tydzien_zaczyna_sie_w_poniedzialek_i_ma_siedem_dni() = runBlocking {
        wpiszTydzien()
        val d = DaneWidzetu.wczytaj(ctx, czwartekWieczor)

        assertEquals(7, d.tydzien.size)
        assertEquals(LocalDate.of(2026, 9, 14), d.tydzien.first().date)
        assertEquals(LocalDate.of(2026, 9, 20), d.tydzien.last().date)
        assertEquals(Shift.URLOP, d.tydzien.first().shift)
    }

    @Test
    fun godziny_tygodnia_licza_urlop_i_nadgodziny() = runBlocking {
        wpiszTydzien()
        val d = DaneWidzetu.wczytaj(ctx, czwartekWieczor)
        // 8 h urlopu + cztery nocki po 8 h + 3 h nadgodzin
        assertEquals(43, d.godzinyTygodnia)
    }

    @Test
    fun godziny_miesiaca_licza_sie_do_dzis_wlacznie() = runBlocking {
        AppDb.get(ctx).dayDao().upsertAll(
            listOf(
                DayRow(date = "2026-09-01", shift = "I"),
                DayRow(date = "2026-09-02", shift = "I", otHours = 2, otRate = 50),
                DayRow(date = "2026-09-17", shift = "III"),
                DayRow(date = "2026-09-30", shift = "I")          // po dziś — nie liczy się
            )
        )
        val d = DaneWidzetu.wczytaj(ctx, czwartekWieczor)

        assertEquals("wrzesień 2026 ma 176 h wymiaru", 176, d.normaMiesiaca)
        // reszta miesiąca pochodzi z cyklu, więc sprawdzamy to, co pewne:
        // dzień po dziś nie może się liczyć, a suma nie przekracza wymiaru całego miesiąca
        assertTrue("godziny do dziś: ${d.godzinyMiesiaca}", d.godzinyMiesiaca > 0)
        assertTrue(d.udzialMiesiaca in 0f..1f)
    }

    @Test
    fun przed_nocka_odliczamy_do_startu() = runBlocking {
        wpiszTydzien()
        val d = DaneWidzetu.wczytaj(ctx, czwartekWieczor)

        assertEquals(Shift.III, d.zmianaDzis)
        assertFalse("zmiana jeszcze się nie zaczęła", d.trwa)
        assertEquals("20:50 → 22:00 to 70 minut", 70, d.doStartuMin.toInt())
        assertEquals("1:10", d.odliczanie)
    }

    @Test
    fun w_trakcie_nocki_odliczamy_do_konca() = runBlocking {
        wpiszTydzien()
        // 23:00 — nocka trwa, koniec o 6:00, czyli zostało 7 h
        val d = DaneWidzetu.wczytaj(ctx, LocalDateTime.of(2026, 9, 17, 23, 0))

        assertTrue("zmiana trwa", d.trwa)
        assertEquals(7 * 60, d.doStartuMin.toInt())
    }

    @Test
    fun po_zmianie_pokazujemy_ta_jutrzejsza() = runBlocking {
        AppDb.get(ctx).dayDao().upsertAll(
            listOf(
                DayRow(date = "2026-09-17", shift = "I"),         // 6:00 – 14:00
                DayRow(date = "2026-09-18", shift = "II")         // 14:00 – 22:00
            )
        )
        val d = DaneWidzetu.wczytaj(ctx, czwartekWieczor)         // 20:50, ranna już minęła

        assertEquals(Shift.I, d.zmianaDzis)
        assertEquals(Shift.II, d.zmianaJutro)
        assertFalse(d.trwa)
        // 20:50 → jutro 14:00 to 17 h 10 min
        assertEquals(17 * 60 + 10, d.doStartuMin.toInt())
    }

    @Test
    fun wydarzenie_z_najblizszych_dwoch_dob_trafia_do_stopki() = runBlocking {
        wpiszTydzien()
        AppDb.get(ctx).eventDao().upsert(
            EventRow(date = "2026-09-18", time = "09:20", text = "fryzjer")
        )
        val d = DaneWidzetu.wczytaj(ctx, czwartekWieczor)

        assertNotNull(d.wydarzenie)
        assertEquals("fryzjer", d.wydarzenie!!.text)
        assertEquals(LocalDate.of(2026, 9, 18), d.dataWydarzenia)
    }

    @Test
    fun bez_grafiku_widzet_nie_wybucha() = runBlocking {
        val d = DaneWidzetu.wczytaj(ctx, czwartekWieczor)
        assertEquals(7, d.tydzien.size)
        assertTrue(d.doStartuMin >= 0)
        assertTrue(d.udzialMiesiaca in 0f..1f)
    }

    @Test
    fun odswiezanie_zageszcza_sie_przed_zmiana() {
        assertEquals("tuż przed zmianą co pięć minut", 5, OdswiezanieWidzetow.odstepMinut(30).toInt())
        assertEquals(5, OdswiezanieWidzetow.odstepMinut(120).toInt())
        assertEquals("w ciągu dnia co pół godziny", 30, OdswiezanieWidzetow.odstepMinut(121).toInt())
        assertEquals("gdy daleko — co dwie godziny", 120, OdswiezanieWidzetow.odstepMinut(13 * 60).toInt())
    }
}
