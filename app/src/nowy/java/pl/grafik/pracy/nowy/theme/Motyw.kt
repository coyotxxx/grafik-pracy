package pl.grafik.pracy.nowy.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Motyw aplikacji — ciemny albo jasny, z tym samym kompletem tokenów.
 *
 * Ekrany nie sięgają po `DarkTokens` ani `LightTokens` wprost, tylko po [Tokeny],
 * czyli po ten zestaw, który akurat obowiązuje. Dzięki temu zmiana motywu nie wymaga
 * dotykania ekranów, a nowy ekran jest jasny i ciemny od razu.
 *
 * Wartości obu zestawów pochodzą z makiet: ciemne z `Month.html` i reszty,
 * jasne z `Month-Light.html`.
 */
@Immutable
data class Tokens(
    val bg: Color,
    val bgElevated: Color,
    val surface: Color,
    val surfaceStrong: Color,
    val surfaceInput: Color,
    val line: Color,
    val lineStrong: Color,
    val lineInput: Color,
    val lineSoft: Color,
    val navBg: Color,
    val navLine: Color,
    val navActiveBg: Color,

    val ink: Color,
    val inkStrong: Color,
    val ink2: Color,
    val ink3: Color,
    val inkMuted: Color,
    val inkFaint: Color,
    val inkDisabled: Color,

    val accent: Color,
    val accentOn: Color,
    val accentTintBg: Color,
    val accentTintLine: Color,
    val accentGlow: Color,

    val ok: Color,
    val warnInk: Color,
    val warnBg: Color,
    val warnLine: Color,
    val warnInk2: Color,

    /** Tor paska postępu — w ciemnym prawie czarny, w jasnym piaskowy. */
    val trackBg: Color,
    /** Podpis przy dużej liczbie na kolorowej karcie (urlop, wypłata). */
    val inkNaKarcie: Color,
    /** Szarość ikon strzałek i drobnych znaczników. */
    val inkIkona: Color,
    /** Przyciemnienie pod treścią: w ciemnym czerń, w jasnym ledwie widoczny cień. */
    val naklad: Color,
    /** Pierścień tarczy doby na ekranie „Teraz". */
    val tarczaTor: Color,
    /** Kreski godzin na tarczy. */
    val tarczaKreski: Color,
    /** Poświaty tła: górna niebieska i dolna zielona. */
    val haloGora: Color,
    val haloDol: Color,
    /** Czy to motyw jasny — kilka miejsc rysuje się inaczej (cienie, przezroczystości). */
    val jasny: Boolean
)

/** Motyw ciemny — wartości 1:1 z `DarkTokens`. */
val TOKENY_CIEMNE = Tokens(
    bg = DarkTokens.bg,
    bgElevated = DarkTokens.bgElevated,
    surface = DarkTokens.surface,
    surfaceStrong = DarkTokens.surfaceStrong,
    surfaceInput = DarkTokens.surfaceInput,
    line = DarkTokens.line,
    lineStrong = DarkTokens.lineStrong,
    lineInput = DarkTokens.lineInput,
    lineSoft = DarkTokens.lineSoft,
    navBg = DarkTokens.navBg,
    navLine = DarkTokens.navLine,
    navActiveBg = DarkTokens.navActiveBg,
    ink = DarkTokens.ink,
    inkStrong = DarkTokens.inkStrong,
    ink2 = DarkTokens.ink2,
    ink3 = DarkTokens.ink3,
    inkMuted = DarkTokens.inkMuted,
    inkFaint = DarkTokens.inkFaint,
    inkDisabled = DarkTokens.inkDisabled,
    accent = DarkTokens.accent,
    accentOn = DarkTokens.accentOn,
    accentTintBg = DarkTokens.accentTintBg,
    accentTintLine = DarkTokens.accentTintLine,
    accentGlow = DarkTokens.accentGlow,
    ok = DarkTokens.ok,
    warnInk = DarkTokens.warnInk,
    warnBg = DarkTokens.warnBg,
    warnLine = DarkTokens.warnLine,
    warnInk2 = DarkTokens.warnInk2,
    trackBg = Color(0xFF1E2327),
    inkNaKarcie = Color(0xFFD7DBDE),
    inkIkona = Color(0xFF8A939B),
    naklad = Color(0x4D000000),
    tarczaTor = Color(0xFF191D21),
    tarczaKreski = Color(0xFF333A40),
    haloGora = Color(0xFF3587D3),
    haloDol = Color(0xFF52D0B3),
    jasny = false
)

/**
 * Motyw jasny. Podstawa z `LightTokens` i makiety `Month-Light.html`; pola, których
 * makieta jasna nie pokazuje wprost, wyprowadzone z jej palety, żeby kontrast
 * trzymał się tych samych proporcji co w ciemnym.
 */
val TOKENY_JASNE = Tokens(
    bg = LightTokens.bg,                          // #F7F6F2
    bgElevated = Color(0xFFFFFFFF),
    surface = LightTokens.surface,                // #FFFFFF
    surfaceStrong = Color(0xFFFFFFFF),
    surfaceInput = Color(0xFFF1EFE9),
    line = LightTokens.line,                      // #E8E6DF
    lineStrong = Color(0xFFE4E2DB),
    lineInput = LightTokens.lineInput,            // #E4E2DB
    lineSoft = Color(0xFFDCD9D1),
    navBg = Color(0xFFFFFFFF),
    navLine = LightTokens.line,
    navActiveBg = LightTokens.accentTintBg,       // #E7F2EE
    ink = LightTokens.ink,                        // #15181A
    inkStrong = Color(0xFF22272B),
    ink2 = LightTokens.ink2,                      // #3E454C
    ink3 = Color(0xFF4A5158),
    inkMuted = LightTokens.inkMuted,              // #5A6169
    inkFaint = LightTokens.inkFaint,              // #8A9099
    inkDisabled = LightTokens.inkDisabled,        // #A9AEB3
    accent = LightTokens.accent,                  // #00876D
    accentOn = Color(0xFFFFFFFF),
    accentTintBg = LightTokens.accentTintBg,      // #E7F2EE
    accentTintLine = LightTokens.accentTintLine,  // #CADFD8
    accentGlow = Color(0x2900876D),
    ok = Color(0xFF00876D),
    warnInk = Color(0xFFAB331F),
    warnBg = Color(0x14C1351E),
    warnLine = Color(0x3DC1351E),
    warnInk2 = Color(0xFF8C4433),
    trackBg = LightTokens.trackBg,                // #ECEAE3
    inkNaKarcie = LightTokens.inkMuted,           // #5A6169
    inkIkona = LightTokens.inkFaint,              // #8A9099
    naklad = Color(0x0F000000),
    tarczaTor = Color(0xFFE6E3DB),
    tarczaKreski = Color(0xFFC8C5BC),
    haloGora = Color(0xFF3587D3),
    haloDol = Color(0xFF00876D),
    jasny = true
)

/**
 * Obowiązujący zestaw tokenów. Ustawiany raz, przy budowie ekranu — ekrany czytają
 * go przez [Tokeny].
 */
val LocalTokeny = compositionLocalOf { TOKENY_CIEMNE }

/**
 * Kolory motywu, który akurat obowiązuje.
 *
 * Pisze się je tak samo jak dawne `DarkTokens.ink`, tylko bez wskazywania motywu:
 * `Tokeny.ink`. Działa wyłącznie w funkcjach `@Composable` — poza nimi trzeba wziąć
 * zestaw z parametru.
 */
val Tokeny: Tokens
    @Composable @ReadOnlyComposable get() = LocalTokeny.current

/** Paleta typów dnia dopasowana do motywu. */
val LocalPaleta = compositionLocalOf { PaletaDni(false) }

/** Kolory zmian dla motywu, który obowiązuje. */
val Paleta: PaletaDni
    @Composable @ReadOnlyComposable get() = LocalPaleta.current

/**
 * Kolory typów dnia. Obie palety są zwalidowane pod daltonizm — jasna nie jest
 * przyciemnioną wersją ciemnej, tylko osobnym zestawem z makiet.
 */
@Immutable
class PaletaDni(private val jasna: Boolean) {
    val I: DayColors get() = if (jasna) ShiftPaletteLight.I else ShiftPaletteDark.I
    val II: DayColors get() = if (jasna) ShiftPaletteLight.II else ShiftPaletteDark.II
    val III: DayColors get() = if (jasna) ShiftPaletteLight.III else ShiftPaletteDark.III
    val URLOP: DayColors get() = if (jasna) ShiftPaletteLight.URLOP else ShiftPaletteDark.URLOP
    val WOLNE: DayColors get() = if (jasna) ShiftPaletteLight.WOLNE else ShiftPaletteDark.WOLNE
    val POZA: DayColors get() = if (jasna) ShiftPaletteLight.POZA else ShiftPaletteDark.POZA

    fun of(kind: TypDnia): DayColors = when (kind) {
        TypDnia.I -> I; TypDnia.II -> II; TypDnia.III -> III
        TypDnia.URLOP -> URLOP; TypDnia.WOLNE -> WOLNE; TypDnia.POZA -> POZA
    }
}

val PALETA_CIEMNA = PaletaDni(false)
val PALETA_JASNA = PaletaDni(true)

/** Który motyw ma obowiązywać. „Systemowy" idzie za ustawieniem telefonu. */
enum class TrybMotywu(val etykieta: String) {
    CIEMNY("ciemny"),
    JASNY("jasny"),
    SYSTEMOWY("systemowy")
}
