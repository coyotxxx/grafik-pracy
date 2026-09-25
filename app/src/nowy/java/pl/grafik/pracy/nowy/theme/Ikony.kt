package pl.grafik.pracy.nowy.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.unit.dp

/**
 * Ikony przepisane ze ścieżek SVG w design/mockups/ (pliki .html).
 * Makiety używają wyłącznie obrysów: stroke-width 1.6–1.9, zaokrąglone końce, bez wypełnienia.
 * Dlatego NIE używamy Icons.Filled.* — tamte mają inną grubość i są wypełnione.
 *
 * Kolor nadaje `Icon(tint = …)`, które nakłada filtr na cały rysunek, także na obrys.
 */
private fun ikona(grubosc: Float = 1.6f, rysuj: PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).run {
        addPath(
            pathData = androidx.compose.ui.graphics.vector.PathData { rysuj() },
            fill = null,
            stroke = SolidColor(Color.White),
            strokeLineWidth = grubosc,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        build()
    }

/** Okrąg jako dwa półłuki — PathBuilder nie zna `circle`, a makiety go używają. */
private fun PathBuilder.okrag(cx: Float, cy: Float, r: Float) {
    moveTo(cx - r, cy)
    arcToRelative(r, r, 0f, true, true, 2 * r, 0f)
    arcToRelative(r, r, 0f, true, true, -2 * r, 0f)
}

/** Zegar — zakładka „Teraz". */
val IkonaTeraz: ImageVector = ikona {
    okrag(12f, 12f, 9f)
    moveTo(12f, 7.5f); verticalLineTo(12f); lineToRelative(3f, 2f)
}

/** Kalendarz — zakładka „Grafik". */
val IkonaGrafik: ImageVector = ikona {
    // zaokrąglony prostokąt 3,4.5 · 18×16 · r3
    moveTo(3f, 7.5f)
    arcToRelative(3f, 3f, 0f, false, true, 3f, -3f)
    horizontalLineTo(18f)
    arcToRelative(3f, 3f, 0f, false, true, 3f, 3f)
    verticalLineTo(17.5f)
    arcToRelative(3f, 3f, 0f, false, true, -3f, 3f)
    horizontalLineTo(6f)
    arcToRelative(3f, 3f, 0f, false, true, -3f, -3f)
    close()
    moveTo(8f, 2.5f); verticalLineToRelative(4f)
    moveTo(16f, 2.5f); verticalLineToRelative(4f)
    moveTo(3f, 9.5f); horizontalLineTo(21f)
}

/** Słupki — zakładka „Bilans". */
val IkonaBilans: ImageVector = ikona {
    moveTo(3f, 20f); horizontalLineTo(21f)
    moveTo(7f, 20f); verticalLineToRelative(-6f)
    moveTo(12f, 20f); verticalLineTo(7f)
    moveTo(17f, 20f); verticalLineToRelative(-9f)
}

/** Dwa suwaki — zakładka „Ustawienia". */
val IkonaUstawienia: ImageVector = ikona {
    moveTo(4f, 8f); horizontalLineToRelative(14f)
    moveTo(6f, 16f); horizontalLineToRelative(14f)
    okrag(9f, 8f, 2.6f)
    okrag(16f, 16f, 2.6f)
}

// ─── ikony ekranu Grafik (Main.html) ───────────────────────────

/** Strzałka w lewo — poprzedni miesiąc. Obrys 1.8 jak w makiecie. */
val IkonaWLewo: ImageVector = ikona(1.8f) {
    moveTo(15f, 5f); lineToRelative(-7f, 7f); lineToRelative(7f, 7f)
}

/** Strzałka w prawo — następny miesiąc, szczegóły dnia. */
val IkonaWPrawo: ImageVector = ikona(1.8f) {
    moveTo(9f, 5f); lineToRelative(7f, 7f); lineToRelative(-7f, 7f)
}

/** Trójkąt ostrzegawczy — pasek kolizji odpoczynku. */
val IkonaOstrzezenie: ImageVector = ikona(1.9f) {
    moveTo(12f, 9f); verticalLineToRelative(5f)
    moveTo(12f, 17f); lineToRelative(0.01f, 0f)
    moveTo(10.3f, 4.2f)
    lineTo(2.6f, 17.4f)
    arcToRelative(2f, 2f, 0f, false, false, 1.7f, 3f)
    horizontalLineToRelative(15.4f)
    arcToRelative(2f, 2f, 0f, false, false, 1.7f, -3f)
    lineTo(13.7f, 4.2f)
    arcToRelative(2f, 2f, 0f, false, false, -3.4f, 0f)
    close()
}

/** Ołówek — „Edytuj grafik". */
val IkonaOlowek: ImageVector = ikona(1.7f) {
    moveTo(4f, 20f); horizontalLineToRelative(4f); lineToRelative(10f, -10f)
    arcToRelative(2.8f, 2.8f, 0f, true, false, -4f, -4f)
    lineTo(4f, 16f); verticalLineToRelative(4f)
    close()
}

/** Trzy kreski — filtr i widok. */
val IkonaFiltr: ImageVector = ikona(1.7f) {
    moveTo(4f, 7f); horizontalLineToRelative(16f)
    moveTo(7f, 12f); horizontalLineToRelative(10f)
    moveTo(10f, 17f); horizontalLineToRelative(4f)
}

// ─── ikony karty dnia (Day.html) ───────────────────────────────

/** Krzyżyk — zamknięcie arkusza. Obrys 2. */
val IkonaZamknij: ImageVector = ikona(2f) {
    moveTo(6f, 6f); lineToRelative(12f, 12f)
    moveTo(18f, 6f); lineTo(6f, 18f)
}

/** Minus — mniej nadgodzin. Obrys 2.2. */
val IkonaMinus: ImageVector = ikona(2.2f) {
    moveTo(5f, 12f); horizontalLineToRelative(14f)
}

/** Plus — więcej nadgodzin. Obrys 2.2. */
val IkonaPlus: ImageVector = ikona(2.2f) {
    moveTo(12f, 5f); verticalLineToRelative(14f)
    moveTo(5f, 12f); horizontalLineToRelative(14f)
}

/** Plus cieńszy — „Dodaj wydarzenie". Obrys 2. */
val IkonaPlusCienki: ImageVector = ikona(2f) {
    moveTo(12f, 5f); verticalLineToRelative(14f)
    moveTo(5f, 12f); horizontalLineToRelative(14f)
}

/** Dzwonek — przypomnienie przy wydarzeniu. */
val IkonaDzwonek: ImageVector = ikona(1.9f) {
    moveTo(18f, 8f)
    arcToRelative(6f, 6f, 0f, true, false, -12f, 0f)
    curveToRelative(0f, 6f, -2f, 7f, -2f, 7f)
    horizontalLineToRelative(16f)
    reflectiveCurveToRelative(-2f, -1f, -2f, -7f)
    close()
    moveTo(10.5f, 21f)
    arcToRelative(2f, 2f, 0f, false, false, 3f, 0f)
}

/** Kosz — usunięcie wydarzenia. */
val IkonaKosz: ImageVector = ikona(1.8f) {
    moveTo(4f, 7f); horizontalLineToRelative(16f)
    moveTo(9f, 7f); verticalLineTo(5f); horizontalLineToRelative(6f); verticalLineToRelative(2f)
    moveTo(7f, 7f); lineToRelative(1f, 13f); horizontalLineToRelative(8f); lineToRelative(1f, -13f)
}

// ─── ikony trybu edycji (Edit.html) ────────────────────────────

/** Strzałka zawracająca — „Cofnij ostatnią zmianę". */
val IkonaCofnij: ImageVector = ikona(1.8f) {
    moveTo(9f, 14f); lineToRelative(-5f, -5f); lineToRelative(5f, -5f)
    moveTo(4f, 9f); horizontalLineToRelative(9f)
    arcToRelative(7f, 7f, 0f, false, true, 0f, 14f)
    horizontalLineToRelative(-3f)
}

/** Zegar z wskazówkami — modyfikator nadgodzin w panelu edycji. */
val IkonaZegarek: ImageVector = ikona(1.9f) {
    moveTo(12f, 6f); verticalLineToRelative(6f); lineToRelative(4f, 2f)
    okrag(12f, 12f, 9f)
}


/** Strzałka w dół — rozwijany wybór miesiąca. */
val IkonaWDol: ImageVector = ikona(2f) {
    moveTo(6f, 9f); lineToRelative(6f, 6f); lineToRelative(6f, -6f)
}

/** Strzałka w górę — zwijanie rozwiniętej sekcji. */
val IkonaWGore: ImageVector = ikona(2f) {
    moveTo(6f, 15f); lineToRelative(6f, -6f); lineToRelative(6f, 6f)
}

/** Dwie strzałki w kółko — „Mój cykl". */
val IkonaCykl: ImageVector = ikona(1.7f) {
    moveTo(4f, 11f); arcToRelative(8f, 8f, 0f, false, true, 13.7f, -5.6f); lineTo(21f, 8f)
    moveTo(21f, 4f); verticalLineToRelative(4f); horizontalLineToRelative(-4f)
    moveTo(20f, 13f); arcToRelative(8f, 8f, 0f, false, true, -13.7f, 5.6f); lineTo(3f, 16f)
    moveTo(3f, 20f); verticalLineToRelative(-4f); horizontalLineToRelative(4f)
}

/** Zegar ze wskazówkami — „Czas pracy i nadgodziny". */
val IkonaCzasPracy: ImageVector = ikona(1.8f) {
    okrag(12f, 12f, 9f)
    moveTo(12f, 7f); verticalLineToRelative(5f); lineToRelative(3.5f, 2f)
}

/** Portfel z monetą — „Stawki i wypłata". */
val IkonaWyplata: ImageVector = ikona(1.7f) {
    moveTo(3f, 8.5f); arcToRelative(2.5f, 2.5f, 0f, false, true, 2.5f, -2.5f); horizontalLineTo(19f)
    arcToRelative(2f, 2f, 0f, false, true, 2f, 2f); verticalLineToRelative(9f)
    arcToRelative(2f, 2f, 0f, false, true, -2f, 2f); horizontalLineTo(5.5f)
    arcToRelative(2.5f, 2.5f, 0f, false, true, -2.5f, -2.5f); close()
    moveTo(3f, 9f); horizontalLineToRelative(18f)
    okrag(16.5f, 13.5f, 1.3f)
}

/** Księżyc — „Odpoczynek". */
val IkonaOdpoczynek: ImageVector = ikona(1.8f) {
    moveTo(17.5f, 14.5f); arcToRelative(7f, 7f, 0f, false, true, -8f, -8f)
    arcToRelative(7f, 7f, 0f, true, false, 8f, 8f); close()
}

/** Parasol — „Urlop". */
val IkonaUrlop: ImageVector = ikona(1.8f) {
    moveTo(12f, 21f); verticalLineToRelative(-8f)
    moveTo(3f, 13f); arcToRelative(9f, 9f, 0f, false, true, 18f, 0f); close()
}

/** Pinezka — „Wykrywanie pracy". */
val IkonaLokalizacja: ImageVector = ikona(1.8f) {
    moveTo(12f, 21f); reflectiveCurveToRelative(7f, -5.6f, 7f, -11f)
    arcToRelative(7f, 7f, 0f, true, false, -14f, 0f); curveToRelative(0f, 5.4f, 7f, 11f, 7f, 11f); close()
    okrag(12f, 10f, 2.6f)
}

/** Paleta — „Wygląd i kolory". */
val IkonaPaleta: ImageVector = ikona(1.7f) {
    moveTo(12f, 3.5f); arcToRelative(8.5f, 8.5f, 0f, true, false, 0f, 17f)
    curveToRelative(1.1f, 0f, 1.9f, -0.9f, 1.9f, -1.9f); curveToRelative(0f, -0.5f, -0.2f, -0.9f, -0.5f, -1.2f)
    curveToRelative(-0.3f, -0.3f, -0.5f, -0.7f, -0.5f, -1.2f); curveToRelative(0f, -1f, 0.8f, -1.8f, 1.8f, -1.8f)
    horizontalLineToRelative(1.1f); arcToRelative(4.7f, 4.7f, 0f, false, false, 4.7f, -4.7f)
    curveToRelative(0f, -3.4f, -3.8f, -6.2f, -8.5f, -6.2f); close()
    okrag(7.8f, 11.2f, 1.1f)
    okrag(11f, 7.8f, 1.1f)
    okrag(15.2f, 8.6f, 1.1f)
}

/** Baza danych — „Dane i kopia". */
val IkonaDane: ImageVector = ikona(1.8f) {
    moveTo(4f, 7f); curveToRelative(0f, -1.7f, 3.6f, -3f, 8f, -3f); reflectiveCurveToRelative(8f, 1.3f, 8f, 3f)
    reflectiveCurveToRelative(-3.6f, 3f, -8f, 3f); reflectiveCurveToRelative(-8f, -1.3f, -8f, -3f); close()
    moveTo(4f, 7f); verticalLineToRelative(10f); curveToRelative(0f, 1.7f, 3.6f, 3f, 8f, 3f)
    reflectiveCurveToRelative(8f, -1.3f, 8f, -3f); verticalLineTo(7f)
    moveTo(4f, 12f); curveToRelative(0f, 1.7f, 3.6f, 3f, 8f, 3f); reflectiveCurveToRelative(8f, -1.3f, 8f, -3f)
}

/** Kółko z „i" — „O aplikacji". */
/** Tort ze świeczką — uroczystości (urodziny, imieniny, rocznice). */
val IkonaTort: ImageVector = ikona(1.8f) {
    moveTo(3.5f, 20.5f); horizontalLineToRelative(17f)
    moveTo(5f, 20.5f); verticalLineTo(13.5f)
    horizontalLineToRelative(14f); verticalLineTo(20.5f)
    moveTo(5f, 16.8f); horizontalLineToRelative(14f)
    moveTo(12f, 13.5f); verticalLineTo(9.6f)
    moveTo(12f, 6.2f)
    curveToRelative(1.5f, 1.5f, 1.5f, 3.4f, 0f, 3.4f)
    curveToRelative(-1.5f, 0f, -1.5f, -1.9f, 0f, -3.4f)
}

val IkonaInfo: ImageVector = ikona(1.8f) {
    okrag(12f, 12f, 9f)
    moveTo(12f, 11f); verticalLineToRelative(5f)
    moveTo(12f, 8f); lineToRelative(0.01f, 0f)
}

/** Ptaszek — wybrany system pracy, potwierdzenia. */
val IkonaPtaszek: ImageVector = ikona(3f) {
    moveTo(5f, 12.5f); lineToRelative(5f, 5f); lineToRelative(9f, -11f)
}

/** Celownik — „zapisz miejsce pracy". */
val IkonaCelownik: ImageVector = ikona(2f) {
    okrag(12f, 12f, 3f)
    okrag(12f, 12f, 8f)
    moveTo(12f, 2f); verticalLineToRelative(2f)
    moveTo(12f, 20f); verticalLineToRelative(2f)
    moveTo(2f, 12f); horizontalLineToRelative(2f)
    moveTo(20f, 12f); horizontalLineToRelative(2f)
}

/** Fale Wi-Fi — firmowa sieć. */
val IkonaWifi: ImageVector = ikona(1.9f) {
    moveTo(4.5f, 9.5f); arcToRelative(11f, 11f, 0f, false, true, 15f, 0f)
    moveTo(7.5f, 13f); arcToRelative(7f, 7f, 0f, false, true, 9f, 0f)
    moveTo(10.5f, 16.5f); arcToRelative(3f, 3f, 0f, false, true, 3f, 0f)
    moveTo(12f, 20f); lineToRelative(0.01f, 0f)
}

// ─── ikony ekranu „Dane i kopia" (Data.html) ───────────────────

/** Strzałka w dół nad kreską — „Zapisz kopię". */
val IkonaZapisz: ImageVector = ikona(2f) {
    moveTo(12f, 4f); verticalLineToRelative(11f)
    moveTo(8f, 11f); lineToRelative(4f, 4f); lineToRelative(4f, -4f)
    moveTo(5f, 19f); horizontalLineToRelative(14f)
}

/** Strzałka w górę nad kreską — „Wyślij kopię". */
val IkonaWyslij: ImageVector = ikona(1.8f) {
    moveTo(12f, 16f); verticalLineTo(4f)
    moveTo(8f, 8f); lineToRelative(4f, -4f); lineToRelative(4f, 4f)
    moveTo(5f, 20f); horizontalLineToRelative(14f)
}

/** Strzałka w górę spod kreski — „Wczytaj z pliku". */
val IkonaWczytaj: ImageVector = ikona(1.8f) {
    moveTo(12f, 20f); verticalLineTo(9f)
    moveTo(8f, 13f); lineToRelative(4f, -4f); lineToRelative(4f, 4f)
    moveTo(5f, 4f); horizontalLineToRelative(14f)
}

/** Kartka z zagiętym rogiem — pozycja na liście kopii. */
val IkonaPlik: ImageVector = ikona(1.7f) {
    moveTo(5f, 4f); horizontalLineToRelative(9f); lineToRelative(5f, 5f); verticalLineToRelative(11f)
    arcToRelative(1f, 1f, 0f, false, true, -1f, 1f)
    horizontalLineTo(5f)
    arcToRelative(1f, 1f, 0f, false, true, -1f, -1f)
    verticalLineTo(5f)
    arcToRelative(1f, 1f, 0f, false, true, 1f, -1f)
    close()
    moveTo(14f, 4f); verticalLineToRelative(5f); horizontalLineToRelative(5f)
}

/** Dwie strzałki w kole — „Przywróć cały grafik do cyklu". */
val IkonaOdswiez: ImageVector = ikona(1.8f) {
    moveTo(4f, 11f)
    arcToRelative(8f, 8f, 0f, false, true, 13.7f, -5.6f)
    lineTo(21f, 8f)
    moveTo(21f, 4f); verticalLineToRelative(4f); horizontalLineToRelative(-4f)
    moveTo(20f, 13f)
    arcToRelative(8f, 8f, 0f, false, true, -13.7f, 5.6f)
    lineTo(3f, 16f)
    moveTo(3f, 20f); verticalLineToRelative(-4f); horizontalLineToRelative(4f)
}
