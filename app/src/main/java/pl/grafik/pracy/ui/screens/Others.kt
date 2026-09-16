package pl.grafik.pracy.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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

@Composable
fun SummaryScreen(vm: Vm) {
    val s by vm.state.collectAsState()
    val st = s.stats
    Column(Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {

        Text("Podsumowanie", fontSize = 21.sp, fontWeight = FontWeight.SemiBold, color = OnBg)
        Text("${s.ym.monthValue}/${s.ym.year} · Brygada ${s.cfg.brigade}", fontSize = 12.sp, color = OnMuted)

        Card(Surface1) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text("RAZEM", fontSize = 10.sp, color = OnFaint, fontWeight = FontWeight.Medium)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("${st.worked}", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = OnBg)
                        Text(" h", fontSize = 14.sp, color = OnMuted, modifier = Modifier.padding(bottom = 5.dp))
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("norma", fontSize = 10.sp, color = OnFaint)
                    Text("${st.norm} h", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = OnMuted)
                }
            }
            Spacer(Modifier.height(10.dp))
            val pct = if (st.norm > 0) (st.worked * 100f / st.norm) else 0f
            LinearProgressIndicator(
                progress = { (pct / 100f).coerceIn(0f, 1.3f).coerceAtMost(1f) },
                modifier = Modifier.fillMaxWidth().height(9.dp).clip(RoundedCornerShape(5.dp)),
                color = if (st.diff >= 0) OtColor100 else Palette.byId("lazur").text,
                trackColor = Surface3
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (st.diff >= 0) "ponad normę +${st.diff} h  ·  ${pct.toInt()}%" else "brakuje ${-st.diff} h  ·  ${pct.toInt()}%",
                fontSize = 11.sp, color = OnMuted
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("${st.ot100}", "nadgodz. 100%", OtColor100, Modifier.weight(1f))
            Stat("${st.ot50}", "nadgodz. 50%", OtColor50, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("${st.sundayWork}", "prac. niedziele", SunColor, Modifier.weight(1f))
            Stat("${st.holidayWork}", "prac. święta", Palette.byId("malina").text, Modifier.weight(1f))
            Stat("${st.saturdayWork}", "prac. soboty", SatColor, Modifier.weight(1f))
        }

        Card(Surface1) {
            Text("ROZKŁAD ZMIAN", fontSize = 10.sp, color = OnFaint, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(10.dp))
            listOf(Shift.I, Shift.II, Shift.III).forEach { sh ->
                val h = st.byShift[sh] ?: 0
                val max = (st.byShift.values.maxOrNull() ?: 1).coerceAtLeast(1)
                val sw = Palette.byId(s.colors[sh.code])
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 5.dp)) {
                    Text(sh.code, Modifier.width(28.dp), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = sw.text)
                    Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(Surface3)) {
                        Box(Modifier.fillMaxWidth(h.toFloat() / max).fillMaxHeight().background(sw.text))
                    }
                    Text("$h h", Modifier.width(46.dp), fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        color = OnBg, textAlign = TextAlign.End)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("${st.daysWork}", "dni pracy", OnBg, Modifier.weight(1f))
            Stat("${st.daysFree}", "dni wolnych", OnMuted, Modifier.weight(1f))
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun Card(bg: Color, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(bg).padding(16.dp), content = content)
}

@Composable
private fun Stat(big: String, small: String, c: Color, m: Modifier) {
    Column(m.clip(RoundedCornerShape(14.dp)).background(Surface1).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(big, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = c)
        Text(small, fontSize = 9.sp, color = OnFaint, textAlign = TextAlign.Center)
    }
}

@Composable
fun SetupScreen(vm: Vm, uvm: pl.grafik.pracy.ui.UpdateVm) {
    val s by vm.state.collectAsState()
    Column(Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {

        Text("Mój cykl", fontSize = 21.sp, fontWeight = FontWeight.SemiBold, color = OnBg)
        Text("Ustaw raz — grafik wyliczy się sam na każdy miesiąc.", fontSize = 12.sp, color = OnMuted)

        Text("SYSTEM PRACY", fontSize = 10.sp, color = OnFaint, fontWeight = FontWeight.Medium)
        CyclePattern.entries.forEach { p ->
            val on = s.cfg.pattern == p
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(if (on) Color(0xFF4A3410) else Surface1)
                    .border(if (on) 2.dp else 0.dp, if (on) Accent else Color.Transparent, RoundedCornerShape(14.dp))
                    .clickable { vm.saveConfig(s.cfg.copy(pattern = p)) }
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(p.label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (on) Accent else OnBg)
                        Text(p.desc, fontSize = 11.sp, color = OnMuted)
                        Text("cykl ${p.length} dni", fontSize = 10.sp, color = OnFaint)
                    }
                    if (on) Icon(Icons.Default.Check, null, tint = Accent)
                }
            }
        }

        Text("MOJA BRYGADA", fontSize = 10.sp, color = OnFaint, fontWeight = FontWeight.Medium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("A","B","C","D").forEach { b ->
                val on = s.cfg.brigade == b
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(13.dp))
                        .background(if (on) Color(0xFF4A3410) else Surface1)
                        .border(if (on) 2.dp else 0.dp, if (on) Accent else Color.Transparent, RoundedCornerShape(13.dp))
                        .clickable { vm.saveConfig(s.cfg.copy(brigade = b)) }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) { Text(b, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (on) Accent else OnMuted) }
            }
        }

        Text("PRZESUNIĘCIE CYKLU", fontSize = 10.sp, color = OnFaint, fontWeight = FontWeight.Medium)
        Text("Jeśli grafik nie zgadza się z Twoim, przesuń start cyklu.", fontSize = 11.sp, color = OnMuted)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { vm.saveConfig(s.cfg.copy(anchorIndex = (s.cfg.anchorIndex - 1 + s.cfg.pattern.length) % s.cfg.pattern.length)) },
                colors = ButtonDefaults.buttonColors(containerColor = Surface2)) { Text("−1 dzień", color = OnBg) }
            Text("pozycja ${s.cfg.anchorIndex}", Modifier.weight(1f), fontSize = 13.sp, color = OnBg, textAlign = TextAlign.Center)
            Button(onClick = { vm.saveConfig(s.cfg.copy(anchorIndex = (s.cfg.anchorIndex + 1) % s.cfg.pattern.length)) },
                colors = ButtonDefaults.buttonColors(containerColor = Surface2)) { Text("+1 dzień", color = OnBg) }
        }

        Text("PODGLĄD CYKLU", fontSize = 10.sp, color = OnFaint, fontWeight = FontWeight.Medium)
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Surface1).padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            s.cfg.pattern.days.forEach { code ->
                val sw = Palette.byId(s.colors[if (code == "w") "w5" else code])
                Box(Modifier.weight(1f).height(30.dp).clip(RoundedCornerShape(5.dp)).background(sw.fill),
                    contentAlignment = Alignment.Center) {
                    Text(if (code == "w") "·" else code, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = sw.text)
                }
            }
        }

        Button(onClick = { vm.resetMonth() }, modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Surface2), shape = RoundedCornerShape(14.dp)) {
            Text("Przywróć ten miesiąc do cyklu", color = OnBg, fontSize = 13.sp)
        }

        // --- aktualizacja aplikacji ---
        val u by uvm.state.collectAsState()
        Text("APLIKACJA", fontSize = 10.sp, color = OnFaint, fontWeight = FontWeight.Medium)
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Surface1).padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Wersja ${u.current}", fontSize = 14.sp, color = OnBg)
                    val podpis = when {
                        u.available != null -> "dostępna ${u.available!!.version} — pasek u góry"
                        u.upToDateMessage != null -> u.upToDateMessage!!
                        u.progress is pl.grafik.pracy.update.UpdateProgress.Checking -> "sprawdzam…"
                        else -> "aktualizacje z GitHuba"
                    }
                    Text(
                        podpis, fontSize = 11.sp,
                        color = if (u.available != null) Accent else OnMuted
                    )
                }
                Button(
                    onClick = { uvm.check(manual = true) },
                    enabled = u.progress !is pl.grafik.pracy.update.UpdateProgress.Checking,
                    colors = ButtonDefaults.buttonColors(containerColor = Surface2, contentColor = OnBg)
                ) { Text("Sprawdź", fontSize = 12.sp) }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
fun ColorsScreen(vm: Vm) {
    val s by vm.state.collectAsState()
    var sel by remember { mutableStateOf("I") }
    val types = listOf("I" to "Zmiana I", "II" to "Zmiana II", "III" to "Zmiana III",
        "w5" to "Wolne", "wś" to "Wolne święto", "DWN" to "Dzień za niedzielę",
        "bezw." to "Bezwzgl. wolna niedziela", "U" to "Urlop")

    Column(Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {

        Text("Kolory", fontSize = 21.sp, fontWeight = FontWeight.SemiBold, color = OnBg)
        Text("Każdy rodzaj dnia ma swój kolor. Wybierasz z gotowego zestawu.", fontSize = 12.sp, color = OnMuted)

        types.forEach { (code, name) ->
            val sw = Palette.byId(s.colors[code] ?: Palette.defaults[code])
            val on = sel == code
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp))
                    .background(if (on) Surface2 else Surface1)
                    .clickable { sel = code }.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(11.dp)).background(sw.fill)
                    .border(1.dp, sw.border, RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
                    Text(code, fontSize = if (code.length > 3) 8.sp else 13.sp, fontWeight = FontWeight.Bold, color = sw.text)
                }
                Spacer(Modifier.width(12.dp))
                Text(name, Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = OnBg)
                if (on) Icon(Icons.Default.Check, null, tint = Accent, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(Modifier.height(4.dp))
        Text("KOLOR DLA: ${types.firstOrNull { it.first == sel }?.second ?: ""}", fontSize = 10.sp, color = OnFaint, fontWeight = FontWeight.Medium)
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Surface1).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Palette.all.chunked(5).forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowItems.forEach { sw ->
                        val on = (s.colors[sel] ?: Palette.defaults[sel]) == sw.id
                        Column(Modifier.weight(1f).clickable {
                            vm.saveColors((s.colors + (sel to sw.id)))
                        }, horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(sw.text)
                                .border(if (on) 3.dp else 0.dp, if (on) OnBg else Color.Transparent, RoundedCornerShape(11.dp)))
                            Spacer(Modifier.height(3.dp))
                            Text(sw.label, fontSize = 8.sp, color = OnFaint, maxLines = 1)
                        }
                    }
                }
            }
        }
        Button(onClick = { vm.saveColors(Palette.defaults) }, modifier = Modifier.fillMaxWidth().height(46.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Surface2), shape = RoundedCornerShape(14.dp)) {
            Text("Przywróć domyślne", color = OnBg, fontSize = 13.sp)
        }
        Spacer(Modifier.height(20.dp))
    }
}
