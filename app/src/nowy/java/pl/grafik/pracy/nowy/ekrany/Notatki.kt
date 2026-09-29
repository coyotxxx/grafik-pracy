package pl.grafik.pracy.nowy.ekrany

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pl.grafik.pracy.data.ZdjeciaNotatek
import pl.grafik.pracy.domain.DayEntry
import pl.grafik.pracy.domain.Shift
import pl.grafik.pracy.nowy.theme.*
import pl.grafik.pracy.nowy.ui.*
import pl.grafik.pracy.ui.Vm
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

private val PL_NOTATKI = Locale.forLanguageTag("pl-PL")

/** Co widać na liście: wszystko albo tylko notatki ze zdjęciem. */
private enum class FiltrNotatek { WSZYSTKIE, ZE_ZDJECIEM }

/**
 * Wszystkie notatki z lat, do przewijania.
 *
 * Po kilku latach uzbiera się tego sporo, a notatka bez daty i bez zmiany niewiele
 * mówi — dlatego w wierszu jest jedno i drugie. Dotknięcie otwiera ten dzień
 * w grafiku, tak samo jak dotąd.
 */
@Composable
fun EkranNotatki(vm: Vm, naPowrot: () -> Unit, naDzien: (LocalDate) -> Unit) {
    val wszystkie by vm.notatki.collectAsState()

    var szukane by remember { mutableStateOf("") }
    var filtr by remember { mutableStateOf(FiltrNotatek.WSZYSTKIE) }
    var odNajstarszych by remember { mutableStateOf(false) }

    val widoczne = remember(wszystkie, szukane, filtr, odNajstarszych) {
        wszystkie
            .filter { filtr == FiltrNotatek.WSZYSTKIE || !it.notePhoto.isNullOrBlank() }
            .filter { szukane.isBlank() || it.note.contains(szukane.trim(), ignoreCase = true) }
            .let { if (odNajstarszych) it.sortedBy { w -> w.date } else it.sortedByDescending { w -> w.date } }
    }

    // Nagłówek miesiąca wchodzi tam, gdzie zmienia się miesiąc — lista jest już posortowana.
    val pozycje = remember(widoczne) { zPodzialemNaMiesiace(widoczne) }

    val zeZdjeciem = remember(wszystkie) { wszystkie.count { !it.notePhoto.isNullOrBlank() } }

    Box(Modifier.fillMaxSize()) {
        TloZPoswiata(Modifier.fillMaxSize())

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Dim.screenGutter, end = Dim.screenGutter,
                top = gornaKrawedz(), bottom = dolnaKrawedz(22.dp)
            ),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    PowrotDoUstawien(naPowrot)
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Notatki", style = GrafikType.h1.copy(fontSize = 26.sp, lineHeight = 26.sp),
                            color = Tokeny.ink)
                        Text(
                            opisZbioru(wszystkie.size, zeZdjeciem),
                            style = GrafikType.caption, color = Tokeny.inkMuted
                        )
                    }
                    PoleTekstowe(szukane, "Szukaj w notatkach") { szukane = it.take(60) }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ChipNotatek("Wszystkie", filtr == FiltrNotatek.WSZYSTKIE) { filtr = FiltrNotatek.WSZYSTKIE }
                        ChipNotatek("Ze zdjęciem", filtr == FiltrNotatek.ZE_ZDJECIEM) { filtr = FiltrNotatek.ZE_ZDJECIEM }
                        ChipNotatek(
                            if (odNajstarszych) "Od najstarszych" else "Od najnowszych",
                            false
                        ) { odNajstarszych = !odNajstarszych }
                    }
                    Spacer(Modifier.height(2.dp))
                }
            }

            if (pozycje.isEmpty()) {
                item { PustaLista(wszystkie.isEmpty(), szukane) }
            }

            items(pozycje, key = { if (it is Pozycja.Miesiac) "m:${it.ym}" else "d:${(it as Pozycja.Dzien).wpis.date}" }) { p ->
                when (p) {
                    is Pozycja.Miesiac -> NaglowekMiesiaca(p.ym, p.ile)
                    is Pozycja.Dzien -> WierszNotatkiZListy(p.wpis) { naDzien(p.wpis.date) }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────

internal sealed interface Pozycja {
    data class Miesiac(val ym: YearMonth, val ile: Int) : Pozycja
    data class Dzien(val wpis: DayEntry) : Pozycja
}

internal fun zPodzialemNaMiesiace(lista: List<DayEntry>): List<Pozycja> {
    if (lista.isEmpty()) return emptyList()
    val ileWMiesiacu = lista.groupingBy { YearMonth.from(it.date) }.eachCount()
    val wynik = mutableListOf<Pozycja>()
    var biezacy: YearMonth? = null
    lista.forEach { w ->
        val ym = YearMonth.from(w.date)
        if (ym != biezacy) {
            wynik += Pozycja.Miesiac(ym, ileWMiesiacu[ym] ?: 0)
            biezacy = ym
        }
        wynik += Pozycja.Dzien(w)
    }
    return wynik
}

internal fun opisZbioru(ile: Int, zeZdjeciem: Int): String = when {
    ile == 0 -> "jeszcze nic nie zapisano"
    zeZdjeciem == 0 -> "$ile ${odmianaZapisanych(ile)}"
    else -> "$ile ${odmianaZapisanych(ile)} · $zeZdjeciem ze zdjęciem"
}

internal fun odmianaZapisanych(ile: Int): String = when {
    ile == 1 -> "zapisana"
    ile % 10 in 2..4 && ile % 100 !in 12..14 -> "zapisane"
    else -> "zapisanych"
}

@Composable
private fun ChipNotatek(tekst: String, wybrany: Boolean, akcja: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(999.dp))
            .background(if (wybrany) Tokeny.notatka.copy(alpha = 0.14f) else Tokeny.surface)
            .border(
                1.dp,
                if (wybrany) Tokeny.notatka.copy(alpha = 0.40f) else Tokeny.line,
                RoundedCornerShape(999.dp)
            )
            .clickable(onClick = akcja)
            .padding(horizontal = 11.dp, vertical = 6.dp)
    ) {
        Text(
            tekst, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = Jakarta,
            color = if (wybrany) Tokeny.notatka else Tokeny.inkMuted, maxLines = 1
        )
    }
}

@Composable
private fun NaglowekMiesiaca(ym: YearMonth, ile: Int) {
    Row(
        Modifier.fillMaxWidth().padding(top = 11.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            (ym.month.getDisplayName(JavaTextStyle.FULL_STANDALONE, PL_NOTATKI)
                .replaceFirstChar { it.uppercase() } + " ${ym.year}").uppercase(PL_NOTATKI),
            style = GrafikType.sectionLabel, color = Tokeny.inkFaint
        )
        Box(Modifier.weight(1f).height(1.dp).background(Tokeny.line))
        Text(
            "$ile",
            style = TextStyle(
                fontFamily = Jakarta, fontSize = 10.5.sp, fontFeatureSettings = TNUM
            ),
            color = Tokeny.inkFaint
        )
    }
}

@Composable
private fun WierszNotatkiZListy(w: DayEntry, naKlik: () -> Unit) {
    val ctx = LocalContext.current
    val nazwaZdjecia = w.notePhoto

    val miniatura by produceState<android.graphics.Bitmap?>(null, nazwaZdjecia) {
        value = nazwaZdjecia?.let {
            withContext(Dispatchers.IO) { miniaturaListy(ZdjeciaNotatek.plik(ctx, it), 120) }
        }
    }

    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Dim.rCardSmall))
            .background(Tokeny.surface)
            .border(1.dp, Tokeny.line, RoundedCornerShape(Dim.rCardSmall))
            .clickable(onClick = naKlik)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        Column(
            Modifier.width(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "${w.date.dayOfMonth}",
                style = TextStyle(
                    fontFamily = Jakarta, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold,
                    lineHeight = 17.sp, fontFeatureSettings = TNUM
                ),
                color = Tokeny.ink
            )
            Text(
                w.date.dayOfWeek.getDisplayName(JavaTextStyle.SHORT, PL_NOTATKI)
                    .lowercase(PL_NOTATKI).trimEnd('.'),
                fontSize = 9.5.sp, fontFamily = Jakarta, color = Tokeny.inkFaint,
                textAlign = TextAlign.Center
            )
        }

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                w.note.ifBlank { "Notatka bez treści" },
                style = TextStyle(fontFamily = Jakarta, fontSize = 13.sp, lineHeight = 18.sp),
                color = if (w.note.isBlank()) Tokeny.inkFaint else Tokeny.ink2,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                w.shift?.let { ZnacznikZmiany(it) }
                godzinyDlaListy(w.shift)?.let {
                    Text(it, style = GrafikType.caption, color = Tokeny.inkFaint)
                }
            }
        }

        Box(
            Modifier.size(42.dp).clip(RoundedCornerShape(11.dp))
                .background(Tokeny.surfaceInput)
                .border(1.dp, Tokeny.line, RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) {
            val bmp = miniatura
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Zdjęcie dołączone do notatki",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    if (nazwaZdjecia != null) IkonaObrazek else IkonaNotatka,
                    null, Modifier.size(15.dp), tint = Tokeny.inkFaint
                )
            }
        }
    }
}

@Composable
private fun ZnacznikZmiany(s: Shift) {
    val k = kolorZmiany(s)
    Box(
        Modifier.clip(RoundedCornerShape(5.dp))
            .background(k.copy(alpha = 0.14f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        // Sam kod zmiany, jak na kafelkach w grafiku — „Zmiana I" rozpychałoby wiersz.
        Text(
            etykietaZmiany(s), fontSize = 9.5.sp, fontWeight = FontWeight.Bold,
            fontFamily = Jakarta, letterSpacing = 0.4.sp, color = k, maxLines = 1
        )
    }
}

@Composable
private fun kolorZmiany(s: Shift): Color = when (s) {
    Shift.I -> Paleta.I.ink
    Shift.II -> Paleta.II.ink
    Shift.III -> Paleta.III.ink
    Shift.URLOP -> Paleta.URLOP.ink
    else -> Tokeny.inkMuted
}

private fun etykietaZmiany(s: Shift): String = when (s) {
    Shift.I, Shift.II, Shift.III -> s.code
    Shift.URLOP -> "Urlop"
    Shift.L4 -> "L4"
    Shift.DWN -> "DWN"
    else -> s.label
}

private fun godzinyDlaListy(s: Shift?): String? =
    if (s != null && s.isWork) "${s.from} – ${s.to}" else null

@Composable
private fun PustaLista(zupelniePusto: Boolean, szukane: String) {
    Column(
        Modifier.fillMaxWidth().padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(IkonaNotatka, null, Modifier.size(26.dp), tint = Tokeny.inkFaint)
        Text(
            when {
                zupelniePusto -> "Jeszcze nic nie zapisano"
                szukane.isNotBlank() -> "Nic nie pasuje do „$szukane”"
                else -> "Brak notatek ze zdjęciem"
            },
            style = GrafikType.cardTitle, color = Tokeny.inkMuted
        )
        Text(
            if (zupelniePusto) "Notatkę dopisujesz przy dniu w grafiku." else "Zmień szukanie albo filtr.",
            style = GrafikType.caption, color = Tokeny.inkFaint, textAlign = TextAlign.Center
        )
    }
}

/** Miniatura do wiersza listy — mała, wczytywana poza wątkiem rysowania. */
private fun miniaturaListy(plik: java.io.File, bok: Int): android.graphics.Bitmap? = runCatching {
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
