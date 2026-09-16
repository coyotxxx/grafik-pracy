package pl.grafik.pracy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.grafik.pracy.ui.Vm
import pl.grafik.pracy.ui.PresenceVm
import pl.grafik.pracy.ui.UpdateVm
import pl.grafik.pracy.ui.screens.*
import pl.grafik.pracy.ui.theme.*

class MainActivity : ComponentActivity() {
    private val vm: Vm by viewModels()
    private val pvm: PresenceVm by viewModels()
    private val uvm: UpdateVm by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { GrafikTheme { App(vm, pvm, uvm) } }
    }
}

private data class Tab(val label: String, val icon: ImageVector)

@Composable
fun App(vm: Vm, pvm: PresenceVm, uvm: UpdateVm) {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        Tab("Miesiąc", Icons.Default.CalendarMonth),
        Tab("Podsum.", Icons.Default.QueryStats),
        Tab("Cykl", Icons.Default.Autorenew),
        Tab("Praca", Icons.Default.LocationOn),
        Tab("Kolory", Icons.Default.Palette)
    )

    Scaffold(
        containerColor = Bg,
        bottomBar = {
            NavigationBar(containerColor = Surface1, tonalElevation = 0.dp) {
                tabs.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(t.icon, t.label, modifier = Modifier.size(21.dp)) },
                        label = { Text(t.label, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Accent, selectedTextColor = Accent,
                            unselectedIconColor = OnFaint, unselectedTextColor = OnFaint,
                            indicatorColor = Surface2
                        )
                    )
                }
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().background(Bg)) {
            // Pasek aktualizacji nad wszystkim — zobaczysz go niezależnie od zakładki.
            UpdateBar(uvm)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (tab) {
                    0 -> CalendarScreen(vm) { }
                    1 -> SummaryScreen(vm)
                    2 -> SetupScreen(vm, uvm)
                    3 -> WorkPlaceScreen(pvm)
                    else -> ColorsScreen(vm)
                }
            }
        }
    }
}
