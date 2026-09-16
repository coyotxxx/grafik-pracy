package pl.grafik.pracy.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.grafik.pracy.domain.*
import pl.grafik.pracy.ui.*
import pl.grafik.pracy.ui.theme.*

@Composable
fun SummaryScreen(vm: Vm) {
    val s by vm.state.collectAsState()
    var pokazPlan by remember { mutableStateOf(false) }
    if (pokazPlan) PlanSheet(vm, s) { pokazPlan = false }
    val st = s.stats
    Column(Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {

        var wybierzMiesiac by remember { mutableStateOf(false) }
        if (wybierzMiesiac) {
            MonthPickerSheet(
                biezacy = s.ym,
                onPick = { vm.setMonth(it); wybierzMiesiac = false },
                onClose = { wybierzMiesiac = false }
            )
        }

        Text("Podsumowanie", fontSize = 21.sp, fontWeight = FontWeight.SemiBold, color = OnBg)

        // Miesiąc do sprawdzenia. Po wejściu w zakładkę zawsze bieżący — reset robi onEnterSummary().
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.prevMonth() }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.ChevronLeft, "poprzedni miesiąc", tint = OnBg)
            }
            Row(
                Modifier.weight(1f).clip(RoundedCornerShape(11.dp)).background(Surface1)
                    .clickable { wybierzMiesiac = true }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(miesiacPl(s.ym), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnBg)
                Icon(Icons.Default.ArrowDropDown, "wybierz miesiąc", tint = OnMuted, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = { vm.nextMonth() }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.ChevronRight, "następny miesiąc", tint = OnBg)
            }
        }
        Text("Brygada ${s.cfg.brigade}", fontSize = 12.sp, color = OnMuted)

        // --- nadgodziny w okresach rozliczeniowych: zrobione / limit, jeden pasek na okres ---
        if (s.okresy.isNotEmpty()) {
            Card(Surface1) {
                Text(
                    "NADGODZINY W OKRESACH · ${s.ym.year}",
                    fontSize = 10.sp, color = OnFaint, fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(10.dp))
                s.okresy.forEachIndexed { i, okr ->
                    if (i > 0) Spacer(Modifier.height(10.dp))
                    LimitNadgodzin(okresLabel(okr.period), okr.ot, okr.limit, okr.biezacy)
                }

                // Limit roczny potrafi domknąć kwartał wcześniej — mówimy o tym tylko wtedy,
                // gdy naprawdę zostaje go mniej niż w bieżącym okresie.
                val teraz = s.okresy.firstOrNull { it.biezacy }
                val zostaloWRoku = (s.otLimitRok - s.otRok).coerceAtLeast(0)
                if (teraz != null && zostaloWRoku < teraz.zostalo) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "W ${s.ym.year} wykorzystane ${s.otRok} z ${s.otLimitRok} h — " +
                            "w tym okresie zostaje już tylko $zostaloWRoku h.",
                        fontSize = 10.sp, color = DevColor, lineHeight = 14.sp
                    )
                }
            }
        }

        Card(Surface1) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (st.biezacyMiesiac) "DO DZIŚ" else "RAZEM",
                        fontSize = 10.sp, color = OnFaint, fontWeight = FontWeight.Medium
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "${if (st.biezacyMiesiac) st.doDzis else st.rozliczone}",
                            fontSize = 34.sp, fontWeight = FontWeight.Bold, color = OnBg
                        )
                        Text(" h", fontSize = 14.sp, color = OnMuted, modifier = Modifier.padding(bottom = 5.dp))
                    }
                    if (st.biezacyMiesiac) {
                        Text("cały miesiąc wyjdzie ${st.rozliczone} h", fontSize = 10.sp, color = OnFaint)
                    }
                    if (st.urlopH > 0) {
                        Text(
                            "${st.worked} h przepracowane + ${st.urlopH} h urlopu",
                            fontSize = 10.sp, color = OnFaint
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("norma", fontSize = 10.sp, color = OnFaint)
                    Text("${st.norm} h", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = OnMuted)
                }
            }
            Spacer(Modifier.height(10.dp))
            val pct = if (st.norm > 0) (st.rozliczone * 100f / st.norm) else 0f
            LinearProgressIndicator(
                progress = { (pct / 100f).coerceIn(0f, 1.3f).coerceAtMost(1f) },
                modifier = Modifier.fillMaxWidth().height(9.dp).clip(RoundedCornerShape(5.dp)),
                color = if (st.diff >= 0) OtColor100 else Palette.byId("lazur", s.motyw).text,
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
            Stat("${st.holidayWork}", "prac. święta", Palette.byId("malina", s.motyw).text, Modifier.weight(1f))
            Stat("${st.saturdayWork}", "prac. soboty", SatColor, Modifier.weight(1f))
        }

        // --- URLOP ---
        val bil = s.urlopBilans
        val dzis = java.time.LocalDate.now()
        Card(Surface1) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text("URLOP ${s.ym.year}", fontSize = 10.sp, color = OnFaint, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "${bil.zostalo}", fontSize = 30.sp, fontWeight = FontWeight.Bold,
                            color = if (bil.zostalo <= 0) Danger else Palette.byId(s.colors["U"], s.motyw).text
                        )
                        Text(" ${dniSlowo(bil.zostalo)} zostało", fontSize = 13.sp, color = OnMuted,
                            modifier = Modifier.padding(bottom = 4.dp))
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("wykorzystane", fontSize = 10.sp, color = OnFaint)
                    Text("${bil.zuzyte} / ${bil.baza}", fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold, color = OnBg)
                }
            }

            // Dwie pule osobno — zaległa ma swój termin.
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PulaUrlopu(
                    "z ${s.ym.year - 1}", bil.zostaloZaleglego, bil.bazaZalegly,
                    if (bil.zostaloZaleglego > 0) DevColor else OnMuted, Modifier.weight(1f)
                )
                PulaUrlopu(
                    "za ${s.ym.year}", bil.zostaloBiezacego, bil.bazaBiezacy,
                    Palette.byId(s.colors["U"], s.motyw).text, Modifier.weight(1f)
                )
            }

            if (bil.zostaloZaleglego > 0) {
                Spacer(Modifier.height(10.dp))
                val doTerminu = bil.dniDoTerminu(dzis)
                val poTerminie = doTerminu < 0
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(if (poTerminie) Color(0xFF3A1F1E) else Color(0xFF33301A))
                        .padding(horizontal = 11.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (poTerminie) "⚠" else "⏳", fontSize = 14.sp)
                    Spacer(Modifier.width(9.dp))
                    Text(
                        if (poTerminie)
                            "Termin na urlop zaległy minął ${bil.termin.format(DATA_KR)}. " +
                                "Masz jeszcze ${bil.zostaloZaleglego} ${dniSlowo(bil.zostaloZaleglego)} — dogadaj to z przełożonym."
                        else
                            "Urlop zaległy wykorzystaj do ${bil.termin.format(DATA_KR)} — " +
                                "zostało $doTerminu ${if (doTerminu == 1L) "dzień" else "dni"}.",
                        fontSize = 11.sp, color = if (poTerminie) Danger else DevColor, lineHeight = 15.sp
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                if (s.urlop.stanData != null && s.urlop.stanData!!.year == s.ym.year)
                    "licząc od stanu zapisanego ${s.urlop.stanData!!.format(DATA_KR)}"
                else "wymiar ${s.urlop.wymiar} dni" + if (s.urlop.zalegly > 0) " + ${s.urlop.zalegly} zaległe" else "",
                fontSize = 10.sp, color = OnFaint
            )

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { pokazPlan = true },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Surface2, contentColor = Accent)
            ) {
                Icon(Icons.Default.BeachAccess, null, Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Kiedy wziąć urlop", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }

            if (s.urlopMiesiaca.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Surface3)
                Spacer(Modifier.height(8.dp))
                Text("W TYM MIESIĄCU", fontSize = 10.sp, color = OnFaint, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                s.urlopMiesiaca.forEach { d ->
                    // Jaką zmianę urlop zastąpił — bierzemy z cyklu, bo wpis dnia już jej nie trzyma.
                    val zmiana = if (s.cfg.covers(d)) CycleGenerator.shiftFor(s.cfg, d) else null
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(7.dp).clip(RoundedCornerShape(4.dp))
                            .background(Palette.byId(s.colors["U"], s.motyw).text))
                        Spacer(Modifier.width(10.dp))
                        Text(d.format(DATA_DZIEN), fontSize = 13.sp, color = OnBg, modifier = Modifier.weight(1f))
                        if (zmiana != null && zmiana.isWork) {
                            val sw = Palette.byId(s.colors[zmiana.code], s.motyw)
                            Box(Modifier.clip(RoundedCornerShape(7.dp)).background(sw.fill)
                                .padding(horizontal = 9.dp, vertical = 3.dp)) {
                                Text("zm. ${zmiana.code}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = sw.text)
                            }
                        } else {
                            Text(if (zmiana == null) "poza cyklem" else "dzień wolny",
                                fontSize = 11.sp, color = OnFaint)
                        }
                    }
                }
            }
        }

        Card(Surface1) {
            Text("ROZKŁAD ZMIAN", fontSize = 10.sp, color = OnFaint, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(10.dp))
            listOf(Shift.I, Shift.II, Shift.III).forEach { sh ->
                val h = st.byShift[sh] ?: 0
                val d = st.daysByShift[sh] ?: 0
                val max = (st.byShift.values.maxOrNull() ?: 1).coerceAtLeast(1)
                val sw = Palette.byId(s.colors[sh.code], s.motyw)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 5.dp)) {
                    Text(sh.code, Modifier.width(28.dp), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = sw.text)
                    Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(Surface3)) {
                        Box(Modifier.fillMaxWidth(h.toFloat() / max).fillMaxHeight().background(sw.text))
                    }
                    Column(Modifier.width(66.dp), horizontalAlignment = Alignment.End) {
                        Text("$d ${dniSlowo(d)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = OnBg)
                        Text("$h h", fontSize = 11.sp, color = OnMuted)
                    }
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
private fun LicznikDni(etykieta: String, wartosc: Int, mniej: () -> Unit, wiecej: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(etykieta, fontSize = 12.sp, color = OnMuted, modifier = Modifier.width(66.dp))
        Row(
            Modifier.clip(RoundedCornerShape(10.dp)).background(Surface2),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = mniej, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Remove, "mniej", tint = OnBg, modifier = Modifier.size(16.dp))
            }
            Text("$wartosc", Modifier.width(30.dp), fontSize = 15.sp, fontWeight = FontWeight.Bold,
                color = OnBg, textAlign = TextAlign.Center)
            IconButton(onClick = wiecej, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Add, "więcej", tint = OnBg, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(dniSlowo(wartosc), fontSize = 11.sp, color = OnFaint)
    }
}

@Composable
private fun PulaUrlopu(etykieta: String, zostalo: Int, baza: Int, kolor: Color, m: Modifier) {
    Column(m.clip(RoundedCornerShape(11.dp)).background(Surface2).padding(horizontal = 11.dp, vertical = 9.dp)) {
        Text(etykieta, fontSize = 9.sp, color = OnFaint)
        Row(verticalAlignment = Alignment.Bottom) {
            Text("$zostalo", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = kolor)
            Text(" z $baza", fontSize = 11.sp, color = OnFaint, modifier = Modifier.padding(bottom = 2.dp))
        }
    }
}

@Composable
private fun Pasek(ile: Int, z: Int, kolor: Color) {
    LinearProgressIndicator(
        progress = { if (z > 0) (ile.toFloat() / z).coerceIn(0f, 1f) else 0f },
        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
        color = kolor,
        trackColor = Surface3
    )
}

/** Wykorzystanie limitu nadgodzin. Na wyczerpaniu robi się czerwono, żeby rzucało się w oczy. */
@Composable
private fun LimitNadgodzin(nazwa: String, ile: Int, limit: Int, biezacy: Boolean = false) {
    val pelny = limit > 0 && ile >= limit
    val blisko = limit > 0 && ile >= limit * 0.9
    val kolor = when {
        pelny -> Danger
        blisko -> DevColor
        else -> OtColor100
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            nazwa, fontSize = 12.sp,
            color = if (biezacy) Accent else OnMuted,
            fontWeight = if (biezacy) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        Text("$ile / $limit h", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = kolor)
    }
    Spacer(Modifier.height(4.dp))
    Pasek(ile, limit, kolor)
    if (pelny) {
        Text(
            "Limit wyczerpany.", fontSize = 10.sp, color = Danger,
            modifier = Modifier.padding(top = 3.dp)
        )
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
    Column(
        Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState())
            .imePadding()                       // klawiatura nie może zasłaniać pól z normami
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        Text("Mój cykl", fontSize = 21.sp, fontWeight = FontWeight.SemiBold, color = OnBg)
        Text("Ustaw raz — grafik wyliczy się sam na każdy miesiąc.", fontSize = 12.sp, color = OnMuted)

        // Domyślnie WYŁĄCZONE — świeża aplikacja ma pusty kalendarz.
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(if (s.cfg.generate) Surface2 else Surface1).padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Wypełnij grafik z cyklu", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = OnBg)
                    Text(
                        if (s.cfg.generate) "Włączone — kalendarz liczy zmiany sam"
                        else "Wyłączone — kalendarz jest pusty, malujesz sam",
                        fontSize = 11.sp, color = if (s.cfg.generate) Accent else OnFaint
                    )
                }
                Switch(
                    checked = s.cfg.generate,
                    onCheckedChange = { wl ->
                        // Przy włączeniu domyślnie rok od bieżącego miesiąca — nie w nieskończoność.
                        vm.saveConfig(
                            s.cfg.copy(
                                generate = wl,
                                genFrom = s.cfg.genFrom ?: java.time.YearMonth.now(),
                                genTo = s.cfg.genTo ?: java.time.YearMonth.now().plusMonths(11)
                            )
                        )
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = AccentOn, checkedTrackColor = Accent)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Włącz dopiero, gdy poniżej ustawisz swój system pracy i brygadę. " +
                    "Dni, które wpiszesz ręcznie, zawsze mają pierwszeństwo przed cyklem.",
                fontSize = 10.sp, color = OnFaint, lineHeight = 14.sp
            )
        }

        // zakres, na jaki cykl ma wypełnić kalendarz
        if (s.cfg.generate) {
            val teraz = java.time.YearMonth.now()
            val od = s.cfg.genFrom ?: teraz
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Surface1).padding(14.dp)) {
                Text("NA JAKI OKRES", fontSize = 10.sp, color = OnFaint, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Od:", fontSize = 13.sp, color = OnMuted)
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { vm.saveConfig(s.cfg.copy(genFrom = od.minusMonths(1))) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Surface2, contentColor = OnBg)
                    ) { Text("−", fontSize = 15.sp) }
                    Text(
                        miesiacPl(od), Modifier.weight(1f), fontSize = 13.sp,
                        color = OnBg, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium
                    )
                    Button(
                        onClick = { vm.saveConfig(s.cfg.copy(genFrom = od.plusMonths(1))) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Surface2, contentColor = OnBg)
                    ) { Text("+", fontSize = 15.sp) }
                }
                Spacer(Modifier.height(10.dp))
                Text("Na ile:", fontSize = 13.sp, color = OnMuted)
                Spacer(Modifier.height(6.dp))
                val opcje = listOf(1 to "1 mies.", 3 to "3 mies.", 6 to "6 mies.", 12 to "rok", 0 to "bez końca")
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    opcje.forEach { (n, etykieta) ->
                        val wybrane = if (n == 0) s.cfg.genTo == null
                        else s.cfg.genTo == od.plusMonths((n - 1).toLong())
                        Box(
                            Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                                .background(if (wybrane) Color(0xFF4A3410) else Surface2)
                                .border(if (wybrane) 2.dp else 0.dp, if (wybrane) Accent else Color.Transparent, RoundedCornerShape(10.dp))
                                .clickable {
                                    vm.saveConfig(
                                        s.cfg.copy(
                                            genFrom = od,
                                            genTo = if (n == 0) null else od.plusMonths((n - 1).toLong())
                                        )
                                    )
                                }
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(etykieta, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                                color = if (wybrane) Accent else OnMuted, maxLines = 1)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    if (s.cfg.genTo == null) "Cykl wypełnia kalendarz od ${miesiacPlD(od)} bez ograniczenia."
                    else "Cykl wypełnia kalendarz od ${miesiacPlD(od)} do ${miesiacPlD(s.cfg.genTo!!)}.",
                    fontSize = 10.sp, color = OnFaint, lineHeight = 14.sp
                )
            }
        }

        // przypomnienia o wydarzeniach
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Surface1).padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Przypomnienia o wydarzeniach", fontSize = 14.sp, color = OnBg)
                    Text(
                        if (s.remindOn) "dzień wcześniej o ${"%02d".format(s.remindHour)}:00" else "wyłączone",
                        fontSize = 11.sp, color = if (s.remindOn) Accent else OnFaint
                    )
                }
                Switch(
                    checked = s.remindOn,
                    onCheckedChange = { vm.setReminders(it, s.remindHour) },
                    colors = SwitchDefaults.colors(checkedThumbColor = AccentOn, checkedTrackColor = Accent)
                )
            }
            if (s.remindOn) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Godzina:", fontSize = 12.sp, color = OnMuted)
                    Spacer(Modifier.width(10.dp))
                    Button(
                        onClick = { vm.setReminders(true, (s.remindHour + 23) % 24) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Surface2, contentColor = OnBg)
                    ) { Text("−", fontSize = 15.sp) }
                    Text("${"%02d".format(s.remindHour)}:00", Modifier.weight(1f), fontSize = 14.sp,
                        color = OnBg, textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
                    Button(
                        onClick = { vm.setReminders(true, (s.remindHour + 1) % 24) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Surface2, contentColor = OnBg)
                    ) { Text("+", fontSize = 15.sp) }
                }
                Text(
                    "Wieczorem dostaniesz listę wszystkiego, co masz zaplanowane na jutro. " +
                        "Ustaw godzinę tak, żeby nie trafiała w Twoją zmianę.",
                    fontSize = 10.sp, color = OnFaint, lineHeight = 14.sp, modifier = Modifier.padding(top = 6.dp)
                )
            }
        }

        Text("SYSTEM PRACY", fontSize = 10.sp, color = OnFaint, fontWeight = FontWeight.Medium)
        CyclePattern.entries.forEach { p ->
            val on = s.cfg.pattern == p
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(if (on) Color(0xFF4A3410) else Surface1)
                    .border(if (on) 2.dp else 0.dp, if (on) Accent else Color.Transparent, RoundedCornerShape(14.dp))
                    .clickable {
                        val kroki = if (p.weekAligned) p.weeks else p.length
                        vm.saveConfig(s.cfg.copy(pattern = p, anchorIndex = s.cfg.anchorIndex % kroki))
                    }
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

        Text("KIERUNEK ROTACJI", fontSize = 10.sp, color = OnFaint, fontWeight = FontWeight.Medium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(false, true).forEach { odwrotnie ->
                val on = s.cfg.reverse == odwrotnie
                val podglad = CycleGenerator.rotationLabel(s.cfg.copy(reverse = odwrotnie))
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(13.dp))
                        .background(if (on) Color(0xFF4A3410) else Surface1)
                        .border(if (on) 2.dp else 0.dp, if (on) Accent else Color.Transparent, RoundedCornerShape(13.dp))
                        .clickable { vm.saveConfig(s.cfg.copy(reverse = odwrotnie)) }
                        .padding(vertical = 12.dp, horizontal = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(podglad, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                        color = if (on) Accent else OnBg, maxLines = 1)
                    Text(if (odwrotnie) "odwrotnie" else "normalnie", fontSize = 10.sp, color = OnFaint)
                }
            }
        }
        Text(
            "Wybierz tę kolejność, w której naprawdę chodzisz. Na kafelkach widać, " +
                "co da każdy wariant dla wybranego wyżej systemu pracy.",
            fontSize = 10.sp, color = OnFaint, lineHeight = 14.sp
        )

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

        val tygodniowy = s.cfg.pattern.weekAligned
        Text(
            if (tygodniowy) "PRZESUNIĘCIE — CO TYDZIEŃ" else "PRZESUNIĘCIE CYKLU",
            fontSize = 10.sp, color = OnFaint, fontWeight = FontWeight.Medium
        )
        Text(
            if (tygodniowy)
                "Ten cykl jest przywiązany do tygodnia — soboty i niedziele zawsze wolne. " +
                    "Przesuwasz go o całe tygodnie, czyli zmieniasz, na której zmianie jesteś w tym tygodniu."
            else "Jeśli grafik nie zgadza się z Twoim, przesuń start cyklu.",
            fontSize = 11.sp, color = OnMuted, lineHeight = 15.sp
        )
        val krokow = if (tygodniowy) s.cfg.pattern.weeks else s.cfg.pattern.length
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { vm.saveConfig(s.cfg.copy(anchorIndex = (s.cfg.anchorIndex - 1 + krokow) % krokow)) },
                colors = ButtonDefaults.buttonColors(containerColor = Surface2)
            ) { Text(if (tygodniowy) "−1 tydzień" else "−1 dzień", color = OnBg) }
            Text(
                if (tygodniowy) "tydzień ${s.cfg.anchorIndex + 1} z $krokow" else "pozycja ${s.cfg.anchorIndex}",
                Modifier.weight(1f), fontSize = 13.sp, color = OnBg, textAlign = TextAlign.Center
            )
            Button(
                onClick = { vm.saveConfig(s.cfg.copy(anchorIndex = (s.cfg.anchorIndex + 1) % krokow)) },
                colors = ButtonDefaults.buttonColors(containerColor = Surface2)
            ) { Text(if (tygodniowy) "+1 tydzień" else "+1 dzień", color = OnBg) }
        }

        Text("PODGLĄD CYKLU", fontSize = 10.sp, color = OnFaint, fontWeight = FontWeight.Medium)
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Surface1).padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            CycleGenerator.days(s.cfg).forEach { code ->
                val sw = Palette.byId(s.colors[if (code == "w") "w5" else code], s.motyw)
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

        // --- czas pracy: okres rozliczeniowy i normy ---
        Text("CZAS PRACY", fontSize = 10.sp, color = OnFaint, fontWeight = FontWeight.Medium)
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Surface1).padding(14.dp)) {
            val okresTeraz = Settlement.periodOf(s.ym, s.okres)

            Text("Okres rozliczeniowy", fontSize = 13.sp, color = OnBg)
            Text(
                "Zakład rozlicza godziny w całym okresie, nie w pojedynczym miesiącu — " +
                    "niedobór z jednego miesiąca odrabiasz w kolejnym.",
                fontSize = 10.sp, color = OnFaint, lineHeight = 14.sp
            )
            Spacer(Modifier.height(8.dp))
            listOf(
                listOf(1 to "miesiąc", 3 to "kwartał", 4 to "4 miesiące"),
                listOf(6 to "pół roku", 12 to "rok")
            ).forEach { rzad ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 6.dp)) {
                    rzad.forEach { (ile, nazwa) ->
                        val on = s.okres.months == ile
                        Box(
                            Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                                .background(if (on) Color(0xFF4A3410) else Surface2)
                                .border(if (on) 2.dp else 0.dp, if (on) Accent else Color.Transparent, RoundedCornerShape(10.dp))
                                .clickable { vm.saveSettlement(s.okres.copy(months = ile)) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(nazwa, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = if (on) Accent else OnMuted)
                        }
                    }
                    repeat(3 - rzad.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Text(
                "Bieżący okres: ${okresLabel(okresTeraz)}",
                fontSize = 11.sp, color = Accent
            )

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = Surface3)
            Spacer(Modifier.height(12.dp))

            Text("Limit nadgodzin", fontSize = 13.sp, color = OnBg)
            Spacer(Modifier.height(4.dp))
            Text(
                "Ustawowy wynika z art. 131 KP — 8 h na każdy pełny tydzień okresu. " +
                    "Obok wpisz limit swojego zakładu; gdy jest wpisany, obowiązuje zamiast ustawowego.",
                fontSize = 10.sp, color = OnFaint, lineHeight = 14.sp
            )
            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                Text(
                    "ustawowo", fontSize = 9.sp, color = OnFaint,
                    modifier = Modifier.width(62.dp), textAlign = TextAlign.End
                )
                Text(
                    "zakład", fontSize = 9.sp, color = OnFaint,
                    modifier = Modifier.width(118.dp), textAlign = TextAlign.Center
                )
            }

            Settlement.periodsOfYear(s.ym.year, s.okres).forEach { okr ->
                val klucz = Settlement.key(okr)
                val ustawowy = Settlement.otLimit(okr)
                val teraz = okr.from == okresTeraz.from
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            okresLabel(okr), fontSize = 13.sp,
                            color = if (teraz) Accent else OnBg,
                            fontWeight = if (teraz) FontWeight.SemiBold else FontWeight.Normal
                        )
                        Text("${okr.weeks} tygodni", fontSize = 10.sp, color = OnFaint)
                    }
                    Text(
                        "$ustawowy h", fontSize = 13.sp, fontWeight = FontWeight.Medium,
                        color = OnMuted, modifier = Modifier.width(62.dp), textAlign = TextAlign.End
                    )
                    // Klucz remembera to sam okres, nigdy zapisywana wartość — opóźniona
                    // emisja z DataStore przestawiałaby cyfry w trakcie pisania.
                    var wpis by remember(klucz) {
                        mutableStateOf(s.okres.otLimitPeriods[klucz]?.toString() ?: "")
                    }
                    OutlinedTextField(
                        value = wpis,
                        onValueChange = { v ->
                            val czyste = v.filter(Char::isDigit).take(3)
                            wpis = czyste
                            val mapa = s.okres.otLimitPeriods.toMutableMap()
                            val h = czyste.toIntOrNull()
                            if (h != null && h > 0) mapa[klucz] = h else mapa.remove(klucz)
                            vm.saveSettlement(s.okres.copy(otLimitPeriods = mapa))
                        },
                        placeholder = { Text("—", fontSize = 13.sp, color = OnFaint) },
                        suffix = { Text("h", fontSize = 12.sp, color = OnMuted) },
                        singleLine = true,
                        modifier = Modifier.width(118.dp).padding(start = 8.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = poleLiczbowe(),
                        textStyle = androidx.compose.ui.text.TextStyle(color = OnBg, fontSize = 14.sp)
                    )
                }
            }
            Text(
                "Puste pole = obowiązuje limit ustawowy.",
                fontSize = 10.sp, color = OnFaint, modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("W roku — ustawowo", fontSize = 12.sp, color = OnBg)
                    Text(
                        "Art. 151 § 3 KP. Zamyka limity okresowe, jeśli wyczerpie się wcześniej.",
                        fontSize = 10.sp, color = OnFaint, lineHeight = 14.sp
                    )
                }
                Text(
                    "${Settlement.OT_LIMIT_YEAR} h",
                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnMuted,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        // --- urlop ---
        Text("URLOP", fontSize = 10.sp, color = OnFaint, fontWeight = FontWeight.Medium)
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Surface1).padding(14.dp)) {
            Text("Wymiar roczny", fontSize = 13.sp, color = OnBg)
            Text("20 dni do 10 lat stażu, 26 powyżej.", fontSize = 10.sp, color = OnFaint)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(20, 26).forEach { w ->
                    val on = s.urlop.wymiar == w
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                            .background(if (on) Color(0xFF4A3410) else Surface2)
                            .border(if (on) 2.dp else 0.dp, if (on) Accent else Color.Transparent, RoundedCornerShape(10.dp))
                            .clickable { vm.saveVacation(s.urlop.copy(wymiar = w)) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) { Text("$w dni", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = if (on) Accent else OnMuted) }
                }
                Row(
                    Modifier.weight(1.2f).clip(RoundedCornerShape(10.dp)).background(Surface2),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(onClick = { vm.saveVacation(s.urlop.copy(wymiar = s.urlop.wymiar - 1)) },
                        modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Remove, "mniej", tint = OnBg, modifier = Modifier.size(15.dp))
                    }
                    Text("${s.urlop.wymiar}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = OnBg)
                    IconButton(onClick = { vm.saveVacation(s.urlop.copy(wymiar = s.urlop.wymiar + 1)) },
                        modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Add, "więcej", tint = OnBg, modifier = Modifier.size(15.dp))
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Zaległy z poprzedniego roku", fontSize = 13.sp, color = OnBg)
                    Text("doliczany do wymiaru", fontSize = 10.sp, color = OnFaint)
                }
                Row(
                    Modifier.clip(RoundedCornerShape(10.dp)).background(Surface2),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { vm.saveVacation(s.urlop.copy(zalegly = s.urlop.zalegly - 1)) },
                        modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Remove, "mniej", tint = OnBg, modifier = Modifier.size(15.dp))
                    }
                    Text("${s.urlop.zalegly}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = OnBg)
                    IconButton(onClick = { vm.saveVacation(s.urlop.copy(zalegly = s.urlop.zalegly + 1)) },
                        modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Add, "więcej", tint = OnBg, modifier = Modifier.size(15.dp))
                    }
                }
            }
            // Jeśli apka zna poprzedni rok, podpowiada ile mogło przejść.
            if (s.urlop.zalegly == 0 && s.urlopSugestiaZaleglego > 0 && s.urlopPoprzedniRok.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "W ${s.ym.year - 1} wykorzystałeś ${s.urlopPoprzedniRok.size} " +
                            "${dniSlowo(s.urlopPoprzedniRok.size)} — mogło zostać ${s.urlopSugestiaZaleglego}.",
                        fontSize = 10.sp, color = OnFaint, modifier = Modifier.weight(1f), lineHeight = 14.sp
                    )
                    TextButton(onClick = { vm.saveVacation(s.urlop.copy(zalegly = s.urlopSugestiaZaleglego)) }) {
                        Text("Wpisz", fontSize = 11.sp, color = Accent)
                    }
                }
            }
            Text(
                "Zaległy trzeba wykorzystać do 30 września.",
                fontSize = 10.sp, color = DevColor, modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = Surface3)
            Spacer(Modifier.height(12.dp))

            // Prawdziwy stan trzyma kadrowa — tu go przepisujesz, a apka liczy dalej sama.
            Text("Stan z zakładu", fontSize = 13.sp, color = OnBg)
            Text(
                "Jeśli kadrowa poda Ci aktualne liczby, zapisz je tutaj. " +
                    "Od tego dnia aplikacja odlicza już sama, zdejmując najpierw urlop zaległy.",
                fontSize = 10.sp, color = OnFaint, lineHeight = 14.sp
            )
            Spacer(Modifier.height(10.dp))
            var stanB by remember(s.urlop.stanBiezacy) { mutableIntStateOf(s.urlop.stanBiezacy) }
            var stanZ by remember(s.urlop.stanZalegly) { mutableIntStateOf(s.urlop.stanZalegly) }
            LicznikDni("bieżący", stanB, { if (stanB > 0) stanB-- }, { stanB++ })
            Spacer(Modifier.height(6.dp))
            LicznikDni("zaległy", stanZ, { if (stanZ > 0) stanZ-- }, { stanZ++ })
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = {
                    vm.saveVacation(
                        s.urlop.copy(
                            stanData = java.time.LocalDate.now(),
                            stanBiezacy = stanB,
                            stanZalegly = stanZ
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = AccentOn)
            ) { Text("Zapisz stan na dziś", fontSize = 12.sp) }

            s.urlop.stanData?.let { d ->
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Zapisane ${d.format(DATA_KR)}: ${s.urlop.stanBiezacy} bieżącego, ${s.urlop.stanZalegly} zaległego",
                        fontSize = 11.sp, color = Accent, modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { vm.saveVacation(s.urlop.copy(stanData = null)) }) {
                        Text("Wyczyść", fontSize = 11.sp, color = OnMuted)
                    }
                }
            }
        }

        // --- czyszczenie danych ---
        var pytanie by remember { mutableStateOf(false) }
        Text("DANE", fontSize = 10.sp, color = OnFaint, fontWeight = FontWeight.Medium)
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Surface1).padding(14.dp)) {
            Text(
                "Grafik w kalendarzu jest wyliczany z cyklu — w pamięci zapisane są tylko dni, " +
                    "które sam zmieniłeś. Tu skasujesz je razem z resztą ustawień.",
                fontSize = 11.sp, color = OnMuted, lineHeight = 15.sp
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { pytanie = true },
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, Danger),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger)
            ) { Text("Wyczyść wszystkie dane", fontSize = 13.sp, fontWeight = FontWeight.Medium) }
        }

        if (pytanie) {
            AlertDialog(
                onDismissRequest = { pytanie = false },
                containerColor = Surface2,
                titleContentColor = OnBg,
                textContentColor = OnMuted,
                title = { Text("Wyczyścić wszystko?", fontWeight = FontWeight.SemiBold) },
                text = {
                    Column {
                        Text("Aplikacja wróci do stanu jak po instalacji. Zniknie:", fontSize = 13.sp)
                        Spacer(Modifier.height(8.dp))
                        listOf(
                            "wszystkie ręcznie wpisane dni i nadgodziny",
                            "historia wykryć pracy",
                            "zapisane miejsce pracy i sieć Wi-Fi",
                            "ustawienia cyklu, brygady i kolorów"
                        ).forEach { Text("•  $it", fontSize = 13.sp, lineHeight = 20.sp) }
                        Spacer(Modifier.height(10.dp))
                        Text("Tego nie da się cofnąć.", fontSize = 12.sp, color = Danger, fontWeight = FontWeight.Medium)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { vm.clearEverything(); pytanie = false }) {
                        Text("Wyczyść", color = Danger, fontWeight = FontWeight.SemiBold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pytanie = false }) { Text("Anuluj", color = OnMuted) }
                }
            )
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

private val PL_LOC = java.util.Locale.forLanguageTag("pl-PL")

/** 1 dzień, 2-4 dni, 5+ dni — żeby nie pisać „2 dzień". */
private fun dniSlowo(n: Int): String = if (n == 1) "dzień" else "dni"

private val DATA_KR = java.time.format.DateTimeFormatter.ofPattern("d.MM.yyyy", PL_LOC)
private val DATA_DZIEN = java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMMM", PL_LOC)

/** Mianownik — „Wrzesień 2026". Do samodzielnego wyświetlenia. */
private fun miesiacPl(ym: java.time.YearMonth): String =
    ym.month.getDisplayName(java.time.format.TextStyle.FULL_STANDALONE, PL_LOC)
        .replaceFirstChar { it.uppercase() } + " " + ym.year

/** Dopełniacz — „od września 2026 do sierpnia 2027". */
private fun miesiacPlD(ym: java.time.YearMonth): String =
    ym.month.getDisplayName(java.time.format.TextStyle.FULL, PL_LOC) + " " + ym.year

@Composable
private fun poleLiczbowe() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Accent,
    unfocusedBorderColor = Surface3,
    cursorColor = Accent,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent
)

/** „III kwartał 2026", „lipiec–wrzesień 2026" albo sam miesiąc — zależnie od długości okresu. */
fun okresLabel(p: Period): String {
    val n = p.months.size
    return when {
        n == 1 -> miesiacPl(p.from)
        n == 3 -> "${(p.from.monthValue - 1) / 3 + 1}. kwartał ${p.from.year}"
        n == 12 -> "rok ${p.from.year}"
        else -> p.from.month.getDisplayName(java.time.format.TextStyle.FULL_STANDALONE, PL_LOC) +
            "–" + p.to.month.getDisplayName(java.time.format.TextStyle.FULL_STANDALONE, PL_LOC) +
            " " + p.from.year
    }
}

/** Skrót miesiąca do siatki wyboru — „sty", „lut". */
private fun miesiacSkrot(ym: java.time.YearMonth): String =
    ym.month.getDisplayName(java.time.format.TextStyle.SHORT_STANDALONE, PL_LOC)
        .replaceFirstChar { it.uppercase() }.trimEnd('.')

/** Wskazanie miesiąca wprost — rok strzałkami, miesiąc z siatki. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthPickerSheet(
    biezacy: java.time.YearMonth,
    onPick: (java.time.YearMonth) -> Unit,
    onClose: () -> Unit
) {
    val stanKarty = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var rok by remember { mutableIntStateOf(biezacy.year) }
    val teraz = java.time.YearMonth.now()

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = stanKarty,
        containerColor = Surface1,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Surface3) }
    ) {
        Column(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Który miesiąc sprawdzamy", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = OnBg)

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { rok-- }) { Icon(Icons.Default.ChevronLeft, "poprzedni rok", tint = OnBg) }
                Text(
                    "$rok", Modifier.weight(1f), fontSize = 19.sp, fontWeight = FontWeight.Bold,
                    color = OnBg, textAlign = TextAlign.Center
                )
                IconButton(onClick = { rok++ }) { Icon(Icons.Default.ChevronRight, "następny rok", tint = OnBg) }
            }

            (0..3).forEach { wiersz ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..3).forEach { kol ->
                        val m = java.time.YearMonth.of(rok, wiersz * 3 + kol)
                        val wybrany = m == biezacy
                        Box(
                            Modifier.weight(1f).clip(RoundedCornerShape(11.dp))
                                .background(if (wybrany) Accent else Surface2)
                                .border(
                                    if (m == teraz && !wybrany) 1.dp else 0.dp,
                                    if (m == teraz && !wybrany) Accent else Color.Transparent,
                                    RoundedCornerShape(11.dp)
                                )
                                .clickable { onPick(m) }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                miesiacSkrot(m), fontSize = 13.sp, fontWeight = FontWeight.Medium,
                                color = if (wybrany) AccentOn else OnBg
                            )
                        }
                    }
                }
            }

            TextButton(onClick = { onPick(teraz) }) {
                Text("Wróć do bieżącego miesiąca", fontSize = 13.sp, color = Accent)
            }
        }
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
        Text("Zestaw kolorów", fontSize = 10.sp, color = OnFaint, fontWeight = FontWeight.Medium)
        PaletteTheme.entries.forEach { m ->
            val on = s.motyw == m
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(if (on) Color(0xFF4A3410) else Surface1)
                    .border(if (on) 2.dp else 0.dp, if (on) Accent else Color.Transparent, RoundedCornerShape(14.dp))
                    .clickable { vm.saveMotyw(m) }
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(m.label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        color = if (on) Accent else OnBg)
                    Text(m.opis, fontSize = 10.sp, color = OnFaint)
                }
                // podgląd: trzy zmiany + wolne w tym zestawie
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    listOf("I", "II", "III", "w5").forEach { kod ->
                        val sw = Palette.byId(s.colors[kod] ?: Palette.defaults[kod], m)
                        Box(
                            Modifier.size(width = 26.dp, height = 30.dp)
                                .clip(RoundedCornerShape(6.dp)).background(sw.fill)
                                .border(1.dp, sw.border, RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(kod, fontSize = if (kod.length > 2) 8.sp else 10.sp,
                                fontWeight = FontWeight.Bold, color = sw.text, maxLines = 1)
                        }
                    }
                }
                if (on) {
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.Check, null, tint = Accent, modifier = Modifier.size(18.dp))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text("Każdy rodzaj dnia ma swój kolor. Wybierasz z gotowego zestawu.", fontSize = 12.sp, color = OnMuted)

        types.forEach { (code, name) ->
            val sw = Palette.byId(s.colors[code] ?: Palette.defaults[code], s.motyw)
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
            Palette.all(s.motyw).chunked(5).forEach { rowItems ->
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
