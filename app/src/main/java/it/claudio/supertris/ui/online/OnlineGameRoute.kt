package it.claudio.supertris.ui.online

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.claudio.supertris.R
import it.claudio.supertris.ui.game.GameScreen
import it.claudio.supertris.ui.vm.EmojiEvent
import it.claudio.supertris.ui.vm.OnlineViewModel
import kotlinx.coroutines.delay

@Composable
fun OnlineGameRoute(
    vm: OnlineViewModel,
    onExitToMenu: () -> Unit,
) {
    val ui by vm.gameUi.collectAsState()
    val opponentOffline by vm.opponentOffline.collectAsState()
    val roomGone by vm.roomGone.collectAsState()
    val incomingEmoji by vm.incomingEmoji.collectAsState()

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
            belowBoard = { EmojiBar(onSend = vm::sendEmoji) },
        )

        IncomingEmojiOverlay(
            event = incomingEmoji,
            modifier = Modifier.align(Alignment.Center),
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

/** Fila di reazioni rapide da mandare all'avversario. */
@Composable
private fun EmojiBar(onSend: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        OnlineViewModel.ALLOWED_EMOJI.forEach { emoji ->
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
                        shape = CircleShape,
                    )
                    .clickable { onSend(emoji) },
                contentAlignment = Alignment.Center,
            ) {
                Text(text = emoji, fontSize = 20.sp)
            }
        }
    }
}

/** L'emoji ricevuta esplode al centro e svanisce. */
@Composable
private fun IncomingEmojiOverlay(
    event: EmojiEvent?,
    modifier: Modifier = Modifier,
) {
    var visible by remember { mutableStateOf<EmojiEvent?>(null) }
    val scale = remember { Animatable(0f) }

    LaunchedEffect(event) {
        if (event == null) return@LaunchedEffect
        visible = event
        scale.snapTo(0.3f)
        scale.animateTo(1f, animationSpec = tween(260, easing = FastOutSlowInEasing))
        delay(1_400)
        scale.animateTo(0f, animationSpec = tween(200))
        visible = null
    }

    val shown = visible
    if (shown != null && scale.value > 0.01f) {
        Text(
            modifier = modifier
                .graphicsLayer(scaleX = scale.value, scaleY = scale.value)
                .alpha(scale.value.coerceIn(0f, 1f)),
            text = shown.emoji,
            fontSize = 72.sp,
        )
    }
}

