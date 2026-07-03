package it.claudio.supertris.ui.online

import android.content.Intent
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.claudio.supertris.R
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
    val resumeCode by vm.resumeCode.collectAsState()
    val ctx = LocalContext.current

    var nicknameField by rememberSaveable { mutableStateOf("") }
    var nicknameLoaded by rememberSaveable { mutableStateOf(false) }
    var codeField by rememberSaveable { mutableStateOf("") }

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
                        resumeCode = resumeCode,
                        onCreate = { vm.createRoom(nicknameField) },
                        onJoin = { vm.joinRoom(codeField, nicknameField) },
                        onResume = { vm.resumeRoom() },
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
    resumeCode: String?,
    onCreate: () -> Unit,
    onJoin: () -> Unit,
    onResume: () -> Unit,
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

    if (resumeCode != null) {
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onResume,
        ) { Text(stringResource(id = R.string.online_riprendi, resumeCode)) }
        Spacer(modifier = Modifier.height(14.dp))
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
