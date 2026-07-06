package it.claudio.supertris.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import android.os.Build
import it.claudio.supertris.SuperTrisApplication
import it.claudio.supertris.core.Difficulty
import it.claudio.supertris.core.GameMode
import it.claudio.supertris.ui.difficulty.DifficultyScreen
import it.claudio.supertris.ui.game.GameRoute
import it.claudio.supertris.ui.menu.MenuScreen
import it.claudio.supertris.ui.nearby.NearbyGameRoute
import it.claudio.supertris.ui.nearby.NearbyLobbyScreen
import it.claudio.supertris.ui.online.OnlineGameRoute
import it.claudio.supertris.ui.online.OnlineLobbyScreen
import it.claudio.supertris.ui.rules.RulesScreen
import it.claudio.supertris.ui.twoplayers.TwoPlayersScreen
import it.claudio.supertris.ui.vm.GameSessionViewModel
import it.claudio.supertris.ui.vm.MenuViewModel
import it.claudio.supertris.ui.vm.NearbyViewModel
import it.claudio.supertris.ui.vm.OnlineViewModel

private object Routes {
    const val MENU = "menu"
    const val DIFFICULTY = "difficulty"
    const val GAME = "game"
    const val TWO_PLAYERS = "two_players"
    const val NEARBY_LOBBY = "nearby_lobby"
    const val NEARBY_GAME = "nearby_game"
    const val ONLINE_LOBBY = "online_lobby"
    const val ONLINE_GAME = "online_game"
    const val RULES = "rules"
}

@Composable
fun SuperTrisApp() {
    val navController = rememberNavController()
    val app = LocalContext.current.applicationContext as SuperTrisApplication

    val menuVm: MenuViewModel = viewModel(factory = MenuViewModel.Factory(app.gameRepository))
    val gameVm: GameSessionViewModel = viewModel(factory = GameSessionViewModel.Factory(app.gameRepository))
    val nearbyVm: NearbyViewModel = viewModel(
        factory = NearbyViewModel.Factory(app.nearbyTransport, Build.MODEL ?: "Android"),
    )
    val onlineVm: OnlineViewModel = viewModel(
        factory = OnlineViewModel.Factory(app.gameRepository, app.onlineRoomClient),
    )

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
                onGiocaIn2 = { navController.navigate(Routes.TWO_PLAYERS) },
                onComeSiGioca = { navController.navigate(Routes.RULES) },
                onEsci = { /* gestito dentro la schermata */ },
            )
        }
        composable(Routes.RULES) {
            RulesScreen(onBack = { navController.popBackStack() })
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
            GameRoute(
                vm = gameVm,
                onBackToMenu = { goToMenu() },
                onNewGame = {
                    // "Nuova partita" torna alla scelta coerente con la modalita'
                    // appena giocata: difficolta' AI oppure scelta 2 giocatori.
                    if (gameVm.uiState.value.gameMode == GameMode.PASS_AND_PLAY) {
                        navController.navigate(Routes.TWO_PLAYERS) {
                            popUpTo(Routes.MENU)
                            launchSingleTop = true
                        }
                    } else {
                        goToDifficulty()
                    }
                },
            )
        }
        composable(Routes.TWO_PLAYERS) {
            TwoPlayersScreen(
                onBack = { navController.popBackStack() },
                onPassAndPlay = {
                    gameVm.startNewGame(Difficulty.FACILE, GameMode.PASS_AND_PLAY)
                    goToGame()
                },
                onNearby = { navController.navigate(Routes.NEARBY_LOBBY) },
                onOnline = { navController.navigate(Routes.ONLINE_LOBBY) },
                onlineEnabled = app.onlineAvailable,
            )
        }
        composable(Routes.ONLINE_LOBBY) {
            OnlineLobbyScreen(
                vm = onlineVm,
                onBack = { navController.popBackStack() },
                onGameReady = {
                    navController.navigate(Routes.ONLINE_GAME) {
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(Routes.ONLINE_GAME) {
            OnlineGameRoute(
                vm = onlineVm,
                onExitToMenu = { goToMenu() },
            )
        }
        composable(Routes.NEARBY_LOBBY) {
            NearbyLobbyScreen(
                vm = nearbyVm,
                onBack = { navController.popBackStack() },
                onGameReady = {
                    navController.navigate(Routes.NEARBY_GAME) {
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(Routes.NEARBY_GAME) {
            NearbyGameRoute(
                vm = nearbyVm,
                onExitToMenu = { goToMenu() },
            )
        }
    }
}