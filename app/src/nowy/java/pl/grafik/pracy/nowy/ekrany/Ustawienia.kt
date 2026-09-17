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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.grafik.pracy.nowy.theme.*
import pl.grafik.pracy.nowy.Podstrona
import pl.grafik.pracy.nowy.ui.*
import pl.grafik.pracy.ui.PresenceVm
import pl.grafik.pracy.ui.UiState
import pl.grafik.pracy.ui.UpdateVm
import pl.grafik.pracy.ui.Vm
import pl.grafik.pracy.update.UpdateProgress
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

private val PL_UST = Locale.forLanguageTag("pl-PL")

/**
 * Ekran „Ustawienia" — odtworzony z design/mockups/Settings.html.
 *
 * Wiersze prowadzą do podstron, które powstają po kolei. Dopóki podstrony nie ma,
 * wiersz jest przygaszony i nieklikalny — widać, co już działa, a co dochodzi.
 * Podpisy pod nazwami pokazują prawdziwy stan aplikacji, nie przykłady z makiety.
 */
@Composable
fun EkranUstawienia(vm: Vm, pvm: PresenceVm, uvm: UpdateVm, naPodstrone: (Podstrona) -> Unit) {
    val s by vm.state.collectAsState()
    val p by pvm.state.collectAsState()
    val u by uvm.state.collectAsState()

    Box(Modifier.fillMaxSize()) {
        TloZPoswiata(Modifier.fillMaxSize())

        Column(
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = gornaKrawedz(), bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val pNag by postepWejscia(Motion.RISE_MS)
            Text(
                "Ustawienia", style = GrafikType.h1, color = DarkTokens.ink,
                modifier = Modifier.wejscie(pNag).padding(horizontal = Dim.screenGutter)
            )

            KartaCyklu(s) { naPodstrone(Podstrona.CYKL) }

            Grupa("ROZLICZANIE", 80) {
                Wiersz(
                    IkonaCzasPracy, ShiftPaletteDark.I.ink, "Czas pracy i nadgodziny",
                    opisOkresu(s), gotowe = true, akcja = { naPodstrone(Podstrona.CZAS_PRACY) }
                )
                Wiersz(
                    IkonaWyplata, ShiftPaletteDark.I.ink, "Stawki i wypłata",
                    "stawka godzinowa, dodatki, premia", gotowe = false
                )
                Wiersz(
                    IkonaOdpoczynek, DarkTokens.warnInk, "Odpoczynek",
                    "11 h na dobę, 35 h w tygodniu · ostrzeżenia", gotowe = false
                )
                Wiersz(
                    IkonaUrlop, ShiftPaletteDark.URLOP.ink, "Urlop",
                    opisUrlopu(s), gotowe = true, akcja = { naPodstrone(Podstrona.URLOP) }
                )
            }

            Grupa("AUTOMATYKA", 160) {
                Wiersz(
                    IkonaLokalizacja, ShiftPaletteDark.III.ink, "Wykrywanie pracy",
                    opisWykrywania(p.place), gotowe = true,
                    akcja = { naPodstrone(Podstrona.WYKRYWANIE) },
                    kropka = if (p.place.enabled) DarkTokens.ok else null
                )
                Wiersz(
                    IkonaDzwonek, DarkTokens.warnInk, "Powiadomienia",
                    opisPowiadomien(s), gotowe = false
                )
            }

            Grupa("APLIKACJA", 240) {
                Wiersz(
                    IkonaPaleta, DarkTokens.ink2, "Wygląd i kolory",
                    "ciemny · zestaw „${s.motyw.label}”", gotowe = true,
                    akcja = { naPodstrone(Podstrona.WYGLAD) },
                    probki = listOf(ShiftPaletteDark.I.ink, ShiftPaletteDark.II.ink, ShiftPaletteDark.III.ink)
                )
                Wiersz(
                    IkonaDane, DarkTokens.ink2, "Dane i kopia",
                    "eksport, import, czyszczenie ręcznych zmian", gotowe = false
                )
                WierszAplikacji(u, uvm)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// KARTA CYKLU
// ─────────────────────────────────────────────────────────────

@Composable
private fun KartaCyklu(s: UiState, naKlik: () -> Unit) {
    val p by postepWejscia(Motion.RISE_MS, 40)
    Row(
        Modifier.wejscie(p).padding(horizontal = Dim.screenGutter).fillMaxWidth()
            .clip(RoundedCornerShape(Dim.rCard))
            .background(
                Brush.linearGradient(
                    listOf(Color(0x1A52D0B3), Color(0x0F7ABDFF))
                )
            )
            .border(1.dp, Color(0x4052D0B3), RoundedCornerShape(Dim.rCard))
            .clickable(onClick = naKlik)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier.size(46.dp).clip(RoundedCornerShape(15.dp))
                .background(Color(0x2952D0B3)),
            contentAlignment = Alignment.Center
        ) { Icon(IkonaCykl, null, Modifier.size(22.dp), tint = DarkTokens.accent) }

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Mój cykl", fontSize = 15.sp, fontWeight = FontWeight.Bold,
                fontFamily = Jakarta, color = DarkTokens.ink)
            Text(
                "Brygada ${s.cfg.brigade} · ${s.cfg.pattern.label}",
                fontSize = 11.sp, fontFamily = Jakarta, color = DarkTokens.ink3,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                opisWypelniania(s), fontSize = 11.sp, fontFamily = Jakarta,
                color = DarkTokens.accent, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Icon(IkonaWPrawo, null, Modifier.size(17.dp), tint = Color(0xFF8A939B))
    }
}

// ─────────────────────────────────────────────────────────────
// GRUPY I WIERSZE
// ─────────────────────────────────────────────────────────────

@Composable
private fun Grupa(tytul: String, opoznienieMs: Int, tresc: @Composable ColumnScope.() -> Unit) {
    val p by postepWejscia(Motion.RISE_MS, opoznienieMs)
    Column(
        Modifier.padding(horizontal = Dim.screenGutter),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(tytul, style = GrafikType.sectionLabel, color = DarkTokens.inkFaint,
            modifier = Modifier.wejscie(p))
        tresc()
    }
}

@Composable
private fun Wiersz(
    ikona: ImageVector,
    kolorIkony: Color,
    nazwa: String,
    podpis: String,
    gotowe: Boolean,
    kropka: Color? = null,
    probki: List<Color>? = null,
    akcja: (() -> Unit)? = null
) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(Dim.rCardSmall))
            .background(DarkTokens.surface)
            .border(1.dp, DarkTokens.line, RoundedCornerShape(Dim.rCardSmall))
            .then(if (gotowe && akcja != null) Modifier.clickable(onClick = akcja) else Modifier)
            .alpha(if (gotowe) 1f else 0.55f)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(13.dp))
                .background(kolorIkony.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) { Icon(ikona, null, Modifier.size(19.dp), tint = kolorIkony) }

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(nazwa, style = GrafikType.cardTitle, color = DarkTokens.ink)
            Text(podpis, fontSize = 11.sp, fontFamily = Jakarta, color = DarkTokens.inkMuted,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
        }

        if (kropka != null) {
            Box(Modifier.size(8.dp).clip(RoundedCornerShape(999.dp)).background(kropka))
        }
        if (probki != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                probki.forEach { k ->
                    Box(Modifier.size(12.dp).clip(RoundedCornerShape(4.dp)).background(k))
                }
            }
        }
        Icon(IkonaWPrawo, null, Modifier.size(16.dp), tint = Color(0xFF8A939B))
    }
}

// ─────────────────────────────────────────────────────────────
// O APLIKACJI I AKTUALIZACJE
// ─────────────────────────────────────────────────────────────

@Composable
private fun WierszAplikacji(u: pl.grafik.pracy.ui.UpdateUi, uvm: UpdateVm) {
    val sprawdza = u.progress is UpdateProgress.Checking
    val pobiera = u.progress as? UpdateProgress.Downloading

    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(Dim.rCardSmall))
            .background(DarkTokens.surface)
            .border(1.dp, DarkTokens.line, RoundedCornerShape(Dim.rCardSmall))
            .clickable(enabled = !sprawdza && pobiera == null) {
                if (u.available != null) uvm.install() else uvm.check(manual = true)
            }
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(13.dp))
                    .background(Color.White.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center
            ) { Icon(IkonaInfo, null, Modifier.size(19.dp), tint = DarkTokens.ink2) }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("O aplikacji", style = GrafikType.cardTitle, color = DarkTokens.ink)
                Text(
                    "wersja ${u.current} · aktualizacje z GitHuba",
                    fontSize = 11.sp, fontFamily = Jakarta, color = DarkTokens.inkMuted,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }

            PigulkaStanu(
                when {
                    sprawdza -> "sprawdzam…"
                    pobiera != null -> "pobieram…"
                    u.available != null -> "jest nowsza"
                    else -> "aktualna"
                },
                nowa = u.available != null
            )
        }

        if (pobiera != null) {
            PasekPostepu(
                if (pobiera.total > 0) pobiera.done.toFloat() / pobiera.total else 0f,
                wysokosc = 6.dp
            )
        }

        u.available?.let { info ->
            // Notatek wydania nie pokazujemy — są pisane w markdownie, a tu wyszłyby
            // z gwiazdkami i odwrotnymi apostrofami.
            val rozmiar = if (info.sizeBytes > 0) " · %.1f MB".format(info.sizeBytes / 1_048_576.0) else ""
            Text(
                "Paczka ${info.version}$rozmiar — dotknij, aby pobrać i zainstalować",
                fontSize = 11.sp, fontFamily = Jakarta, color = DarkTokens.accent,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
        }
        u.upToDateMessage?.let {
            Text(it, fontSize = 11.sp, fontFamily = Jakarta, color = DarkTokens.inkMuted)
        }
    }
}

@Composable
private fun PigulkaStanu(tekst: String, nowa: Boolean) {
    val kolor = if (nowa) ShiftPaletteDark.I.ink else DarkTokens.accent
    Box(
        Modifier.height(24.dp).clip(RoundedCornerShape(999.dp))
            .background(kolor.copy(alpha = 0.14f))
            .border(1.dp, kolor.copy(alpha = 0.3f), RoundedCornerShape(999.dp))
            .padding(horizontal = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(tekst, fontSize = 10.sp, fontWeight = FontWeight.Bold,
            fontFamily = Jakarta, color = kolor)
    }
}

// ─────────────────────────────────────────────────────────────
// PODPISY Z PRAWDZIWYCH DANYCH
// ─────────────────────────────────────────────────────────────

private fun opisWypelniania(s: UiState): String = when {
    !s.cfg.generate -> "grafik wypełniasz sam"
    s.cfg.genTo != null -> "grafik wypełnia się sam do " + miesiacDopelniaczUst(s.cfg.genTo!!)
    else -> "grafik wypełnia się sam bez końca"
}

private fun miesiacDopelniaczUst(ym: java.time.YearMonth): String =
    ym.month.getDisplayName(JavaTextStyle.FULL, PL_UST) + " " + ym.year

private fun opisOkresu(s: UiState): String {
    val okres = s.okresy.firstOrNull { it.biezacy } ?: s.okresy.firstOrNull()
    val dlugosc = when (s.okres.months) {
        1 -> "miesiąc"; 3 -> "kwartał"; 4 -> "4 miesiące"; 6 -> "pół roku"; 12 -> "rok"
        else -> "${s.okres.months} mies."
    }
    val limit = okres?.limit?.let { " · limit $it h" } ?: ""
    return "okres: $dlugosc$limit"
}

private fun opisUrlopu(s: UiState): String {
    val u = s.urlopBilans
    return "${u.bazaBiezacy} dni wymiaru · ${u.bazaZalegly} zaległych · zostało ${u.zostalo}"
}

private fun opisWykrywania(p: pl.grafik.pracy.domain.WorkPlace): String {
    if (!p.enabled) return "wyłączone"
    val wifi = if (p.ssid.isNotBlank()) " · ${p.ssid}" else ""
    val auto = if (p.autoSave) " · auto-zapis" else ""
    return "włączone · ${p.radiusM} m$wifi$auto"
}

private fun opisPowiadomien(s: UiState): String =
    if (s.remindOn) "wydarzenia: dzień wcześniej ${s.remindHour}:00"
    else "wyłączone"
