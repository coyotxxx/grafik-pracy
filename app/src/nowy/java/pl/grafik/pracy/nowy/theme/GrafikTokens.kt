package pl.grafik.pracy.nowy.theme

import pl.grafik.pracy.R

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Tokeny UI aplikacji „Grafik pracy".
 * Wartości pochodzą 1:1 z makiet w design/mockups/ (pliki .html) — NIE ZMIENIAĆ.
 * Pełny opis: design/DESIGN_SPEC.md
 */

// ─────────────────────────────────────────────────────────────
// KOLORY — motyw ciemny
// ─────────────────────────────────────────────────────────────
object DarkTokens {
    val bg = Color(0xFF0A0D0F)
    val bgElevated = Color(0xFF111417)
    val surface = Color(0x09FFFFFF)          // rgba(255,255,255,0.035)
    val surfaceStrong = Color(0x0BFFFFFF)    // rgba(255,255,255,0.045)
    val surfaceInput = Color(0xFF13171A)
    val line = Color(0xFF1F2428)
    val lineStrong = Color(0xFF23282D)
    val lineInput = Color(0xFF262B30)
    val lineSoft = Color(0xFF2A3036)
    val navBg = Color(0xEB0D1012)            // rgba(13,16,18,0.92)
    val navLine = Color(0xFF1B1F23)
    val navActiveBg = Color(0xFF17211F)

    val ink = Color(0xFFF1F3F4)
    val inkStrong = Color(0xFFE7EAEC)
    val ink2 = Color(0xFFC5CCD1)
    val ink3 = Color(0xFFB7BEC3)
    val inkMuted = Color(0xFF97A0A7)
    val inkFaint = Color(0xFF6E777E)
    val inkDisabled = Color(0xFF4E565C)

    val accent = Color(0xFF52D0B3)
    val accentOn = Color(0xFF06231D)
    val accentTintBg = Color(0x2452D0B3)     // 0.14
    val accentTintLine = Color(0x5952D0B3)   // 0.35
    val accentGlow = Color(0x3852D0B3)       // 0.22

    val ok = Color(0xFF3FA98F)
    val warnInk = Color(0xFFFF937E)
    val warnBg = Color(0x14FF937E)           // 0.08
    val warnLine = Color(0x42FF937E)         // 0.26
    val warnInk2 = Color(0xFFE6B0A2)
}

// ─────────────────────────────────────────────────────────────
// KOLORY — motyw jasny
// ─────────────────────────────────────────────────────────────
object LightTokens {
    val bg = Color(0xFFF7F6F2)
    val surface = Color(0xFFFFFFFF)
    val line = Color(0xFFE8E6DF)
    val lineInput = Color(0xFFE4E2DB)
    val trackBg = Color(0xFFECEAE3)

    val ink = Color(0xFF15181A)
    val ink2 = Color(0xFF3E454C)
    val inkMuted = Color(0xFF5A6169)
    val inkFaint = Color(0xFF8A9099)
    val inkDisabled = Color(0xFFA9AEB3)

    val accent = Color(0xFF00876D)
    val accentTintBg = Color(0xFFE7F2EE)
    val accentTintLine = Color(0xFFCADFD8)
}

// ─────────────────────────────────────────────────────────────
// KOLORY TYPÓW DNIA
// Paleta zwalidowana pod daltonizm (ΔE ≥ 8 dla par sąsiednich). NIE PODMIENIAĆ.
// ─────────────────────────────────────────────────────────────
/**
 * Typ dnia na potrzeby kolorów kafelka.
 * UWAGA: to NIE jest `pl.grafik.pracy.domain.DayKind` — tamten mówi, czy dzień jest
 * zwykły, sobotą, niedzielą czy świętem. Nazwa zmieniona, żeby nic się nie myliło.
 */
enum class TypDnia { I, II, III, URLOP, WOLNE, POZA }

@Immutable
data class DayColors(
    val ink: Color,       // litera/oznaczenie
    val fill: Color,      // tło kafelka
    val line: Color,      // obrys kafelka
    val solid: Color      // wypełnienie na wykresach
)

object ShiftPaletteDark {
    val I = DayColors(Color(0xFFDAC559), Color(0x21DAC559), Color(0x4DDAC559), Color(0xFFAE9400))
    val II = DayColors(Color(0xFFFF937E), Color(0x1FFF937E), Color(0x47FF937E), Color(0xFFC1351E))
    val III = DayColors(Color(0xFF7ABDFF), Color(0x1F7ABDFF), Color(0x477ABDFF), Color(0xFF3587D3))
    val URLOP = DayColors(Color(0xFFF490D9), Color(0x1FF490D9), Color(0x47F490D9), Color(0xFFC04CA5))
    val WOLNE = DayColors(Color(0xFF8A939B), Color(0x08FFFFFF), Color(0xFF1F2428), Color(0xFF5FB8A0))
    val POZA = DayColors(Color(0xFF4E565C), Color.Transparent, Color(0xFF171B1E), Color(0xFF4E565C))

    fun of(kind: TypDnia) = when (kind) {
        TypDnia.I -> I; TypDnia.II -> II; TypDnia.III -> III
        TypDnia.URLOP -> URLOP; TypDnia.WOLNE -> WOLNE; TypDnia.POZA -> POZA
    }
}

object ShiftPaletteLight {
    val I = DayColors(Color(0xFF7C6800), Color(0x1AAE9400), Color(0x47AE9400), Color(0xFFAE9400))
    val II = DayColors(Color(0xFFAB331F), Color(0x17C1351E), Color(0x3DC1351E), Color(0xFFC1351E))
    val III = DayColors(Color(0xFF0465AF), Color(0x1A3587D3), Color(0x423587D3), Color(0xFF3587D3))
    val URLOP = DayColors(Color(0xFF9C3084), Color(0x17C04CA5), Color(0x3DC04CA5), Color(0xFFC04CA5))
    val WOLNE = DayColors(Color(0xFF6B7178), Color(0xFFEFEDE7), Color(0xFFE4E2DB), Color(0xFF5FB8A0))
    val POZA = DayColors(Color(0xFFA9AEB3), Color.Transparent, Color(0xFFEDEBE4), Color(0xFFA9AEB3))
}

// ─────────────────────────────────────────────────────────────
// TYPOGRAFIA
// Pliki: res/font/bricolage_grotesque_{semibold,bold}.ttf
//        res/font/plus_jakarta_sans_{regular,medium,semibold,bold}.ttf
// ─────────────────────────────────────────────────────────────
val Bricolage = FontFamily(
    Font(R.font.bricolage_grotesque_semibold, FontWeight.SemiBold),
    Font(R.font.bricolage_grotesque_bold, FontWeight.Bold)
)

val Jakarta = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold)
)

/** Cyfry tabelaryczne — obowiązkowe dla każdej liczby w UI. */
const val TNUM = "tnum"

object GrafikType {
    val h1 = TextStyle(
        fontFamily = Bricolage, fontWeight = FontWeight.Bold,
        fontSize = 27.sp, lineHeight = 27.sp, letterSpacing = (-0.81).sp
    )
    val heroNumber = TextStyle(
        fontFamily = Bricolage, fontWeight = FontWeight.Bold,
        fontSize = 46.sp, lineHeight = 41.sp, letterSpacing = (-1.38).sp,
        fontFeatureSettings = TNUM
    )
    val counter = TextStyle(
        fontFamily = Bricolage, fontWeight = FontWeight.Bold,
        fontSize = 32.sp, lineHeight = 32.sp, letterSpacing = (-0.64).sp,
        fontFeatureSettings = TNUM
    )
    val cardTitle = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    val cardTitleStrong = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    val body = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.5.sp)
    val caption = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 16.5.sp)
    val sectionLabel = TextStyle(
        fontFamily = Jakarta, fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp, letterSpacing = 0.88.sp
    )
    val micro = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.SemiBold, fontSize = 10.sp)
    val dayNumber = TextStyle(
        fontFamily = Jakarta, fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp, lineHeight = 13.sp, fontFeatureSettings = TNUM
    )
    val dayLabel = TextStyle(
        fontFamily = Jakarta, fontWeight = FontWeight.Bold,
        fontSize = 13.sp, lineHeight = 13.sp, letterSpacing = 0.78.sp
    )
}

// ─────────────────────────────────────────────────────────────
// WYMIARY
// ─────────────────────────────────────────────────────────────
object Dim {
    val screenGutter = 16.dp
    val topSafe = 44.dp
    val cardGap = 14.dp
    val cardPadding = 16.dp
    val cardPaddingCompact = 14.dp
    val gridGap = 4.dp
    val chipGap = 6.dp
    val tileGap = 8.dp

    val dayCell = 62.dp
    val dayCellEdit = 60.dp
    val touchMin = 44.dp
    val navItemMin = 48.dp

    val rCell = 14.dp
    val rCard = 22.dp
    val rCardSmall = 18.dp
    val rButton = 15.dp
    val rInput = 12.dp
    val rIconTile = 13.dp
    val rSheet = 26.dp
}

// ─────────────────────────────────────────────────────────────
// ANIMACJE — jedna krzywa dla całej aplikacji
// ─────────────────────────────────────────────────────────────
object Motion {
    val Ease = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

    const val RISE_MS = 700
    const val CELL_IN_MS = 500
    const val CELL_STAGGER_MS = 16      // index * 16 ms, start 160 ms
    const val CELL_START_MS = 160
    const val BAR_GROW_MS = 1300
    const val BAR_GROW_DELAY_MS = 300
    const val COUNT_UP_MS = 1100
    const val ARC_DRAW_MS = 1400
    const val ARC_DRAW_DELAY_MS = 250
    const val SWEEP_MS = 1500
    const val SWEEP_DELAY_MS = 350
    const val TODAY_PULSE_MS = 3200
    const val TAP_MS = 160
    const val SHEET_MS = 550

    /** Wygładzenie licznika: 1-(1-p)^3 */
    fun easeOutCubic(p: Float) = 1f - (1f - p) * (1f - p) * (1f - p)
}

// ─────────────────────────────────────────────────────────────
// TARCZA DOBY (ekran Teraz) — geometria dokładna
// ─────────────────────────────────────────────────────────────
object DialSpec {
    const val SIZE = 240f
    const val CENTER = 120f
    const val RADIUS = 96f
    const val STROKE = 14f
    const val CIRCUMFERENCE = 603.19f            // 2 * PI * 96
    const val SHIFT_ARC = 201.06f                // 8 h z 24 h
    const val ARC_ROTATION_22H = 240f            // -90 + 22*15
    /** Kąt wskazówki „teraz" dla godziny w formacie dziesiętnym (np. 12.98). */
    fun handAngle(hourDecimal: Float) = -90f + hourDecimal * 15f
}
