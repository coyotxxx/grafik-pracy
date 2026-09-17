package pl.grafik.pracy.nowy.ui

import pl.grafik.pracy.domain.Shift
import pl.grafik.pracy.nowy.theme.TypDnia
import pl.grafik.pracy.ui.Tool

/** Zmiana z modelu domeny → typ dnia w palecie projektu. */
fun typDniaZ(s: Shift?): TypDnia = when (s) {
    Shift.I -> TypDnia.I
    Shift.II -> TypDnia.II
    Shift.III -> TypDnia.III
    Shift.URLOP -> TypDnia.URLOP
    else -> TypDnia.WOLNE
}

/** Narzędzie malowania → zmiana, którą ustawia. */
fun shiftZNarzedzia(t: Tool): Shift? = when (t) {
    Tool.I -> Shift.I
    Tool.II -> Shift.II
    Tool.III -> Shift.III
    Tool.W5 -> Shift.W5
    Tool.WS -> Shift.WS
    Tool.DWN -> Shift.DWN
    Tool.BWN -> Shift.BWN
    Tool.URLOP -> Shift.URLOP
    Tool.L4 -> Shift.L4
    else -> null
}
