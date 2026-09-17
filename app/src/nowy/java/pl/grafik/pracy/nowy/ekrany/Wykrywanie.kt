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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.grafik.pracy.data.PresenceRow
import pl.grafik.pracy.domain.Shift
import pl.grafik.pracy.nowy.theme.*
import pl.grafik.pracy.nowy.ui.*
import pl.grafik.pracy.ui.PresenceUi
import pl.grafik.pracy.ui.PresenceVm
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val DZIEN_MIES = DateTimeFormatter.ofPattern("dd.MM")
private val GODZINA = DateTimeFormatter.ofPattern("HH:mm")

/**
 * „Wykrywanie pracy" — odtworzone z design/mockups/Detect.html.
 *
 * Ustawienia trafiają do tego samego `WorkPlace`, z którego korzysta geofence
 * i watchdog klasycznej aplikacji. Lokalizacja nie opuszcza telefonu — nie ma serwera.
 */
@Composable
fun EkranWykrywanie(pvm: PresenceVm, naPowrot: () -> Unit) {
    val p by pvm.state.collectAsState()

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
                Text("Wykrywanie pracy",
                    style = GrafikType.h1.copy(fontSize = 26.sp, lineHeight = 26.sp),
                    color = DarkTokens.ink)
                Text("Telefon sam rozpozna, że jesteś w zakładzie. Lokalizacja nie opuszcza telefonu.",
                    fontSize = 12.sp, lineHeight = 18.sp, fontFamily = Jakarta,
                    color = DarkTokens.inkMuted)
            }

            KartaStrefy(p, pvm)
            KartaMiejsca(p, pvm)
            KartaWifi(p, pvm)
            KartaCzulosci(p, pvm)
            KartaHistorii(p)
        }
    }
}

// ─────────────────────────────────────────────────────────────
// STREFA
// ─────────────────────────────────────────────────────────────

@Composable
private fun KartaStrefy(p: PresenceUi, pvm: PresenceVm) {
    val pr by postepWejscia(Motion.RISE_MS, 50)
    val niebieski = ShiftPaletteDark.III.ink

    Column(
        Modifier.wejscie(pr).fillMaxWidth()
            .clip(RoundedCornerShape(Dim.rCard))
            .background(Color(0x0F7ABDFF))
            .border(1.dp, Color(0x387ABDFF), RoundedCornerShape(Dim.rCard))
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Radar(Modifier.size(180.dp))
        }

        Row(
            Modifier.fillMaxWidth().clickable { pvm.setEnabled(!p.place.enabled) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Automatyczne wykrywanie", fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    fontFamily = Jakarta, color = DarkTokens.ink)
                Text(
                    opisStrefy(p), fontSize = 11.sp, fontFamily = Jakarta,
                    color = Color(0xFF9EC9F0), maxLines = 2, overflow = TextOverflow.Ellipsis
                )
            }
            Przelacznik(p.place.enabled) { pvm.setEnabled(it) }
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x297ABDFF)))

        Row(
            Modifier.fillMaxWidth().clickable { pvm.setAutoSave(!p.place.autoSave) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Zapisuj automatycznie", style = GrafikType.cardTitle, color = DarkTokens.ink)
                Text("wykryty dzień wchodzi do grafiku sam, powiadomienie pozwala cofnąć",
                    fontSize = 11.sp, lineHeight = 16.5.sp, fontFamily = Jakarta,
                    color = DarkTokens.inkMuted)
            }
            Przelacznik(p.place.autoSave) { pvm.setAutoSave(it) }
        }
    }
}

private fun opisStrefy(p: PresenceUi): String {
    if (!p.place.enabled) return "wyłączone — dni wpisujesz sam"
    val dystans = p.distanceM?.let {
        if (it >= 1000) " · jesteś ${it / 1000} km od pracy" else " · jesteś $it m od pracy"
    } ?: ""
    return "włączone · promień ${p.place.radiusM} m$dystans"
}

/** Kręgi strefy z makiety: trzy okręgi i pinezka pośrodku. */
@Composable
private fun Radar(modifier: Modifier) {
    val niebieski = ShiftPaletteDark.III.ink
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val srodek = Offset(size.width / 2f, size.height / 2f)
            val r = size.minDimension / 2f
            drawCircle(Color(0x0D7ABDFF), radius = r * 0.845f, center = srodek)
            drawCircle(
                niebieski.copy(alpha = 0.28f), radius = r * 0.845f, center = srodek,
                style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(4.dp.toPx(), 6.dp.toPx())
                ))
            )
            drawCircle(Color(0x127ABDFF), radius = r * 0.555f, center = srodek)
            drawCircle(niebieski.copy(alpha = 0.22f), radius = r * 0.555f, center = srodek,
                style = Stroke(1.dp.toPx()))
            drawCircle(Color(0x1F7ABDFF), radius = r * 0.267f, center = srodek)
            drawCircle(niebieski.copy(alpha = 0.35f), radius = r * 0.267f, center = srodek,
                style = Stroke(1.dp.toPx()))
        }
        Icon(IkonaLokalizacja, null, Modifier.size(40.dp), tint = ShiftPaletteDark.III.ink)
    }
}

// ─────────────────────────────────────────────────────────────
// MIEJSCE I PROMIEŃ
// ─────────────────────────────────────────────────────────────

@Composable
private fun KartaMiejsca(p: PresenceUi, pvm: PresenceVm) {
    val pr by postepWejscia(Motion.RISE_MS, 90)
    val zapisane = p.place.lat != 0.0 || p.place.lon != 0.0

    KartaUstawien(Modifier.wejscie(pr)) {
        Text("GDZIE JEST TWOJA PRACA", style = GrafikType.sectionLabel, color = DarkTokens.inkFaint)

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                if (zapisane) "%.5f · %.5f".format(p.place.lat, p.place.lon)
                else "miejsce nie jest jeszcze zapisane",
                style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    fontFamily = Jakarta, fontFeatureSettings = TNUM),
                color = if (zapisane) DarkTokens.ink else DarkTokens.inkMuted,
                modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            if (zapisane) {
                Box(
                    Modifier.height(24.dp).clip(RoundedCornerShape(999.dp))
                        .background(Color(0x1F52D0B3))
                        .border(1.dp, Color(0x4D52D0B3), RoundedCornerShape(999.dp))
                        .padding(horizontal = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("zapisane", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        fontFamily = Jakarta, color = DarkTokens.accent)
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(15.dp))
                .background(DarkTokens.accent)
                .clickable(enabled = !p.capturing) { pvm.captureHere() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            Icon(IkonaCelownik, null, Modifier.size(17.dp), tint = DarkTokens.accentOn)
            Text(
                if (p.capturing) "Szukam lokalizacji…" else "Jestem teraz w pracy — zapisz miejsce",
                fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = Jakarta,
                color = DarkTokens.accentOn
            )
        }

        p.error?.let {
            Text(it, fontSize = 11.sp, fontFamily = Jakarta, color = DarkTokens.warnInk)
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(DarkTokens.line))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Promień strefy", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                fontFamily = Jakarta, color = DarkTokens.ink,
                modifier = Modifier.weight(1f).alignByBaseline())
            Text(
                "${p.place.radiusM} m",
                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    fontFamily = Jakarta, fontFeatureSettings = TNUM),
                color = ShiftPaletteDark.III.ink, modifier = Modifier.alignByBaseline()
            )
        }
        Suwak(
            wartosc = p.place.radiusM.toFloat(),
            zakres = 100f..500f,
            krokow = 7,
            naZmiane = { pvm.setRadius(it.toInt()) }
        )
        Text(
            "Poniżej 100 m Android zaczyna gubić zdarzenia. Duży zakład — większy promień.",
            fontSize = 11.sp, lineHeight = 16.5.sp, fontFamily = Jakarta, color = DarkTokens.inkFaint
        )
    }
}

// ─────────────────────────────────────────────────────────────
// WI-FI
// ─────────────────────────────────────────────────────────────

@Composable
private fun KartaWifi(p: PresenceUi, pvm: PresenceVm) {
    val pr by postepWejscia(Motion.RISE_MS, 130)

    KartaUstawien(Modifier.wejscie(pr)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text("WI-FI W PRACY", style = GrafikType.sectionLabel,
                color = DarkTokens.inkFaint, modifier = Modifier.weight(1f))
            Text("opcjonalnie", fontSize = 10.sp, fontFamily = Jakarta, color = DarkTokens.inkFaint)
        }
        Text(
            "Pewniejsze niż GPS w hali i nie zużywa baterii. Gdy telefon wisi na tej sieci, " +
                "nie uznamy przypadkowego wyjścia ze strefy.",
            fontSize = 11.sp, lineHeight = 16.5.sp, fontFamily = Jakarta, color = DarkTokens.inkMuted
        )

        if (p.place.ssid.isNotBlank()) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(Color(0x143FA98F))
                    .border(1.dp, Color(0x403FA98F), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(IkonaWifi, null, Modifier.size(18.dp), tint = DarkTokens.ok)
                Text(p.place.ssid, style = GrafikType.cardTitle, color = DarkTokens.ink,
                    modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Box(
                    Modifier.height(32.dp).clip(RoundedCornerShape(10.dp))
                        .border(1.dp, DarkTokens.lineSoft, RoundedCornerShape(10.dp))
                        .clickable { pvm.clearSsid() }
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Usuń", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                        fontFamily = Jakarta, color = DarkTokens.inkMuted)
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.04f))
                .border(1.dp, DarkTokens.lineSoft, RoundedCornerShape(14.dp))
                .clickable { pvm.captureSsid() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            Icon(IkonaPlus, null, Modifier.size(15.dp), tint = DarkTokens.ink2)
            Text(
                p.currentSsid?.let { "Dodaj sieć, na której jesteś teraz" }
                    ?: "Dodaj sieć, na której jesteś teraz",
                fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                fontFamily = Jakarta, color = DarkTokens.ink2
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// CZUŁOŚĆ
// ─────────────────────────────────────────────────────────────

@Composable
private fun KartaCzulosci(p: PresenceUi, pvm: PresenceVm) {
    val pr by postepWejscia(Motion.RISE_MS, 170)

    KartaUstawien(Modifier.wejscie(pr)) {
        Text("CZUŁOŚĆ", style = GrafikType.sectionLabel, color = DarkTokens.inkFaint)

        WierszSuwaka(
            tytul = "Minimalny pobyt uznawany za pracę",
            wartosc = "${p.place.minStayMin} min",
            liczba = p.place.minStayMin.toFloat(),
            zakres = 10f..120f,
            krokow = 10,
            podpis = "Przejazd obok zakładu nie zrobi dnia pracy."
        ) { pvm.setMinStay(it.toInt()) }

        WierszSuwaka(
            tytul = "Krótkie wyjście sklejane do",
            wartosc = "${p.place.mergeGapMin} min",
            liczba = p.place.mergeGapMin.toFloat(),
            zakres = 5f..90f,
            krokow = 16,
            podpis = "Skok do sklepu nie potnie dnia na dwie obecności."
        ) { pvm.setMergeGap(it.toInt()) }
    }
}

@Composable
private fun WierszSuwaka(
    tytul: String,
    wartosc: String,
    liczba: Float,
    zakres: ClosedFloatingPointRange<Float>,
    krokow: Int,
    podpis: String,
    naZmiane: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(tytul, fontSize = 13.sp, fontFamily = Jakarta, color = DarkTokens.ink,
                modifier = Modifier.weight(1f).alignByBaseline())
            Text(
                wartosc,
                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    fontFamily = Jakarta, fontFeatureSettings = TNUM),
                color = DarkTokens.accent, modifier = Modifier.alignByBaseline()
            )
        }
        Suwak(liczba, zakres, krokow, naZmiane)
        Text(podpis, fontSize = 11.sp, fontFamily = Jakarta, color = DarkTokens.inkFaint)
    }
}

/** Suwak z jawnymi kolorami z tokenów — Material 3 nie wnosi tu własnej palety. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun Suwak(
    wartosc: Float,
    zakres: ClosedFloatingPointRange<Float>,
    krokow: Int,
    naZmiane: (Float) -> Unit
) {
    val kolory = SliderDefaults.colors(
        thumbColor = DarkTokens.accent,
        activeTrackColor = DarkTokens.accent,
        activeTickColor = Color.Transparent,
        inactiveTrackColor = Color(0xFF1E2327),
        inactiveTickColor = Color.Transparent
    )
    Slider(
        value = wartosc.coerceIn(zakres.start, zakres.endInclusive),
        onValueChange = naZmiane,
        valueRange = zakres,
        steps = krokow,
        colors = kolory,
        // Material 3 rysuje na końcu toru kropkę „stop indicator" — makieta jej nie ma.
        track = { stan ->
            SliderDefaults.Track(
                sliderState = stan,
                colors = kolory,
                drawStopIndicator = null,
                drawTick = { _, _ -> },
                modifier = Modifier.height(6.dp)
            )
        },
        modifier = Modifier.fillMaxWidth().height(28.dp)
    )
}

// ─────────────────────────────────────────────────────────────
// HISTORIA
// ─────────────────────────────────────────────────────────────

@Composable
private fun KartaHistorii(p: PresenceUi) {
    val pr by postepWejscia(Motion.RISE_MS, 210)
    val ostatnie = p.log.take(10)

    KartaUstawien(Modifier.wejscie(pr)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text("OSTATNIE WYKRYCIA", style = GrafikType.sectionLabel,
                color = DarkTokens.inkFaint, modifier = Modifier.weight(1f))
            Text("${p.log.size} zapisanych", fontSize = 10.sp,
                fontFamily = Jakarta, color = DarkTokens.inkFaint)
        }

        if (ostatnie.isEmpty()) {
            Text("Nic jeszcze nie wykryliśmy.", fontSize = 12.sp,
                fontFamily = Jakarta, color = DarkTokens.inkMuted)
        }

        ostatnie.forEach { wpis -> WierszWykrycia(wpis) }
    }
}

@Composable
private fun WierszWykrycia(wpis: PresenceRow) {
    val zmiana = Shift.entries.firstOrNull { it.code == wpis.shiftCode }
    val k = ShiftPaletteDark.of(typDniaZ(zmiana))
    val od = runCatching { LocalDateTime.parse(wpis.enterAt) }.getOrNull()
    val doKiedy = runCatching { LocalDateTime.parse(wpis.exitAt) }.getOrNull()

    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier.size(7.dp).clip(RoundedCornerShape(999.dp))
                .background(
                    when (wpis.status) {
                        "accepted" -> DarkTokens.ok
                        "rejected" -> DarkTokens.inkDisabled
                        else -> ShiftPaletteDark.I.ink
                    }
                )
        )
        Text(
            od?.toLocalDate()?.format(DZIEN_MIES) ?: wpis.date,
            Modifier.width(44.dp),
            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                fontFamily = Jakarta, fontFeatureSettings = TNUM),
            color = DarkTokens.ink
        )
        Text(
            if (od != null && doKiedy != null) "${od.format(GODZINA)} – ${doKiedy.format(GODZINA)}"
            else "—",
            Modifier.weight(1f),
            style = TextStyle(fontSize = 12.sp, fontFamily = Jakarta, fontFeatureSettings = TNUM),
            color = DarkTokens.ink3
        )
        Text(
            wpis.shiftCode.ifBlank { "—" }, fontSize = 12.sp, fontWeight = FontWeight.Bold,
            fontFamily = Jakarta, color = k.ink
        )
    }
}
