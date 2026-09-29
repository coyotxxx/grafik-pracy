package pl.grafik.pracy

import android.content.Context
import androidx.core.content.FileProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import pl.grafik.pracy.data.Odcinki
import pl.grafik.pracy.data.ZdjeciaNotatek
import java.io.File

/**
 * Pliki, które aplikacja podaje innym programom do otwarcia.
 *
 * FileProvider wydaje adres tylko dla katalogów wymienionych w `file_paths.xml`.
 * Brak wpisu kończy się wyjątkiem, a że intencja powstaje w `runCatching`, dotknięcie
 * pliku wyglądało po prostu na martwe — tak było ze zdjęciem w notatce
 * (zgłoszenie Macieja z 29.09.2026).
 *
 * Ten test sprawdza każdy katalog, z którego coś podajemy na zewnątrz. Dołożenie
 * nowego rodzaju plików bez wpisu w `file_paths.xml` wywali się tutaj.
 */
@RunWith(AndroidJUnit4::class)
class PodgladPlikowTest {

    private val ctx: Context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val autorytet: String get() = "${ctx.packageName}.fileprovider"

    private fun adresDla(plik: File) {
        plik.parentFile?.mkdirs()
        if (!plik.exists()) plik.writeBytes(byteArrayOf(1, 2, 3))
        val uri = FileProvider.getUriForFile(ctx, autorytet, plik)
        assertNotNull(uri)
        assertEquals("content", uri.scheme)
    }

    @Test
    fun zdjecie_notatki_da_sie_podac_do_otwarcia() {
        adresDla(ZdjeciaNotatek.plik(ctx, "2026-09-29.jpg"))
    }

    @Test
    fun odcinek_wyplaty_da_sie_podac_do_otwarcia() {
        val row = pl.grafik.pracy.data.PayslipRow(
            ym = "2026-08", fileName = "2026-08.pdf",
            originalName = "odcinek.pdf", addedAt = "2026-09-01T10:00"
        )
        adresDla(Odcinki.plik(ctx, row))
    }

    @Test
    fun paczka_aktualizacji_da_sie_podac_do_instalacji() {
        adresDla(File(ctx.cacheDir, "updates/grafik.apk"))
    }

    @Test
    fun intencja_otwarcia_zdjecia_powstaje_kompletna() {
        val nazwa = "2026-09-29.jpg"
        val plik = ZdjeciaNotatek.plik(ctx, nazwa)
        plik.parentFile?.mkdirs()
        plik.writeBytes(byteArrayOf(1, 2, 3))

        val intencja = ZdjeciaNotatek.intencjaOtwarcia(ctx, nazwa)
        assertNotNull("bez intencji dotkniecie zdjecia nic nie robi", intencja)
        assertEquals("image/jpeg", intencja!!.type)
        assertEquals(android.content.Intent.ACTION_VIEW, intencja.action)
        assertTrue(
            "przegladarka obrazow musi dostac prawo odczytu",
            intencja.flags and android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION != 0
        )
    }

    @Test
    fun typ_pliku_bierze_sie_z_rozszerzenia() {
        listOf("a.png" to "image/png", "a.webp" to "image/webp", "a.jpg" to "image/jpeg")
            .forEach { (nazwa, oczekiwany) ->
                val plik = ZdjeciaNotatek.plik(ctx, nazwa)
                plik.parentFile?.mkdirs()
                plik.writeBytes(byteArrayOf(1))
                assertEquals(oczekiwany, ZdjeciaNotatek.intencjaOtwarcia(ctx, nazwa)?.type)
            }
    }
}
