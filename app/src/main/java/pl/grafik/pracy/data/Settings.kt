package pl.grafik.pracy.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pl.grafik.pracy.domain.CyclePattern
import pl.grafik.pracy.domain.CycleConfig
import pl.grafik.pracy.domain.WorkPlace
import java.time.LocalDate

private val Context.ds by preferencesDataStore("settings")

/** Ustawienia cyklu i kolorów. Kolory wybierane z gotowej palety — bez ręcznego mieszania. */
class SettingsStore(private val ctx: Context) {

    private val kPattern = stringPreferencesKey("pattern")
    private val kAnchor = stringPreferencesKey("anchor")
    private val kIndex = intPreferencesKey("anchorIndex")
    private val kBrigade = stringPreferencesKey("brigade")
    private val kColors = stringPreferencesKey("colors")
    private val kWpOn = booleanPreferencesKey("wp_enabled")
    private val kWpLat = doublePreferencesKey("wp_lat")
    private val kWpLon = doublePreferencesKey("wp_lon")
    private val kWpR = intPreferencesKey("wp_radius")
    private val kWpSsid = stringPreferencesKey("wp_ssid")
    private val kWpStay = intPreferencesKey("wp_min_stay")
    private val kWpGap = intPreferencesKey("wp_merge_gap")

    val config: Flow<CycleConfig> = ctx.ds.data.map { p ->
        CycleConfig(
            pattern = runCatching { CyclePattern.valueOf(p[kPattern] ?: "") }.getOrDefault(CyclePattern.B4_16D),
            anchorDate = runCatching { LocalDate.parse(p[kAnchor]) }.getOrDefault(LocalDate.of(2024, 3, 1)),
            anchorIndex = p[kIndex] ?: 0,
            brigade = p[kBrigade] ?: "A"
        )
    }

    /** Mapa "kodTypu:idKoloru" rozdzielona przecinkami. */
    val colors: Flow<Map<String, String>> = ctx.ds.data.map { p ->
        (p[kColors] ?: "").split(",").mapNotNull {
            val kv = it.split(":"); if (kv.size == 2) kv[0] to kv[1] else null
        }.toMap()
    }

    suspend fun saveConfig(c: CycleConfig) {
        ctx.ds.edit { p ->
            p[kPattern] = c.pattern.name
            p[kAnchor] = c.anchorDate.toString()
            p[kIndex] = c.anchorIndex
            p[kBrigade] = c.brigade
        }
    }

    /** Miejsce pracy — strefa wykrywania obecności. Domyślnie WYŁĄCZONA. */
    val workPlace: Flow<WorkPlace> = ctx.ds.data.map { p ->
        WorkPlace(
            enabled = p[kWpOn] ?: false,
            lat = p[kWpLat] ?: 0.0,
            lon = p[kWpLon] ?: 0.0,
            radiusM = p[kWpR] ?: 200,
            ssid = p[kWpSsid] ?: "",
            minStayMin = p[kWpStay] ?: 30,
            mergeGapMin = p[kWpGap] ?: 30
        )
    }

    suspend fun saveWorkPlace(w: WorkPlace) {
        ctx.ds.edit { p ->
            p[kWpOn] = w.enabled
            p[kWpLat] = w.lat
            p[kWpLon] = w.lon
            p[kWpR] = w.radiusM
            p[kWpSsid] = w.ssid
            p[kWpStay] = w.minStayMin
            p[kWpGap] = w.mergeGapMin
        }
    }

    suspend fun saveColors(m: Map<String, String>) {
        ctx.ds.edit { p -> p[kColors] = m.entries.joinToString(",") { "${it.key}:${it.value}" } }
    }
}
