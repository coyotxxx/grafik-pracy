package pl.grafik.pracy

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import pl.grafik.pracy.events.ekranStartowy

/**
 * Powiadomienie ma otwierać ekran startowy TEJ aplikacji.
 *
 * Wcześniej wskazywało na sztywno `MainActivity`, przez co w wariancie „nowy"
 * dotknięcie powiadomienia otwierało stary wygląd — stara aktywność nadal siedzi
 * w pakiecie, usuwamy jej tylko ikonę z pulpitu.
 */
@RunWith(AndroidJUnit4::class)
class PowiadomienieOtwieraTest {

    private val ctx: Context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun powiadomienie_wskazuje_ekran_startowy_tego_wariantu() {
        val intencja = ekranStartowy(ctx)
        val cel = intencja.component

        assertNotNull("intencja musi wskazywać konkretną aktywność", cel)
        assertEquals("i to aktywność tej aplikacji, nie innego wariantu",
            ctx.packageName, cel!!.packageName)
    }

    @Test
    fun wskazana_aktywnosc_da_sie_uruchomic() {
        val intencja = ekranStartowy(ctx)
        val rozwiazane = ctx.packageManager.resolveActivity(intencja, 0)
        assertNotNull("system musi umieć otworzyć ten ekran", rozwiazane)
    }
}
