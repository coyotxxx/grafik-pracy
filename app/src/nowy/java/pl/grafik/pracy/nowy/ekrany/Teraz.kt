package pl.grafik.pracy.nowy.ekrany

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import pl.grafik.pracy.domain.DayEntry
import pl.grafik.pracy.domain.PresenceEngine
import pl.grafik.pracy.domain.Shift
import pl.grafik.pracy.data.EventRow
import pl.grafik.pracy.nowy.theme.*
import pl.grafik.pracy.nowy.ui.*
import pl.grafik.pracy.ui.DayPresence
import pl.grafik.pracy.ui.UiState
import pl.grafik.pracy.ui.Vm
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

private val PL_TERAZ = Locale.forLanguageTag("pl-PL")

/** Liczba na małym kafelku: 16 sp/700, cyfry o stałej szerokości. */
private val liczbaKafelka = TextStyle(
    fontSize = 16.sp, fontWeight = FontWeight.Bold,
    fontFamily = Jakarta, fontFeatureSettings = TNUM
)

/** Ile dni pokazuje pasek „NAJBLIŻSZE DNI". */
private const val ILE_DNI = 5

/**
 * Ekran „Teraz" — odtworzony z design/mockups/Home.html.
 *
 * Tarcza doby pokazuje najbliższą zmianę: trwającą, a gdy takiej nie ma — pierwszą,
 * która się zaczyna. Makieta rysuje tylko przypadek „przed nocką", bo zakłada dzień
 * pracujący; pozostałe stany wynikają z tej samej geometrii.
 */
@Composable
fun EkranTeraz(
    vm: Vm,
    naGrafik: () -> Unit,
    naBilans: () -> Unit,
    naUstawienia: () -> Unit,
    naDzien: (LocalDate) -> Unit
) {
    val s by vm.state.collectAsState()
    val dni by vm.najblizszeDni.collectAsState()

    var teraz by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(20_000)
            teraz = LocalDateTime.now()
        }
    }
    val dzis = teraz.toLocalDate()

    val zmiana = remember(dni, teraz.withSecond(0).withNano(0)) { najblizszaZmiana(dni, teraz) }
    val kolejne = remember(dni, dzis) {
        val mapa = dni.associateBy { it.date }
        (1..ILE_DNI).map { i ->
            val d = dzis.plusDays(i.toLong())
            mapa[d] ?: DayEntry(date = d)
        }
    }
    var wybrany by remember(dzis) { mutableStateOf(dzis.plusDays(1)) }
    val wybranyWpis = kolejne.firstOrNull { it.date == wybrany } ?: kolejne.first()

    Box(Modifier.fillMaxSize()) {
        TloZPoswiata(Modifier.fillMaxSize())

        // Makieta ma 900 dp wysokości, telefon około 730 dp — treść przewijamy,
        // dokładnie jak na ekranie Grafik.
        Column(
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = gornaKrawedz(), bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            PasekGorny(s, dzis, naUstawienia)
            TarczaDoby(zmiana, teraz, s.obecnosc[dzis], Modifier.align(Alignment.CenterHorizontally))
            TrzyKafelki(s, naBilans)
            NajblizszeDni(
                kolejne, wybrany, wybranyWpis,
                s.events[wybrany].orEmpty(), s.obecnosc[wybrany]
            ) { wybrany = it }
            KartaWydarzenia(s, dni, dzis, naDzien)
            RzadAkcji(naGrafik) { naDzien(dzis) }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 1. PASEK GÓRNY
// ─────────────────────────────────────────────────────────────

@Composable
private fun PasekGorny(s: UiState, dzis: LocalDate, naUstawienia: () -> Unit) {
    val p by postepWejscia(Motion.RISE_MS)
    Row(
        Modifier.wejscie(p).padding(horizontal = Dim.screenGutter).fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                "BRYGADA ${s.cfg.brigade.uppercase(PL_TERAZ)}",
                style = GrafikType.sectionLabel, color = Tokeny.accent
            )
            Text(
                "${dzienTygodnia(dzis)}, ${dzis.dayOfMonth} ${miesiacDopelniacz(dzis)}",
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                fontFamily = Jakarta, color = Tokeny.inkStrong
            )
        }
        Box(
            Modifier.size(42.dp).clip(RoundedCornerShape(14.dp))
                .background(Tokeny.surface)
                .border(1.dp, Tokeny.lineStrong, RoundedCornerShape(14.dp))
                .clickable(onClick = naUstawienia),
            contentAlignment = Alignment.Center
        ) {
            Icon(IkonaUstawienia, "Ustawienia", Modifier.size(19.dp), tint = Tokeny.ink2)
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 2. TARCZA DOBY
// ─────────────────────────────────────────────────────────────

/** Najbliższa zmiana: trwająca albo pierwsza nadchodząca. */
private data class NajblizszaZmiana(
    val shift: Shift,
    val data: LocalDate,
    val start: LocalDateTime,
    val koniec: LocalDateTime,
    val trwa: Boolean
)

private fun najblizszaZmiana(dni: List<DayEntry>, teraz: LocalDateTime): NajblizszaZmiana? {
    var nadchodzi: NajblizszaZmiana? = null
    for (e in dni) {
        val s = e.shift ?: continue
        val w = PresenceEngine.shiftWindow(e.date, s) ?: continue
        if (!teraz.isBefore(w.first) && teraz.isBefore(w.second)) {
            return NajblizszaZmiana(s, e.date, w.first, w.second, trwa = true)
        }
        if (w.first.isAfter(teraz) && nadchodzi == null) {
            nadchodzi = NajblizszaZmiana(s, e.date, w.first, w.second, trwa = false)
        }
    }
    return nadchodzi
}

/** Godzina jako ułamek doby przeliczony na kąt: 0:00 na górze, doba = 360°. */
private fun kat(czas: LocalDateTime): Float = -90f + (czas.hour + czas.minute / 60f) * 15f

/**
 * Średnica tarczy doby. Makieta ma 240 dp, ale Maciej poprosił o większą, żeby
 * w środku było więcej miejsca na godzinę i opis zmiany (zgłoszenie z 17.09.2026).
 */
private val TARCZA = 296.dp

// Proporcje względem średnicy — wszystkie wzięte z makiety (np. 96/240 = 0.40).
private const val PROMIEN = 0.40f
private const val GRUBOSC = 0.0583f
private const val KRESKA_OD = 0.45f
private const val KRESKA_KROTKA = 0.475f
private const val KRESKA_DLUGA = 0.4833f
private const val WSKAZ_OD = 0.35f
private const val WSKAZ_DO = 0.4583f
private const val KULKA_OD = 0.4667f
private const val KULKA_R = 0.01875f

@Composable
private fun TarczaDoby(
    zm: NajblizszaZmiana?,
    teraz: LocalDateTime,
    obecnosc: DayPresence?,
    modifier: Modifier = Modifier
) {
    val kolory = Paleta.of(typDniaZ(zm?.shift))
    val pLuk by postepWejscia(Motion.ARC_DRAW_MS, Motion.ARC_DRAW_DELAY_MS)
    val pWsk by postepWejscia(Motion.SWEEP_MS, Motion.SWEEP_DELAY_MS)

    val katStart = zm?.let { kat(it.start) } ?: 0f
    val dlugosc = zm?.let {
        (Duration.between(it.start, it.koniec).toMinutes() / 60f * 15f).coerceAtMost(360f)
    } ?: 0f
    val katTeraz = kat(teraz)
    // Kolory czytamy przed Canvasem — wewnątrz rysowania nie ma kontekstu kompozycji.
    val kolorWskazowki = Tokeny.accent
    val kolorToru = Tokeny.tarczaTor
    val kolorKresek = Tokeny.tarczaKreski

    Box(modifier.size(TARCZA), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(TARCZA)) {
            val srodek = Offset(size.width / 2f, size.height / 2f)
            // Wymiary liczymy z rozmiaru tarczy, a nie wpisujemy na sztywno — dzięki temu
            // zmiana jednej liczby (TARCZA) skaluje pierścień, kreski i wskazówkę naraz.
            val bok = size.minDimension
            val r = bok * PROMIEN
            val grubosc = bok * GRUBOSC
            val rogTarczy = Offset(srodek.x - r, srodek.y - r)
            val bokTarczy = Size(r * 2, r * 2)

            drawCircle(kolorToru, radius = r, center = srodek, style = Stroke(grubosc))

            if (zm != null && dlugosc > 0f) {
                // Poświata łuku — drop-shadow(0 0 12px rgba(kolor,0.55)) z makiety.
                drawIntoCanvas { plotno ->
                    val farba = androidx.compose.ui.graphics.Paint().apply {
                        style = androidx.compose.ui.graphics.PaintingStyle.Stroke
                        strokeWidth = grubosc
                        strokeCap = StrokeCap.Round
                        color = Color.Transparent
                    }
                    farba.asFrameworkPaint().setShadowLayer(
                        12.dp.toPx(), 0f, 0f, kolory.solid.copy(alpha = 0.55f).toArgb()
                    )
                    plotno.nativeCanvas.drawArc(
                        rogTarczy.x, rogTarczy.y, rogTarczy.x + bokTarczy.width, rogTarczy.y + bokTarczy.height,
                        katStart, dlugosc * pLuk, false, farba.asFrameworkPaint()
                    )
                }
                drawArc(
                    color = kolory.solid,
                    startAngle = katStart,
                    sweepAngle = dlugosc * pLuk,
                    useCenter = false,
                    topLeft = rogTarczy,
                    size = bokTarczy,
                    style = Stroke(grubosc, cap = StrokeCap.Round)
                )
            }

            // Osiem znaczników co trzy godziny: dłuższe co 6 h.
            for (i in 0 until 8) {
                val dlugi = i % 2 == 0
                rotate(-90f + i * 45f, srodek) {
                    drawLine(
                        kolorKresek,
                        start = Offset(srodek.x + bok * KRESKA_OD, srodek.y),
                        end = Offset(
                            srodek.x + bok * (if (dlugi) KRESKA_DLUGA else KRESKA_KROTKA),
                            srodek.y
                        ),
                        strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round
                    )
                }
            }

            // Wskazówka „teraz" — jedzie od godziny 0 do bieżącej.
            val katWsk = -90f + (katTeraz + 90f) * pWsk
            rotate(katWsk, srodek) {
                drawLine(
                    kolorWskazowki,
                    start = Offset(srodek.x + bok * WSKAZ_OD, srodek.y),
                    end = Offset(srodek.x + bok * WSKAZ_DO, srodek.y),
                    strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round
                )
                drawCircle(kolorWskazowki, radius = bok * KULKA_R,
                    center = Offset(srodek.x + bok * KULKA_OD, srodek.y))
            }
        }

        SrodekTarczy(zm, teraz, kolory, obecnosc)
    }
}

@Composable
private fun SrodekTarczy(
    zm: NajblizszaZmiana?,
    teraz: LocalDateTime,
    kolory: DayColors,
    obecnosc: DayPresence?
) {
    val p1 by postepWejscia(Motion.RISE_MS, 350)
    val p2 by postepWejscia(Motion.RISE_MS, 450)
    val p3 by postepWejscia(Motion.RISE_MS, 550)
    val p4 by postepWejscia(Motion.RISE_MS, 650)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            Modifier.wejscie(p1),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                Modifier.size(7.dp).clip(RoundedCornerShape(999.dp))
                    .background(if (zm != null) kolory.solid else Tokeny.inkDisabled)
            )
            Text(
                if (zm != null) nazwaZmiany(zm.shift) else "DZIEŃ WOLNY",
                fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = Jakarta,
                letterSpacing = 1.1.sp,
                color = if (zm != null) kolory.ink else Tokeny.inkMuted
            )
        }
        Text(
            if (zm != null) odliczanie(teraz, if (zm.trwa) zm.koniec else zm.start)
            else "%d:%02d".format(teraz.hour, teraz.minute),
            style = GrafikType.heroNumber, color = Tokeny.ink,
            modifier = Modifier.wejscie(p2)
        )
        Text(
            when {
                zm == null -> "brak zaplanowanej zmiany"
                zm.trwa -> "do końca zmiany"
                else -> "do startu zmiany"
            },
            fontSize = 12.sp, fontFamily = Jakarta, color = Tokeny.inkMuted,
            modifier = Modifier.wejscie(p3)
        )
        if (zm != null) {
            Box(
                Modifier.padding(top = 4.dp).wejscie(p4)
                    .height(26.dp).clip(RoundedCornerShape(999.dp))
                    .background(kolory.solid.copy(alpha = 0.14f))
                    .border(1.dp, kolory.solid.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${godzinyZmiany(zm.shift)} · ${zm.shift.hours} h",
                    style = TextStyle(
                        fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                        fontFamily = Jakarta, fontFeatureSettings = TNUM
                    ),
                    color = kolory.ink
                )
            }
        }

        // Wykryta obecność — Maciej chciał wiedzieć wprost, że aplikacja widzi go w pracy.
        if (obecnosc?.trwa == true) {
            val p5 by postepWejscia(Motion.RISE_MS, 750)
            Row(
                Modifier.padding(top = 2.dp).wejscie(p5)
                    .height(24.dp).clip(RoundedCornerShape(999.dp))
                    .background(Tokeny.accent.copy(alpha = 0.16f))
                    .border(1.dp, Tokeny.accent.copy(alpha = 0.40f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    Modifier.size(6.dp).clip(RoundedCornerShape(999.dp))
                        .background(Tokeny.accent)
                )
                Text(
                    tekstObecnosciWTarczy(obecnosc),
                    style = TextStyle(
                        fontSize = 10.5.sp, fontWeight = FontWeight.Bold,
                        fontFamily = Jakarta, letterSpacing = 0.6.sp
                    ),
                    color = Tokeny.accent
                )
            }
        }
    }
}

/** „JESTEŚ W PRACY · OD 21:52" — krótko, bo miejsca w tarczy jest niewiele. */
private fun tekstObecnosciWTarczy(o: DayPresence): String {
    val od = o.od?.let { " · OD %02d:%02d".format(it.hour, it.minute) } ?: ""
    return "JESTEŚ W PRACY$od"
}

// ─────────────────────────────────────────────────────────────
// 3. TRZY KAFELKI
// ─────────────────────────────────────────────────────────────

@Composable
private fun TrzyKafelki(s: UiState, naBilans: () -> Unit) {
    val p by postepWejscia(Motion.RISE_MS, 700)
    val doDzis = if (s.stats.biezacyMiesiac) s.stats.doDzis else s.stats.rozliczone
    Row(
        Modifier.wejscie(p).padding(horizontal = Dim.screenGutter).fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        KafelekLiczby(Modifier.weight(1f), "godziny", naBilans) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$doDzis", style = liczbaKafelka, color = Tokeny.ink)
                Text("/${s.stats.norm}", style = liczbaKafelka.copy(
                    fontSize = 11.sp, fontWeight = FontWeight.Medium), color = Tokeny.inkMuted)
            }
        }
        KafelekLiczby(Modifier.weight(1f), "nadgodziny", naBilans) {
            Text("${s.stats.ot100 + s.stats.ot50} h", style = liczbaKafelka, color = Tokeny.ink)
        }
        KafelekLiczby(Modifier.weight(1f), "dni urlopu", naBilans) {
            Text("${s.urlopBilans.zostalo}", style = liczbaKafelka, color = Paleta.URLOP.ink)
        }
    }
}

@Composable
private fun KafelekLiczby(
    modifier: Modifier,
    podpis: String,
    akcja: () -> Unit,
    liczba: @Composable () -> Unit
) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp))
            .background(Tokeny.surface)
            .border(1.dp, Tokeny.line, RoundedCornerShape(16.dp))
            .clickable(onClick = akcja)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        liczba()
        Text(podpis, style = GrafikType.micro.copy(fontWeight = FontWeight.Normal),
            color = Tokeny.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ─────────────────────────────────────────────────────────────
// 4. NAJBLIŻSZE DNI
// ─────────────────────────────────────────────────────────────

@Composable
private fun NajblizszeDni(
    dni: List<DayEntry>,
    wybrany: LocalDate,
    wpis: DayEntry,
    wydarzenia: List<EventRow>,
    obecnosc: DayPresence?,
    naWybor: (LocalDate) -> Unit
) {
    val pNag by postepWejscia(Motion.RISE_MS, 750)
    val pOpis by postepWejscia(Motion.RISE_MS, 900)

    Column(
        Modifier.padding(horizontal = Dim.screenGutter),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("NAJBLIŻSZE DNI", style = GrafikType.sectionLabel,
            color = Tokeny.inkMuted, modifier = Modifier.wejscie(pNag))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            dni.forEachIndexed { i, e ->
                KafelekDnia(e, e.date == wybrany, i, Modifier.weight(1f)) { naWybor(e.date) }
            }
        }

        PlanDnia(wpis, wydarzenia, obecnosc, Modifier.wejscie(pOpis))
    }
}

/**
 * Wszystko, co na wybrany dzień zaplanowane — jedno pod drugim: zmiana z godzinami,
 * nadgodziny i wydarzenia z kalendarza. Maciej poprosił, żeby pasek pokazywał cały
 * dzień, a nie samą nazwę zmiany (zgłoszenie z 17.09.2026).
 */
@Composable
private fun PlanDnia(
    wpis: DayEntry,
    wydarzenia: List<EventRow>,
    obecnosc: DayPresence?,
    modifier: Modifier = Modifier
) {
    val k = Paleta.of(typDniaZ(wpis.shift))

    Column(
        modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Tokeny.surface)
            .border(1.dp, Tokeny.line, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(Modifier.size(7.dp).clip(RoundedCornerShape(999.dp)).background(k.solid))
            Text(
                "${dzienTygodnia(wpis.date)} ${wpis.date.dayOfMonth} ${miesiacDopelniacz(wpis.date)}",
                fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                fontFamily = Jakarta, color = Tokeny.ink
            )
        }

        WierszPlanu(opisDnia(wpis.shift), k.ink)

        if (wpis.otHours > 0) {
            WierszPlanu(
                "nadgodziny +${wpis.otHours} h · ${wpis.otRate.percent} %",
                Paleta.II.ink
            )
        }

        opisObecnosci(obecnosc)?.let { WierszPlanu(it, Tokeny.accent) }

        wydarzenia.forEach { ev ->
            WierszPlanu(
                listOfNotNull(ev.time.ifEmpty { null }, ev.text).joinToString(" · "),
                Tokeny.warnInk
            )
        }

        if (wpis.note.isNotBlank()) WierszPlanu(wpis.note, Tokeny.inkMuted)
    }
}

/** Jedna pozycja planu dnia: kropka w kolorze i tekst. */
@Composable
private fun WierszPlanu(tekst: String, kolor: androidx.compose.ui.graphics.Color) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            Modifier.padding(start = 2.dp, top = 6.dp).size(4.dp)
                .clip(RoundedCornerShape(999.dp)).background(kolor)
        )
        Text(
            tekst, fontSize = 12.sp, lineHeight = 17.sp,
            fontFamily = Jakarta, color = Tokeny.ink2
        )
    }
}

@Composable
private fun KafelekDnia(
    e: DayEntry,
    wybrany: Boolean,
    indeks: Int,
    modifier: Modifier,
    akcja: () -> Unit
) {
    val p by postepWejscia(Motion.CELL_IN_MS, 780 + indeks * 60)
    val k = Paleta.of(typDniaZ(e.shift))
    Column(
        modifier.height(84.dp)
            .alpha(p)
            .scale(0.94f + 0.06f * p)
            .clip(RoundedCornerShape(16.dp))
            .background(k.fill)
            .border(1.dp, if (wybrany) k.line else Tokeny.line, RoundedCornerShape(16.dp))
            .then(
                if (wybrany) Modifier.border(1.5.dp, Tokeny.accent, RoundedCornerShape(16.dp))
                else Modifier
            )
            .clickable(onClick = akcja)
            .padding(horizontal = 4.dp, vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(skrotDnia(e.date), fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
            fontFamily = Jakarta, letterSpacing = 0.4.sp, color = Tokeny.inkMuted)
        Text("${e.date.dayOfMonth}",
            style = TextStyle(
                fontSize = 17.sp, lineHeight = 17.sp, fontWeight = FontWeight.Bold,
                fontFamily = Jakarta, fontFeatureSettings = TNUM
            ),
            color = if (wybrany) Tokeny.ink else Tokeny.ink2)
        Text(oznaczenie(e.shift), fontSize = 12.sp, fontWeight = FontWeight.Bold,
            fontFamily = Jakarta, letterSpacing = 0.48.sp, color = k.ink,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ─────────────────────────────────────────────────────────────
// 5. KARTA WYDARZENIA
// ─────────────────────────────────────────────────────────────

@Composable
private fun KartaWydarzenia(
    s: UiState,
    dni: List<DayEntry>,
    dzis: LocalDate,
    naDzien: (LocalDate) -> Unit
) {
    val nastepne = remember(s.events, dzis) {
        s.events.filterKeys { !it.isBefore(dzis) }
            .toSortedMap()
            .entries.firstOrNull()
            ?.let { (d, lista) -> d to lista.first() }
    } ?: return

    val (data, ev) = nastepne
    val p by postepWejscia(Motion.RISE_MS, 950)

    Row(
        Modifier.wejscie(p).padding(horizontal = Dim.screenGutter).fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0x12FF937E))
            .border(1.dp, Color(0x38FF937E), RoundedCornerShape(18.dp))
            .clickable { naDzien(data) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(11.dp))
                .background(Color(0x24FF937E)),
            contentAlignment = Alignment.Center
        ) {
            Icon(IkonaDzwonek, null, Modifier.size(17.dp), tint = Tokeny.warnInk)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                tytulWydarzenia(ev, data, dzis),
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = Jakarta,
                color = Tokeny.ink, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                podpisWydarzenia(ev, data, dzis, s, dni),
                fontSize = 11.sp, fontFamily = Jakarta, color = Tokeny.inkMuted,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
        }
        Icon(IkonaWPrawo, null, Modifier.size(16.dp), tint = Tokeny.inkFaint)
    }
}

private fun tytulWydarzenia(ev: EventRow, data: LocalDate, dzis: LocalDate): String {
    val kiedy = when (data) {
        dzis -> "Dziś"
        dzis.plusDays(1) -> "Jutro"
        else -> "${dzienTygodnia(data)} ${data.dayOfMonth} ${miesiacDopelniacz(data)}"
    }
    val godzina = if (ev.time.isNotBlank()) " ${ev.time}" else ""
    return "$kiedy$godzina — ${ev.text}"
}

private fun podpisWydarzenia(
    ev: EventRow,
    data: LocalDate,
    dzis: LocalDate,
    s: UiState,
    dni: List<DayEntry>
): String {
    if (!ev.remind || !s.remindOn) return "bez przypomnienia"
    val dzienPrzed = data.minusDays(1)
    val kiedy = if (dzienPrzed == dzis) "dziś" else "${dzienTygodnia(dzienPrzed)}"
    val zmiana = dni.firstOrNull { it.date == dzienPrzed }?.shift
    val ogon = when (zmiana) {
        Shift.I -> ", przed ranną"
        Shift.II -> ", przed popołudniówką"
        Shift.III -> ", przed nocką"
        else -> ""
    }
    return "przypomnimy $kiedy o ${s.remindHour}:00$ogon"
}

// ─────────────────────────────────────────────────────────────
// 6. PRZYCISKI
// ─────────────────────────────────────────────────────────────

@Composable
private fun RzadAkcji(naGrafik: () -> Unit, naDzisiaj: () -> Unit) {
    val p by postepWejscia(Motion.RISE_MS, 1000)
    Row(
        Modifier.wejscie(p).padding(horizontal = Dim.screenGutter).fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            Modifier.weight(1f).height(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Tokeny.accent)
                .clickable(onClick = naGrafik),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            Icon(IkonaGrafik, null, Modifier.size(18.dp), tint = Tokeny.accentOn)
            Text("Otwórz grafik", fontSize = 14.sp, fontWeight = FontWeight.Bold,
                fontFamily = Jakarta, color = Tokeny.accentOn)
        }
        Box(
            Modifier.size(52.dp).clip(RoundedCornerShape(16.dp))
                .background(Tokeny.surface)
                .border(1.dp, Tokeny.lineStrong, RoundedCornerShape(16.dp))
                .clickable(onClick = naDzisiaj),
            contentAlignment = Alignment.Center
        ) {
            Icon(IkonaOlowek, "Edytuj dzisiejszy dzień", Modifier.size(19.dp), tint = Tokeny.ink2)
        }
    }
}

// ─────────────────────────────────────────────────────────────
// TEKSTY
// ─────────────────────────────────────────────────────────────

private fun nazwaZmiany(s: Shift): String = when (s) {
    Shift.I -> "ZMIANA RANNA"
    Shift.II -> "ZMIANA POPOŁUDNIOWA"
    Shift.III -> "ZMIANA NOCNA"
    else -> "DZIEŃ WOLNY"
}

private fun godzinyZmiany(s: Shift): String = when (s) {
    Shift.I -> "06:00 – 14:00"
    Shift.II -> "14:00 – 22:00"
    Shift.III -> "22:00 – 06:00"
    else -> ""
}

/**
 * Zdanie o wykrytej obecności w pracy: „jesteś w pracy od 21:52 · Wi-Fi",
 * a po wyjściu „byłeś w pracy 21:52–06:05 · 8 h".
 *
 * Wykrywanie ma dwa źródła — lokalizację i sieć Wi-Fi — więc piszemy, które
 * rozpoznało pobyt. Wpisy zrobione ręcznie nie udają wykrycia.
 */
fun opisObecnosci(o: DayPresence?): String? {
    if (o == null) return null
    val zrodlo = when {
        o.reczne || o.zrodlo == "manual" -> null
        o.zrodlo.contains("wifi") && o.zrodlo.contains("geo") -> "lokalizacja i Wi-Fi"
        o.zrodlo.contains("wifi") -> "Wi-Fi"
        o.zrodlo.contains("geo") -> "lokalizacja"
        else -> null
    }
    val ogon = zrodlo?.let { " · $it" } ?: ""

    if (o.trwa) {
        val od = o.od?.let { " od %02d:%02d".format(it.hour, it.minute) } ?: ""
        return "jesteś w pracy$od$ogon"
    }
    if (o.hours <= 0 && o.od == null) return null

    val zakres = if (o.od != null && o.doKiedy != null)
        " %02d:%02d–%02d:%02d".format(o.od!!.hour, o.od!!.minute, o.doKiedy!!.hour, o.doKiedy!!.minute)
    else ""
    val godziny = if (o.hours > 0) " · ${o.hours} h" else ""
    val czasownik = if (o.reczne) "obecność wpisana ręcznie" else "byłeś w pracy"
    return "$czasownik$zakres$godziny$ogon"
}

private fun opisDnia(s: Shift?): String = when (s) {
    Shift.I -> "zmiana ranna · 06:00–14:00 · 8 h"
    Shift.II -> "zmiana popołudniowa · 14:00–22:00 · 8 h"
    Shift.III -> "zmiana nocna · 22:00–06:00 · 8 h"
    Shift.URLOP -> "urlop wypoczynkowy"
    Shift.L4 -> "zwolnienie lekarskie"
    Shift.WS -> "wolne za święto"
    Shift.DWN -> "dzień za pracującą niedzielę"
    Shift.BWN -> "bezwzględnie wolna niedziela"
    else -> "dzień wolny w cyklu"
}

private fun oznaczenie(s: Shift?): String = when (s) {
    null -> "—"
    Shift.W5 -> "w5"
    else -> s.code
}

/** Odliczanie w formacie `g:mm`; ponad dobę pokazujemy pełną liczbę godzin. */
private fun odliczanie(od: LocalDateTime, doKiedy: LocalDateTime): String {
    val minuty = Duration.between(od, doKiedy).toMinutes().coerceAtLeast(0)
    return "%d:%02d".format(minuty / 60, minuty % 60)
}

private fun dzienTygodnia(d: LocalDate): String =
    d.dayOfWeek.getDisplayName(JavaTextStyle.FULL, PL_TERAZ)

private fun skrotDnia(d: LocalDate): String = when (d.dayOfWeek.value) {
    1 -> "pn"; 2 -> "wt"; 3 -> "śr"; 4 -> "cz"; 5 -> "pt"; 6 -> "so"; else -> "nd"
}

private fun miesiacDopelniacz(d: LocalDate): String =
    d.month.getDisplayName(JavaTextStyle.FULL, PL_TERAZ)
