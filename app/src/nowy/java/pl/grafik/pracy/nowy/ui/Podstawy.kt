package pl.grafik.pracy.nowy.ui

import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import pl.grafik.pracy.nowy.theme.Jakarta
import pl.grafik.pracy.nowy.theme.TNUM
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

/**
 * Przełącznik z DESIGN_SPEC 4.4: tor 46×28, tło `#262B30`, obrys `#333A40`,
 * uchwyt 20 dp z odstępem 4 dp. Włączony: tor i obrys w akcencie, przesunięcie 18 dp,
 * animacja 160 ms. Klikalny jest cały wiersz, nie sam przełącznik.
 */
@Composable
fun Przelacznik(wlaczony: Boolean, naZmiane: (Boolean) -> Unit) {
    val gramy = animacjeWlaczone()
    val przesun by animateDpAsState(
        if (wlaczony) 18.dp else 0.dp,
        tween(if (gramy) 160 else 0, easing = Motion.Ease),
        label = "przelacznik"
    )
    Box(
        Modifier.size(46.dp, 28.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (wlaczony) DarkTokens.accent.copy(alpha = 0.22f) else Color(0xFF262B30))
            .border(
                1.dp,
                if (wlaczony) DarkTokens.accent.copy(alpha = 0.5f) else Color(0xFF333A40),
                RoundedCornerShape(999.dp)
            )
            .clickable { naZmiane(!wlaczony) }
    ) {
        Box(
            Modifier.padding(start = 4.dp + przesun, top = 4.dp)
                .size(20.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(if (wlaczony) DarkTokens.accent else Color(0xFF8A939B))
        )
    }
}

/** Kwadratowy przycisk steppera: 36 dp, r12, `surfaceInput`, obrys `lineInput`. */
@Composable
fun PrzyciskKwadrat(
    ikona: androidx.compose.ui.graphics.vector.ImageVector,
    opis: String,
    rozmiar: Dp = 36.dp,
    tlo: Color = DarkTokens.surfaceInput,
    obrys: Color = DarkTokens.lineInput,
    kolorIkony: Color = DarkTokens.ink2,
    akcja: () -> Unit
) {
    Box(
        Modifier.size(rozmiar).clip(RoundedCornerShape(12.dp))
            .background(tlo).border(1.dp, obrys, RoundedCornerShape(12.dp))
            .clickable(onClick = akcja),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Icon(ikona, opis, Modifier.size(15.dp), tint = kolorIkony)
    }
}

/** Pigułka z sumą godzin: wysokość 28, padding 0/10, tło `#0E1113`, obrys `lineSoft`. */
@Composable
fun Pigulka(tekst: String) {
    Box(
        Modifier.height(28.dp).clip(RoundedCornerShape(999.dp))
            .background(Color(0xFF0E1113))
            .border(1.dp, DarkTokens.lineSoft, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Text(
            tekst,
            style = androidx.compose.ui.text.TextStyle(
                fontSize = 12.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                fontFamily = Jakarta,
                fontFeatureSettings = TNUM
            ),
            color = DarkTokens.ink
        )
    }
}

/** Przycisk główny: 50 dp, r16, akcent, tekst `accentOn` 15 sp/700, cień akcentowy. */
@Composable
fun PrzyciskGlowny(tekst: String, modifier: Modifier = Modifier, akcja: () -> Unit) {
    Box(
        modifier.fillMaxWidth().height(50.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(DarkTokens.accent)
            .clickable(onClick = akcja),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Text(
            tekst, fontSize = 15.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            fontFamily = Jakarta, color = DarkTokens.accentOn
        )
    }
}

/**
 * Pole tekstowe `.fld` z makiet: 44 dp, r12, `surfaceInput`, obrys `lineInput`.
 * Zbudowane z `BasicTextField`, bo `OutlinedTextField` wymusza własne odstępy
 * i przy 44 dp przycina tekst w pionie.
 */
@Composable
fun PoleTekstowe(
    wartosc: String,
    podpowiedz: String,
    cyfry: Boolean = false,
    modifier: Modifier = Modifier.fillMaxWidth(),
    wyrownanie: androidx.compose.ui.text.style.TextAlign = androidx.compose.ui.text.style.TextAlign.Start,
    naZmiane: (String) -> Unit
) {
    androidx.compose.foundation.text.BasicTextField(
        value = wartosc,
        onValueChange = naZmiane,
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(
            color = DarkTokens.ink, fontSize = 14.sp, fontFamily = Jakarta,
            textAlign = wyrownanie, fontFeatureSettings = if (cyfry) TNUM else null
        ),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(DarkTokens.accent),
        keyboardOptions = if (cyfry)
            androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
            ) else androidx.compose.foundation.text.KeyboardOptions.Default,
        modifier = modifier.height(44.dp)
    ) { pole ->
        Box(
            Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
                .background(DarkTokens.surfaceInput)
                .border(1.dp, DarkTokens.lineInput, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp),
            contentAlignment = if (wyrownanie == androidx.compose.ui.text.style.TextAlign.End)
                Alignment.CenterEnd else Alignment.CenterStart
        ) {
            if (wartosc.isEmpty()) {
                androidx.compose.material3.Text(
                    podpowiedz, fontSize = 14.sp, fontFamily = Jakarta,
                    color = DarkTokens.inkFaint, maxLines = 1
                )
            }
            pole()
        }
    }
}

/** Przycisk drugorzędny: przezroczyste tło, obrys `lineSoft`, tekst `ink2`. */
@Composable
fun PrzyciskDrugorzedny(tekst: String, modifier: Modifier = Modifier, akcja: () -> Unit) {
    Box(
        modifier.height(44.dp).clip(RoundedCornerShape(13.dp))
            .border(1.dp, DarkTokens.lineSoft, RoundedCornerShape(13.dp))
            .clickable(onClick = akcja),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Text(
            tekst, fontSize = 13.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            fontFamily = Jakarta, color = DarkTokens.ink2
        )
    }
}

/** Animacja `rise` z makiet: opacity 0→1 i przesunięcie 14 px w górę. */
fun Modifier.wejscie(p: Float): Modifier = this
    .alpha(p)
    .layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            placeable.placeRelative(0, ((1f - p) * 14.dp.toPx()).toInt())
        }
    }

/**
 * Górna krawędź ekranu przy rysowaniu edge-to-edge.
 *
 * Makiety są rysowane na 390 × 900 dla telefonu z gestami — tam 44 dp u góry akurat
 * pokrywa pasek stanu. Na telefonie z wyższym paskiem (dziurka, trzy przyciski) sama
 * liczba z makiety już nie wystarcza i tekst wchodzi pod zegarek. Dlatego bierzemy
 * to, co większe: odstęp z makiety albo prawdziwy pasek stanu z odetchnięciem.
 */
@Composable
fun gornaKrawedz(): Dp =
    krawedz(WindowInsets.statusBars.asPaddingValues().calculateTopPadding(), Dim.topSafe, ODDECH_GORA)

/**
 * Dolna krawędź — tyle, ile zajmuje nawigacja systemowa, ale nie mniej niż
 * 18 dp z makiety. Przy gestach wychodzi wartość z makiety, przy trzech przyciskach
 * tyle, żeby nic się nie przykrywało.
 */
@Composable
fun dolnaKrawedz(minimum: Dp = MIN_DOL): Dp =
    krawedz(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(), minimum)

/** Odstęp z makiety, gdy nawigacja systemowa nic nie zajmuje (gesty, sprzętowe klawisze). */
val MIN_DOL = 18.dp

/** Ile powietrza zostawiamy pod paskiem stanu, gdy jest wyższy niż odstęp z makiety. */
val ODDECH_GORA = 10.dp

/**
 * Reguła krawędzi: bierzemy to, co większe — odstęp z makiety albo miejsce zajęte
 * przez pasek systemowy (z odetchnięciem, jeśli podane). Na telefonie z gestami
 * wychodzą dokładnie wartości z makiety, na telefonie z trzema przyciskami tyle,
 * żeby nic nie zostało przykryte.
 */
fun krawedz(inset: Dp, minimum: Dp, oddech: Dp = 0.dp): Dp =
    maxOf(minimum, inset + oddech)

/** Powrót z podstrony ustawień — „‹ Ustawienia" z makiet podstron. */
@Composable
fun PowrotDoUstawien(naPowrot: () -> Unit) {
    androidx.compose.foundation.layout.Row(
        Modifier.height(40.dp).offset(x = (-6).dp)
            .clip(RoundedCornerShape(11.dp))
            .clickable(onClick = naPowrot)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
    ) {
        androidx.compose.material3.Icon(
            pl.grafik.pracy.nowy.theme.IkonaWLewo, null,
            Modifier.size(17.dp), tint = DarkTokens.inkMuted
        )
        androidx.compose.material3.Text(
            "Ustawienia", fontSize = 13.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            fontFamily = Jakarta, color = DarkTokens.inkMuted
        )
    }
}
