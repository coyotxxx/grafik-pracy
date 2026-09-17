package pl.grafik.pracy.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.grafik.pracy.location.GeofenceManager
import pl.grafik.pracy.ui.PresenceVm
import pl.grafik.pracy.ui.theme.*
import java.time.format.DateTimeFormatter

private val PL = java.util.Locale.forLanguageTag("pl-PL")
private val hm = DateTimeFormatter.ofPattern("HH:mm", PL)
private val dmy = DateTimeFormatter.ofPattern("d.MM", PL)

@Composable
fun WorkPlaceScreen(vm: PresenceVm) {
    val ctx = LocalContext.current
    val s by vm.state.collectAsState()
    val wp = s.place

    var fineGranted by remember { mutableStateOf(GeofenceManager.hasFineLocation(ctx)) }
    var bgGranted by remember { mutableStateOf(GeofenceManager.hasBackgroundLocation(ctx)) }
    var notifGranted by remember { mutableStateOf(notificationsAllowed(ctx)) }

    val askFine = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        fineGranted = GeofenceManager.hasFineLocation(ctx)
        bgGranted = GeofenceManager.hasBackgroundLocation(ctx)
    }
    val askNotif = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notifGranted = notificationsAllowed(ctx)
    }

    // Odświeżenie po powrocie z ustawień systemowych. Samo LaunchedEffect(Unit) tu nie
    // wystarczało — ekran zostaje żywy, gdy odchodzisz do ustawień Androida, więc ptaszki
    // pokazywały stary stan uprawnień jeszcze długo po ich nadaniu.
    val zycie = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(zycie) {
        val obs = LifecycleEventObserver { _, zdarzenie ->
            if (zdarzenie == Lifecycle.Event.ON_RESUME) {
                fineGranted = GeofenceManager.hasFineLocation(ctx)
                bgGranted = GeofenceManager.hasBackgroundLocation(ctx)
                notifGranted = notificationsAllowed(ctx)
            }
        }
        zycie.addObserver(obs)
        onDispose { zycie.removeObserver(obs) }
    }

    Column(
        Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Miejsce pracy", fontSize = 21.sp, fontWeight = FontWeight.SemiBold, color = OnBg)
        Text(
            // Zdanie o dotknięciu przestaje być prawdą przy automatycznym zapisie —
            // ekran nie może obiecywać czegoś, czego ustawienie niżej właśnie nie robi.
            "Telefon sam rozpozna, że jesteś w pracy. " +
                (if (wp.autoSave) "Wykryty dzień wchodzi do grafiku sam — możesz go cofnąć."
                 else "Wpis czeka na Twoje dotknięcie.") +
                " Lokalizacja nie opuszcza telefonu.",
            fontSize = 12.sp, color = OnMuted, lineHeight = 17.sp
        )

        // ---- główny włącznik ----
        Panel(if (wp.enabled) Surface2 else Surface1) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Automatyczne wykrywanie", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = OnBg)
                    Text(
                        when {
                            !wp.isSet -> "Najpierw zapisz miejsce pracy"
                            wp.enabled -> "Włączone · promień ${wp.radiusM} m"
                            else -> "Wyłączone"
                        },
                        fontSize = 11.sp, color = if (wp.enabled) Accent else OnFaint
                    )
                }
                Switch(
                    checked = wp.enabled,
                    enabled = wp.isSet,
                    onCheckedChange = { vm.setEnabled(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = AccentOn, checkedTrackColor = Accent)
                )
            }
            if (wp.enabled) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = Surface3)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).padding(end = 12.dp)) {
                        Text("Zapisuj automatycznie", fontSize = 14.sp, color = OnBg)
                        Text(
                            if (wp.autoSave)
                                "Wykryty dzień wchodzi do grafiku sam. Powiadomienie tylko informuje, " +
                                    "z możliwością cofnięcia."
                            else
                                "Teraz każdy wykryty dzień czeka na Twoje „Zapisz” w powiadomieniu.",
                            fontSize = 11.sp, color = if (wp.autoSave) Accent else OnFaint, lineHeight = 15.sp
                        )
                    }
                    Switch(
                        checked = wp.autoSave,
                        onCheckedChange = { vm.setAutoSave(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = AccentOn, checkedTrackColor = Accent)
                    )
                }
            }
            s.error?.let {
                Spacer(Modifier.height(8.dp))
                Text("⚠ $it", fontSize = 11.sp, color = OtColor100)
            }
        }

        // ---- kreator uprawnień ----
        if (!fineGranted || !bgGranted || !notifGranted) {
            Panel(Surface1) {
                Text("ZANIM ZADZIAŁA", fontSize = 10.sp, color = OnFaint, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Step(1, "Dostęp do lokalizacji", fineGranted, "Zezwól") {
                    askFine.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                }
                Step(2, "Lokalizacja „Zawsze\" (w tle)", bgGranted, "Ustawienia") {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        ctx.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}"))
                        )
                    } else {
                        askFine.launch(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION))
                    }
                }
                Step(3, "Powiadomienia", notifGranted, "Zezwól") {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        askNotif.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        ctx.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}"))
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Bez powiadomień nie zobaczysz propozycji wpisu. " +
                        "Android wymaga, byś opcję „Zawsze zezwalaj\" wybrał ręcznie w ustawieniach aplikacji " +
                        "→ Uprawnienia → Lokalizacja. Warto też ustawić baterię na „Bez ograniczeń\".",
                    fontSize = 10.sp, color = OnFaint, lineHeight = 14.sp
                )
            }
        }

        // ---- lokalizacja zakładu ----
        Panel(Surface1) {
            Text("GDZIE JEST TWOJA PRACA", fontSize = 10.sp, color = OnFaint, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            if (wp.isSet) {
                Text("%.5f, %.5f".format(wp.lat, wp.lon), fontSize = 13.sp, color = OnBg)
                s.distanceM?.let {
                    Text(
                        if (it <= wp.radiusM) "Jesteś w strefie (${it} m od środka)" else "${it} m od miejsca pracy",
                        fontSize = 11.sp, color = if (it <= wp.radiusM) SunColor else OnMuted
                    )
                }
            } else {
                Text("Nie ustawione", fontSize = 13.sp, color = OnFaint)
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { if (!fineGranted) askFine.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)) else vm.captureHere() },
                enabled = !s.capturing,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = AccentOn)
            ) {
                Icon(Icons.Default.MyLocation, null, Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (s.capturing) "Ustalam pozycję…" else "Jestem teraz w pracy — zapisz to miejsce", fontSize = 13.sp)
            }
            Spacer(Modifier.height(12.dp))
            Text("Promień strefy: ${wp.radiusM} m", fontSize = 12.sp, color = OnMuted)
            Slider(
                value = wp.radiusM.toFloat(),
                onValueChange = { vm.setRadius(it.toInt()) },
                valueRange = 100f..500f,
                steps = 7,
                colors = SliderDefaults.colors(thumbColor = Accent, activeTrackColor = Accent, inactiveTrackColor = Surface3)
            )
            Text(
                "Poniżej 100 m Android zaczyna gubić zdarzenia. Duży zakład → większy promień.",
                fontSize = 10.sp, color = OnFaint
            )
        }

        // ---- Wi-Fi ----
        Panel(Surface1) {
            Text("WI-FI W PRACY (OPCJONALNIE)", fontSize = 10.sp, color = OnFaint, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            Text(
                "Pewniejsze niż GPS w hali i nie zużywa baterii. Jeśli telefon wisi na tej sieci, " +
                    "apka nie uzna przypadkowego wyjścia ze strefy.",
                fontSize = 11.sp, color = OnMuted, lineHeight = 15.sp
            )
            Spacer(Modifier.height(10.dp))
            if (wp.hasWifi) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Wifi, null, Modifier.size(16.dp), tint = SunColor)
                    Spacer(Modifier.width(8.dp))
                    Text(wp.ssid, fontSize = 14.sp, color = OnBg, modifier = Modifier.weight(1f))
                    TextButton(onClick = { vm.clearSsid() }) { Text("Usuń", fontSize = 12.sp, color = OnMuted) }
                }
            } else {
                OutlinedButton(
                    onClick = { vm.captureSsid() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = OnBg)
                ) {
                    Icon(Icons.Default.Wifi, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Użyj sieci, do której jestem teraz podłączony", fontSize = 12.sp)
                }
                s.currentSsid?.let { Text("Teraz: $it", fontSize = 10.sp, color = OnFaint, modifier = Modifier.padding(top = 6.dp)) }
            }
        }

        // ---- progi ----
        Panel(Surface1) {
            Text("CZUŁOŚĆ", fontSize = 10.sp, color = OnFaint, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(10.dp))
            Text("Minimalny pobyt uznawany za pracę: ${wp.minStayMin} min", fontSize = 12.sp, color = OnMuted)
            Slider(
                value = wp.minStayMin.toFloat(), onValueChange = { vm.setMinStay(it.toInt()) },
                valueRange = 10f..120f, steps = 10,
                colors = SliderDefaults.colors(thumbColor = Accent, activeTrackColor = Accent, inactiveTrackColor = Surface3)
            )
            Text("Przejazd obok zakładu nie zrobi dnia pracy.", fontSize = 10.sp, color = OnFaint)
            Spacer(Modifier.height(12.dp))
            Text("Krótkie wyjście sklejane do: ${wp.mergeGapMin} min", fontSize = 12.sp, color = OnMuted)
            Slider(
                value = wp.mergeGapMin.toFloat(), onValueChange = { vm.setMergeGap(it.toInt()) },
                valueRange = 5f..90f, steps = 16,
                colors = SliderDefaults.colors(thumbColor = Accent, activeTrackColor = Accent, inactiveTrackColor = Surface3)
            )
            Text("Skok do sklepu nie potnie dnia na dwie obecności.", fontSize = 10.sp, color = OnFaint)
        }

        // ---- historia ----
        Panel(Surface1) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("HISTORIA WYKRYĆ", fontSize = 10.sp, color = OnFaint, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                if (s.log.isNotEmpty()) Text("${s.log.size}", fontSize = 10.sp, color = OnFaint)
            }
            Spacer(Modifier.height(8.dp))
            if (s.log.isEmpty()) {
                Text("Jeszcze nic nie wykryto.", fontSize = 12.sp, color = OnFaint)
            } else {
                s.log.forEach { r ->
                    val statusColor = when (r.status) {
                        "accepted" -> SunColor
                        "rejected" -> OnFaint
                        else -> Accent
                    }
                    Column(Modifier.padding(vertical = 7.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(7.dp).clip(RoundedCornerShape(4.dp)).background(statusColor))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                java.time.LocalDate.parse(r.date).format(dmy),
                                fontSize = 13.sp, color = OnBg, fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "${java.time.LocalDateTime.parse(r.enterAt).format(hm)}–${java.time.LocalDateTime.parse(r.exitAt).format(hm)}",
                                fontSize = 12.sp, color = OnMuted, modifier = Modifier.weight(1f)
                            )
                            // Rozpoznana zmiana — przy zamianie zmian to ona ląduje w grafiku.
                            r.shift?.takeIf { it.isWork }?.let { z ->
                                Text(
                                    z.code, fontSize = 12.sp, color = OnMuted,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                            Text(
                                if (r.otHours > 0) "+${r.otHours} h ${r.otRate}%" else "bez NG",
                                fontSize = 12.sp,
                                color = if (r.otHours > 0) OtColor100 else OnFaint,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (r.status == "pending") {
                            Row(Modifier.padding(start = 15.dp, top = 5.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { vm.accept(r.id) },
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = AccentOn)
                                ) { Text("Zapisz", fontSize = 12.sp) }
                                TextButton(
                                    onClick = { vm.reject(r.id) },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                                ) { Text("Odrzuć", fontSize = 12.sp, color = OnMuted) }
                            }
                        } else {
                            Text(
                                if (r.status == "accepted") "zapisane w grafiku" else "odrzucone",
                                fontSize = 10.sp, color = OnFaint, modifier = Modifier.padding(start = 15.dp)
                            )
                        }
                    }
                    HorizontalDivider(color = Surface3)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

private fun notificationsAllowed(ctx: android.content.Context): Boolean =
    androidx.core.app.NotificationManagerCompat.from(ctx).areNotificationsEnabled()

@Composable
private fun Panel(bg: Color, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(bg).padding(14.dp), content = content)
}

@Composable
private fun Step(n: Int, label: String, done: Boolean, cta: String, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 5.dp)) {
        Box(
            Modifier.size(22.dp).clip(RoundedCornerShape(11.dp)).background(if (done) SunColor else Surface3),
            contentAlignment = Alignment.Center
        ) {
            if (done) Icon(Icons.Default.Check, null, Modifier.size(14.dp), tint = Bg)
            else Text("$n", fontSize = 11.sp, color = OnMuted, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        Text(label, fontSize = 13.sp, color = if (done) OnMuted else OnBg, modifier = Modifier.weight(1f))
        if (!done) {
            TextButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)) {
                Text(cta, fontSize = 12.sp, color = Accent)
            }
        }
    }
}
