package pl.grafik.pracy.nowy

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import pl.grafik.pracy.events.ekranStartowy

/**
 * W wariancie „nowy" powiadomienie musi otwierać NOWY wygląd.
 *
 * Maciej zgłosił 25.09.2026: „wszedłem w powiadomienie i pokazało mi starą szatę
 * graficzną, choć odinstalowałem starą wersję". Stara aktywność jest w pakiecie
 * i to ją otwierało.
 */
@RunWith(AndroidJUnit4::class)
class PowiadomienieNowyTest {

    private val ctx: Context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun powiadomienie_otwiera_nowy_wyglad_a_nie_stary() {
        val cel = ekranStartowy(ctx).component
        assertEquals(
            "powiadomienie ma otwierać NowaActivity, nie MainActivity",
            NowaActivity::class.java.name, cel?.className
        )
    }
}
