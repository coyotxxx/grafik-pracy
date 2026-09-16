package pl.grafik.pracy.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.grafik.pracy.domain.*
import pl.grafik.pracy.ui.*
import pl.grafik.pracy.ui.theme.*
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private val PL = Locale("pl", "PL")

/** Święta w kalendarzu: numer dnia na czerwonym krążku — widać od razu i nic nie zasłania. */
private val SwietoKolor = Color(0xFFD64545)

@Composable
fun CalendarScreen(vm: Vm, onOpenDay: (LocalDate) -> Unit) {
    val s by vm.state.collectAsState()

    // Domyślnie schowana — kalendarz zajmuje cały ekran, paleta wyskakuje tylko na wybór.
    var paletaOtwarta by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Bg)) {
        Header(vm, s)
        WeekHeader()
        // Siatka bierze całe wolne miejsce — po zwinięciu palety kalendarz robi się duży.
        Grid(
            s, Modifier.weight(1f),
            onTap = { vm.tap(it) },
            onLong = onOpenDay,
            onPrev = { vm.prevMonth() },
            onNext = { vm.nextMonth() }
        )
        Palette(vm, s, paletaOtwarta) { paletaOtwarta = !paletaOtwarta }
    }
}

@Composable
private fun Header(vm: Vm, s: UiState) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                s.ym.month.getDisplayName(TextStyle.FULL_STANDALONE, PL).replaceFirstChar { it.uppercase() } + " " + s.ym.year,
                fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = OnBg
            )
            Text(
            if (s.cfg.generate) "Brygada ${s.cfg.brigade} · ${s.cfg.pattern.label}"
            else "Grafik pusty — ustaw swój cykl w zakładce Cykl albo maluj dni ręcznie",
            fontSize = 11.sp,
            color = if (s.cfg.generate) OnMuted else Accent,
            maxLines = 2
        )
        }
        IconButton(onClick = { vm.prevMonth() }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.ChevronLeft, "poprzedni", tint = OnBg) }
        IconButton(onClick = { vm.nextMonth() }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.ChevronRight, "następny", tint = OnBg) }
    }

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("${s.stats.rozliczone}", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = OnBg)
        Text(" / ${s.stats.norm} h", fontSize = 12.sp, color = OnMuted)
        if (s.stats.urlopH > 0) {
            Spacer(Modifier.width(6.dp))
            Text("(w tym ${s.stats.urlopH} h urlopu)", fontSize = 10.sp, color = OnFaint)
        }
        Spacer(Modifier.width(10.dp))
        val d = s.stats.diff
        Text(if (d >= 0) "+$d h" else "$d h", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            color = if (d >= 0) OtColor100 else OnMuted)
        Spacer(Modifier.weight(1f))
        if (s.stats.ot > 0) {
            Box(Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFF3A2E14)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                Text("nadg. ${s.stats.ot} h", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = OtColor100)
            }
        }
        if (s.stats.sundayWork > 0) {
            Spacer(Modifier.width(5.dp))
            Box(Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFF1E3A29)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                Text("nd ${s.stats.sundayWork}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = SunColor)
            }
        }
    }
}

@Composable
private fun Chip(big: String, small: String, bg: Color, fg: Color, m: Modifier = Modifier) {
    Column(m.clip(RoundedCornerShape(12.dp)).background(bg).padding(horizontal = 8.dp, vertical = 4.dp)) {
        Text(big, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = fg)
        Text(small, fontSize = 8.sp, color = OnFaint, maxLines = 1)
    }
}

@Composable
private fun WeekHeader() {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp)) {
        listOf("PN","WT","ŚR","CZ","PT","SO","ND").forEachIndexed { i, t ->
            Text(t, Modifier.weight(1f), fontSize = 9.sp, fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = when (i) { 5 -> SatColor; 6 -> SunColor; else -> OnFaint })
        }
    }
}

@Composable
private fun Grid(
    s: UiState, m: Modifier,
    onTap: (LocalDate) -> Unit, onLong: (LocalDate) -> Unit,
    onPrev: () -> Unit, onNext: () -> Unit
) {
    val first = s.ym.atDay(1)
    val start = first.minusDays(((first.dayOfWeek.value + 6) % 7).toLong())
    val last = s.ym.atEndOfMonth()
    val end = last.plusDays((7 - last.dayOfWeek.value).toLong())
    val weeks = (java.time.temporal.ChronoUnit.DAYS.between(start, end).toInt() + 1) / 7
    val today = LocalDate.now()

    // Przesunięcie palcem przełącza miesiąc. Próg zależy od szerokości ekranu,
    // żeby drobny ruch przy dotykaniu dnia niczego nie przewijał.
    var przesuniecie by remember(s.ym) { mutableFloatStateOf(0f) }

    Column(
        m.fillMaxWidth()
            .pointerInput(s.ym) {
                val prog = size.width * 0.18f
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (przesuniecie > prog) onPrev() else if (przesuniecie < -prog) onNext()
                        przesuniecie = 0f
                    },
                    onDragCancel = { przesuniecie = 0f },
                    onHorizontalDrag = { _, delta -> przesuniecie += delta }
                )
            }
            .padding(horizontal = 10.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        repeat(weeks) { w ->
            Row(
                Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                repeat(7) { i ->
                    val d = start.plusDays((w * 7 + i).toLong())
                    DayCell(
                        motyw = s.motyw,
                        date = d,
                        e = s.entries[d],
                        events = s.events[d].orEmpty().size,
                        colors = s.colors,
                        isToday = d == today,
                        // Sąsiednie miesiące widać, ale są przygaszone.
                        obcy = java.time.YearMonth.from(d) != s.ym,
                        m = Modifier.weight(1f).fillMaxHeight(),
                        onTap = { onTap(d) },
                        onLong = { onLong(d) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DayCell(
    motyw: PaletteTheme,
    date: LocalDate, e: DayEntry?, events: Int, colors: Map<String, String>,
    isToday: Boolean, obcy: Boolean, m: Modifier, onTap: () -> Unit, onLong: () -> Unit
) {
    val sw = Palette.byId(colors[e?.shift?.code] ?: Palette.defaults[e?.shift?.code], motyw)
    val kind = Holidays.kindOf(date)
    val swieto = Holidays.isHoliday(date)
    val works = e?.shift?.isWork == true
    val special = works && (kind == DayKind.NIEDZIELA || kind == DayKind.SWIETO)
    val satWork = works && kind == DayKind.SOBOTA

    val border = when {
        isToday -> Accent
        special -> SunColor
        satWork -> SatColor
        else -> sw.border
    }
    val bw = if (isToday || special || satWork) 2.dp else 1.dp

    Column(
        m.alpha(if (obcy) 0.42f else 1f)
            .clip(RoundedCornerShape(10.dp))
            .background(if (e?.shift == null) Surface1 else sw.fill)
            .border(bw, border, RoundedCornerShape(10.dp))
            .combinedClickable(onClick = onTap, onLongClick = onLong)
    ) {
        if ((e?.otHours ?: 0) > 0) {
            Box(Modifier.fillMaxWidth().background(if (e!!.otRate == OtRate.P100) OtColor100 else OtColor50)) {
                Text(
                    "+${e.otHours}h ${e.otRate.percent}%",
                    Modifier.fillMaxWidth().padding(vertical = 1.dp),
                    fontSize = 8.sp, fontWeight = FontWeight.Bold,
                    color = Color(0xFF2A1C06), textAlign = TextAlign.Center, maxLines = 1
                )
            }
        }
        // Numer dnia leży NA polu etykiety, a nie nad nim — dzięki temu etykieta
        // zawsze ma całą wysokość komórki i nigdy nie zostaje przycięta.
        Box(Modifier.fillMaxWidth().weight(1f)) {
            val lbl = e?.shift?.code ?: ""
            if (lbl.isNotEmpty()) {
                Text(
                    lbl,
                    // Pod numerem dnia, nie na nim — w wąskich komórkach nachodziły na siebie.
                    Modifier.align(Alignment.BottomCenter).padding(bottom = 3.dp),
                    fontSize = if (lbl.length > 3) 11.sp else 18.sp,
                    fontWeight = FontWeight.Bold, color = sw.text, maxLines = 1
                )
            }
            Row(
                Modifier.align(Alignment.TopStart).fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (swieto) {
                    Box(
                        Modifier.size(21.dp).clip(RoundedCornerShape(11.dp)).background(SwietoKolor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${date.dayOfMonth}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                } else {
                    Text(
                        "${date.dayOfMonth}", fontSize = 12.sp,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                        color = when (kind) {
                            DayKind.SOBOTA -> SatColor
                            DayKind.NIEDZIELA, DayKind.SWIETO -> SunColor
                            else -> OnBg
                        }
                    )
                }
                Spacer(Modifier.weight(1f))
                if (e?.deviation == true) Box(Modifier.padding(start = 2.dp).size(6.dp).clip(RoundedCornerShape(2.dp)).background(DevColor))
                if (events > 0) Box(Modifier.padding(start = 2.dp).size(6.dp).clip(RoundedCornerShape(3.dp)).background(EventColor))
            }
        }
    }
}

@Composable
private fun Palette(vm: Vm, s: UiState, otwarta: Boolean, przelacz: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .background(Surface1).padding(start = 8.dp, end = 8.dp, top = 2.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Pasek zawsze widoczny: co maluję, sterowanie nadgodzinami i cofanie.
        Row(
            Modifier.fillMaxWidth().clickable(onClick = przelacz).padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (otwarta) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                if (otwarta) "schowaj paletę" else "wybierz, czym malujesz",
                tint = Accent, modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    toolName(s), fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    color = if (s.painting) OnBg else OnFaint, maxLines = 1
                )
                Text(
                    if (s.painting) "malujesz — dotykaj dni" else "podgląd — dotknięcia nic nie zmieniają",
                    fontSize = 8.sp, color = if (s.painting) Accent else OnFaint
                )
            }

            // Blokada przed przypadkową zmianą grafiku.
            Row(
                Modifier.clip(RoundedCornerShape(11.dp))
                    .background(if (s.painting) Color(0xFF4A3410) else Surface2)
                    .border(
                        if (s.painting) 2.dp else 1.dp,
                        if (s.painting) Accent else Surface3,
                        RoundedCornerShape(11.dp)
                    )
                    .clickable { vm.setPainting(!s.painting) }
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (s.painting) Icons.Default.Edit else Icons.Default.Lock,
                    if (s.painting) "zablokuj malowanie" else "odblokuj malowanie",
                    tint = if (s.painting) Accent else OnMuted,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    "MALUJ", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    color = if (s.painting) Accent else OnMuted
                )
            }
            Spacer(Modifier.width(4.dp))

            TextButton(onClick = { vm.undo() }, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)) {
                Icon(Icons.Default.Undo, null, tint = OnMuted, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(3.dp)); Text("Cofnij", fontSize = 10.sp, color = OnMuted)
            }
        }

        if (!otwarta) return@Column

        // Po wyborze paleta chowa się sama — od razu malujesz na pełnym kalendarzu.
        val wybierz: (Tool) -> Unit = { t -> vm.pick(t); przelacz() }

        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Tool(s, Tool.I, "I", "6–14", Modifier.weight(1f), wybierz)
            Tool(s, Tool.II, "II", "14–22", Modifier.weight(1f), wybierz)
            Tool(s, Tool.III, "III", "22–6", Modifier.weight(1f), wybierz)
            Tool(s, Tool.W5, "w5", "wolne", Modifier.weight(1f), wybierz)
            Tool(s, Tool.WS, "wś", "wolne św.", Modifier.weight(1f), wybierz)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Tool(s, Tool.DWN, "DWN", "za niedz.", Modifier.weight(1f), wybierz)
            Tool(s, Tool.BWN, "bezw.", "wolna nd.", Modifier.weight(1f), wybierz)
            Tool(s, Tool.URLOP, "U", "urlop", Modifier.weight(1f), wybierz)
            Tool(s, Tool.DEV, "odb.", "od schem.", Modifier.weight(1f), wybierz)
            Tool(s, Tool.ERASE, "×", "wyczyść", Modifier.weight(1f), wybierz)
        }
        // Nadgodziny w menu razem z godzinami i stawką. Ustawiasz je tutaj,
        // a dopiero dotknięcie napisu NADGODZINY wybiera narzędzie i chowa paletę.
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
            val on = s.tool == Tool.OT
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(11.dp))
                    .background(if (on) Color(0xFF4A3410) else Surface2)
                    .border(if (on) 2.dp else 0.dp, if (on) OtColor100 else Color.Transparent, RoundedCornerShape(11.dp))
                    .clickable { wybierz(Tool.OT) }.padding(horizontal = 9.dp, vertical = 8.dp)
            ) {
                Text("NADGODZINY", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (on) OtColor100 else OnMuted)
                Text("dotknij i maluj dni", fontSize = 8.sp, color = OnFaint, maxLines = 1)
            }
            Row(
                Modifier.clip(RoundedCornerShape(11.dp)).background(Surface2).padding(horizontal = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { vm.otMinus() }, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.Remove, "mniej", tint = OnBg, modifier = Modifier.size(16.dp))
                }
                Text("${s.otHours}h", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = OnBg)
                IconButton(onClick = { vm.otPlus() }, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.Add, "więcej", tint = OnBg, modifier = Modifier.size(16.dp))
                }
            }
            Box(
                Modifier.clip(RoundedCornerShape(11.dp))
                    .background(if (s.otRate == OtRate.P100) Color(0xFF4A3410) else Surface2)
                    .clickable { vm.toggleRate() }.padding(horizontal = 12.dp, vertical = 9.dp)
            ) {
                Text("${s.otRate.percent}%", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    color = if (s.otRate == OtRate.P100) OtColor100 else OnMuted)
            }
        }
        Spacer(Modifier.height(2.dp))
    }
}

@Composable
private fun Tool(s: UiState, t: Tool, big: String, small: String, m: Modifier, wybierz: (Tool) -> Unit) {
    val on = s.tool == t
    val sw = when (t) {
        Tool.I -> Palette.byId(s.colors["I"], s.motyw); Tool.II -> Palette.byId(s.colors["II"], s.motyw)
        Tool.III -> Palette.byId(s.colors["III"], s.motyw); Tool.W5 -> Palette.byId(s.colors["w5"], s.motyw)
        Tool.WS -> Palette.byId(s.colors["wś"], s.motyw); Tool.DWN -> Palette.byId(s.colors["DWN"], s.motyw)
        Tool.BWN -> Palette.byId(s.colors["bezw."], s.motyw); Tool.URLOP -> Palette.byId(s.colors["U"], s.motyw)
        else -> Palette.byId("grafit", s.motyw)
    }
    Column(
        m.clip(RoundedCornerShape(11.dp))
            .background(if (on) sw.fill else Surface2)
            .border(if (on) 2.dp else 0.dp, if (on) sw.text else Color.Transparent, RoundedCornerShape(11.dp))
            .clickable { wybierz(t) }.padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(big, fontSize = if (big.length > 3) 9.sp else 14.sp, fontWeight = FontWeight.Bold, color = if (on) sw.text else OnMuted, maxLines = 1)
        Text(small, fontSize = 7.sp, color = if (on) sw.text.copy(alpha = .75f) else OnFaint, maxLines = 1, lineHeight = 8.sp)
    }
}

private fun toolName(s: UiState): String = when (s.tool) {
    Tool.I -> "ZMIANA I"; Tool.II -> "ZMIANA II"; Tool.III -> "ZMIANA III"
    Tool.W5 -> "WOLNE"; Tool.WS -> "WOLNE ŚWIĘTO"; Tool.DWN -> "DZIEŃ ZA PRACUJĄCĄ NIEDZIELĘ"
    Tool.BWN -> "BEZWZGLĘDNIE WOLNA NIEDZIELA"; Tool.URLOP -> "URLOP"; Tool.L4 -> "ZWOLNIENIE"
    Tool.DEV -> "ODBIEG OD SCHEMATU"; Tool.ERASE -> "GUMKA"
    Tool.OT -> "NADGODZINY ${s.otHours}h ${s.otRate.percent}%"
}
