package it.claudio.supertris.ui.nearby

import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import it.claudio.supertris.R
import it.claudio.supertris.ui.game.GameScreen
import it.claudio.supertris.ui.vm.NearbyViewModel

@Composable
fun NearbyGameRoute(
    vm: NearbyViewModel,
    onExitToMenu: () -> Unit,
) {
    val ui by vm.gameUi.collectAsState()
    val opponentLeft by vm.opponentLeft.collectAsState()

    val quitToMenu = {
        vm.quitGame()
        onExitToMenu()
    }
    BackHandler { quitToMenu() }

    GameScreen(
        ui = ui,
        onTap = vm::onTap,
        onBackToMenu = quitToMenu,
        onNewGame = { vm.requestRematch() },
        newGameLabel = stringResource(id = R.string.azione_rivincita),
    )

    if (opponentLeft) {
        AlertDialog(
            onDismissRequest = { /* scelta esplicita richiesta */ },
            title = { Text(stringResource(id = R.string.disconnesso_titolo)) },
            text = { Text(stringResource(id = R.string.disconnesso_testo)) },
            confirmButton = {
                Button(onClick = quitToMenu) {
                    Text(stringResource(id = R.string.azione_menu))
                }
            },
        )
    }
}
