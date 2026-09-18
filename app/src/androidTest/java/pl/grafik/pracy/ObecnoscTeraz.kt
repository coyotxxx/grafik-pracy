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
import pl.grafik.pracy.location.PresenceState
import java.time.LocalDateTime

/**
 * Trwający pobyt w pracy — to z niego bierze się napis „JESTEŚ W PRACY" w tarczy.
 *
 * Wcześniej ekran szukał pobytu w mapie dni, pod dzisiejszą datą. Na nocce po północy
 * pobyt należy do dnia poprzedniego (dzień grafiku), więc napis się nie pokazywał,
 * mimo że aplikacja widziała pracę. Dlatego stan czytamy wprost, bez wiązania z dniem.
 */
@RunWith(AndroidJUnit4::class)
class ObecnoscTeraz {

    private val ctx: Context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun czysto() = runBlocking { PresenceState.clear(ctx) }

    @After
    fun posprzataj() = runBlocking { PresenceState.clear(ctx) }

    @Test
    fun po_wejsciu_do_pracy_stan_mowi_od_kiedy() = runBlocking {
        assertNull("na starcie nie ma pobytu", PresenceState.openEnter(ctx))

        val wejscie = LocalDateTime.of(2026, 9, 17, 21, 52)
        PresenceState.markEnter(ctx, wejscie)

        assertEquals(wejscie, PresenceState.openEnter(ctx))
    }

    @Test
    fun pobyt_z_nocki_widac_takze_po_polnocy() = runBlocking {
        // Wejście w czwartek wieczorem; sprawdzamy w piątek o 1:57 — tak jak u Macieja.
        PresenceState.markEnter(ctx, LocalDateTime.of(2026, 9, 17, 21, 52))
        PresenceState.touch(ctx, LocalDateTime.of(2026, 9, 18, 1, 57))

        val od = PresenceState.openEnter(ctx)
        assertNotNull("pobyt ma być widoczny po północy", od)
        assertEquals("i ma pamiętać godzinę wejścia, nie chwilę sprawdzenia",
            17, od!!.dayOfMonth)
    }

    @Test
    fun pierwsze_wejscie_wyznacza_poczatek_pobytu() = runBlocking {
        val pierwsze = LocalDateTime.of(2026, 9, 17, 21, 52)
        PresenceState.markEnter(ctx, pierwsze)
        PresenceState.markEnter(ctx, LocalDateTime.of(2026, 9, 17, 23, 30))

        assertEquals("kolejne wykrycie nie przesuwa startu", pierwsze, PresenceState.openEnter(ctx))
    }

    @Test
    fun po_wyjsciu_nie_ma_juz_pobytu() = runBlocking {
        PresenceState.markEnter(ctx, LocalDateTime.of(2026, 9, 17, 21, 52))
        PresenceState.clear(ctx)

        assertNull(PresenceState.openEnter(ctx))
    }
}
