package it.claudio.supertris.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import it.claudio.supertris.R
import it.claudio.supertris.core.GameMode
import it.claudio.supertris.core.SuperTrisRules
import it.claudio.supertris.core.isRemote
import it.claudio.supertris.ui.components.MarkGlyph
import it.claudio.supertris.ui.components.SuperBackground
import it.claudio.supertris.ui.vm.GameSessionViewModel
import it.claudio.supertris.ui.vm.GameUiState
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private fun markSymbol(mark: Int): String = if (mark == SuperTrisRules.X) "X" else "O"

@Composable
fun GameRoute(
    vm: GameSessionViewModel,
    onBackToMenu: () -> Unit,
    onNewGame: () -> Unit,
) {
    val ui by vm.uiState.collectAsState()

    DisposableEffect(Unit) {
        vm.setGameVisible(true)
        onDispose { vm.setGameVisible(false) }
    }

    GameScreen(
        ui = ui,
        onTap = vm::onHumanTap,
        onBackToMenu = onBackToMenu,
        onNewGame = onNewGame,
    )
}

@Composable
fun GameScreen(
    ui: GameUiState,
    onTap: (micro: Int, cell: Int) -> Unit,
    onBackToMenu: () -> Unit,
    onNewGame: () -> Unit,
    newGameLabel: String = stringResource(id = R.string.azione_nuova_partita),
    belowBoard: (@Composable () -> Unit)? = null,
) {
    val winnerMark = when (ui.macroStatus) {
        SuperTrisRules.STATUS_X -> SuperTrisRules.X
        SuperTrisRules.STATUS_O -> SuperTrisRules.O
        else -> SuperTrisRules.EMPTY
    }
    val localWon = winnerMark != SuperTrisRules.EMPTY && winnerMark == ui.humanMark
    val headline = when {
        winnerMark == SuperTrisRules.EMPTY -> stringResource(R.string.fine_partita_pareggio)
        ui.gameMode == GameMode.PASS_AND_PLAY -> stringResource(R.string.fine_partita_vince, markSymbol(winnerMark))
        localWon -> stringResource(R.string.fine_partita_vittoria)
        else -> stringResource(R.string.fine_partita_sconfitta)
    }
    val subtitle = when {
        ui.waitingRematch -> stringResource(R.string.rematch_in_attesa)
        winnerMark == SuperTrisRules.EMPTY -> stringResource(R.string.overlay_pareggio_finale)
        ui.gameMode == GameMode.PASS_AND_PLAY -> stringResource(R.string.overlay_vince_chiuso, markSymbol(winnerMark))
        localWon -> stringResource(R.string.overlay_vittoria_finale)
        ui.gameMode.isRemote -> stringResource(R.string.overlay_sconfitta_avversario)
        else -> stringResource(R.string.overlay_sconfitta_finale)
    }

    SuperBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
            ) {
                TopInfoBar(ui = ui)

                Spacer(modifier = Modifier.height(10.dp))

                MicroConquestBanner(
                    token = ui.microCelebrationToken,
                    mark = ui.lastResolvedMark,
                )

                Spacer(modifier = Modifier.height(12.dp))

                SuperBoard(
                    enabled = ui.isHumanTurn && !ui.isAiThinking && !ui.isGameOver,
                    forcedMicro = ui.forcedMicro,
                    cells = ui.cells,
                    microStatus = ui.microStatus,
                    celebrationToken = ui.microCelebrationToken,
                    resolvedMicro = ui.lastResolvedMicro,
                    resolvedMark = ui.lastResolvedMark,
                    onTap = onTap,
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (belowBoard != null) {
                    belowBoard()
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = onBackToMenu,
                    ) {
                        Text(stringResource(id = R.string.azione_menu))
                    }
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = onNewGame,
                    ) {
                        Text(newGameLabel)
                    }
                }
            }

            GameOverCelebrationOverlay(
                visible = ui.isGameOver,
                celebrationToken = ui.gameOverCelebrationToken,
                winnerMark = winnerMark,
                headline = headline,
                subtitle = subtitle,
                newGameLabel = newGameLabel,
                onBackToMenu = onBackToMenu,
                onNewGame = onNewGame,
            )
        }
    }
}

@Composable
private fun TopInfoBar(ui: GameUiState) {
    val isPassAndPlay = ui.gameMode == GameMode.PASS_AND_PLAY
    // In pass-and-play il colore segue chi deve giocare, altrimenti il simbolo locale.
    val referenceMark = if (isPassAndPlay) ui.turnMark else ui.humanMark
    val symbolColor = if (referenceMark == SuperTrisRules.X) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary

    val titleText = if (isPassAndPlay) {
        stringResource(id = R.string.gioco_due_giocatori)
    } else {
        stringResource(id = R.string.gioco_tuo_simbolo, markSymbol(ui.humanMark))
    }
    val turnText = when {
        isPassAndPlay -> stringResource(id = R.string.gioco_turno_di, markSymbol(ui.turnMark))
        ui.isHumanTurn -> stringResource(id = R.string.gioco_turno_tuo)
        ui.gameMode.isRemote -> stringResource(
            id = R.string.gioco_turno_di,
            ui.opponentName ?: stringResource(id = R.string.nearby_avversario),
        )
        else -> stringResource(id = R.string.gioco_turno_ai)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.30f),
                shape = RoundedCornerShape(24.dp),
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
                shape = RoundedCornerShape(24.dp),
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = titleText,
            color = symbolColor,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = turnText,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.88f),
                style = MaterialTheme.typography.bodyLarge,
            )

            AnimatedVisibility(visible = ui.isAiThinking) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                    Text(
                        modifier = Modifier.padding(start = 10.dp),
                        text = stringResource(id = R.string.gioco_ai_pensa),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun MicroConquestBanner(
    token: Int,
    mark: Int,
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(token, mark) {
        if (token <= 0 || mark == SuperTrisRules.EMPTY) {
            visible = false
            return@LaunchedEffect
        }

        visible = true
        delay(1_250)
        visible = false
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(220)) + slideInVertically(initialOffsetY = { -it / 2 }),
        exit = fadeOut(animationSpec = tween(180)) + slideOutVertically(targetOffsetY = { -it / 2 }),
    ) {
        val accent = if (mark == SuperTrisRules.X) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.28f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.54f),
                        ),
                    ),
                    shape = RoundedCornerShape(20.dp),
                )
                .border(
                    width = 1.dp,
                    color = accent.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(20.dp),
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(accent.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
                    .padding(5.dp),
                contentAlignment = Alignment.Center,
            ) {
                MarkGlyph(
                    mark = mark,
                    color = accent,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(id = R.string.label_conquista),
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(id = R.string.overlay_super_tris),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.74f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

// Riusata anche dal replay della cronologia (enabled = false).
@Composable
internal fun SuperBoard(
    enabled: Boolean,
    forcedMicro: Int,
    cells: IntArray,
    microStatus: IntArray,
    celebrationToken: Int,
    resolvedMicro: Int,
    resolvedMark: Int,
    onTap: (micro: Int, cell: Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.18f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.10f),
                    ),
                ),
                shape = RoundedCornerShape(24.dp),
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                shape = RoundedCornerShape(24.dp),
            )
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (mr in 0..2) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (mc in 0..2) {
                    val micro = mr * 3 + mc
                    MicroBoard(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        enabled = enabled && (forcedMicro == -1 || forcedMicro == micro),
                        highlighted = (forcedMicro == -1 && microStatus[micro] == SuperTrisRules.STATUS_IN_CORSO) || forcedMicro == micro,
                        micro = micro,
                        cells = cells,
                        status = microStatus[micro],
                        celebrationToken = celebrationToken,
                        resolvedMicro = resolvedMicro,
                        resolvedMark = resolvedMark,
                        onTap = onTap,
                    )
                }
            }
        }
    }
}

@Composable
private fun MicroBoard(
    modifier: Modifier,
    enabled: Boolean,
    highlighted: Boolean,
    micro: Int,
    cells: IntArray,
    status: Int,
    celebrationToken: Int,
    resolvedMicro: Int,
    resolvedMark: Int,
    onTap: (micro: Int, cell: Int) -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    val accent = when (status) {
        SuperTrisRules.STATUS_X -> MaterialTheme.colorScheme.tertiary
        SuperTrisRules.STATUS_O -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primary
    }
    val pulse = remember { Animatable(0f) }
    val isCelebrated = celebrationToken > 0 && resolvedMicro == micro && resolvedMark != SuperTrisRules.EMPTY

    LaunchedEffect(celebrationToken, isCelebrated) {
        if (!isCelebrated) {
            pulse.snapTo(0f)
            return@LaunchedEffect
        }

        pulse.snapTo(1f)
        pulse.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 950, easing = FastOutSlowInEasing),
        )
    }

    val borderColor by animateColorAsState(
        targetValue = when {
            isCelebrated -> accent.copy(alpha = 0.95f)
            highlighted -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        },
        label = "microBorder",
    )

    val bg = when {
        status == SuperTrisRules.STATUS_X -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
        status == SuperTrisRules.STATUS_O -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
        highlighted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.06f)
    }

    Box(
        modifier = modifier
            .graphicsLayer(
                scaleX = 1f + (pulse.value * 0.035f),
                scaleY = 1f + (pulse.value * 0.035f),
            )
            .background(bg, shape)
            .border(
                width = if (highlighted || isCelebrated) 2.dp else 1.dp,
                color = borderColor,
                shape = shape,
            )
            .alpha(if (enabled || status != SuperTrisRules.STATUS_IN_CORSO) 1f else 0.55f)
            .padding(6.dp),
    ) {
        if (pulse.value > 0f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRoundRect(
                    color = accent.copy(alpha = 0.14f * pulse.value),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                )
                drawRoundRect(
                    color = accent.copy(alpha = 0.45f * pulse.value),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx() * pulse.value.coerceAtLeast(0.5f)),
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            for (r in 0..2) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    for (c in 0..2) {
                        val cell = r * 3 + c
                        val idx = micro * 9 + cell
                        val mark = cells[idx]
                        Cell(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            enabled = enabled && status == SuperTrisRules.STATUS_IN_CORSO && mark == SuperTrisRules.EMPTY,
                            mark = mark,
                            onClick = { onTap(micro, cell) },
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = status != SuperTrisRules.STATUS_IN_CORSO,
            enter = fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.88f),
            exit = fadeOut(animationSpec = tween(180)),
        ) {
            val overlayMark = when (status) {
                SuperTrisRules.STATUS_X -> SuperTrisRules.X
                SuperTrisRules.STATUS_O -> SuperTrisRules.O
                else -> SuperTrisRules.EMPTY
            }
            val overlayColor = when (overlayMark) {
                SuperTrisRules.X -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.28f)
                SuperTrisRules.O -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.28f)
                else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.16f)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center,
            ) {
                if (overlayMark != SuperTrisRules.EMPTY) {
                    MarkGlyph(
                        mark = overlayMark,
                        color = overlayColor,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                    )
                } else {
                    Text(
                        text = stringResource(id = R.string.micro_pareggio),
                        color = overlayColor,
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun Cell(
    modifier: Modifier,
    enabled: Boolean,
    mark: Int,
    onClick: () -> Unit,
) {
    val color = when (mark) {
        SuperTrisRules.X -> MaterialTheme.colorScheme.tertiary
        SuperTrisRules.O -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f)
    }

    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (mark != SuperTrisRules.EMPTY) {
            MarkGlyph(
                mark = mark,
                color = color,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp),
            )
        }
    }
}

@Composable
private fun GameOverCelebrationOverlay(
    visible: Boolean,
    celebrationToken: Int,
    winnerMark: Int,
    headline: String,
    subtitle: String,
    newGameLabel: String,
    onBackToMenu: () -> Unit,
    onNewGame: () -> Unit,
) {
    val burst = remember { Animatable(0f) }
    val accent = when (winnerMark) {
        SuperTrisRules.X -> MaterialTheme.colorScheme.tertiary
        SuperTrisRules.O -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primary
    }

    LaunchedEffect(visible, celebrationToken) {
        if (!visible) {
            burst.snapTo(0f)
            return@LaunchedEffect
        }

        burst.snapTo(0f)
        burst.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1_050, easing = LinearOutSlowInEasing),
        )
    }

    AnimatedVisibility(
        modifier = Modifier.fillMaxSize(),
        visible = visible,
        enter = fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.94f, animationSpec = tween(320)),
        exit = fadeOut(animationSpec = tween(180)) + scaleOut(targetScale = 0.96f, animationSpec = tween(200)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.76f)),
        ) {
            CelebrationBurst(
                progress = burst.value,
                accent = accent,
                modifier = Modifier.fillMaxSize(),
            )

            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.94f),
                                ),
                            ),
                            shape = RoundedCornerShape(30.dp),
                        )
                        .border(
                            width = 1.dp,
                            color = accent.copy(alpha = 0.40f),
                            shape = RoundedCornerShape(30.dp),
                        )
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = if (winnerMark != SuperTrisRules.EMPTY) stringResource(id = R.string.overlay_super_tris) else stringResource(id = R.string.dialog_titolo_fine_partita),
                        color = accent,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = headline,
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    if (winnerMark != SuperTrisRules.EMPTY) {
                        Box(
                            modifier = Modifier
                                .size(156.dp)
                                .background(accent.copy(alpha = 0.10f), RoundedCornerShape(32.dp))
                                .border(
                                    width = 1.dp,
                                    color = accent.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(32.dp),
                                )
                                .padding(18.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            MarkGlyph(
                                mark = winnerMark,
                                color = accent,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = subtitle,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = onBackToMenu,
                        ) {
                            Text(stringResource(id = R.string.azione_menu))
                        }
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = onNewGame,
                        ) {
                            Text(newGameLabel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CelebrationBurst(
    progress: Float,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        if (progress <= 0f) return@Canvas

        val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height * 0.40f)
        val alpha = (1f - progress).coerceIn(0f, 1f)
        val baseRadius = size.minDimension * (0.16f + (progress * 0.18f))

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    accent.copy(alpha = 0.24f * alpha),
                    Color.Transparent,
                ),
                center = center,
                radius = baseRadius * 1.8f,
            ),
            radius = baseRadius * 1.8f,
            center = center,
        )

        repeat(14) { index ->
            val angle = ((PI * 2.0) / 14.0) * index.toDouble() - (PI / 2.0)
            val inner = baseRadius * 0.62f
            val outer = baseRadius * 1.22f
            val start = androidx.compose.ui.geometry.Offset(
                x = center.x + (cos(angle) * inner).toFloat(),
                y = center.y + (sin(angle) * inner).toFloat(),
            )
            val end = androidx.compose.ui.geometry.Offset(
                x = center.x + (cos(angle) * outer).toFloat(),
                y = center.y + (sin(angle) * outer).toFloat(),
            )
            drawLine(
                color = accent.copy(alpha = 0.36f * alpha),
                start = start,
                end = end,
                strokeWidth = size.minDimension * 0.012f,
                cap = StrokeCap.Round,
            )
        }

        drawCircle(
            color = accent.copy(alpha = 0.40f * alpha),
            radius = baseRadius,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = size.minDimension * 0.014f),
        )
    }
}
