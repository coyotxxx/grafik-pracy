package pl.grafik.pracy.nowy.ekrany

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.grafik.pracy.domain.OtRate
import androidx.compose.ui.platform.LocalContext
import pl.grafik.pracy.data.AppDb
import pl.grafik.pracy.data.Odcinki
import pl.grafik.pracy.domain.Holidays
import pl.grafik.pracy.domain.Shift
import pl.grafik.pracy.nowy.theme.*
import pl.grafik.pracy.nowy.ui.*
import pl.grafik.pracy.ui.UiState
import pl.grafik.pracy.ui.Vm
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

private val PL = Locale.forLanguageTag("pl-PL")

/**
 * Ekran „Grafik" — odtworzony z design/mockups/Main.html.
 * Wszystkie wartości pochodzą z makiety; dane z tego samego modelu widoku,
 * którego używa klasyczna aplikacja.
 */
@Composable
fun EkranGrafik(
    vm: Vm,
    naDzien: (LocalDate) -> Unit,
    naEdycje: () -> Unit,
    naOdpoczynek: () -> Unit,
    naWyplate: () -> Unit
) {
    val s by vm.state.collectAsState()
    val dzis = remember { LocalDate.now() }
    var wybrany by remember(s.ym) { mutableStateOf(if (YearMonth.from(dzis) == s.ym) dzis else s.ym.atDay(1)) }

    Box(Modifier.fillMaxSize()) {
        TloZPoswiata(Modifier.fillMaxSize())

        // Makieta jest rysowana na 900 dp wysokości, a telefon ma około 730 dp —
        // bez przewijania pasek wybranego dnia i przyciski wypadają poza ekran.
        Column(
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = gornaKrawedz(), bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Naglowek(s, naWyplate)
            KartaGodzin(s)
            PasekOdpoczynku(s, naOdpoczynek)
            SiatkaMiesiaca(
                s, dzis, wybrany,
                naPoprzedni = { vm.prevMonth() },
                naNastepny = { vm.nextMonth() }
            ) { wybrany = it }
            PasekWybranego(s, wybrany) { naDzien(wybrany) }
            RzadPrzyciskow(naEdycje)
        }
    }
}

@Composable
private fun Naglowek(s: UiState, naWyplate: () -> Unit) {
    val p by postepWejscia(Motion.RISE_MS)
    Row(
        Modifier.wejscie(p).padding(horizontal = Dim.screenGutter).fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row {
                Text(
                    s.ym.month.getDisplayName(JavaTextStyle.FULL_STANDALONE, PL)
                        .replaceFirstChar { it.uppercase() } + " ",
                    style = GrafikType.h1, color = Tokeny.ink
                )
                Text("${s.ym.year}", style = GrafikType.h1, color = Tokeny.inkFaint)
            }
            Text(
                "Brygada ${s.cfg.brigade} · ${s.cfg.pattern.label}",
                style = GrafikType.caption.copy(fontSize = 11.sp),
                color = Tokeny.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        // Strzałek nie ma — miesiąc zmienia się przesunięciem palca po kalendarzu.
        // W ich miejscu skrót do odcinka wypłaty za oglądany miesiąc.
        SkrotDoOdcinka(s.ym, naWyplate)
    }
}

/**
 * Skrót do odcinka wypłaty za oglądany miesiąc.
 *
 * Gdy odcinek jest wgrany — dotknięcie otwiera go w czytniku. Gdy go nie ma, ikona
 * jest przygaszona i prowadzi na ekran wypłaty, gdzie można wgrać kartkę; miesiąc
 * jest tam już ustawiony na ten, który właśnie oglądasz.
 */
@Composable
private fun SkrotDoOdcinka(ym: YearMonth, naWyplate: () -> Unit) {
    val ctx = LocalContext.current
    val odcinki by remember { AppDb.get(ctx).payslipDao().observeAll() }
        .collectAsState(initial = emptyList())
    val odcinek = odcinki.firstOrNull { it.ym == ym.toString() }

    Box(
        Modifier.size(40.dp).clip(RoundedCornerShape(13.dp))
            .background(if (odcinek != null) Color(0x1FDAC559) else Tokeny.surface)
            .border(
                1.dp,
                if (odcinek != null) Color(0x47DAC559) else Tokeny.lineStrong,
                RoundedCornerShape(13.dp)
            )
            .clickable {
                if (odcinek != null) {
                    Odcinki.intencjaOtwarcia(ctx, odcinek)
                        ?.let { runCatching { ctx.startActivity(it) } }
                } else naWyplate()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            IkonaWyplata,
            if (odcinek != null) "Otwórz odcinek wypłaty" else "Wgraj odcinek wypłaty",
            Modifier.size(19.dp),
            tint = if (odcinek != null) Paleta.I.ink else Tokeny.inkDisabled
        )
    }
}

@Composable
private fun PrzyciskIkonowy(ikona: androidx.compose.ui.graphics.vector.ImageVector, opis: String, akcja: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(RoundedCornerShape(13.dp))
            .background(Tokeny.surface)
            .border(1.dp, Tokeny.lineStrong, RoundedCornerShape(13.dp))
            .clickable(onClick = akcja),
        contentAlignment = Alignment.Center
    ) { Icon(ikona, opis, Modifier.size(17.dp), tint = Tokeny.ink2) }
}

@Composable
private fun KartaGodzin(s: UiState) {
    val p by postepWejscia(Motion.RISE_MS)
    val doDzis = if (s.stats.biezacyMiesiac) s.stats.doDzis else s.stats.rozliczone
    val licznik = licznikDo(doDzis)

    Karta(Modifier.wejscie(p).padding(horizontal = Dim.screenGutter)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("$licznik", style = GrafikType.counter, color = Tokeny.ink)
                Text("/ ${s.stats.norm} h", fontSize = 13.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    fontFamily = Jakarta, color = Tokeny.inkMuted)
            }
            Text(if (s.stats.biezacyMiesiac) "do dziś" else "razem",
                style = GrafikType.caption, color = Tokeny.inkMuted)
        }
        Spacer(Modifier.height(10.dp))
        PasekPostepu(if (s.stats.norm > 0) doDzis.toFloat() / s.stats.norm else 0f)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val bilans = s.stats.diff
            Text(
                "cały miesiąc ${s.stats.rozliczone} h · bilans " +
                    (if (bilans > 0) "+$bilans" else "$bilans") + " h",
                style = GrafikType.caption, color = Tokeny.inkMuted
            )
            if (s.stats.urlopH > 0) {
                Text("urlop ${s.stats.urlopH} h", style = GrafikType.caption, color = Tokeny.inkMuted)
            }
        }
    }
}

/**
 * Pasek kolizji odpoczynku z makiety — pojawia się tylko wtedy, gdy w miesiącu
 * jest przerwa krótsza niż 11 h (art. 132 KP). Prowadzi do ekranu „Odpoczynek".
 */
@Composable
private fun PasekOdpoczynku(s: UiState, naOdpoczynek: () -> Unit) {
    val pierwsza = s.kolizje.firstOrNull() ?: return
    Row(
        Modifier.padding(horizontal = Dim.screenGutter).fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(Tokeny.warnBg)
            .border(1.dp, Tokeny.warnLine, RoundedCornerShape(15.dp))
            .clickable(onClick = naOdpoczynek)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(IkonaOstrzezenie, null, Modifier.size(16.dp), tint = Tokeny.warnInk)
        Text(
            "%02d.%02d — tylko %d h przerwy między zmianami".format(
                pierwsza.date.dayOfMonth, pierwsza.date.monthValue, pierwsza.przerwaH
            ) + if (s.kolizje.size > 1) " (+${s.kolizje.size - 1})" else "",
            style = GrafikType.caption, color = Tokeny.warnInk2,
            modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        Icon(IkonaWPrawo, null, Modifier.size(15.dp), tint = Tokeny.warnInk2)
    }
}

private val DNI_TYGODNIA = listOf("PN", "WT", "ŚR", "CZ", "PT", "SO", "ND")

@Composable
private fun SiatkaMiesiaca(
    s: UiState,
    dzis: LocalDate,
    wybrany: LocalDate,
    naPoprzedni: () -> Unit,
    naNastepny: () -> Unit,
    naWybor: (LocalDate) -> Unit
) {
    val pNag by postepWejscia(Motion.RISE_MS, 100)
    // Jedna animacja na całą siatkę; kaskadę liczymy per kafelek z jej postępu.
    val calosc = Motion.CELL_START_MS + 41 * Motion.CELL_STAGGER_MS + Motion.CELL_IN_MS
    val pSiatki by postepWejscia(calosc)
    val dniZKolizja = remember(s.kolizje) { s.kolizje.map { it.date }.toSet() }

    val pierwszy = s.ym.atDay(1)
    val start = pierwszy.minusDays((pierwszy.dayOfWeek.value - 1).toLong())
    val tygodnie = 6

    // Przesunięcie palcem zmienia miesiąc — w lewo następny, w prawo poprzedni.
    // Próg 60 dp, żeby lekkie drgnięcie przy dotykaniu dnia nie przewijało kalendarza.
    val prog = with(LocalDensity.current) { 60.dp.toPx() }
    var przesuniecie by remember(s.ym) { mutableFloatStateOf(0f) }

    Column(
        Modifier.padding(horizontal = Dim.screenGutter)
            .pointerInput(s.ym) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        when {
                            przesuniecie <= -prog -> naNastepny()
                            przesuniecie >= prog -> naPoprzedni()
                        }
                        przesuniecie = 0f
                    },
                    onDragCancel = { przesuniecie = 0f }
                ) { _, delta -> przesuniecie += delta }
            },
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(Modifier.wejscie(pNag).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            DNI_TYGODNIA.forEachIndexed { i, d ->
                Text(
                    d, Modifier.weight(1f), textAlign = TextAlign.Center,
                    fontSize = 10.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    letterSpacing = 0.8.sp, fontFamily = Jakarta,
                    color = if (i >= 5) Tokeny.inkFaint else Tokeny.inkMuted
                )
            }
        }
        repeat(tygodnie) { w ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(7) { k ->
                    val i = w * 7 + k
                    val d = start.plusDays(i.toLong())
                    KafelekDnia(
                        data = d,
                        e = s.entries[d],
                        godziny = s.obecnosc[d],
                        poza = YearMonth.from(d) != s.ym,
                        dzisiaj = d == dzis,
                        zaznaczony = d == wybrany,
                        maWydarzenie = s.events[d].orEmpty().isNotEmpty(),
                        maNotatke = !s.entries[d]?.note.isNullOrBlank(),
                        kolizja = s.restZnacznik && d in dniZKolizja,
                        postepSiatki = pSiatki,
                        indeks = i,
                        calosc = calosc,
                        modifier = Modifier.weight(1f)
                    ) { naWybor(d) }
                }
            }
        }
    }
}

@Composable
private fun KafelekDnia(
    data: LocalDate,
    e: pl.grafik.pracy.domain.DayEntry?,
    godziny: pl.grafik.pracy.ui.DayPresence?,
    poza: Boolean,
    dzisiaj: Boolean,
    zaznaczony: Boolean,
    maWydarzenie: Boolean,
    maNotatke: Boolean,
    kolizja: Boolean,
    postepSiatki: Float,
    indeks: Int,
    calosc: Int,
    modifier: Modifier = Modifier,
    naKlik: () -> Unit
) {
    // Kaskada z makiety: start 160 ms, 16 ms na kafelek, każdy wchodzi przez 500 ms.
    val odMs = Motion.CELL_START_MS + indeks * Motion.CELL_STAGGER_MS
    val lokalny = (((postepSiatki * calosc) - odMs) / Motion.CELL_IN_MS).coerceIn(0f, 1f)

    val kolory = Paleta.of(typDniaZ(e?.shift))
    // Dzień ustawowo wolny wyróżnia się całym kafelkiem, nie samym numerem —
    // w siatce trzydziestu dni pojedyncza czerwona cyfra ginie.
    val swieto = remember(data) { Holidays.isHoliday(data) }
    // Kolor czytamy przed rysowaniem — w `drawBehind` nie ma już kontekstu kompozycji.
    val swiatecznaBarwa = Tokeny.swiateczny
    val tlo = if (poza) Color.Transparent else kolory.fill
    val obrys = if (poza) Paleta.POZA.line else kolory.line
    val atrament = if (poza) Tokeny.inkDisabled else kolory.ink
    val numer = when {
        poza -> Tokeny.inkDisabled
        swieto -> Tokeny.swiateczny
        e?.shift?.isWork != true -> Tokeny.inkMuted
        else -> Tokeny.inkStrong
    }

    val pulsGramy = animacjeWlaczone() && dzisiaj
    val puls = rememberInfiniteTransition(label = "dzis")
    val mocPulsu by if (pulsGramy) puls.animateFloat(
        0.9f, 0.55f,
        infiniteRepeatable(tween(Motion.TODAY_PULSE_MS / 2, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "puls"
    ) else remember { mutableFloatStateOf(0.9f) }

    val pierscien = when {
        zaznaczony -> Tokeny.accent
        dzisiaj -> Tokeny.accent.copy(alpha = mocPulsu)
        else -> null
    }

    Box(
        modifier.height(Dim.dayCell)
            .alpha(lokalny)
            .scale(0.94f + 0.06f * lokalny)
            .clip(RoundedCornerShape(Dim.rCell))
            .background(tlo)
            .border(1.dp, obrys, RoundedCornerShape(Dim.rCell))
            .then(if (pierscien != null) Modifier.border(1.5.dp, pierscien, RoundedCornerShape(Dim.rCell)) else Modifier)
            .clickable(enabled = !poza, onClick = naKlik)
            .then(
                // WARIANT E: ukośne paski w tle — faktura, nie barwa.
                if (swieto && !poza) Modifier.drawBehind {
                    // Deseń gaśnie ku dołowi kafelka, żeby nie zagłuszał oznaczenia zmiany.
                    val krok = 9.dp.toPx()
                    val pedzel = androidx.compose.ui.graphics.Brush.verticalGradient(
                        0f to swiatecznaBarwa.copy(alpha = 0.55f),
                        1f to swiatecznaBarwa.copy(alpha = 0.10f)
                    )
                    var x = -size.height
                    while (x < size.width) {
                        drawLine(
                            brush = pedzel,
                            start = androidx.compose.ui.geometry.Offset(x, size.height),
                            end = androidx.compose.ui.geometry.Offset(x + size.height, 0f),
                            strokeWidth = 2.dp.toPx()
                        )
                        x += krok
                    }
                } else Modifier
            )
            .padding(horizontal = 6.dp, vertical = 7.dp)
    ) {
        // Każda informacja ma swój róg i nie wchodzi w drogę pozostałym:
        // numer i kropka kolizji u góry z lewej, obecność u góry z prawej,
        // oznaczenie zmiany na dole z lewej, nadgodziny na dole z prawej,
        // a kropka wydarzenia na dole pośrodku — jedyne wolne miejsce.
        if ((maWydarzenie || maNotatke) && !poza) {
            Row(
                Modifier.align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                if (maWydarzenie) {
                    Box(
                        Modifier.size(5.dp).clip(RoundedCornerShape(999.dp))
                            .background(Tokeny.wydarzenie)
                    )
                }
                if (maNotatke) {
                    Box(
                        Modifier.size(5.dp).clip(RoundedCornerShape(999.dp))
                            .background(Tokeny.notatka)
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth().align(Alignment.TopStart), verticalAlignment = Alignment.Bottom) {
            Text("${data.dayOfMonth}", style = GrafikType.dayNumber, color = numer)
            if (kolizja && !poza) {
                Box(
                    Modifier.padding(start = 3.dp, bottom = 3.dp).size(5.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Tokeny.warnInk)
                )
            }
            Spacer(Modifier.weight(1f))
            if (godziny != null && !poza) {
                Text(
                    if (godziny.trwa) "praca" else "${godziny.hours}h",
                    fontSize = 9.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    fontFamily = Jakarta, color = Tokeny.inkMuted
                )
            }
        }
        if (!poza) {
            Row(
                Modifier.fillMaxWidth().align(Alignment.BottomStart),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(e?.shift?.code.orEmpty(), style = GrafikType.dayLabel, color = atrament)
                Spacer(Modifier.weight(1f))
                val ot = e?.otHours ?: 0
                if (ot > 0) {
                    Text(
                        "+$ot",
                        fontSize = 9.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        fontFamily = Jakarta,
                        color = if (e?.otRate == OtRate.P100) Paleta.I.ink
                        else Paleta.I.ink.copy(alpha = 0.65f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PasekWybranego(s: UiState, dzien: LocalDate, naSzczegoly: () -> Unit) {
    val p by postepWejscia(Motion.RISE_MS, 900)
    val e = s.entries[dzien]
    val k = Paleta.of(typDniaZ(e?.shift))
    val opisZmiany = when (e?.shift) {
        Shift.I -> "Zmiana ranna · 06:00–14:00 · 8 h"
        Shift.II -> "Zmiana popołudniowa · 14:00–22:00 · 8 h"
        Shift.III -> "Zmiana nocna · 22:00–06:00 · 8 h"
        Shift.URLOP -> "Urlop wypoczynkowy · 8 h"
        null -> "Brak wpisu w grafiku"
        else -> e.shift!!.label
    }
    // Wszystko, co tego dnia było albo jest zaplanowane — jedno pod drugim.
    // Maciej poprosił o to samo, co na ekranie „Teraz" (zgłoszenie z 17.09.2026).
    val pozycje = buildList {
        Holidays.nameOf(dzien)?.let { add("$it · święto ustawowo wolne" to Tokeny.warnInk) }
        add(opisZmiany to Tokeny.inkMuted)
        if ((e?.otHours ?: 0) > 0) {
            add("Nadgodziny +${e!!.otHours} h · ${e.otRate.percent} %" to Paleta.II.ink)
        }
        opisObecnosci(s.obecnosc[dzien])?.let {
            add(it.replaceFirstChar { z -> z.uppercase() } to Tokeny.accent)
        }
        s.events[dzien].orEmpty().forEach { ev ->
            add(
                listOfNotNull(ev.time.ifEmpty { null }, ev.text).joinToString(" · ") to Tokeny.warnInk
            )
        }
        if (!e?.note.isNullOrBlank()) add(e!!.note to Tokeny.inkFaint)
    }

    Row(
        Modifier.wejscie(p).padding(horizontal = Dim.screenGutter, vertical = 2.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dim.rCardSmall))
            .background(Tokeny.surfaceStrong)
            .border(1.dp, Tokeny.lineStrong, RoundedCornerShape(Dim.rCardSmall))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = if (pozycje.size > 1) Alignment.Top else Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(42.dp).clip(RoundedCornerShape(Dim.rCell))
                .background(k.fill).border(1.dp, k.line, RoundedCornerShape(Dim.rCell)),
            contentAlignment = Alignment.Center
        ) {
            Text(e?.shift?.code.orEmpty(), fontSize = 14.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                letterSpacing = 0.84.sp, fontFamily = Jakarta, color = k.ink)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                dzien.dayOfWeek.getDisplayName(JavaTextStyle.FULL_STANDALONE, PL)
                    .replaceFirstChar { it.uppercase() } + ", ${dzien.dayOfMonth} " +
                    dzien.month.getDisplayName(JavaTextStyle.FULL, PL),
                style = GrafikType.cardTitle, color = Tokeny.ink, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            pozycje.forEach { (tekst, kolor) ->
                Text(tekst, style = GrafikType.caption, color = kolor,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(12.dp))
                .background(Tokeny.accentTintBg)
                .border(1.dp, Tokeny.accentTintLine, RoundedCornerShape(12.dp))
                .clickable(onClick = naSzczegoly),
            contentAlignment = Alignment.Center
        ) { Icon(IkonaWPrawo, "Szczegóły dnia", Modifier.size(17.dp), tint = Tokeny.accent) }
    }
}

@Composable
private fun RzadPrzyciskow(naEdycje: () -> Unit) {
    val p by postepWejscia(Motion.RISE_MS, 950)
    Row(
        Modifier.wejscie(p).padding(horizontal = Dim.screenGutter).fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(Dim.rCell))
                .background(Tokeny.surface)
                .border(1.dp, Tokeny.lineStrong, RoundedCornerShape(Dim.rCell))
                .clickable(onClick = naEdycje),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            Icon(IkonaOlowek, null, Modifier.size(17.dp), tint = Tokeny.inkStrong)
            Text("Edytuj grafik", fontSize = 13.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                fontFamily = Jakarta, color = Tokeny.inkStrong)
        }
        Box(
            Modifier.size(46.dp).clip(RoundedCornerShape(Dim.rCell))
                .background(Tokeny.surface)
                .border(1.dp, Tokeny.lineStrong, RoundedCornerShape(Dim.rCell)),
            contentAlignment = Alignment.Center
        ) { Icon(IkonaFiltr, "Filtr i widok", Modifier.size(17.dp), tint = Tokeny.ink2) }
    }
}
