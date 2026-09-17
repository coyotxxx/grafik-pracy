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
