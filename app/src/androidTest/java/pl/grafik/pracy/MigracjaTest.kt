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
 * Migracja 3 → 4 na prawdziwym SQLite. Zasada „zero utraty danych": stary wpis
 * musi przeżyć dołożenie kolumny i dostać pusty kod zmiany, a nie zniknąć.
 */
@RunWith(AndroidJUnit4::class)
class MigracjaTest {

    private val ctx: Context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val nazwa = "migracja_3_4_test.db"

    /** Dokładnie taka tabela `presence` powstawała w wersji 3 (z MIGRATION_1_2). */
    private val presenceV3 = """
        CREATE TABLE IF NOT EXISTS `presence` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `date` TEXT NOT NULL,
            `enterAt` TEXT NOT NULL,
            `exitAt` TEXT NOT NULL,
            `source` TEXT NOT NULL,
            `status` TEXT NOT NULL,
            `otHours` INTEGER NOT NULL,
            `otRate` INTEGER NOT NULL,
            `countedFrom` TEXT NOT NULL,
            `countedTo` TEXT NOT NULL,
            `createdAt` TEXT NOT NULL
        )
    """.trimIndent()

    @Before fun czysto() { ctx.deleteDatabase(nazwa) }

    private fun otworzV3(): SupportSQLiteDatabase {
        val cfg = SupportSQLiteOpenHelper.Configuration.builder(ctx)
            .name(nazwa)
            .callback(object : SupportSQLiteOpenHelper.Callback(3) {
                override fun onCreate(db: SupportSQLiteDatabase) { db.execSQL(presenceV3) }
                override fun onUpgrade(db: SupportSQLiteDatabase, old: Int, new: Int) = Unit
            })
            .build()
        return FrameworkSQLiteOpenHelperFactory().create(cfg).writableDatabase
    }

    @Test fun stary_wpis_przezywa_dolozenie_kolumny() {
        val db = otworzV3()
        db.execSQL(
            """
            INSERT INTO presence
              (date, enterAt, exitAt, source, status, otHours, otRate, countedFrom, countedTo, createdAt)
            VALUES
              ('2026-09-19', '2026-09-19T21:30', '2026-09-20T06:15', 'geo', 'accepted', 8, 100,
               '2026-09-19T22:00', '2026-09-20T06:00', '2026-09-19T06:20')
            """.trimIndent()
        )

        AppDb.MIGRATION_3_4.migrate(db)

        db.query("SELECT date, otHours, otRate, status, shiftCode FROM presence").use { c ->
            assertTrue("wpis zniknął po migracji", c.moveToFirst())
            assertEquals("2026-09-19", c.getString(0))
            assertEquals(8, c.getInt(1))
            assertEquals(100, c.getInt(2))
            assertEquals("accepted", c.getString(3))
            assertEquals("", c.getString(4))          // stare wpisy nie znają zmiany
            assertFalse("pojawił się drugi wiersz", c.moveToNext())
        }
        db.close()
    }

    @Test fun po_migracji_kolumna_przyjmuje_kod_zmiany() {
        val db = otworzV3()
        AppDb.MIGRATION_3_4.migrate(db)
        db.execSQL(
            """
            INSERT INTO presence
              (date, enterAt, exitAt, source, status, otHours, otRate, countedFrom, countedTo, createdAt, shiftCode)
            VALUES
              ('2026-09-21', '2026-09-21T21:30', '2026-09-22T06:15', 'geo', 'pending', 0, 50,
               '2026-09-21T22:00', '2026-09-22T06:00', '2026-09-21T06:20', 'III')
            """.trimIndent()
        )
        db.query("SELECT shiftCode FROM presence WHERE date = '2026-09-21'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("III", c.getString(0))
        }
        db.close()
    }
}
