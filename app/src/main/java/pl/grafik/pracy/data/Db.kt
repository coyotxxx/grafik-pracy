package pl.grafik.pracy.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import kotlinx.coroutines.flow.Flow
import pl.grafik.pracy.domain.*
import java.time.LocalDate

@Entity(tableName = "days")
data class DayRow(
    @PrimaryKey val date: String,          // ISO yyyy-MM-dd
    val shift: String?,                    // kod Shift albo null
    val otHours: Int = 0,
    val otRate: Int = 100,
    val deviation: Boolean = false,
    val note: String = "",
    /** Nazwa pliku ze zdjęciem dołączonym do notatki; null = bez zdjęcia. */
    val notePhoto: String? = null,
    val dwnFor: String? = null
) {
    fun toEntry(): DayEntry = DayEntry(
        date = LocalDate.parse(date),
        shift = shift?.let { c -> Shift.entries.firstOrNull { it.code == c } },
        otHours = otHours,
        otRate = if (otRate == 50) OtRate.P50 else OtRate.P100,
        deviation = deviation,
        note = note,
        notePhoto = notePhoto,
        dwnFor = dwnFor?.let { LocalDate.parse(it) }
    )

    companion object {
        fun from(e: DayEntry) = DayRow(
            date = e.date.toString(),
            shift = e.shift?.code,
            otHours = e.otHours,
            otRate = e.otRate.percent,
            deviation = e.deviation,
            note = e.note,
            notePhoto = e.notePhoto,
            dwnFor = e.dwnFor?.toString()
        )
    }
}

/** Wykryty pobyt w pracy. Nic nie trafia do grafiku, dopóki Maciej nie zatwierdzi. */
@Entity(tableName = "presence")
data class PresenceRow(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Dzień grafiku, do którego przypisano pobyt (ISO yyyy-MM-dd). */
    val date: String,
    val enterAt: String,                   // ISO LocalDateTime
    val exitAt: String,
    /** Czym wykryto: geo | wifi | geo+wifi | manual */
    val source: String = "geo",
    /** pending | accepted | rejected */
    val status: String = "pending",
    val otHours: Int = 0,
    val otRate: Int = 100,
    val countedFrom: String = "",
    val countedTo: String = "",
    val createdAt: String = "",
    /** Kod zmiany rozpoznanej przy analizie pobytu; pusty = nie wiadomo (stare wpisy). */
    val shiftCode: String = ""
) {
    val shift: Shift? get() = Shift.entries.firstOrNull { it.code == shiftCode }
}

@Dao
interface PresenceDao {
    @Query("SELECT * FROM presence ORDER BY enterAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 100): Flow<List<PresenceRow>>

    @Query("SELECT * FROM presence WHERE status = 'pending' ORDER BY enterAt")
    suspend fun pending(): List<PresenceRow>

    /** Cała historia wykryć — do kopii zapasowej. */
    @Query("SELECT * FROM presence ORDER BY enterAt")
    suspend fun wszystkie(): List<PresenceRow>

    @Query("SELECT * FROM presence WHERE id = :id LIMIT 1")
    suspend fun byId(id: Long): PresenceRow?

    @Query("SELECT * FROM presence WHERE date BETWEEN :from AND :to ORDER BY date")
    fun observeRange(from: String, to: String): Flow<List<PresenceRow>>

    @Query("SELECT * FROM presence WHERE date = :date ORDER BY enterAt")
    suspend fun forDate(date: String): List<PresenceRow>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(row: PresenceRow): Long

    @Update
    suspend fun update(row: PresenceRow)

    @Query("UPDATE presence SET status = :status WHERE id = :id")
    suspend fun setStatus(id: Long, status: String)

    @Query("DELETE FROM presence WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM presence")
    suspend fun clearAll()
}

/** Wydarzenie na dany dzień — „fryzjer 10:00", „wizyta", „urodziny". */
@Entity(tableName = "events")
data class EventRow(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(index = true) val date: String,                      // ISO yyyy-MM-dd
    /** HH:mm albo pusty, gdy wydarzenie bez godziny. */
    val time: String = "",
    val text: String,
    /** Czy przypomnieć dzień wcześniej. */
    val remind: Boolean = true
)

@Dao
interface EventDao {
    @Query("SELECT * FROM events WHERE date >= :from AND date <= :to ORDER BY date, time")
    suspend fun rangeOnce(from: String, to: String): List<EventRow>

    @Query("SELECT * FROM events WHERE date >= :from AND date <= :to ORDER BY date, time")
    fun observeRange(from: String, to: String): Flow<List<EventRow>>

    @Query("SELECT * FROM events WHERE date = :date ORDER BY time")
    suspend fun forDate(date: String): List<EventRow>

    @Query("SELECT * FROM events WHERE date = :date AND remind = 1 ORDER BY time")
    suspend fun remindersFor(date: String): List<EventRow>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: EventRow): Long

    @Update
    suspend fun update(row: EventRow)

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM events")
    suspend fun clearAll()
}

@Dao
interface DayDao {
    @Query("SELECT * FROM days WHERE date >= :from AND date <= :to")
    fun observeRange(from: String, to: String): Flow<List<DayRow>>

    @Query("SELECT * FROM days WHERE date >= :from AND date <= :to")
    suspend fun rangeOnce(from: String, to: String): List<DayRow>

    @Query("SELECT * FROM days WHERE date = :date LIMIT 1")
    suspend fun get(date: String): DayRow?

    /** Dni z konkretnym oznaczeniem (np. urlop) w zadanym zakresie. */
    @Query("SELECT * FROM days WHERE shift = :code AND date >= :from AND date <= :to ORDER BY date")
    fun observeWithShift(code: String, from: String, to: String): Flow<List<DayRow>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: DayRow)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rows: List<DayRow>)

    @Query("DELETE FROM days WHERE date = :date")
    suspend fun delete(date: String)

    @Query("DELETE FROM days WHERE date >= :from AND date <= :to")
    suspend fun clearRange(from: String, to: String)

    @Query("DELETE FROM days")
    suspend fun clearAll()
}

/**
 * Odcinek wypłaty wgrany przez użytkownika. Sam plik leży w pamięci aplikacji,
 * tu trzymamy tylko, czego dotyczy i jak się nazywa.
 */
@Entity(tableName = "payslips")
data class PayslipRow(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Miesiąc, którego dotyczy odcinek — „2026-08". */
    @ColumnInfo(index = true) val ym: String,
    /** Nazwa pliku w katalogu `odcinki/`. */
    val fileName: String,
    /** Nazwa, jaką plik miał na telefonie — pokazujemy ją na liście. */
    val originalName: String,
    val addedAt: String
)

@Dao
interface PayslipDao {
    @Query("SELECT * FROM payslips ORDER BY ym DESC")
    fun observeAll(): Flow<List<PayslipRow>>

    @Query("SELECT * FROM payslips WHERE ym = :ym LIMIT 1")
    suspend fun forMonth(ym: String): PayslipRow?

    /** Wszystkie odcinki — do kopii zapasowej i czyszczenia archiwum. */
    @Query("SELECT * FROM payslips ORDER BY ym")
    suspend fun wszystkie(): List<PayslipRow>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: PayslipRow)

    @Query("DELETE FROM payslips WHERE id = :id")
    suspend fun delete(id: Long)
}

@Database(
    entities = [DayRow::class, PresenceRow::class, EventRow::class, PayslipRow::class],
    version = 6, exportSchema = false
)
abstract class AppDb : RoomDatabase() {
    abstract fun dayDao(): DayDao
    abstract fun presenceDao(): PresenceDao
    abstract fun eventDao(): EventDao
    abstract fun payslipDao(): PayslipDao

    companion object {
        /**
         * v1 -> v2: automatyczne wykrywanie pracy (lokalizacja).
         * Prawdziwa migracja, nigdy destructive — zasada „zero utraty danych".
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    """
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
                )
            }
        }

        /** v2 -> v3: wydarzenia z przypomnieniem dzień wcześniej. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `events` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `date` TEXT NOT NULL,
                        `time` TEXT NOT NULL,
                        `text` TEXT NOT NULL,
                        `remind` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_events_date` ON `events` (`date`)")
            }
        }

        /**
         * v3 -> v4: zapamiętujemy zmianę rozpoznaną przy analizie pobytu.
         * Bez tego zamiana zmian gubiła się przy zapisie — dzień zostawał z etykietą z grafiku.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `presence` ADD COLUMN `shiftCode` TEXT NOT NULL DEFAULT ''")
            }
        }

        /** v4 -> v5: archiwum odcinków wypłaty. Same pliki leżą w pamięci aplikacji. */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `payslips` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `ym` TEXT NOT NULL,
                        `fileName` TEXT NOT NULL,
                        `originalName` TEXT NOT NULL,
                        `addedAt` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_payslips_ym` ON `payslips` (`ym`)")
            }
        }

        /**
         * v5 -> v6: zdjęcie dołączone do notatki dnia. Sam plik leży w pamięci
         * aplikacji, w bazie trzymamy tylko jego nazwę.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `days` ADD COLUMN `notePhoto` TEXT")
            }
        }

        @Volatile private var inst: AppDb? = null
        fun get(ctx: Context): AppDb = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(ctx.applicationContext, AppDb::class.java, "grafik.db")
                .addMigrations(
                    MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6
                )
                .build().also { inst = it }
        }
    }
}
