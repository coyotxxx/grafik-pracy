package pl.grafik.pracy.domain

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Odpoczynek dobowy i tygodniowy — art. 132 i 133 Kodeksu pracy.
 *
 * Liczymy wyłącznie z godzin zmian wpisanych w grafiku. To podpowiedź, nie porada prawna:
 * ruchomy czas pracy, doba pracownicza i ustalenia zakładowe mogą zmienić wynik.
 *
 * Silnik jest czysty — bez bazy i bez Androida — żeby dało się go przetestować liczbami.
 */

/** Zbyt krótka przerwa między końcem jednej zmiany a początkiem następnej. */
data class KolizjaOdpoczynku(
    /** Dzień, w którym zaczyna się zmiana po zbyt krótkiej przerwie. */
    val date: LocalDate,
    val przerwaH: Long,
    val poprzednia: Shift,
    val nastepna: Shift,
    val koniecPoprzedniej: LocalDateTime,
    val startNastepnej: LocalDateTime
) {
    /** Ile godzin brakuje do normy z art. 132. */
    val brakujeH: Long get() = PresenceEngine.MIN_REST_HOURS - przerwaH
}

/** Najdłuższa nieprzerwana przerwa w jednym tygodniu — art. 133 wymaga 35 h. */
data class TydzienOdpoczynku(
    val od: LocalDate,
    val doKiedy: LocalDate,
    val najdluzszaPrzerwaH: Long
) {
    val spelnia: Boolean get() = najdluzszaPrzerwaH >= Odpoczynek.MIN_TYGODNIOWY_H
}

object Odpoczynek {

    /** Art. 133 KP — 35 h nieprzerwanego odpoczynku w każdym tygodniu. */
    const val MIN_TYGODNIOWY_H = 35L

    /**
     * Okna zmian z wpisanych dni, posortowane po starcie.
     * Dni bez zmiany roboczej (wolne, urlop, L4) nie mają okna — są po prostu przerwą.
     */
    fun okna(dni: Map<LocalDate, DayEntry>): List<Triple<LocalDate, Shift, Pair<LocalDateTime, LocalDateTime>>> =
        dni.entries
            .mapNotNull { (d, e) ->
                val s = e.shift ?: return@mapNotNull null
                val w = PresenceEngine.shiftWindow(d, s) ?: return@mapNotNull null
                Triple(d, s, w)
            }
            .sortedBy { it.third.first }

    /**
     * Kolizje odpoczynku dobowego w podanym zbiorze dni.
     *
     * Przerwę liczymy od końca jednej zmiany do początku następnej — nocka kończy się
     * rano następnego dnia, więc to ona najczęściej zderza się z popołudniówką.
     */
    fun kolizjeDobowe(dni: Map<LocalDate, DayEntry>): List<KolizjaOdpoczynku> {
        val lista = okna(dni)
        val out = mutableListOf<KolizjaOdpoczynku>()
        for (i in 0 until lista.size - 1) {
            val (_, zmiana, oknoA) = lista[i]
            val (dataB, zmianaB, oknoB) = lista[i + 1]
            val przerwa = Duration.between(oknoA.second, oknoB.first).toHours()
            if (przerwa < 0) continue                       // nakładające się wpisy — nie nasz przypadek
            if (przerwa < PresenceEngine.MIN_REST_HOURS) {
                out += KolizjaOdpoczynku(
                    date = dataB,
                    przerwaH = przerwa,
                    poprzednia = zmiana,
                    nastepna = zmianaB,
                    koniecPoprzedniej = oknoA.second,
                    startNastepnej = oknoB.first
                )
            }
        }
        return out
    }

    /**
     * Odpoczynek tygodniowy dla każdego tygodnia miesiąca.
     *
     * Bierzemy najdłuższą nieprzerwaną przerwę, jaka mieści się w tygodniu — także tę,
     * która zaczyna się w poprzednim tygodniu albo kończy w następnym, bo odpoczynek
     * nie przestaje być odpoczynkiem przy zmianie numeru tygodnia.
     */
    fun tygodnie(dni: Map<LocalDate, DayEntry>, od: LocalDate, doKiedy: LocalDate): List<TydzienOdpoczynku> {
        val lista = okna(dni)
        val out = mutableListOf<TydzienOdpoczynku>()

        var poczatek = od.minusDays(((od.dayOfWeek.value + 6) % 7).toLong())
        while (!poczatek.isAfter(doKiedy)) {
            val koniec = poczatek.plusDays(6)
            val oknoTygodnia = poczatek.atStartOfDay() to koniec.plusDays(1).atStartOfDay()

            // Granice przerw: początek tygodnia, końce i starty zmian, koniec tygodnia.
            var najdluzsza = 0L
            var kursor = oknoTygodnia.first
            for ((_, _, okno) in lista) {
                if (!okno.second.isAfter(oknoTygodnia.first)) {
                    // zmiana skończyła się przed tygodniem — przerwa zaczyna się od jej końca
                    if (okno.second.isAfter(kursor)) kursor = okno.second
                    continue
                }
                if (!okno.first.isBefore(oknoTygodnia.second)) break
                val przerwa = Duration.between(kursor, okno.first).toHours()
                if (przerwa > najdluzsza) najdluzsza = przerwa
                if (okno.second.isAfter(kursor)) kursor = okno.second
            }
            // Ogon: od ostatniej zmiany do pierwszej zmiany po tygodniu (albo do końca tygodnia).
            val nastepnyStart = lista.firstOrNull { it.third.first >= oknoTygodnia.second }?.third?.first
            val ogon = Duration.between(kursor, nastepnyStart ?: oknoTygodnia.second).toHours()
            if (ogon > najdluzsza) najdluzsza = ogon

            out += TydzienOdpoczynku(poczatek, koniec, najdluzsza.coerceAtLeast(0))
            poczatek = poczatek.plusWeeks(1)
        }
        return out
    }

    /** Dni, na których kalendarz ma pokazać znacznik kolizji. */
    fun dniZKolizja(dni: Map<LocalDate, DayEntry>): Set<LocalDate> =
        kolizjeDobowe(dni).map { it.date }.toSet()
}
