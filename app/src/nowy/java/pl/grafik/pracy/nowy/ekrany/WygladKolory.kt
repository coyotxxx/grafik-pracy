package pl.grafik.pracy.nowy.ekrany

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import pl.grafik.pracy.data.SettingsStore
import pl.grafik.pracy.domain.Shift
import pl.grafik.pracy.nowy.theme.*
import pl.grafik.pracy.nowy.ui.*
import pl.grafik.pracy.ui.UiState
import pl.grafik.pracy.ui.Vm
import pl.grafik.pracy.ui.theme.Palette
import pl.grafik.pracy.ui.theme.PaletteTheme
import java.time.LocalDate

/** Typy dni, którym można ustawić kolor — kolejność jak w palecie domyślnej. */
private val TYPY = listOf(
    Shift.I to "Zmiana I", Shift.II to "Zmiana II", Shift.III to "Zmiana III",
    Shift.W5 to "Wolne", Shift.WS to "Wolne święto", Shift.DWN to "Za niedzielę",
    Shift.BWN to "Bezwzgl. wolna", Shift.URLOP to "Urlop", Shift.L4 to "Zwolnienie"
)

/**
 * „Wygląd i kolory" — odtworzone z design/mockups/Look.html.
 *
 * Zestawy i kolory to ta sama `Palette` i `PaletteTheme`, których używa klasyczna
 * aplikacja — zmiana tutaj widać także tam.
 */
@Composable
fun EkranWygladKolory(vm: Vm, naPowrot: () -> Unit) {
    val s by vm.state.collectAsState()
    var wybranyTyp by remember { mutableStateOf(Shift.I) }

    Box(Modifier.fillMaxSize()) {
        TloZPoswiata(Modifier.fillMaxSize())

        Column(
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = gornaKrawedz(), bottom = 22.dp)
                .padding(horizontal = Dim.screenGutter),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            PowrotDoUstawien(naPowrot)

            val pNag by postepWejscia(Motion.RISE_MS)
            Column(Modifier.wejscie(pNag), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Wygląd i kolory",
                    style = GrafikType.h1.copy(fontSize = 26.sp, lineHeight = 26.sp),
                    color = Tokeny.ink)
                Text("Zmiany widzisz od razu na podglądzie tygodnia.",
                    fontSize = 12.sp, fontFamily = Jakarta, color = Tokeny.inkMuted)
            }

            WyborMotywu()
            ZestawyKolorow(s, vm)
            PodgladTygodnia(s)
            KolorDnia(s, wybranyTyp) { wybranyTyp = it }
            WyborKoloru(s, vm, wybranyTyp)
        }
    }
}

// ─────────────────────────────────────────────────────────────
// MOTYW
// ─────────────────────────────────────────────────────────────

/**
 * Trzy karty z miniaturami — makieta `Look.html`. Miniatura systemowego ma ukośny
 * podział, bo ten tryb idzie za ustawieniem telefonu.
 */
@Composable
private fun WyborMotywu() {
    val p by postepWejscia(Motion.RISE_MS, 40)
    val ctx = LocalContext.current
    val zakres = rememberCoroutineScope()
    val ustawienia = remember { SettingsStore(ctx) }
    val nazwa by ustawienia.trybMotywu.collectAsState(initial = TrybMotywu.CIEMNY.name)
    val wybrany = remember(nazwa) {
        runCatching { TrybMotywu.valueOf(nazwa) }.getOrDefault(TrybMotywu.CIEMNY)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("MOTYW", style = GrafikType.sectionLabel,
            color = Tokeny.inkFaint, modifier = Modifier.wejscie(p))

        Row(Modifier.wejscie(p).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TrybMotywu.entries.forEach { tryb ->
                KartaMotywu(
                    tryb = tryb,
                    wybrany = tryb == wybrany,
                    modifier = Modifier.weight(1f)
                ) { zakres.launch { ustawienia.saveTrybMotywu(tryb.name) } }
            }
        }
    }
}

@Composable
private fun KartaMotywu(
    tryb: TrybMotywu,
    wybrany: Boolean,
    modifier: Modifier = Modifier,
    naKlik: () -> Unit
) {
    Column(
        modifier.clip(RoundedCornerShape(18.dp))
            .background(if (wybrany) Tokeny.accentTintBg else Tokeny.surface)
            .border(
                1.dp,
                if (wybrany) Tokeny.accentTintLine else Tokeny.line,
                RoundedCornerShape(18.dp)
            )
            .clickable(onClick = naKlik)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MiniaturaMotywu(tryb)
        Text(
            tryb.etykieta.replaceFirstChar { it.uppercase() },
            fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = Jakarta,
            color = if (wybrany) Tokeny.accent else Tokeny.inkMuted
        )
    }
}

/** Miniatura 52 dp: kreska tytułu i trzy kafelki w kolorach zmian. */
@Composable
private fun MiniaturaMotywu(tryb: TrybMotywu) {
    val ciemne = Color(0xFF111417) to Color(0xFF23282D)
    val jasne = Color(0xFFF5F4F0) to Color(0xFFE0DED7)
    val (tlo, obrys) = when (tryb) {
        TrybMotywu.JASNY -> jasne
        else -> ciemne
    }
    val kreska = when (tryb) {
        TrybMotywu.JASNY -> Color(0xFFB9B7B0)
        TrybMotywu.SYSTEMOWY -> Tokeny.inkIkona
        TrybMotywu.CIEMNY -> Color(0xFF6E777E)
    }
    val kafelki = when (tryb) {
        TrybMotywu.JASNY -> listOf(Color(0xFFAE9400), Color(0xFFC1351E), Color(0xFF3587D3))
        TrybMotywu.SYSTEMOWY -> listOf(Color(0xFFDAC559), Color(0xFFC1351E), Color(0xFF7ABDFF))
        TrybMotywu.CIEMNY -> listOf(Color(0xFFDAC559), Color(0xFFFF937E), Color(0xFF7ABDFF))
    }

    Column(
        Modifier.fillMaxWidth().height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(
                // Systemowy: pół na pół, po ukosie — tak jak w makiecie.
                if (tryb == TrybMotywu.SYSTEMOWY)
                    Modifier.background(
                        Brush.linearGradient(
                            0f to ciemne.first, 0.5f to ciemne.first,
                            0.5f to jasne.first, 1f to jasne.first
                        )
                    )
                else Modifier.background(tlo)
            )
            .border(
                1.dp,
                if (tryb == TrybMotywu.SYSTEMOWY) Color(0xFF3A4046) else obrys,
                RoundedCornerShape(12.dp)
            )
            .padding(6.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            Modifier.fillMaxWidth(0.6f).height(5.dp)
                .clip(RoundedCornerShape(999.dp)).background(kreska)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            kafelki.forEach { kolor ->
                Box(
                    Modifier.weight(1f).height(14.dp)
                        .clip(RoundedCornerShape(4.dp)).background(kolor)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// ZESTAW KOLORÓW
// ─────────────────────────────────────────────────────────────

@Composable
private fun ZestawyKolorow(s: UiState, vm: Vm) {
    val p by postepWejscia(Motion.RISE_MS, 80)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("ZESTAW KOLORÓW", style = GrafikType.sectionLabel,
            color = Tokeny.inkFaint, modifier = Modifier.wejscie(p))

        PaletteTheme.entries.forEach { zestaw ->
            val wybrany = s.motyw == zestaw
            Row(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(Dim.rCardSmall))
                    .background(if (wybrany) Color(0x2452D0B3) else Tokeny.surface)
                    .border(
                        1.dp,
                        if (wybrany) Color(0x7352D0B3) else Tokeny.line,
                        RoundedCornerShape(Dim.rCardSmall)
                    )
                    .clickable { vm.saveMotyw(zestaw) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(zestaw.label, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        fontFamily = Jakarta,
                        color = if (wybrany) Tokeny.accent else Tokeny.ink)
                    Text(zestaw.opis, fontSize = 11.sp, fontFamily = Jakarta,
                        color = Tokeny.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("I", "II", "III", "U").forEach { typ ->
                        val swatch = Palette.byId(s.colors[typ], zestaw)
                        Box(
                            Modifier.size(22.dp).clip(RoundedCornerShape(7.dp))
                                .background(swatch.border)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// PODGLĄD TYGODNIA
// ─────────────────────────────────────────────────────────────

@Composable
private fun PodgladTygodnia(s: UiState) {
    val p by postepWejscia(Motion.RISE_MS, 140)
    val dzis = remember { LocalDate.now() }
    val poniedzialek = remember(dzis) { dzis.minusDays(((dzis.dayOfWeek.value + 6) % 7).toLong()) }
    val dni = remember(poniedzialek, s.entries) {
        (0..6).map { i ->
            val d = poniedzialek.plusDays(i.toLong())
            d to s.entries[d]?.shift
        }
    }

    KartaUstawien(Modifier.wejscie(p)) {
        Text("PODGLĄD TYGODNIA", style = GrafikType.sectionLabel, color = Tokeny.inkFaint)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            dni.forEach { (data, zmiana) ->
                val swatch = Palette.byId(s.colors[zmiana?.code], s.motyw)
                Column(
                    Modifier.weight(1f).height(52.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (zmiana != null) swatch.fill else Tokeny.surface)
                        .border(
                            1.dp,
                            if (zmiana != null) swatch.border else Tokeny.line,
                            RoundedCornerShape(11.dp)
                        )
                        .padding(horizontal = 5.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${data.dayOfMonth}",
                        style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                            fontFamily = Jakarta, fontFeatureSettings = TNUM),
                        color = Tokeny.inkMuted
                    )
                    Text(
                        zmiana?.code ?: "", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        fontFamily = Jakarta, color = swatch.text,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// WYBÓR TYPU DNIA
// ─────────────────────────────────────────────────────────────

@Composable
private fun KolorDnia(s: UiState, wybrany: Shift, naWybor: (Shift) -> Unit) {
    val p by postepWejscia(Motion.RISE_MS, 180)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("KOLOR POJEDYNCZEGO DNIA", style = GrafikType.sectionLabel,
            color = Tokeny.inkFaint, modifier = Modifier.wejscie(p))

        TYPY.chunked(3).forEach { rzad ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                rzad.forEach { (typ, nazwa) ->
                    val swatch = Palette.byId(s.colors[typ.code], s.motyw)
                    val aktywny = typ == wybrany
                    Row(
                        Modifier.weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (aktywny) Color(0x2452D0B3) else Tokeny.surface)
                            .border(
                                1.dp,
                                if (aktywny) Color(0x7352D0B3) else Tokeny.line,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { naWybor(typ) }
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            Modifier.size(18.dp).clip(RoundedCornerShape(6.dp))
                                .background(swatch.border)
                        )
                        Text(
                            nazwa, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                            fontFamily = Jakarta, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            color = if (aktywny) Tokeny.accent else Tokeny.inkMuted
                        )
                    }
                }
                repeat(3 - rzad.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// PALETA
// ─────────────────────────────────────────────────────────────

@Composable
private fun WyborKoloru(s: UiState, vm: Vm, typ: Shift) {
    val p by postepWejscia(Motion.RISE_MS, 220)
    val wybrany = Palette.byId(s.colors[typ.code], s.motyw)
    val nazwaTypu = TYPY.firstOrNull { it.first == typ }?.second ?: typ.label

    Column(
        Modifier.wejscie(p).fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x0BFFFFFF))
            .border(1.dp, Tokeny.lineStrong, RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier.size(30.dp)
                    .drawBehind {
                        drawCircle(wybrany.border.copy(alpha = 0.35f), radius = size.minDimension * 0.85f)
                    }
                    .clip(RoundedCornerShape(10.dp))
                    .background(wybrany.border)
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(nazwaTypu, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    fontFamily = Jakarta, color = Tokeny.ink)
                Text(wybrany.label, fontSize = 11.sp, fontFamily = Jakarta,
                    color = Tokeny.inkMuted)
            }
        }

        Palette.all(s.motyw).chunked(5).forEach { rzad ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rzad.forEach { swatch ->
                    Column(
                        Modifier.weight(1f).clickable {
                            vm.saveColors(s.colors + (typ.code to swatch.id))
                        },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                                .background(swatch.border)
                                .then(
                                    if (swatch.id == wybrany.id)
                                        Modifier.border(2.dp, Tokeny.ink, RoundedCornerShape(14.dp))
                                    else Modifier
                                )
                        )
                        Text(swatch.label, fontSize = 9.sp, fontFamily = Jakarta,
                            color = Tokeny.inkIkona, maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                    }
                }
                repeat(5 - rzad.size) { Spacer(Modifier.weight(1f)) }
            }
        }

        PrzyciskDrugorzedny("Przywróć domyślne kolory", Modifier.fillMaxWidth()) {
            vm.saveColors(Palette.defaults)
        }
    }
}
