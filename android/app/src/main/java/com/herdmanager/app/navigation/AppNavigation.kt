package com.herdmanager.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PregnantWoman
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.herdmanager.app.ui.screens.AddAnimalScreen
import com.herdmanager.app.ui.screens.EditAnimalScreen
import com.herdmanager.app.ui.screens.AnimalDetailScreen
import com.herdmanager.app.ui.screens.BreedingScreen
import com.herdmanager.app.ui.screens.FarmSettingsScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.herdmanager.app.ui.screens.HerdListScreen
import com.herdmanager.app.ui.screens.HerdSummaryScreen
import com.herdmanager.app.ui.screens.HerdSummaryViewModel
import com.herdmanager.app.ui.screens.HomeScreen
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Home : Screen("home", "Home", Icons.Default.Home)
    data object HerdList : Screen("herd_list", "Profiles", Icons.AutoMirrored.Filled.List)
    data object Breeding : Screen("breeding", "Alerts", Icons.Default.Notifications)
    data object AddAnimal : Screen("add_animal", "Add", Icons.AutoMirrored.Filled.List)
    data object AnimalDetail : Screen("animal/{animalId}", "Animal", Icons.AutoMirrored.Filled.List) {
        fun route(animalId: String) = "animal/$animalId"
    }
    data object EditAnimal : Screen("edit_animal/{animalId}", "Edit", Icons.AutoMirrored.Filled.List) {
        fun route(animalId: String) = "edit_animal/$animalId"
    }
    data object FarmSettings : Screen("farm_settings", "Settings", Icons.Default.Settings)
    data object HerdSummary : Screen("herd_summary", "Analytics", Icons.Default.Assessment)
}

val bottomNavScreens = listOf(Screen.Home, Screen.HerdList, Screen.Breeding, Screen.HerdSummary, Screen.FarmSettings)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomNavScreens.map { it.route }
    val summaryVm: HerdSummaryViewModel = hiltViewModel()
    val lifecycleOwner = LocalLifecycleOwner.current
    var showAddAnimalSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) summaryVm.syncNow()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BoxWithConstraints {
        val isLargeScreen = maxWidth >= 600.dp

        if (showAddAnimalSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showAddAnimalSheet = false },
                sheetState = sheetState,
                content = {
                    AddAnimalScreen(onNavigateBack = { scope.launch { sheetState.hide() } })
                }
            )
        }

    androidx.compose.material3.Scaffold(
        bottomBar = {
            if (showBottomBar) {
                val summary by summaryVm.summary.collectAsState()
                val dueSoonCount = summary.dueSoonCount
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    for (screen in bottomNavScreens) {
                        val showBadge = screen == Screen.Breeding && dueSoonCount > 0
                        NavigationBarItem(
                            modifier = Modifier.testTag("nav_${screen.route}"),
                            icon = {
                                Box {
                                    Icon(screen.icon, contentDescription = screen.title)
                                    if (showBadge) {
                                        Badge(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .offset(x = 8.dp, y = (-4).dp),
                                            containerColor = MaterialTheme.colorScheme.error,
                                            contentColor = MaterialTheme.colorScheme.onError
                                        ) {
                                            Text(
                                                text = if (dueSoonCount > 99) "99+" else "$dueSoonCount",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }
                            },
                            label = { Text(screen.title) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(Screen.Home.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToProfiles = {
                        navController.navigate(Screen.HerdList.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToAlerts = {
                        navController.navigate(Screen.Breeding.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToAnalytics = {
                        navController.navigate(Screen.HerdSummary.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.FarmSettings.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToAnimal = { id -> navController.navigate(Screen.AnimalDetail.route(id)) }
                )
            }
            composable(Screen.HerdList.route) {
                HerdListScreen(
                    onAddAnimal = if (isLargeScreen) { { showAddAnimalSheet = true } } else { { navController.navigate(Screen.AddAnimal.route) } },
                    onAnimalClick = { id -> navController.navigate(Screen.AnimalDetail.route(id)) },
                    onNavigateToBreeding = {
                        navController.navigate(Screen.Breeding.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToSettings = { navController.navigate(Screen.FarmSettings.route) },
                    onNavigateToSummary = {
                        navController.navigate(Screen.HerdSummary.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Screen.Breeding.route) {
                BreedingScreen(
                    onAnimalClick = { id -> navController.navigate(Screen.AnimalDetail.route(id)) },
                    onNavigateToProfiles = {
                        navController.navigate(Screen.HerdList.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Screen.AddAnimal.route) {
                AddAnimalScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(
                route = Screen.AnimalDetail.route,
                arguments = listOf(navArgument("animalId") { type = NavType.StringType })
            ) { backStackEntry ->
                val animalId = checkNotNull(backStackEntry.arguments?.getString("animalId"))
                AnimalDetailScreen(
                    animalId = animalId,
                    onNavigateBack = { navController.popBackStack() },
                    onEditAnimal = { navController.navigate(Screen.EditAnimal.route(animalId)) },
                    onAnimalDeleted = { navController.popBackStack() },
                    viewModel = androidx.hilt.navigation.compose.hiltViewModel(backStackEntry)
                )
            }
            composable(Screen.FarmSettings.route) {
                FarmSettingsScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.HerdSummary.route) {
                HerdSummaryScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSettings = {
                        navController.navigate(Screen.FarmSettings.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(
                route = Screen.EditAnimal.route,
                arguments = listOf(navArgument("animalId") { type = NavType.StringType })
            ) { backStackEntry ->
                val animalId = checkNotNull(backStackEntry.arguments?.getString("animalId"))
                EditAnimalScreen(
                    animalId = animalId,
                    onNavigateBack = { navController.popBackStack() },
                    viewModel = androidx.hilt.navigation.compose.hiltViewModel(backStackEntry)
                )
            }
        }
    }
    }
}
