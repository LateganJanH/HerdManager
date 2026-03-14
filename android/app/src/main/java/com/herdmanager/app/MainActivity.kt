package com.herdmanager.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.herdmanager.app.domain.model.ThemeMode
import com.herdmanager.app.navigation.AppNavigation
import com.herdmanager.app.notifications.EXTRA_NAVIGATE_TO
import com.herdmanager.app.ui.screens.MinVersionGateViewModel
import com.herdmanager.app.ui.screens.MinVersionState
import com.herdmanager.app.ui.screens.SignInScreen
import com.herdmanager.app.ui.screens.SubscriptionLapsedScreen
import com.herdmanager.app.ui.screens.UpdateRequiredScreen
import com.herdmanager.app.ui.screens.ThemeViewModel
import com.herdmanager.app.ui.theme.HerdManagerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val pendingNavigation = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingNavigation.value = intent.getStringExtra(EXTRA_NAVIGATE_TO)
        setContent {
            RootContent(
                pendingNavigation = pendingNavigation,
                onConsumeNavigation = { pendingNavigation.value = it }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(EXTRA_NAVIGATE_TO)?.let { pendingNavigation.value = it }
    }
}

@Composable
private fun RootContent(
    pendingNavigation: androidx.compose.runtime.MutableState<String?>,
    onConsumeNavigation: (String?) -> Unit,
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    val themeMode by themeViewModel.themeMode.collectAsState(ThemeMode.SYSTEM)
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    HerdManagerTheme(darkTheme = darkTheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AuthGate(
                pendingNavigation = pendingNavigation,
                onConsumeNavigation = onConsumeNavigation
            )
        }
    }
}

@Composable
private fun AuthGate(
    pendingNavigation: androidx.compose.runtime.MutableState<String?>,
    onConsumeNavigation: (String?) -> Unit,
    viewModel: MinVersionGateViewModel = hiltViewModel()
) {
    val user by viewModel.authState.collectAsState(initial = null)
    val minVersionState by viewModel.minVersionState.collectAsState(initial = MinVersionState.Loading)
    if (user == null) {
        SignInScreen()
    } else when (minVersionState) {
        is MinVersionState.Loading -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        is MinVersionState.UpdateRequired -> UpdateRequiredScreen()
        is MinVersionState.AccessSuspended -> SubscriptionLapsedScreen()
        is MinVersionState.Ok -> AppNavigation(
            pendingNavigation = pendingNavigation,
            onConsumeNavigation = onConsumeNavigation
        )
    }
}
