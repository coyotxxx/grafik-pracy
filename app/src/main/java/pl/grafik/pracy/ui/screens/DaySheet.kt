package pl.grafik.pracy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.grafik.pracy.data.EventRow
import pl.grafik.pracy.domain.DayKind
import pl.grafik.pracy.domain.Holidays
import pl.grafik.pracy.domain.OtRate
import pl.grafik.pracy.ui.UiState
import pl.grafik.pracy.ui.Vm
import pl.grafik.pracy.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PLL = Locale.forLanguageTag("pl-PL")
private val DATA = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", PLL)

/** Karta dnia: co to za święto, co masz zaplanowane, notatki. Otwierana przytrzymaniem dnia. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DaySheet(vm: Vm, s: UiState, date: LocalDate, onClose: () -> Unit) {
    val entry = s.entries[date]
    val events = s.events[date].orEmpty()
    val swieto = Holidays.nameOf(date)
    val kind = Holidays.kindOf(date)

    var nowyTekst by remember(date) { mutableStateOf("") }
    var nowaGodzina by remember(date) { mutableStateOf("") }
    var przypomnij by remember(date) { mutableStateOf(true) }

    // Pełna wysokość — lista nie ma się chować za krawędzią ekranu.
    val stanKarty = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = stanKarty,
        containerColor = Surface1,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Surface3) }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                // Bez tego klawiatura zasłania pola i nie widać, co się wpisuje.
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                date.format(DATA).replaceFirstChar { it.uppercase() },
                fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = OnBg
            )

            // --- charakter dnia i święto ---
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Surface2).padding(14.dp)) {
                if (swieto != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(9.dp).clip(RoundedCornerShape(5.dp)).background(SunColor))
                        Spacer(Modifier.width(9.dp))
                        Text(swieto, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = SunColor)
                    }
                    Text("dzień ustawowo wolny od pracy", fontSize = 11.sp, color = OnMuted,
                        modifier = Modifier.padding(start = 18.dp, top = 2.dp))
                } else {
                    Text(
                        when (kind) {
                            DayKind.SOBOTA -> "Sobota"
                            DayKind.NIEDZIELA -> "Niedziela"
                            DayKind.SWIETO -> "Święto"
                            else -> "Dzień roboczy"
                        },
                        fontSize = 14.sp, color = OnMuted
                    )
                }
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = Surface3)
                Spacer(Modifier.height(10.dp))
                val sh = entry?.shift
                Text(
                    if (sh == null) "Brak wpisu w grafiku"
                    else buildString {
                        append(sh.label)
                        if (sh.from.isNotBlank()) append("  ${sh.from}–${sh.to}")
                    },
                    fontSize = 14.sp, color = OnBg
                )
                if ((entry?.otHours ?: 0) > 0) {
                    Text(
                        "nadgodziny: ${entry!!.otHours} h po ${entry.otRate.percent}%",
                        fontSize = 12.sp,
                        color = if (entry.otRate == OtRate.P100) OtColor100 else OtColor50
                    )
                }
                if (entry?.deviation == true) {
                    Text("odbieg od stałego schematu", fontSize = 11.sp, color = DevColor)
                }
            }

            // --- wydarzenia ---
            Text("WYDARZENIA", fontSize = 10.sp, color = OnFaint, fontWeight = FontWeight.Medium)

            if (events.isEmpty()) {
                Text("Nic zaplanowanego.", fontSize = 12.sp, color = OnFaint)
            } else {
                events.forEach { ev -> EventRowItem(ev) { vm.deleteEvent(ev.id) } }
            }

            // --- dodawanie ---
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Surface2).padding(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Wpisujesz same cyfry — dwukropek wstawia się sam.
                    // Nie każda klawiatura ma „:", a kropka czy przecinek nie powinny przeszkadzać.
                    OutlinedTextField(
                        value = nowaGodzina,
                        onValueChange = { nowaGodzina = it.filter(Char::isDigit).take(4) },
                        placeholder = { Text("9 20", fontSize = 13.sp, color = OnFaint) },
                        singleLine = true,
                        modifier = Modifier.width(104.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = CzasTransformation,
                        colors = poleKolory(),
                        textStyle = androidx.compose.ui.text.TextStyle(color = OnBg, fontSize = 13.sp)
                    )
                    OutlinedTextField(
                        value = nowyTekst,
                        onValueChange = { nowyTekst = it.take(80) },
                        placeholder = { Text("np. fryzjer", fontSize = 13.sp, color = OnFaint) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        colors = poleKolory(),
                        textStyle = androidx.compose.ui.text.TextStyle(color = OnBg, fontSize = 13.sp)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                    Checkbox(
                        checked = przypomnij,
                        onCheckedChange = { przypomnij = it },
                        colors = CheckboxDefaults.colors(checkedColor = Accent, checkmarkColor = AccentOn, uncheckedColor = OnFaint)
                    )
                    Text("przypomnij dzień wcześniej", fontSize = 12.sp, color = OnMuted, modifier = Modifier.weight(1f))
                    Button(
                        onClick = {
                            vm.addEvent(date, sformatujCzas(nowaGodzina), nowyTekst, przypomnij)
                            nowyTekst = ""; nowaGodzina = ""
                        },
                        enabled = nowyTekst.isNotBlank(),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Accent, contentColor = AccentOn,
                            disabledContainerColor = Surface3, disabledContentColor = OnFaint
                        )
                    ) { Text("Dodaj", fontSize = 13.sp) }
                }
            }
        }
    }
}

@Composable
private fun EventRowItem(ev: EventRow, onDelete: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Surface2)
            .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(7.dp).clip(RoundedCornerShape(4.dp)).background(EventColor))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (ev.time.isBlank()) ev.text else "${ev.time}  ${ev.text}",
                fontSize = 14.sp, color = OnBg
            )
            if (ev.remind) Text("przypomnienie dzień wcześniej", fontSize = 10.sp, color = OnFaint)
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
            Icon(Icons.Default.Delete, "usuń", tint = OnFaint, modifier = Modifier.size(17.dp))
        }
    }
}

/**
 * Z samych cyfr robi godzinę: „920" → „9:20", „0920" → „09:20", „9" → „9".
 * Dwukropek jest tylko wyświetlany — w polu siedzą cyfry, więc kursor się nie gubi.
 */
private object CzasTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val cyfry = text.text.filter(Char::isDigit).take(4)
        if (cyfry.length <= 2) return TransformedText(AnnotatedString(cyfry), OffsetMapping.Identity)
        val pozycja = cyfry.length - 2
        val wynik = cyfry.substring(0, pozycja) + ":" + cyfry.substring(pozycja)
        return TransformedText(
            AnnotatedString(wynik),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int) = if (offset <= pozycja) offset else offset + 1
                override fun transformedToOriginal(offset: Int) = if (offset <= pozycja) offset else offset - 1
            }
        )
    }
}

/** Normalizacja do HH:MM przy zapisie. Puste zostaje puste — wydarzenie bez godziny. */
fun sformatujCzas(cyfry: String): String {
    val c = cyfry.filter(Char::isDigit).take(4)
    if (c.isEmpty()) return ""
    val (g, m) = when (c.length) {
        1, 2 -> c.toInt() to 0
        else -> c.dropLast(2).toInt() to c.takeLast(2).toInt()
    }
    return "%02d:%02d".format(g.coerceIn(0, 23), m.coerceIn(0, 59))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun poleKolory() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Accent,
    unfocusedBorderColor = Surface3,
    cursorColor = Accent,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent
)
