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
import androidx.compose.ui.window.Dialog
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import pl.grafik.pracy.data.Kopia
import pl.grafik.pracy.data.KopiaAuto
import pl.grafik.pracy.data.SettingsStore
import pl.grafik.pracy.nowy.theme.*
import pl.grafik.pracy.nowy.ui.*
import pl.grafik.pracy.update.UpdateProgress
import pl.grafik.pracy.ui.UpdateVm

/**
 * „Dane i kopia" — makieta `design/mockups/Data.html`.
 *
 * Cała treść ekranu mówi jedno: dane są w telefonie i użytkownik ma nad nimi władzę.
 * Kopia to jeden plik `.json`, w którym siedzi też archiwum odcinków — Maciej wybrał
 * jeden plik na wszystko, żeby zmiana telefonu naprawdę zajmowała minutę.
 */
@Composable
fun EkranDane(uvm: UpdateVm, naPowrot: () -> Unit) {
    val ctx = LocalContext.current
    val zakres = rememberCoroutineScope()
    val ustawienia = remember { SettingsStore(ctx) }

    var stan by remember { mutableStateOf(Kopia.StanDanych()) }
    var kopie by remember { mutableStateOf(emptyList<Kopia.PlikKopii>()) }
    var komunikat by remember { mutableStateOf<Komunikat?>(null) }
    var pytanie by remember { mutableStateOf<Pytanie?>(null) }
    val auto by ustawienia.kopiaAuto.collectAsState(initial = false)

    /** Po każdej operacji liczby i lista kopii muszą się zgadzać z tym, co w telefonie. */
    suspend fun odswiez() {
        stan = Kopia.stan(ctx)
        kopie = Kopia.kopieLokalne(ctx)
    }

    LaunchedEffect(Unit) { odswiez() }

    val zapisz = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        zakres.launch {
            Kopia.zapiszDo(ctx, uri)
                .onSuccess { komunikat = Komunikat(Gdzie.KOPIA, "Kopia zapisana.") }
                .onFailure { komunikat = Komunikat(Gdzie.KOPIA, "Nie udało się zapisać kopii.") }
            odswiez()
        }
    }

    val wczytaj = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        zakres.launch {
            Kopia.wczytajZ(ctx, uri)
                .onSuccess { p ->
                    komunikat = Komunikat(
                        Gdzie.KOPIA,
                        "Wczytano: ${p.dni} dni, ${p.wydarzenia} wydarzeń, ${p.odcinki} odcinków."
                    )
                }
                .onFailure { komunikat = Komunikat(Gdzie.KOPIA, "Nie udało się wczytać pliku.") }
            odswiez()
        }
    }

    Box(Modifier.fillMaxSize()) {
        TloZPoswiata(Modifier.fillMaxSize())

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(
                    start = Dim.screenGutter, end = Dim.screenGutter,
                    top = gornaKrawedz(), bottom = dolnaKrawedz(22.dp)
                ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            PowrotDoUstawien(naPowrot)

            val pTytul by postepWejscia(Motion.RISE_MS)
            Column(Modifier.wejscie(pTytul), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Dane i kopia", style = GrafikType.h1.copy(fontSize = 26.sp, lineHeight = 26.sp), color = DarkTokens.ink)
                Text(
                    "Wszystko jest w telefonie. Nic nie leci na żaden serwer.",
                    fontSize = 12.sp, lineHeight = 18.sp,
                    fontFamily = Jakarta, color = DarkTokens.inkMuted
                )
            }

            KartaCoZapisane(stan)

            KartaKopii(
                notka = komunikat?.takeIf { it.gdzie == Gdzie.KOPIA }?.tekst,
                naZapis = { zapisz.launch(Kopia.nazwaPliku()) },
                naWyslanie = { zakres.launch { wyslij(ctx) { komunikat = Komunikat(Gdzie.KOPIA, it) } } },
                naWczytanie = { wczytaj.launch(arrayOf("application/json", "text/plain", "*/*")) }
            )

            KartaAutomatycznej(
                wlaczona = auto,
                kopie = kopie,
                naPrzelaczenie = { wl ->
                    zakres.launch {
                        ustawienia.saveKopiaAuto(wl)
                        KopiaAuto.ustaw(ctx, wl)
                        if (wl && kopie.isEmpty()) {
                            Kopia.zrobKopieLokalna(ctx)
                            odswiez()
                        }
                    }
                },
                naPrzywrocenie = { k ->
                    pytanie = Pytanie(
                        tytul = "Przywrócić kopię z ${k.nazwa}?",
                        tresc = "Obecne dane zostaną zastąpione tym, co jest w kopii.",
                        przycisk = "Przywróć",
                        grozne = false
                    ) {
                        zakres.launch {
                            Kopia.przywroc(ctx, k)
                                .onSuccess { p ->
                                    komunikat = Komunikat(Gdzie.KOPIA, "Przywrócono ${p.dni} dni.")
                                }
                                .onFailure {
                                    komunikat = Komunikat(Gdzie.KOPIA, "Nie udało się przywrócić kopii.")
                                }
                            odswiez()
                        }
                    }
                }
            )

            KartaNieodwracalnych(
                dni = stan.dni,
                notka = komunikat?.takeIf { it.gdzie == Gdzie.OPERACJE }?.tekst,
                naPrzywrocenieCyklu = {
                    pytanie = Pytanie(
                        tytul = "Przywrócić grafik do cyklu?",
                        tresc = "Zniknie ${stan.dni} ręcznie zmienionych dni. " +
                            "Ustawienia i wydarzenia zostają.",
                        przycisk = "Przywróć",
                        grozne = false
                    ) {
                        zakres.launch {
                            Kopia.przywrocGrafikDoCyklu(ctx)
                            komunikat = Komunikat(Gdzie.OPERACJE, "Grafik liczy się znów z cyklu.")
                            odswiez()
                        }
                    }
                },
                naCzyszczenie = {
                    pytanie = Pytanie(
                        tytul = "Wyczyścić wszystkie dane?",
                        tresc = "Znikną grafik, ustawienia, wydarzenia, historia wykryć " +
                            "i odcinki. Tego nie da się cofnąć — najpierw zapisz kopię.",
                        przycisk = "Wyczyść",
                        grozne = true
                    ) {
                        zakres.launch {
                            Kopia.wyczyscWszystko(ctx)
                            KopiaAuto.odwolaj(ctx)
                            komunikat = Komunikat(Gdzie.OPERACJE, "Dane wyczyszczone.")
                            odswiez()
                        }
                    }
                }
            )

            KartaWersji(uvm)
        }
    }

    // Komunikat gaśnie sam — nie chcemy paska, który zasłania treść ekranu.
    LaunchedEffect(komunikat) {
        if (komunikat != null) {
            kotlinx.coroutines.delay(5000)
            komunikat = null
        }
    }

    pytanie?.let { p ->
        OknoPytania(p, naZamkniecie = { pytanie = null })
    }
}

// ─────────────────────────────────────────────────────────────
// CO JEST ZAPISANE
// ─────────────────────────────────────────────────────────────

@Composable
private fun KartaCoZapisane(stan: Kopia.StanDanych) {
    val p by postepWejscia(Motion.RISE_MS, 50)

    Column(
        Modifier.wejscie(p).fillMaxWidth()
            .clip(RoundedCornerShape(Dim.rCard))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, DarkTokens.line, RoundedCornerShape(Dim.rCard))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("CO JEST ZAPISANE", style = GrafikType.sectionLabel, color = DarkTokens.inkFaint)

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Liczba(stan.dni, "ręcznych dni", Modifier.weight(1f))
            Liczba(stan.wydarzenia, "wydarzeń", Modifier.weight(1f))
            Liczba(stan.wykrycia, "wykryć pracy", Modifier.weight(1f))
        }

        Text(
            "Grafik w kalendarzu liczy się z cyklu — w pamięci siedzą tylko dni, " +
                "które zmieniłeś sam, ustawienia i wydarzenia.",
            fontSize = 11.sp, lineHeight = 16.5.sp,
            fontFamily = Jakarta, color = DarkTokens.inkMuted
        )
    }
}

@Composable
private fun Liczba(wartosc: Int, podpis: String, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.28f))
            .border(1.dp, Color(0xFF23282D), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            licznikDo(wartosc).toString(),
            style = TextStyle(
                fontSize = 20.sp, fontWeight = FontWeight.Bold,
                fontFamily = Jakarta, fontFeatureSettings = TNUM
            ),
            color = DarkTokens.ink
        )
        Text(podpis, fontSize = 10.sp, fontFamily = Jakarta, color = DarkTokens.inkMuted,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ─────────────────────────────────────────────────────────────
// KOPIA ZAPASOWA
// ─────────────────────────────────────────────────────────────

@Composable
private fun KartaKopii(
    notka: String?,
    naZapis: () -> Unit,
    naWyslanie: () -> Unit,
    naWczytanie: () -> Unit
) {
    val p by postepWejscia(Motion.RISE_MS, 90)

    Column(
        Modifier.wejscie(p).fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DarkTokens.accent.copy(alpha = 0.06f))
            .border(1.dp, DarkTokens.accent.copy(alpha = 0.24f), RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("Kopia zapasowa", fontSize = 14.sp, fontWeight = FontWeight.Bold,
                fontFamily = Jakarta, color = DarkTokens.ink)
            Text(
                "Jeden plik .json z całym stanem aplikacji — zmiana telefonu zajmie minutę.",
                fontSize = 11.sp, lineHeight = 16.5.sp,
                fontFamily = Jakarta, color = Color(0xFF9FE3D2)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.weight(1f).height(48.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(DarkTokens.accent)
                    .clickable(onClick = naZapis),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally)
            ) {
                Icon(IkonaZapisz, null, Modifier.size(16.dp), tint = DarkTokens.accentOn)
                Text("Zapisz kopię", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    fontFamily = Jakarta, color = DarkTokens.accentOn)
            }

            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(15.dp))
                    .background(Color.Black.copy(alpha = 0.3f))
                    .border(1.dp, Color(0xFF2A3036), RoundedCornerShape(15.dp))
                    .clickable(onClick = naWyslanie),
                contentAlignment = Alignment.Center
            ) {
                Icon(IkonaWyslij, "Wyślij kopię", Modifier.size(17.dp), tint = DarkTokens.ink2)
            }
        }

        Row(
            Modifier.fillMaxWidth().height(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, DarkTokens.accent.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                .clickable(onClick = naWczytanie),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally)
        ) {
            Icon(IkonaWczytaj, null, Modifier.size(16.dp), tint = DarkTokens.accent)
            Text("Wczytaj z pliku", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                fontFamily = Jakarta, color = DarkTokens.accent)
        }

        Text(
            notka ?: "Wczytanie nadpisze obecne dane — najpierw zrób kopię.",
            fontSize = 11.sp, lineHeight = 16.5.sp, fontFamily = Jakarta,
            color = if (notka != null) DarkTokens.accent else DarkTokens.inkFaint
        )
    }
}

// ─────────────────────────────────────────────────────────────
// AUTOMATYCZNA KOPIA
// ─────────────────────────────────────────────────────────────

@Composable
private fun KartaAutomatycznej(
    wlaczona: Boolean,
    kopie: List<Kopia.PlikKopii>,
    naPrzelaczenie: (Boolean) -> Unit,
    naPrzywrocenie: (Kopia.PlikKopii) -> Unit
) {
    val p by postepWejscia(Motion.RISE_MS, 130)

    Column(
        Modifier.wejscie(p).fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.035f))
            .border(1.dp, DarkTokens.line, RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().clickable { naPrzelaczenie(!wlaczona) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Automatyczna kopia", fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    fontFamily = Jakarta, color = DarkTokens.ink)
                Text("co niedzielę, do pamięci telefonu", fontSize = 11.sp,
                    fontFamily = Jakarta, color = DarkTokens.inkMuted)
            }
            Przelacznik(wlaczona, naPrzelaczenie)
        }

        if (kopie.isNotEmpty()) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(DarkTokens.line))

            kopie.forEach { k ->
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(IkonaPlik, null, Modifier.size(15.dp), tint = DarkTokens.inkFaint)
                    Text(
                        k.nazwa, Modifier.weight(1f),
                        style = TextStyle(fontSize = 12.sp, fontFamily = Jakarta,
                            fontFeatureSettings = TNUM),
                        color = DarkTokens.ink3, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        k.rozmiar,
                        style = TextStyle(fontSize = 11.sp, fontFamily = Jakarta,
                            fontFeatureSettings = TNUM),
                        color = DarkTokens.inkFaint
                    )
                    Box(
                        Modifier.size(32.dp).clip(RoundedCornerShape(10.dp))
                            .border(1.dp, Color(0xFF2A3036), RoundedCornerShape(10.dp))
                            .clickable { naPrzywrocenie(k) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(IkonaCofnij, "Przywróć tę kopię", Modifier.size(14.dp),
                            tint = DarkTokens.inkMuted)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// OPERACJE NIEODWRACALNE
// ─────────────────────────────────────────────────────────────

@Composable
private fun KartaNieodwracalnych(
    dni: Int,
    notka: String?,
    naPrzywrocenieCyklu: () -> Unit,
    naCzyszczenie: () -> Unit
) {
    val p by postepWejscia(Motion.RISE_MS, 170)

    Column(
        Modifier.wejscie(p).fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x0FC1351E))
            .border(1.dp, Color(0x3DFF937E), RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("OPERACJE NIEODWRACALNE", style = GrafikType.sectionLabel, color = Color(0xFFFF937E))

        WierszOperacji(
            ikona = IkonaOdswiez,
            kolorIkony = DarkTokens.ink2,
            tloIkony = Color.White.copy(alpha = 0.05f),
            tytul = "Przywróć cały grafik do cyklu",
            kolorTytulu = Color(0xFFE7EAEC),
            pogrubiony = false,
            opis = "kasuje $dni ręcznych dni, ustawienia zostają",
            obrys = Color(0xFF2A3036),
            akcja = naPrzywrocenieCyklu
        )

        WierszOperacji(
            ikona = IkonaKosz,
            kolorIkony = Color(0xFFFF937E),
            tloIkony = Color(0x24FF937E),
            tytul = "Wyczyść wszystkie dane",
            kolorTytulu = Color(0xFFFF937E),
            pogrubiony = true,
            opis = "grafik, ustawienia, wydarzenia, historia wykryć",
            obrys = Color(0x66FF937E),
            akcja = naCzyszczenie
        )

        Text(
            notka ?: "Przed czyszczeniem zapytamy jeszcze raz i zaproponujemy zapisanie kopii.",
            fontSize = 11.sp, lineHeight = 16.5.sp, fontFamily = Jakarta,
            color = if (notka != null) DarkTokens.accent else DarkTokens.inkMuted
        )
    }
}

@Composable
private fun WierszOperacji(
    ikona: androidx.compose.ui.graphics.vector.ImageVector,
    kolorIkony: Color,
    tloIkony: Color,
    tytul: String,
    kolorTytulu: Color,
    pogrubiony: Boolean,
    opis: String,
    obrys: Color,
    akcja: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(Color.Black.copy(alpha = 0.25f))
            .border(1.dp, obrys, RoundedCornerShape(15.dp))
            .clickable(onClick = akcja)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(tloIkony),
            contentAlignment = Alignment.Center
        ) { Icon(ikona, null, Modifier.size(16.dp), tint = kolorIkony) }

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                tytul, fontSize = 13.sp,
                fontWeight = if (pogrubiony) FontWeight.Bold else FontWeight.SemiBold,
                fontFamily = Jakarta, color = kolorTytulu
            )
            Text(opis, fontSize = 11.sp, fontFamily = Jakarta, color = DarkTokens.inkMuted,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ─────────────────────────────────────────────────────────────
// WERSJA
// ─────────────────────────────────────────────────────────────

@Composable
private fun KartaWersji(uvm: UpdateVm) {
    val p by postepWejscia(Motion.RISE_MS, 210)
    val u by uvm.state.collectAsState()
    val sprawdza = u.progress is UpdateProgress.Checking
    val pobiera = u.progress as? UpdateProgress.Downloading

    Row(
        Modifier.wejscie(p).fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(1.dp, DarkTokens.line, RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("Wersja ${u.current}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                fontFamily = Jakarta, color = DarkTokens.ink,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                when {
                    sprawdza -> "sprawdzam wydania na GitHubie…"
                    pobiera != null -> "pobieram nową wersję…"
                    u.available != null -> "jest nowsza — dotknij, żeby zainstalować"
                    else -> "aktualizacje z GitHuba"
                },
                fontSize = 11.sp, fontFamily = Jakarta,
                color = if (u.available != null) DarkTokens.accent else DarkTokens.inkMuted,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            Modifier.height(36.dp).clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, Color(0xFF2A3036), RoundedCornerShape(12.dp))
                .clickable(enabled = !sprawdza && pobiera == null) {
                    if (u.available != null) uvm.install() else uvm.check(manual = true)
                }
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (u.available != null) "Zainstaluj" else "Sprawdź",
                fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                fontFamily = Jakarta, color = DarkTokens.ink2
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// POTWIERDZENIE I KOMUNIKAT
// ─────────────────────────────────────────────────────────────

/** Pytanie zadawane przed operacją, której nie da się cofnąć. */
private data class Pytanie(
    val tytul: String,
    val tresc: String,
    val przycisk: String,
    val grozne: Boolean,
    val akcja: () -> Unit
)

@Composable
private fun OknoPytania(p: Pytanie, naZamkniecie: () -> Unit) {
    Dialog(onDismissRequest = naZamkniecie) {
        Column(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(Dim.rCard))
                // Okno musi być nieprzezroczyste — `surface` to półprzezroczysta biel
                // i treść ekranu prześwitywała przez tekst pytania.
                .background(DarkTokens.bgElevated)
                .border(1.dp, DarkTokens.line, RoundedCornerShape(Dim.rCard))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(p.tytul, style = GrafikType.cardTitle, color = DarkTokens.ink)
            Text(p.tresc, fontSize = 12.sp, lineHeight = 18.sp,
                fontFamily = Jakarta, color = DarkTokens.ink3)

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PrzyciskDrugorzedny("Anuluj", Modifier.weight(1f), naZamkniecie)

                Box(
                    Modifier.weight(1f).height(44.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(if (p.grozne) Color(0xFFFF937E) else DarkTokens.accent)
                        .clickable { p.akcja(); naZamkniecie() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        p.przycisk, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        fontFamily = Jakarta,
                        color = if (p.grozne) Color(0xFF2A0D07) else DarkTokens.accentOn
                    )
                }
            }
        }
    }
}

/** Gdzie pokazać odpowiedź po operacji — przy przycisku, który ją wywołał. */
private enum class Gdzie { KOPIA, OPERACJE }

/**
 * Odpowiedź po operacji. Zamiast paska nad treścią podmieniamy na chwilę notkę
 * w tej samej karcie — makieta nie ma Snackbara, a pasek zasłaniałby kartę wersji.
 */
private data class Komunikat(val gdzie: Gdzie, val tekst: String)

/**
 * Wysłanie kopii tam, gdzie wskaże użytkownik. Plik powstaje w pamięci aplikacji
 * i idzie dalej przez FileProvider — nic nie trafia nigdzie samo.
 */
private suspend fun wyslij(ctx: android.content.Context, komunikat: (String) -> Unit) {
    Kopia.zrobKopieLokalna(ctx)
        .onSuccess { k ->
            runCatching {
                val uri = FileProvider.getUriForFile(
                    ctx, "${ctx.packageName}.fileprovider", k.plik
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, k.nazwa)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                ctx.startActivity(
                    Intent.createChooser(intent, "Wyślij kopię")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }.onFailure { komunikat("Nie udało się wysłać kopii.") }
        }
        .onFailure { komunikat("Nie udało się przygotować kopii.") }
}
