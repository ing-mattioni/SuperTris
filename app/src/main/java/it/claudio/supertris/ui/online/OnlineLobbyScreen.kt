package it.claudio.supertris.ui.online

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import it.claudio.supertris.R
import it.claudio.supertris.net.GameSummary
import it.claudio.supertris.net.GameSummaryStatus
import it.claudio.supertris.ui.components.BrandLockup
import it.claudio.supertris.ui.components.SuperBackground
import it.claudio.supertris.ui.vm.OnlineFailure
import it.claudio.supertris.ui.vm.OnlineLobbyState
import it.claudio.supertris.ui.vm.OnlineViewModel

@Composable
fun OnlineLobbyScreen(
    vm: OnlineViewModel,
    onBack: () -> Unit,
    onGameReady: () -> Unit,
) {
    val state by vm.lobbyState.collectAsState()
    val savedNickname by vm.nickname.collectAsState()
    val myGames by vm.myGames.collectAsState()
    val ctx = LocalContext.current

    var nicknameField by rememberSaveable { mutableStateOf("") }
    var nicknameLoaded by rememberSaveable { mutableStateOf(false) }
    var codeField by rememberSaveable { mutableStateOf("") }
    var notifPermissionAsked by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(Unit) {
        vm.startGamesListener()
        onDispose { vm.stopGamesListener() }
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    // Con almeno una partita attiva chiediamo (una volta sola) il permesso
    // notifiche, cosi' il worker puo' avvisare "e' il tuo turno".
    LaunchedEffect(myGames) {
        if (Build.VERSION.SDK_INT >= 33 && myGames.isNotEmpty() && !notifPermissionAsked) {
            val granted = ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notifPermissionAsked = true
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(savedNickname) {
        if (!nicknameLoaded && savedNickname.isNotBlank()) {
            nicknameField = savedNickname
            nicknameLoaded = true
        }
    }

    LaunchedEffect(state) {
        if (state is OnlineLobbyState.InGame) onGameReady()
    }

    val goBack = {
        if (state is OnlineLobbyState.WaitingGuest) vm.cancelWaiting() else vm.resetToIdle()
        onBack()
    }
    BackHandler { goBack() }

    SuperBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BrandLockup(showTagline = false)
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = stringResource(id = R.string.two_players_online),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(18.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.20f),
                        shape = RoundedCornerShape(28.dp),
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(28.dp),
                    )
                    .padding(16.dp),
            ) {
                when (val st = state) {
                    OnlineLobbyState.Idle, OnlineLobbyState.InGame -> IdleContent(
                        nickname = nicknameField,
                        onNicknameChange = { nicknameField = it; nicknameLoaded = true },
                        code = codeField,
                        onCodeChange = { codeField = it.uppercase() },
                        games = myGames,
                        onOpenGame = { vm.openGame(it.code) },
                        onCreate = { vm.createRoom(nicknameField) },
                        onJoin = { vm.joinRoom(codeField, nicknameField) },
                        onBack = goBack,
                    )

                    OnlineLobbyState.Working -> WaitingContent(
                        text = stringResource(id = R.string.online_lavoro),
                        onCancel = { vm.resetToIdle() },
                    )

                    is OnlineLobbyState.WaitingGuest -> WaitingGuestContent(
                        code = st.code,
                        onShare = {
                            val text = ctx.getString(R.string.online_condividi_testo, st.code)
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            ctx.startActivity(Intent.createChooser(send, null))
                        },
                        onCancel = { vm.cancelWaiting() },
                    )

                    is OnlineLobbyState.Failed -> FailedContent(
                        reason = st.reason,
                        onRetry = { vm.resetToIdle() },
                        onBack = goBack,
                    )
                }
            }
        }
    }
}

@Composable
private fun IdleContent(
    nickname: String,
    onNicknameChange: (String) -> Unit,
    code: String,
    onCodeChange: (String) -> Unit,
    games: List<GameSummary>,
    onOpenGame: (GameSummary) -> Unit,
    onCreate: () -> Unit,
    onJoin: () -> Unit,
    onBack: () -> Unit,
) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = nickname,
        onValueChange = { if (it.length <= 20) onNicknameChange(it) },
        singleLine = true,
        label = { Text(stringResource(id = R.string.online_nickname_label)) },
    )

    Spacer(modifier = Modifier.height(14.dp))

    if (games.isNotEmpty()) {
        Text(
            text = stringResource(id = R.string.online_mie_partite),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        games.forEach { game ->
            GameSummaryCard(game = game, onClick = { onOpenGame(game) })
            Spacer(modifier = Modifier.height(8.dp))
        }
        Spacer(modifier = Modifier.height(10.dp))
    }

    Button(
        modifier = Modifier.fillMaxWidth(),
        enabled = nickname.isNotBlank(),
        onClick = onCreate,
    ) { Text(stringResource(id = R.string.online_crea)) }
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        text = stringResource(id = R.string.online_crea_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.height(18.dp))

    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = code,
        onValueChange = { if (it.length <= 5) onCodeChange(it) },
        singleLine = true,
        label = { Text(stringResource(id = R.string.online_codice_label)) },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
    )
    Spacer(modifier = Modifier.height(8.dp))
    Button(
        modifier = Modifier.fillMaxWidth(),
        enabled = nickname.isNotBlank() && code.length == 5,
        onClick = onJoin,
    ) { Text(stringResource(id = R.string.online_unisciti)) }

    Spacer(modifier = Modifier.height(18.dp))
    Button(
        modifier = Modifier.fillMaxWidth(),
        onClick = onBack,
    ) { Text(stringResource(id = R.string.azione_indietro)) }
}

@Composable
private fun GameSummaryCard(
    game: GameSummary,
    onClick: () -> Unit,
) {
    val (statusText, statusColor) = when (game.status) {
        GameSummaryStatus.YOUR_TURN ->
            stringResource(id = R.string.online_stato_tuo_turno) to MaterialTheme.colorScheme.primary
        GameSummaryStatus.THEIR_TURN ->
            stringResource(id = R.string.online_stato_turno_avversario) to MaterialTheme.colorScheme.secondary
        GameSummaryStatus.WAITING_GUEST ->
            stringResource(id = R.string.online_stato_in_attesa_sfidante) to
                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        GameSummaryStatus.FINISHED ->
            stringResource(id = R.string.online_stato_finita) to
                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
    }
    val title = game.opponentName
        ?.let { stringResource(id = R.string.online_vs, it) }
        ?: stringResource(id = R.string.online_partita_codice, game.code)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.28f), RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                color = if (game.status == GameSummaryStatus.YOUR_TURN) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
                },
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = statusColor,
            )
            Text(
                text = game.code,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
            )
        }
    }
}

@Composable
private fun WaitingGuestContent(
    code: String,
    onShare: () -> Unit,
    onCancel: () -> Unit,
) {
    Text(
        modifier = Modifier.fillMaxWidth(),
        text = code,
        style = MaterialTheme.typography.displaySmall,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Bold,
        letterSpacing = 8.sp,
    )
    Spacer(modifier = Modifier.height(10.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
        )
        Text(
            modifier = Modifier.padding(start = 10.dp),
            text = stringResource(id = R.string.online_in_attesa),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
        )
    }
    Spacer(modifier = Modifier.height(16.dp))
    Button(
        modifier = Modifier.fillMaxWidth(),
        onClick = onShare,
    ) { Text(stringResource(id = R.string.online_codice_condividi)) }
    Spacer(modifier = Modifier.height(10.dp))
    Button(
        modifier = Modifier.fillMaxWidth(),
        onClick = onCancel,
    ) { Text(stringResource(id = R.string.azione_annulla)) }
}

@Composable
private fun WaitingContent(
    text: String,
    onCancel: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            strokeWidth = 2.dp,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
        )
    }
    Spacer(modifier = Modifier.height(16.dp))
    Button(
        modifier = Modifier.fillMaxWidth(),
        onClick = onCancel,
    ) { Text(stringResource(id = R.string.azione_annulla)) }
}

@Composable
private fun FailedContent(
    reason: OnlineFailure,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    val message = when (reason) {
        OnlineFailure.NOT_FOUND -> stringResource(id = R.string.online_err_not_found)
        OnlineFailure.FULL -> stringResource(id = R.string.online_err_full)
        OnlineFailure.VERSION -> stringResource(id = R.string.online_err_version)
        OnlineFailure.NETWORK -> stringResource(id = R.string.online_err_network)
        OnlineFailure.ROOM_GONE -> stringResource(id = R.string.online_err_room_gone)
    }
    Text(
        text = message,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
    )
    Spacer(modifier = Modifier.height(16.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Button(
            modifier = Modifier.weight(1f),
            onClick = onBack,
        ) { Text(stringResource(id = R.string.azione_indietro)) }
        Button(
            modifier = Modifier.weight(1f),
            onClick = onRetry,
        ) { Text(stringResource(id = R.string.lobby_riprova)) }
    }
}
