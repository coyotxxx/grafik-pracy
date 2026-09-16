package pl.grafik.pracy.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Bg      = Color(0xFF16181F)
val Surface1= Color(0xFF1D2029)
val Surface2= Color(0xFF262A35)
val Surface3= Color(0xFF30353F)
val OnBg    = Color(0xFFF2F3F7)
val OnMuted = Color(0xFF9AA0B0)
val OnFaint = Color(0xFF6B7280)
val Accent  = Color(0xFFF2A43A)
val AccentOn= Color(0xFF2A1C06)

private val dark = darkColorScheme(
    primary = Accent, onPrimary = AccentOn,
    background = Bg, onBackground = OnBg,
    surface = Surface1, onSurface = OnBg,
    surfaceVariant = Surface2, onSurfaceVariant = OnMuted
)

@Composable
fun GrafikTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = dark, typography = Typography(), content = content)
}

/** Gotowa paleta — użytkownik wybiera z niej, nie miesza własnych odcieni. */
data class Swatch(val id: String, val label: String, val fill: Color, val border: Color, val text: Color)

object Palette {
    val all = listOf(
        Swatch("bursztyn", "bursztyn", Color(0xFF5A421C), Color(0xFF8A6526), Color(0xFFF7C46A)),
        Swatch("koral",    "koral",    Color(0xFF5C2A2C), Color(0xFF8E4144), Color(0xFFF79098)),
        Swatch("indygo",   "indygo",   Color(0xFF32355F), Color(0xFF4C5190), Color(0xFFA7ADF5)),
        Swatch("szalwia",  "szałwia",  Color(0xFF26372C), Color(0xFF3A5343), Color(0xFF8FCFA3)),
        Swatch("malina",   "malina",   Color(0xFF4E2340), Color(0xFF7A3763), Color(0xFFEF8FD0)),
        Swatch("fiolet",   "fiolet",   Color(0xFF3F2B57), Color(0xFF614285), Color(0xFFC9A3F5)),
        Swatch("lazur",    "lazur",    Color(0xFF1F3B52), Color(0xFF2F587B), Color(0xFF7FC3F0)),
        Swatch("morski",   "morski",   Color(0xFF1C3D3B), Color(0xFF2B5C59), Color(0xFF6FD1C9)),
        Swatch("oliwka",   "oliwka",   Color(0xFF3A3F1E), Color(0xFF5A612E), Color(0xFFC6D26A)),
        Swatch("grafit",   "grafit",   Color(0xFF2A2E38), Color(0xFF3E4452), Color(0xFFAEB4C2))
    )

    fun byId(id: String?): Swatch = all.firstOrNull { it.id == id } ?: all.last()

    /** Domyślne przypisanie kolorów do typów dni. */
    val defaults = mapOf(
        "I" to "bursztyn", "II" to "koral", "III" to "indygo",
        "w5" to "szalwia", "wś" to "malina", "DWN" to "lazur",
        "bezw." to "oliwka", "U" to "fiolet", "L4" to "grafit"
    )
}

val OtColor100 = Color(0xFFF5A623)
val OtColor50  = Color(0xFFBE8434)
val DevColor   = Color(0xFFE3D64B)
val SatColor   = Color(0xFFF0A44B)
val SunColor   = Color(0xFF6FD08C)
val Danger     = Color(0xFFE5706B)
