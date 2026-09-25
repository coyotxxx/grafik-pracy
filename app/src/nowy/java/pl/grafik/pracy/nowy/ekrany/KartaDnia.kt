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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.runtime.produceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import pl.grafik.pracy.data.ZdjeciaNotatek
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.grafik.pracy.domain.Holidays
import pl.grafik.pracy.domain.OtRate
import pl.grafik.pracy.domain.Shift
import pl.grafik.pracy.nowy.theme.*
import pl.grafik.pracy.nowy.ui.*
import pl.grafik.pracy.ui.Tool
import pl.grafik.pracy.ui.UiState
import pl.grafik.pracy.ui.Vm
import java.time.LocalDate
import java.time.LocalDateTime
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
    val ctx = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = naZamkniecie,
        sheetState = stan,
        containerColor = Tokeny.bgElevated,
        contentColor = Tokeny.ink,
        shape = RoundedCornerShape(topStart = Dim.rSheet, topEnd = Dim.rSheet),
        dragHandle = {
            Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(40.dp, 4.dp).clip(RoundedCornerShape(999.dp)).background(Tokeny.lineSoft))
            }
        }
    ) {
        Column(
            Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp).padding(bottom = 22.dp + dolnaKrawedz(minimum = 0.dp)),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            NaglowekDnia(dzien, naZamkniecie)
            KartaTypuDnia(s, dzien, vm)
            WierszNadgodzin(s, dzien, vm)
            SekcjaObecnosci(s, dzien, vm)
            SekcjaWydarzen(s, dzien, vm)
            SekcjaNotatki(s, dzien, vm)
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
                color = Tokeny.ink
            )
            Text(
                "tydzień $tydzien" + if (dzien == dzis) " · dziś" else "",
                style = GrafikType.caption, color = Tokeny.inkMuted
            )
            Holidays.nameOf(dzien)?.let {
                Text(
                    "$it · święto ustawowo wolne",
                    style = GrafikType.caption, color = Tokeny.warnInk
                )
            }
        }
        PrzyciskKwadrat(
            IkonaZamknij, "Zamknij",
            tlo = Tokeny.surface, obrys = Tokeny.lineStrong,
            akcja = naZamkniecie
        )
    }
}

@Composable
private fun KartaTypuDnia(s: UiState, dzien: LocalDate, vm: Vm) {
    val e = s.entries[dzien]
    val k = Paleta.of(typDniaZ(e?.shift))
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
                Text(nazwaZmiany(e?.shift), style = GrafikType.cardTitleStrong, color = Tokeny.ink,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    godzinyZmiany(e?.shift),
                    style = TextStyle(fontFamily = Jakarta, fontSize = 12.sp, fontFeatureSettings = TNUM),
                    color = Tokeny.ink3
                )
            }
            if (godzin > 0) Pigulka("$godzin h")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            SEGMENTY.forEach { (narzedzie, etykieta) ->
                val wybrany = e?.shift == shiftZNarzedzia(narzedzie)
                val kol = Paleta.of(typDniaZ(shiftZNarzedzia(narzedzie)))
                Box(
                    Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(12.dp))
                        .background(if (wybrany) kol.fill else Tokeny.surfaceInput)
                        .border(1.dp, if (wybrany) kol.line else Tokeny.lineInput, RoundedCornerShape(12.dp))
                        .clickable { vm.setShift(dzien, shiftZNarzedzia(narzedzie)) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        etykieta, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        letterSpacing = 0.36.sp, fontFamily = Jakarta,
                        color = if (wybrany) kol.ink else Tokeny.inkMuted,
                        maxLines = 1, textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun WierszNadgodzin(s: UiState, dzien: LocalDate, vm: Vm) {
    val e = s.entries[dzien]
    val ile = e?.otHours ?: 0
    val stawka = e?.otRate ?: OtRate.P100

    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Dim.rCardSmall))
            .background(Tokeny.surface)
            .border(1.dp, Tokeny.line, RoundedCornerShape(Dim.rCardSmall))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Nadgodziny", style = GrafikType.cardTitle, color = Tokeny.ink)
            Text(
                if (ile == 0) "brak — dotknij plus, aby dodać" else "$ile h po ${stawka.percent} %",
                style = GrafikType.caption, color = Tokeny.inkMuted
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            PrzyciskKwadrat(IkonaMinus, "Mniej nadgodzin") {
                vm.setOvertime(dzien, ile - 1, stawka)
            }
            Text(
                "$ile",
                Modifier.widthIn(min = 36.dp),
                style = TextStyle(
                    fontFamily = Jakarta, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    fontFeatureSettings = TNUM
                ),
                color = Tokeny.ink, textAlign = TextAlign.Center
            )
            PrzyciskKwadrat(IkonaPlus, "Więcej nadgodzin") {
                vm.setOvertime(dzien, ile + 1, stawka)
            }
            val setka = stawka == OtRate.P100
            Box(
                Modifier.height(36.dp).widthIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
                    .background(if (setka) Paleta.I.fill else Tokeny.surfaceInput)
                    .border(1.dp, if (setka) Paleta.I.line else Tokeny.lineInput, RoundedCornerShape(12.dp))
                    .clickable {
                        vm.setOvertime(dzien, ile, if (setka) OtRate.P50 else OtRate.P100)
                    }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${stawka.percent}%", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    fontFamily = Jakarta,
                    color = if (setka) Paleta.I.ink else Tokeny.inkMuted
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
        Text("OBECNOŚĆ", style = GrafikType.sectionLabel, color = Tokeny.inkFaint)
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(Dim.rCardSmall))
                .background(Tokeny.surface)
                .border(1.dp, Tokeny.line, RoundedCornerShape(Dim.rCardSmall))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(8.dp).clip(RoundedCornerShape(999.dp))
                    .background(if (ob != null) Tokeny.ok else Tokeny.lineSoft))
                Text(
                    when {
                        ob?.trwa == true -> "Jesteś w pracy"
                        ob?.od != null && ob.doKiedy != null ->
                            "Wykryto ${ob.od.format(GODZINA)} – ${ob.doKiedy.format(GODZINA)}"
                        ob != null -> "Zapisane ${ob.hours} h"
                        else -> "Nic nie wykryto"
                    },
                    Modifier.weight(1f), fontSize = 13.sp, fontFamily = Jakarta, color = Tokeny.inkNaKarcie
                )
                if (ob != null) {
                    Text(if (ob.reczne) "ręcznie" else "auto",
                        style = GrafikType.caption, color = Tokeny.inkMuted)
                }
            }
            // Poszczególne wykrycia z prawdziwymi godzinami — żeby dało się porównać
            // z tym, jak było naprawdę, i odrzucić to, czego nie było.
            val wykrycia = s.wykrycia[dzien].orEmpty()
            if (wykrycia.isNotEmpty()) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(Tokeny.line))
                wykrycia.forEach { w -> WierszWykryciaDnia(w) { vm.odrzucWykrycie(w.id) } }
            }

            if (ob == null || ob.reczne) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(Tokeny.line))
                Row(
                    Modifier.fillMaxWidth().clickable(enabled = zmianaRobocza) {
                        vm.oznaczObecnosc(dzien, ob == null)
                    },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Byłem w pracy", style = GrafikType.cardTitle, color = Tokeny.ink)
                        Text(
                            if (zmianaRobocza) "godziny bierzemy ze zmiany" else "najpierw ustaw zmianę",
                            style = GrafikType.caption, color = Tokeny.inkMuted
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
    val ctx = LocalContext.current
    var dodaje by remember(dzien) { mutableStateOf(false) }
    var godzina by remember(dzien) { mutableStateOf("") }
    var nazwa by remember(dzien) { mutableStateOf("") }
    var przypomnij by remember(dzien) { mutableStateOf(true) }
    var rodzaj by remember(dzien) { mutableStateOf(RODZAJE.first()) }
    var osoba by remember(dzien) { mutableStateOf("") }
    var coroczne by remember(dzien) { mutableStateOf(true) }

    val wybierzKontakt = rememberLauncherForActivityResult(
        ActivityResultContracts.PickContact()
    ) { uri -> if (uri != null) imieZKontaktu(ctx, uri)?.let { osoba = it } }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("WYDARZENIA", Modifier.weight(1f), style = GrafikType.sectionLabel, color = Tokeny.inkFaint)
            Text("przypomnienia działają też przy zmianie nocnej",
                style = GrafikType.caption, color = Tokeny.inkFaint,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        wydarzenia.forEach { ev ->
            // Uroczystość zamiast godziny dostaje tort i własną barwę — nie ma pory dnia,
            // ma osobę.
            val swietuje = ev.rodzaj.isNotBlank()
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(Dim.rCardSmall))
                    .background(
                        if (swietuje) Tokeny.uroczystosc.copy(alpha = 0.07f) else Tokeny.surface
                    )
                    .border(
                        1.dp,
                        if (swietuje) Tokeny.uroczystosc.copy(alpha = 0.32f) else Tokeny.line,
                        RoundedCornerShape(Dim.rCardSmall)
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (swietuje) {
                    Icon(
                        IkonaTort, null, Modifier.width(44.dp).size(20.dp),
                        tint = Tokeny.uroczystosc
                    )
                } else {
                    Text(
                        ev.time, Modifier.width(44.dp),
                        style = TextStyle(
                            fontFamily = Jakarta, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                            fontFeatureSettings = TNUM
                        ),
                        color = Paleta.III.ink
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        if (swietuje) ev.osoba.ifBlank { ev.text } else ev.text,
                        style = GrafikType.cardTitle, color = Tokeny.ink,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    if (swietuje) {
                        Text(
                            etykietaRodzaju(ev.rodzaj) +
                                if (ev.coroczne) " · co roku" else " · tylko ten rok",
                            style = GrafikType.caption, color = Tokeny.uroczystosc
                        )
                    }
                    if (ev.remind) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Icon(IkonaDzwonek, null, Modifier.size(12.dp), tint = Tokeny.inkMuted)
                            Text("przypomnienie dzień wcześniej, ${"%02d".format(s.remindHour)}:00",
                                style = GrafikType.caption, color = Tokeny.inkMuted)
                        }
                    }
                }
                PrzyciskKwadrat(
                    IkonaKosz, "Usuń wydarzenie",
                    tlo = Color.Transparent, obrys = Tokeny.lineStrong,
                    kolorIkony = Tokeny.inkIkona
                ) { vm.deleteEvent(ev.id) }
            }
        }

        if (dodaje) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(Dim.rCardSmall))
                    .background(Tokeny.accent.copy(alpha = 0.06f))
                    .border(1.dp, Tokeny.accent.copy(alpha = 0.28f), RoundedCornerShape(Dim.rCardSmall))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Rodzaj rozstrzyga, o co pytamy dalej: wydarzenie ma godzinę i nazwę,
                // uroczystość — osobę i powtarzanie co roku.
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RODZAJE.forEach { r ->
                        val wybrany = r == rodzaj
                        Box(
                            Modifier.weight(1f).height(34.dp).clip(RoundedCornerShape(11.dp))
                                .background(if (wybrany) Tokeny.accentTintBg else Tokeny.surfaceInput)
                                .border(
                                    1.dp,
                                    if (wybrany) Tokeny.accentTintLine else Tokeny.lineInput,
                                    RoundedCornerShape(11.dp)
                                )
                                .clickable { rodzaj = r },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                etykietaRodzaju(r), fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold, fontFamily = Jakarta,
                                color = if (wybrany) Tokeny.accent else Tokeny.inkMuted,
                                maxLines = 1
                            )
                        }
                    }
                }

                if (rodzaj.isEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.width(92.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("godzina", fontSize = 10.sp, fontFamily = Jakarta, color = Tokeny.inkMuted)
                            PoleTekstowe(godzina, "9 20", cyfry = true) { godzina = it.filter(Char::isDigit).take(4) }
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("co to za wydarzenie", fontSize = 10.sp, fontFamily = Jakarta, color = Tokeny.inkMuted)
                            PoleTekstowe(nazwa, "np. badania okresowe") { nazwa = it.take(80) }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("czyje ${etykietaRodzaju(rodzaj).lowercase(PLL)}", fontSize = 10.sp,
                            fontFamily = Jakarta, color = Tokeny.inkMuted)
                        PoleOsoby(
                            osoba,
                            naZmiane = { osoba = it },
                            naWybor = { k -> osoba = k.imie }
                        )
                    }
                    PrzyciskDrugorzedny("Wybierz z kontaktów", Modifier.fillMaxWidth()) {
                        runCatching { wybierzKontakt.launch(null) }
                    }
                    Row(
                        Modifier.fillMaxWidth().clickable { coroczne = !coroczne },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Checkbox(
                            coroczne, { coroczne = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Tokeny.accent,
                                checkmarkColor = Tokeny.accentOn,
                                uncheckedColor = Tokeny.lineSoft
                            )
                        )
                        Text("Powtarzaj co roku", fontSize = 13.sp,
                            fontFamily = Jakarta, color = Tokeny.ink)
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
                            checkedColor = Tokeny.accent,
                            checkmarkColor = Tokeny.accentOn,
                            uncheckedColor = Tokeny.lineSoft
                        )
                    )
                    Text("Przypomnij dzień wcześniej o ${"%02d".format(s.remindHour)}:00",
                        fontSize = 13.sp, fontFamily = Jakarta, color = Tokeny.ink)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrzyciskDrugorzedny("Anuluj", Modifier.weight(1f)) { dodaje = false }
                    Box(
                        Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(13.dp))
                            .background(Tokeny.accent)
                            .clickable {
                                if (rodzaj.isEmpty()) {
                                    val g = godzina.padStart(4, '0')
                                    val czas = "${g.take(2).toInt()}:${g.takeLast(2)}"
                                    if (nazwa.isNotBlank()) {
                                        vm.addEvent(dzien, czas, nazwa.trim(), przypomnij)
                                        dodaje = false; godzina = ""; nazwa = ""
                                    }
                                } else if (osoba.isNotBlank()) {
                                    vm.addUroczystosc(dzien, rodzaj, osoba, przypomnij, coroczne)
                                    dodaje = false; osoba = ""
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Dodaj", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            fontFamily = Jakarta, color = Tokeny.accentOn)
                    }
                }
            }
        } else {
            // Kolor czytamy przed rysowaniem — wewnątrz `drawBehind` nie ma już
            // kontekstu kompozycji, z którego bierze się motyw.
            val kolorKreski = Tokeny.lineSoft
            Row(
                // Makieta: „border: 1px dashed #2A3036" — Compose nie ma obrysu
                // przerywanego w Modifier.border, więc rysujemy go sami.
                Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(15.dp))
                    .background(Tokeny.surface)
                    .drawBehind {
                        drawRoundRect(
                            color = kolorKreski,
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
                Icon(IkonaPlusCienki, null, Modifier.size(16.dp), tint = Tokeny.ink2)
                Text("Dodaj wydarzenie lub uroczystość", fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold, fontFamily = Jakarta, color = Tokeny.ink2)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// NOTATKA
// ─────────────────────────────────────────────────────────────

/**
 * Notatka do dnia — jedno zdanie własnymi słowami: „zamiana z Krzyśkiem",
 * „zabrać kask". Działa jak wydarzenie, tylko bez godziny i bez przypomnienia,
 * a na kafelku znaczy ją kropka w innym kolorze.
 */
@Composable
private fun SekcjaNotatki(s: UiState, dzien: LocalDate, vm: Vm) {
    val ctx = LocalContext.current
    val zapisana = s.entries[dzien]?.note.orEmpty()
    val zdjecie = s.entries[dzien]?.notePhoto
    var edytuje by remember(dzien) { mutableStateOf(false) }
    var tekst by remember(dzien, zapisana) { mutableStateOf(zapisana) }

    val wybierzZdjecie = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) vm.setNotePhoto(dzien, uri) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("NOTATKA", style = GrafikType.sectionLabel, color = Tokeny.inkFaint)

        when {
            edytuje -> Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(Dim.rCardSmall))
                    .background(Tokeny.notatka.copy(alpha = 0.07f))
                    .border(1.dp, Tokeny.notatka.copy(alpha = 0.30f), RoundedCornerShape(Dim.rCardSmall))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PoleTekstowe(tekst, "np. zamiana z Krzyśkiem") { tekst = it.take(200) }

                if (zdjecie != null) {
                    PodgladZdjecia(zdjecie, ctx) { vm.clearNotePhoto(dzien) }
                } else {
                    PrzyciskDrugorzedny("Dodaj zdjęcie z galerii", Modifier.fillMaxWidth()) {
                        wybierzZdjecie.launch(arrayOf("image/*"))
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrzyciskDrugorzedny("Anuluj", Modifier.weight(1f)) {
                        tekst = zapisana; edytuje = false
                    }
                    Box(
                        Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(13.dp))
                            .background(Tokeny.accent)
                            .clickable { vm.setNote(dzien, tekst); edytuje = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Zapisz", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            fontFamily = Jakarta, color = Tokeny.accentOn)
                    }
                }
            }

            zapisana.isNotBlank() || zdjecie != null -> Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (zdjecie != null) PodgladZdjecia(zdjecie, ctx) { vm.clearNotePhoto(dzien) }
                if (zapisana.isBlank()) {
                    PrzyciskDrugorzedny("Dopisz notatkę", Modifier.fillMaxWidth()) { edytuje = true }
                }
                if (zapisana.isNotBlank()) WierszNotatki(zapisana, { edytuje = true }) {
                    vm.setNote(dzien, ""); tekst = ""
                }
            }

            else -> {
                val kolorKreski = Tokeny.lineSoft
                Row(
                    Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(15.dp))
                        .background(Tokeny.surface)
                        .drawBehind {
                            drawRoundRect(
                                color = kolorKreski,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = 1.dp.toPx(),
                                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                        floatArrayOf(6.dp.toPx(), 5.dp.toPx())
                                    )
                                ),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(15.dp.toPx())
                            )
                        }
                        .clickable { edytuje = true },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    Icon(IkonaPlusCienki, null, Modifier.size(16.dp), tint = Tokeny.ink2)
                    Text("Dodaj notatkę", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        fontFamily = Jakarta, color = Tokeny.ink2)
                }
            }
        }
    }
}

/** Wiersz z zapisaną notatką: dotknięcie edytuje, kosz kasuje. */
@Composable
private fun WierszNotatki(tekst: String, naEdycje: () -> Unit, naUsuniecie: () -> Unit) {
    Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(Dim.rCardSmall))
                    .background(Tokeny.surface)
                    .border(1.dp, Tokeny.line, RoundedCornerShape(Dim.rCardSmall))
                    .clickable(onClick = naEdycje)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    Modifier.size(7.dp).clip(RoundedCornerShape(999.dp))
                        .background(Tokeny.notatka)
                )
                Text(tekst, Modifier.weight(1f), style = GrafikType.cardTitle,
                    color = Tokeny.ink, maxLines = 3, overflow = TextOverflow.Ellipsis)
                PrzyciskKwadrat(
            IkonaKosz, "Usuń notatkę",
            tlo = Color.Transparent, obrys = Tokeny.lineStrong,
            kolorIkony = Tokeny.inkIkona,
            akcja = naUsuniecie
        )
    }
}

/**
 * Zdjęcie dołączone do notatki. Dotknięcie otwiera je w przeglądarce obrazów,
 * kosz odpina od notatki i kasuje plik z pamięci aplikacji.
 */
@Composable
private fun PodgladZdjecia(nazwa: String, ctx: android.content.Context, naUsuniecie: () -> Unit) {
    val plik = remember(nazwa) { ZdjeciaNotatek.plik(ctx, nazwa) }
    // Miniaturę wczytujemy pomniejszoną i poza wątkiem rysowania — pełne zdjęcie
    // z aparatu ma kilkanaście megapikseli i zjadłoby pamięć na nic.
    val miniatura by produceState<android.graphics.Bitmap?>(null, plik.path) {
        value = withContext(Dispatchers.IO) { miniaturaZ(plik, 160) }
    }

    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Dim.rCardSmall))
            .background(Tokeny.surface)
            .border(1.dp, Tokeny.line, RoundedCornerShape(Dim.rCardSmall))
            .clickable {
                ZdjeciaNotatek.intencjaOtwarcia(ctx, nazwa)
                    ?.let { runCatching { ctx.startActivity(it) } }
            }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(56.dp).clip(RoundedCornerShape(12.dp))
                .background(Tokeny.surfaceInput),
            contentAlignment = Alignment.Center
        ) {
            miniatura?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Zdjęcie dołączone do notatki",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Zdjęcie", style = GrafikType.cardTitle, color = Tokeny.ink)
            Text(
                "${(plik.length() / 1024).coerceAtLeast(1)} kB · dotknij, aby otworzyć",
                style = GrafikType.caption, color = Tokeny.inkMuted
            )
        }
        PrzyciskKwadrat(
            IkonaKosz, "Usuń zdjęcie",
            tlo = Color.Transparent, obrys = Tokeny.lineStrong,
            kolorIkony = Tokeny.inkIkona,
            akcja = naUsuniecie
        )
    }
}

/**
 * Jedno wykrycie pobytu: godziny WEJŚCIA i WYJŚCIA prosto z czujników, obok
 * policzone godziny i źródło. Kosz odrzuca wykrycie, gdy go nie było.
 */
@Composable
private fun WierszWykryciaDnia(w: pl.grafik.pracy.data.PresenceRow, naOdrzucenie: () -> Unit) {
    val wejscie = remember(w.enterAt) { runCatching { LocalDateTime.parse(w.enterAt) }.getOrNull() }
    val wyjscie = remember(w.exitAt) { runCatching { LocalDateTime.parse(w.exitAt) }.getOrNull() }
    val odKiedy = remember(w.countedFrom) { runCatching { LocalDateTime.parse(w.countedFrom) }.getOrNull() }
    val doKiedy = remember(w.countedTo) { runCatching { LocalDateTime.parse(w.countedTo) }.getOrNull() }
    val policzone = pl.grafik.pracy.domain.PresenceEngine.countedHours(odKiedy, doKiedy)

    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                if (wejscie != null && wyjscie != null)
                    "${wejscie.format(GODZINA)} – ${wyjscie.format(GODZINA)}"
                else "godziny nieznane",
                style = TextStyle(
                    fontFamily = Jakarta, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    fontFeatureSettings = TNUM
                ),
                color = Tokeny.ink
            )
            Text(
                // Policzony zakres bywa inny niż wykryty — grafik zostaje grafikiem,
                // więc spóźnienie nie obcina normy. Pokazujemy oba, żeby liczba
                // godzin nie wyglądała na wziętą z powietrza.
                buildString {
                    append("wykryto przez ${zrodloWykrycia(w.source)}")
                    if (odKiedy != null && doKiedy != null) {
                        append(" · liczone ${odKiedy.format(GODZINA)}–${doKiedy.format(GODZINA)}")
                        append(" ($policzone h)")
                    } else {
                        append(" · liczone $policzone h")
                    }
                },
                style = GrafikType.caption, color = Tokeny.inkMuted
            )
        }
        PrzyciskKwadrat(
            IkonaKosz, "Odrzuć to wykrycie",
            tlo = Color.Transparent, obrys = Tokeny.lineStrong,
            kolorIkony = Tokeny.inkIkona,
            akcja = naOdrzucenie
        )
    }
}

private fun zrodloWykrycia(s: String): String = when {
    s.contains("wifi") && s.contains("geo") -> "lokalizację i Wi-Fi"
    s.contains("wifi") -> "Wi-Fi"
    s.contains("geo") -> "lokalizację"
    s == "manual" -> "wpis ręczny"
    else -> s
}

/** Pomniejszona kopia zdjęcia — tyle, ile trzeba na miniaturę w karcie dnia. */
private fun miniaturaZ(plik: java.io.File, bok: Int): android.graphics.Bitmap? = runCatching {
    if (!plik.exists()) return null
    val wymiary = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
    android.graphics.BitmapFactory.decodeFile(plik.path, wymiary)
    var skala = 1
    while (wymiary.outWidth / (skala * 2) >= bok && wymiary.outHeight / (skala * 2) >= bok) {
        skala *= 2
    }
    android.graphics.BitmapFactory.decodeFile(
        plik.path,
        android.graphics.BitmapFactory.Options().apply { inSampleSize = skala }
    )
}.getOrNull()

/** Rodzaje wpisów: pusty to zwykłe wydarzenie, reszta to uroczystości. */
private val RODZAJE = listOf("", "urodziny", "imieniny", "rocznica")

internal fun etykietaRodzaju(r: String): String = when (r) {
    "urodziny" -> "Urodziny"
    "imieniny" -> "Imieniny"
    "rocznica" -> "Rocznica"
    else -> "Wydarzenie"
}

/**
 * Imię osoby wskazanej w systemowym oknie kontaktów.
 *
 * Nie prosimy o dostęp do całej książki adresowej — wybór przez system daje
 * dostęp wyłącznie do tej jednej osoby, którą wskazałeś.
 */
private fun imieZKontaktu(ctx: android.content.Context, uri: android.net.Uri): String? =
    runCatching {
        ctx.contentResolver.query(
            uri,
            arrayOf(android.provider.ContactsContract.Contacts.DISPLAY_NAME),
            null, null, null
        )?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    }.getOrNull()
