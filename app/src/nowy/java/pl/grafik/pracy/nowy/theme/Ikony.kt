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

