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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                    color = DarkTokens.ink)
                Text("Zmiany widzisz od razu na podglądzie tygodnia.",
                    fontSize = 12.sp, fontFamily = Jakarta, color = DarkTokens.inkMuted)
            }

            ZestawyKolorow(s, vm)
            PodgladTygodnia(s)
            KolorDnia(s, wybranyTyp) { wybranyTyp = it }
            WyborKoloru(s, vm, wybranyTyp)
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
            color = DarkTokens.inkFaint, modifier = Modifier.wejscie(p))

        PaletteTheme.entries.forEach { zestaw ->
            val wybrany = s.motyw == zestaw
            Row(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(Dim.rCardSmall))
                    .background(if (wybrany) Color(0x2452D0B3) else DarkTokens.surface)
                    .border(
                        1.dp,
                        if (wybrany) Color(0x7352D0B3) else DarkTokens.line,
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
                        color = if (wybrany) DarkTokens.accent else DarkTokens.ink)
                    Text(zestaw.opis, fontSize = 11.sp, fontFamily = Jakarta,
                        color = DarkTokens.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
        Text("PODGLĄD TYGODNIA", style = GrafikType.sectionLabel, color = DarkTokens.inkFaint)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            dni.forEach { (data, zmiana) ->
                val swatch = Palette.byId(s.colors[zmiana?.code], s.motyw)
                Column(
                    Modifier.weight(1f).height(52.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (zmiana != null) swatch.fill else DarkTokens.surface)
                        .border(
                            1.dp,
                            if (zmiana != null) swatch.border else DarkTokens.line,
                            RoundedCornerShape(11.dp)
                        )
                        .padding(horizontal = 5.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${data.dayOfMonth}",
                        style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                            fontFamily = Jakarta, fontFeatureSettings = TNUM),
                        color = DarkTokens.inkMuted
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
            color = DarkTokens.inkFaint, modifier = Modifier.wejscie(p))

        TYPY.chunked(3).forEach { rzad ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                rzad.forEach { (typ, nazwa) ->
                    val swatch = Palette.byId(s.colors[typ.code], s.motyw)
                    val aktywny = typ == wybrany
                    Row(
                        Modifier.weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (aktywny) Color(0x2452D0B3) else DarkTokens.surface)
                            .border(
                                1.dp,
                                if (aktywny) Color(0x7352D0B3) else DarkTokens.line,
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
                            color = if (aktywny) DarkTokens.accent else DarkTokens.inkMuted
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
            .border(1.dp, DarkTokens.lineStrong, RoundedCornerShape(20.dp))
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
                    fontFamily = Jakarta, color = DarkTokens.ink)
                Text(wybrany.label, fontSize = 11.sp, fontFamily = Jakarta,
                    color = DarkTokens.inkMuted)
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
                                        Modifier.border(2.dp, DarkTokens.ink, RoundedCornerShape(14.dp))
                                    else Modifier
                                )
                        )
                        Text(swatch.label, fontSize = 9.sp, fontFamily = Jakarta,
                            color = Color(0xFF8A939B), maxLines = 1,
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
