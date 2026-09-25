package pl.grafik.pracy.nowy.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotateRad
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.random.Random

/**
 * Ile ścinków leci przez ekran. Przy dłuższym wysypie potrzeba ich więcej, żeby
 * deszcz był gęsty od początku do końca, a nie rozrzedzał się w połowie.
 */
private const val ILE = 80

/**
 * Czas przelotu przez cały ekran — od górnej krawędzi poza dolną.
 *
 * Siedem sekund — tyle, żeby spokojnie popatrzeć, a nie łapać wzrokiem. Ścinki
 * startują rozrzucone w czasie, więc ostatnie schodzą z ekranu jeszcze później.
 */
const val KONFETTI_MS = 7000

/** Jeden ścinek: skąd startuje, jak szybko leci i jak się obraca. */
private data class Scinek(
    val xUlamek: Float,
    val opoznienie: Float,
    val zasieg: Float,
    val zniesienie: Float,
    val obrot: Float,
    val wysokosc: Float,
    val barwa: Int
)

private fun losujScinki(ziarno: Int, ile: Int = ILE): List<Scinek> {
    val los = Random(ziarno)
    return List(ile) { i ->
        Scinek(
            xUlamek = 0.02f + los.nextFloat() * 0.96f,
            // Rozjazd startów rozbija „ścianę" ścinków na deszcz. Sięga połowy wysypu,
            // więc kiedy pierwsze schodzą z ekranu, następne dopiero wchodzą u góry.
            opoznienie = los.nextFloat() * 0.52f,
            // Ponad 1.0, żeby każdy wyleciał poza dolną krawędź, a nie zatrzymał się na niej.
            // Rozpiętość daje różne prędkości opadania — cięższe i lżejsze ścinki.
            zasieg = 1.08f + los.nextFloat() * 0.62f,
            zniesienie = los.nextFloat() * 70f - 35f,
            obrot = (los.nextFloat() * 5f - 2.5f) * PI.toFloat(),
            wysokosc = 7f + los.nextFloat() * 6f,
            barwa = i % 5
        )
    }
}

/**
 * Konfetti przez całą wysokość aplikacji — uroczystość wita cały ekran, od górnej
 * krawędzi po dolną.
 *
 * Rysujemy na jednym [Canvas], bo to warstwa bez wpływu na układ, i nie łapiemy
 * dotknięć: przez cały czas trwania da się klikać wszystko pod spodem.
 *
 * Gdy w systemie włączone jest „ogranicz animacje", nie leci nic — zgodnie z regułą
 * projektu, że każdą animację da się wyłączyć.
 */
@Composable
fun Konfetti(
    klucz: Any,
    modifier: Modifier = Modifier,
    barwy: List<Color>
) {
    if (!animacjeWlaczone()) return

    val scinki = remember(klucz) { losujScinki(klucz.hashCode()) }
    val postep = remember(klucz) { Animatable(0f) }

    LaunchedEffect(klucz) {
        postep.snapTo(0f)
        // Jedyne miejsce w aplikacji bez krzywej projektu, i to celowo: „0.22, 1, 0.36, 1"
        // to ruch typu wystrzel-i-zwolnij. Ścinki pokonywały nim większość ekranu
        // w pierwszym ułamku sekundy, a potem pełzły przy dolnej krawędzi — nie dało się
        // tego zobaczyć. Spadanie ma być równomierne.
        postep.animateTo(1f, tween(durationMillis = KONFETTI_MS, easing = LinearEasing))
    }

    val p = postep.value
    if (p >= 1f) return

    Canvas(modifier) {
        val szerokoscScinka = 5.dp.toPx()

        scinki.forEach { s ->
            // Każdy ścinek ma własne okno czasu; przed swoim opóźnieniem jeszcze nie istnieje.
            val wlasny = ((p - s.opoznienie) / (1f - s.opoznienie)).coerceIn(0f, 1f)
            if (wlasny <= 0f) return@forEach

            // Pojawia się szybko i leci widoczny aż za krawędź — nie gaśnie w połowie drogi.
            val krycie = if (wlasny < 0.10f) wlasny / 0.10f else 1f

            val x = size.width * s.xUlamek + s.zniesienie * wlasny
            val y = -20f + s.zasieg * size.height * wlasny
            val wys = s.wysokosc.dp.toPx()

            rotateRad(s.obrot * wlasny, pivot = Offset(x, y)) {
                drawRect(
                    color = barwy[s.barwa % barwy.size].copy(alpha = krycie),
                    topLeft = Offset(x - szerokoscScinka / 2f, y - wys / 2f),
                    size = Size(szerokoscScinka, wys)
                )
            }
        }
    }
}
