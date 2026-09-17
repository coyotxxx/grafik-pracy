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
import pl.grafik.pracy.domain.KalkulatorWyplaty
import pl.grafik.pracy.domain.SkladnikWyplaty
import pl.grafik.pracy.domain.StawkiCfg
import pl.grafik.pracy.domain.Wyplata
import pl.grafik.pracy.nowy.theme.*
import pl.grafik.pracy.nowy.ui.*
import pl.grafik.pracy.ui.UiState
import pl.grafik.pracy.ui.Vm
import java.time.YearMonth
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

private val PL_PAY = Locale.forLanguageTag("pl-PL")

/**
 * „Wypłata" — odtworzona z design/mockups/Pay.html.
 *
 * Liczy `KalkulatorWyplaty` z dni miesiąca i stawek użytkownika. Zawsze brutto:
 * kwoty do ręki wymagałyby znajomości składek i ulg, których aplikacja nie zna.
 */
@Composable
fun EkranWyplata(vm: Vm, naPowrot: () -> Unit) {
    val s by vm.state.collectAsState()
    val cfg = s.stawki
    val wyplata = remember(s.entries, s.ym, cfg) {
        KalkulatorWyplaty.policz(
            s.entries.filterKeys { YearMonth.from(it) == s.ym }.values, cfg, s.ym
        )
    }

    Box(Modifier.fillMaxSize()) {
        TloZPoswiata(Modifier.fillMaxSize())

        Column(
            Modifier.fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(top = gornaKrawedz(), bottom = 22.dp)
                .padding(horizontal = Dim.screenGutter),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            PowrotDoUstawien(naPowrot)

            Naglowek(s)
            KartaKwoty(wyplata, cfg)
            if (cfg.ustawiona) KartaSkladnikow(wyplata)
            KartaStawek(wyplata, cfg) { vm.saveStawki(it) }
            KartaCzegoNieLiczymy()
        }
    }
}

@Composable
private fun Naglowek(s: UiState) {
    val p by postepWejscia(Motion.RISE_MS)
    Row(
        Modifier.wejscie(p).fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("Wypłata", style = GrafikType.h1.copy(fontSize = 26.sp, lineHeight = 26.sp),
                color = DarkTokens.ink)
            Text(
                "szacunek z grafiku · " + s.ym.month.getDisplayName(JavaTextStyle.FULL_STANDALONE, PL_PAY)
                    .replaceFirstChar { it.uppercase(PL_PAY) }.lowercase(PL_PAY) + " ${s.ym.year}",
                fontSize = 12.sp, fontFamily = Jakarta, color = DarkTokens.inkMuted
            )
        }
        Box(
            Modifier.height(28.dp).clip(RoundedCornerShape(999.dp))
                .background(Color(0x1FDAC559))
                .border(1.dp, Color(0x4DDAC559), RoundedCornerShape(999.dp))
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("brutto", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                fontFamily = Jakarta, color = ShiftPaletteDark.I.ink)
        }
    }
}

// ─────────────────────────────────────────────────────────────
// KWOTA
// ─────────────────────────────────────────────────────────────

@Composable
private fun KartaKwoty(w: Wyplata, cfg: StawkiCfg) {
    val p by postepWejscia(Motion.RISE_MS, 50)

    Column(
        Modifier.wejscie(p).fillMaxWidth()
            .clip(RoundedCornerShape(Dim.rCard))
            .background(Color(0x12DAC559))
            .border(1.dp, Color(0x3DDAC559), RoundedCornerShape(Dim.rCard))
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (!cfg.ustawiona) {
            Text("Ustaw stawkę", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                fontFamily = Jakarta, color = DarkTokens.ink)
            Text(
                "Bez stawki zasadniczej nie ma z czego liczyć. Wpisz ją niżej — " +
                    "resztę weźmiemy z grafiku.",
                fontSize = 11.sp, lineHeight = 16.5.sp, fontFamily = Jakarta,
                color = DarkTokens.inkMuted
            )
            return@Column
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("≈", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, fontFamily = Jakarta,
                color = DarkTokens.inkMuted, modifier = Modifier.alignByBaseline())
            Text(
                KalkulatorWyplaty.zlote(w.razem),
                style = GrafikType.heroNumber.copy(fontSize = 44.sp, lineHeight = 40.sp),
                color = DarkTokens.ink, modifier = Modifier.alignByBaseline()
            )
            Text("zł", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = Jakarta,
                color = Color(0xFFD7DBDE), modifier = Modifier.alignByBaseline())
        }

        PasekSkladnikow(w)

        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Icon(IkonaInfo, null, Modifier.size(15.dp).padding(top = 1.dp), tint = DarkTokens.inkMuted)
            Text(
                "To wyliczenie z grafiku i Twoich stawek. Kwoty do ręki nie policzymy — " +
                    "aplikacja nie zna Twoich składek, ulg ani premii regulaminowej.",
                fontSize = 11.sp, lineHeight = 16.5.sp, fontFamily = Jakarta,
                color = DarkTokens.inkMuted
            )
        }
    }
}

/** Pasek udziałów: podstawa, urlop, nadgodziny, dodatek nocny, premia. */
@Composable
private fun PasekSkladnikow(w: Wyplata) {
    val razem = w.razem
    if (razem <= 0.0) return
    Row(Modifier.fillMaxWidth().height(12.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        w.skladniki.forEach { s ->
            Box(
                Modifier.weight((s.kwota / razem).toFloat().coerceAtLeast(0.01f))
                    .fillMaxHeight().clip(RoundedCornerShape(4.dp))
                    .background(kolorSkladnika(s.nazwa))
            )
        }
    }
}

private fun kolorSkladnika(nazwa: String): Color = when {
    nazwa.startsWith("Zasadnicza") -> ShiftPaletteDark.I.solid
    nazwa.startsWith("Nadgodziny") -> ShiftPaletteDark.II.solid
    nazwa.startsWith("Dodatek") -> ShiftPaletteDark.III.solid
    else -> DarkTokens.ok
}

// ─────────────────────────────────────────────────────────────
// SKŁADNIKI
// ─────────────────────────────────────────────────────────────

@Composable
private fun KartaSkladnikow(w: Wyplata) {
    val p by postepWejscia(Motion.RISE_MS, 90)

    Column(
        Modifier.wejscie(p).fillMaxWidth()
            .clip(RoundedCornerShape(Dim.rCard))
            .background(DarkTokens.surface)
            .border(1.dp, DarkTokens.line, RoundedCornerShape(Dim.rCard))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Z czego się składa", style = GrafikType.cardTitle, color = DarkTokens.ink)

        w.skladniki.forEach { s -> WierszSkladnika(s) }

        Box(Modifier.fillMaxWidth().height(1.dp).background(DarkTokens.line))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Razem brutto", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                fontFamily = Jakarta, color = DarkTokens.ink, modifier = Modifier.weight(1f))
            Text(
                "${KalkulatorWyplaty.zlote(w.razem)} zł",
                style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold,
                    fontFamily = Jakarta, fontFeatureSettings = TNUM),
                color = DarkTokens.ink
            )
        }
    }
}

@Composable
private fun WierszSkladnika(s: SkladnikWyplaty) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier.size(9.dp).clip(RoundedCornerShape(3.dp))
                .background(kolorSkladnika(s.nazwa))
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(s.nazwa, fontSize = 13.sp, fontFamily = Jakarta, color = DarkTokens.inkStrong,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (s.nazwa.startsWith("Zasadnicza")) "${s.godziny} h planu · ${KalkulatorWyplaty.zlote(s.stawka)} zł/h"
                else "${s.godziny} h × ${KalkulatorWyplaty.zlote(s.stawka)} zł",
                style = TextStyle(fontSize = 10.sp, fontFamily = Jakarta, fontFeatureSettings = TNUM),
                color = DarkTokens.inkFaint
            )
        }
        Text(
            KalkulatorWyplaty.zlote(s.kwota),
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                fontFamily = Jakarta, fontFeatureSettings = TNUM),
            color = kolorSkladnika(s.nazwa)
        )
    }
}

// ─────────────────────────────────────────────────────────────
// STAWKI
// ─────────────────────────────────────────────────────────────

@Composable
private fun KartaStawek(w: Wyplata, cfg: StawkiCfg, zapisz: (StawkiCfg) -> Unit) {
    val p by postepWejscia(Motion.RISE_MS, 130)

    Column(
        Modifier.wejscie(p).fillMaxWidth()
            .clip(RoundedCornerShape(Dim.rCard))
            .background(DarkTokens.surface)
            .border(1.dp, DarkTokens.line, RoundedCornerShape(Dim.rCard))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("TWOJE STAWKI", style = GrafikType.sectionLabel, color = DarkTokens.inkFaint)

        PoleStawki(
            "Wynagrodzenie zasadnicze", "brutto za miesiąc, z umowy", cfg.zasadnicza
        ) { zapisz(cfg.copy(zasadnicza = it)) }

        if (cfg.ustawiona && w.normaMiesiaca > 0) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp))
                    .background(Color(0x14DAC559))
                    .border(1.dp, Color(0x3DDAC559), RoundedCornerShape(13.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Stawka za godzinę w tym miesiącu: " +
                        "${KalkulatorWyplaty.zlote(w.stawkaGodzinowa)} zł " +
                        "(zasadnicza ÷ ${w.normaMiesiaca} h planu)",
                    fontSize = 11.sp, lineHeight = 16.5.sp, fontFamily = Jakarta,
                    color = ShiftPaletteDark.I.ink
                )
            }
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(DarkTokens.line))

        PoleStawki(
            "Dodatek za nocki", "kwota za godzinę III zmiany, z regulaminu", cfg.dodatekNocny
        ) { zapisz(cfg.copy(dodatekNocny = it)) }

        Box(Modifier.fillMaxWidth().height(1.dp).background(DarkTokens.line))

        Row(
            Modifier.fillMaxWidth().clickable {
                zapisz(cfg.copy(pokazujWBilansie = !cfg.pokazujWBilansie))
            },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Pokazuj wypłatę w Bilansie", style = GrafikType.cardTitle, color = DarkTokens.ink)
                Text("jeśli wolisz, żeby kwoty nie świeciły się na wierzchu",
                    fontSize = 11.sp, fontFamily = Jakarta, color = DarkTokens.inkMuted)
            }
            Przelacznik(cfg.pokazujWBilansie) { zapisz(cfg.copy(pokazujWBilansie = it)) }
        }
    }
}

/** Czego świadomie nie liczymy — żeby różnica wobec paska była zrozumiała. */
@Composable
private fun KartaCzegoNieLiczymy() {
    val p by postepWejscia(Motion.RISE_MS, 170)
    Column(
        Modifier.wejscie(p).fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x08FFFFFF))
            .border(1.dp, DarkTokens.line, RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("CZEGO NIE LICZYMY", style = GrafikType.sectionLabel, color = DarkTokens.inkFaint)
        listOf(
            "Premii — jest uznaniowa, raz jest, raz jej nie ma.",
            "Dodatku urlopowego i nadgodzin ze średniej — liczą się z poprzednich miesięcy.",
            "Potrąceń: składek, zaliczki na podatek, PZU, kasy zapomogowej."
        ).forEach { zdanie ->
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Box(
                    Modifier.padding(top = 6.dp).size(5.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(DarkTokens.inkDisabled)
                )
                Text(zdanie, fontSize = 12.sp, lineHeight = 18.sp,
                    fontFamily = Jakarta, color = DarkTokens.ink3)
            }
        }
        Text(
            "Dlatego kwota z aplikacji jest niższa niż suma naliczeń na pasku — " +
                "pokazujemy tylko to, co da się policzyć z grafiku i umowy.",
            fontSize = 11.sp, lineHeight = 16.5.sp, fontFamily = Jakarta,
            color = DarkTokens.inkMuted
        )
    }
}


/**
 * Kwota wpisywana z klawiatury. Makieta ma tu stepper, ale wbicie 7 950 zł skokiem
 * po 50 gr nie miałoby końca — pensję wpisuje się raz i na lata.
 */
@Composable
private fun PoleStawki(
    tytul: String,
    podpis: String,
    wartosc: Double,
    naZmiane: (Double) -> Unit
) {
    // Klucz remembera to tytuł pola, nie zapisywana wartość — opóźniona emisja
    // z DataStore przestawiałaby cyfry w trakcie pisania.
    var wpis by remember(tytul) {
        mutableStateOf(if (wartosc > 0.0) KalkulatorWyplaty.zlote(wartosc) else "")
    }

    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(tytul, style = GrafikType.cardTitle, color = DarkTokens.ink)
            Text(podpis, fontSize = 11.sp, fontFamily = Jakarta, color = DarkTokens.inkMuted,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PoleTekstowe(
                wartosc = wpis,
                podpowiedz = "0,00",
                cyfry = true,
                modifier = Modifier.width(104.dp),
                wyrownanie = TextAlign.End,
                naZmiane = { tekst ->
                    wpis = przyjmijKwote(tekst, wpis).also {
                        naZmiane(it.replace(',', '.').toDoubleOrNull() ?: 0.0)
                    }
                }
            )
            Text("zł", fontSize = 13.sp, fontFamily = Jakarta, color = DarkTokens.inkMuted)
        }
    }
}

/**
 * Wpis kwoty: najwyżej pięć cyfr złotych i dwie groszy. Pensja miesięczna ma cztery
 * cyfry, dodatek za godzinę dwie — jedno pole obsługuje oba.
 */
private fun przyjmijKwote(tekst: String, poprzedni: String): String {
    val czyste = tekst.replace('.', ',').filter { it.isDigit() || it == ',' }
    val czesci = czyste.split(",")
    if (czesci.size > 2) return poprzedni
    if (czesci[0].length > 5) return poprzedni
    val grosze = czesci.getOrNull(1)?.take(2)
    return if (grosze == null) czesci[0] else "${czesci[0]},$grosze"
}
