package pl.grafik.pracy.nowy.ekrany

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.grafik.pracy.domain.OtRate
import pl.grafik.pracy.domain.Shift
import pl.grafik.pracy.nowy.theme.*
import pl.grafik.pracy.nowy.ui.*
import pl.grafik.pracy.ui.UiState
import pl.grafik.pracy.ui.Vm
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

private val PLE = Locale.forLanguageTag("pl-PL")

/** Narzędzie z panelu: co maluje, jak się nazywa i co pokazuje w banerze. */
private data class Narzedzie(
    val shift: Shift?,
    val etykieta: String,
    val podpis: String,
    val nazwa: String,
    val meta: String
)

private val NARZEDZIA = listOf(
    Narzedzie(Shift.I, "I", "6–14", "Zmiana I · ranna", "6–14"),
    Narzedzie(Shift.II, "II", "14–22", "Zmiana II · popołudniowa", "14–22"),
    Narzedzie(Shift.III, "III", "22–6", "Zmiana III · nocna", "22–06"),
    Narzedzie(Shift.W5, "w5", "wolne", "Dzień wolny", "w5"),
    Narzedzie(Shift.URLOP, "U", "urlop", "Urlop wypoczynkowy", "U")
)

/** Pod „···" kryje się reszta oznaczeń — tak jak opisuje DESIGN_SPEC 5.3. */
private val POZOSTALE = listOf(
    Narzedzie(Shift.WS, "wś", "wolne św.", "Wolne święto", "wś"),
    Narzedzie(Shift.DWN, "DWN", "za niedz.", "Dzień za pracującą niedzielę", "DWN"),
    Narzedzie(Shift.BWN, "bezw.", "wolna nd.", "Bezwzględnie wolna niedziela", "bezw."),
    Narzedzie(Shift.L4, "L4", "zwolnienie", "Zwolnienie lekarskie", "L4"),
    Narzedzie(null, "×", "wyczyść", "Wyczyść dzień", "—")
)

/**
 * Tryb edycji — odtworzony z design/mockups/Edit.html.
 * Osobny ekran zamiast blokady malowania: wejście w tryb jest świadomą decyzją,
 * więc przypadkowe dotknięcie dnia niczego nie zmieni.
 */
@Composable
fun TrybEdycji(vm: Vm, naGotowe: () -> Unit) {
    val s by vm.state.collectAsState()
    var wybrane by remember { mutableStateOf(NARZEDZIA[0]) }
    var pozostaleOtwarte by remember { mutableStateOf(false) }
    var ngWlaczone by remember { mutableStateOf(false) }
    var ngGodziny by remember { mutableIntStateOf(8) }
    var ngStawka by remember { mutableStateOf(OtRate.P100) }

    val kolory = ShiftPaletteDark.of(typDniaZ(wybrane.shift))

    Box(Modifier.fillMaxSize().background(DarkTokens.bg)) {
        Column(Modifier.fillMaxSize().padding(top = Dim.topSafe)) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PasekEdycji(s, vm, naGotowe)
                BanerNarzedzia(wybrane, kolory)
                SiatkaEdycji(s, wybrane, ngWlaczone, ngGodziny, ngStawka, vm)
                Spacer(Modifier.height(4.dp))
            }
            PanelNarzedzi(
                wybrane = wybrane,
                pozostaleOtwarte = pozostaleOtwarte,
                ngWlaczone = ngWlaczone,
                ngGodziny = ngGodziny,
                ngStawka = ngStawka,
                naNarzedzie = { wybrane = it; pozostaleOtwarte = false },
                naWiecej = { pozostaleOtwarte = !pozostaleOtwarte },
                naNg = { ngWlaczone = it },
                naGodziny = { ngGodziny = it.coerceIn(1, 12) },
                naStawke = { ngStawka = it },
                naPrzywroc = { vm.resetMonth() }
            )
        }
    }
}

@Composable
private fun PasekEdycji(s: UiState, vm: Vm, naGotowe: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Dim.screenGutter),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Tryb edycji", fontSize = 15.sp, fontWeight = FontWeight.Bold,
                fontFamily = Jakarta, color = DarkTokens.ink)
            Text(
                s.ym.month.getDisplayName(JavaTextStyle.FULL_STANDALONE, PLE)
                    .replaceFirstChar { it.uppercase() } + " ${s.ym.year} · zmiany zapisują się od razu",
                style = GrafikType.caption, color = DarkTokens.inkMuted,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(13.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, DarkTokens.lineStrong, RoundedCornerShape(13.dp))
                    .clickable { vm.undo() },
                contentAlignment = Alignment.Center
            ) { Icon(IkonaCofnij, "Cofnij ostatnią zmianę", Modifier.size(17.dp), tint = DarkTokens.ink2) }
            Box(
                Modifier.height(40.dp).clip(RoundedCornerShape(13.dp))
                    .background(DarkTokens.accent).clickable(onClick = naGotowe)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Gotowe", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    fontFamily = Jakarta, color = DarkTokens.accentOn)
            }
        }
    }
}

@Composable
private fun BanerNarzedzia(n: Narzedzie, k: DayColors) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Dim.screenGutter)
            .clip(RoundedCornerShape(16.dp))
            .background(k.fill).border(1.dp, k.line, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(12.dp))
                .background(DarkTokens.bg).border(1.dp, k.line, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(n.etykieta, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 0.52.sp, fontFamily = Jakarta, color = k.ink)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(n.nazwa, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                fontFamily = Jakarta, color = DarkTokens.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("dotykaj dni, aby ustawić", style = GrafikType.caption, color = DarkTokens.inkMuted)
        }
        Text(
            n.meta,
            style = TextStyle(fontFamily = Jakarta, fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold, fontFeatureSettings = TNUM),
            color = k.ink
        )
    }
}

@Composable
private fun SiatkaEdycji(
    s: UiState, n: Narzedzie,
    ngWlaczone: Boolean, ngGodziny: Int, ngStawka: OtRate,
    vm: Vm
) {
    val pierwszy = s.ym.atDay(1)
    val start = pierwszy.minusDays((pierwszy.dayOfWeek.value - 1).toLong())

    Column(
        Modifier.padding(horizontal = Dim.screenGutter),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("PN", "WT", "ŚR", "CZ", "PT", "SO", "ND").forEachIndexed { i, d ->
                Text(
                    d, Modifier.weight(1f), textAlign = TextAlign.Center,
                    fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp, fontFamily = Jakarta,
                    color = if (i >= 5) DarkTokens.inkFaint else DarkTokens.inkMuted
                )
            }
        }
        repeat(6) { w ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(7) { kol ->
                    val d = start.plusDays((w * 7 + kol).toLong())
                    val e = s.entries[d]
                    val poza = YearMonth.from(d) != s.ym
                    val k = ShiftPaletteDark.of(typDniaZ(e?.shift))
                    val ot = e?.otHours ?: 0

                    Box(
                        Modifier.weight(1f).height(Dim.dayCellEdit)
                            .clip(RoundedCornerShape(13.dp))
                            .background(if (poza) Color.Transparent else k.fill)
                            .border(1.dp, if (poza) Color(0xFF171B1E) else k.line, RoundedCornerShape(13.dp))
                            .then(
                                // Pierścień na dniach nadpisanych ręcznie — reszta jest z cyklu.
                                if (!poza && d in s.reczne)
                                    Modifier.border(1.dp, DarkTokens.accent.copy(alpha = 0.55f), RoundedCornerShape(13.dp))
                                else Modifier
                            )
                            .clickable(enabled = !poza) {
                                vm.paintDay(
                                    d, n.shift,
                                    if (ngWlaczone) ngGodziny else null,
                                    ngStawka
                                )
                            }
                            .padding(6.dp)
                    ) {
                        Row(Modifier.fillMaxWidth().align(Alignment.TopStart), verticalAlignment = Alignment.Bottom) {
                            Text(
                                "${d.dayOfMonth}",
                                style = TextStyle(
                                    fontFamily = Jakarta, fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold, fontFeatureSettings = TNUM
                                ),
                                color = when {
                                    poza -> DarkTokens.inkDisabled
                                    e?.shift?.isWork != true -> DarkTokens.inkMuted
                                    else -> DarkTokens.inkStrong
                                }
                            )
                            Spacer(Modifier.weight(1f))
                            if (ot > 0 && !poza) {
                                Text("+$ot", fontSize = 9.sp, fontWeight = FontWeight.Bold,
                                    fontFamily = Jakarta, color = ShiftPaletteDark.I.ink)
                            }
                        }
                        if (!poza) {
                            val etykieta = e?.shift?.code.orEmpty()
                            if (etykieta.isNotEmpty()) {
                                Text(
                                    etykieta, Modifier.align(Alignment.BottomStart),
                                    fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.72.sp, fontFamily = Jakarta, color = k.ink
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PanelNarzedzi(
    wybrane: Narzedzie,
    pozostaleOtwarte: Boolean,
    ngWlaczone: Boolean,
    ngGodziny: Int,
    ngStawka: OtRate,
    naNarzedzie: (Narzedzie) -> Unit,
    naWiecej: () -> Unit,
    naNg: (Boolean) -> Unit,
    naGodziny: (Int) -> Unit,
    naStawke: (OtRate) -> Unit,
    naPrzywroc: () -> Unit
) {
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            .background(Color(0xF50F1214))
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            NARZEDZIA.forEach { n ->
                KafelekNarzedzia(n, n == wybrane, Modifier.weight(1f)) { naNarzedzie(n) }
            }
            KafelekWiecej(pozostaleOtwarte, Modifier.weight(1f), naWiecej)
        }

        if (pozostaleOtwarte) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                POZOSTALE.forEach { n ->
                    KafelekNarzedzia(n, n == wybrane, Modifier.weight(1f)) { naNarzedzie(n) }
                }
            }
        }

        WierszNadgodzinEdycji(ngWlaczone, ngGodziny, ngStawka, naNg, naGodziny, naStawke)

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Malowanie nadpisuje dni z cyklu. Cofnij przywraca poprzedni stan.",
                Modifier.weight(1f), style = GrafikType.caption, color = DarkTokens.inkFaint
            )
            Box(
                Modifier.height(34.dp).clip(RoundedCornerShape(11.dp))
                    .border(1.dp, DarkTokens.lineSoft, RoundedCornerShape(11.dp))
                    .clickable(onClick = naPrzywroc).padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Przywróć cykl", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    fontFamily = Jakarta, color = DarkTokens.inkMuted)
            }
        }
    }
}

@Composable
private fun KafelekNarzedzia(n: Narzedzie, aktywne: Boolean, modifier: Modifier, akcja: () -> Unit) {
    val k = ShiftPaletteDark.of(typDniaZ(n.shift))
    Column(
        modifier.height(56.dp).clip(RoundedCornerShape(15.dp))
            .background(if (aktywne) k.fill else DarkTokens.surfaceInput)
            .border(1.dp, if (aktywne) k.line else Color(0xFF20252A), RoundedCornerShape(15.dp))
            .clickable(onClick = akcja).padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically)
    ) {
        Text(n.etykieta, fontSize = 14.sp, fontWeight = FontWeight.Bold,
            letterSpacing = 0.56.sp, fontFamily = Jakarta,
            color = if (aktywne) k.ink else DarkTokens.ink3, maxLines = 1)
        Text(n.podpis, fontSize = 8.sp, fontWeight = FontWeight.Medium,
            fontFamily = Jakarta, color = Color(0xFF8A939B), maxLines = 1)
    }
}

@Composable
private fun KafelekWiecej(otwarte: Boolean, modifier: Modifier, akcja: () -> Unit) {
    Column(
        modifier.height(56.dp).clip(RoundedCornerShape(15.dp))
            .background(if (otwarte) Color.White.copy(alpha = 0.04f) else DarkTokens.surfaceInput)
            .border(1.dp, if (otwarte) Color(0xFF252A2F) else Color(0xFF20252A), RoundedCornerShape(15.dp))
            .clickable(onClick = akcja).padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically)
    ) {
        Text("···", fontSize = 14.sp, fontWeight = FontWeight.Bold,
            fontFamily = Jakarta, color = DarkTokens.ink2)
        Text("więcej", fontSize = 8.sp, fontWeight = FontWeight.Medium,
            fontFamily = Jakarta, color = Color(0xFF8A939B))
    }
}

@Composable
private fun WierszNadgodzinEdycji(
    wlaczone: Boolean, godziny: Int, stawka: OtRate,
    naNg: (Boolean) -> Unit, naGodziny: (Int) -> Unit, naStawke: (OtRate) -> Unit
) {
    val bursztyn = ShiftPaletteDark.I
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(if (wlaczone) bursztyn.fill else DarkTokens.surface)
            .border(1.dp, if (wlaczone) bursztyn.line else DarkTokens.line, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            Modifier.weight(1f).clickable { naNg(!wlaczone) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                Modifier.size(34.dp).clip(RoundedCornerShape(11.dp))
                    .background(if (wlaczone) bursztyn.line else DarkTokens.surfaceInput),
                contentAlignment = Alignment.Center
            ) {
                Icon(IkonaZegarek, null, Modifier.size(16.dp),
                    tint = if (wlaczone) bursztyn.ink else DarkTokens.ink2)
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Nadgodziny", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    fontFamily = Jakarta, color = if (wlaczone) bursztyn.ink else DarkTokens.ink)
                Text(
                    if (wlaczone) "dopisywane do malowanych dni" else "wyłączone — dotknij, aby dopisywać",
                    fontSize = 10.sp, lineHeight = 13.sp, fontFamily = Jakarta,
                    color = DarkTokens.inkMuted, maxLines = 2, overflow = TextOverflow.Ellipsis
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            PrzyciskKwadrat(IkonaMinus, "Mniej godzin", rozmiar = 34.dp) { naGodziny(godziny - 1) }
            Text(
                "$godziny", Modifier.widthIn(min = 34.dp),
                style = TextStyle(fontFamily = Jakarta, fontSize = 13.sp,
                    fontWeight = FontWeight.Bold, fontFeatureSettings = TNUM),
                color = if (wlaczone) bursztyn.ink else DarkTokens.ink, textAlign = TextAlign.Center
            )
            PrzyciskKwadrat(IkonaPlus, "Więcej godzin", rozmiar = 34.dp) { naGodziny(godziny + 1) }
            val setka = stawka == OtRate.P100
            Box(
                Modifier.height(34.dp).widthIn(min = 46.dp).clip(RoundedCornerShape(11.dp))
                    .background(if (setka) bursztyn.fill else DarkTokens.surfaceInput)
                    .border(1.dp, if (setka) bursztyn.line else DarkTokens.lineInput, RoundedCornerShape(11.dp))
                    .clickable { naStawke(if (setka) OtRate.P50 else OtRate.P100) }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("${stawka.percent}%", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    fontFamily = Jakarta, color = if (setka) bursztyn.ink else DarkTokens.inkMuted)
            }
        }
    }
}
