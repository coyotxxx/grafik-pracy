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
import pl.grafik.pracy.domain.PlanPowiadomien
import pl.grafik.pracy.domain.PowiadomieniaCfg
import pl.grafik.pracy.domain.Shift
import pl.grafik.pracy.nowy.theme.*
import pl.grafik.pracy.nowy.ui.*
import pl.grafik.pracy.ui.UiState
import pl.grafik.pracy.ui.Vm
import java.time.LocalDate

/**
 * „Powiadomienia" — odtworzone z design/mockups/Notify.html.
 *
 * Wszystko jest lokalne: zadania planuje WorkManager na telefonie, nic nie wychodzi
 * na zewnątrz. Godziny, które wypadłyby na nocce, przesuwa `PlanPowiadomien`.
 */
@Composable
fun EkranPowiadomienia(vm: Vm, naPowrot: () -> Unit) {
    val s by vm.state.collectAsState()
    val cfg = s.powiadomienia

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
                Text("Powiadomienia", style = GrafikType.h1.copy(fontSize = 26.sp, lineHeight = 26.sp),
                    color = DarkTokens.ink)
                Text("Aplikacja odzywa się tylko wtedy, kiedy ma po co.",
                    fontSize = 12.sp, fontFamily = Jakarta, color = DarkTokens.inkMuted)
            }

            Podglad(s, cfg)
            KartaWydarzen(cfg) { vm.savePowiadomienia(it) }
            KartaCiszy(cfg) { vm.savePowiadomienia(it) }
            KartaZmiany(cfg) { vm.savePowiadomienia(it) }

            val pNotka by postepWejscia(Motion.RISE_MS, 200)
            Text(
                "Wszystkie powiadomienia są lokalne — nic nie wychodzi z telefonu. " +
                    "Jeśli system je blokuje, otwórz ustawienia Androida dla tej aplikacji.",
                fontSize = 11.sp, lineHeight = 16.5.sp, fontFamily = Jakarta,
                color = DarkTokens.inkFaint, modifier = Modifier.wejscie(pNotka)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// PODGLĄD POWIADOMIENIA
// ─────────────────────────────────────────────────────────────

@Composable
private fun Podglad(s: UiState, cfg: PowiadomieniaCfg) {
    val p by postepWejscia(Motion.RISE_MS, 30)
    val jutro = remember { LocalDate.now().plusDays(1) }
    val wydarzenie = s.events[jutro]?.firstOrNull()
    val zmiana = s.entries[jutro]?.shift

    Row(
        Modifier.wejscie(p).fillMaxWidth()
            .clip(RoundedCornerShape(Dim.rCardSmall))
            .background(Color(0x12FFFFFF))
            .border(1.dp, DarkTokens.lineSoft, RoundedCornerShape(Dim.rCardSmall))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(DarkTokens.accent),
            contentAlignment = Alignment.Center
        ) { Icon(IkonaGrafik, null, Modifier.size(18.dp), tint = DarkTokens.accentOn) }

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Grafik pracy", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    fontFamily = Jakarta, color = DarkTokens.inkMuted,
                    modifier = Modifier.weight(1f).alignByBaseline())
                Text("${cfg.godzinaWieczorna}:00", fontSize = 10.sp, fontFamily = Jakarta,
                    color = DarkTokens.inkFaint, modifier = Modifier.alignByBaseline())
            }
            Text(
                wydarzenie?.let { "Jutro o ${it.time} — ${it.text}" } ?: "Jutro: plan dnia",
                fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = Jakarta,
                color = DarkTokens.ink, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                opisJutra(zmiana), fontSize = 11.sp, fontFamily = Jakarta,
                color = DarkTokens.ink3, maxLines = 2, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun opisJutra(zmiana: Shift?): String = when (zmiana) {
    Shift.I -> "Jutro masz zmianę I (06:00–14:00)."
    Shift.II -> "Jutro masz zmianę II (14:00–22:00)."
    Shift.III -> "Jutro masz zmianę III (22:00–06:00). Wolne od rana."
    Shift.URLOP -> "Jutro urlop — nic nie planujemy."
    null -> "Jutro nie masz jeszcze nic w grafiku."
    else -> "Jutro dzień wolny."
}

// ─────────────────────────────────────────────────────────────
// WYDARZENIA
// ─────────────────────────────────────────────────────────────

@Composable
private fun KartaWydarzen(cfg: PowiadomieniaCfg, zapisz: (PowiadomieniaCfg) -> Unit) {
    val p by postepWejscia(Motion.RISE_MS, 80)

    Column(
        Modifier.wejscie(p).fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DarkTokens.surface)
            .border(1.dp, DarkTokens.line, RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp)
                .clickable { zapisz(cfg.copy(wydarzenia = !cfg.wydarzenia)) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier.size(34.dp).clip(RoundedCornerShape(11.dp))
                    .background(Color(0x24FF937E)),
                contentAlignment = Alignment.Center
            ) { Icon(IkonaDzwonek, null, Modifier.size(17.dp), tint = DarkTokens.warnInk) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Wydarzenia", fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    fontFamily = Jakarta, color = DarkTokens.ink)
                Text("fryzjer, badania, wizyty — wszystko z kalendarza",
                    fontSize = 11.sp, fontFamily = Jakarta, color = DarkTokens.inkMuted,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Przelacznik(cfg.wydarzenia) { zapisz(cfg.copy(wydarzenia = it)) }
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(DarkTokens.line))

        Row(
            Modifier.fillMaxWidth().heightIn(min = 54.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Dzień wcześniej", fontSize = 14.sp, fontFamily = Jakarta, color = DarkTokens.ink)
                Text("wieczorem dostajesz plan na jutro", fontSize = 11.sp,
                    fontFamily = Jakarta, color = DarkTokens.inkMuted)
            }
            StepperGodziny(cfg.godzinaWieczorna) { zapisz(cfg.copy(godzinaWieczorna = it)) }
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(DarkTokens.line))

        Column(
            Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().clickable { zapisz(cfg.copy(wDniu = !cfg.wDniu)) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("W dniu wydarzenia", fontSize = 14.sp, fontFamily = Jakarta,
                        color = DarkTokens.ink)
                    Text("krótko przed godziną", fontSize = 11.sp,
                        fontFamily = Jakarta, color = DarkTokens.inkMuted)
                }
                Przelacznik(cfg.wDniu) { zapisz(cfg.copy(wDniu = it)) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PlanPowiadomien.WYPRZEDZENIA.forEach { minuty ->
                    val wybrane = cfg.wyprzedzenieMin == minuty
                    Box(
                        Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(11.dp))
                            .background(if (wybrane) Color(0x2452D0B3) else DarkTokens.surfaceInput)
                            .border(
                                1.dp,
                                if (wybrane) Color(0x7352D0B3) else DarkTokens.lineInput,
                                RoundedCornerShape(11.dp)
                            )
                            .clickable(enabled = cfg.wDniu) { zapisz(cfg.copy(wyprzedzenieMin = minuty)) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            PlanPowiadomien.etykietaWyprzedzenia(minuty),
                            fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = Jakarta,
                            color = if (wybrane) DarkTokens.accent else DarkTokens.inkMuted
                        )
                    }
                }
            }
        }
    }
}

/** Godzina wieczornego przypomnienia — makieta ma pole czasu, tu stepper. */
@Composable
private fun StepperGodziny(godzina: Int, naZmiane: (Int) -> Unit) {
    Row(
        Modifier.height(40.dp).clip(RoundedCornerShape(11.dp))
            .background(DarkTokens.surfaceInput)
            .border(1.dp, DarkTokens.lineInput, RoundedCornerShape(11.dp))
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(
            Modifier.size(32.dp).clip(RoundedCornerShape(9.dp))
                .clickable { naZmiane((godzina - 1 + 24) % 24) },
            contentAlignment = Alignment.Center
        ) { Icon(IkonaMinus, "Wcześniej", Modifier.size(13.dp), tint = DarkTokens.ink2) }
        Text(
            "%02d:00".format(godzina), Modifier.widthIn(min = 46.dp),
            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                fontFamily = Jakarta, fontFeatureSettings = TNUM),
            color = DarkTokens.ink, textAlign = TextAlign.Center
        )
        Box(
            Modifier.size(32.dp).clip(RoundedCornerShape(9.dp))
                .clickable { naZmiane((godzina + 1) % 24) },
            contentAlignment = Alignment.Center
        ) { Icon(IkonaPlus, "Później", Modifier.size(13.dp), tint = DarkTokens.ink2) }
    }
}

// ─────────────────────────────────────────────────────────────
// CISZA NA NOCCE
// ─────────────────────────────────────────────────────────────

@Composable
private fun KartaCiszy(cfg: PowiadomieniaCfg, zapisz: (PowiadomieniaCfg) -> Unit) {
    val p by postepWejscia(Motion.RISE_MS, 120)
    Row(
        Modifier.wejscie(p).fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x127ABDFF))
            .border(1.dp, Color(0x387ABDFF), RoundedCornerShape(20.dp))
            .clickable { zapisz(cfg.copy(ciszaNaNocce = !cfg.ciszaNaNocce)) }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(11.dp))
                .background(Color(0x297ABDFF)),
            contentAlignment = Alignment.Center
        ) { Icon(IkonaOdpoczynek, null, Modifier.size(17.dp), tint = ShiftPaletteDark.III.ink) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("Nie budź mnie na nocce", style = GrafikType.cardTitle, color = DarkTokens.ink)
            Text(
                "Powiadomienie, które wypadłoby w trakcie zmiany nocnej albo do " +
                    "${PlanPowiadomien.CISZA_PO_NOCCE_H} h po niej, przesuwamy na późniejszą porę.",
                fontSize = 11.sp, lineHeight = 16.5.sp, fontFamily = Jakarta, color = DarkTokens.ink3
            )
        }
        Przelacznik(cfg.ciszaNaNocce) { zapisz(cfg.copy(ciszaNaNocce = it)) }
    }
}

// ─────────────────────────────────────────────────────────────
// PRZYPOMNIENIE O ZMIANIE
// ─────────────────────────────────────────────────────────────

@Composable
private fun KartaZmiany(cfg: PowiadomieniaCfg, zapisz: (PowiadomieniaCfg) -> Unit) {
    val p by postepWejscia(Motion.RISE_MS, 160)
    Column(
        Modifier.wejscie(p).fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DarkTokens.surface)
            .border(1.dp, DarkTokens.line, RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 54.dp)
                .clickable { zapisz(cfg.copy(przedZmiana = !cfg.przedZmiana)) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Przypomnienie o zmianie", style = GrafikType.cardTitle, color = DarkTokens.ink)
                Text(
                    "${PlanPowiadomien.etykietaWyprzedzenia(cfg.przedZmianaMin)} przed startem · tylko dni robocze",
                    fontSize = 11.sp, fontFamily = Jakarta, color = DarkTokens.inkMuted
                )
            }
            Przelacznik(cfg.przedZmiana) { zapisz(cfg.copy(przedZmiana = it)) }
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(DarkTokens.line))

        Row(
            Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PlanPowiadomien.WYPRZEDZENIA.forEach { minuty ->
                val wybrane = cfg.przedZmianaMin == minuty
                Box(
                    Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(11.dp))
                        .background(if (wybrane) Color(0x2452D0B3) else DarkTokens.surfaceInput)
                        .border(
                            1.dp,
                            if (wybrane) Color(0x7352D0B3) else DarkTokens.lineInput,
                            RoundedCornerShape(11.dp)
                        )
                        .clickable(enabled = cfg.przedZmiana) { zapisz(cfg.copy(przedZmianaMin = minuty)) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        PlanPowiadomien.etykietaWyprzedzenia(minuty),
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = Jakarta,
                        color = if (wybrane) DarkTokens.accent else DarkTokens.inkMuted
                    )
                }
            }
        }
    }
}
