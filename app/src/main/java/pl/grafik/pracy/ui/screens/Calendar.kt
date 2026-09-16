package pl.grafik.pracy.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@Composable
fun CalendarScreen(vm: Vm, onOpenDay: (LocalDate) -> Unit) {
    val s by vm.state.collectAsState()

    Column(Modifier.fillMaxSize().background(Bg)) {
        Header(vm, s)
        WeekHeader()
        Grid(s, onTap = { vm.tap(it) }, onLong = onOpenDay)
        Spacer(Modifier.weight(1f))
        Palette(vm, s)
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
            Text("Brygada ${s.cfg.brigade} · ${s.cfg.pattern.label}", fontSize = 11.sp, color = OnMuted, maxLines = 1)
        }
        IconButton(onClick = { vm.prevMonth() }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.ChevronLeft, "poprzedni", tint = OnBg) }
        IconButton(onClick = { vm.nextMonth() }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.ChevronRight, "następny", tint = OnBg) }
    }

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("${s.stats.worked}", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = OnBg)
        Text(" / ${s.stats.norm} h", fontSize = 12.sp, color = OnMuted)
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
private fun Grid(s: UiState, onTap: (LocalDate) -> Unit, onLong: (LocalDate) -> Unit) {
    val first = s.ym.atDay(1)
    val lead = first.dayOfWeek.value - 1
    val total = s.ym.lengthOfMonth()
    val weeks = ((lead + total + 6) / 7)
    val today = LocalDate.now()

    Column(Modifier.padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(weeks) { w ->
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(7) { i ->
                    val n = w * 7 + i - lead + 1
                    if (n < 1 || n > total) {
                        Box(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF1A1C22)))
                    } else {
                        val d = s.ym.atDay(n)
                        DayCell(d, s.entries[d], s.colors, d == today, Modifier.weight(1f), { onTap(d) }, { onLong(d) })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DayCell(
    date: LocalDate, e: DayEntry?, colors: Map<String, String>,
    isToday: Boolean, m: Modifier, onTap: () -> Unit, onLong: () -> Unit
) {
    val sw = Palette.byId(colors[e?.shift?.code] ?: Palette.defaults[e?.shift?.code])
    val kind = Holidays.kindOf(date)
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
        m.height(48.dp).clip(RoundedCornerShape(10.dp))
            .background(if (e?.shift == null) Surface1 else sw.fill)
            .border(bw, border, RoundedCornerShape(10.dp))
            .combinedClickable(onClick = onTap, onLongClick = onLong)
    ) {
        if ((e?.otHours ?: 0) > 0) {
            Box(Modifier.fillMaxWidth().background(if (e!!.otRate == OtRate.P100) OtColor100 else OtColor50)) {
                Text("+${e.otHours}h ${e.otRate.percent}%", Modifier.fillMaxWidth().padding(vertical = 1.dp),
                    fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2A1C06), textAlign = TextAlign.Center)
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("$${date.dayOfMonth}".removePrefix("$"), fontSize = 11.sp,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                color = when (kind) { DayKind.SOBOTA -> SatColor; DayKind.NIEDZIELA, DayKind.SWIETO -> SunColor; else -> OnBg })
            Spacer(Modifier.weight(1f))
            if (e?.deviation == true) Box(Modifier.size(6.dp).clip(RoundedCornerShape(2.dp)).background(DevColor))
            if (e?.note?.isNotEmpty() == true) Box(Modifier.padding(start = 2.dp).size(5.dp).clip(RoundedCornerShape(3.dp)).background(OnMuted))
        }
        val lbl = e?.shift?.code ?: ""
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            Text(lbl, fontSize = if (lbl.length > 3) 9.sp else 15.sp,
                fontWeight = FontWeight.Bold, color = sw.text, maxLines = 1)
        }
    }
}

@Composable
private fun Palette(vm: Vm, s: UiState) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .background(Surface1).padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(toolName(s), fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = OnMuted, modifier = Modifier.weight(1f))
            TextButton(onClick = { vm.undo() }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                Icon(Icons.Default.Undo, null, tint = OnMuted, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(3.dp)); Text("Cofnij", fontSize = 10.sp, color = OnMuted)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Tool(vm, s, Tool.I, "I", "6–14", Modifier.weight(1f))
            Tool(vm, s, Tool.II, "II", "14–22", Modifier.weight(1f))
            Tool(vm, s, Tool.III, "III", "22–6", Modifier.weight(1f))
            Tool(vm, s, Tool.W5, "w5", "wolne", Modifier.weight(1f))
            Tool(vm, s, Tool.WS, "wś", "wolne św.", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Tool(vm, s, Tool.DWN, "DWN", "za niedz.", Modifier.weight(1f))
            Tool(vm, s, Tool.BWN, "bezw.", "wolna nd.", Modifier.weight(1f))
            Tool(vm, s, Tool.URLOP, "U", "urlop", Modifier.weight(1f))
            Tool(vm, s, Tool.DEV, "odb.", "od schem.", Modifier.weight(1f))
            Tool(vm, s, Tool.ERASE, "×", "wyczyść", Modifier.weight(1f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
            val on = s.tool == Tool.OT
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(11.dp))
                    .background(if (on) Color(0xFF4A3410) else Surface2)
                    .border(if (on) 2.dp else 0.dp, if (on) OtColor100 else Color.Transparent, RoundedCornerShape(11.dp))
                    .clickable { vm.pick(Tool.OT) }.padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Text("NADGODZINY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (on) OtColor100 else OnMuted)
                Text("dotknij dzień", fontSize = 8.sp, color = OnFaint)
            }
            Row(
                Modifier.clip(RoundedCornerShape(11.dp)).background(Surface2).padding(horizontal = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { vm.otMinus() }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Remove, "mniej", tint = OnBg, modifier = Modifier.size(14.dp)) }
                Text("${s.otHours}h", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = OnBg)
                IconButton(onClick = { vm.otPlus() }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Add, "więcej", tint = OnBg, modifier = Modifier.size(14.dp)) }
            }
            Box(
                Modifier.clip(RoundedCornerShape(11.dp))
                    .background(if (s.otRate == OtRate.P100) Color(0xFF4A3410) else Surface2)
                    .clickable { vm.toggleRate() }.padding(horizontal = 10.dp, vertical = 7.dp)
            ) { Text("${s.otRate.percent}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (s.otRate == OtRate.P100) OtColor100 else OnMuted) }
        }
    }
}

@Composable
private fun Tool(vm: Vm, s: UiState, t: Tool, big: String, small: String, m: Modifier) {
    val on = s.tool == t
    val sw = when (t) {
        Tool.I -> Palette.byId(s.colors["I"]); Tool.II -> Palette.byId(s.colors["II"])
        Tool.III -> Palette.byId(s.colors["III"]); Tool.W5 -> Palette.byId(s.colors["w5"])
        Tool.WS -> Palette.byId(s.colors["wś"]); Tool.DWN -> Palette.byId(s.colors["DWN"])
        Tool.BWN -> Palette.byId(s.colors["bezw."]); Tool.URLOP -> Palette.byId(s.colors["U"])
        else -> Palette.byId("grafit")
    }
    Column(
        m.clip(RoundedCornerShape(11.dp))
            .background(if (on) sw.fill else Surface2)
            .border(if (on) 2.dp else 0.dp, if (on) sw.text else Color.Transparent, RoundedCornerShape(11.dp))
            .clickable { vm.pick(t) }.padding(vertical = 4.dp),
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
