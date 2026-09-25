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
import pl.grafik.pracy.nowy.ui.PamiecKonfetti

/**
 * Konfetti wita uroczystość raz.
 *
 * Maciej wybrał ten wariant pod warunkiem, że przy kolejnym wejściu w ten sam dzień
 * karta otwiera się już normalnie — inaczej po tygodniu zaczęłoby przeszkadzać.
 */
@RunWith(AndroidJUnit4::class)
class KonfettiPamiecTest {

    private val ctx: Context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before fun czysto(): Unit = runBlocking { PamiecKonfetti.zapomnij(ctx) }

    @After fun posprzataj(): Unit = runBlocking { PamiecKonfetti.zapomnij(ctx) }

    @Test
    fun pierwsze_wejscie_sypie_kolejne_juz_nie() = runBlocking {
        assertFalse("na początku nic nie było pokazane",
            PamiecKonfetti.czyPokazano(ctx, "2026-09-24"))

        PamiecKonfetti.zapamietaj(ctx, "2026-09-24")

        assertTrue("drugie wejście w ten sam dzień ma być ciche",
            PamiecKonfetti.czyPokazano(ctx, "2026-09-24"))
    }

    @Test
    fun kazdy_dzien_liczy_sie_osobno() = runBlocking {
        PamiecKonfetti.zapamietaj(ctx, "2026-09-24")

        assertFalse("inna uroczystość dostaje własne powitanie",
            PamiecKonfetti.czyPokazano(ctx, "2026-12-05"))
    }

    @Test
    fun ta_sama_uroczystosc_za_rok_sypie_od_nowa() = runBlocking {
        // Klucz zawiera rok, więc coroczne imieniny wracają z konfetti.
        PamiecKonfetti.zapamietaj(ctx, "2026-09-24")

        assertFalse(PamiecKonfetti.czyPokazano(ctx, "2027-09-24"))
    }

    @Test
    fun pamietamy_najwyzej_szescdziesiat_najnowszych() = runBlocking {
        // 70 dni z rzędu — najstarsze mają wypaść, najnowsze zostać.
        repeat(70) { i ->
            PamiecKonfetti.zapamietaj(ctx, java.time.LocalDate.of(2026, 1, 1).plusDays(i.toLong()).toString())
        }

        assertTrue("najnowsza data zostaje",
            PamiecKonfetti.czyPokazano(ctx, "2026-03-11"))
        assertFalse("najstarsze wypadają, żeby zapis nie puchł",
            PamiecKonfetti.czyPokazano(ctx, "2026-01-01"))
    }

    @Test
    fun powtorny_zapis_tej_samej_daty_nic_nie_psuje() = runBlocking {
        PamiecKonfetti.zapamietaj(ctx, "2026-09-24")
        PamiecKonfetti.zapamietaj(ctx, "2026-09-24")

        assertTrue(PamiecKonfetti.czyPokazano(ctx, "2026-09-24"))
    }
}
