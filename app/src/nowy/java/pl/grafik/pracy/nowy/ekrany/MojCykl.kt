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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.grafik.pracy.domain.CycleConfig
import pl.grafik.pracy.domain.CycleGenerator
import pl.grafik.pracy.domain.CyclePattern
import pl.grafik.pracy.nowy.theme.*
import pl.grafik.pracy.nowy.ui.*
import pl.grafik.pracy.ui.UiState
import pl.grafik.pracy.ui.Vm
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

private val PL_CYKL = Locale.forLanguageTag("pl-PL")

/** Zakresy wypełniania z makiety: „1 mies.", „3 mies.", „6 mies.", „rok", „bez końca". */
private data class Zakres(val etykieta: String, val miesiecy: Int?)

private val ZAKRESY = listOf(
    Zakres("1 mies.", 1), Zakres("3 mies.", 3), Zakres("6 mies.", 6),
    Zakres("rok", 12), Zakres("bez końca", null)
)

/**
 * „Mój cykl" — odtworzony z design/mockups/Cycle.html.
 *
 * Każda zmiana zapisuje się od razu przez `Vm.saveConfig`, tak samo jak w klasycznej
 * aplikacji — to ten sam `CycleConfig`, więc obie aplikacje widzą ten sam cykl.
 */
@Composable
fun EkranMojCykl(vm: Vm, naPowrot: () -> Unit) {
    val s by vm.state.collectAsState()
    val cfg = s.cfg

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
                Text("Mój cykl", style = GrafikType.h1.copy(fontSize = 26.sp, lineHeight = 26.sp),
                    color = DarkTokens.ink)
                Text("Ustawiasz raz — grafik liczy się sam na każdy miesiąc.",
                    fontSize = 12.sp, fontFamily = Jakarta, color = DarkTokens.inkMuted)
            }

            KartaWypelniania(s, vm)
            SystemPracy(cfg, vm)
            KierunekRotacji(cfg, vm)
            MojaBrygada(cfg, vm)
            Podglad(cfg)

            PrzyciskDrugorzedny("Przywróć ten miesiąc do cyklu", Modifier.fillMaxWidth()) {
                vm.resetMonth()
            }
        }
    }
}

@Composable
private fun PowrotDoUstawien(naPowrot: () -> Unit) {
    Row(
        Modifier.height(40.dp).offset(x = (-6).dp)
            .clip(RoundedCornerShape(11.dp))
            .clickable(onClick = naPowrot)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(IkonaWLewo, null, Modifier.size(17.dp), tint = DarkTokens.inkMuted)
        Text("Ustawienia", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            fontFamily = Jakarta, color = DarkTokens.inkMuted)
    }
}

// ─────────────────────────────────────────────────────────────
// WYPEŁNIANIE Z CYKLU
// ─────────────────────────────────────────────────────────────

@Composable
private fun KartaWypelniania(s: UiState, vm: Vm) {
    val p by postepWejscia(Motion.RISE_MS, 50)
    val cfg = s.cfg
    val od = cfg.genFrom ?: YearMonth.now()

    Column(
        Modifier.wejscie(p).fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x1252D0B3))
            .border(1.dp, Color(0x4252D0B3), RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().clickable { vm.saveConfig(cfg.copy(generate = !cfg.generate)) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Wypełniaj grafik z cyklu", fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    fontFamily = Jakarta, color = DarkTokens.ink)
                Text(
                    if (cfg.generate) "włączone — kalendarz liczy zmiany sam"
                    else "wyłączone — kalendarz pokazuje tylko to, co wpiszesz",
                    fontSize = 11.sp, fontFamily = Jakarta, color = Color(0xFF9FE3D2)
                )
            }
            Przelacznik(cfg.generate) { vm.saveConfig(cfg.copy(generate = it)) }
        }

        Text(
            "Dni wpisane ręcznie zawsze mają pierwszeństwo przed cyklem — nic Ci ich nie nadpisze.",
            fontSize = 11.sp, lineHeight = 16.5.sp, fontFamily = Jakarta, color = DarkTokens.inkMuted
        )

        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x2E52D0B3)))

        Row(
            Modifier.fillMaxWidth().alpha(if (cfg.generate) 1f else 0.5f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Wypełniaj od", fontSize = 12.sp, fontFamily = Jakarta,
                color = DarkTokens.ink3, modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PrzyciskKwadrat(IkonaMinus, "Wcześniejszy miesiąc", rozmiar = 34.dp,
                    tlo = Color(0x4D000000), obrys = DarkTokens.lineSoft) {
                    if (cfg.generate) vm.saveConfig(przesunOd(cfg, od.minusMonths(1)))
                }
                Text(
                    miesiacRokCykl(od), Modifier.widthIn(min = 108.dp),
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = Jakarta,
                    color = DarkTokens.ink, textAlign = TextAlign.Center
                )
                PrzyciskKwadrat(IkonaPlus, "Późniejszy miesiąc", rozmiar = 34.dp,
                    tlo = Color(0x4D000000), obrys = DarkTokens.lineSoft) {
                    if (cfg.generate) vm.saveConfig(przesunOd(cfg, od.plusMonths(1)))
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().alpha(if (cfg.generate) 1f else 0.5f),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            ZAKRESY.forEach { z ->
                val wybrany = dlugoscZakresu(cfg) == z.miesiecy
                Box(
                    Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(11.dp))
                        .background(if (wybrany) Color(0x2952D0B3) else Color(0x4D000000))
                        .border(
                            1.dp,
                            if (wybrany) Color(0x7352D0B3) else DarkTokens.lineSoft,
                            RoundedCornerShape(11.dp)
                        )
                        .clickable(enabled = cfg.generate) { vm.saveConfig(ustawZakres(cfg, od, z.miesiecy)) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(z.etykieta, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                        fontFamily = Jakarta, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        color = if (wybrany) DarkTokens.accent else DarkTokens.inkMuted)
                }
            }
        }

        Text(opisZakresu(cfg, od), fontSize = 11.sp, fontFamily = Jakarta, color = DarkTokens.inkMuted)
    }
}

/** Zmiana miesiąca startowego zachowuje długość zakresu. */
private fun przesunOd(cfg: CycleConfig, nowyOd: YearMonth): CycleConfig =
    ustawZakres(cfg.copy(genFrom = nowyOd), nowyOd, dlugoscZakresu(cfg))

private fun ustawZakres(cfg: CycleConfig, od: YearMonth, miesiecy: Int?): CycleConfig =
    cfg.copy(
        genFrom = od,
        genTo = if (miesiecy == null) null else od.plusMonths((miesiecy - 1).toLong())
    )

/** Ile miesięcy obejmuje bieżące ustawienie; null = bez końca. */
private fun dlugoscZakresu(cfg: CycleConfig): Int? {
    val od = cfg.genFrom ?: return null
    val doKiedy = cfg.genTo ?: return null
    return (java.time.temporal.ChronoUnit.MONTHS.between(od, doKiedy) + 1).toInt()
}

private fun opisZakresu(cfg: CycleConfig, od: YearMonth): String {
    val doKiedy = cfg.genTo ?: return "Cykl wypełnia kalendarz bez końca."
    return if (doKiedy == od) "Cykl wypełni ${miesiacRokCykl(od).lowercase(PL_CYKL)}."
    else "Cykl wypełni ${miesiacRokCykl(od).lowercase(PL_CYKL)} – ${miesiacRokCykl(doKiedy).lowercase(PL_CYKL)}."
}

// ─────────────────────────────────────────────────────────────
// SYSTEM PRACY
// ─────────────────────────────────────────────────────────────

@Composable
private fun SystemPracy(cfg: CycleConfig, vm: Vm) {
    val p by postepWejscia(Motion.RISE_MS, 100)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("SYSTEM PRACY", style = GrafikType.sectionLabel, color = DarkTokens.inkFaint,
            modifier = Modifier.wejscie(p))
        CyclePattern.entries.forEach { wzorzec ->
            val wybrany = wzorzec == cfg.pattern
            Row(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(Dim.rCardSmall))
                    .background(if (wybrany) Color(0x2452D0B3) else DarkTokens.surface)
                    .border(
                        1.dp,
                        if (wybrany) Color(0x7352D0B3) else DarkTokens.line,
                        RoundedCornerShape(Dim.rCardSmall)
                    )
                    .clickable {
                        val kroki = if (wzorzec.weekAligned) wzorzec.weeks else wzorzec.length
                        vm.saveConfig(cfg.copy(pattern = wzorzec, anchorIndex = cfg.anchorIndex % kroki))
                    }
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(wzorzec.label, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        fontFamily = Jakarta,
                        color = if (wybrany) DarkTokens.accent else DarkTokens.ink)
                    Text(wzorzec.desc, fontSize = 11.sp, fontFamily = Jakarta,
                        color = DarkTokens.inkMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Text("${wzorzec.length} dni", fontSize = 10.sp, fontFamily = Jakarta,
                    color = DarkTokens.inkFaint)
                Box(
                    Modifier.size(20.dp).clip(RoundedCornerShape(999.dp))
                        .background(if (wybrany) DarkTokens.accent else Color.Transparent)
                        .border(
                            1.dp,
                            if (wybrany) DarkTokens.accent else DarkTokens.lineSoft,
                            RoundedCornerShape(999.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (wybrany) Icon(IkonaPtaszek, null, Modifier.size(12.dp), tint = DarkTokens.accentOn)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// KIERUNEK ROTACJI
// ─────────────────────────────────────────────────────────────

@Composable
private fun KierunekRotacji(cfg: CycleConfig, vm: Vm) {
    val p by postepWejscia(Motion.RISE_MS, 140)
    val kolejnosc = remember(cfg.pattern, cfg.reverse) { CycleGenerator.rotationLabel(cfg) }
    val odwrotna = remember(cfg.pattern) {
        CycleGenerator.rotationLabel(cfg.copy(reverse = !cfg.reverse))
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("KIERUNEK ROTACJI", style = GrafikType.sectionLabel, color = DarkTokens.inkFaint,
            modifier = Modifier.wejscie(p))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            KafelekKierunku(
                Modifier.weight(1f),
                if (cfg.reverse) odwrotna else kolejnosc,
                "zwykła kolejność", !cfg.reverse
            ) { vm.saveConfig(cfg.copy(reverse = false)) }
            KafelekKierunku(
                Modifier.weight(1f),
                if (cfg.reverse) kolejnosc else odwrotna,
                "odwrotna kolejność", cfg.reverse
            ) { vm.saveConfig(cfg.copy(reverse = true)) }
        }
    }
}

@Composable
private fun KafelekKierunku(
    modifier: Modifier,
    etykieta: String,
    podpis: String,
    wybrany: Boolean,
    akcja: () -> Unit
) {
    Column(
        modifier.height(58.dp).clip(RoundedCornerShape(16.dp))
            .background(if (wybrany) Color(0x2452D0B3) else DarkTokens.surface)
            .border(
                1.dp,
                if (wybrany) Color(0x7352D0B3) else DarkTokens.line,
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = akcja),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically)
    ) {
        Text(etykieta, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = Jakarta,
            letterSpacing = 0.28.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            color = if (wybrany) DarkTokens.accent else DarkTokens.inkMuted)
        Text(podpis, fontSize = 10.sp, fontFamily = Jakarta, color = DarkTokens.inkMuted)
    }
}

// ─────────────────────────────────────────────────────────────
// BRYGADA I PRZESUNIĘCIE
// ─────────────────────────────────────────────────────────────

@Composable
private fun MojaBrygada(cfg: CycleConfig, vm: Vm) {
    val p by postepWejscia(Motion.RISE_MS, 180)
    val tygodniowy = cfg.pattern.weekAligned
    val krokow = if (tygodniowy) cfg.pattern.weeks else cfg.pattern.length

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("MOJA BRYGADA", style = GrafikType.sectionLabel, color = DarkTokens.inkFaint,
            modifier = Modifier.wejscie(p))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("A", "B", "C", "D").forEach { b ->
                val wybrana = cfg.brigade == b
                Box(
                    Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(16.dp))
                        .background(if (wybrana) Color(0x2452D0B3) else DarkTokens.surface)
                        .border(
                            1.dp,
                            if (wybrana) Color(0x7352D0B3) else DarkTokens.line,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { vm.saveConfig(cfg.copy(brigade = b)) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(b, fontSize = 17.sp, fontWeight = FontWeight.Bold, fontFamily = Jakarta,
                        color = if (wybrana) DarkTokens.accent else DarkTokens.inkMuted)
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(DarkTokens.surface)
                .border(1.dp, DarkTokens.line, RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PrzyciskPrzesuniecia(if (tygodniowy) "−1 tydzień" else "−1 dzień") {
                vm.saveConfig(cfg.copy(anchorIndex = (cfg.anchorIndex - 1 + krokow) % krokow))
            }
            Text(
                if (tygodniowy) "tydzień ${cfg.anchorIndex + 1} z $krokow"
                else "pozycja ${cfg.anchorIndex}",
                Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                fontFamily = Jakarta, color = DarkTokens.ink3, textAlign = TextAlign.Center
            )
            PrzyciskPrzesuniecia(if (tygodniowy) "+1 tydzień" else "+1 dzień") {
                vm.saveConfig(cfg.copy(anchorIndex = (cfg.anchorIndex + 1) % krokow))
            }
        }

        Text(
            "Nie wiesz, które przesunięcie jest Twoje? Ustaw takie, przy którym podgląd poniżej zgadza się z tym tygodniem.",
            fontSize = 11.sp, lineHeight = 16.5.sp, fontFamily = Jakarta, color = DarkTokens.inkFaint
        )
    }
}

@Composable
private fun PrzyciskPrzesuniecia(tekst: String, akcja: () -> Unit) {
    Box(
        Modifier.height(34.dp).clip(RoundedCornerShape(11.dp))
            .background(DarkTokens.surfaceInput)
            .border(1.dp, DarkTokens.lineInput, RoundedCornerShape(11.dp))
            .clickable(onClick = akcja)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(tekst, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            fontFamily = Jakarta, color = DarkTokens.ink2)
    }
}

// ─────────────────────────────────────────────────────────────
// PODGLĄD
// ─────────────────────────────────────────────────────────────

@Composable
private fun Podglad(cfg: CycleConfig) {
    val p by postepWejscia(Motion.RISE_MS, 220)
    val dzis = remember { LocalDate.now() }
    val dni = remember(cfg, dzis) {
        (0 until 21).map { i ->
            val d = dzis.plusDays(i.toLong())
            d to (if (cfg.generate) CycleGenerator.shiftFor(cfg, d) else null)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("PODGLĄD — 21 DNI OD DZIŚ", style = GrafikType.sectionLabel,
            color = DarkTokens.inkFaint, modifier = Modifier.wejscie(p))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(Dim.rCardSmall))
                .background(DarkTokens.surface)
                .border(1.dp, DarkTokens.line, RoundedCornerShape(Dim.rCardSmall))
                .padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            dni.forEach { (_, zmiana) ->
                val k = ShiftPaletteDark.of(typDniaZ(zmiana))
                Box(
                    Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(7.dp))
                        .background(k.fill)
                        .border(1.dp, k.line, RoundedCornerShape(7.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        zmiana?.code ?: "·", fontSize = 8.sp, fontWeight = FontWeight.Bold,
                        fontFamily = Jakarta, letterSpacing = 0.16.sp, color = k.ink
                    )
                }
            }
        }
    }
}

private fun miesiacRokCykl(ym: YearMonth): String =
    ym.month.getDisplayName(JavaTextStyle.FULL_STANDALONE, PL_CYKL)
        .replaceFirstChar { it.uppercase(PL_CYKL) } + " " + ym.year
