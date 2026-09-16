package pl.grafik.pracy

import android.Manifest
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pl.grafik.pracy.data.AppDb
import pl.grafik.pracy.data.SettingsStore
import pl.grafik.pracy.domain.WorkPlace
import pl.grafik.pracy.location.LocationProbe
import pl.grafik.pracy.location.PresenceRepo
import pl.grafik.pracy.location.PresenceState
import java.time.LocalDateTime

/**
 * Sonda pozycji na PRAWDZIWYM urządzeniu — bez podstawiania lokalizacji.
 * To ona wcześniej zwracała null i przez to watchdog nic nie robił.
 */
@RunWith(AndroidJUnit4::class)
class LocationProbeTest {

    @get:Rule
    val perms: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private val ctx: Context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val ZAKLAD_LAT = 52.2297
    private val ZAKLAD_LON = 21.0122

    /**
     * Android dławi lokalizację aplikacjom w tle. Test instrumentacyjny nie ma otwartej
     * aktywności, więc proces jest „w tle" i sonda nic nie dostaje — tak jak w prawdziwym
     * telefonie z zamkniętą apką. Wystawiamy ekran na wierzch, żeby mierzyć samą sondę.
     */
    private fun naWierzch() {
        val i = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
            ?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) ?: return
        ctx.startActivity(i)
        Thread.sleep(3000)
    }

    @Before
    fun clean() = runBlocking {
        naWierzch()
        PresenceState.clear(ctx)
        AppDb.get(ctx).presenceDao().observeRecent(500).let { }
        AppDb.get(ctx).presenceDao().pending().forEach { AppDb.get(ctx).presenceDao().delete(it.id) }
    }

    @Test
    fun sonda_daje_pozycje_gdy_urzadzenie_ja_ma() = runBlocking {
        val loc = LocationProbe.current(ctx)
        org.junit.Assume.assumeNotNull(
            "POMINIĘTO: urządzenie nie ma świeżej pozycji (emulator wstrzykuje fix jednorazowo)", loc
        )
        assertNotNull(loc)
    }

    @Test
    fun sonda_poprawnie_rozroznia_w_strefie_i_poza() = runBlocking {
        val maybe = LocationProbe.current(ctx)
        org.junit.Assume.assumeNotNull("POMINIĘTO: brak świeżej pozycji na urządzeniu", maybe)
        val loc = maybe!!
        val tutaj = WorkPlace(enabled = true, lat = loc.latitude, lon = loc.longitude, radiusM = 200)
        assertTrue("moja własna pozycja musi być w strefie", LocationProbe.isInside(loc, tutaj))

        val daleko = tutaj.copy(lat = loc.latitude + 0.5)      // ~55 km na północ
        assertFalse("55 km to nie jest strefa 200 m", LocationProbe.isInside(loc, daleko))
    }

    /** Brak świeżej pozycji to „nie wiem", a nie „nie ma mnie w pracy" — watchdog nie może wtedy zamykać. */
    @Test
    fun bez_swiezej_pozycji_watchdog_nie_zamyka_pobytu() = runBlocking {
        SettingsStore(ctx).saveWorkPlace(
            WorkPlace(enabled = true, lat = ZAKLAD_LAT, lon = ZAKLAD_LON, radiusM = 200, minStayMin = 30)
        )
        PresenceRepo.onEnter(ctx, LocalDateTime.now().minusMinutes(90))
        PresenceRepo.onStillHere(ctx, LocalDateTime.now().minusMinutes(5))
        // sonda zwraca null (brak fixa), cisza tylko 5 min
        PresenceRepo.watchdog(ctx, LocalDateTime.now()) { null }
        assertTrue(AppDb.get(ctx).presenceDao().pending().isEmpty())
        assertNotNull(PresenceState.openEnter(ctx))
    }

    /**
     * Pełna ścieżka ratunkowa z PRAWDZIWĄ sondą: zakład 55 km stąd, otwarty pobyt,
     * Android nie zgłosił wyjścia — watchdog musi sam to wykryć i zamknąć.
     */
    @Test
    fun watchdog_z_prawdziwa_sonda_domyka_pobyt_poza_strefa() = runBlocking {
        val maybe = LocationProbe.current(ctx)
        org.junit.Assume.assumeNotNull("POMINIĘTO: brak świeżej pozycji na urządzeniu", maybe)
        val loc = maybe!!
        val zaklad = WorkPlace(
            enabled = true, lat = loc.latitude + 0.5, lon = loc.longitude,
            radiusM = 200, minStayMin = 30
        )
        SettingsStore(ctx).saveWorkPlace(zaklad)

        val start = LocalDateTime.now().minusHours(9)
        PresenceRepo.onEnter(ctx, start)
        assertNotNull(PresenceState.openEnter(ctx))

        PresenceRepo.watchdog(ctx, LocalDateTime.now())     // domyślna, prawdziwa sonda

        val rows = AppDb.get(ctx).presenceDao().observeRecent(10).let { AppDb.get(ctx).presenceDao().pending() }
        assertEquals("watchdog miał zamknąć pobyt", 1, rows.size)
        assertEquals("watchdog", rows[0].source)
        assertNull("stan pobytu musi zostać wyczyszczony", PresenceState.openEnter(ctx))
    }

    @Test
    fun watchdog_z_prawdziwa_sonda_nie_zamyka_gdy_zaklad_jest_tu_gdzie_stoje() = runBlocking {
        val maybe = LocationProbe.current(ctx)
        org.junit.Assume.assumeNotNull("POMINIĘTO: brak świeżej pozycji na urządzeniu", maybe)
        val loc = maybe!!
        SettingsStore(ctx).saveWorkPlace(
            WorkPlace(enabled = true, lat = loc.latitude, lon = loc.longitude, radiusM = 200, minStayMin = 30)
        )
        PresenceRepo.onEnter(ctx, LocalDateTime.now().minusHours(3))
        PresenceRepo.watchdog(ctx, LocalDateTime.now())

        assertTrue("nie wolno zamykać pobytu, gdy stoję w zakładzie",
            AppDb.get(ctx).presenceDao().pending().isEmpty())
        assertNotNull(PresenceState.openEnter(ctx))
    }
}
