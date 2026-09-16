package pl.grafik.pracy.location

import android.content.Context
import android.net.wifi.WifiManager

/**
 * Sprawdzenie, czy telefon jest w firmowej sieci Wi-Fi.
 * Wi-Fi jest pewniejsze niż GPS w hali i nie kosztuje baterii — używamy go
 * do potwierdzania obecności i do wetowania fałszywych wyjść ze strefy.
 */
object WifiCheck {

    fun currentSsid(ctx: Context): String? {
        val wm = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return null
        @Suppress("DEPRECATION")
        val raw = runCatching { wm.connectionInfo?.ssid }.getOrNull() ?: return null
        val ssid = raw.trim().removeSurrounding("\"")
        return if (ssid.isBlank() || ssid == "<unknown ssid>" || ssid == "0x") null else ssid
    }

    fun isAtWork(ctx: Context, workSsid: String): Boolean {
        if (workSsid.isBlank()) return false
        return currentSsid(ctx)?.equals(workSsid, ignoreCase = true) == true
    }
}
