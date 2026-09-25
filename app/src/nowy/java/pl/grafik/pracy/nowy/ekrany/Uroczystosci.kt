package pl.grafik.pracy.nowy.ekrany

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.grafik.pracy.data.EventRow
import pl.grafik.pracy.nowy.theme.*
import pl.grafik.pracy.nowy.ui.*
import pl.grafik.pracy.ui.Vm
import java.time.LocalDate
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

private val PL_UR = Locale.forLanguageTag("pl-PL")

/**
 * „Uroczystości" — urodziny, imieniny i rocznice w jednym miejscu.
 *
 * To samo da się dopisać przy konkretnym dniu w grafiku; ta zakładka jest po to,
 * żeby zobaczyć wszystkie naraz i dopisać kilka za jednym posiedzeniem.
 */
@Composable
fun EkranUroczystosci(vm: Vm, naPowrot: () -> Unit) {
    val s by vm.state.collectAsState()
    var dodaje by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        TloZPoswiata(Modifier.fillMaxSize())

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(
                    start = Dim.screenGutter, end = Dim.screenGutter,
                    top = gornaKrawedz(), bottom = dolnaKrawedz(22.dp)
                )
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            PowrotDoUstawien(naPowrot)

            val p by postepWejscia(Motion.RISE_MS)
            Column(Modifier.wejscie(p), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Uroczystości", style = GrafikType.h1.copy(fontSize = 26.sp, lineHeight = 26.sp),
                    color = Tokeny.ink)
                Text(
                    "Urodziny, imieniny i rocznice. Wracają co roku, więc wpisujesz je raz.",
                    fontSize = 12.sp, lineHeight = 18.sp,
                    fontFamily = Jakarta, color = Tokeny.inkMuted
                )
            }

            if (dodaje) {
                FormularzUroczystosci(vm) { dodaje = false }
            } else {
                PrzyciskGlowny("Dodaj uroczystość") { dodaje = true }
            }

            ListaUroczystosci(s.uroczystosci) { vm.deleteEvent(it) }
        }
    }
}

@Composable
private fun ListaUroczystosci(lista: List<EventRow>, naUsuniecie: (Long) -> Unit) {
    val p by postepWejscia(Motion.RISE_MS, 90)

    Column(
        Modifier.wejscie(p).fillMaxWidth()
            .clip(RoundedCornerShape(Dim.rCard))
            .background(Tokeny.surface)
            .border(1.dp, Tokeny.line, RoundedCornerShape(Dim.rCard))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text("ZAPISANE", Modifier.weight(1f), style = GrafikType.sectionLabel,
                color = Tokeny.inkFaint)
            Text(
                if (lista.isEmpty()) "nic nie wpisano" else "${lista.size}",
                fontSize = 10.sp, fontFamily = Jakarta, color = Tokeny.inkFaint
            )
        }

        if (lista.isEmpty()) {
            Text(
                "Dopisz pierwszą — pojawi się w grafiku w swoim dniu, co roku.",
                fontSize = 11.sp, lineHeight = 16.5.sp,
                fontFamily = Jakarta, color = Tokeny.inkMuted
            )
        }

        lista.forEach { u ->
            val data = remember(u.date) { runCatching { LocalDate.parse(u.date) }.getOrNull() }
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    Modifier.size(42.dp).clip(RoundedCornerShape(13.dp))
                        .background(Tokeny.uroczystosc.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        data?.dayOfMonth?.toString() ?: "?",
                        style = TextStyle(
                            fontSize = 15.sp, fontWeight = FontWeight.Bold,
                            fontFamily = Jakarta, fontFeatureSettings = TNUM
                        ),
                        color = Tokeny.uroczystosc
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(u.osoba.ifBlank { u.text }, style = GrafikType.cardTitle,
                        color = Tokeny.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        buildString {
                            append(etykietaRodzaju(u.rodzaj))
                            data?.let {
                                append(" · ${it.dayOfMonth} ")
                                append(it.month.getDisplayName(JavaTextStyle.FULL, PL_UR))
                            }
                            if (!u.coroczne) append(" · tylko raz")
                            if (u.remind) append(" · przypomnienie")
                        },
                        style = GrafikType.caption, color = Tokeny.inkMuted,
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                }
                PrzyciskKwadrat(
                    IkonaKosz, "Usuń uroczystość",
                    tlo = Color.Transparent, obrys = Tokeny.lineStrong,
                    kolorIkony = Tokeny.inkIkona
                ) { naUsuniecie(u.id) }
            }
        }
    }
}

@Composable
private fun FormularzUroczystosci(vm: Vm, naZamkniecie: () -> Unit) {
    val ctx = LocalContext.current
    val dzis = remember { LocalDate.now() }
    var rodzaj by remember { mutableStateOf("urodziny") }
    var osoba by remember { mutableStateOf("") }
    var dzien by remember { mutableStateOf("") }
    var miesiac by remember { mutableStateOf("") }
    var przypomnij by remember { mutableStateOf(true) }
    var coroczne by remember { mutableStateOf(true) }
    var zKontaktu by remember { mutableStateOf("") }

    val data = remember(dzien, miesiac) { dataUroczystosci(dzien, miesiac, dzis.year) }
    val moznaDodac = osoba.isNotBlank() && data != null

    val wybierzKontakt = rememberLauncherForActivityResult(
        ActivityResultContracts.PickContact()
    ) { uri -> if (uri != null) imieZKontaktuUr(ctx, uri)?.let { osoba = it } }

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Dim.rCard))
            .background(Tokeny.accent.copy(alpha = 0.06f))
            .border(1.dp, Tokeny.accent.copy(alpha = 0.28f), RoundedCornerShape(Dim.rCard))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("urodziny", "imieniny", "rocznica").forEach { r ->
                val wybrany = r == rodzaj
                Box(
                    Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(11.dp))
                        .background(if (wybrany) Tokeny.accentTintBg else Tokeny.surfaceInput)
                        .border(
                            1.dp,
                            if (wybrany) Tokeny.accentTintLine else Tokeny.lineInput,
                            RoundedCornerShape(11.dp)
                        )
                        .clickable { rodzaj = r },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        etykietaRodzaju(r), fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        fontFamily = Jakarta,
                        color = if (wybrany) Tokeny.accent else Tokeny.inkMuted
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("czyje", fontSize = 10.sp, fontFamily = Jakarta, color = Tokeny.inkMuted)
            PoleOsoby(
                osoba,
                naZmiane = { osoba = it },
                naWybor = { k ->
                    osoba = k.imie
                    // Kontakt z datą urodzin wypełnia też dzień i miesiąc —
                    // po to jest dostęp do kontaktów.
                    if (k.maUrodziny) {
                        dzien = k.dzien.toString()
                        miesiac = k.miesiac.toString()
                        rodzaj = "urodziny"
                        zKontaktu = k.imie
                    }
                }
            )
        }
        PrzyciskDrugorzedny("Wybierz z kontaktów", Modifier.fillMaxWidth()) {
            runCatching { wybierzKontakt.launch(null) }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("dzień", fontSize = 10.sp, fontFamily = Jakarta, color = Tokeny.inkMuted)
                PoleTekstowe(dzien, "24", cyfry = true) { dzien = it.filter(Char::isDigit).take(2) }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("miesiąc", fontSize = 10.sp, fontFamily = Jakarta, color = Tokeny.inkMuted)
                PoleTekstowe(miesiac, "9", cyfry = true) { miesiac = it.filter(Char::isDigit).take(2) }
            }
        }

        if (zKontaktu.isNotBlank() && zKontaktu == osoba && data != null) {
            Text(
                "Datę urodzin wzięto z kontaktów.",
                style = GrafikType.caption, color = Tokeny.uroczystosc
            )
        }

        // 31 lutego nie istnieje, a wpisać się da — więc mówimy o tym od razu,
        // zamiast milczeć po naciśnięciu „Dodaj".
        if (dzien.isNotBlank() && miesiac.isNotBlank() && data == null) {
            Text(
                "Nie ma takiego dnia w kalendarzu.",
                style = GrafikType.caption, color = Tokeny.warnInk
            )
        }

        Row(
            Modifier.fillMaxWidth().clickable { przypomnij = !przypomnij },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Checkbox(
                przypomnij, { przypomnij = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = Tokeny.accent, checkmarkColor = Tokeny.accentOn,
                    uncheckedColor = Tokeny.lineSoft
                )
            )
            Text("Przypomnij dzień wcześniej", fontSize = 13.sp,
                fontFamily = Jakarta, color = Tokeny.ink)
        }

        Row(
            Modifier.fillMaxWidth().clickable { coroczne = !coroczne },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Checkbox(
                coroczne, { coroczne = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = Tokeny.accent, checkmarkColor = Tokeny.accentOn,
                    uncheckedColor = Tokeny.lineSoft
                )
            )
            Text("Powtarzaj co roku", fontSize = 13.sp, fontFamily = Jakarta, color = Tokeny.ink)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PrzyciskDrugorzedny("Anuluj", Modifier.weight(1f), naZamkniecie)
            Box(
                Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(13.dp))
                    .background(if (moznaDodac) Tokeny.accent else Tokeny.surfaceInput)
                    .clickable(enabled = moznaDodac) {
                        vm.addUroczystosc(data!!, rodzaj, osoba, przypomnij, coroczne)
                        naZamkniecie()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("Dodaj", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    fontFamily = Jakarta,
                    color = if (moznaDodac) Tokeny.accentOn else Tokeny.inkDisabled)
            }
        }
    }
}

/**
 * Jak wpis ma się czytać na listach: „Imieniny · Anna Kowalska" zamiast surowego
 * „imieniny — Anna Kowalska" spod spodu. Zwykłe wydarzenie zostaje bez zmian.
 */
internal fun opisWpisu(ev: EventRow): String =
    if (ev.rodzaj.isBlank()) {
        listOfNotNull(ev.time.ifEmpty { null }, ev.text).joinToString(" · ")
    } else {
        "${etykietaRodzaju(ev.rodzaj)} · ${ev.osoba.ifBlank { ev.text }}"
    }

/**
 * Data uroczystości z dwóch pól: dnia i miesiąca. `null`, gdy takiego dnia nie ma
 * w kalendarzu — wtedy formularz nie pozwala zapisać i mówi, dlaczego.
 *
 * Rok bierzemy bieżący, bo przy powtarzaniu liczy się tylko dzień i miesiąc.
 * 29 lutego przyjmujemy zawsze: rok przestępny wraca co cztery lata, a uroczystość
 * ma tam zostać wpisana raz na zawsze.
 */
internal fun dataUroczystosci(dzien: String, miesiac: String, rok: Int): LocalDate? {
    val d = dzien.toIntOrNull() ?: return null
    val m = miesiac.toIntOrNull() ?: return null
    if (m == 2 && d == 29) return LocalDate.of(nastepnyPrzestepny(rok), 2, 29)
    return runCatching { LocalDate.of(rok, m, d) }.getOrNull()
}

private fun nastepnyPrzestepny(rok: Int): Int {
    var r = rok
    while (!java.time.Year.isLeap(r.toLong())) r++
    return r
}

/** Imię z systemowego okna kontaktów — bez dostępu do całej książki adresowej. */
private fun imieZKontaktuUr(ctx: android.content.Context, uri: android.net.Uri): String? =
    runCatching {
        ctx.contentResolver.query(
            uri,
            arrayOf(android.provider.ContactsContract.Contacts.DISPLAY_NAME),
            null, null, null
        )?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    }.getOrNull()
