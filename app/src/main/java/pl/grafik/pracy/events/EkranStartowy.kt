package pl.grafik.pracy.events

import android.content.Context
import android.content.Intent
import pl.grafik.pracy.MainActivity

/**
 * Ekran, który ma się otworzyć po dotknięciu powiadomienia.
 *
 * Nie wskazujemy aktywności na sztywno, bo w wariancie „nowy" stara `MainActivity`
 * nadal siedzi w pakiecie — usuwamy jej tylko ikonę z pulpitu. Powiadomienie
 * otwierało więc stary ekran w nowej aplikacji (zgłoszenie Macieja z 25.09.2026:
 * „pokazało mi starą szatę graficzną, choć odinstalowałem starą wersję").
 *
 * Pytamy system o to, co jest ekranem startowym TEJ aplikacji: w wariancie nowym
 * jest to `NowaActivity`, w klasycznym `MainActivity`. Dzięki temu jedno wspólne
 * rozwiązanie działa w obu wariantach i nie zmienia zachowania klasycznego.
 */
fun ekranStartowy(ctx: Context): Intent =
    ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
        ?: Intent(ctx, MainActivity::class.java)
