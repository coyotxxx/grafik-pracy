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
 * Migracja 5 → 6 na prawdziwym SQLite: zdjęcie dołączane do notatki dnia.
 *
 * Zasada „zero utraty danych": grafik zapisany przed aktualizacją ma przeżyć
 * dołożenie kolumny w komplecie — ze zmianą, nadgodzinami i notatką.
 */
@RunWith(AndroidJUnit4::class)
class Migracja5do6Test {

    private val ctx: Context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val nazwa = "migracja_5_6_test.db"

    /** Tabela `days` dokładnie taka, jaka była do wersji 5 — bez `notePhoto`. */
    private val daysV5 = """
        CREATE TABLE IF NOT EXISTS `days` (
            `date` TEXT NOT NULL,
            `shift` TEXT,
            `otHours` INTEGER NOT NULL,
            `otRate` INTEGER NOT NULL,
            `deviation` INTEGER NOT NULL,
            `note` TEXT NOT NULL,
            `dwnFor` TEXT,
            PRIMARY KEY(`date`)
        )
    """.trimIndent()

    @Before fun czysto() {
        ctx.deleteDatabase(nazwa)
    }

    private fun otworzV5(): SupportSQLiteDatabase {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(ctx)
                .name(nazwa)
                .callback(object : SupportSQLiteOpenHelper.Callback(5) {
                    override fun onCreate(db: SupportSQLiteDatabase) = db.execSQL(daysV5)
                    override fun onUpgrade(db: SupportSQLiteDatabase, old: Int, new: Int) = Unit
                })
                .build()
        )
        return helper.writableDatabase
    }

    @Test fun grafik_zapisany_wczesniej_przezywa_migracje() {
        val db = otworzV5()
        db.execSQL(
            """
            INSERT INTO days (date, shift, otHours, otRate, deviation, note, dwnFor)
            VALUES ('2026-09-24', 'II', 3, 50, 0, 'zamiana z Krzyśkiem', NULL)
            """.trimIndent()
        )

        AppDb.MIGRATION_5_6.migrate(db)

        db.query("SELECT date, shift, otHours, otRate, note, notePhoto FROM days").use { c ->
            assertTrue("wpis musi przeżyć migrację", c.moveToFirst())
            assertEquals("2026-09-24", c.getString(0))
            assertEquals("II", c.getString(1))
            assertEquals(3, c.getInt(2))
            assertEquals(50, c.getInt(3))
            assertEquals("zamiana z Krzyśkiem", c.getString(4))
            assertTrue("stary wpis nie ma zdjęcia", c.isNull(5))
        }
        db.close()
    }

    @Test fun po_migracji_da_sie_zapisac_zdjecie() {
        val db = otworzV5()
        AppDb.MIGRATION_5_6.migrate(db)
        db.execSQL(
            """
            INSERT INTO days (date, shift, otHours, otRate, deviation, note, notePhoto, dwnFor)
            VALUES ('2026-09-25', 'I', 0, 100, 0, 'zabrać kask', '2026-09-25.jpg', NULL)
            """.trimIndent()
        )

        db.query("SELECT note, notePhoto FROM days WHERE date = '2026-09-25'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("zabrać kask", c.getString(0))
            assertEquals("2026-09-25.jpg", c.getString(1))
        }
        db.close()
    }

    @Test fun notatka_bez_zdjecia_dalej_dziala() {
        val db = otworzV5()
        AppDb.MIGRATION_5_6.migrate(db)
        db.execSQL(
            """
            INSERT INTO days (date, shift, otHours, otRate, deviation, note, dwnFor)
            VALUES ('2026-09-26', 'III', 0, 100, 0, 'sama notatka', NULL)
            """.trimIndent()
        )

        db.query("SELECT note, notePhoto FROM days WHERE date = '2026-09-26'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("sama notatka", c.getString(0))
            assertTrue("brak zdjęcia to null, nie pusty tekst", c.isNull(1))
        }
        db.close()
    }
}
