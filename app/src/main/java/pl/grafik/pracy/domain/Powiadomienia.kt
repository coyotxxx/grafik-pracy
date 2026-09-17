package pl.grafik.pracy.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Kiedy aplikacja ma się odezwać. Czysta logika — bez Androida i bez bazy,
 * żeby dało się ją sprawdzić liczbami, a nie klikaniem po telefonie.
 */

/** Ustawienia powiadomień. */
data class PowiadomieniaCfg(
    /** Przypomnienia o wydarzeniach w ogóle. */
    val wydarzenia: Boolean = true,
    /** Wieczorem dzień wcześniej — plan na jutro. */
    val dzienWczesniej: Boolean = true,
    val godzinaWieczorna: Int = 18,
    /** Krótko przed samym wydarzeniem. */
    val wDniu: Boolean = true,
    val wyprzedzenieMin: Int = 60,
    /** Przypomnienie o starcie zmiany. */
    val przedZmiana: Boolean = false,
    val przedZmianaMin: Int = 60,
    /** Nie budzić w trakcie nocki ani zaraz po niej. */
    val ciszaNaNocce: Boolean = true
)

object PlanPowiadomien {

    /** Ile godzin po nocce jeszcze nie wybudzamy. */
    const val CISZA_PO_NOCCE_H = 6L

    /** Dopuszczalne wyprzedzenia z makiety: 15 min, 30 min, 1 h, 2 h. */
    val WYPRZEDZENIA = listOf(15, 30, 60, 120)

    /**
     * Moment przypomnienia o wydarzeniu w dniu, w którym ono wypada.
     * Puste godziny (wydarzenie całodniowe) nie dostają przypomnienia „krótko przed".
     */
    fun przedWydarzeniem(data: LocalDate, godzina: String, wyprzedzenieMin: Int): LocalDateTime? {
        val czas = parsujGodzine(godzina) ?: return null
        return data.atTime(czas).minusMinutes(wyprzedzenieMin.toLong())
    }

    /** Moment przypomnienia o starcie zmiany. Dni wolne nie mają startu. */
    fun przedZmiana(data: LocalDate, zmiana: Shift?, wyprzedzenieMin: Int): LocalDateTime? {
        val okno = PresenceEngine.shiftWindow(data, zmiana) ?: return null
        return okno.first.minusMinutes(wyprzedzenieMin.toLong())
    }

    /**
     * Cisza na nocce: powiadomienie, które wypadłoby w trakcie zmiany nocnej albo
     * do sześciu godzin po niej, przesuwamy na koniec tego okna.
     *
     * @param zmianaDnia skąd wziąć zmianę danego dnia — zwykle grafik
     * @return moment po przesunięciu; ten sam, gdy nic nie koliduje
     */
    fun zCiszaNaNocce(
        kiedy: LocalDateTime,
        zmianaDnia: (LocalDate) -> Shift?
    ): LocalDateTime {
        // Nocka mogła zacząć się poprzedniego dnia, więc sprawdzamy oba.
        for (dzien in listOf(kiedy.toLocalDate().minusDays(1), kiedy.toLocalDate())) {
            if (zmianaDnia(dzien) != Shift.III) continue
            val okno = PresenceEngine.shiftWindow(dzien, Shift.III) ?: continue
            val koniecCiszy = okno.second.plusHours(CISZA_PO_NOCCE_H)
            if (!kiedy.isBefore(okno.first) && kiedy.isBefore(koniecCiszy)) return koniecCiszy
        }
        return kiedy
    }

    /** „15 min", „30 min", „1 h", „2 h" — etykieta wyprzedzenia. */
    fun etykietaWyprzedzenia(minuty: Int): String =
        if (minuty < 60) "$minuty min" else "${minuty / 60} h"

    /** „9:20", „09:20", „9" → LocalTime. Puste i bzdurne wartości dają null. */
    fun parsujGodzine(s: String): LocalTime? {
        val czesci = s.trim().split(":", ".")
        val g = czesci.getOrNull(0)?.toIntOrNull() ?: return null
        val m = czesci.getOrNull(1)?.toIntOrNull() ?: 0
        if (g !in 0..23 || m !in 0..59) return null
        return LocalTime.of(g, m)
    }
}
