package it.claudio.supertris.ui.online

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.claudio.supertris.R
import it.claudio.supertris.ui.game.GameScreen
import it.claudio.supertris.ui.vm.OnlineViewModel

@Composable
fun OnlineGameRoute(
    vm: OnlineViewModel,
    onExitToMenu: () -> Unit,
) {
    val ui by vm.gameUi.collectAsState()
    val opponentOffline by vm.opponentOffline.collectAsState()
    val roomGone by vm.roomGone.collectAsState()

    val exitToMenu = {
        vm.exitGame()
        onExitToMenu()
    }
    BackHandler { exitToMenu() }

    Box(modifier = Modifier.fillMaxSize()) {
        GameScreen(
            ui = ui,
            onTap = vm::onTap,
            onBackToMenu = exitToMenu,
            onNewGame = { vm.requestRematch() },
            newGameLabel = stringResource(id = R.string.azione_rivincita),
        )

        // Banner "avversario offline" sopra la barra info.
        AnimatedVisibility(
            visible = opponentOffline && !roomGone,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .safeDrawingPadding(),
        ) {
            Row(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        shape = RoundedCornerShape(16.dp),
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                )
                Text(
                    modifier = Modifier.padding(start = 8.dp),
                    text = stringResource(id = R.string.online_avversario_offline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                )
            }
        }
    }

    if (roomGone) {
        AlertDialog(
            onDismissRequest = { /* scelta esplicita richiesta */ },
            title = { Text(stringResource(id = R.string.online_dialog_room_gone_titolo)) },
            text = { Text(stringResource(id = R.string.online_err_room_gone)) },
            confirmButton = {
                Button(onClick = exitToMenu) {
                    Text(stringResource(id = R.string.azione_menu))
                }
            },
        )
    }
}
