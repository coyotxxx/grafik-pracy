package pl.grafik.pracy

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pl.grafik.pracy.data.AppDb
import pl.grafik.pracy.data.DayRow
import pl.grafik.pracy.data.Kopia
import pl.grafik.pracy.data.SettingsStore
import pl.grafik.pracy.domain.CycleConfig
import pl.grafik.pracy.domain.CyclePattern
import pl.grafik.pracy.domain.PowiadomieniaCfg
import pl.grafik.pracy.domain.SettlementCfg
import pl.grafik.pracy.domain.VacationCfg
import pl.grafik.pracy.events.PowiadomieniaWarunkowe
import java.time.LocalDate

/**
 * Powiadomienia warunkowe na prawdziwym urządzeniu: czy z grafiku i ustawień
 * powstaje powiadomienie, które naprawdę ląduje na pasku.
 */
@RunWith(AndroidJUnit4::class)
class WarunkoweFlowTest {

    @get:Rule
    val perms: GrantPermissionRule =
        if (Build.VERSION.SDK_INT >= 33)
            GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
        else GrantPermissionRule.grant()

    private val ctx: Context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val nm: NotificationManager
        get() = ctx.getSystemService(NotificationManager::class.java)

    @Before
    fun czysto() = runBlocking {
        Kopia.wyczyscWszystko(ctx)
        nm.cancelAll()
    }

    @After
    fun posprzataj() = runBlocking {
        Kopia.wyczyscWszystko(ctx)
        nm.cancelAll()
    }

    private fun powiadomienia(): List<String> =
        nm.activeNotifications.mapNotNull {
            it.notification.extras.getCharSequence("android.title")?.toString()
        }

    @Test
    fun limit_nadgodzin_dociera_gdy_przekroczony_prog() = runBlocking {
        assumeTrue("system blokuje powiadomienia", nm.areNotificationsEnabled())

        SettingsStore(ctx).savePowiadomienia(
            PowiadomieniaCfg(
                zmianaBrygady = false, limitNadgodzin = true, zaleglyUrlop = false
            )
        )
        // Limit zakładowy Macieja na III kwartał to 94 h — próg wypada na 76 h.
        // Ustawowy byłby wyższy (13 pełnych tygodni × 8 h = 104 h), więc przy okazji
        // sprawdzamy, że liczy się ten wpisany przez zakład.
        SettingsStore(ctx).saveSettlement(
            SettlementCfg(months = 3, otLimitPeriods = mapOf("2026-07" to 94))
        )

        val db = AppDb.get(ctx)
        db.dayDao().upsertAll(
            (1..10).map { i ->
                DayRow(date = "2026-07-%02d".format(i), shift = "I", otHours = 8, otRate = 100)
            }
        )

        PowiadomieniaWarunkowe.sprawdz(ctx, LocalDate.of(2026, 9, 15))

        val tytuly = powiadomienia()
        assertTrue("brak powiadomienia o limicie: $tytuly",
            tytuly.any { it.contains("Limit nadgodzin") })
    }

    @Test
    fun ponizej_progu_nic_nie_przychodzi() = runBlocking {
        assumeTrue("system blokuje powiadomienia", nm.areNotificationsEnabled())

        SettingsStore(ctx).savePowiadomienia(
            PowiadomieniaCfg(
                zmianaBrygady = false, limitNadgodzin = true, zaleglyUrlop = false
            )
        )
        SettingsStore(ctx).saveSettlement(
            SettlementCfg(months = 3, otLimitPeriods = mapOf("2026-07" to 94))
        )
        AppDb.get(ctx).dayDao().upsert(
            DayRow(date = "2026-07-01", shift = "I", otHours = 8, otRate = 100)
        )

        PowiadomieniaWarunkowe.sprawdz(ctx, LocalDate.of(2026, 9, 15))

        assertTrue("8 h to nie powód do alarmu: ${powiadomienia()}", powiadomienia().isEmpty())
    }

    @Test
    fun zalegly_urlop_odzywa_sie_w_sierpniu() = runBlocking {
        assumeTrue("system blokuje powiadomienia", nm.areNotificationsEnabled())

        SettingsStore(ctx).savePowiadomienia(
            PowiadomieniaCfg(
                zmianaBrygady = false, limitNadgodzin = false, zaleglyUrlop = true
            )
        )
        SettingsStore(ctx).saveVacation(VacationCfg(wymiar = 26, zalegly = 4))

        PowiadomieniaWarunkowe.sprawdz(ctx, LocalDate.of(2026, 8, 5))
        assertTrue("brak przypomnienia o urlopie: ${powiadomienia()}",
            powiadomienia().any { it.contains("Zaległy urlop") })

        nm.cancelAll()
        PowiadomieniaWarunkowe.sprawdz(ctx, LocalDate.of(2026, 6, 5))
        assertTrue("w czerwcu ma być cisza: ${powiadomienia()}", powiadomienia().isEmpty())
    }

    @Test
    fun wylaczone_warunki_nie_wysylaja_niczego() = runBlocking {
        assumeTrue("system blokuje powiadomienia", nm.areNotificationsEnabled())

        SettingsStore(ctx).savePowiadomienia(
            PowiadomieniaCfg(
                zmianaBrygady = false, limitNadgodzin = false, zaleglyUrlop = false
            )
        )
        SettingsStore(ctx).saveVacation(VacationCfg(wymiar = 26, zalegly = 4))
        SettingsStore(ctx).saveConfig(
            CycleConfig(
                pattern = CyclePattern.B4_16D,
                anchorDate = LocalDate.of(2024, 3, 1),
                anchorIndex = 0,
                brigade = "A"
            )
        )

        PowiadomieniaWarunkowe.sprawdz(ctx, LocalDate.of(2026, 8, 5))
        assertTrue("nic nie powinno przyjść: ${powiadomienia()}", powiadomienia().isEmpty())
    }
}
