package pl.grafik.pracy.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.grafik.pracy.ui.UpdateVm
import pl.grafik.pracy.ui.theme.*
import pl.grafik.pracy.update.UpdateProgress

/** Pasek „dostępna aktualizacja" nad kalendarzem. Znika, gdy nie ma czego instalować. */
@Composable
fun UpdateBar(vm: UpdateVm) {
    val s by vm.state.collectAsState()
    val info = s.available
    val busy = s.progress is UpdateProgress.Downloading || s.progress is UpdateProgress.Installing
    val visible = info != null && (!s.dismissed || busy)

    AnimatedVisibility(visible, enter = expandVertically(), exit = shrinkVertically()) {
        if (info == null) return@AnimatedVisibility
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Surface2)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SystemUpdate, null, Modifier.size(19.dp), tint = Accent)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Dostępna aktualizacja ${info.version}",
                        fontSize = 13.sp, fontWeight = FontWeight.Medium, color = OnBg
                    )
                    val sub = when (val p = s.progress) {
                        is UpdateProgress.Downloading -> "Pobieram… ${p.percent}%  (${mb(p.done)} z ${mb(p.total)})"
                        is UpdateProgress.Installing -> "Uruchamiam instalator…"
                        is UpdateProgress.Failed -> p.message
                        else -> "masz ${s.current} · ${mb(info.sizeBytes)}"
                    }
                    Text(
                        sub, fontSize = 11.sp,
                        color = if (s.progress is UpdateProgress.Failed) OtColor100 else OnMuted
                    )
                }
                if (!busy) {
                    Button(
                        onClick = { vm.install() },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = AccentOn)
                    ) { Text("Aktualizuj", fontSize = 12.sp, fontWeight = FontWeight.Medium) }
                    IconButton(onClick = { vm.dismiss() }, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, "Ukryj", Modifier.size(16.dp), tint = OnFaint)
                    }
                }
            }
            if (s.progress is UpdateProgress.Downloading) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (s.progress as UpdateProgress.Downloading).percent / 100f },
                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                    color = Accent, trackColor = Surface3
                )
            }
            if (info.notes.isNotBlank() && !busy) {
                Spacer(Modifier.height(6.dp))
                Text(
                    info.notes.lineSequence().take(3).joinToString("\n"),
                    fontSize = 10.sp, color = OnFaint, lineHeight = 14.sp
                )
            }
        }
    }
}

private fun mb(bytes: Long): String =
    if (bytes <= 0) "?" else String.format("%.1f MB", bytes / 1_048_576.0)
