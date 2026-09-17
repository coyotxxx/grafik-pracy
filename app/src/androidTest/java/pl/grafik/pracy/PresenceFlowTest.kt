package pl.grafik.pracy

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pl.grafik.pracy.data.AppDb
import pl.grafik.pracy.data.DayRow
import pl.grafik.pracy.data.SettingsStore
import pl.grafik.pracy.domain.*
import pl.grafik.pracy.location.PresenceRepo
import pl.grafik.pracy.location.PresenceState
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Pełna ścieżka na prawdziwym urządzeniu: wejście do strefy → wyjście →
 * propozycja w tabeli `presence` → zatwierdzenie → wpis w grafiku.
 */
@RunWith(AndroidJUnit4::class)
class PresenceFlowTest {

    @get:Rule
    val perms: androidx.test.rule.GrantPermissionRule =
        androidx.test.rule.GrantPermissionRule.grant(
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

    private val ctx: Context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val db get() = AppDb.get(ctx)
    private val day = LocalDate.of(2026, 10, 14)      // środa, dzień zwykły

    @Before
    fun setup() = runBlocking {
        PresenceState.clear(ctx)
        db.presenceDao().observeRecent(500).first().forEach { db.presenceDao().delete(it.id) }
        db.dayDao().clearRange("2026-10-01", "2026-10-31")
        SettingsStore(ctx).saveWorkPlace(
            WorkPlace(enabled = true, lat = 52.1, lon = 21.0, radiusM = 200, minStayMin = 30)
        )
        // Test nie może zależeć od cyklu, który ktoś zostawił w ustawieniach urządzenia.
        SettingsStore(ctx).saveConfig(CycleConfig())
    }

    private suspend fun planShift(d: LocalDate, s: Shift) {
        db.dayDao().upsert(DayRow.from(DayEntry(date = d, shift = s)))
    }

    @Test
    fun przyklad_Macieja_zmiana_I_do_15_15_daje_jedna_godzine() = runBlocking {
        planShift(day, Shift.I)

        PresenceRepo.onEnter(ctx, day.atTime(6, 0))
        val id = PresenceRepo.onExit(ctx, day.atTime(15, 15))
        assertNotNull("pobyt powinien zostać zamknięty", id)

        val row = db.presenceDao().byId(id!!)!!
        assertEquals(day.toString(), row.date)
        assertEquals("pending", row.status)
        assertEquals(1, row.otHours)
        assertEquals(50, row.otRate)

        // dopóki nie zatwierdzę — grafik nietknięty
        assertEquals(0, db.dayDao().get(day.toString())!!.otHours)

        PresenceRepo.accept(ctx, id)
        val saved = db.dayDao().get(day.toString())!!
        assertEquals(1, saved.otHours)
        assertEquals(50, saved.otRate)
        assertEquals("I", saved.shift)
        assertEquals("accepted", db.presenceDao().byId(id)!!.status)
    }

    @Test
    fun po_wykryciu_pojawia_sie_powiadomienie_z_przyciskami() = runBlocking {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.cancelAll()
        planShift(day, Shift.I)
        PresenceRepo.onEnter(ctx, day.atTime(6, 0))
        val id = PresenceRepo.onExit(ctx, day.atTime(15, 15))!!

        // System publikuje powiadomienie asynchronicznie — dajemy mu chwilę.
        var n = nm.activeNotifications.firstOrNull { it.id == id.toInt() }
        var czekano = 0
        while (n == null && czekano < 5000) {
            Thread.sleep(250); czekano += 250
            n = nm.activeNotifications.firstOrNull { it.id == id.toInt() }
        }
        assertTrue("powiadomienia muszą być włączone dla testu",
            androidx.core.app.NotificationManagerCompat.from(ctx).areNotificationsEnabled())
        assertNotNull("propozycja musi trafić na powiadomienie", n)
        val title = n!!.notification.extras.getString("android.title") ?: ""
        val text = n.notification.extras.getString("android.text") ?: ""
        assertTrue("tytuł ma mówić ile nadgodzin: $title", title.contains("1 godz. nadgodzin"))
        assertTrue("nazwa miesiąca po polsku, nie po angielsku: $title", title.contains("października"))
        assertTrue("treść ma pokazać godziny obecności: $text", text.contains("6:00") && text.contains("15:15"))
        assertTrue("treść ma pokazać co policzono: $text", text.contains("15:00") && text.contains("9 h"))
        assertEquals("Zapisz i Odrzuć", 2, n.notification.actions.size)
        assertEquals("Zapisz", n.notification.actions[0].title)
        assertEquals("Odrzuć", n.notification.actions[1].title)
    }

    @Test
    fun przyklad_Macieja_przyjazd_4_30_wyjscie_14_15() = runBlocking {
        planShift(day, Shift.I)
        PresenceRepo.onEnter(ctx, day.atTime(4, 30))
        val id = PresenceRepo.onExit(ctx, day.atTime(14, 15))!!
        val row = db.presenceDao().byId(id)!!
        assertEquals(1, row.otHours)
        assertEquals(day.atTime(5, 0).toString(), row.countedFrom)
        assertEquals(day.atTime(14, 0).toString(), row.countedTo)
    }

    @Test
    fun praca_w_dniu_wolnym_to_100_procent_i_zero_normy() = runBlocking {
        planShift(day, Shift.W5)
        PresenceRepo.onEnter(ctx, day.atTime(7, 40))
        val id = PresenceRepo.onExit(ctx, day.atTime(12, 50))!!
        val row = db.presenceDao().byId(id)!!
        assertEquals(4, row.otHours)
        assertEquals(100, row.otRate)

        PresenceRepo.accept(ctx, id)
        val saved = db.dayDao().get(day.toString())!!.toEntry()
        assertEquals("w5", saved.shift?.code)          // dzień zostaje wolny…
        assertEquals(4, saved.workedHours)             // …a przepracowane to same nadgodziny
    }

    /**
     * Pusty grafik (cykl wyłączony, dzień niepomalowany): apka nie może uznać dnia
     * za wolny i wrzucić całej obecności w nadgodziny 100 %.
     */
    @Test
    fun praca_przy_pustym_grafiku_liczy_osiem_godzin_normy() = runBlocking {
        db.dayDao().clearRange("2026-10-01", "2026-10-31")      // żadnego wpisu na ten dzień
        PresenceRepo.onEnter(ctx, day.atTime(6, 0))
        val id = PresenceRepo.onExit(ctx, day.atTime(15, 15))!!
        val row = db.presenceDao().byId(id)!!
        assertEquals("tylko nadwyżka ponad 8 h", 1, row.otHours)
        assertEquals("dzień roboczy, nie wolny", 50, row.otRate)

        PresenceRepo.accept(ctx, id)
        val saved = db.dayDao().get(day.toString())!!.toEntry()
        assertEquals("zmiana odczytana z godziny wejścia", Shift.I, saved.shift)
        assertEquals(9, saved.workedHours)
    }

    @Test
    fun przejazd_obok_zakladu_nie_tworzy_wpisu() = runBlocking {
        planShift(day, Shift.I)
        PresenceRepo.onEnter(ctx, day.atTime(6, 0))
        val id = PresenceRepo.onExit(ctx, day.atTime(6, 12))
        assertNull("12 minut to za mało na dzień pracy", id)
        assertTrue(db.presenceDao().forDate(day.toString()).isEmpty())
    }

    @Test
    fun odrzucenie_propozycji_nie_rusza_grafiku() = runBlocking {
        planShift(day, Shift.I)
        PresenceRepo.onEnter(ctx, day.atTime(6, 0))
        val id = PresenceRepo.onExit(ctx, day.atTime(16, 30))!!
        PresenceRepo.reject(ctx, id)
        assertEquals("rejected", db.presenceDao().byId(id)!!.status)
        assertEquals(0, db.dayDao().get(day.toString())!!.otHours)
    }

    /** Pozycja w środku strefy / 5 km dalej — do sterowania watchdogiem w teście. */
    private fun loc(lat: Double, lon: Double) = android.location.Location("test").apply {
        latitude = lat; longitude = lon
    }
    private val wPracy get() = loc(52.1, 21.0)
    private val wDomu get() = loc(52.15, 21.0)          // ~5,5 km od strefy

    @Test
    fun watchdog_domyka_pobyt_gdy_pozycja_potwierdza_ze_wyszedlem() = runBlocking {
        planShift(day, Shift.I)
        PresenceRepo.onEnter(ctx, day.atTime(6, 0))
        // EXIT nigdy nie przyszedł, ale o 15:15 telefon jest już daleko od zakładu
        PresenceRepo.watchdog(ctx, day.atTime(15, 15)) { wDomu }

        val rows = db.presenceDao().forDate(day.toString())
        assertEquals(1, rows.size)
        assertEquals("watchdog", rows[0].source)
        assertEquals(day.atTime(15, 15).toString(), rows[0].exitAt)
        assertEquals(1, rows[0].otHours)                               // 15:15 -> 15:00
    }

    /**
     * REGRESJA: przy ośmiogodzinnej zmianie Android nie przysyła żadnego zdarzenia.
     * Watchdog NIE MOŻE uznać ciszy za wyjście — inaczej ucinałby zmianę po 35 minutach.
     */
    @Test
    fun watchdog_nie_ucina_zmiany_gdy_nadal_jestem_w_pracy() = runBlocking {
        planShift(day, Shift.I)
        PresenceRepo.onEnter(ctx, day.atTime(6, 0))
        // pięć kolejnych przebiegów przez całą zmianę, zero zdarzeń z geofence
        listOf(7, 9, 11, 13, 14).forEach { h ->
            PresenceRepo.watchdog(ctx, day.atTime(h, 0)) { wPracy }
        }
        assertTrue("pobyt nie może zostać zamknięty", db.presenceDao().forDate(day.toString()).isEmpty())
        assertNotNull(PresenceState.openEnter(ctx))
        assertEquals(day.atTime(14, 0), PresenceState.lastSeen(ctx))
    }

    /**
     * REGRESJA #2: Android dławi lokalizację aplikacjom w tle, więc przez CAŁĄ zmianę
     * sonda może zwracać null. To znaczy „nie wiem", a nie „wyszedłem" — watchdog
     * nie ma prawa zamknąć pobytu i zapisać godziny zamiast ośmiu.
     */
    @Test
    fun brak_pozycji_przez_cala_zmiane_nie_tworzy_falszywego_wpisu() = runBlocking {
        planShift(day, Shift.I)
        PresenceRepo.onEnter(ctx, day.atTime(6, 0))
        PresenceRepo.onStillHere(ctx, day.atTime(6, 30))          // ostatnie zdarzenie: DWELL

        // watchdog budzi się co 15 min przez całą zmianę i ani razu nie dostaje pozycji
        listOf(7 to 0, 9 to 0, 11 to 0, 13 to 0, 14 to 30).forEach { (h, m) ->
            PresenceRepo.watchdog(ctx, day.atTime(h, m)) { null }
        }
        assertTrue("żaden wpis nie może powstać bez potwierdzenia",
            db.presenceDao().forDate(day.toString()).isEmpty())
        assertNotNull("pobyt musi zostać otwarty do czasu EXIT", PresenceState.openEnter(ctx))
    }

    /** Pobyt absurdalnie długi domykamy awaryjnie, żeby nie wisiał w nieskończoność. */
    @Test
    fun pobyt_otwarty_ponad_16_godzin_jest_domykany_awaryjnie() = runBlocking {
        planShift(day, Shift.I)
        PresenceRepo.onEnter(ctx, day.atTime(6, 0))
        PresenceRepo.onStillHere(ctx, day.atTime(15, 30))

        PresenceRepo.watchdog(ctx, day.atTime(21, 0)) { null }     // 15 h — jeszcze nie
        assertTrue(db.presenceDao().forDate(day.toString()).isEmpty())

        PresenceRepo.watchdog(ctx, day.plusDays(1).atTime(0, 30)) { null }   // 18,5 h
        val rows = db.presenceDao().forDate(day.toString())
        assertEquals(1, rows.size)
        assertEquals("watchdog-awaryjnie", rows[0].source)
        assertEquals("domykamy OSTATNIM KONTAKTEM, nie chwilą obecną",
            day.atTime(15, 30).toString(), rows[0].exitAt)
        assertEquals(1, rows[0].otHours)
    }

    @Test
    fun drgania_GPS_tuz_za_promieniem_nie_zamykaja_pobytu() = runBlocking {
        planShift(day, Shift.I)
        PresenceRepo.onEnter(ctx, day.atTime(6, 0))
        // 52.1005 to ~55 m od środka strefy o promieniu 200 m — wciąż w środku
        PresenceRepo.watchdog(ctx, day.atTime(10, 0)) { loc(52.1005, 21.0) }
        assertTrue(db.presenceDao().forDate(day.toString()).isEmpty())
    }

    @Test
    fun nocka_przypisuje_sie_do_dnia_rozpoczecia() = runBlocking {
        planShift(day, Shift.III)
        PresenceRepo.onEnter(ctx, day.atTime(21, 40))
        val id = PresenceRepo.onExit(ctx, day.plusDays(1).atTime(7, 20))!!
        val row = db.presenceDao().byId(id)!!
        assertEquals(day.toString(), row.date)      // nie następny dzień
        assertEquals(1, row.otHours)                // 21:40->22:00, 7:20->7:00 = 9 h
    }

    @Test
    fun zamiana_zmian_wpisuje_do_grafiku_te_zmiane_na_ktora_przyszedlem() = runBlocking {
        planShift(day, Shift.I)                                   // grafik mówi: zmiana I 6–14

        PresenceRepo.onEnter(ctx, day.atTime(21, 30))             // a ja przyjechałem na nockę
        val id = PresenceRepo.onExit(ctx, day.plusDays(1).atTime(6, 15))
        assertNotNull("brak propozycji", id)

        val row = db.presenceDao().byId(id!!)!!
        assertEquals("III", row.shiftCode)
        assertEquals(0, row.otHours)                              // zamiana zmian to nie nadgodziny

        PresenceRepo.accept(ctx, id)
        val wpis = db.dayDao().get(day.toString())!!.toEntry()
        assertEquals(Shift.III, wpis.shift)                       // grafik poprawiony
        assertEquals(0, wpis.otHours)
    }

    @Test
    fun w_dniu_wolnym_zostaje_oznaczenie_dnia_a_godziny_ida_w_nadgodziny() = runBlocking {
        planShift(day, Shift.W5)

        PresenceRepo.onEnter(ctx, day.atTime(21, 30))
        val id = PresenceRepo.onExit(ctx, day.plusDays(1).atTime(6, 15))!!

        PresenceRepo.accept(ctx, id)
        val wpis = db.dayDao().get(day.toString())!!.toEntry()
        assertEquals(Shift.W5, wpis.shift)                        // dzień wolny zostaje wolny
        assertEquals(8, wpis.otHours)
        assertEquals(OtRate.P100, wpis.otRate)
    }

    // ——— firmowe Wi-Fi jako samodzielne źródło wykrycia ———

    @Test
    fun firmowe_wifi_samo_otwiera_pobyt() = runBlocking {
        SettingsStore(ctx).saveWorkPlace(
            WorkPlace(enabled = true, lat = 52.1, lon = 21.0, radiusM = 200, minStayMin = 30, ssid = "FirmaWiFi")
        )
        PresenceState.clear(ctx)
        assertNull(PresenceState.openEnter(ctx))

        // Brak pozycji z GPS — liczy się wyłącznie sieć.
        PresenceRepo.watchdog(ctx, now = day.atTime(21, 40), probe = { null }, wifi = { true })

        assertEquals(day.atTime(21, 40), PresenceState.openEnter(ctx))
    }

    @Test
    fun obca_siec_nie_otwiera_pobytu() = runBlocking {
        SettingsStore(ctx).saveWorkPlace(
            WorkPlace(enabled = true, lat = 52.1, lon = 21.0, radiusM = 200, minStayMin = 30, ssid = "FirmaWiFi")
        )
        PresenceState.clear(ctx)

        PresenceRepo.watchdog(ctx, now = day.atTime(21, 40), probe = { null }, wifi = { false })

        assertNull(PresenceState.openEnter(ctx))
    }

    @Test
    fun bez_skonfigurowanej_sieci_wifi_nic_nie_otwiera() = runBlocking {
        SettingsStore(ctx).saveWorkPlace(
            WorkPlace(enabled = true, lat = 52.1, lon = 21.0, radiusM = 200, minStayMin = 30, ssid = "")
        )
        PresenceState.clear(ctx)

        PresenceRepo.watchdog(ctx, now = day.atTime(21, 40), probe = { null }, wifi = { true })

        assertNull(PresenceState.openEnter(ctx))
    }

    // ——— automatyczny zapis ———

    @Test
    fun z_automatycznym_zapisem_dzien_wchodzi_do_grafiku_bez_pytania() = runBlocking {
        SettingsStore(ctx).saveWorkPlace(
            WorkPlace(enabled = true, lat = 52.1, lon = 21.0, radiusM = 200, minStayMin = 30, autoSave = true)
        )
        planShift(day, Shift.I)

        PresenceRepo.onEnter(ctx, day.atTime(6, 0))
        val id = PresenceRepo.onExit(ctx, day.atTime(16, 15))!!

        assertEquals("accepted", db.presenceDao().byId(id)!!.status)
        assertEquals(2, db.dayDao().get(day.toString())!!.toEntry().otHours)
    }

    @Test
    fun cofniecie_zdejmuje_z_grafiku_dopisane_nadgodziny() = runBlocking {
        SettingsStore(ctx).saveWorkPlace(
            WorkPlace(enabled = true, lat = 52.1, lon = 21.0, radiusM = 200, minStayMin = 30, autoSave = true)
        )
        planShift(day, Shift.I)

        PresenceRepo.onEnter(ctx, day.atTime(6, 0))
        val id = PresenceRepo.onExit(ctx, day.atTime(16, 15))!!
        assertEquals(2, db.dayDao().get(day.toString())!!.toEntry().otHours)

        PresenceRepo.reject(ctx, id)

        assertEquals("rejected", db.presenceDao().byId(id)!!.status)
        assertEquals(0, db.dayDao().get(day.toString())!!.toEntry().otHours)
    }

    @Test
    fun bez_automatycznego_zapisu_propozycja_czeka() = runBlocking {
        SettingsStore(ctx).saveWorkPlace(
            WorkPlace(enabled = true, lat = 52.1, lon = 21.0, radiusM = 200, minStayMin = 30, autoSave = false)
        )
        planShift(day, Shift.I)

        PresenceRepo.onEnter(ctx, day.atTime(6, 0))
        val id = PresenceRepo.onExit(ctx, day.atTime(16, 15))!!

        assertEquals("pending", db.presenceDao().byId(id)!!.status)
        assertEquals(0, db.dayDao().get(day.toString())!!.toEntry().otHours)
    }

    // ——— „Byłem w pracy" z karty dnia ———

    @Test
    fun reczne_oznaczenie_bierze_godziny_ze_zmiany() = runBlocking {
        SettingsStore(ctx).saveConfig(CycleConfig())
        planShift(day, Shift.III)

        PresenceRepo.markManual(ctx, day)

        val wpisy = db.presenceDao().forDate(day.toString())
        assertEquals(1, wpisy.size)
        val r = wpisy.first()
        assertEquals("manual", r.source)
        assertEquals("accepted", r.status)
        assertEquals("III", r.shiftCode)
        // III to 22:00–6:00 dnia następnego
        assertEquals(day.atTime(22, 0).toString(), r.countedFrom)
        assertEquals(day.plusDays(1).atTime(6, 0).toString(), r.countedTo)
        assertEquals(0, r.otHours)
        // Grafik zostaje nietknięty — ręczne oznaczenie nie dopisuje nadgodzin.
        assertEquals(0, db.dayDao().get(day.toString())!!.toEntry().otHours)
    }

    @Test
    fun ponowne_oznaczenie_nie_dubluje_wpisu() = runBlocking {
        planShift(day, Shift.I)
        PresenceRepo.markManual(ctx, day)
        PresenceRepo.markManual(ctx, day)
        assertEquals(1, db.presenceDao().forDate(day.toString()).size)
    }

    @Test
    fun wylaczenie_kasuje_tylko_reczny_wpis() = runBlocking {
        planShift(day, Shift.I)
        PresenceRepo.markManual(ctx, day)
        assertEquals(1, db.presenceDao().forDate(day.toString()).size)

        PresenceRepo.clearManual(ctx, day)
        assertEquals(0, db.presenceDao().forDate(day.toString()).size)
    }

    @Test
    fun dzien_wolny_nie_da_sie_oznaczyc_bez_zmiany() = runBlocking {
        planShift(day, Shift.W5)
        PresenceRepo.markManual(ctx, day)
        assertEquals(0, db.presenceDao().forDate(day.toString()).size)
    }
}
