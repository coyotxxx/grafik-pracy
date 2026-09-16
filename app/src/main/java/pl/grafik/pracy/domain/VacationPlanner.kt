package pl.grafik.pracy.domain

import java.time.LocalDate

/**
 * Propozycja: weź urlop w tych dniach, a dostaniesz tyle wolnego pod rząd.
 */
data class VacationSuggestion(
    /** Dni, na które trzeba wziąć urlop. */
    val urlop: List<LocalDate>,
    /** Pierwszy i ostatni dzień całego wolnego bloku, jaki z tego wyjdzie. */
    val wolneOd: LocalDate,
    val wolneDo: LocalDate,
    /** Co tworzy ten blok poza urlopem — święta w środku. */
    val swieta: List<LocalDate>
) {
    val koszt: Int get() = urlop.size
    val dlugosc: Int get() = (java.time.temporal.ChronoUnit.DAYS.between(wolneOd, wolneDo) + 1).toInt()
    /** Ile dni wolnego za jeden dzień urlopu — im więcej, tym lepszy interes. */
    val oplacalnosc: Double get() = if (koszt == 0) 0.0 else dlugosc.toDouble() / koszt
}

/**
 * Szuka momentów, w których kilka dni urlopu daje długi ciąg wolnego.
 *
 * Zasada: wolne dni z grafiku (w5, wś, święta, już wzięty urlop) tworzą wyspy,
 * a dni pracy je rozdzielają. Jeśli wyspy dzieli krótki odcinek pracy, opłaca się
 * go „przepłacić" urlopem i połączyć wszystko w jeden długi blok.
 */
object VacationPlanner {

    /** Dłuższych odcinków pracy nie proponujemy — to już nie jest długi weekend, tylko zwykły urlop. */
    const val MAX_DNI_URLOPU = 5

    /**
     * @param wolny czy dany dzień jest wolny od pracy (grafik + święta + już wzięty urlop)
     * @param budzet ile dni urlopu użytkownik ma jeszcze do dyspozycji
     */
    fun zaproponuj(
        od: LocalDate,
        do_: LocalDate,
        wolny: (LocalDate) -> Boolean,
        swieto: (LocalDate) -> Boolean,
        budzet: Int,
        ile: Int = 8
    ): List<VacationSuggestion> {
        if (od.isAfter(do_) || budzet <= 0) return emptyList()

        val dni = generateSequence(od) { d -> d.plusDays(1).takeIf { !it.isAfter(do_) } }.toList()
        val wolne = dni.associateWith(wolny)

        val propozycje = mutableListOf<VacationSuggestion>()

        var i = 0
        while (i < dni.size) {
            if (wolne[dni[i]] == true) { i++; continue }
            // odcinek pracy
            var j = i
            while (j < dni.size && wolne[dni[j]] == false) j++
            val odcinek = dni.subList(i, j)

            if (odcinek.size in 1..minOf(MAX_DNI_URLOPU, budzet)) {
                // most: bierzemy cały odcinek i łączymy wolne po obu stronach
                val start = cofnijDoPoczatkuWolnego(dni, wolne, i)
                val koniec = przesunDoKoncaWolnego(dni, wolne, j - 1)
                // most ma sens tylko wtedy, gdy po którejś stronie JEST wolne
                if (start < i || koniec > j - 1) {
                    propozycje += VacationSuggestion(
                        urlop = odcinek.toList(),
                        wolneOd = dni[start],
                        wolneDo = dni[koniec],
                        swieta = dni.subList(start, koniec + 1).filter(swieto)
                    )
                }
            }
            i = j
        }

        return propozycje
            // 1.6 zamiast 2.0: w cyklu 4-brygadowym wolne bloki są krótkie, więc
            // „4 dni urlopu za 7 dni wolnego" to nadal dobry interes, choć nie dwukrotny.
            .filter { it.koszt <= budzet && it.oplacalnosc >= 1.6 }
            .sortedWith(
                compareByDescending<VacationSuggestion> { it.oplacalnosc }
                    .thenByDescending { it.swieta.size }      // święta w środku to dodatkowa wartość
                    .thenByDescending { it.dlugosc }
            )
            // Cykl się powtarza, więc bez tego lista byłaby dwunastoma kopiami tej samej
            // propozycji z jednego miesiąca. Pokazujemy najlepszą z każdego miesiąca.
            .distinctBy { java.time.YearMonth.from(it.wolneOd) }
            .take(ile)
    }

    private fun cofnijDoPoczatkuWolnego(
        dni: List<LocalDate>, wolne: Map<LocalDate, Boolean>, odIndeksu: Int
    ): Int {
        var k = odIndeksu - 1
        while (k >= 0 && wolne[dni[k]] == true) k--
        return k + 1
    }

    private fun przesunDoKoncaWolnego(
        dni: List<LocalDate>, wolne: Map<LocalDate, Boolean>, odIndeksu: Int
    ): Int {
        var k = odIndeksu + 1
        while (k < dni.size && wolne[dni[k]] == true) k++
        return k - 1
    }
}
