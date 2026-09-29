package pl.grafik.pracy.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Termin ważności notatki — ile jeszcze obowiązuje.
 *
 * Powstało dla kar porządkowych: karę uważa się za niebyłą, a odpis zawiadomienia
 * usuwa z akt osobowych po roku nienagannej pracy (art. 113 § 1 Kodeksu pracy).
 * Ten sam mechanizm obsługuje badania okresowe, szkolenia i uprawnienia.
 *
 * Liczymy kalendarzowo. Przepis mówi o roku NIENAGANNEJ pracy, więc kolejna kara
 * przesuwa termin — aplikacja tego nie zgadnie i mówi o tym wprost przy wpisywaniu.
 */
object Waznosc {

    /** Domyślny termin: rok, zgodnie z art. 113 § 1 KP. */
    const val MIESIECY_KARA = 12

    /** Poniżej tylu dni liczymy w dniach — wtedy dokładność zaczyna mieć znaczenie. */
    private const val PROG_DNI = 60

    /** Przeciętna długość miesiąca — do zaokrąglania odliczania na miesiące. */
    private const val SREDNI_MIESIAC = 30.436875

    sealed interface Stan {
        /** Termin biegnie; [dni] to ile zostało. */
        data class Biegnie(val dni: Long, val blisko: Boolean) : Stan
        /** Termin minął — kara zatarta, badania nieaktualne. */
        data object Minal : Stan
    }

    fun stan(doKiedy: LocalDate, dzis: LocalDate = LocalDate.now()): Stan {
        val dni = ChronoUnit.DAYS.between(dzis, doKiedy)
        return if (dni < 0) Stan.Minal else Stan.Biegnie(dni, dni <= PROG_DNI)
    }

    /**
     * Ile zostało, po ludzku: „12 miesięcy", „36 dni", „ostatni dzień".
     *
     * Powyżej progu podajemy miesiące, bo „365 dni" czyta się gorzej niż „12 miesięcy".
     * Poniżej — dni, bo wtedy każdy dzień coś znaczy.
     */
    fun ileZostalo(doKiedy: LocalDate, dzis: LocalDate = LocalDate.now()): String? =
        when (val s = stan(doKiedy, dzis)) {
            is Stan.Minal -> null
            is Stan.Biegnie -> when {
                s.dni == 0L -> "ostatni dzień"
                s.dni == 1L -> "został 1 dzień"
                s.dni <= PROG_DNI -> "zostało ${s.dni} dni"
                else -> {
                    // Zaokrąglamy z dni, a nie liczymy pełnych miesięcy. Dodanie
                    // miesięcy potrafi cofnąć dzień (29.09 + 5 miesięcy to 28.02),
                    // więc pełne miesiące pokazałyby „5" tuż po ustawieniu sześciu.
                    val miesiace = Math.round(s.dni / SREDNI_MIESIAC)
                    if (miesiace < 1) "zostało ${s.dni} dni"
                    else "zostało $miesiace ${odmianaMiesiecy(miesiace)}"
                }
            }
        }

    private fun odmianaMiesiecy(ile: Long): String = when {
        ile == 1L -> "miesiąc"
        ile % 10 in 2..4 && ile % 100 !in 12..14 -> "miesiące"
        else -> "miesięcy"
    }
}
