package pl.grafik.pracy.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Kopia zapasowa: jeden plik `.json` z całym stanem aplikacji.
 *
 * W środku siedzi wszystko, co zna aplikacja — ręcznie zmienione dni, wydarzenia,
 * historia wykryć pracy, ustawienia i odcinki wypłaty razem z treścią plików
 * (decyzja Macieja z 17.09.2026: jedna kopia ma wystarczyć przy zmianie telefonu).
 * Grafik z cyklu nie jest zapisywany, bo liczy się sam z ustawień.
 *
 * Plik powstaje wyłącznie tam, gdzie wskaże użytkownik, albo w pamięci aplikacji —
 * nic nie jest nigdzie wysyłane.
 */
object Kopia {

    private const val TAG = "GrafikKopia"

    /** Wersja formatu. Rośnie, gdy zmieni się układ pliku. */
    const val FORMAT = 1

    private const val KATALOG = "kopie"

    /** Ile automatycznych kopii trzymamy w telefonie. */
    const val ILE_AUTOMATYCZNYCH = 5

    // ─────────────────────────────────────────────────────────────
    // CO JEST ZAPISANE
    // ─────────────────────────────────────────────────────────────

    /** Liczby na kartę „co jest zapisane". */
    data class StanDanych(
        val dni: Int = 0,
        val wydarzenia: Int = 0,
        val wykrycia: Int = 0,
        val odcinki: Int = 0
    )

    /** Co weszło do aplikacji po wczytaniu kopii. */
    data class Podsumowanie(
        val dni: Int,
        val wydarzenia: Int,
        val wykrycia: Int,
        val odcinki: Int,
        val ustawienia: Int
    )

    /** Jedna kopia leżąca w pamięci aplikacji. */
    data class PlikKopii(val nazwa: String, val bajty: Long, val plik: File) {
        /** „34 kB" — rozmiar po ludzku. */
        val rozmiar: String
            get() = if (bajty < 1024) "$bajty B" else "${(bajty + 512) / 1024} kB"
    }

    suspend fun stan(ctx: Context): StanDanych = withContext(Dispatchers.IO) {
        runCatching {
            val db = AppDb.get(ctx)
            StanDanych(
                dni = db.dayDao().rangeOnce(ZAWSZE_OD, ZAWSZE_DO).size,
                wydarzenia = db.eventDao().rangeOnce(ZAWSZE_OD, ZAWSZE_DO).size,
                wykrycia = db.presenceDao().wszystkie().size,
                odcinki = db.payslipDao().wszystkie().size
            )
        }.getOrDefault(StanDanych())
    }

    /** Zakres „od zawsze do zawsze" — daty trzymamy jako tekst ISO, więc porównanie jest leksykalne. */
    private const val ZAWSZE_OD = "0000-01-01"
    private const val ZAWSZE_DO = "9999-12-31"

    // ─────────────────────────────────────────────────────────────
    // ZRZUT
    // ─────────────────────────────────────────────────────────────

    /** Wersja apki, z której powstała kopia — do wglądu, gdyby format kiedyś się zmienił. */
    private fun wersja(ctx: Context): String = runCatching {
        ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: ""
    }.getOrDefault("")

    /** Cały stan aplikacji jako tekst JSON. */
    suspend fun zrzut(ctx: Context, wersjaApki: String = ""): String = withContext(Dispatchers.IO) {
        val db = AppDb.get(ctx)
        val root = JSONObject()
        root.put("format", FORMAT)
        root.put("aplikacja", "grafik-pracy")
        root.put("wersja", wersjaApki.ifEmpty { wersja(ctx) })
        root.put("utworzono", LocalDateTime.now().toString())

        val ustawienia = JSONArray()
        SettingsStore(ctx).zrzucWszystko().forEach { (nazwa, wartosc) ->
            ustawienia.put(
                JSONObject()
                    .put("k", nazwa)
                    .put("t", literaTypu(wartosc))
                    .put("v", wartosc.toString())
            )
        }
        root.put("ustawienia", ustawienia)

        val dni = JSONArray()
        db.dayDao().rangeOnce(ZAWSZE_OD, ZAWSZE_DO).forEach { r ->
            dni.put(
                JSONObject()
                    .put("date", r.date)
                    .put("shift", r.shift ?: JSONObject.NULL)
                    .put("otHours", r.otHours)
                    .put("otRate", r.otRate)
                    .put("deviation", r.deviation)
                    .put("note", r.note)
                    .put("dwnFor", r.dwnFor ?: JSONObject.NULL)
            )
        }
        root.put("dni", dni)

        val wydarzenia = JSONArray()
        db.eventDao().rangeOnce(ZAWSZE_OD, ZAWSZE_DO).forEach { r ->
            wydarzenia.put(
                JSONObject()
                    .put("date", r.date)
                    .put("time", r.time)
                    .put("text", r.text)
                    .put("remind", r.remind)
            )
        }
        root.put("wydarzenia", wydarzenia)

        val wykrycia = JSONArray()
        db.presenceDao().wszystkie().forEach { r ->
            wykrycia.put(
                JSONObject()
                    .put("date", r.date)
                    .put("enterAt", r.enterAt)
                    .put("exitAt", r.exitAt)
                    .put("source", r.source)
                    .put("status", r.status)
                    .put("otHours", r.otHours)
                    .put("otRate", r.otRate)
                    .put("countedFrom", r.countedFrom)
                    .put("countedTo", r.countedTo)
                    .put("createdAt", r.createdAt)
                    .put("shiftCode", r.shiftCode)
            )
        }
        root.put("wykrycia", wykrycia)

        val odcinki = JSONArray()
        db.payslipDao().wszystkie().forEach { r ->
            val plik = Odcinki.plik(ctx, r)
            val tresc = runCatching {
                if (plik.exists()) Base64.encodeToString(plik.readBytes(), Base64.NO_WRAP) else ""
            }.getOrDefault("")
            odcinki.put(
                JSONObject()
                    .put("ym", r.ym)
                    .put("fileName", r.fileName)
                    .put("originalName", r.originalName)
                    .put("addedAt", r.addedAt)
                    .put("plik", tresc)
            )
        }
        root.put("odcinki", odcinki)

        root.toString()
    }

    private fun literaTypu(wartosc: Any): String = when (wartosc) {
        is Boolean -> "b"
        is Int -> "i"
        is Long -> "l"
        is Float -> "f"
        is Double -> "d"
        else -> "s"
    }

    private fun zTekstu(litera: String, tekst: String): Any? = when (litera) {
        "b" -> tekst.toBooleanStrictOrNull()
        "i" -> tekst.toIntOrNull()
        "l" -> tekst.toLongOrNull()
        "f" -> tekst.toFloatOrNull()
        "d" -> tekst.toDoubleOrNull()
        else -> tekst
    }

    // ─────────────────────────────────────────────────────────────
    // WCZYTANIE
    // ─────────────────────────────────────────────────────────────

    /**
     * Wczytuje kopię. Obecne dane znikają — dlatego ekran pyta o to wprost,
     * a przed czyszczeniem proponuje zapisanie kopii.
     */
    suspend fun wczytaj(ctx: Context, json: String): Result<Podsumowanie> =
        withContext(Dispatchers.IO) {
            runCatching {
                val root = JSONObject(json)
                val format = root.optInt("format", 0)
                require(root.optString("aplikacja") == "grafik-pracy") {
                    "to nie jest kopia Grafiku Pracy"
                }
                require(format in 1..FORMAT) { "nieznana wersja pliku ($format)" }

                val db = AppDb.get(ctx)

                val ustawienia = mutableMapOf<String, Any>()
                root.optJSONArray("ustawienia")?.let { tab ->
                    for (i in 0 until tab.length()) {
                        val o = tab.getJSONObject(i)
                        zTekstu(o.optString("t", "s"), o.optString("v"))
                            ?.let { ustawienia[o.getString("k")] = it }
                    }
                }

                val dni = mutableListOf<DayRow>()
                root.optJSONArray("dni")?.let { tab ->
                    for (i in 0 until tab.length()) {
                        val o = tab.getJSONObject(i)
                        dni += DayRow(
                            date = o.getString("date"),
                            shift = o.tekstAlboNull("shift"),
                            otHours = o.optInt("otHours"),
                            otRate = o.optInt("otRate", 100),
                            deviation = o.optBoolean("deviation"),
                            note = o.optString("note"),
                            dwnFor = o.tekstAlboNull("dwnFor")
                        )
                    }
                }

                val wydarzenia = mutableListOf<EventRow>()
                root.optJSONArray("wydarzenia")?.let { tab ->
                    for (i in 0 until tab.length()) {
                        val o = tab.getJSONObject(i)
                        wydarzenia += EventRow(
                            date = o.getString("date"),
                            time = o.optString("time"),
                            text = o.optString("text"),
                            remind = o.optBoolean("remind", true)
                        )
                    }
                }

                val wykrycia = mutableListOf<PresenceRow>()
                root.optJSONArray("wykrycia")?.let { tab ->
                    for (i in 0 until tab.length()) {
                        val o = tab.getJSONObject(i)
                        wykrycia += PresenceRow(
                            date = o.getString("date"),
                            enterAt = o.optString("enterAt"),
                            exitAt = o.optString("exitAt"),
                            source = o.optString("source", "geo"),
                            status = o.optString("status", "pending"),
                            otHours = o.optInt("otHours"),
                            otRate = o.optInt("otRate", 100),
                            countedFrom = o.optString("countedFrom"),
                            countedTo = o.optString("countedTo"),
                            createdAt = o.optString("createdAt"),
                            shiftCode = o.optString("shiftCode")
                        )
                    }
                }

                // Wszystko sprawdzone — dopiero teraz ruszamy obecne dane.
                db.dayDao().clearAll()
                db.eventDao().clearAll()
                db.presenceDao().clearAll()
                wyczyscOdcinki(ctx)

                if (dni.isNotEmpty()) db.dayDao().upsertAll(dni)
                wydarzenia.forEach { db.eventDao().upsert(it) }
                wykrycia.forEach { db.presenceDao().insert(it) }

                var ileOdcinkow = 0
                root.optJSONArray("odcinki")?.let { tab ->
                    for (i in 0 until tab.length()) {
                        val o = tab.getJSONObject(i)
                        val row = PayslipRow(
                            ym = o.getString("ym"),
                            fileName = o.getString("fileName"),
                            originalName = o.optString("originalName"),
                            addedAt = o.optString("addedAt")
                        )
                        val tresc = o.optString("plik")
                        if (tresc.isNotEmpty()) {
                            runCatching {
                                val cel = Odcinki.plik(ctx, row)
                                cel.parentFile?.mkdirs()
                                cel.writeBytes(Base64.decode(tresc, Base64.NO_WRAP))
                            }
                        }
                        db.payslipDao().upsert(row)
                        ileOdcinkow++
                    }
                }

                SettingsStore(ctx).wczytajWszystko(ustawienia)

                Log.i(TAG, "wczytano kopię: ${dni.size} dni, ${wydarzenia.size} wydarzeń")
                Podsumowanie(
                    dni = dni.size,
                    wydarzenia = wydarzenia.size,
                    wykrycia = wykrycia.size,
                    odcinki = ileOdcinkow,
                    ustawienia = ustawienia.size
                )
            }.onFailure { Log.e(TAG, "nie udało się wczytać kopii", it) }
        }

    private fun JSONObject.tekstAlboNull(nazwa: String): String? =
        if (isNull(nazwa)) null else optString(nazwa).ifEmpty { null }

    // ─────────────────────────────────────────────────────────────
    // PLIKI
    // ─────────────────────────────────────────────────────────────

    /** „grafik-2026-09-17.json" */
    fun nazwaPliku(dzien: LocalDate = LocalDate.now()): String = "grafik-$dzien.json"

    suspend fun zapiszDo(ctx: Context, cel: Uri, wersjaApki: String = ""): Result<Int> =
        withContext(Dispatchers.IO) {
            runCatching {
                val tekst = zrzut(ctx, wersjaApki)
                ctx.contentResolver.openOutputStream(cel, "wt")?.use { wy ->
                    wy.write(tekst.toByteArray())
                } ?: error("nie udało się otworzyć pliku do zapisu")
                Log.i(TAG, "zapisano kopię (${tekst.length / 1024} kB)")
                tekst.length
            }.onFailure { Log.e(TAG, "nie udało się zapisać kopii", it) }
        }

    suspend fun wczytajZ(ctx: Context, zrodlo: Uri): Result<Podsumowanie> =
        withContext(Dispatchers.IO) {
            runCatching {
                val tekst = ctx.contentResolver.openInputStream(zrodlo)?.use {
                    it.readBytes().decodeToString()
                } ?: error("nie udało się otworzyć pliku")
                wczytaj(ctx, tekst).getOrThrow()
            }.onFailure { Log.e(TAG, "nie udało się wczytać pliku", it) }
        }

    private fun katalog(ctx: Context): File =
        File(ctx.filesDir, KATALOG).apply { if (!exists()) mkdirs() }

    /** Kopie leżące w pamięci telefonu, od najnowszej. */
    suspend fun kopieLokalne(ctx: Context): List<PlikKopii> = withContext(Dispatchers.IO) {
        runCatching {
            katalog(ctx).listFiles { f -> f.isFile && f.name.endsWith(".json") }
                ?.sortedByDescending { it.name }
                ?.map { PlikKopii(it.name, it.length(), it) }
                ?: emptyList()
        }.getOrDefault(emptyList())
    }

    /**
     * Kopia do pamięci aplikacji. Zostawiamy [ILE_AUTOMATYCZNYCH] najnowszych —
     * starsze kasujemy, żeby archiwum nie rosło bez końca.
     */
    suspend fun zrobKopieLokalna(ctx: Context, wersjaApki: String = ""): Result<PlikKopii> =
        withContext(Dispatchers.IO) {
            runCatching {
                val plik = File(katalog(ctx), nazwaPliku())
                plik.writeText(zrzut(ctx, wersjaApki))
                kopieLokalne(ctx).drop(ILE_AUTOMATYCZNYCH).forEach { it.plik.delete() }
                Log.i(TAG, "kopia automatyczna: ${plik.name} (${plik.length() / 1024} kB)")
                PlikKopii(plik.name, plik.length(), plik)
            }.onFailure { Log.e(TAG, "nie udało się zrobić kopii automatycznej", it) }
        }

    suspend fun przywroc(ctx: Context, kopia: PlikKopii): Result<Podsumowanie> =
        withContext(Dispatchers.IO) {
            runCatching { wczytaj(ctx, kopia.plik.readText()).getOrThrow() }
        }

    // ─────────────────────────────────────────────────────────────
    // OPERACJE NIEODWRACALNE
    // ─────────────────────────────────────────────────────────────

    /** Kasuje ręcznie zmienione dni. Kalendarz wraca do tego, co wynika z cyklu. */
    suspend fun przywrocGrafikDoCyklu(ctx: Context): Unit = withContext(Dispatchers.IO) {
        AppDb.get(ctx).dayDao().clearAll()
        Log.i(TAG, "grafik przywrócony do cyklu")
    }

    /** Kasuje wszystko: grafik, wydarzenia, wykrycia, odcinki i ustawienia. */
    suspend fun wyczyscWszystko(ctx: Context): Unit = withContext(Dispatchers.IO) {
        val db = AppDb.get(ctx)
        db.dayDao().clearAll()
        db.eventDao().clearAll()
        db.presenceDao().clearAll()
        wyczyscOdcinki(ctx)
        SettingsStore(ctx).clearAll()
        Log.i(TAG, "wyczyszczono wszystkie dane")
    }

    private suspend fun wyczyscOdcinki(ctx: Context) {
        val dao = AppDb.get(ctx).payslipDao()
        dao.wszystkie().forEach { row ->
            runCatching { Odcinki.plik(ctx, row).delete() }
            dao.delete(row.id)
        }
    }
}
