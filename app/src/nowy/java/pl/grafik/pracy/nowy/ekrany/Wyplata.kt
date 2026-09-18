package pl.grafik.pracy.nowy.ekrany

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import pl.grafik.pracy.data.AppDb
import pl.grafik.pracy.data.Odcinki
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
    val wyplata = remember(s.entries, s.ym, cfg, s.ot100Okresu, s.ostatniMiesiacOkresu) {
        KalkulatorWyplaty.policz(
            s.entries.filterKeys { YearMonth.from(it) == s.ym }.values, cfg, s.ym,
            s.ot100Okresu, s.ostatniMiesiacOkresu
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
            if (cfg.ustawiona) KartaCzekajacych(wyplata, s)
            KartaStawek(wyplata, cfg) { vm.saveStawki(it) }
            KartaOdcinkow(s.ym)
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
                color = Tokeny.ink)
            Text(
                "szacunek z grafiku · " + s.ym.month.getDisplayName(JavaTextStyle.FULL_STANDALONE, PL_PAY)
                    .replaceFirstChar { it.uppercase(PL_PAY) }.lowercase(PL_PAY) + " ${s.ym.year}",
                fontSize = 12.sp, fontFamily = Jakarta, color = Tokeny.inkMuted
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
                fontFamily = Jakarta, color = Paleta.I.ink)
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
                fontFamily = Jakarta, color = Tokeny.ink)
            Text(
                "Bez stawki zasadniczej nie ma z czego liczyć. Wpisz ją niżej — " +
                    "resztę weźmiemy z grafiku.",
                fontSize = 11.sp, lineHeight = 16.5.sp, fontFamily = Jakarta,
                color = Tokeny.inkMuted
            )
            return@Column
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("≈", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, fontFamily = Jakarta,
                color = Tokeny.inkMuted, modifier = Modifier.alignByBaseline())
            Text(
                KalkulatorWyplaty.zlote(w.razem),
                style = GrafikType.heroNumber.copy(fontSize = 44.sp, lineHeight = 40.sp),
                color = Tokeny.ink, modifier = Modifier.alignByBaseline()
            )
            Text("zł", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = Jakarta,
                color = Tokeny.inkNaKarcie, modifier = Modifier.alignByBaseline())
        }

        PasekSkladnikow(w)

        val n = KalkulatorWyplaty.netto(w.razem, cfg)
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x24DAC559)))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Na rękę", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    fontFamily = Jakarta, color = Tokeny.ink)
                Text(
                    "po składkach i zaliczce" +
                        (if (cfg.stalePotracenia > 0) " oraz stałych potrąceniach" else ""),
                    fontSize = 10.sp, fontFamily = Jakarta, color = Tokeny.inkMuted
                )
            }
            Text(
                "≈ ${KalkulatorWyplaty.zlote(n.naReke)} zł",
                style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    fontFamily = Jakarta, fontFeatureSettings = TNUM),
                color = Tokeny.accent
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                "składki" to n.spoleczne,
                "zdrowotna" to n.zdrowotna,
                "zaliczka" to n.zaliczka
            ).forEach { (nazwa, kwota) ->
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text("− ${KalkulatorWyplaty.zlote(kwota)}",
                        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                            fontFamily = Jakarta, fontFeatureSettings = TNUM),
                        color = Tokeny.ink3)
                    Text(nazwa, fontSize = 9.sp, fontFamily = Jakarta, color = Tokeny.inkFaint)
                }
            }
            if (cfg.stalePotracenia > 0) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text("− ${KalkulatorWyplaty.zlote(n.potracenia)}",
                        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                            fontFamily = Jakarta, fontFeatureSettings = TNUM),
                        color = Tokeny.ink3)
                    Text("potrącenia", fontSize = 9.sp, fontFamily = Jakarta,
                        color = Tokeny.inkFaint)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Icon(IkonaInfo, null, Modifier.size(15.dp).padding(top = 1.dp), tint = Tokeny.inkMuted)
            Text(
                "Szacunek z grafiku i Twoich stawek. Na rękę liczymy ze składek ustawowych, " +
                    "kosztów i ulgi — bez premii i bez świadczeń doliczanych przez zakład, " +
                    "więc na odcinku kwota bywa o około procent inna.",
                fontSize = 11.sp, lineHeight = 16.5.sp, fontFamily = Jakarta,
                color = Tokeny.inkMuted
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

@Composable
private fun kolorSkladnika(nazwa: String): Color = when {
    nazwa.startsWith("Zasadnicza") -> Paleta.I.solid
    nazwa.startsWith("Nadgodziny") -> Paleta.II.solid
    nazwa.startsWith("Dodatek") -> Paleta.III.solid
    else -> Tokeny.ok
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
            .background(Tokeny.surface)
            .border(1.dp, Tokeny.line, RoundedCornerShape(Dim.rCard))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Z czego się składa", style = GrafikType.cardTitle, color = Tokeny.ink)

        w.skladniki.forEach { s -> WierszSkladnika(s) }

        Box(Modifier.fillMaxWidth().height(1.dp).background(Tokeny.line))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Razem brutto", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                fontFamily = Jakarta, color = Tokeny.ink, modifier = Modifier.weight(1f))
            Text(
                "${KalkulatorWyplaty.zlote(w.razem)} zł",
                style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold,
                    fontFamily = Jakarta, fontFeatureSettings = TNUM),
                color = Tokeny.ink
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
            Text(s.nazwa, fontSize = 13.sp, fontFamily = Jakarta, color = Tokeny.inkStrong,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (s.nazwa.startsWith("Zasadnicza")) "${s.godziny} h planu · ${KalkulatorWyplaty.zlote(s.stawka)} zł/h"
                else "${s.godziny} h × ${KalkulatorWyplaty.zlote(s.stawka)} zł",
                style = TextStyle(fontSize = 10.sp, fontFamily = Jakarta, fontFeatureSettings = TNUM),
                color = Tokeny.inkFaint
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
            .background(Tokeny.surface)
            .border(1.dp, Tokeny.line, RoundedCornerShape(Dim.rCard))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("TWOJE STAWKI", style = GrafikType.sectionLabel, color = Tokeny.inkFaint)

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
                    color = Paleta.I.ink
                )
            }
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(Tokeny.line))

        PoleStawki(
            "Dodatek za nocki", "kwota za godzinę III zmiany, z regulaminu", cfg.dodatekNocny
        ) { zapisz(cfg.copy(dodatekNocny = it)) }

        Box(Modifier.fillMaxWidth().height(1.dp).background(Tokeny.line))

        Text("DO KWOTY NA RĘKĘ", style = GrafikType.sectionLabel, color = Tokeny.inkFaint)

        PoleStawki(
            "Koszty uzyskania", "250 zł podstawowe, 300 zł przy dojazdach", cfg.kosztyUzyskania
        ) { zapisz(cfg.copy(kosztyUzyskania = it)) }

        PoleStawki(
            "Ulga podatkowa", "300 zł miesięcznie, gdy złożyłeś PIT-2", cfg.ulgaPodatkowa
        ) { zapisz(cfg.copy(ulgaPodatkowa = it)) }

        PoleStawki(
            "Stałe potrącenia", "opieka medyczna, PZU, kasa zapomogowa, związki",
            cfg.stalePotracenia
        ) { zapisz(cfg.copy(stalePotracenia = it)) }

        Box(Modifier.fillMaxWidth().height(1.dp).background(Tokeny.line))

        Row(
            Modifier.fillMaxWidth().clickable {
                zapisz(cfg.copy(pokazujWBilansie = !cfg.pokazujWBilansie))
            },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Pokazuj wypłatę w Bilansie", style = GrafikType.cardTitle, color = Tokeny.ink)
                Text("jeśli wolisz, żeby kwoty nie świeciły się na wierzchu",
                    fontSize = 11.sp, fontFamily = Jakarta, color = Tokeny.inkMuted)
            }
            Przelacznik(cfg.pokazujWBilansie) { zapisz(cfg.copy(pokazujWBilansie = it)) }
        }
    }
}

/**
 * Nadgodziny po 100 % czekające na zamknięcie okresu. Zakład wypłaca je zbiorczo
 * w wypłacie za ostatni miesiąc kwartału, więc w pozostałych miesiącach pokazujemy
 * je osobno — żeby nie wyglądało, że przepadły.
 */
@Composable
private fun KartaCzekajacych(w: Wyplata, s: UiState) {
    val czekaja = w.nadgodziny100Czekaja
    if (czekaja.godziny <= 0) return
    val p by postepWejscia(Motion.RISE_MS, 110)
    val okres = s.okresy.firstOrNull { it.biezacy }

    Row(
        Modifier.wejscie(p).fillMaxWidth()
            .clip(RoundedCornerShape(Dim.rCard))
            .background(Color(0x127ABDFF))
            .border(1.dp, Color(0x387ABDFF), RoundedCornerShape(Dim.rCard))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier.size(42.dp).clip(RoundedCornerShape(14.dp))
                .background(Color(0x297ABDFF)),
            contentAlignment = Alignment.Center
        ) { Icon(IkonaCzasPracy, null, Modifier.size(20.dp), tint = Paleta.III.ink) }

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Czeka na koniec okresu", fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    fontFamily = Jakarta, color = Tokeny.ink,
                    modifier = Modifier.weight(1f).alignByBaseline())
                Text(
                    "${KalkulatorWyplaty.zlote(czekaja.kwota)} zł",
                    style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold,
                        fontFamily = Jakarta, fontFeatureSettings = TNUM),
                    color = Paleta.III.ink, modifier = Modifier.alignByBaseline()
                )
            }
            Text(
                "${czekaja.godziny} h po 100 % za dni wolne, w które przyszedłeś do pracy. " +
                    "Zakład wypłaca je zbiorczo" +
                    (okres?.let { " w wypłacie za ${ostatniMiesiacOkresu(it)}" } ?: "") + ".",
                fontSize = 11.sp, lineHeight = 16.5.sp, fontFamily = Jakarta,
                color = Tokeny.ink3
            )
        }
    }
}

/** „wrzesień", nie „września" — po przyimku „za" idzie biernik. */
private fun ostatniMiesiacOkresu(o: pl.grafik.pracy.domain.PeriodStats): String =
    o.period.to.month.getDisplayName(JavaTextStyle.FULL_STANDALONE, PL_PAY).lowercase(PL_PAY)

/**
 * Archiwum odcinków. Plik kopiujemy do pamięci aplikacji — zostaje nawet wtedy, gdy
 * oryginał zniknie z telefonu, i nie wychodzi nigdzie poza urządzenie.
 */
@Composable
private fun KartaOdcinkow(ym: YearMonth) {
    val ctx = LocalContext.current
    val zakres = rememberCoroutineScope()
    val p by postepWejscia(Motion.RISE_MS, 150)
    val odcinki by remember { AppDb.get(ctx).payslipDao().observeAll() }
        .collectAsState(initial = emptyList())
    // Odcinek przychodzi zwykle za miesiąc wstecz, a ekran pokazuje miesiąc z Grafiku —
    // dlatego karta ma własny wybór, żeby nie trzeba było przestawiać kalendarza.
    var miesiac by remember(ym) { mutableStateOf(ym) }
    val tegoMiesiaca = odcinki.firstOrNull { it.ym == miesiac.toString() }
    var blad by remember { mutableStateOf<String?>(null) }

    val wybierz = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        zakres.launch {
            val wynik = Odcinki.dodaj(ctx, uri, miesiac)
            blad = if (wynik.isFailure) "Nie udało się wczytać pliku." else null
        }
    }

    Column(
        Modifier.wejscie(p).fillMaxWidth()
            .clip(RoundedCornerShape(Dim.rCard))
            .background(Tokeny.surface)
            .border(1.dp, Tokeny.line, RoundedCornerShape(Dim.rCard))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text("ODCINKI", style = GrafikType.sectionLabel,
                color = Tokeny.inkFaint, modifier = Modifier.weight(1f))
            Text(
                if (odcinki.isEmpty()) "nic nie wgrano" else ileZapisanych(odcinki.size),
                fontSize = 10.sp, fontFamily = Jakarta, color = Tokeny.inkFaint
            )
        }
        Text(
            "Wgraj kartkę od wypłaty — plik PDF albo zdjęcie. Zostanie w aplikacji " +
                "i będzie pod ręką, gdybyś chciał sprawdzić, jak zakład policzył dany miesiąc.",
            fontSize = 11.sp, lineHeight = 16.5.sp, fontFamily = Jakarta,
            color = Tokeny.inkMuted
        )

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PrzyciskKwadrat(IkonaWLewo, "Wcześniejszy miesiąc", rozmiar = 34.dp) {
                miesiac = miesiac.minusMonths(1)
            }
            Text(
                miesiacPay(miesiac).replaceFirstChar { it.uppercase(PL_PAY) },
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                fontFamily = Jakarta, color = Tokeny.ink
            )
            PrzyciskKwadrat(
                IkonaWPrawo, "Późniejszy miesiąc", rozmiar = 34.dp,
                kolorIkony = if (miesiac < ym) Tokeny.ink2 else Tokeny.inkDisabled
            ) {
                if (miesiac < ym) miesiac = miesiac.plusMonths(1)
            }
        }

        Row(
            Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(14.dp))
                .background(Tokeny.surface)
                .border(1.dp, Tokeny.lineSoft, RoundedCornerShape(14.dp))
                .clickable { wybierz.launch(arrayOf("application/pdf", "image/*")) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            Icon(IkonaPlus, null, Modifier.size(15.dp), tint = Tokeny.ink2)
            Text(
                if (tegoMiesiaca == null) "Dodaj odcinek za ${miesiacPay(miesiac)}"
                else "Zastąp odcinek za ${miesiacPay(miesiac)}",
                fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                fontFamily = Jakarta, color = Tokeny.ink2
            )
        }

        blad?.let {
            Text(it, fontSize = 11.sp, fontFamily = Jakarta, color = Tokeny.warnInk)
        }

        odcinki.forEach { row -> WierszOdcinka(row, ctx, zakres) }
    }
}

@Composable
private fun WierszOdcinka(
    row: pl.grafik.pracy.data.PayslipRow,
    ctx: android.content.Context,
    zakres: kotlinx.coroutines.CoroutineScope
) {
    val ym = remember(row.ym) { runCatching { YearMonth.parse(row.ym) }.getOrNull() }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp))
            .background(Tokeny.surfaceInput)
            .border(1.dp, Tokeny.lineInput, RoundedCornerShape(13.dp))
            .clickable {
                Odcinki.intencjaOtwarcia(ctx, row)?.let { runCatching { ctx.startActivity(it) } }
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(11.dp))
                .background(Color(0x1FDAC559)),
            contentAlignment = Alignment.Center
        ) { Icon(IkonaWyplata, null, Modifier.size(16.dp), tint = Paleta.I.ink) }

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                ym?.let { miesiacPay(it) } ?: row.ym,
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = Jakarta,
                color = Tokeny.ink, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(row.originalName, fontSize = 10.sp, fontFamily = Jakarta,
                color = Tokeny.inkFaint, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        Box(
            Modifier.size(32.dp).clip(RoundedCornerShape(10.dp))
                .clickable { zakres.launch { Odcinki.usun(ctx, row) } },
            contentAlignment = Alignment.Center
        ) { Icon(IkonaKosz, "Usuń odcinek", Modifier.size(15.dp), tint = Tokeny.inkMuted) }
    }
}

/** „1 zapisany", „3 zapisane", „5 zapisanych" — polska odmiana. */
private fun ileZapisanych(ile: Int): String = when {
    ile == 1 -> "1 zapisany"
    ile % 10 in 2..4 && ile % 100 !in 12..14 -> "$ile zapisane"
    else -> "$ile zapisanych"
}

private fun miesiacPay(ym: YearMonth): String =
    ym.month.getDisplayName(JavaTextStyle.FULL_STANDALONE, PL_PAY)
        .replaceFirstChar { it.uppercase(PL_PAY) } + " " + ym.year

/** Czego świadomie nie liczymy — żeby różnica wobec paska była zrozumiała. */
@Composable
private fun KartaCzegoNieLiczymy() {
    val p by postepWejscia(Motion.RISE_MS, 170)
    Column(
        Modifier.wejscie(p).fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x08FFFFFF))
            .border(1.dp, Tokeny.line, RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("CZEGO NIE LICZYMY", style = GrafikType.sectionLabel, color = Tokeny.inkFaint)
        listOf(
            "Premii — jest uznaniowa, raz jest, raz jej nie ma.",
            "Dodatku urlopowego i nadgodzin ze średniej — liczą się z poprzednich miesięcy.",
            "Świadczeń doliczanych przez zakład — opieka medyczna czy kafeteria " +
                "podnoszą podstawę składek."
        ).forEach { zdanie ->
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Box(
                    Modifier.padding(top = 6.dp).size(5.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Tokeny.inkDisabled)
                )
                Text(zdanie, fontSize = 12.sp, lineHeight = 18.sp,
                    fontFamily = Jakarta, color = Tokeny.ink3)
            }
        }
        Text(
            "Dlatego kwota z aplikacji jest niższa niż suma naliczeń na pasku — " +
                "pokazujemy tylko to, co da się policzyć z grafiku i umowy.",
            fontSize = 11.sp, lineHeight = 16.5.sp, fontFamily = Jakarta,
            color = Tokeny.inkMuted
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
            Text(tytul, style = GrafikType.cardTitle, color = Tokeny.ink)
            Text(podpis, fontSize = 11.sp, fontFamily = Jakarta, color = Tokeny.inkMuted,
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
            Text("zł", fontSize = 13.sp, fontFamily = Jakarta, color = Tokeny.inkMuted)
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
