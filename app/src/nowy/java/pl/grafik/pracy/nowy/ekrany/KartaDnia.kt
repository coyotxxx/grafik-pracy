package pl.grafik.pracy.nowy.ekrany

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
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
import pl.grafik.pracy.ui.Tool
import pl.grafik.pracy.ui.UiState
import pl.grafik.pracy.ui.Vm
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import java.time.temporal.WeekFields
import java.util.Locale

private val PLL = Locale.forLanguageTag("pl-PL")
private val GODZINA = DateTimeFormatter.ofPattern("HH:mm", PLL)

/** Pięć segmentów wyboru z makiety — w tej kolejności. */
private val SEGMENTY = listOf(
    Tool.I to "I", Tool.II to "II", Tool.III to "III",
    Tool.URLOP to "Urlop", Tool.W5 to "Wolne"
)

private fun nazwaZmiany(s: Shift?): String = when (s) {
    Shift.I -> "Zmiana ranna"
    Shift.II -> "Zmiana popołudniowa"
    Shift.III -> "Zmiana nocna"
    Shift.URLOP -> "Urlop wypoczynkowy"
    null -> "Brak wpisu"
    else -> s.label
}

private fun godzinyZmiany(s: Shift?): String = when {
    s == null -> "dotknij, aby ustawić"
    s.isWork -> "${s.from} – ${s.to}"
    s == Shift.URLOP -> "cały dzień"
    else -> "dzień wolny w cyklu"
}

/**
 * Karta dnia — odtworzona z design/mockups/Day.html.
 * Arkusz dolny: promień 26 u góry, tło `bgElevated`, cień `0 -24px 60px rgba(0,0,0,0.55)`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KartaDnia(vm: Vm, dzien: LocalDate, naZamkniecie: () -> Unit) {
    val s by vm.state.collectAsState()
    val stan = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = naZamkniecie,
        sheetState = stan,
        containerColor = DarkTokens.bgElevated,
        contentColor = DarkTokens.ink,
        shape = RoundedCornerShape(topStart = Dim.rSheet, topEnd = Dim.rSheet),
        dragHandle = {
            Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(40.dp, 4.dp).clip(RoundedCornerShape(999.dp)).background(DarkTokens.lineSoft))
            }
        }
    ) {
        Column(
            Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp).padding(bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            NaglowekDnia(dzien, naZamkniecie)
            KartaTypuDnia(s, dzien, vm)
            WierszNadgodzin(s, dzien, vm)
            SekcjaObecnosci(s, dzien, vm)
            SekcjaWydarzen(s, dzien, vm)
            PrzyciskGlowny("Gotowe", akcja = naZamkniecie)
        }
    }
}

@Composable
private fun NaglowekDnia(dzien: LocalDate, naZamkniecie: () -> Unit) {
    val tydzien = dzien.get(WeekFields.ISO.weekOfWeekBasedYear())
    val dzis = remember { LocalDate.now() }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                dzien.dayOfWeek.getDisplayName(JavaTextStyle.FULL_STANDALONE, PLL)
                    .replaceFirstChar { it.uppercase() } + ", ${dzien.dayOfMonth} " +
                    dzien.month.getDisplayName(JavaTextStyle.FULL, PLL),
                style = TextStyle(
                    fontFamily = Bricolage, fontWeight = FontWeight.Bold,
                    fontSize = 21.sp, lineHeight = 23.sp, letterSpacing = (-0.42).sp
                ),
                color = DarkTokens.ink
            )
            Text(
                "tydzień $tydzien" + if (dzien == dzis) " · dziś" else "",
                style = GrafikType.caption, color = DarkTokens.inkMuted
            )
        }
        PrzyciskKwadrat(
            IkonaZamknij, "Zamknij",
            tlo = Color.White.copy(alpha = 0.05f), obrys = DarkTokens.lineStrong,
            akcja = naZamkniecie
        )
    }
}

@Composable
private fun KartaTypuDnia(s: UiState, dzien: LocalDate, vm: Vm) {
    val e = s.entries[dzien]
    val k = ShiftPaletteDark.of(typDniaZ(e?.shift))
    val godzin = (if (e?.shift?.isWork == true) 8 else 0) + (e?.otHours ?: 0)

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(k.fill).border(1.dp, k.line, RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(15.dp))
                    .background(Color(0xFF0E1113))
                    .border(1.dp, k.line, RoundedCornerShape(15.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    e?.shift?.code.orEmpty(),
                    style = TextStyle(
                        fontFamily = Bricolage, fontWeight = FontWeight.Bold,
                        fontSize = 18.sp, letterSpacing = 0.72.sp
                    ),
                    color = k.ink
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(nazwaZmiany(e?.shift), style = GrafikType.cardTitleStrong, color = DarkTokens.ink,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    godzinyZmiany(e?.shift),
                    style = TextStyle(fontFamily = Jakarta, fontSize = 12.sp, fontFeatureSettings = TNUM),
                    color = DarkTokens.ink3
                )
            }
            if (godzin > 0) Pigulka("$godzin h")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            SEGMENTY.forEach { (narzedzie, etykieta) ->
                val wybrany = e?.shift == shiftZNarzedzia(narzedzie)
                val kol = ShiftPaletteDark.of(typDniaZ(shiftZNarzedzia(narzedzie)))
                Box(
                    Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(12.dp))
                        .background(if (wybrany) kol.fill else DarkTokens.surfaceInput)
                        .border(1.dp, if (wybrany) kol.line else DarkTokens.lineInput, RoundedCornerShape(12.dp))
                        .clickable { vm.pick(narzedzie); vm.tap(dzien) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        etykieta, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        letterSpacing = 0.36.sp, fontFamily = Jakarta,
                        color = if (wybrany) kol.ink else DarkTokens.inkMuted,
                        maxLines = 1, textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun WierszNadgodzin(s: UiState, dzien: LocalDate, vm: Vm) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val zakres = rememberCoroutineScope()
    val e = s.entries[dzien]
    val ile = e?.otHours ?: 0
    val stawka = e?.otRate ?: OtRate.P100

    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Dim.rCardSmall))
            .background(DarkTokens.surface)
            .border(1.dp, DarkTokens.line, RoundedCornerShape(Dim.rCardSmall))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Nadgodziny", style = GrafikType.cardTitle, color = DarkTokens.ink)
            Text(
                if (ile == 0) "brak — dotknij plus, aby dodać" else "$ile h po ${stawka.percent} %",
                style = GrafikType.caption, color = DarkTokens.inkMuted
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            PrzyciskKwadrat(IkonaMinus, "Mniej nadgodzin") {
                zakres.launch { AkcjeDnia.nadgodziny(ctx, dzien, ile - 1, stawka) }
            }
            Text(
                "$ile",
                Modifier.widthIn(min = 36.dp),
                style = TextStyle(
                    fontFamily = Jakarta, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    fontFeatureSettings = TNUM
                ),
                color = DarkTokens.ink, textAlign = TextAlign.Center
            )
            PrzyciskKwadrat(IkonaPlus, "Więcej nadgodzin") {
                zakres.launch { AkcjeDnia.nadgodziny(ctx, dzien, ile + 1, stawka) }
            }
            val setka = stawka == OtRate.P100
            Box(
                Modifier.height(36.dp).widthIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
                    .background(if (setka) ShiftPaletteDark.I.fill else DarkTokens.surfaceInput)
                    .border(1.dp, if (setka) ShiftPaletteDark.I.line else DarkTokens.lineInput, RoundedCornerShape(12.dp))
                    .clickable {
                        zakres.launch {
                            AkcjeDnia.nadgodziny(ctx, dzien, ile, if (setka) OtRate.P50 else OtRate.P100)
                        }
                    }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${stawka.percent}%", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    fontFamily = Jakarta,
                    color = if (setka) ShiftPaletteDark.I.ink else DarkTokens.inkMuted
                )
            }
        }
    }
}

@Composable
private fun SekcjaObecnosci(s: UiState, dzien: LocalDate, vm: Vm) {
    val ob = s.obecnosc[dzien]
    val zmianaRobocza = s.entries[dzien]?.shift?.isWork == true

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("OBECNOŚĆ", style = GrafikType.sectionLabel, color = DarkTokens.inkFaint)
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(Dim.rCardSmall))
                .background(DarkTokens.surface)
                .border(1.dp, DarkTokens.line, RoundedCornerShape(Dim.rCardSmall))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(8.dp).clip(RoundedCornerShape(999.dp))
                    .background(if (ob != null) DarkTokens.ok else DarkTokens.lineSoft))
                Text(
                    when {
                        ob?.trwa == true -> "Jesteś w pracy"
                        ob?.od != null && ob.doKiedy != null ->
                            "Wykryto ${ob.od.format(GODZINA)} – ${ob.doKiedy.format(GODZINA)}"
                        ob != null -> "Zapisane ${ob.hours} h"
                        else -> "Nic nie wykryto"
                    },
                    Modifier.weight(1f), fontSize = 13.sp, fontFamily = Jakarta, color = Color(0xFFD7DBDE)
                )
                if (ob != null) {
                    Text(if (ob.reczne) "ręcznie" else "auto",
                        style = GrafikType.caption, color = DarkTokens.inkMuted)
                }
            }
            if (ob == null || ob.reczne) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(DarkTokens.line))
                Row(
                    Modifier.fillMaxWidth().clickable(enabled = zmianaRobocza) {
                        vm.oznaczObecnosc(dzien, ob == null)
                    },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Byłem w pracy", style = GrafikType.cardTitle, color = DarkTokens.ink)
                        Text(
                            if (zmianaRobocza) "godziny bierzemy ze zmiany" else "najpierw ustaw zmianę",
                            style = GrafikType.caption, color = DarkTokens.inkMuted
                        )
                    }
                    Przelacznik(ob != null) { if (zmianaRobocza) vm.oznaczObecnosc(dzien, it) }
                }
            }
        }
    }
}

@Composable
private fun SekcjaWydarzen(s: UiState, dzien: LocalDate, vm: Vm) {
    val wydarzenia = s.events[dzien].orEmpty()
    var dodaje by remember(dzien) { mutableStateOf(false) }
    var godzina by remember(dzien) { mutableStateOf("") }
    var nazwa by remember(dzien) { mutableStateOf("") }
    var przypomnij by remember(dzien) { mutableStateOf(true) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("WYDARZENIA", Modifier.weight(1f), style = GrafikType.sectionLabel, color = DarkTokens.inkFaint)
            Text("przypomnienia działają też przy zmianie nocnej",
                style = GrafikType.caption, color = DarkTokens.inkFaint,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        wydarzenia.forEach { ev ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(Dim.rCardSmall))
                    .background(DarkTokens.surface)
                    .border(1.dp, DarkTokens.line, RoundedCornerShape(Dim.rCardSmall))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    ev.time, Modifier.width(44.dp),
                    style = TextStyle(
                        fontFamily = Jakarta, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        fontFeatureSettings = TNUM
                    ),
                    color = ShiftPaletteDark.III.ink
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(ev.text, style = GrafikType.cardTitle, color = DarkTokens.ink,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (ev.remind) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Icon(IkonaDzwonek, null, Modifier.size(12.dp), tint = DarkTokens.inkMuted)
                            Text("przypomnienie dzień wcześniej, ${"%02d".format(s.remindHour)}:00",
                                style = GrafikType.caption, color = DarkTokens.inkMuted)
                        }
                    }
                }
                PrzyciskKwadrat(
                    IkonaKosz, "Usuń wydarzenie",
                    tlo = Color.Transparent, obrys = DarkTokens.lineStrong,
                    kolorIkony = Color(0xFF8A939B)
                ) { vm.deleteEvent(ev.id) }
            }
        }

        if (dodaje) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(Dim.rCardSmall))
                    .background(DarkTokens.accent.copy(alpha = 0.06f))
                    .border(1.dp, DarkTokens.accent.copy(alpha = 0.28f), RoundedCornerShape(Dim.rCardSmall))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.width(92.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("godzina", fontSize = 10.sp, fontFamily = Jakarta, color = DarkTokens.inkMuted)
                        PoleTekstowe(godzina, "9 20", cyfry = true) { godzina = it.filter(Char::isDigit).take(4) }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("co to za wydarzenie", fontSize = 10.sp, fontFamily = Jakarta, color = DarkTokens.inkMuted)
                        PoleTekstowe(nazwa, "np. badania okresowe") { nazwa = it.take(80) }
                    }
                }
                Row(
                    Modifier.fillMaxWidth().clickable { przypomnij = !przypomnij },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Checkbox(
                        przypomnij, { przypomnij = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = DarkTokens.accent,
                            checkmarkColor = DarkTokens.accentOn,
                            uncheckedColor = DarkTokens.lineSoft
                        )
                    )
                    Text("Przypomnij dzień wcześniej o ${"%02d".format(s.remindHour)}:00",
                        fontSize = 13.sp, fontFamily = Jakarta, color = DarkTokens.ink)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrzyciskDrugorzedny("Anuluj", Modifier.weight(1f)) { dodaje = false }
                    Box(
                        Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(13.dp))
                            .background(DarkTokens.accent)
                            .clickable {
                                val g = godzina.padStart(4, '0')
                                val czas = "${g.take(2).toInt()}:${g.takeLast(2)}"
                                if (nazwa.isNotBlank()) {
                                    vm.addEvent(dzien, czas, nazwa.trim(), przypomnij)
                                    dodaje = false; godzina = ""; nazwa = ""
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Dodaj", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            fontFamily = Jakarta, color = DarkTokens.accentOn)
                    }
                }
            }
        } else {
            Row(
                // Makieta: „border: 1px dashed #2A3036" — Compose nie ma obrysu
                // przerywanego w Modifier.border, więc rysujemy go sami.
                Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(15.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .drawBehind {
                        drawRoundRect(
                            color = DarkTokens.lineSoft,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 1.dp.toPx(),
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                    floatArrayOf(6.dp.toPx(), 5.dp.toPx())
                                )
                            ),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(15.dp.toPx())
                        )
                    }
                    .clickable { dodaje = true },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                Icon(IkonaPlusCienki, null, Modifier.size(16.dp), tint = DarkTokens.ink2)
                Text("Dodaj wydarzenie", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    fontFamily = Jakarta, color = DarkTokens.ink2)
            }
        }
    }
}
