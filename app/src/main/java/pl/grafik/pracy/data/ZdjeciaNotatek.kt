package pl.grafik.pracy.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

/**
 * Zdjęcia dołączane do notatek dnia.
 *
 * Plik kopiujemy do pamięci aplikacji, żeby notatka została nawet wtedy, gdy
 * oryginał zniknie z galerii — i żeby nic nie wychodziło na zewnątrz. Jeden dzień
 * ma jedno zdjęcie; wgranie nowego zastępuje poprzednie.
 */
object ZdjeciaNotatek {

    private const val TAG = "GrafikZdjecia"
    private const val KATALOG = "notatki"

    private fun katalog(ctx: Context): File =
        File(ctx.filesDir, KATALOG).apply { if (!exists()) mkdirs() }

    fun plik(ctx: Context, nazwa: String): File = File(katalog(ctx), nazwa)

    /** Wszystkie zdjęcia notatek — potrzebne kopii zapasowej. */
    fun wszystkie(ctx: Context): List<File> =
        katalog(ctx).listFiles()?.filter { it.isFile }.orEmpty()

    /**
     * Kopiuje wskazane zdjęcie do pamięci aplikacji i zwraca nazwę pliku
     * do zapisania przy dniu.
     */
    suspend fun dodaj(ctx: Context, zrodlo: Uri, dzien: LocalDate): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val rozszerzenie = rozszerzenieZ(ctx, zrodlo)
                val nazwa = "$dzien.$rozszerzenie"
                val cel = plik(ctx, nazwa)

                ctx.contentResolver.openInputStream(zrodlo)?.use { we ->
                    cel.outputStream().use { wy -> we.copyTo(wy) }
                } ?: error("nie udało się otworzyć zdjęcia")

                Log.i(TAG, "dodano zdjęcie do notatki $dzien (${cel.length() / 1024} kB)")
                nazwa
            }.onFailure { Log.e(TAG, "nie udało się dodać zdjęcia", it) }
        }

    suspend fun usun(ctx: Context, nazwa: String) = withContext(Dispatchers.IO) {
        runCatching { plik(ctx, nazwa).delete() }
            .onFailure { Log.e(TAG, "nie udało się usunąć zdjęcia", it) }
        Unit
    }

    /** Otwarcie zdjęcia w przeglądarce obrazów zainstalowanej na telefonie. */
    fun intencjaOtwarcia(ctx: Context, nazwa: String): Intent? = runCatching {
        val uri = FileProvider.getUriForFile(
            ctx, "${ctx.packageName}.fileprovider", plik(ctx, nazwa)
        )
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, typ(nazwa))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }.getOrNull()

    private fun typ(nazwa: String): String =
        when (nazwa.substringAfterLast('.', "").lowercase()) {
            "png" -> "image/png"
            "webp" -> "image/webp"
            else -> "image/jpeg"
        }

    /** Rozszerzenie bierzemy z typu podanego przez system, bo nazwa pliku bywa pusta. */
    private fun rozszerzenieZ(ctx: Context, uri: Uri): String =
        when (ctx.contentResolver.getType(uri)) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
}
