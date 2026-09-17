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
import pl.grafik.pracy.domain.Settlement
import pl.grafik.pracy.nowy.theme.*
import pl.grafik.pracy.nowy.ui.*
import pl.grafik.pracy.ui.UiState
import pl.grafik.pracy.ui.Vm
import pl.grafik.pracy.ui.screens.okresLabel

/** Długości okresu rozliczeniowego dopuszczone przez `Settlement.DLUGOSCI`. */
private val NAZWY_OKRESU = mapOf(
    1 to "miesiąc", 3 to "kwartał", 4 to "4 miesiące", 6 to "pół roku", 12 to "rok"
)

/**
 * „Czas pracy i nadgodziny" — odtworzone z design/mockups/Worktime.html.
 *
 * Dwa świadome odstępstwa od makiety, zgodnie z decyzją Macieja zapisaną w CLAUDE.md:
 * nie ma przełącznika „Norma z kalendarza" (norma jest zawsze ustawowa, art. 130 KP)
 * ani stawki „odbiór" — zostają 50 % i 100 %.
 */
@Composable
fun EkranCzasPracy(vm: Vm, naPowrot: () -> Unit) {
    val s by vm.state.collectAsState()

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
                Text("Czas pracy i nadgodziny",
                    style = GrafikType.h1.copy(fontSize = 26.sp, lineHeight = 30.sp),
                    color = DarkTokens.ink)
                Text("Tak liczymy Twoją normę i limity. Ustaw zgodnie z tym, co robi kadrowa.",
                    fontSize = 12.sp, fontFamily = Jakarta, color = DarkTokens.inkMuted)
            }

            KartaOkresuRozliczeniowego(s, vm)
            KartaStawki(s, vm)
            KartaLimitow(s, vm)
            KartaPodpowiedzi()
        }
    }
}

// ─────────────────────────────────────────────────────────────
// OKRES ROZLICZENIOWY
// ─────────────────────────────────────────────────────────────

@Composable
private fun KartaOkresuRozliczeniowego(s: UiState, vm: Vm) {
    val p by postepWejscia(Motion.RISE_MS, 50)
    val biezacy = s.okresy.firstOrNull { it.biezacy } ?: s.okresy.firstOrNull()

    KartaUstawien(Modifier.wejscie(p)) {
        Text("Okres rozliczeniowy", style = GrafikType.cardTitle, color = DarkTokens.ink)
        Text(
            "Zakład rozlicza godziny w całym okresie, nie w pojedynczym miesiącu — " +
                "niedobór z jednego miesiąca odrabiasz w kolejnym.",
            fontSize = 11.sp, lineHeight = 16.5.sp, fontFamily = Jakarta, color = DarkTokens.inkMuted
        )

        // Pięć długości w siatce po trzy w rzędzie — jak w makiecie.
        Settlement.DLUGOSCI.chunked(3).forEach { rzad ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                rzad.forEach { ile ->
                    val wybrany = s.okres.months == ile
                    Box(
                        Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(13.dp))
                            .background(if (wybrany) Color(0x2452D0B3) else DarkTokens.surface)
                            .border(
                                1.dp,
                                if (wybrany) Color(0x7352D0B3) else DarkTokens.line,
                                RoundedCornerShape(13.dp)
                            )
                            .clickable { vm.saveSettlement(s.okres.copy(months = ile)) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            NAZWY_OKRESU[ile] ?: "$ile mies.",
                            fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = Jakarta,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            color = if (wybrany) DarkTokens.accent else DarkTokens.inkMuted
                        )
                    }
                }
                // Ostatni rząd bywa krótszy — dopełniamy, żeby kafelki nie rosły.
                repeat(3 - rzad.size) { Spacer(Modifier.weight(1f)) }
            }
        }

        if (biezacy != null) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp))
                    .background(Color(0x1452D0B3))
                    .border(1.dp, Color(0x3D52D0B3), RoundedCornerShape(13.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(Modifier.size(7.dp).clip(RoundedCornerShape(999.dp)).background(DarkTokens.accent))
                Text("Bieżący okres: ${okresLabel(biezacy.period)}",
                    fontSize = 12.sp, fontFamily = Jakarta, color = Color(0xFF9FE3D2))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// DOMYŚLNA STAWKA
// ─────────────────────────────────────────────────────────────

@Composable
private fun KartaStawki(s: UiState, vm: Vm) {
    val p by postepWejscia(Motion.RISE_MS, 90)
    KartaUstawien(Modifier.wejscie(p)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Domyślna stawka nadgodzin", style = GrafikType.cardTitle, color = DarkTokens.ink)
                Text("podpowiadana przy malowaniu i w dniu",
                    fontSize = 11.sp, fontFamily = Jakarta, color = DarkTokens.inkMuted)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                OtRate.entries.forEach { stawka ->
                    val wybrana = s.otRate == stawka
                    Box(
                        Modifier.height(38.dp).clip(RoundedCornerShape(12.dp))
                            .background(if (wybrana) Color(0x2452D0B3) else DarkTokens.surfaceInput)
                            .border(
                                1.dp,
                                if (wybrana) Color(0x7352D0B3) else DarkTokens.lineInput,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { if (!wybrana) vm.toggleRate() }
                            .padding(horizontal = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${stawka.percent}%", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            fontFamily = Jakarta,
                            color = if (wybrana) DarkTokens.accent else DarkTokens.inkMuted)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// LIMITY NADGODZIN
// ─────────────────────────────────────────────────────────────

@Composable
private fun KartaLimitow(s: UiState, vm: Vm) {
    val p by postepWejscia(Motion.RISE_MS, 130)
    val rok = s.ym.year
    val sufit = remember(rok) { Settlement.yearCeiling(rok) }

    KartaUstawien(Modifier.wejscie(p)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text("Limit nadgodzin", style = GrafikType.cardTitle,
                color = DarkTokens.ink, modifier = Modifier.weight(1f))
            Text("art. 131 KP", fontSize = 10.sp, fontFamily = Jakarta, color = DarkTokens.inkFaint)
        }
        Text(
            "Ustawowy to 8 h na każdy pełny tydzień okresu. Jeśli Twój zakład ma własny limit, " +
                "wpisz go obok — wtedy obowiązuje zamiast ustawowego.",
            fontSize = 11.sp, lineHeight = 16.5.sp, fontFamily = Jakarta, color = DarkTokens.inkMuted
        )

        Row(
            Modifier.fillMaxWidth().padding(bottom = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Spacer(Modifier.weight(1f))
            Text("USTAWOWO", Modifier.width(64.dp), fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold, fontFamily = Jakarta, letterSpacing = 0.4.sp,
                color = DarkTokens.inkFaint, textAlign = TextAlign.End)
            Text("ZAKŁAD", Modifier.width(74.dp), fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold, fontFamily = Jakarta, letterSpacing = 0.4.sp,
                color = DarkTokens.inkFaint, textAlign = TextAlign.End)
        }

        s.okresy.forEach { okres ->
            WierszLimitu(s, vm, okres)
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(DarkTokens.line))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("W roku razem", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    fontFamily = Jakarta, color = DarkTokens.ink)
                Text(
                    "suma limitów okresów · art. 131 KP dopuszcza w $rok najwyżej $sufit h",
                    fontSize = 10.sp, lineHeight = 14.sp, fontFamily = Jakarta,
                    color = DarkTokens.inkFaint
                )
            }
            Text(
                "${s.otLimitRok} h",
                style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    fontFamily = Jakarta, fontFeatureSettings = TNUM),
                color = if (s.otLimitRok > sufit) DarkTokens.warnInk else ShiftPaletteDark.I.ink
            )
        }
    }
}

@Composable
private fun WierszLimitu(s: UiState, vm: Vm, okres: pl.grafik.pracy.domain.PeriodStats) {
    val klucz = remember(okres.period) { Settlement.key(okres.period) }
    // Klucz remembera to sam okres, nigdy zapisywana wartość — opóźniona emisja
    // z DataStore przestawiałaby cyfry w trakcie pisania.
    var wpis by remember(klucz) {
        mutableStateOf(s.okres.otLimitPeriods[klucz]?.toString() ?: "")
    }

    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                okresLabel(okres.period), fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                fontFamily = Jakarta, maxLines = 1, overflow = TextOverflow.Ellipsis,
                color = if (okres.biezacy) DarkTokens.ink else DarkTokens.ink3
            )
            Text("${okres.period.weeks} tygodni", fontSize = 10.sp,
                fontFamily = Jakarta, color = DarkTokens.inkFaint)
        }
        Text(
            "${okres.otLimit} h", Modifier.width(64.dp),
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                fontFamily = Jakarta, fontFeatureSettings = TNUM),
            color = DarkTokens.ink3, textAlign = TextAlign.End
        )
        PoleTekstowe(
            wartosc = wpis,
            podpowiedz = "—",
            cyfry = true,
            modifier = Modifier.width(74.dp),
            wyrownanie = TextAlign.End,
            naZmiane = { v ->
                val czyste = v.filter(Char::isDigit).take(3)
                wpis = czyste
                val mapa = s.okres.otLimitPeriods.toMutableMap()
                val h = czyste.toIntOrNull()
                if (h != null && h > 0) mapa[klucz] = h else mapa.remove(klucz)
                vm.saveSettlement(s.okres.copy(otLimitPeriods = mapa))
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────
// PODPOWIEDŹ
// ─────────────────────────────────────────────────────────────

@Composable
private fun KartaPodpowiedzi() {
    val p by postepWejscia(Motion.RISE_MS, 170)
    Row(
        Modifier.wejscie(p).fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x127ABDFF))
            .border(1.dp, Color(0x387ABDFF), RoundedCornerShape(20.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(11.dp))
                .background(Color(0x297ABDFF)),
            contentAlignment = Alignment.Center
        ) { Icon(IkonaInfo, null, Modifier.size(17.dp), tint = ShiftPaletteDark.III.ink) }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("Puste pole = limit ustawowy", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                fontFamily = Jakarta, color = DarkTokens.ink)
            Text(
                "Bilans i ostrzeżenia w zakładce Bilans liczą się zawsze z tego, co masz ustawione tutaj.",
                fontSize = 11.sp, lineHeight = 16.5.sp, fontFamily = Jakarta, color = DarkTokens.ink3
            )
        }
    }
}

/** Karta ustawień z makiet podstron: r20, tło `surface`, obrys `line`, padding 14. */
@Composable
internal fun KartaUstawien(modifier: Modifier = Modifier, tresc: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DarkTokens.surface)
            .border(1.dp, DarkTokens.line, RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = tresc
    )
}
