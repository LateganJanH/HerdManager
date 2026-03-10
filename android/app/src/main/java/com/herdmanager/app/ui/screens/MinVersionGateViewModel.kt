package com.herdmanager.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.herdmanager.app.BuildConfig
import com.herdmanager.app.domain.repository.AppConfigRepository
import com.herdmanager.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class MinVersionState {
    data object Loading : MinVersionState()
    data object Ok : MinVersionState()
    data object UpdateRequired : MinVersionState()
    /** Access suspended (e.g. subscription lapsed). Show "Subscription lapsed. Contact support." */
    data object AccessSuspended : MinVersionState()
}

@HiltViewModel
class MinVersionGateViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val appConfigRepository: AppConfigRepository
) : ViewModel() {

    val authState = authRepository.authState()

    private val _minVersionState = MutableStateFlow<MinVersionState>(MinVersionState.Loading)
    val minVersionState: StateFlow<MinVersionState> = _minVersionState.asStateFlow()

    init {
        authRepository.authState()
            .distinctUntilChanged()
            .onEach { user ->
                if (user == null) {
                    _minVersionState.value = MinVersionState.Ok
                    return@onEach
                }
                _minVersionState.value = MinVersionState.Loading
                viewModelScope.launch {
                    val accessSuspended = appConfigRepository.getAccessSuspended(user.uid)
                    _minVersionState.value = when {
                        accessSuspended -> MinVersionState.AccessSuspended
                        else -> {
                            val minCode = appConfigRepository.getMinVersionCode(user.uid)
                            if (minCode != null && BuildConfig.VERSION_CODE < minCode) {
                                MinVersionState.UpdateRequired
                            } else {
                                MinVersionState.Ok
                            }
                        }
                    }
                }
            }
            .launchIn(viewModelScope)
    }
}
