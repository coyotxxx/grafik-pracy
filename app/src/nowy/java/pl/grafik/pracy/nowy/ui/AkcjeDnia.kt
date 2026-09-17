package pl.grafik.pracy.nowy.ui

import android.content.Context
import pl.grafik.pracy.data.AppDb
import pl.grafik.pracy.data.DayRow
import pl.grafik.pracy.domain.DayEntry
import pl.grafik.pracy.domain.OtRate
import java.time.LocalDate

/**
 * Zmiany dnia wychodzące z karty dnia.
 *
 * Nowy wygląd korzysta z tego samego modelu widoku co klasyczna aplikacja, ale
 * `Vm` nie ma funkcji ustawiającej dowolną liczbę nadgodzin na konkretnym dniu —
 * jego stepper chodzi co 2 h w zakresie 2–12. Karta dnia z makiety ma krok 1 od zera,
 * więc zapisujemy wprost do bazy.
 *
 * ⚠️ Kompromis: te zmiany omijają historię cofania w `Vm`. Docelowo lepiej dodać
 * `Vm.setOvertime(date, hours, rate)` do wspólnego kodu — wymaga to jednak ruszenia
 * `src/main`, na co nie ma zgody przy pracy nad wyglądem.
 */
object AkcjeDnia {

    suspend fun nadgodziny(ctx: Context, d: LocalDate, ile: Int, stawka: OtRate) {
        val dao = AppDb.get(ctx).dayDao()
        val teraz = dao.get(d.toString())?.toEntry() ?: DayEntry(date = d)
        dao.upsert(DayRow.from(teraz.copy(otHours = ile.coerceIn(0, 12), otRate = stawka)))
    }
}
