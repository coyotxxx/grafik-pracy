package pl.grafik.pracy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.grafik.pracy.domain.Holidays
import pl.grafik.pracy.domain.VacationSuggestion
import pl.grafik.pracy.ui.UiState
import pl.grafik.pracy.ui.Vm
import pl.grafik.pracy.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PLP = Locale.forLanguageTag("pl-PL")
private val DZIEN_MIES = DateTimeFormatter.ofPattern("d MMMM", PLP)
private val DZIEN_KR = DateTimeFormatter.ofPattern("d.MM", PLP)

/** „Kiedy wziąć urlop" — podpowiedzi, gdzie kilka dni daje długi blok wolnego. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanSheet(vm: Vm, s: UiState, onClose: () -> Unit) {
    val plan by vm.plan.collectAsState()
    LaunchedEffect(Unit) { vm.policzPlan() }

    // Pełna wysokość — lista nie ma się chować za krawędzią ekranu.
    val stanKarty = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = stanKarty,
        containerColor = Surface1,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Surface3) }
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Kiedy wziąć urlop", fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = OnBg)
            Text(
                "Miejsca, w których kilka dni urlopu daje długi ciąg wolnego. " +
                    "Liczone na rok do przodu z Twojego grafiku i świąt.",
                fontSize = 11.sp, color = OnMuted, lineHeight = 15.sp
            )

            when {
                !s.cfg.generate -> Info(
                    "Najpierw włącz „Wypełnij grafik z cyklu” na zakładce Cykl — " +
                        "bez grafiku nie ma z czego liczyć długich weekendów."
                )
                s.urlopBilans.zostalo <= 0 -> Info("Nie masz już dni urlopu do wykorzystania w tym roku.")
                plan.isEmpty() -> Info(
                    "W najbliższym roku nie ma układu, w którym urlop dawałby wyraźnie " +
                        "więcej wolnego niż kosztuje. Twój cykl i tak daje sporo dni wolnych."
                )
                else -> {
                    Text(
                        "Masz ${s.urlopBilans.zostalo} ${dniSlowoP(s.urlopBilans.zostalo)} do wykorzystania.",
                        fontSize = 11.sp, color = Accent
                    )
                    plan.forEach { p -> Propozycja(p, s.motyw) }
                }
            }
        }
    }
}

@Composable
private fun Info(tekst: String) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Surface2).padding(14.dp)) {
        Text(tekst, fontSize = 12.sp, color = OnMuted, lineHeight = 17.sp)
    }
}

@Composable
private fun Propozycja(p: VacationSuggestion, motyw: PaletteTheme) {
    val kolorUrlopu = Palette.byId("fiolet", motyw).text
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Surface2).padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${p.dlugosc}", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = SunColor)
                    Text(" dni wolnego pod rząd", fontSize = 12.sp, color = OnMuted,
                        modifier = Modifier.padding(bottom = 3.dp))
                }
                Text(
                    "${p.wolneOd.format(DZIEN_MIES)} – ${p.wolneDo.format(DZIEN_MIES)}",
                    fontSize = 13.sp, color = OnBg, fontWeight = FontWeight.Medium
                )
            }
            Column(
                Modifier.clip(RoundedCornerShape(11.dp)).background(Color0(kolorUrlopu))
                    .padding(horizontal = 11.dp, vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("${p.koszt}", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = kolorUrlopu)
                Text(dniSlowoP(p.koszt) + " urlopu", fontSize = 9.sp, color = kolorUrlopu.copy(alpha = .8f))
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.BeachAccess, null, Modifier.size(13.dp), tint = kolorUrlopu)
            Spacer(Modifier.width(7.dp))
            Text(
                "urlop: " + p.urlop.joinToString(", ") { it.format(DZIEN_KR) },
                fontSize = 11.sp, color = OnMuted
            )
        }
        if (p.swieta.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).clip(RoundedCornerShape(4.dp)).background(SunColor))
                Spacer(Modifier.width(10.dp))
                Text(
                    "w środku: " + p.swieta.mapNotNull { Holidays.nameOf(it) }.distinct().joinToString(", "),
                    fontSize = 11.sp, color = SunColor
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            "%.1f dnia wolnego za 1 dzień urlopu".format(p.oplacalnosc),
            fontSize = 10.sp, color = OnFaint
        )
    }
}

private fun Color0(c: androidx.compose.ui.graphics.Color) = c.copy(alpha = 0.16f)

private fun dniSlowoP(n: Int) = if (n == 1) "dzień" else "dni"
