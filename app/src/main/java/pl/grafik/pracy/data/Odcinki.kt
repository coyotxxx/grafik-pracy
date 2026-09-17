package pl.grafik.pracy.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.YearMonth

/**
 * Archiwum odcinków wypłaty. Pliki kopiujemy do pamięci aplikacji, żeby zostały nawet
 * wtedy, gdy oryginał zniknie z telefonu — i żeby nic nie wychodziło na zewnątrz.
 */
object Odcinki {

    private const val TAG = "GrafikOdcinki"
    private const val KATALOG = "odcinki"

    private fun katalog(ctx: Context): File =
        File(ctx.filesDir, KATALOG).apply { if (!exists()) mkdirs() }

    fun plik(ctx: Context, row: PayslipRow): File = File(katalog(ctx), row.fileName)

    /**
     * Kopiuje wskazany plik do archiwum i zapisuje wpis. Jeden miesiąc = jeden odcinek;
     * wgranie nowego zastępuje poprzedni, żeby nie robić bałaganu.
     */
    suspend fun dodaj(ctx: Context, zrodlo: Uri, ym: YearMonth): Result<PayslipRow> =
        withContext(Dispatchers.IO) {
            runCatching {
                val dao = AppDb.get(ctx).payslipDao()
                dao.forMonth(ym.toString())?.let { stary ->
                    plik(ctx, stary).delete()
                    dao.delete(stary.id)
                }

                val nazwaOryginalna = nazwaPliku(ctx, zrodlo) ?: "odcinek.pdf"
                val rozszerzenie = nazwaOryginalna.substringAfterLast('.', "pdf")
                val nazwa = "$ym.$rozszerzenie"
                val cel = File(katalog(ctx), nazwa)

                ctx.contentResolver.openInputStream(zrodlo)?.use { we ->
                    cel.outputStream().use { wy -> we.copyTo(wy) }
                } ?: error("nie udało się otworzyć pliku")

                val row = PayslipRow(
                    ym = ym.toString(),
                    fileName = nazwa,
                    originalName = nazwaOryginalna,
                    addedAt = LocalDateTime.now().toString()
                )
                dao.upsert(row)
                Log.i(TAG, "dodano odcinek $ym (${cel.length() / 1024} kB)")
                row
            }.onFailure { Log.e(TAG, "nie udało się dodać odcinka", it) }
        }

    suspend fun usun(ctx: Context, row: PayslipRow) = withContext(Dispatchers.IO) {
        runCatching {
            plik(ctx, row).delete()
            AppDb.get(ctx).payslipDao().delete(row.id)
        }.onFailure { Log.e(TAG, "nie udało się usunąć odcinka", it) }
        Unit
    }

    /** Intencja otwarcia odcinka w czytniku PDF zainstalowanym na telefonie. */
    fun intencjaOtwarcia(ctx: Context, row: PayslipRow): Intent? = runCatching {
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", plik(ctx, row))
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, typ(row.originalName))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }.getOrNull()

    private fun typ(nazwa: String): String = when (nazwa.substringAfterLast('.', "").lowercase()) {
        "pdf" -> "application/pdf"
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        else -> "*/*"
    }

    private fun nazwaPliku(ctx: Context, uri: Uri): String? =
        runCatching {
            ctx.contentResolver.query(uri, null, null, null, null)?.use { c ->
                val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (i >= 0 && c.moveToFirst()) c.getString(i) else null
            }
        }.getOrNull()
}
