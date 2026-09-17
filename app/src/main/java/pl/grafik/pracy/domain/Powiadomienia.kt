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
    val ciszaNaNocce: Boolean = true,
    /** W niedzielę wieczorem, gdy od jutra wypada inna zmiana niż w mijającym tygodniu. */
    val zmianaBrygady: Boolean = true,
    /** Gdy nadgodziny przekroczą 80 % limitu okresu rozliczeniowego. */
    val limitNadgodzin: Boolean = false,
    /** W sierpniu, gdy został urlop zaległy — termin to 30 września (art. 168 KP). */
    val zaleglyUrlop: Boolean = true
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

    /**
     * Powiadomienia warunkowe — odzywają się tylko wtedy, gdy grafik albo kalendarz
     * dają po temu powód. Każda z tych funkcji zwraca gotową treść albo null,
     * gdy nie ma o czym mówić.
     */

    /** Po ilu procentach limitu okresu ostrzegamy przed nadgodzinami. */
    const val PROG_LIMITU = 0.80

    /** Miesiąc, w którym przypominamy o zaległym urlopie — zostają dwa na jego wybranie. */
    const val MIESIAC_ZALEGLEGO = 8

    /**
     * Zmiana brygady: w niedzielę wieczorem sprawdzamy, czy poniedziałek zaczyna
     * inną zmianę niż ta, na której chodziło się w mijającym tygodniu.
     *
     * @param mijajacyTydzien zmiany od poniedziałku do soboty, w kolejności
     * @param jutrzejsza zmiana poniedziałkowa
     */
    fun zmianaBrygady(dzis: LocalDate, mijajacyTydzien: List<Shift?>, jutrzejsza: Shift?): String? {
        if (dzis.dayOfWeek != java.time.DayOfWeek.SUNDAY) return null
        if (jutrzejsza == null || !jutrzejsza.isWork) return null
        val ostatnia = mijajacyTydzien.lastOrNull { it?.isWork == true } ?: return null
        if (ostatnia == jutrzejsza) return null
        return "Od jutra ${opisZmiany(jutrzejsza)} — w mijającym tygodniu była ${opisZmiany(ostatnia)}."
    }

    private fun opisZmiany(s: Shift): String = when (s) {
        Shift.I -> "I zmiana"
        Shift.II -> "II zmiana"
        Shift.III -> "zmiana nocna"
        else -> s.code
    }

    /**
     * Limit nadgodzin: ostrzegamy po przekroczeniu [PROG_LIMITU], ale jeszcze przed
     * samym limitem — po jego przekroczeniu ostrzeżenie nie ma już czego zapowiadać.
     */
    fun limitNadgodzin(wykorzystane: Int, limit: Int): String? {
        if (limit <= 0) return null
        val udzial = wykorzystane.toDouble() / limit
        if (udzial < PROG_LIMITU) return null
        val zostalo = limit - wykorzystane
        return if (zostalo > 0)
            "Masz $wykorzystane z $limit h nadgodzin w tym okresie — zostało $zostalo h."
        else
            "Masz $wykorzystane z $limit h nadgodzin — limit okresu jest wyczerpany."
    }

    /**
     * Zaległy urlop: raz, w sierpniu, gdy coś jeszcze zostało.
     * Art. 168 KP każe go wybrać do 30 września.
     */
    fun zaleglyUrlop(dzis: LocalDate, dni: Int): String? {
        if (dzis.monthValue != MIESIAC_ZALEGLEGO || dni <= 0) return null
        val termin = VacationCfg.terminZaleglego(dzis.year)
        val zostalo = java.time.temporal.ChronoUnit.DAYS.between(dzis, termin)
        return "Został Ci $dni ${dniOdmiana(dni)} urlopu zaległego — do wybrania w $zostalo dni, " +
            "termin to 30 września."
    }

    private fun dniOdmiana(ile: Int): String = when {
        ile == 1 -> "dzień"
        ile % 10 in 2..4 && ile % 100 !in 12..14 -> "dni"
        else -> "dni"
    }

    /** „9:20", „09:20", „9" → LocalTime. Puste i bzdurne wartości dają null. */
    fun parsujGodzine(s: String): LocalTime? {
        val czesci = s.trim().split(":", ".")
        val g = czesci.getOrNull(0)?.toIntOrNull() ?: return null
        val m = czesci.getOrNull(1)?.toIntOrNull() ?: 0
        if (g !in 0..23 || m !in 0..59) return null
        return LocalTime.of(g, m)
    }
}
