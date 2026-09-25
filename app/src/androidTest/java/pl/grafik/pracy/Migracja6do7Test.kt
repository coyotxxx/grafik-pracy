package pl.grafik.pracy

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pl.grafik.pracy.data.AppDb

/**
 * Migracja 6 → 7 na prawdziwym SQLite: uroczystości dopisane do wydarzeń.
 *
 * Zasada „zero utraty danych": wydarzenia zapisane przed aktualizacją mają zostać
 * dokładnie takie, jakie były, a nowe kolumny dostać wartości domyślne — zwykłe
 * wydarzenie nie może po aktualizacji udawać uroczystości.
 */
@RunWith(AndroidJUnit4::class)
class Migracja6do7Test {

    private val ctx: Context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val nazwa = "migracja_6_7_test.db"

    /** Tabela `events` dokładnie taka, jaka była do wersji 6. */
    private val eventsV6 = """
        CREATE TABLE IF NOT EXISTS `events` (
            `id` INTEGER NOT NULL,
            `date` TEXT NOT NULL,
            `time` TEXT NOT NULL,
            `text` TEXT NOT NULL,
            `remind` INTEGER NOT NULL,
            PRIMARY KEY(`id`)
        )
    """.trimIndent()

    @Before fun czysto() {
        ctx.deleteDatabase(nazwa)
    }

    private fun otworzV6(): SupportSQLiteDatabase {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(ctx)
                .name(nazwa)
                .callback(object : SupportSQLiteOpenHelper.Callback(6) {
                    override fun onCreate(db: SupportSQLiteDatabase) = db.execSQL(eventsV6)
                    override fun onUpgrade(db: SupportSQLiteDatabase, old: Int, new: Int) = Unit
                })
                .build()
        )
        return helper.writableDatabase
    }

    @Test fun wydarzenia_zapisane_wczesniej_przezywaja_migracje() {
        val db = otworzV6()
        db.execSQL(
            """
            INSERT INTO events (id, date, time, text, remind)
            VALUES (1, '2026-09-20', '10:00', 'fryzjer', 1)
            """.trimIndent()
        )

        AppDb.MIGRATION_6_7.migrate(db)

        db.query("SELECT date, time, text, remind, rodzaj, osoba, coroczne FROM events").use { c ->
            assertTrue("wpis musi przeżyć migrację", c.moveToFirst())
            assertEquals("2026-09-20", c.getString(0))
            assertEquals("10:00", c.getString(1))
            assertEquals("fryzjer", c.getString(2))
            assertEquals(1, c.getInt(3))
            assertEquals("stare wydarzenie nie jest uroczystością", "", c.getString(4))
            assertEquals("", c.getString(5))
            assertEquals("i nie powtarza się co roku", 0, c.getInt(6))
        }
        db.close()
    }

    @Test fun po_migracji_da_sie_zapisac_uroczystosc() {
        val db = otworzV6()
        AppDb.MIGRATION_6_7.migrate(db)
        db.execSQL(
            """
            INSERT INTO events (id, date, time, text, remind, rodzaj, osoba, coroczne)
            VALUES (2, '2026-09-24', '', 'imieniny — Anna Kowalska', 1, 'imieniny', 'Anna Kowalska', 1)
            """.trimIndent()
        )

        db.query("SELECT rodzaj, osoba, coroczne, time FROM events WHERE id = 2").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("imieniny", c.getString(0))
            assertEquals("Anna Kowalska", c.getString(1))
            assertEquals(1, c.getInt(2))
            assertEquals("uroczystość nie ma pory dnia", "", c.getString(3))
        }
        db.close()
    }

    @Test fun zapytanie_o_coroczne_omija_zwykle_wydarzenia() {
        val db = otworzV6()
        db.execSQL(
            "INSERT INTO events (id, date, time, text, remind) " +
                "VALUES (1, '2026-09-20', '10:00', 'fryzjer', 1)"
        )
        AppDb.MIGRATION_6_7.migrate(db)
        db.execSQL(
            "INSERT INTO events (id, date, time, text, remind, rodzaj, osoba, coroczne) " +
                "VALUES (2, '2026-09-24', '', 'imieniny — Anna', 1, 'imieniny', 'Anna', 1)"
        )

        db.query("SELECT osoba FROM events WHERE coroczne = 1 ORDER BY substr(date, 6), text").use { c ->
            assertEquals("tylko uroczystość jest coroczna", 1, c.count)
            assertTrue(c.moveToFirst())
            assertEquals("Anna", c.getString(0))
        }
        db.close()
    }
}
