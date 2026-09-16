package pl.grafik.pracy.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pl.grafik.pracy.BuildConfig
import pl.grafik.pracy.update.UpdateInfo
import pl.grafik.pracy.update.UpdateProgress
import pl.grafik.pracy.update.Updater

data class UpdateUi(
    val current: String = BuildConfig.VERSION_NAME,
    val available: UpdateInfo? = null,
    val progress: UpdateProgress = UpdateProgress.Idle,
    /** Pasek schowany na życzenie — do następnego uruchomienia apki. */
    val dismissed: Boolean = false,
    /** Ustawiane tylko przy ręcznym sprawdzeniu, żeby dać odpowiedź „masz najnowszą". */
    val upToDateMessage: String? = null
)

class UpdateVm(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(UpdateUi())
    val state: StateFlow<UpdateUi> = _state.asStateFlow()

    init { check(manual = false) }

    fun check(manual: Boolean) = viewModelScope.launch {
        if (manual) _state.value = _state.value.copy(progress = UpdateProgress.Checking, upToDateMessage = null)
        val info = Updater.check(BuildConfig.VERSION_NAME)
        _state.value = _state.value.copy(
            available = info,
            dismissed = if (info != null) false else _state.value.dismissed,
            progress = UpdateProgress.Idle,
            upToDateMessage = if (manual && info == null) "Masz najnowszą wersję (${BuildConfig.VERSION_NAME})" else null
        )
    }

    fun install() = viewModelScope.launch {
        val info = _state.value.available ?: return@launch
        Updater.downloadAndInstall(getApplication(), info) { p ->
            _state.value = _state.value.copy(progress = p)
        }
    }

    fun dismiss() { _state.value = _state.value.copy(dismissed = true) }
    fun clearMessage() { _state.value = _state.value.copy(upToDateMessage = null) }
}
