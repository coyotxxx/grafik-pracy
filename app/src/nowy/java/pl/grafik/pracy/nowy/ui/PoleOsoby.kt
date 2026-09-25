package pl.grafik.pracy.nowy.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import pl.grafik.pracy.nowy.theme.*

/**
 * Pole „czyje" z podpowiedziami z kontaktów telefonu.
 *
 * Po dwóch literach pokazuje pasujące osoby; wybranie takiej, która ma w kontaktach
 * datę urodzin, wypełnia też dzień i miesiąc — stąd [naWybor] dostaje cały [Kontakt].
 *
 * O dostęp do kontaktów pytamy dopiero tutaj i tylko raz. Odmowa niczego nie psuje:
 * imię dalej można wpisać ręcznie albo wskazać osobę systemowym oknem wyboru.
 */
@Composable
fun PoleOsoby(
    wartosc: String,
    podpowiedz: String = "imię i nazwisko",
    naZmiane: (String) -> Unit,
    naWybor: (Kontakt) -> Unit
) {
    val ctx = LocalContext.current
    var maZgode by remember { mutableStateOf(Kontakty.czyMaZgode(ctx)) }
    var pytano by rememberSaveable { mutableStateOf(false) }
    var wybrano by remember { mutableStateOf("") }
    var podpowiedzi by remember { mutableStateOf(emptyList<Kontakt>()) }

    val pytaj = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { zgoda -> maZgode = zgoda }

    // Szukamy z chwilą zwłoki, żeby nie odpytywać książki adresowej na każdą literę.
    LaunchedEffect(wartosc, maZgode) {
        if (wartosc == wybrano || wartosc.trim().length < Kontakty.MIN_ZNAKOW) {
            podpowiedzi = emptyList()
            return@LaunchedEffect
        }
        if (!maZgode) {
            podpowiedzi = emptyList()
            if (!pytano) {
                pytano = true
                pytaj.launch(Manifest.permission.READ_CONTACTS)
            }
            return@LaunchedEffect
        }
        delay(250)
        podpowiedzi = Kontakty.szukaj(ctx, wartosc)
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        PoleTekstowe(wartosc, podpowiedz) { naZmiane(it.take(80)) }

        podpowiedzi.forEach { k ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp))
                    .background(Tokeny.surfaceInput)
                    .border(1.dp, Tokeny.lineInput, RoundedCornerShape(11.dp))
                    .clickable {
                        wybrano = k.imie
                        podpowiedzi = emptyList()
                        naWybor(k)
                    }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(IkonaOsoba, null, Modifier.size(16.dp), tint = Tokeny.inkIkona)
                Text(
                    k.imie, Modifier.weight(1f),
                    fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFamily = Jakarta,
                    color = Tokeny.ink, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                if (k.maUrodziny) {
                    Text(
                        "urodziny ${k.dzien}.${k.miesiac}",
                        fontSize = 10.5.sp, fontFamily = Jakarta,
                        color = Tokeny.uroczystosc
                    )
                }
            }
        }

        if (!maZgode && pytano && wartosc.trim().length >= Kontakty.MIN_ZNAKOW) {
            Text(
                "Bez dostępu do kontaktów wpisz imię ręcznie albo wskaż osobę przyciskiem niżej.",
                style = GrafikType.caption, color = Tokeny.inkMuted
            )
        }
    }
}
