package it.claudio.supertris.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import it.claudio.supertris.SuperTrisApplication
import it.claudio.supertris.ui.difficulty.DifficultyScreen
import it.claudio.supertris.ui.game.GameScreen
import it.claudio.supertris.ui.menu.MenuScreen
import it.claudio.supertris.ui.vm.GameSessionViewModel
import it.claudio.supertris.ui.vm.MenuViewModel

private object Routes {
    const val MENU = "menu"
    const val DIFFICULTY = "difficulty"
    const val GAME = "game"
}

@Composable
fun SuperTrisApp() {
    val navController = rememberNavController()
    val app = LocalContext.current.applicationContext as SuperTrisApplication

    val menuVm: MenuViewModel = viewModel(factory = MenuViewModel.Factory(app.gameRepository))
    val gameVm: GameSessionViewModel = viewModel(factory = GameSessionViewModel.Factory(app.gameRepository))

    val goToMenu = remember {
        {
            navController.navigate(Routes.MENU) {
                popUpTo(Routes.MENU) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    val goToDifficulty = remember { { navController.navigate(Routes.DIFFICULTY) } }

    val goToGame = remember {
        {
            navController.navigate(Routes.GAME) {
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.MENU,
    ) {
        composable(Routes.MENU) {
            MenuScreen(
                vm = menuVm,
                onNuovaPartita = { goToDifficulty() },
                onContinua = { goToGame() },
                onEsci = { /* gestito dentro la schermata */ },
            )
        }
        composable(Routes.DIFFICULTY) {
            DifficultyScreen(
                onBack = { navController.popBackStack() },
                onDifficultyChosen = { diff ->
                    gameVm.startNewGame(diff)
                    goToGame()
                },
            )
        }
        composable(Routes.GAME) {
            GameScreen(
                vm = gameVm,
                onBackToMenu = { goToMenu() },
                onNewGame = { goToDifficulty() },
            )
        }
    }
}