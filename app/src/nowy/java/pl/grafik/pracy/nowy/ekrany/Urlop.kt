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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.grafik.pracy.domain.VacationCfg
import pl.grafik.pracy.nowy.theme.*
import pl.grafik.pracy.nowy.ui.*
import pl.grafik.pracy.ui.UiState
import pl.grafik.pracy.ui.Vm
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val DATA_PL = DateTimeFormatter.ofPattern("dd.MM.yyyy")

/**
 * „Urlop" — odtworzony z design/mockups/Leave.html.
 *
 * Liczby to ten sam `VacationCfg` i `VacationBalance`, których używa klasyczna
 * aplikacja: zaległy zużywa się przed bieżącym, a dzień urlopu pokrywa 8 h normy.
 */
@Composable
fun EkranUrlop(vm: Vm, naPowrot: () -> Unit) {
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
                Text("Urlop", style = GrafikType.h1.copy(fontSize = 26.sp, lineHeight = 26.sp),
                    color = DarkTokens.ink)
                Text("Ustaw raz w roku — dalej aplikacja odlicza sama.",
                    fontSize = 12.sp, fontFamily = Jakarta, color = DarkTokens.inkMuted)
            }

            KartaPozostalo(s)
            KartaWymiaru(s, vm)
            KartaStanuZZakladu(s, vm)
            KartaJakLiczymy()
        }
    }
}

// ─────────────────────────────────────────────────────────────
// ZOSTAŁO
// ─────────────────────────────────────────────────────────────

@Composable
private fun KartaPozostalo(s: UiState) {
    val p by postepWejscia(Motion.RISE_MS, 50)
    val u = s.urlopBilans
    val pula = u.bazaZalegly + u.bazaBiezacy
    val rozowy = ShiftPaletteDark.URLOP.ink

    Column(
        Modifier.wejscie(p).fillMaxWidth()
            .clip(RoundedCornerShape(Dim.rCard))
            .background(Color(0x12F490D9))
            .border(1.dp, Color(0x3DF490D9), RoundedCornerShape(Dim.rCard))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("ZOSTAŁO NA ${u.rok}", style = GrafikType.sectionLabel, color = DarkTokens.inkMuted)

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(
                    "${u.zostalo}",
                    style = GrafikType.heroNumber.copy(fontSize = 42.sp, lineHeight = 38.sp),
                    color = rozowy, modifier = Modifier.alignByBaseline()
                )
                Text("dni", fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    fontFamily = Jakarta, color = Color(0xFFD7DBDE),
                    modifier = Modifier.alignByBaseline())
            }
            Text(
                "z $pula dni wymiaru\n${u.zuzyte} wykorzystane",
                fontSize = 11.sp, lineHeight = 16.sp, fontFamily = Jakarta,
                color = DarkTokens.inkMuted, textAlign = TextAlign.End
            )
        }

        Box(
            Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(999.dp))
                .background(Color(0x4D000000))
        ) {
            val udzial = if (pula > 0) u.zuzyte.toFloat() / pula else 0f
            Box(
                Modifier.fillMaxWidth(udzial.coerceIn(0f, 1f)).fillMaxHeight()
                    .clip(RoundedCornerShape(999.dp)).background(rozowy)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// WYMIAR I ZALEGŁY
// ─────────────────────────────────────────────────────────────

@Composable
private fun KartaWymiaru(s: UiState, vm: Vm) {
    val p by postepWejscia(Motion.RISE_MS, 90)
    val cfg = s.urlop
    val rok = s.urlopBilans.rok

    KartaUstawien(Modifier.wejscie(p)) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("Wymiar roczny", style = GrafikType.cardTitle, color = DarkTokens.ink)
            Text("20 dni do 10 lat stażu, 26 powyżej",
                fontSize = 11.sp, fontFamily = Jakarta, color = DarkTokens.inkMuted)
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(20, 26).forEach { ile ->
                val wybrany = cfg.wymiar == ile
                Box(
                    Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(14.dp))
                        .background(if (wybrany) Color(0x29F490D9) else DarkTokens.surfaceInput)
                        .border(
                            1.dp,
                            if (wybrany) Color(0x6BF490D9) else DarkTokens.lineInput,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { vm.saveVacation(cfg.copy(wymiar = ile)) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("$ile dni", fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        fontFamily = Jakarta,
                        color = if (wybrany) ShiftPaletteDark.URLOP.ink else DarkTokens.inkMuted)
                }
            }
            StepperDni(cfg.wymiar, "dni wymiaru") { vm.saveVacation(cfg.copy(wymiar = it.coerceIn(0, 40))) }
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(DarkTokens.line))

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Zaległy z ${rok - 1}", style = GrafikType.cardTitle, color = DarkTokens.ink)
                Text("doliczany do wymiaru", fontSize = 11.sp,
                    fontFamily = Jakarta, color = DarkTokens.inkMuted)
            }
            StepperDni(cfg.zalegly, "dni zaległych") { vm.saveVacation(cfg.copy(zalegly = it.coerceIn(0, 40))) }
        }

        PasekTerminu(cfg, rok)
    }
}

/** Wygląd paska terminu — tło, obrys, kolor tekstu i sama treść. */
private data class Termin(val tlo: Color, val obrys: Color, val ink: Color, val tekst: String)

/** Ostrzeżenie o terminie z art. 168 KP — zaległy urlop przepada po 30 września. */
@Composable
private fun PasekTerminu(cfg: VacationCfg, rok: Int) {
    val dzis = remember { LocalDate.now() }
    val termin = remember(rok) { VacationCfg.terminZaleglego(rok) }

    val stan = when {
        cfg.zalegly <= 0 -> Termin(
            Color(0x08FFFFFF), DarkTokens.line, Color(0xFF8A939B),
            "Brak zaległego urlopu — nic nie przepada."
        )
        dzis.isAfter(termin) -> Termin(
            DarkTokens.warnBg, DarkTokens.warnLine, DarkTokens.warnInk,
            "Termin na zaległy urlop minął ${termin.format(DATA_PL)}."
        )
        else -> Termin(
            Color(0x14DAC559), Color(0x42DAC559), ShiftPaletteDark.I.ink,
            "Zaległy urlop trzeba wykorzystać do 30 września. Przypomnimy w sierpniu."
        )
    }

    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(stan.tlo)
            .border(1.dp, stan.obrys, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(IkonaOstrzezenie, null, Modifier.size(16.dp).padding(top = 1.dp), tint = stan.ink)
        Text(stan.tekst, fontSize = 11.sp, lineHeight = 16.5.sp,
            fontFamily = Jakarta, color = stan.ink)
    }
}

// ─────────────────────────────────────────────────────────────
// STAN Z ZAKŁADU
// ─────────────────────────────────────────────────────────────

@Composable
private fun KartaStanuZZakladu(s: UiState, vm: Vm) {
    val p by postepWejscia(Motion.RISE_MS, 130)
    val cfg = s.urlop
    val rozowy = ShiftPaletteDark.URLOP.ink
    // Stepper trzyma własną wartość do czasu zapisu — dopóki nie dotkniesz
    // „Zapisz stan na dziś", nic nie zmienia się w rozliczeniu.
    var biezacy by remember(cfg.stanData, cfg.stanBiezacy) {
        mutableIntStateOf(if (cfg.stanData != null) cfg.stanBiezacy else cfg.wymiar)
    }

    KartaUstawien(Modifier.wejscie(p)) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("Stan z zakładu", style = GrafikType.cardTitle, color = DarkTokens.ink)
            Text(
                "Jeśli kadrowa poda Ci aktualne liczby, zapisz je tutaj. Od tego dnia aplikacja " +
                    "odlicza sama, zdejmując najpierw urlop zaległy.",
                fontSize = 11.sp, lineHeight = 16.5.sp, fontFamily = Jakarta, color = DarkTokens.inkMuted
            )
        }

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("bieżący", fontSize = 13.sp, fontFamily = Jakarta,
                color = DarkTokens.ink3, modifier = Modifier.weight(1f))
            StepperDni(biezacy, "dni bieżących") { biezacy = it.coerceIn(0, 60) }
        }

        Box(
            Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(15.dp))
                .background(rozowy)
                .clickable {
                    vm.saveVacation(
                        cfg.copy(
                            stanData = LocalDate.now(),
                            stanBiezacy = biezacy,
                            stanZalegly = cfg.zalegly
                        )
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text("Zapisz stan na dziś", fontSize = 14.sp, fontWeight = FontWeight.Bold,
                fontFamily = Jakarta, color = Color(0xFF2A0B24))
        }

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                cfg.stanData?.let {
                    "Zapisane ${it.format(DATA_PL)}: ${cfg.stanBiezacy} bieżącego, ${cfg.stanZalegly} zaległego"
                } ?: "Nic nie zapisano — liczymy od początku roku.",
                fontSize = 11.sp, fontFamily = Jakarta, color = DarkTokens.inkMuted,
                modifier = Modifier.weight(1f)
            )
            if (cfg.stanData != null) {
                Box(
                    Modifier.height(30.dp).clip(RoundedCornerShape(10.dp))
                        .border(1.dp, DarkTokens.lineSoft, RoundedCornerShape(10.dp))
                        .clickable {
                            vm.saveVacation(cfg.copy(stanData = null, stanBiezacy = 0, stanZalegly = 0))
                        }
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Wyczyść", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                        fontFamily = Jakarta, color = DarkTokens.inkMuted)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// JAK TO LICZYMY
// ─────────────────────────────────────────────────────────────

@Composable
private fun KartaJakLiczymy() {
    val p by postepWejscia(Motion.RISE_MS, 170)
    Column(
        Modifier.wejscie(p).fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x08FFFFFF))
            .border(1.dp, DarkTokens.line, RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("JAK TO LICZYMY", style = GrafikType.sectionLabel, color = DarkTokens.inkFaint)
        listOf(
            "Dzień urlopu = 8 h i wlicza się do normy miesiąca.",
            "Najpierw zdejmujemy zaległy, dopiero potem bieżący.",
            "Liczymy dni oznaczone w grafiku jako U — także te w przyszłości."
        ).forEach { zdanie ->
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Box(
                    Modifier.padding(top = 6.dp).size(5.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(ShiftPaletteDark.URLOP.ink)
                )
                Text(zdanie, fontSize = 12.sp, lineHeight = 18.sp,
                    fontFamily = Jakarta, color = DarkTokens.ink3)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// STEPPER
// ─────────────────────────────────────────────────────────────

/** Stepper dni z makiet urlopu: dwa przyciski 34 dp w kapsule r14. */
@Composable
private fun StepperDni(wartosc: Int, opis: String, naZmiane: (Int) -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(14.dp))
            .background(DarkTokens.surfaceInput)
            .border(1.dp, DarkTokens.lineInput, RoundedCornerShape(14.dp))
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(10.dp))
                .clickable { naZmiane(wartosc - 1) },
            contentAlignment = Alignment.Center
        ) { Icon(IkonaMinus, "Mniej $opis", Modifier.size(14.dp), tint = DarkTokens.ink2) }

        Text(
            "$wartosc", Modifier.widthIn(min = 26.dp),
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold,
                fontFamily = Jakarta, fontFeatureSettings = TNUM),
            color = DarkTokens.ink, textAlign = TextAlign.Center
        )

        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(10.dp))
                .clickable { naZmiane(wartosc + 1) },
            contentAlignment = Alignment.Center
        ) { Icon(IkonaPlus, "Więcej $opis", Modifier.size(14.dp), tint = DarkTokens.ink2) }
    }
}
