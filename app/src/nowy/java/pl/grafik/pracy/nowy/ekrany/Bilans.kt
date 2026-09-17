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
import pl.grafik.pracy.domain.Shift
import pl.grafik.pracy.nowy.theme.*
import pl.grafik.pracy.nowy.ui.*
import pl.grafik.pracy.ui.UiState
import pl.grafik.pracy.ui.Vm
import pl.grafik.pracy.ui.screens.okresLabel
import java.time.YearMonth
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

private val PL_BILANS = Locale.forLanguageTag("pl-PL")

/** Liczba z `tnum` — wszystkie liczby na tym ekranie mają stałą szerokość cyfr. */
private fun liczba(rozmiar: Int, waga: FontWeight = FontWeight.Bold) = TextStyle(
    fontFamily = Jakarta, fontWeight = waga,
    fontSize = rozmiar.sp, fontFeatureSettings = TNUM
)

/**
 * Ekran „Bilans" — odtworzony z design/mockups/Summary.html.
 *
 * Liczby pochodzą z tego samego `MonthStats` i `PeriodStats`, które liczy klasyczna
 * aplikacja; ten ekran tylko inaczej je układa.
 */
@Composable
fun EkranBilans(vm: Vm) {
    val s by vm.state.collectAsState()

    // Wejście w bilans zawsze zaczyna od bieżącego miesiąca — jak w klasycznej aplikacji.
    LaunchedEffect(Unit) { vm.onEnterSummary() }
    var wyborMiesiaca by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        TloZPoswiata(Modifier.fillMaxSize())

        Column(
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = Dim.topSafe, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Naglowek(s, vm, wyborMiesiaca) { wyborMiesiaca = !wyborMiesiaca }
            if (wyborMiesiaca) {
                WyborMiesiaca(s.ym) { vm.setMonth(it); wyborMiesiaca = false }
            }
            KartaMiesiaca(s)
            KartaOkresu(s)
            KafelkiNadgodzin(s)
            KartaUrlopu(s)
            KartaRozkladu(s)
        }
    }
}

// ─────────────────────────────────────────────────────────────
// NAGŁÓWEK I WYBÓR MIESIĄCA
// ─────────────────────────────────────────────────────────────

@Composable
private fun Naglowek(s: UiState, vm: Vm, otwarty: Boolean, naPrzelaczenie: () -> Unit) {
    val p by postepWejscia(Motion.RISE_MS)
    Row(
        Modifier.wejscie(p).padding(horizontal = Dim.screenGutter).fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Bilans", style = GrafikType.h1, color = DarkTokens.ink, modifier = Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            PrzyciskMiesiaca(IkonaWLewo, "Poprzedni miesiąc") { vm.prevMonth() }
            Row(
                Modifier.height(38.dp).clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(1.dp, DarkTokens.lineStrong, RoundedCornerShape(12.dp))
                    .clickable(onClick = naPrzelaczenie)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    miesiacRok(s.ym), fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    fontFamily = Jakarta, color = DarkTokens.inkStrong, maxLines = 1
                )
                Icon(
                    if (otwarty) IkonaWGore else IkonaWDol, "Wybierz miesiąc",
                    Modifier.size(13.dp), tint = DarkTokens.inkStrong
                )
            }
            PrzyciskMiesiaca(IkonaWPrawo, "Następny miesiąc") { vm.nextMonth() }
        }
    }
}

@Composable
private fun PrzyciskMiesiaca(
    ikona: androidx.compose.ui.graphics.vector.ImageVector,
    opis: String,
    akcja: () -> Unit
) {
    Box(
        Modifier.size(38.dp).clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, DarkTokens.lineStrong, RoundedCornerShape(12.dp))
            .clickable(onClick = akcja),
        contentAlignment = Alignment.Center
    ) { Icon(ikona, opis, Modifier.size(16.dp), tint = DarkTokens.ink2) }
}

/**
 * Siatka miesięcy — makieta pokazuje sam przycisk z nazwą, nie rozwinięcie.
 * Układ jest ten sam, co w klasycznej aplikacji: strzałki roku i 12 kafelków.
 */
@Composable
private fun WyborMiesiaca(biezacy: YearMonth, naWybor: (YearMonth) -> Unit) {
    val teraz = remember { YearMonth.now() }
    var rok by remember(biezacy) { mutableIntStateOf(biezacy.year) }

    Karta(
        Modifier.padding(horizontal = Dim.screenGutter),
        promien = Dim.rCard, paddingPion = 14.dp, paddingBok = 14.dp
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            PrzyciskKwadrat(IkonaWLewo, "Poprzedni rok", rozmiar = 34.dp) { rok-- }
            Text(
                "$rok", Modifier.weight(1f), style = liczba(17), color = DarkTokens.ink,
                textAlign = TextAlign.Center
            )
            PrzyciskKwadrat(IkonaWPrawo, "Następny rok", rozmiar = 34.dp) { rok++ }
        }
        Spacer(Modifier.height(10.dp))
        (0..3).forEach { wiersz ->
            Row(
                Modifier.fillMaxWidth().padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                (1..3).forEach { kol ->
                    val m = YearMonth.of(rok, wiersz * 3 + kol)
                    val wybrany = m == biezacy
                    Box(
                        Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(11.dp))
                            .background(if (wybrany) DarkTokens.accentTintBg else DarkTokens.surfaceInput)
                            .border(
                                1.dp,
                                when {
                                    wybrany -> DarkTokens.accentTintLine
                                    m == teraz -> DarkTokens.lineSoft
                                    else -> DarkTokens.lineInput
                                },
                                RoundedCornerShape(11.dp)
                            )
                            .clickable { naWybor(m) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            m.month.getDisplayName(JavaTextStyle.SHORT_STANDALONE, PL_BILANS),
                            fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = Jakarta,
                            color = if (wybrany) DarkTokens.accent else DarkTokens.ink2
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// KARTA MIESIĄCA
// ─────────────────────────────────────────────────────────────

@Composable
private fun KartaMiesiaca(s: UiState) {
    val p by postepWejscia(Motion.RISE_MS, 60)
    val doDzis = if (s.stats.biezacyMiesiac) s.stats.doDzis else s.stats.rozliczone
    val licznik = licznikDo(doDzis)
    val bilans = s.stats.diff

    Karta(
        Modifier.wejscie(p).padding(horizontal = Dim.screenGutter),
        tlo = Color(0x0AFFFFFF), promien = Dim.rCard, paddingPion = 18.dp, paddingBok = 16.dp
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    if (s.stats.biezacyMiesiac) "PRZEPRACOWANE DO DZIŚ" else "PRZEPRACOWANE",
                    style = GrafikType.sectionLabel, color = DarkTokens.inkMuted
                )
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("$licznik", style = GrafikType.heroNumber, color = DarkTokens.ink,
                        modifier = Modifier.alignByBaseline())
                    Text("h", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        fontFamily = Jakarta, color = DarkTokens.inkMuted,
                        modifier = Modifier.alignByBaseline())
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PigulkaBilansu(bilans)
                Text("norma ${s.stats.norm} h", fontSize = 11.sp,
                    fontFamily = Jakarta, color = DarkTokens.inkMuted)
            }
        }
        Spacer(Modifier.height(14.dp))
        PasekPostepu(
            if (s.stats.norm > 0) doDzis.toFloat() / s.stats.norm else 0f,
            wysokosc = 8.dp
        )
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(Modifier.weight(1f)) {
                Text("cały miesiąc wyjdzie ", fontSize = 11.sp,
                    fontFamily = Jakarta, color = DarkTokens.inkMuted)
                Text("${s.stats.rozliczone} h", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    fontFamily = Jakarta, color = DarkTokens.inkStrong)
            }
            Text(
                "${s.stats.worked} h pracy + ${s.stats.urlopH} h urlopu",
                fontSize = 11.sp, fontFamily = Jakarta, color = DarkTokens.inkMuted,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PigulkaBilansu(bilans: Int) {
    val naPlus = bilans >= 0
    val kolor = if (naPlus) DarkTokens.accent else DarkTokens.warnInk
    Box(
        Modifier.height(26.dp).clip(RoundedCornerShape(999.dp))
            .background(kolor.copy(alpha = 0.14f))
            .border(1.dp, kolor.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "bilans ${if (bilans > 0) "+" else ""}$bilans h",
            style = liczba(11, FontWeight.SemiBold), color = kolor
        )
    }
}

// ─────────────────────────────────────────────────────────────
// OKRES ROZLICZENIOWY
// ─────────────────────────────────────────────────────────────

@Composable
private fun KartaOkresu(s: UiState) {
    val p by postepWejscia(Motion.RISE_MS, 120)
    val okres = s.okresy.firstOrNull { it.biezacy } ?: s.okresy.firstOrNull() ?: return
    var wszystkie by remember { mutableStateOf(false) }

    Karta(
        Modifier.wejscie(p).padding(horizontal = Dim.screenGutter),
        promien = Dim.rCard, paddingPion = 16.dp, paddingBok = 16.dp
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text("Okres rozliczeniowy", style = GrafikType.cardTitle,
                color = DarkTokens.ink, modifier = Modifier.weight(1f))
            Text(okresLabel(okres.period), fontSize = 11.sp,
                fontFamily = Jakarta, color = DarkTokens.inkMuted)
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text("nadgodziny w okresie", fontSize = 12.sp,
                fontFamily = Jakarta, color = DarkTokens.ink3, modifier = Modifier.weight(1f))
            Row {
                Text("${okres.ot} ", style = liczba(15), color = DarkTokens.ink,
                    modifier = Modifier.alignByBaseline())
                Text("/ ${okres.limit} h", style = liczba(12, FontWeight.Medium),
                    color = DarkTokens.inkMuted, modifier = Modifier.alignByBaseline())
            }
        }
        Spacer(Modifier.height(12.dp))
        PasekPostepu(
            if (okres.limit > 0) okres.ot.toFloat() / okres.limit else 0f,
            kolor = ShiftPaletteDark.I.ink
        )
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(13.dp))
                .background(Color.White.copy(alpha = 0.04f))
                .border(1.dp, DarkTokens.lineStrong, RoundedCornerShape(13.dp))
                .clickable { wszystkie = !wszystkie }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Wszystkie okresy w ${s.ym.year} · razem ${s.otRok} / ${s.otLimitRok} h",
                fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = Jakarta,
                color = DarkTokens.ink2, modifier = Modifier.weight(1f),
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Icon(
                if (wszystkie) IkonaWGore else IkonaWPrawo, null,
                Modifier.size(15.dp), tint = DarkTokens.ink2
            )
        }
        if (wszystkie) {
            Spacer(Modifier.height(12.dp))
            s.okresy.forEach { o ->
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        okresLabel(o.period), fontSize = 11.sp, fontFamily = Jakarta,
                        color = if (o.biezacy) DarkTokens.inkStrong else DarkTokens.inkMuted,
                        modifier = Modifier.width(86.dp), maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    PasekPostepu(
                        if (o.limit > 0) o.ot.toFloat() / o.limit else 0f,
                        Modifier.weight(1f), wysokosc = 6.dp, kolor = ShiftPaletteDark.I.ink
                    )
                    Text(
                        "${o.ot} / ${o.limit} h", style = liczba(11, FontWeight.SemiBold),
                        color = DarkTokens.ink2, modifier = Modifier.width(58.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// KAFELKI NADGODZIN
// ─────────────────────────────────────────────────────────────

@Composable
private fun KafelkiNadgodzin(s: UiState) {
    val p by postepWejscia(Motion.RISE_MS, 160)
    val zolty = ShiftPaletteDark.I.ink
    Row(
        Modifier.wejscie(p).padding(horizontal = Dim.screenGutter).fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        KafelekStatystyki(
            Modifier.weight(2f), "${s.stats.ot100} h", "nadgodziny 100%",
            zolty, Color(0x14DAC559), Color(0x38DAC559)
        )
        KafelekStatystyki(
            Modifier.weight(2f), "${s.stats.ot50} h", "nadgodziny 50%",
            zolty, Color(0x0DDAC559), Color(0x29DAC559)
        )
        KafelekStatystyki(
            Modifier.weight(1f), "${s.stats.sundayWork}", "prac. nd.",
            DarkTokens.ink, DarkTokens.surface, DarkTokens.line, wasko = true
        )
    }
}

@Composable
private fun KafelekStatystyki(
    modifier: Modifier,
    wartosc: String,
    podpis: String,
    kolorLiczby: Color,
    tlo: Color,
    obrys: Color,
    wasko: Boolean = false
) {
    Column(
        modifier.clip(RoundedCornerShape(18.dp))
            .background(tlo)
            .border(1.dp, obrys, RoundedCornerShape(18.dp))
            .padding(horizontal = if (wasko) 10.dp else 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(wartosc, style = liczba(19), color = kolorLiczby)
        Text(podpis, fontSize = 10.sp, fontFamily = Jakarta, color = DarkTokens.inkMuted,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ─────────────────────────────────────────────────────────────
// URLOP
// ─────────────────────────────────────────────────────────────

@Composable
private fun KartaUrlopu(s: UiState) {
    val p by postepWejscia(Motion.RISE_MS, 200)
    val u = s.urlopBilans
    val rozowy = ShiftPaletteDark.URLOP.ink

    Karta(
        Modifier.wejscie(p).padding(horizontal = Dim.screenGutter),
        tlo = Color(0x0FF490D9), obrys = Color(0x33F490D9),
        promien = Dim.rCard, paddingPion = 16.dp, paddingBok = 16.dp
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("URLOP ${u.rok}", style = GrafikType.sectionLabel, color = DarkTokens.inkMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("${u.zostalo}", style = GrafikType.heroNumber.copy(fontSize = 34.sp,
                        lineHeight = 31.sp), color = rozowy, modifier = Modifier.alignByBaseline())
                    Text("dni zostało", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        fontFamily = Jakarta, color = Color(0xFFD7DBDE),
                        modifier = Modifier.alignByBaseline())
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("wykorzystane", fontSize = 11.sp, fontFamily = Jakarta, color = DarkTokens.inkMuted)
                Text("${u.zuzyte} / ${u.bazaZalegly + u.bazaBiezacy}",
                    style = liczba(14), color = DarkTokens.ink)
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KafelekUrlopu(Modifier.weight(1f), "zaległy z ${u.rok - 1}",
                u.zostaloZaleglego, u.bazaZalegly)
            KafelekUrlopu(Modifier.weight(1f), "za ${u.rok}",
                u.zostaloBiezacego, u.bazaBiezacy)
        }
        if (s.urlopMiesiaca.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(Modifier.size(6.dp).clip(RoundedCornerShape(999.dp)).background(rozowy))
                Text(
                    "w tym miesiącu: ${dniUrlopu(s)}",
                    fontSize = 11.sp, fontFamily = Jakarta, color = DarkTokens.inkMuted,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun KafelekUrlopu(modifier: Modifier, podpis: String, zostalo: Int, baza: Int) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp))
            .background(Color(0x40000000))
            .border(1.dp, DarkTokens.lineStrong, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(podpis, fontSize = 10.sp, fontFamily = Jakarta, color = DarkTokens.inkMuted)
        Row {
            Text("$zostalo ", style = liczba(14), color = DarkTokens.ink,
                modifier = Modifier.alignByBaseline())
            Text("z $baza", style = liczba(10, FontWeight.Medium), color = DarkTokens.inkMuted,
                modifier = Modifier.alignByBaseline())
        }
    }
}

/** „11 i 14 września" albo „2, 5 i 9 października" — dni urlopu w wyświetlanym miesiącu. */
private fun dniUrlopu(s: UiState): String {
    val dni = s.urlopMiesiaca.map { it.dayOfMonth }.sorted()
    val miesiac = s.ym.month.getDisplayName(JavaTextStyle.FULL, PL_BILANS)
    val lista = when (dni.size) {
        1 -> "${dni[0]}"
        else -> dni.dropLast(1).joinToString(", ") + " i " + dni.last()
    }
    return "$lista $miesiac"
}

// ─────────────────────────────────────────────────────────────
// ROZKŁAD ZMIAN
// ─────────────────────────────────────────────────────────────

@Composable
private fun KartaRozkladu(s: UiState) {
    val p by postepWejscia(Motion.RISE_MS, 240)
    val maks = listOf(Shift.I, Shift.II, Shift.III)
        .maxOf { s.stats.daysByShift[it] ?: 0 }
        .coerceAtLeast(1)

    Karta(
        Modifier.wejscie(p).padding(horizontal = Dim.screenGutter),
        promien = Dim.rCard, paddingPion = 16.dp, paddingBok = 16.dp
    ) {
        Text("Rozkład zmian", style = GrafikType.cardTitle, color = DarkTokens.ink)
        Spacer(Modifier.height(14.dp))

        listOf(Shift.I to ShiftPaletteDark.I, Shift.II to ShiftPaletteDark.II,
            Shift.III to ShiftPaletteDark.III).forEach { (zm, kolory) ->
            val dni = s.stats.daysByShift[zm] ?: 0
            val godziny = s.stats.byShift[zm] ?: 0
            Row(
                Modifier.fillMaxWidth().padding(bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(zm.code, Modifier.width(22.dp), style = liczba(12, FontWeight.Bold),
                    color = kolory.ink)
                PasekPostepu(dni.toFloat() / maks, Modifier.weight(1f),
                    wysokosc = 10.dp, kolor = kolory.ink)
                Column(Modifier.width(62.dp), horizontalAlignment = Alignment.End) {
                    Text("$dni dni", style = liczba(12, FontWeight.SemiBold), color = DarkTokens.ink)
                    Text("$godziny h", style = liczba(10, FontWeight.Medium), color = DarkTokens.inkMuted)
                }
            }
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(DarkTokens.line))
        Spacer(Modifier.height(14.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PodsumowanieDni(Modifier.weight(1f), "${s.stats.daysWork}", "dni pracy", DarkTokens.ink)
            PodsumowanieDni(Modifier.weight(1f), "${s.stats.daysFree}", "dni wolnych", DarkTokens.ink)
            PodsumowanieDni(Modifier.weight(1f), "${s.urlopMiesiaca.size}", "urlopu",
                ShiftPaletteDark.URLOP.ink)
        }
    }
}

@Composable
private fun PodsumowanieDni(modifier: Modifier, wartosc: String, podpis: String, kolor: Color) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(wartosc, style = liczba(18), color = kolor, modifier = Modifier.alignByBaseline())
        Text(podpis, fontSize = 11.sp, fontFamily = Jakarta, color = DarkTokens.inkMuted,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.alignByBaseline())
    }
}

private fun miesiacRok(ym: YearMonth): String =
    ym.month.getDisplayName(JavaTextStyle.FULL_STANDALONE, PL_BILANS)
        .replaceFirstChar { it.uppercase(PL_BILANS) } + " " + ym.year
