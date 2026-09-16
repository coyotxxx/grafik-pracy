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

/** Zestawy kolorów do wyboru — każdy ma te same identyfikatory, różnią się odcieniami. */
enum class PaletteTheme(val label: String, val opis: String) {
    OBECNA("Stonowana", "ciemne kafelki, spokojne kolory"),
    WYRAZNA("Wyrazista", "te same kolory, mocniejsze wypełnienia"),
    PORY_DNIA("Pory dnia", "poranek, popołudnie, noc"),
    JASNA("Jasne kafelki", "jasne tła, ciemny napis — jak w planerze");
}

object Palette {

    private val OBECNA = listOf(
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

    private val WYRAZNA = listOf(
        Swatch("bursztyn", "bursztyn", Color(0xFF7A5410), Color(0xFFC08A22), Color(0xFFFFD27A)),
        Swatch("koral",    "koral",    Color(0xFF7A2C30), Color(0xFFC0484F), Color(0xFFFFA8AE)),
        Swatch("indygo",   "indygo",   Color(0xFF2E3470), Color(0xFF4E57B8), Color(0xFFB7BCFF)),
        Swatch("szalwia",  "szałwia",  Color(0xFF234531), Color(0xFF3A6E4C), Color(0xFF86E0A6)),
        Swatch("malina",   "malina",   Color(0xFF63234F), Color(0xFF9E3A7E), Color(0xFFFFA0DE)),
        Swatch("fiolet",   "fiolet",   Color(0xFF4B2E70), Color(0xFF7647B0), Color(0xFFD4AEFF)),
        Swatch("lazur",    "lazur",    Color(0xFF17425E), Color(0xFF2A6C99), Color(0xFF7FD0FF)),
        Swatch("morski",   "morski",   Color(0xFF1C4B47), Color(0xFF2E7570), Color(0xFF6FE0D2)),
        Swatch("oliwka",   "oliwka",   Color(0xFF4A5019), Color(0xFF737C2A), Color(0xFFD8E878)),
        Swatch("grafit",   "grafit",   Color(0xFF2E3340), Color(0xFF495060), Color(0xFFB6BDCB))
    )

    private val PORY_DNIA = listOf(
        Swatch("bursztyn", "bursztyn", Color(0xFF6B5411), Color(0xFFB99524), Color(0xFFFFDD6E)),
        Swatch("koral",    "koral",    Color(0xFF6E3413), Color(0xFFB75C25), Color(0xFFFFB07A)),
        Swatch("indygo",   "indygo",   Color(0xFF2A2B5E), Color(0xFF474AA0), Color(0xFFABAFF5)),
        Swatch("szalwia",  "szałwia",  Color(0xFF1C4440), Color(0xFF2E6F68), Color(0xFF6FE0D2)),
        Swatch("malina",   "malina",   Color(0xFF5C2A4E), Color(0xFF93447D), Color(0xFFFF9FDB)),
        Swatch("fiolet",   "fiolet",   Color(0xFF3E2B63), Color(0xFF6446A0), Color(0xFFC5A9F7)),
        Swatch("lazur",    "lazur",    Color(0xFF1A3F55), Color(0xFF2E6B8C), Color(0xFF7ACBF5)),
        Swatch("morski",   "morski",   Color(0xFF17403C), Color(0xFF276A64), Color(0xFF63D6C7)),
        Swatch("oliwka",   "oliwka",   Color(0xFF47461A), Color(0xFF74722C), Color(0xFFDDD87A)),
        Swatch("grafit",   "grafit",   Color(0xFF303542), Color(0xFF4A5161), Color(0xFFB8BFCC))
    )

    private val JASNA = listOf(
        Swatch("bursztyn", "bursztyn", Color(0xFFE8B54A), Color(0xFFC9922A), Color(0xFF3A2A00)),
        Swatch("koral",    "koral",    Color(0xFFE8827F), Color(0xFFC4605D), Color(0xFF3E0F0E)),
        Swatch("indygo",   "indygo",   Color(0xFF8E96E8), Color(0xFF6B73C6), Color(0xFF10143A)),
        Swatch("szalwia",  "szałwia",  Color(0xFF7DCB98), Color(0xFF57A874), Color(0xFF0C2A17)),
        Swatch("malina",   "malina",   Color(0xFFE48FC8), Color(0xFFC06BA4), Color(0xFF3A0E2B)),
        Swatch("fiolet",   "fiolet",   Color(0xFFB79BE8), Color(0xFF9478C5), Color(0xFF23103F)),
        Swatch("lazur",    "lazur",    Color(0xFF77C0E8), Color(0xFF549DC5), Color(0xFF05283A)),
        Swatch("morski",   "morski",   Color(0xFF6FD3C6), Color(0xFF4CAFA2), Color(0xFF06302B)),
        Swatch("oliwka",   "oliwka",   Color(0xFFCBD16F), Color(0xFFA7AD4C), Color(0xFF2A2C05)),
        Swatch("grafit",   "grafit",   Color(0xFFA9B0BE), Color(0xFF868D9A), Color(0xFF1A1D24))
    )

    fun all(motyw: PaletteTheme = PaletteTheme.OBECNA): List<Swatch> = when (motyw) {
        PaletteTheme.OBECNA -> OBECNA
        PaletteTheme.WYRAZNA -> WYRAZNA
        PaletteTheme.PORY_DNIA -> PORY_DNIA
        PaletteTheme.JASNA -> JASNA
    }

    fun byId(id: String?, motyw: PaletteTheme = PaletteTheme.OBECNA): Swatch =
        all(motyw).firstOrNull { it.id == id } ?: all(motyw).last()

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
val EventColor = Color(0xFF7FC3F0)
