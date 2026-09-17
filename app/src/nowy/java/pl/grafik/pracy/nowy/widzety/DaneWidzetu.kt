package pl.grafik.pracy.nowy.widzety

import android.content.Context
import pl.grafik.pracy.data.AppDb
import pl.grafik.pracy.data.EventRow
import pl.grafik.pracy.data.SettingsStore
import pl.grafik.pracy.domain.CycleGenerator
import pl.grafik.pracy.domain.DayEntry
import pl.grafik.pracy.domain.PresenceEngine
import pl.grafik.pracy.domain.Settlement
import pl.grafik.pracy.domain.Shift
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

/**
 * Stan, który pokazują widżety. Liczony dokładnie tak samo jak w aplikacji:
 * cykl daje tło, ręczne wpisy mają pierwszeństwo.
 *
 * Wszystko powstaje w telefonie i nigdzie nie wychodzi — widżet czyta tę samą bazę
 * co ekran „Teraz".
 */
data class DaneWidzetu(
    val dzis: LocalDate = LocalDate.now(),
    val zmianaDzis: Shift? = null,
    val oknoDzis: Pair<LocalDateTime, LocalDateTime>? = null,
    val zmianaJutro: Shift? = null,
    val oknoJutro: Pair<LocalDateTime, LocalDateTime>? = null,
    /** Minuty do startu zmiany, a gdy zmiana trwa — do jej końca. */
    val doStartuMin: Long = 0,
    /** Czy zmiana właśnie trwa (wtedy odliczamy do końca). */
    val trwa: Boolean = false,
    val godzinyMiesiaca: Int = 0,
    val normaMiesiaca: Int = 0,
    /** Siedem dni bieżącego tygodnia, od poniedziałku. */
    val tydzien: List<DayEntry> = emptyList(),
    val godzinyTygodnia: Int = 0,
    /** Najbliższe wydarzenie z dwóch dób — pokazujemy je w stopce widżetu tygodnia. */
    val wydarzenie: EventRow? = null,
    val dataWydarzenia: LocalDate? = null
) {
    val udzialMiesiaca: Float
        get() = if (normaMiesiaca > 0) (godzinyMiesiaca.toFloat() / normaMiesiaca).coerceIn(0f, 1f) else 0f

    /** „9:01" — tak samo jak na ekranie „Teraz". */
    val odliczanie: String
        get() = "%d:%02d".format(doStartuMin / 60, doStartuMin % 60)

    companion object {

        /** Ile godzin liczymy za jeden dzień pracy. */
        private const val H_ZMIANY = 8

        suspend fun wczytaj(ctx: Context, teraz: LocalDateTime = LocalDateTime.now()): DaneWidzetu {
            val dzis = teraz.toLocalDate()
            val cykl = SettingsStore(ctx).config.first()

            val poniedzialek = dzis.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val niedziela = poniedzialek.plusDays(6)
            val miesiac = YearMonth.from(dzis)

            // Bierzemy jednym zapytaniem wszystko, czego dotyka którykolwiek widżet.
            val od = minOf(poniedzialek, miesiac.atDay(1))
            val doKiedy = maxOf(niedziela, miesiac.atEndOfMonth(), dzis.plusDays(1))
            val dni = grafik(ctx, cykl, od, doKiedy)

            val zmianaDzis = dni[dzis]?.shift
            val zmianaJutro = dni[dzis.plusDays(1)]?.shift
            val oknoDzis = PresenceEngine.shiftWindow(dzis, zmianaDzis)
            val oknoJutro = PresenceEngine.shiftWindow(dzis.plusDays(1), zmianaJutro)

            // Do startu dzisiejszej zmiany; gdy już trwa — do końca; gdy minęła — do jutrzejszej.
            val trwa = oknoDzis != null && !teraz.isBefore(oknoDzis.first) && teraz.isBefore(oknoDzis.second)
            val cel = when {
                trwa -> oknoDzis!!.second
                oknoDzis != null && teraz.isBefore(oknoDzis.first) -> oknoDzis.first
                else -> oknoJutro?.first
            }
            val doStartu = cel?.let { Duration.between(teraz, it).toMinutes().coerceAtLeast(0) } ?: 0

            val wMiesiacu = dni.values
                .filter { YearMonth.from(it.date) == miesiac && it.date <= dzis }
                .sumOf { godzinyDnia(it) }

            val tydzien = (0..6).map { i ->
                val d = poniedzialek.plusDays(i.toLong())
                dni[d] ?: DayEntry(date = d, shift = null)
            }

            val wydarzenia = AppDb.get(ctx).eventDao()
                .rangeOnce(dzis.toString(), dzis.plusDays(1).toString())
            val najblizsze = wydarzenia.minByOrNull { it.date + it.time.padStart(5, '0') }

            return DaneWidzetu(
                dzis = dzis,
                zmianaDzis = zmianaDzis,
                oknoDzis = oknoDzis,
                zmianaJutro = zmianaJutro,
                oknoJutro = oknoJutro,
                doStartuMin = doStartu,
                trwa = trwa,
                godzinyMiesiaca = wMiesiacu,
                normaMiesiaca = Settlement.statutoryNorm(miesiac),
                tydzien = tydzien,
                godzinyTygodnia = tydzien.sumOf { godzinyDnia(it) },
                wydarzenie = najblizsze,
                dataWydarzenia = najblizsze?.let { LocalDate.parse(it.date) }
            )
        }

        /**
         * Godziny dnia liczone dokładnie tak, jak w Bilansie: przepracowane plus dzień
         * urlopu po 8 h. Bez urlopu widżet pokazywałby mniej niż aplikacja.
         */
        private fun godzinyDnia(e: DayEntry): Int =
            e.workedHours + (if (e.shift == Shift.URLOP) H_ZMIANY else 0)

        private suspend fun grafik(
            ctx: Context,
            cykl: pl.grafik.pracy.domain.CycleConfig,
            od: LocalDate,
            doKiedy: LocalDate
        ): Map<LocalDate, DayEntry> {
            val zapisane = AppDb.get(ctx).dayDao().rangeOnce(od.toString(), doKiedy.toString())
                .associate { LocalDate.parse(it.date) to it.toEntry() }
            val out = LinkedHashMap<LocalDate, DayEntry>()
            CycleGenerator.range(cykl, od, doKiedy).forEach { (d, sh) ->
                out[d] = zapisane[d] ?: DayEntry(date = d, shift = sh)
            }
            zapisane.forEach { (d, e) -> out[d] = e }
            return out
        }
    }
}
