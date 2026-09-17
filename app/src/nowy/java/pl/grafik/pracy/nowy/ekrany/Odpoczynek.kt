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
import pl.grafik.pracy.domain.KolizjaOdpoczynku
import pl.grafik.pracy.domain.Odpoczynek
import pl.grafik.pracy.domain.Shift
import pl.grafik.pracy.domain.TydzienOdpoczynku
import pl.grafik.pracy.nowy.theme.*
import pl.grafik.pracy.nowy.ui.*
import pl.grafik.pracy.ui.UiState
import pl.grafik.pracy.ui.Vm
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

private val PL_ODP = Locale.forLanguageTag("pl-PL")
private val GODZ = DateTimeFormatter.ofPattern("HH:mm")

/**
 * „Odpoczynek" — odtworzony z design/mockups/Rest.html.
 *
 * Kolizje i odpoczynek tygodniowy liczy `domain/Rest.kt` z godzin zmian w grafiku
 * (art. 132 i 133 KP). To podpowiedź, nie porada prawna.
 */
@Composable
fun EkranOdpoczynek(vm: Vm, naPowrot: () -> Unit, naDzien: (LocalDate) -> Unit) {
    val s by vm.state.collectAsState()
    var pominiete by remember(s.ym) { mutableStateOf(setOf<LocalDate>()) }
    val kolizje = s.kolizje.filter { it.date !in pominiete }

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
                Text("Odpoczynek", style = GrafikType.h1.copy(fontSize = 26.sp, lineHeight = 26.sp),
                    color = DarkTokens.ink)
                Text(
                    "Pilnujemy 11 h przerwy na dobę i 35 h w tygodniu. Sprawdzamy przy każdej zmianie w grafiku.",
                    fontSize = 12.sp, lineHeight = 18.sp, fontFamily = Jakarta,
                    color = DarkTokens.inkMuted
                )
            }

            Podsumowanie(kolizje.size, s)
            kolizje.forEach { k ->
                KartaKolizji(k, naDzien) { pominiete = pominiete + k.date }
            }
            KartaTygodni(s)
            Przelaczniki(s, vm)
            NotkaPrawna()
        }
    }
}

// ─────────────────────────────────────────────────────────────
// PODSUMOWANIE
// ─────────────────────────────────────────────────────────────

@Composable
private fun Podsumowanie(ile: Int, s: UiState) {
    val p by postepWejscia(Motion.RISE_MS, 50)
    val czysto = ile == 0
    val kolor = if (czysto) DarkTokens.accent else DarkTokens.warnInk
    val miesiac = wMiesiacu(s.ym.monthValue)

    Row(
        Modifier.wejscie(p).fillMaxWidth()
            .clip(RoundedCornerShape(Dim.rCard))
            .background(kolor.copy(alpha = 0.08f))
            .border(1.dp, kolor.copy(alpha = 0.28f), RoundedCornerShape(Dim.rCard))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier.size(46.dp).clip(RoundedCornerShape(15.dp))
                .background(kolor.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (czysto) IkonaPtaszek else IkonaOstrzezenie, null,
                Modifier.size(22.dp), tint = kolor
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                if (czysto) "Bez kolizji $miesiac" else "${liczbaKolizji(ile)} $miesiac",
                fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = Jakarta,
                color = DarkTokens.ink
            )
            Text(
                if (czysto) "każda przerwa ma co najmniej 11 h"
                else "przerwa krótsza niż 11 h · reszta miesiąca w porządku",
                fontSize = 11.sp, fontFamily = Jakarta,
                color = if (czysto) Color(0xFF9FE3D2) else DarkTokens.warnInk2
            )
        }
    }
}

/** Miejscownik z przyimkiem — „we wrześniu", „w lipcu". Java daje dopełniacz. */
private fun wMiesiacu(numer: Int): String = when (numer) {
    1 -> "w styczniu"; 2 -> "w lutym"; 3 -> "w marcu"; 4 -> "w kwietniu"
    5 -> "w maju"; 6 -> "w czerwcu"; 7 -> "w lipcu"; 8 -> "w sierpniu"
    9 -> "we wrześniu"; 10 -> "w październiku"; 11 -> "w listopadzie"; else -> "w grudniu"
}

private fun liczbaKolizji(ile: Int) = when (ile) {
    1 -> "1 kolizja"
    in 2..4 -> "$ile kolizje"
    else -> "$ile kolizji"
}

// ─────────────────────────────────────────────────────────────
// KARTA KOLIZJI
// ─────────────────────────────────────────────────────────────

@Composable
private fun KartaKolizji(k: KolizjaOdpoczynku, naDzien: (LocalDate) -> Unit, naPomin: () -> Unit) {
    val p by postepWejscia(Motion.RISE_MS, 90)

    KartaUstawien(Modifier.wejscie(p)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    "${dzienTygodniaOdp(k.date)} ${k.date.dayOfMonth} ${miesiacOdp(k.date)}",
                    fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = Jakarta,
                    color = DarkTokens.ink
                )
                Text(opisPrzejscia(k), fontSize = 11.sp, fontFamily = Jakarta,
                    color = DarkTokens.inkMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Box(
                Modifier.height(26.dp).clip(RoundedCornerShape(999.dp))
                    .background(Color(0x24FF937E))
                    .border(1.dp, Color(0x52FF937E), RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${k.przerwaH} h przerwy",
                    style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        fontFamily = Jakarta, fontFeatureSettings = TNUM),
                    color = DarkTokens.warnInk
                )
            }
        }

        OsCzasu(k)

        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp))
                .background(Color(0x40000000))
                .border(1.dp, DarkTokens.lineStrong, RoundedCornerShape(13.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                "Brakuje ${k.brakujeH} h do normy. Zwykle wychodzi przy przejściu z nocki " +
                    "na popołudniówkę albo przy dobranych nadgodzinach.",
                fontSize = 11.sp, lineHeight = 16.5.sp, fontFamily = Jakarta, color = DarkTokens.ink3
            )
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, DarkTokens.lineSoft, RoundedCornerShape(14.dp))
                    .clickable { naDzien(k.date) },
                contentAlignment = Alignment.Center
            ) {
                Text("Otwórz ten dzień", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    fontFamily = Jakarta, color = DarkTokens.inkStrong)
            }
            Box(
                Modifier.height(44.dp).clip(RoundedCornerShape(14.dp))
                    .border(1.dp, DarkTokens.lineSoft, RoundedCornerShape(14.dp))
                    .clickable(onClick = naPomin)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Wiem, ignoruj", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    fontFamily = Jakarta, color = DarkTokens.inkMuted)
            }
        }
    }
}

/** Pasek doby: koniec poprzedniej zmiany, przerwa, start następnej. */
@Composable
private fun OsCzasu(k: KolizjaOdpoczynku) {
    val poprzednia = ShiftPaletteDark.of(typDniaZ(k.poprzednia))
    val nastepna = ShiftPaletteDark.of(typDniaZ(k.nastepna))
    val dlugoscPoprzedniej = k.poprzednia.hours.toFloat()
    val dlugoscNastepnej = k.nastepna.hours.toFloat()
    val przerwa = k.przerwaH.toFloat()
    val margines = 4f
    val calosc = margines + dlugoscPoprzedniej + przerwa + dlugoscNastepnej + margines

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth().height(34.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Box(
                Modifier.weight(margines / calosc).fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp,
                        topEnd = 3.dp, bottomEnd = 3.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(1.dp, DarkTokens.line, RoundedCornerShape(topStart = 8.dp,
                        bottomStart = 8.dp, topEnd = 3.dp, bottomEnd = 3.dp))
            )
            SegmentOsi(Modifier.weight(dlugoscPoprzedniej / calosc), k.poprzednia.code,
                poprzednia.fill, poprzednia.line, poprzednia.ink)
            SegmentOsi(Modifier.weight(przerwa / calosc), "${k.przerwaH} h",
                Color(0x24FF937E), Color(0x80FF937E), DarkTokens.warnInk, maleLitery = true)
            SegmentOsi(Modifier.weight(dlugoscNastepnej / calosc), k.nastepna.code,
                nastepna.fill, nastepna.line, nastepna.ink)
            Box(
                Modifier.weight(margines / calosc).fillMaxHeight()
                    .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp,
                        topStart = 3.dp, bottomStart = 3.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(1.dp, DarkTokens.line, RoundedCornerShape(topEnd = 8.dp,
                        bottomEnd = 8.dp, topStart = 3.dp, bottomStart = 3.dp))
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf(
                k.koniecPoprzedniej.minusHours(k.poprzednia.hours.toLong()),
                k.koniecPoprzedniej,
                k.startNastepnej,
                k.startNastepnej.plusHours(k.nastepna.hours.toLong())
            ).forEach {
                Text(
                    it.format(GODZ),
                    style = TextStyle(fontSize = 9.sp, fontFamily = Jakarta, fontFeatureSettings = TNUM),
                    color = DarkTokens.inkFaint
                )
            }
        }
    }
}

@Composable
private fun SegmentOsi(
    modifier: Modifier,
    etykieta: String,
    tlo: Color,
    obrys: Color,
    ink: Color,
    maleLitery: Boolean = false
) {
    Box(
        modifier.fillMaxHeight().clip(RoundedCornerShape(3.dp))
            .background(tlo)
            .border(1.dp, obrys, RoundedCornerShape(3.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            etykieta, fontSize = if (maleLitery) 10.sp else 11.sp,
            fontWeight = FontWeight.Bold, fontFamily = Jakarta, color = ink,
            maxLines = 1, overflow = TextOverflow.Clip
        )
    }
}

private fun opisPrzejscia(k: KolizjaOdpoczynku): String = when {
    k.poprzednia == Shift.III && k.nastepna == Shift.II -> "koniec nocki, a po południu znów na halę"
    k.poprzednia == Shift.III && k.nastepna == Shift.I -> "koniec nocki i rano z powrotem"
    k.poprzednia == Shift.II && k.nastepna == Shift.I -> "popołudniówka, a rano znów na zmianę"
    else -> "${k.poprzednia.label} kończy się ${k.koniecPoprzedniej.format(GODZ)}, " +
        "${k.nastepna.label} zaczyna ${k.startNastepnej.format(GODZ)}"
}

// ─────────────────────────────────────────────────────────────
// ODPOCZYNEK TYGODNIOWY
// ─────────────────────────────────────────────────────────────

@Composable
private fun KartaTygodni(s: UiState) {
    val p by postepWejscia(Motion.RISE_MS, 130)
    val tygodnie = s.tygodnieOdpoczynku
    val najkrotszy = tygodnie.minOfOrNull { it.najdluzszaPrzerwaH } ?: 0L
    val wszystkieOk = tygodnie.all { it.spelnia }
    val kolor = if (wszystkieOk) DarkTokens.accent else DarkTokens.warnInk

    Column(
        Modifier.wejscie(p).fillMaxWidth()
            .clip(RoundedCornerShape(Dim.rCard))
            .background(kolor.copy(alpha = 0.06f))
            .border(1.dp, kolor.copy(alpha = 0.22f), RoundedCornerShape(Dim.rCard))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier.size(34.dp).clip(RoundedCornerShape(11.dp))
                    .background(kolor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (wszystkieOk) IkonaPtaszek else IkonaOstrzezenie, null,
                    Modifier.size(17.dp), tint = kolor
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Odpoczynek tygodniowy", style = GrafikType.cardTitle, color = DarkTokens.ink)
                Text(
                    "najkrótszy w miesiącu: $najkrotszy h · norma ${Odpoczynek.MIN_TYGODNIOWY_H} h",
                    fontSize = 11.sp, fontFamily = Jakarta,
                    color = if (wszystkieOk) Color(0xFF9FE3D2) else DarkTokens.warnInk2
                )
            }
        }

        tygodnie.forEach { t -> WierszTygodnia(t) }
    }
}

@Composable
private fun WierszTygodnia(t: TydzienOdpoczynku) {
    val kolor = if (t.spelnia) DarkTokens.accent else DarkTokens.warnInk
    // Pasek pełny przy dwukrotności normy — 70 h to tyle, ile daje wolny weekend.
    val udzial = (t.najdluzszaPrzerwaH / (Odpoczynek.MIN_TYGODNIOWY_H * 2f)).coerceIn(0f, 1f)

    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "${t.od.dayOfMonth} – ${t.doKiedy.dayOfMonth}.${"%02d".format(t.doKiedy.monthValue)}",
            Modifier.width(54.dp),
            style = TextStyle(fontSize = 11.sp, fontFamily = Jakarta, fontFeatureSettings = TNUM),
            color = DarkTokens.inkMuted
        )
        Box(
            Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(999.dp))
                .background(Color(0x4D000000))
        ) {
            Box(
                Modifier.fillMaxWidth(udzial).fillMaxHeight()
                    .clip(RoundedCornerShape(999.dp)).background(kolor)
            )
        }
        Text(
            "${t.najdluzszaPrzerwaH} h", Modifier.width(40.dp),
            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                fontFamily = Jakarta, fontFeatureSettings = TNUM),
            color = kolor, textAlign = TextAlign.End
        )
    }
}

// ─────────────────────────────────────────────────────────────
// PRZEŁĄCZNIKI I NOTKA
// ─────────────────────────────────────────────────────────────

@Composable
private fun Przelaczniki(s: UiState, vm: Vm) {
    val p by postepWejscia(Motion.RISE_MS, 170)
    Column(
        Modifier.wejscie(p).fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DarkTokens.surface)
            .border(1.dp, DarkTokens.line, RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        WierszPrzelacznika(
            "Znacznik w kalendarzu", "mała kropka na dniu z kolizją", s.restZnacznik
        ) { vm.saveOdpoczynek(it, s.restOstrzegaj) }

        Box(Modifier.fillMaxWidth().height(1.dp).background(DarkTokens.line))

        WierszPrzelacznika(
            "Ostrzegaj przy malowaniu", "od razu, gdy wpiszesz zmianę łamiącą przerwę", s.restOstrzegaj
        ) { vm.saveOdpoczynek(s.restZnacznik, it) }
    }
}

@Composable
private fun WierszPrzelacznika(
    tytul: String,
    podpis: String,
    wlaczony: Boolean,
    naZmiane: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 54.dp).clickable { naZmiane(!wlaczony) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(tytul, style = GrafikType.cardTitle, color = DarkTokens.ink)
            Text(podpis, fontSize = 11.sp, fontFamily = Jakarta, color = DarkTokens.inkMuted)
        }
        Przelacznik(wlaczony, naZmiane)
    }
}

@Composable
private fun NotkaPrawna() {
    val p by postepWejscia(Motion.RISE_MS, 210)
    Row(
        Modifier.wejscie(p).fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x08FFFFFF))
            .border(1.dp, DarkTokens.line, RoundedCornerShape(20.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(IkonaInfo, null, Modifier.size(16.dp).padding(top = 1.dp), tint = Color(0xFF8A939B))
        Text(
            "Liczymy z godzin zmian w grafiku (art. 132 i 133 Kodeksu pracy). To podpowiedź, " +
                "nie porada prawna — ruchomy czas pracy, doba pracownicza i ustalenia zakładowe " +
                "mogą zmienić wynik.",
            fontSize = 11.sp, lineHeight = 17.sp, fontFamily = Jakarta, color = DarkTokens.inkMuted
        )
    }
}

private fun dzienTygodniaOdp(d: LocalDate): String =
    d.dayOfWeek.getDisplayName(JavaTextStyle.FULL, PL_ODP)

private fun miesiacOdp(d: LocalDate): String =
    d.month.getDisplayName(JavaTextStyle.FULL, PL_ODP)
