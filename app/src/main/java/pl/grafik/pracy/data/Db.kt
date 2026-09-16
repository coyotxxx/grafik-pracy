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
    val dwnFor: String? = null
) {
    fun toEntry(): DayEntry = DayEntry(
        date = LocalDate.parse(date),
        shift = shift?.let { c -> Shift.entries.firstOrNull { it.code == c } },
        otHours = otHours,
        otRate = if (otRate == 50) OtRate.P50 else OtRate.P100,
        deviation = deviation,
        note = note,
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
    val createdAt: String = ""
)

@Dao
interface PresenceDao {
    @Query("SELECT * FROM presence ORDER BY enterAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 100): Flow<List<PresenceRow>>

    @Query("SELECT * FROM presence WHERE status = 'pending' ORDER BY enterAt")
    suspend fun pending(): List<PresenceRow>

    @Query("SELECT * FROM presence WHERE id = :id LIMIT 1")
    suspend fun byId(id: Long): PresenceRow?

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
}

@Dao
interface DayDao {
    @Query("SELECT * FROM days WHERE date >= :from AND date <= :to")
    fun observeRange(from: String, to: String): Flow<List<DayRow>>

    @Query("SELECT * FROM days WHERE date = :date LIMIT 1")
    suspend fun get(date: String): DayRow?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: DayRow)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rows: List<DayRow>)

    @Query("DELETE FROM days WHERE date = :date")
    suspend fun delete(date: String)

    @Query("DELETE FROM days WHERE date >= :from AND date <= :to")
    suspend fun clearRange(from: String, to: String)
}

@Database(entities = [DayRow::class, PresenceRow::class], version = 2, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun dayDao(): DayDao
    abstract fun presenceDao(): PresenceDao

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

        @Volatile private var inst: AppDb? = null
        fun get(ctx: Context): AppDb = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(ctx.applicationContext, AppDb::class.java, "grafik.db")
                .addMigrations(MIGRATION_1_2)
                .build().also { inst = it }
        }
    }
}
