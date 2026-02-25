package com.herdmanager.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.herdmanager.app.domain.model.ThemeMode
import com.herdmanager.app.navigation.AppNavigation
import com.herdmanager.app.ui.screens.AuthViewModel
import com.herdmanager.app.ui.screens.SignInScreen
import com.herdmanager.app.ui.screens.ThemeViewModel
import com.herdmanager.app.ui.theme.HerdManagerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RootContent()
        }
    }
}

@Composable
private fun RootContent(
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
            AuthGate()
        }
    }
}

@Composable
private fun AuthGate(
    viewModel: AuthViewModel = hiltViewModel()
) {
    val user by viewModel.authState.collectAsState(initial = null)
    if (user == null) {
        SignInScreen()
    } else {
        AppNavigation()
    }
}
