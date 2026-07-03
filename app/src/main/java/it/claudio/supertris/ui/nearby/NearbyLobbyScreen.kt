package it.claudio.supertris.ui.nearby

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import it.claudio.supertris.R
import it.claudio.supertris.net.NearbyPermissions
import it.claudio.supertris.ui.components.BrandLockup
import it.claudio.supertris.ui.components.SuperBackground
import it.claudio.supertris.ui.vm.LobbyEndpoint
import it.claudio.supertris.ui.vm.LobbyFailure
import it.claudio.supertris.ui.vm.LobbyState
import it.claudio.supertris.ui.vm.NearbyViewModel

@Composable
fun NearbyLobbyScreen(
    vm: NearbyViewModel,
    onBack: () -> Unit,
    onGameReady: () -> Unit,
) {
    val state by vm.lobbyState.collectAsState()
    val savedNickname by vm.savedNickname.collectAsState()
    val ctx = LocalContext.current

    var nicknameField by remember { mutableStateOf("") }
    var nicknameLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(savedNickname) {
        if (!nicknameLoaded && savedNickname.isNotBlank()) {
            nicknameField = savedNickname
            nicknameLoaded = true
        }
    }

    var permissionsGranted by remember { mutableStateOf(NearbyPermissions.allGranted(ctx)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        permissionsGranted = NearbyPermissions.allGranted(ctx)
    }

    LaunchedEffect(state) {
        if (state is LobbyState.InGame) onGameReady()
    }

    val leaveAndGoBack = {
        vm.leaveLobby()
        onBack()
    }
    BackHandler { leaveAndGoBack() }

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
                text = stringResource(id = R.string.two_players_due_telefoni),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(18.dp))

            LobbyPanel {
                when {
                    !permissionsGranted -> PermissionContent(
                        onRequest = { permissionLauncher.launch(NearbyPermissions.required()) },
                        onBack = leaveAndGoBack,
                    )

                    else -> when (val st = state) {
                        LobbyState.Idle -> RoleChoiceContent(
                            nickname = nicknameField,
                            onNicknameChange = { nicknameField = it; nicknameLoaded = true },
                            onHost = { vm.startHosting(nicknameField) },
                            onJoin = { vm.startJoining(nicknameField) },
                            onBack = leaveAndGoBack,
                        )

                        LobbyState.Advertising -> WaitingContent(
                            text = stringResource(id = R.string.lobby_in_attesa),
                            onCancel = { vm.leaveLobby() },
                        )

                        is LobbyState.Discovering -> DiscoveringContent(
                            endpoints = st.endpoints,
                            onSelect = { vm.connectTo(it) },
                            onCancel = { vm.leaveLobby() },
                        )

                        is LobbyState.AuthConfirm -> AuthConfirmContent(
                            peerName = st.peerName,
                            digits = st.authDigits,
                            onAccept = { vm.confirmAuth() },
                            onReject = { vm.rejectAuth() },
                        )

                        LobbyState.Connecting -> WaitingContent(
                            text = stringResource(id = R.string.lobby_connessione),
                            onCancel = { vm.leaveLobby() },
                        )

                        LobbyState.InGame -> WaitingContent(
                            text = stringResource(id = R.string.lobby_connessione),
                            onCancel = { vm.quitGame() },
                        )

                        is LobbyState.Failed -> FailedContent(
                            reason = st.reason,
                            onRetry = { vm.leaveLobby() },
                            onBack = leaveAndGoBack,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LobbyPanel(content: @Composable () -> Unit) {
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
        content()
    }
}

@Composable
private fun PermissionContent(
    onRequest: () -> Unit,
    onBack: () -> Unit,
) {
    Text(
        text = stringResource(id = R.string.lobby_permessi_testo),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
    )
    Spacer(modifier = Modifier.height(16.dp))
    Button(
        modifier = Modifier.fillMaxWidth(),
        onClick = onRequest,
    ) { Text(stringResource(id = R.string.lobby_permessi_bottone)) }
    Spacer(modifier = Modifier.height(10.dp))
    Button(
        modifier = Modifier.fillMaxWidth(),
        onClick = onBack,
    ) { Text(stringResource(id = R.string.azione_indietro)) }
}

@Composable
private fun RoleChoiceContent(
    nickname: String,
    onNicknameChange: (String) -> Unit,
    onHost: () -> Unit,
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
    LobbyCard(
        title = stringResource(id = R.string.lobby_crea),
        subtitle = stringResource(id = R.string.lobby_crea_hint),
        onClick = onHost,
    )
    Spacer(modifier = Modifier.height(10.dp))
    LobbyCard(
        title = stringResource(id = R.string.lobby_cerca),
        subtitle = stringResource(id = R.string.lobby_cerca_hint),
        onClick = onJoin,
    )
    Spacer(modifier = Modifier.height(18.dp))
    Button(
        modifier = Modifier.fillMaxWidth(),
        onClick = onBack,
    ) { Text(stringResource(id = R.string.azione_indietro)) }
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
private fun DiscoveringContent(
    endpoints: List<LobbyEndpoint>,
    onSelect: (LobbyEndpoint) -> Unit,
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
            text = stringResource(id = R.string.lobby_ricerca),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
        )
    }
    Spacer(modifier = Modifier.height(14.dp))

    if (endpoints.isEmpty()) {
        Text(
            text = stringResource(id = R.string.lobby_nessuna_partita),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        )
    } else {
        endpoints.forEach { endpoint ->
            LobbyCard(
                title = endpoint.name,
                subtitle = stringResource(id = R.string.lobby_tocca_per_unirti),
                onClick = { onSelect(endpoint) },
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }

    Spacer(modifier = Modifier.height(14.dp))
    Button(
        modifier = Modifier.fillMaxWidth(),
        onClick = onCancel,
    ) { Text(stringResource(id = R.string.azione_annulla)) }
}

@Composable
private fun AuthConfirmContent(
    peerName: String,
    digits: String,
    onAccept: () -> Unit,
    onReject: () -> Unit,
) {
    Text(
        text = stringResource(id = R.string.lobby_connessione_con, peerName),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        modifier = Modifier.fillMaxWidth(),
        text = digits,
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Bold,
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = stringResource(id = R.string.lobby_codice_hint),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
    )
    Spacer(modifier = Modifier.height(16.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Button(
            modifier = Modifier.weight(1f),
            onClick = onReject,
        ) { Text(stringResource(id = R.string.lobby_rifiuta)) }
        Button(
            modifier = Modifier.weight(1f),
            onClick = onAccept,
        ) { Text(stringResource(id = R.string.lobby_accetta)) }
    }
}

@Composable
private fun FailedContent(
    reason: LobbyFailure,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    val message = when (reason) {
        LobbyFailure.CONNECTION -> stringResource(id = R.string.lobby_connessione_fallita)
        LobbyFailure.VERSION -> stringResource(id = R.string.lobby_versione_incompatibile)
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

@Composable
private fun LobbyCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.28f), RoundedCornerShape(18.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
        )
    }
}
