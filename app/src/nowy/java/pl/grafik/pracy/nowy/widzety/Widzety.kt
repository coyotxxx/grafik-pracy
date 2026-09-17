package pl.grafik.pracy.nowy.widzety

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.action.actionStartActivity
import pl.grafik.pracy.domain.DayEntry
import pl.grafik.pracy.domain.Shift
import pl.grafik.pracy.nowy.NowaActivity
import java.time.LocalDate
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

/**
 * Widżety ekranu głównego — makieta `design/mockups/Widget.html`.
 *
 * Trzy sztuki: „DZIŚ" (2×2), „DO STARTU" i tydzień na pełną szerokość.
 * Na widżetach tła są nieprzezroczyste, bo system nie gwarantuje rozmycia —
 * tak każe DESIGN_SPEC 5.18.
 */

private val PL = Locale.forLanguageTag("pl-PL")

// Kolory z makiety. Glance nie zna naszych tokenów Compose, więc powtarzamy je tutaj
// jako wartości wprost — te same liczby co w `GrafikTokens`.
private object Barwy {
    val tloDzis = Color(0xFF0E1620)
    val tloStartu = Color(0xFF0C1214)
    val tloTygodnia = Color(0xFF0B0E11)
    val obrysDzis = Color(0x387ABDFF)
    val obrysStartu = Color(0x3352D0B3)
    val obrysTygodnia = Color(0x1AFFFFFF)
    val ink = Color(0xFFF1F3F4)
    val ink2 = Color(0xFFB7BEC3)
    val inkMuted = Color(0xFF97A0A7)
    val inkFaint = Color(0xFF8A939B)
    val akcent = Color(0xFF52D0B3)
    val niebieski = Color(0xFF7ABDFF)
    val tor = Color(0xFF1E2327)
    val alarm = Color(0xFFFF937E)
}

private fun kolorZmiany(s: Shift?): Color = when (s) {
    Shift.I -> Color(0xFFE8C468)
    Shift.II -> Color(0xFFFF9E8A)
    Shift.III -> Barwy.niebieski
    Shift.URLOP -> Color(0xFFD89CE8)
    null -> Barwy.inkFaint
    else -> Color(0xFF8FD9A8)
}

private fun oznaczenie(s: Shift?): String = when (s) {
    null -> "—"
    Shift.W5 -> "w5"
    else -> s.code
}

private fun godziny(okno: Pair<java.time.LocalDateTime, java.time.LocalDateTime>?): String =
    okno?.let {
        "%02d:%02d – %02d:%02d".format(
            it.first.hour, it.first.minute, it.second.hour, it.second.minute
        )
    } ?: "dzień wolny"

// ─────────────────────────────────────────────────────────────
// WIDŻET „DZIŚ" — 2 × 2
// ─────────────────────────────────────────────────────────────

class WidzetDzis : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dane = DaneWidzetu.wczytaj(context)
        provideContent { GlanceTheme { TrescDzis(dane) } }
    }
}

class WidzetDzisReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WidzetDzis()
}

@Composable
private fun TrescDzis(d: DaneWidzetu) {
    Column(
        GlanceModifier.fillMaxSize()
            .background(Barwy.tloDzis)
            .cornerRadius(26.dp)
            .padding(16.dp)
            .clickable(actionStartActivity<NowaActivity>()),
        verticalAlignment = Alignment.Vertical.Top
    ) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
            Text(
                "DZIŚ",
                style = TextStyle(
                    color = ColorProvider(Barwy.niebieski),
                    fontSize = 10.sp, fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier.defaultWeight()
            )
            Box(
                GlanceModifier.size(8.dp).cornerRadius(4.dp)
                    .background(kolorZmiany(d.zmianaDzis))
            ) {}
        }

        Spacer(GlanceModifier.defaultWeight())

        Text(
            oznaczenie(d.zmianaDzis),
            style = TextStyle(
                color = ColorProvider(kolorZmiany(d.zmianaDzis)),
                fontSize = 40.sp, fontWeight = FontWeight.Bold
            )
        )
        Text(
            godziny(d.oknoDzis),
            style = TextStyle(
                color = ColorProvider(Barwy.ink), fontSize = 13.sp, fontWeight = FontWeight.Medium
            )
        )

        Spacer(GlanceModifier.defaultWeight())

        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
            Text(
                "jutro",
                style = TextStyle(color = ColorProvider(Barwy.inkMuted), fontSize = 11.sp),
                modifier = GlanceModifier.defaultWeight()
            )
            Text(
                if (d.zmianaJutro?.isWork == true)
                    "${oznaczenie(d.zmianaJutro)} · ${d.oknoJutro?.first?.let { "%02d:%02d".format(it.hour, it.minute) } ?: ""}"
                else oznaczenie(d.zmianaJutro),
                style = TextStyle(
                    color = ColorProvider(kolorZmiany(d.zmianaJutro)),
                    fontSize = 12.sp, fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// WIDŻET „DO STARTU"
// ─────────────────────────────────────────────────────────────

class WidzetDoStartu : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dane = DaneWidzetu.wczytaj(context)
        provideContent { GlanceTheme { TrescDoStartu(dane) } }
    }
}

class WidzetDoStartuReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WidzetDoStartu()
}

@Composable
private fun TrescDoStartu(d: DaneWidzetu) {
    Column(
        GlanceModifier.fillMaxSize()
            .background(Barwy.tloStartu)
            .cornerRadius(26.dp)
            .padding(horizontal = 14.dp, vertical = 16.dp)
            .clickable(actionStartActivity<NowaActivity>())
    ) {
        Text(
            if (d.trwa) "DO KOŃCA" else "DO STARTU",
            style = TextStyle(
                color = ColorProvider(Barwy.akcent), fontSize = 10.sp, fontWeight = FontWeight.Bold
            )
        )

        Spacer(GlanceModifier.defaultWeight())

        Text(
            d.odliczanie,
            style = TextStyle(
                color = ColorProvider(Barwy.ink), fontSize = 36.sp, fontWeight = FontWeight.Bold
            )
        )

        Spacer(GlanceModifier.defaultWeight())

        PasekGodzin(d.udzialMiesiaca)
        Spacer(GlanceModifier.height(6.dp))
        Text(
            "${d.godzinyMiesiaca} / ${d.normaMiesiaca} h w miesiącu",
            style = TextStyle(color = ColorProvider(Barwy.inkMuted), fontSize = 11.sp)
        )
    }
}

/** Pasek 5 dp z makiety: tor `#1E2327`, wypełnienie akcentem. */
@Composable
private fun PasekGodzin(udzial: Float) {
    LinearProgressIndicator(
        progress = udzial,
        modifier = GlanceModifier.fillMaxWidth().height(5.dp).cornerRadius(3.dp),
        color = ColorProvider(Barwy.akcent),
        backgroundColor = ColorProvider(Barwy.tor)
    )
}

// ─────────────────────────────────────────────────────────────
// WIDŻET TYGODNIA
// ─────────────────────────────────────────────────────────────

class WidzetTydzien : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dane = DaneWidzetu.wczytaj(context)
        provideContent { GlanceTheme { TrescTygodnia(dane) } }
    }
}

class WidzetTydzienReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WidzetTydzien()
}

@Composable
private fun TrescTygodnia(d: DaneWidzetu) {
    Column(
        GlanceModifier.fillMaxSize()
            .background(Barwy.tloTygodnia)
            .cornerRadius(26.dp)
            .padding(horizontal = 14.dp, vertical = 16.dp)
            .clickable(actionStartActivity<NowaActivity>())
    ) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
            Text(
                "Ten tydzień",
                style = TextStyle(
                    color = ColorProvider(Barwy.ink), fontSize = 12.sp, fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier.defaultWeight()
            )
            Text(
                zakresTygodnia(d.tydzien),
                style = TextStyle(color = ColorProvider(Barwy.inkMuted), fontSize = 11.sp)
            )
        }

        Spacer(GlanceModifier.height(12.dp))

        Row(GlanceModifier.fillMaxWidth()) {
            d.tydzien.forEach { e ->
                Box(GlanceModifier.defaultWeight().padding(horizontal = 2.dp)) {
                    KomorkaDnia(e, dzisiaj = e.date == d.dzis)
                }
            }
        }

        Spacer(GlanceModifier.height(12.dp))

        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
            Text(
                d.wydarzenie?.let { ev ->
                    val kiedy = if (d.dataWydarzenia == d.dzis) "dziś" else "jutro"
                    listOfNotNull(kiedy, ev.time.ifEmpty { null }, ev.text).joinToString(" ")
                } ?: "bez wydarzeń",
                style = TextStyle(
                    color = ColorProvider(
                        if (d.wydarzenie != null) Barwy.alarm else Barwy.inkMuted
                    ),
                    fontSize = 11.sp
                ),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight()
            )
            Text(
                "${d.godzinyTygodnia} h",
                style = TextStyle(
                    color = ColorProvider(Barwy.akcent), fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

@Composable
private fun KomorkaDnia(e: DayEntry, dzisiaj: Boolean) {
    Column(
        GlanceModifier.fillMaxWidth().height(62.dp)
            .cornerRadius(13.dp)
            .background(if (dzisiaj) Color(0x2352D0B3) else Color(0x14FFFFFF))
            .padding(vertical = 7.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally
    ) {
        Text(
            skrotDnia(e.date),
            style = TextStyle(color = ColorProvider(Barwy.inkFaint), fontSize = 9.sp,
                fontWeight = FontWeight.Medium)
        )
        Spacer(GlanceModifier.defaultWeight())
        Text(
            e.date.dayOfMonth.toString(),
            style = TextStyle(
                color = ColorProvider(if (dzisiaj) Barwy.akcent else Barwy.ink),
                fontSize = 13.sp, fontWeight = FontWeight.Bold
            )
        )
        Spacer(GlanceModifier.defaultWeight())
        Text(
            oznaczenie(e.shift),
            style = TextStyle(
                color = ColorProvider(kolorZmiany(e.shift)),
                fontSize = 11.sp, fontWeight = FontWeight.Bold
            )
        )
    }
}

private fun skrotDnia(d: LocalDate): String = when (d.dayOfWeek.value) {
    1 -> "pn"; 2 -> "wt"; 3 -> "śr"; 4 -> "cz"; 5 -> "pt"; 6 -> "so"; else -> "nd"
}

/** „14 – 20 września" — zakres tygodnia jak w makiecie. */
private fun zakresTygodnia(tydzien: List<DayEntry>): String {
    val od = tydzien.firstOrNull()?.date ?: return ""
    val doKiedy = tydzien.lastOrNull()?.date ?: return ""
    val miesiac = doKiedy.month.getDisplayName(JavaTextStyle.FULL, PL).lowercase(PL)
    return "${od.dayOfMonth} – ${doKiedy.dayOfMonth} $miesiac"
}
