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
 * Migracja 7 → 8: termin ważności notatki.
 *
 * Zasada „zero utraty danych": notatki zapisane wcześniej mają zostać nietknięte,
 * a nowa kolumna nie może im dorobić terminu, którego nigdy nie było.
 */
@RunWith(AndroidJUnit4::class)
class Migracja7do8Test {

    private val ctx: Context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val nazwa = "migracja_7_8_test.db"

    /** Tabela `days` dokładnie taka, jaka była do wersji 7. */
    private val daysV7 = """
        CREATE TABLE IF NOT EXISTS `days` (
            `date` TEXT NOT NULL,
            `shift` TEXT,
            `otHours` INTEGER NOT NULL,
            `otRate` INTEGER NOT NULL,
            `deviation` INTEGER NOT NULL,
            `note` TEXT NOT NULL,
            `notePhoto` TEXT,
            `dwnFor` TEXT,
            PRIMARY KEY(`date`)
        )
    """.trimIndent()

    @Before fun czysto() {
        ctx.deleteDatabase(nazwa)
    }

    private fun otworzV7(): SupportSQLiteDatabase {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(ctx)
                .name(nazwa)
                .callback(object : SupportSQLiteOpenHelper.Callback(7) {
                    override fun onCreate(db: SupportSQLiteDatabase) = db.execSQL(daysV7)
                    override fun onUpgrade(db: SupportSQLiteDatabase, old: Int, new: Int) = Unit
                })
                .build()
        )
        return helper.writableDatabase
    }

    @Test fun notatki_zapisane_wczesniej_przezywaja_migracje() {
        val db = otworzV7()
        db.execSQL(
            """
            INSERT INTO days (date, shift, otHours, otRate, deviation, note, notePhoto, dwnFor)
            VALUES ('2026-09-29', 'I', 0, 100, 0, 'Kara Upomnienia', '2026-09-29.jpg', NULL)
            """.trimIndent()
        )

        AppDb.MIGRATION_7_8.migrate(db)

        db.query("SELECT note, notePhoto, noteUntil FROM days").use { c ->
            assertTrue("wpis musi przeżyć migrację", c.moveToFirst())
            assertEquals("Kara Upomnienia", c.getString(0))
            assertEquals("2026-09-29.jpg", c.getString(1))
            assertTrue("stara notatka nie ma terminu", c.isNull(2))
        }
        db.close()
    }

    @Test fun po_migracji_da_sie_zapisac_termin() {
        val db = otworzV7()
        AppDb.MIGRATION_7_8.migrate(db)
        db.execSQL(
            """
            INSERT INTO days (date, shift, otHours, otRate, deviation, note, noteUntil, dwnFor)
            VALUES ('2026-09-29', 'I', 0, 100, 0, 'Kara Upomnienia', '2027-09-29', NULL)
            """.trimIndent()
        )

        db.query("SELECT note, noteUntil FROM days WHERE date = '2026-09-29'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("Kara Upomnienia", c.getString(0))
            assertEquals("2027-09-29", c.getString(1))
        }
        db.close()
    }

    @Test fun notatka_bez_terminu_dalej_dziala() {
        val db = otworzV7()
        AppDb.MIGRATION_7_8.migrate(db)
        db.execSQL(
            """
            INSERT INTO days (date, shift, otHours, otRate, deviation, note, dwnFor)
            VALUES ('2026-09-24', 'II', 0, 100, 0, 'zamiana z Krzyśkiem', NULL)
            """.trimIndent()
        )

        db.query("SELECT note, noteUntil FROM days WHERE date = '2026-09-24'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("zamiana z Krzyśkiem", c.getString(0))
            assertTrue("brak terminu to null, nie pusty tekst", c.isNull(1))
        }
        db.close()
    }
}
