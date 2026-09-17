package pl.grafik.pracy.nowy.ui

import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pl.grafik.pracy.nowy.theme.DarkTokens
import pl.grafik.pracy.nowy.theme.Dim
import pl.grafik.pracy.nowy.theme.Motion

/**
 * Czy odtwarzać animacje. Makiety respektują `prefers-reduced-motion`, a jego
 * odpowiednikiem na Androidzie jest wyzerowana skala animacji w ustawieniach systemu.
 * Przy wyłączonych animacjach pokazujemy od razu stan końcowy — tak każe DESIGN_SPEC 2.6.
 */
@Composable
fun animacjeWlaczone(): Boolean {
    val ctx = LocalContext.current
    return remember {
        runCatching {
            Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
        }.getOrDefault(true)
    }
}

/**
 * Postęp animacji wejścia, 0→1 w podanym czasie, z krzywą projektu.
 * Zwraca od razu 1f, gdy animacje są wyłączone.
 */
@Composable
fun postepWejscia(czasMs: Int, opoznienieMs: Int = 0): State<Float> {
    val gramy = animacjeWlaczone()
    val p = remember { Animatable(if (gramy) 0f else 1f) }
    LaunchedEffect(gramy) {
        if (!gramy) { p.snapTo(1f); return@LaunchedEffect }
        p.animateTo(
            1f,
            tween(durationMillis = czasMs, delayMillis = opoznienieMs, easing = Motion.Ease)
        )
    }
    return p.asState()
}

/**
 * Tło ekranu z dwiema poświatami (klasa `.halo` w makietach).
 * Górna niebieska i dolna zielona, oddychające w pętli — 18 s i 23 s, naprzemiennie.
 */
@Composable
fun TloZPoswiata(modifier: Modifier = Modifier) {
    val gramy = animacjeWlaczone()
    val ruch = rememberInfiniteTransition(label = "halo")
    val przesun by if (gramy) ruch.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(18_000, easing = Motion.Ease), RepeatMode.Reverse),
        label = "halo1"
    ) else remember { mutableFloatStateOf(0f) }
    val przesun2 by if (gramy) ruch.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(23_000, easing = Motion.Ease), RepeatMode.Reverse),
        label = "halo2"
    ) else remember { mutableFloatStateOf(0f) }

    Box(
        modifier.background(DarkTokens.bg).drawBehind {
            // górna: 520×520 w (-110, -230), rgba(53,135,211,0.26) → 0
            val r1 = 260.dp.toPx()
            val c1 = Offset(150.dp.toPx(), 30.dp.toPx() - przesun * 18.dp.toPx())
            drawCircle(
                Brush.radialGradient(
                    0f to Color(0xFF3587D3).copy(alpha = 0.26f),
                    0.48f to Color(0xFF3587D3).copy(alpha = 0.07f),
                    0.72f to Color.Transparent,
                    center = c1, radius = r1
                ),
                radius = r1, center = c1
            )
            // dolna: 440×440 przy prawej krawędzi, rgba(82,208,179,0.12) → 0
            val r2 = 220.dp.toPx()
            val c2 = Offset(size.width + 70.dp.toPx(), size.height + 20.dp.toPx() - przesun2 * 22.dp.toPx())
            drawCircle(
                Brush.radialGradient(
                    0f to Color(0xFF52D0B3).copy(alpha = 0.12f),
                    0.70f to Color.Transparent,
                    center = c2, radius = r2
                ),
                radius = r2, center = c2
            )
        }
    )
}

/** Karta z makiety: tło `surface`, obrys 1 dp `line`, promień 18 dp, padding 14/16. */
@Composable
fun Karta(
    modifier: Modifier = Modifier,
    tlo: Color = DarkTokens.surface,
    obrys: Color = DarkTokens.line,
    promien: Dp = Dim.rCardSmall,
    paddingPion: Dp = 14.dp,
    paddingBok: Dp = 16.dp,
    tresc: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier.fillMaxWidth()
            .clip(RoundedCornerShape(promien))
            .background(tlo)
            .border(1.dp, obrys, RoundedCornerShape(promien))
            .padding(vertical = paddingPion, horizontal = paddingBok),
        content = tresc
    )
}

/**
 * Pasek postępu z karty godzin: wysokość 6 dp, tor `#1E2327`, wypełnienie akcentem
 * z poświatą. Animacja `grow` — 1300 ms, opóźnienie 300 ms.
 */
@Composable
fun PasekPostepu(
    udzial: Float,
    modifier: Modifier = Modifier,
    wysokosc: Dp = 6.dp,
    kolor: Color = DarkTokens.accent
) {
    val p by postepWejscia(Motion.BAR_GROW_MS, Motion.BAR_GROW_DELAY_MS)
    Box(
        modifier.fillMaxWidth().height(wysokosc)
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFF1E2327))
    ) {
        Box(
            Modifier.fillMaxWidth(udzial.coerceIn(0f, 1f) * p)
                .fillMaxHeight()
                .clip(RoundedCornerShape(999.dp))
                .drawBehind {
                    drawRect(kolor.copy(alpha = 0.45f), Offset(0f, -4f), Size(size.width, size.height + 8f))
                }
                .background(kolor)
        )
    }
}

/** Licznik odliczający od zera, `1-(1-p)³`, 1100 ms — jak w makietach. */
@Composable
fun licznikDo(wartosc: Int, czasMs: Int = Motion.COUNT_UP_MS): Int {
    val p by postepWejscia(czasMs)
    return (wartosc * Motion.easeOutCubic(p)).toInt()
}
