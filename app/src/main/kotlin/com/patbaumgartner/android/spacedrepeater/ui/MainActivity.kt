package com.patbaumgartner.android.spacedrepeater.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.patbaumgartner.android.spacedrepeater.ui.screens.HomeScreen
import com.patbaumgartner.android.spacedrepeater.ui.screens.ImportScreen
import com.patbaumgartner.android.spacedrepeater.ui.screens.SettingsScreen
import com.patbaumgartner.android.spacedrepeater.ui.screens.StatsScreen
import com.patbaumgartner.android.spacedrepeater.ui.screens.StatsViewModel
import com.patbaumgartner.android.spacedrepeater.ui.screens.StudyScreen
import com.patbaumgartner.android.spacedrepeater.ui.screens.StudyViewModel
import com.patbaumgartner.android.spacedrepeater.ui.theme.SpacedRepeaterTheme
import dagger.hilt.android.AndroidEntryPoint

object Routes {
    const val HOME = "home"
    const val IMPORT = "import"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val STUDY = "study/{deckId}"
    fun study(deckId: Long) = "study/$deckId"
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            SpacedRepeaterTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val context = LocalContext.current
                    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission(),
                    ) { /* Granting is optional; reminders simply do not post (UC-07 3a). */ }
                    LaunchedEffect(Unit) {
                        @Suppress("NewApi") // TIRAMISU is API 31; the check guards a permissions request, not a feature check.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                            android.content.pm.PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = Routes.HOME) {
                        composable(Routes.HOME) {
                            HomeScreen(
                                viewModel = viewModel(),
                                onOpenDeck = { id -> navController.navigate(Routes.study(id)) },
                                onImport = { navController.navigate(Routes.IMPORT) },
                                onStats = { navController.navigate(Routes.STATS) },
                                onSettings = { navController.navigate(Routes.SETTINGS) },
                            )
                        }
                        composable(Routes.SETTINGS) {
                            SettingsScreen(
                                onBack = { navController.popBackStack() },
                                onLicenses = { navController.popBackStack() },
                            )
                        }
                        composable(Routes.IMPORT) {
                            ImportScreen(
                                viewModel = viewModel(),
                                onBack = { navController.popBackStack() },
                                onImported = { navController.popBackStack() },
                            )
                        }
                        composable(Routes.STATS) {
                            StatsScreen(
                                viewModel = viewModel(),
                                onBack = { navController.popBackStack() },
                            )
                        }
                        composable(Routes.STUDY) { backStackEntry ->
                            val deckId = backStackEntry.arguments?.getString("deckId")?.toLongOrNull() ?: 0L
                            StudyScreen(
                                deckId = deckId,
                                viewModel = viewModel(),
                                onExit = { navController.popBackStack() },
                            )
                        }
                    }
                }
            }
        }
    }
}
