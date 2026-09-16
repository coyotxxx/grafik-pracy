package pl.grafik.pracy.ui

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pl.grafik.pracy.data.AppDb
import pl.grafik.pracy.data.PresenceRow
import pl.grafik.pracy.data.SettingsStore
import pl.grafik.pracy.domain.WorkPlace
import pl.grafik.pracy.location.*

data class PresenceUi(
    val place: WorkPlace = WorkPlace(),
    val log: List<PresenceRow> = emptyList(),
    val currentSsid: String? = null,
    val distanceM: Int? = null,
    val capturing: Boolean = false,
    val error: String? = null
)

class PresenceVm(app: Application) : AndroidViewModel(app) {

    private val settings = SettingsStore(app)
    private val dao = AppDb.get(app).presenceDao()

    private val _ssid = MutableStateFlow<String?>(null)
    private val _dist = MutableStateFlow<Int?>(null)
    private val _capturing = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    val state: StateFlow<PresenceUi> = combine(
        settings.workPlace, dao.observeRecent(60), _ssid, _dist, _capturing, _error
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        PresenceUi(
            place = arr[0] as WorkPlace,
            log = arr[1] as List<PresenceRow>,
            currentSsid = arr[2] as String?,
            distanceM = arr[3] as Int?,
            capturing = arr[4] as Boolean,
            error = arr[5] as String?
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PresenceUi())

    init {
        refreshSsid()
        viewModelScope.launch { refreshDistance() }
    }

    fun refreshSsid() {
        _ssid.value = WifiCheck.currentSsid(getApplication())
    }

    /** „Jestem teraz w pracy" — prostsze i celniejsze niż szukanie po mapie. */
    fun captureHere() = viewModelScope.launch {
        _capturing.value = true
        _error.value = null
        val loc = lastLocation()
        if (loc == null) {
            _error.value = "Nie udało się ustalić pozycji. Sprawdź, czy lokalizacja jest włączona, " +
                "wyjdź pod okno albo na zewnątrz i spróbuj ponownie."
        } else {
            val cur = settings.workPlace.first()
            settings.saveWorkPlace(cur.copy(lat = loc.latitude, lon = loc.longitude))
            _dist.value = 0
        }
        _capturing.value = false
    }

    fun captureSsid() = viewModelScope.launch {
        val ssid = WifiCheck.currentSsid(getApplication())
        _ssid.value = ssid
        if (ssid == null) {
            _error.value = "Telefon nie jest połączony z żadną siecią Wi-Fi."
        } else {
            settings.saveWorkPlace(settings.workPlace.first().copy(ssid = ssid))
            _error.value = null
        }
    }

    fun clearSsid() = viewModelScope.launch {
        settings.saveWorkPlace(settings.workPlace.first().copy(ssid = ""))
    }

    fun setRadius(m: Int) = update { it.copy(radiusM = m.coerceIn(100, 500)) }
    fun setMinStay(m: Int) = update { it.copy(minStayMin = m.coerceIn(10, 120)) }
    fun setMergeGap(m: Int) = update { it.copy(mergeGapMin = m.coerceIn(5, 90)) }

    fun setEnabled(on: Boolean) = viewModelScope.launch {
        val app = getApplication<Application>()
        val wp = settings.workPlace.first().copy(enabled = on)
        settings.saveWorkPlace(wp)
        _error.value = if (on) {
            val err = GeofenceManager.register(app, wp)
            if (err == null) PresenceWatchdog.schedule(app)
            err
        } else {
            GeofenceManager.unregister(app)
            PresenceWatchdog.cancel(app)
            null
        }
    }

    fun accept(id: Long) = viewModelScope.launch { PresenceRepo.accept(getApplication(), id) }
    fun reject(id: Long) = viewModelScope.launch { PresenceRepo.reject(getApplication(), id) }

    private fun update(f: (WorkPlace) -> WorkPlace) = viewModelScope.launch {
        val wp = f(settings.workPlace.first())
        settings.saveWorkPlace(wp)
        if (wp.enabled) {
            GeofenceManager.unregister(getApplication())
            _error.value = GeofenceManager.register(getApplication(), wp)
        }
    }

    private suspend fun refreshDistance() {
        val wp = settings.workPlace.first()
        if (!wp.isSet) return
        val loc = lastLocation() ?: return
        val out = FloatArray(1)
        Location.distanceBetween(loc.latitude, loc.longitude, wp.lat, wp.lon, out)
        _dist.value = out[0].toInt()
    }

    /** Ta sama sonda co watchdog — z limitem czasu, żeby przycisk nie wisiał bez końca. */
    private suspend fun lastLocation(): Location? = LocationProbe.current(getApplication())
}
