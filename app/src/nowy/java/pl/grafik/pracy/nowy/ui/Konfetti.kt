package pl.grafik.pracy.nowy.ui

import androidx.compose.animation.core.Animatable
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pl.grafik.pracy.nowy.theme.Motion
import kotlin.math.PI
import kotlin.random.Random

/** Ile ścinków leci w jednym wysypie — tyle, żeby było wesoło, ale nie tłoczno. */
private const val ILE = 26

/** Czas całego wysypu. Krótko, bo to ma minąć zanim zdążysz przeczytać nagłówek. */
const val KONFETTI_MS = 1100

/** Jeden ścinek: skąd startuje, dokąd leci i jak się obraca. */
private data class Scinek(
    val xUlamek: Float,
    val opoznienie: Float,
    val spadek: Float,
    val zniesienie: Float,
    val obrot: Float,
    val wysokosc: Float,
    val barwa: Int
)

private fun losujScinki(ziarno: Int, ile: Int = ILE): List<Scinek> {
    val los = Random(ziarno)
    return List(ile) { i ->
        Scinek(
            xUlamek = 0.04f + los.nextFloat() * 0.92f,
            // Rozjazd startów rozbija „ścianę" ścinków na deszcz.
            opoznienie = los.nextFloat() * 0.30f,
            spadek = 110f + los.nextFloat() * 90f,
            zniesienie = los.nextFloat() * 44f - 22f,
            obrot = (los.nextFloat() * 4f - 2f) * PI.toFloat(),
            wysokosc = 7f + los.nextFloat() * 5f,
            barwa = i % 5
        )
    }
}

/**
 * Krótki wysyp konfetti nad treścią — uroczystość w karcie dnia.
 *
 * Rysujemy na [Canvas], a nie dwudziestoma sześcioma polami, bo to jedna warstwa
 * bez wpływu na układ. Warstwa nie łapie dotknięć: pod spodem da się klikać przez
 * cały czas trwania animacji.
 *
 * Gdy w systemie włączone jest „ogranicz animacje", nie leci nic — zgodnie z regułą
 * projektu, że każda animacja daje się wyłączyć.
 */
@Composable
fun Konfetti(
    klucz: Any,
    modifier: Modifier = Modifier,
    barwy: List<Color>,
    wysokoscObszaru: Dp = 150.dp
) {
    if (!animacjeWlaczone()) return

    val scinki = remember(klucz) { losujScinki(klucz.hashCode()) }
    val postep = remember(klucz) { Animatable(0f) }

    LaunchedEffect(klucz) {
        postep.snapTo(0f)
        postep.animateTo(1f, tween(durationMillis = KONFETTI_MS, easing = Motion.Ease))
    }

    val p = postep.value
    if (p >= 1f) return

    Canvas(modifier) {
        val szerokoscScinka = 5.dp.toPx()
        val zasieg = wysokoscObszaru.toPx()

        scinki.forEach { s ->
            // Każdy ścinek ma własne okno czasu; przed swoim opóźnieniem jeszcze nie istnieje.
            val wlasny = ((p - s.opoznienie) / (1f - s.opoznienie)).coerceIn(0f, 1f)
            if (wlasny <= 0f) return@forEach

            // Widoczność: szybkie pojawienie, długi lot, zgaśnięcie na końcu.
            val krycie = when {
                wlasny < 0.12f -> wlasny / 0.12f
                wlasny > 0.80f -> (1f - wlasny) / 0.20f
                else -> 1f
            }

            val x = size.width * s.xUlamek + s.zniesienie * wlasny
            val y = -12f + (s.spadek / 200f) * zasieg * wlasny
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
