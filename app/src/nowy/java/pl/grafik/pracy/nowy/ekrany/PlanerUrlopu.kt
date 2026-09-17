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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.grafik.pracy.domain.CycleGenerator
import pl.grafik.pracy.domain.VacationSuggestion
import pl.grafik.pracy.nowy.theme.*
import pl.grafik.pracy.nowy.ui.*
import pl.grafik.pracy.ui.UiState
import pl.grafik.pracy.ui.Vm
import java.time.LocalDate
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

private val PL_PLAN = Locale.forLanguageTag("pl-PL")

/** Po czym układamy propozycje — trzy filtry z makiety. */
private enum class Filtr(val etykieta: String) {
    ZYSK("Najlepszy zysk"), DLUGOSC("Najdłuższe"), BLISKO("Najbliższe")
}

/**
 * „Kiedy wziąć urlop" — odtworzone z design/mockups/Vacation.html.
 *
 * Propozycje liczy `VacationPlanner` z cyklu i kalendarza świąt. Zgodnie z decyzją
 * Macieja z CLAUDE.md ekran jest **tylko do przeglądania** — nie ma przycisku
 * „Wpisz w grafik", urlop wpisuje się samemu w trybie edycji.
 */
@Composable
fun EkranPlanerUrlopu(vm: Vm, naPowrot: () -> Unit) {
    val s by vm.state.collectAsState()
    val propozycje by vm.plan.collectAsState()
    var filtr by remember { mutableStateOf(Filtr.ZYSK) }

    LaunchedEffect(Unit) { vm.policzPlan() }

    val ulozone = remember(propozycje, filtr) {
        when (filtr) {
            Filtr.ZYSK -> propozycje.sortedByDescending { it.oplacalnosc }
            Filtr.DLUGOSC -> propozycje.sortedByDescending { it.dlugosc }
            Filtr.BLISKO -> propozycje.sortedBy { it.wolneOd }
        }
    }

    Box(Modifier.fillMaxSize()) {
        TloZPoswiata(Modifier.fillMaxSize())

        Column(
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = gornaKrawedz(), bottom = 22.dp)
                .padding(horizontal = Dim.screenGutter),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            PowrotDoBilansu(naPowrot)

            val pNag by postepWejscia(Motion.RISE_MS)
            Column(Modifier.wejscie(pNag)) {
                Text("Kiedy wziąć urlop",
                    style = GrafikType.h1.copy(lineHeight = 28.sp), color = DarkTokens.ink)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Miejsca, w których kilka dni urlopu daje najdłuższy ciąg wolnego. " +
                        "Liczone rok do przodu z Twojego cyklu i kalendarza świąt.",
                    fontSize = 12.sp, lineHeight = 18.sp, fontFamily = Jakarta,
                    color = DarkTokens.inkMuted
                )
            }

            KartaCykluPlanera(s)
            Filtry(filtr) { filtr = it }

            if (!s.cfg.generate) {
                BrakCyklu()
            } else if (ulozone.isEmpty()) {
                BrakPropozycji(s)
            } else {
                ulozone.forEachIndexed { i, p -> KartaPropozycji(p, i) }
            }
        }
    }
}

@Composable
private fun PowrotDoBilansu(naPowrot: () -> Unit) {
    Row(
        Modifier.height(40.dp).offset(x = (-6).dp)
            .clip(RoundedCornerShape(11.dp))
            .clickable(onClick = naPowrot)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(IkonaWLewo, null, Modifier.size(17.dp), tint = DarkTokens.inkMuted)
        Text("Bilans", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            fontFamily = Jakarta, color = DarkTokens.inkMuted)
    }
}

// ─────────────────────────────────────────────────────────────
// KARTA CYKLU I FILTRY
// ─────────────────────────────────────────────────────────────

@Composable
private fun KartaCykluPlanera(s: UiState) {
    val p by postepWejscia(Motion.RISE_MS, 50)
    val rozowy = ShiftPaletteDark.URLOP.ink
    val rotacja = remember(s.cfg) { CycleGenerator.rotationLabel(s.cfg) }

    Row(
        Modifier.wejscie(p).fillMaxWidth()
            .clip(RoundedCornerShape(Dim.rCardSmall))
            .background(DarkTokens.surface)
            .border(1.dp, DarkTokens.line, RoundedCornerShape(Dim.rCardSmall))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(11.dp))
                .background(Color(0x24F490D9)),
            contentAlignment = Alignment.Center
        ) { Icon(IkonaCykl, null, Modifier.size(17.dp), tint = rozowy) }

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(s.cfg.pattern.label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                fontFamily = Jakarta, color = DarkTokens.ink,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "brygada ${s.cfg.brigade} · $rotacja" +
                    if (s.cfg.anchorIndex > 0) " · przesunięcie ${s.cfg.anchorIndex}" else "",
                fontSize = 11.sp, fontFamily = Jakarta, color = DarkTokens.inkMuted,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            Modifier.height(26.dp).clip(RoundedCornerShape(999.dp))
                .background(Color(0x24F490D9))
                .border(1.dp, Color(0x52F490D9), RoundedCornerShape(999.dp))
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "${s.urlopBilans.zostalo} dni",
                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    fontFamily = Jakarta, fontFeatureSettings = TNUM),
                color = rozowy
            )
        }
    }
}

@Composable
private fun Filtry(wybrany: Filtr, naWybor: (Filtr) -> Unit) {
    val p by postepWejscia(Motion.RISE_MS, 80)
    Row(
        Modifier.wejscie(p).fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Filtr.entries.forEach { f ->
            val aktywny = f == wybrany
            Box(
                Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(12.dp))
                    .background(if (aktywny) Color(0x2452D0B3) else DarkTokens.surface)
                    .border(
                        1.dp,
                        if (aktywny) Color(0x7352D0B3) else DarkTokens.line,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { naWybor(f) },
                contentAlignment = Alignment.Center
            ) {
                Text(f.etykieta, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    fontFamily = Jakarta, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = if (aktywny) DarkTokens.accent else DarkTokens.inkMuted)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// PROPOZYCJA
// ─────────────────────────────────────────────────────────────

@Composable
private fun KartaPropozycji(p: VacationSuggestion, indeks: Int) {
    val wejscie by postepWejscia(Motion.RISE_MS, 110 + indeks * 40)
    val rozowy = ShiftPaletteDark.URLOP.ink
    // Im więcej dni wolnego za jeden dzień urlopu, tym mocniejszy kolor liczby.
    val swietny = p.oplacalnosc >= 3.0
    val kolorLiczby = if (swietny) DarkTokens.accent else DarkTokens.ink

    Column(
        Modifier.wejscie(wejscie).fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (swietny) Color(0x1452D0B3) else DarkTokens.surface)
            .border(
                1.dp,
                if (swietny) Color(0x4D52D0B3) else DarkTokens.line,
                RoundedCornerShape(20.dp)
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "${p.dlugosc}",
                        style = GrafikType.heroNumber.copy(fontSize = 28.sp, lineHeight = 25.sp),
                        color = kolorLiczby, modifier = Modifier.alignByBaseline()
                    )
                    Text("dni wolnego pod rząd", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        fontFamily = Jakarta, color = DarkTokens.ink3,
                        modifier = Modifier.alignByBaseline())
                }
                Text(zakres(p), fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    fontFamily = Jakarta, color = DarkTokens.ink)
            }

            Column(
                Modifier.size(58.dp, 52.dp).clip(RoundedCornerShape(15.dp))
                    .background(Color(0x1FF490D9))
                    .border(1.dp, Color(0x47F490D9), RoundedCornerShape(15.dp)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically)
            ) {
                Text(
                    "${p.koszt}",
                    style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        fontFamily = Jakarta, fontFeatureSettings = TNUM),
                    color = rozowy
                )
                Text("dni urlopu", fontSize = 9.sp, fontFamily = Jakarta, color = DarkTokens.inkMuted)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Icon(IkonaUrlop, null, Modifier.size(13.dp), tint = rozowy)
            Text("urlop: ${dniUrlopu(p)}", fontSize = 11.sp, fontFamily = Jakarta,
                color = DarkTokens.ink2, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }

        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Box(
                Modifier.size(6.dp).clip(RoundedCornerShape(999.dp))
                    .background(if (p.swieta.isEmpty()) DarkTokens.inkDisabled else ShiftPaletteDark.I.ink)
            )
            Text(
                opisSwiat(p), fontSize = 11.sp, fontFamily = Jakarta,
                color = if (p.swieta.isEmpty()) DarkTokens.inkFaint else ShiftPaletteDark.I.ink,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFF1E2327))
            ) {
                // Pełny pasek przy czterech dniach wolnego za jeden dzień urlopu.
                Box(
                    Modifier.fillMaxWidth((p.oplacalnosc / 4.0).coerceIn(0.0, 1.0).toFloat())
                        .fillMaxHeight().clip(RoundedCornerShape(999.dp)).background(kolorLiczby)
                )
            }
            Text(
                "%.1f× za dzień".format(p.oplacalnosc),
                style = TextStyle(fontSize = 10.sp, fontFamily = Jakarta, fontFeatureSettings = TNUM),
                color = DarkTokens.inkMuted
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// STANY PUSTE
// ─────────────────────────────────────────────────────────────

@Composable
private fun BrakCyklu() {
    KartaUstawien {
        Text("Najpierw włącz cykl", style = GrafikType.cardTitle, color = DarkTokens.ink)
        Text(
            "Propozycje liczymy z grafiku wypełnianego cyklem. Włącz „Wypełniaj grafik z cyklu” " +
                "w ustawieniach „Mój cykl”, a wrócę z podpowiedziami.",
            fontSize = 11.sp, lineHeight = 16.5.sp, fontFamily = Jakarta, color = DarkTokens.inkMuted
        )
    }
}

@Composable
private fun BrakPropozycji(s: UiState) {
    KartaUstawien {
        Text("Nie ma czego proponować", style = GrafikType.cardTitle, color = DarkTokens.ink)
        Text(
            if (s.urlopBilans.zostalo <= 0)
                "Cały urlop już wykorzystany — w tym roku nie ma czym płacić za dłuższe wolne."
            else "W najbliższym roku nie widzę układu, w którym urlop dokłada się do wolnego z cyklu.",
            fontSize = 11.sp, lineHeight = 16.5.sp, fontFamily = Jakarta, color = DarkTokens.inkMuted
        )
    }
}

// ─────────────────────────────────────────────────────────────
// TEKSTY
// ─────────────────────────────────────────────────────────────

private fun zakres(p: VacationSuggestion): String {
    val od = p.wolneOd
    val doKiedy = p.wolneDo
    return if (od.month == doKiedy.month)
        "${od.dayOfMonth}–${doKiedy.dayOfMonth} ${miesiacPlaner(doKiedy)}"
    else "${od.dayOfMonth} ${miesiacPlaner(od)} – ${doKiedy.dayOfMonth} ${miesiacPlaner(doKiedy)}"
}

private fun dniUrlopu(p: VacationSuggestion): String = daty(p.urlop)

/**
 * W ruchu ciągłym święto bywa dniem pracy, więc trzeba je wykupić urlopem —
 * inaczej „święta w środku" obok tej samej daty w „urlop:" wyglądałoby jak błąd.
 */
private fun opisSwiat(p: VacationSuggestion): String {
    if (p.swieta.isEmpty()) return "bez świąt — wolne z samego cyklu"
    val zUrlopu = p.swieta.filter { it in p.urlop }
    val wolne = p.swieta.filterNot { it in p.urlop }
    return when {
        zUrlopu.isEmpty() -> "święta w środku: ${daty(wolne)}"
        wolne.isEmpty() -> "święta wypadają na zmianie — bierzesz je z urlopu: ${daty(zUrlopu)}"
        else -> "święta: ${daty(wolne)} · z urlopu: ${daty(zUrlopu)}"
    }
}

private fun daty(lista: List<LocalDate>): String =
    lista.joinToString(", ") { "${it.dayOfMonth}.${"%02d".format(it.monthValue)}" }

private fun miesiacPlaner(d: LocalDate): String =
    d.month.getDisplayName(JavaTextStyle.FULL, PL_PLAN)
