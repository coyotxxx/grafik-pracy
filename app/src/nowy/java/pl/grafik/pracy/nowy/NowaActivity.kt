package pl.grafik.pracy.nowy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import pl.grafik.pracy.nowy.ekrany.EkranBilans
import pl.grafik.pracy.nowy.ekrany.EkranGrafik
import pl.grafik.pracy.nowy.ekrany.EkranTeraz
import pl.grafik.pracy.nowy.ekrany.KartaDnia
import pl.grafik.pracy.nowy.ekrany.EkranCzasPracy
import pl.grafik.pracy.nowy.ekrany.EkranDane
import pl.grafik.pracy.nowy.ekrany.EkranMojCykl
import pl.grafik.pracy.nowy.ekrany.EkranOdpoczynek
import pl.grafik.pracy.nowy.ekrany.EkranPlanerUrlopu
import pl.grafik.pracy.nowy.ekrany.EkranPowiadomienia
import pl.grafik.pracy.nowy.ekrany.EkranUrlop
import pl.grafik.pracy.nowy.ekrany.EkranWygladKolory
import pl.grafik.pracy.nowy.ekrany.EkranWyplata
import pl.grafik.pracy.nowy.ekrany.EkranWykrywanie
import pl.grafik.pracy.nowy.ekrany.EkranUstawienia
import pl.grafik.pracy.nowy.ekrany.TrybEdycji
import java.time.LocalDate
import pl.grafik.pracy.nowy.theme.*
import pl.grafik.pracy.nowy.ui.dolnaKrawedz
import pl.grafik.pracy.ui.PresenceVm
import pl.grafik.pracy.ui.UpdateVm
import pl.grafik.pracy.ui.Vm

/**
 * Ekran startowy wariantu „nowy".
 *
 * Silnik jest ten sam co w klasycznej aplikacji — te same modele widoku, ta sama baza,
 * to samo wykrywanie pracy. Zmienia się wyłącznie warstwa, którą widać.
 */
class NowaActivity : ComponentActivity() {
    private val vm: Vm by viewModels()
    private val pvm: PresenceVm by viewModels()
    private val uvm: UpdateVm by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { NowaApp(vm, pvm, uvm) }
    }

    override fun onResume() {
        super.onResume()
        pvm.ensureGeofence()
    }
}

/** Podstrony ustawień — dochodzą po kolei, każda z własnej makiety. */
enum class Podstrona {
    CYKL, CZAS_PRACY, URLOP, WYKRYWANIE, WYGLAD, ODPOCZYNEK, POWIADOMIENIA, PLANER, WYPLATA,
    DANE
}

private enum class Zakladka(val etykieta: String, val ikona: ImageVector) {
    TERAZ("Teraz", IkonaTeraz),
    GRAFIK("Grafik", IkonaGrafik),
    BILANS("Bilans", IkonaBilans),
    USTAWIENIA("Ustawienia", IkonaUstawienia)
}

@Composable
private fun NowaApp(vm: Vm, pvm: PresenceVm, uvm: UpdateVm) {
    var zakladka by remember { mutableStateOf(Zakladka.TERAZ) }
    var otwartyDzien by remember { mutableStateOf<LocalDate?>(null) }
    var edycja by remember { mutableStateOf(false) }
    var podstrona by remember { mutableStateOf<Podstrona?>(null) }

    otwartyDzien?.let { d -> KartaDnia(vm, d) { otwartyDzien = null } }

    // Systemowe „wstecz" ma cofać o jeden krok w aplikacji, a nie od razu z niej wychodzić.
    BackHandler(enabled = edycja) { edycja = false }
    BackHandler(enabled = !edycja && podstrona != null) { podstrona = null }
    BackHandler(enabled = !edycja && podstrona == null && zakladka != Zakladka.TERAZ) {
        zakladka = Zakladka.TERAZ
    }

    if (edycja) {
        TrybEdycji(vm) { edycja = false }
        return
    }

    when (podstrona) {
        Podstrona.CYKL -> { EkranMojCykl(vm) { podstrona = null }; return }
        Podstrona.CZAS_PRACY -> { EkranCzasPracy(vm) { podstrona = null }; return }
        Podstrona.URLOP -> {
            EkranUrlop(vm, naPowrot = { podstrona = null },
                naPlaner = { podstrona = Podstrona.PLANER })
            return
        }
        Podstrona.WYKRYWANIE -> { EkranWykrywanie(pvm) { podstrona = null }; return }
        Podstrona.WYGLAD -> { EkranWygladKolory(vm) { podstrona = null }; return }
        Podstrona.POWIADOMIENIA -> { EkranPowiadomienia(vm) { podstrona = null }; return }
        Podstrona.PLANER -> { EkranPlanerUrlopu(vm) { podstrona = null }; return }
        Podstrona.WYPLATA -> { EkranWyplata(vm) { podstrona = null }; return }
        Podstrona.DANE -> { EkranDane(uvm) { podstrona = null }; return }
        Podstrona.ODPOCZYNEK -> {
            EkranOdpoczynek(vm, naPowrot = { podstrona = null },
                naDzien = { podstrona = null; otwartyDzien = it })
            return
        }
        null -> {}
    }

    Column(Modifier.fillMaxSize().background(DarkTokens.bg)) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            // Ekrany dochodzą po kolei, jeden na commit — patrz design/README.md.
            when (zakladka) {
                Zakladka.TERAZ -> EkranTeraz(
                    vm,
                    naGrafik = { zakladka = Zakladka.GRAFIK },
                    naBilans = { zakladka = Zakladka.BILANS },
                    naUstawienia = { zakladka = Zakladka.USTAWIENIA },
                    naDzien = { otwartyDzien = it }
                )
                Zakladka.GRAFIK -> EkranGrafik(
                    vm,
                    naDzien = { otwartyDzien = it },
                    naEdycje = { edycja = true },
                    naOdpoczynek = { podstrona = Podstrona.ODPOCZYNEK }
                )
                Zakladka.BILANS -> EkranBilans(
                    vm,
                    naPlaner = { podstrona = Podstrona.PLANER },
                    naWyplate = { podstrona = Podstrona.WYPLATA }
                )
                Zakladka.USTAWIENIA -> EkranUstawienia(vm, pvm, uvm) { podstrona = it }
                else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(zakladka.etykieta, style = GrafikType.h1, color = DarkTokens.ink)
                }
            }
        }
        PasekNawigacji(zakladka) { zakladka = it }
    }
}

/**
 * Pasek nawigacji 1:1 z makiety (DESIGN_SPEC 4.8):
 * siatka 4 kolumny, gap 4, padding 8/10/18, tło navBg, górna krawędź 1 dp navLine,
 * pozycja: ikona 20 dp + etykieta 10 sp/600, min. wysokość 48 dp, promień 13 dp.
 */
@Composable
private fun PasekNawigacji(wybrana: Zakladka, naZmiane: (Zakladka) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(DarkTokens.navLine))
        Row(
            Modifier.fillMaxWidth().background(DarkTokens.navBg)
                .padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = dolnaKrawedz()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Zakladka.entries.forEach { z ->
                val aktywna = z == wybrana
                Column(
                    Modifier.weight(1f).heightIn(min = Dim.navItemMin)
                        .clip(RoundedCornerShape(13.dp))
                        .background(if (aktywna) DarkTokens.navActiveBg else Color.Transparent)
                        .clickable { naZmiane(z) }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically)
                ) {
                    Icon(
                        z.ikona, z.etykieta,
                        Modifier.size(20.dp),
                        tint = if (aktywna) DarkTokens.accent else DarkTokens.inkMuted
                    )
                    Text(
                        z.etykieta,
                        style = GrafikType.micro,
                        color = if (aktywna) DarkTokens.accent else DarkTokens.inkMuted
                    )
                }
            }
        }
    }
}
