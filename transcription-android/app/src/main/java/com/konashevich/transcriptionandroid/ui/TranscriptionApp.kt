package com.konashevich.transcriptionandroid.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.konashevich.transcriptionandroid.MainUiState
import com.konashevich.transcriptionandroid.MainViewModel
import com.konashevich.transcriptionandroid.UiEvent
import com.konashevich.transcriptionandroid.data.ImportedAudio
import com.konashevich.transcriptionandroid.data.ListenMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscriptionApp(
    state: MainUiState,
    viewModel: MainViewModel,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showAbout by rememberSaveable { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var importedExpanded by rememberSaveable(state.importedAudio?.file?.absolutePath) { mutableStateOf(false) }

    val recordPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted && state.settings.listenMode == ListenMode.TOGGLE) {
            viewModel.startRecording()
        }
    }

    val hasRecordPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO,
    ) == PackageManager.PERMISSION_GRANTED

    val openSessionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let(viewModel::importSessionFrom)
    }

    val saveSessionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        uri?.let(viewModel::exportSessionTo)
    }

    val openAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let {
            viewModel.importAudioFromUri(
                uri = it,
                sourceLabel = "Picked on device",
                autoTranscribe = false,
            )
        }
    }

    val importSettingsLauncher = rememberLauncherForActivityResult(
        contract = OpenDocumentFromInitialUri(downloadsInitialUri()),
    ) { uri ->
        uri?.let(viewModel::importSettingsFrom)
    }

    val saveImportedAudioLauncher = rememberLauncherForActivityResult(
        contract = CreateDocumentFromInitialUri(downloadsInitialUri()),
    ) { uri ->
        uri?.let(viewModel::exportImportedAudioTo)
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.Snackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    val ensureRecordPermission = {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.startRecording()
        } else {
            recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Listen & Polish", maxLines = 1)
                        Text(
                            text = state.settings.transcriptionService.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Open Session") },
                            onClick = {
                                menuExpanded = false
                                openSessionLauncher.launch(arrayOf("application/json"))
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Save Session") },
                            onClick = {
                                menuExpanded = false
                                saveSessionLauncher.launch(viewModel.suggestedSessionFileName())
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Open Audio") },
                            enabled = !state.isTranscribing && !state.isImportingAudio,
                            onClick = {
                                menuExpanded = false
                                openAudioLauncher.launch(arrayOf("audio/*"))
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("About") },
                            leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                showAbout = true
                            },
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            val wideLayout = maxWidth >= 900.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                state.importedAudio?.let { importedAudio ->
                    ImportedAudioCard(
                        importedAudio = importedAudio,
                        expanded = importedExpanded,
                        isBusy = state.isTranscribing || state.isImportingAudio,
                        onExpandedChange = { importedExpanded = it },
                        onTranscribe = viewModel::transcribeImportedAudio,
                        onSave = {
                            saveImportedAudioLauncher.launch(
                                CreateDocumentRequest(
                                    mimeType = importedAudio.mimeType,
                                    displayName = importedAudio.displayName,
                                ),
                            )
                        },
                        onClear = viewModel::clearImportedAudio,
                    )
                }

                if (wideLayout) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        RawEditorPanel(
                            modifier = Modifier.weight(1.15f),
                            state = state,
                            viewModel = viewModel,
                            hasRecordPermission = hasRecordPermission,
                            onEnsurePermission = ensureRecordPermission,
                            onRequestPermission = { recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                            clipboardText = { clipboardManager.setText(AnnotatedString(state.rawTextValue.text)) },
                        )
                        PolishedEditorPanel(
                            modifier = Modifier.weight(0.85f),
                            state = state,
                            viewModel = viewModel,
                            clipboardText = { clipboardManager.setText(AnnotatedString(state.polishedTextValue.text)) },
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        RawEditorPanel(
                            modifier = Modifier.weight(1.2f),
                            state = state,
                            viewModel = viewModel,
                            hasRecordPermission = hasRecordPermission,
                            onEnsurePermission = ensureRecordPermission,
                            onRequestPermission = { recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                            clipboardText = { clipboardManager.setText(AnnotatedString(state.rawTextValue.text)) },
                        )
                        PolishedEditorPanel(
                            modifier = Modifier.weight(0.8f),
                            state = state,
                            viewModel = viewModel,
                            clipboardText = { clipboardManager.setText(AnnotatedString(state.polishedTextValue.text)) },
                        )
                    }
                }
            }
        }
    }

    if (showSettings) {
        SettingsSheet(
            settings = state.settings,
            onDismiss = { showSettings = false },
            onThemeChanged = viewModel::updateThemeMode,
            onFontSizeChanged = viewModel::updateFontSize,
            onListenModeChanged = viewModel::updateListenMode,
            onTranscriptionServiceChanged = viewModel::updateTranscriptionService,
            onGeminiApiKeyChanged = viewModel::updateGeminiApiKey,
            onGeminiModelChanged = viewModel::updateGeminiModel,
            onPolishPromptChanged = viewModel::updatePolishPrompt,
            onServerSchemeChanged = viewModel::updateServerScheme,
            onServerHostChanged = viewModel::updateServerHost,
            onServerPortChanged = viewModel::updateServerPort,
            onServerPathChanged = viewModel::updateServerPath,
            onServerTimeoutChanged = viewModel::updateServerTimeoutSeconds,
            onImportSettings = {
                importSettingsLauncher.launch(arrayOf("application/json"))
            },
        )
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) {
                    Text("Close")
                }
            },
            title = { Text("About") },
            text = {
                Text(
                    "Android port of Listen & Polish with the same raw/polished workflow, " +
                        "Gemini polishing, Gemini or self-hosted transcription, and audio sharing from other apps.",
                )
            },
        )
    }
}

@Composable
private fun RawEditorPanel(
    modifier: Modifier,
    state: MainUiState,
    viewModel: MainViewModel,
    hasRecordPermission: Boolean,
    onEnsurePermission: () -> Unit,
    onRequestPermission: () -> Unit,
    clipboardText: () -> Unit,
) {
    EditorPanel(
        modifier = modifier,
        title = "Raw Transcription",
        value = state.rawTextValue,
        onValueChange = viewModel::updateRawText,
        fontSizeSp = state.settings.fontSize.editorSp,
        controls = {
            ListenControls(
                state = state,
                hasRecordPermission = hasRecordPermission,
                onStartRecording = onEnsurePermission,
                onRequestPermission = onRequestPermission,
                onStopRecording = viewModel::stopRecording,
                onToggleRecording = {
                    if (state.isRecording) {
                        viewModel.stopRecording()
                    } else {
                        onEnsurePermission()
                    }
                },
            )
            ActionIconButton(
                icon = Icons.Filled.AutoFixHigh,
                contentDescription = "Polish text",
                isBusy = state.isPolishing,
                onClick = viewModel::polishText,
            )
            ActionIconButton(
                icon = Icons.Filled.ContentCopy,
                contentDescription = "Copy raw transcription",
                onClick = clipboardText,
            )
            ActionIconButton(
                icon = Icons.Filled.Delete,
                contentDescription = "Clear raw transcription",
                onClick = viewModel::clearRawText,
            )
        },
    )
}

@Composable
private fun PolishedEditorPanel(
    modifier: Modifier,
    state: MainUiState,
    viewModel: MainViewModel,
    clipboardText: () -> Unit,
) {
    EditorPanel(
        modifier = modifier,
        title = "Polished Text",
        value = state.polishedTextValue,
        onValueChange = viewModel::updatePolishedText,
        fontSizeSp = state.settings.fontSize.editorSp,
        controls = {
            ActionIconButton(
                icon = Icons.Filled.ContentCopy,
                contentDescription = "Copy polished text",
                onClick = clipboardText,
            )
            ActionIconButton(
                icon = Icons.Filled.Delete,
                contentDescription = "Clear polished text",
                onClick = viewModel::clearPolishedText,
            )
            ActionIconButton(
                icon = Icons.Filled.DeleteSweep,
                contentDescription = "Clear both editors",
                onClick = viewModel::clearAllText,
            )
        },
    )
}

@Composable
private fun EditorPanel(
    modifier: Modifier = Modifier,
    title: String,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    fontSizeSp: Int,
    controls: @Composable () -> Unit,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        val editorTextStyle = MaterialTheme.typography.bodyLarge.copy(
            fontSize = fontSizeSp.sp,
            lineHeight = fontSizeSp.sp,
            platformStyle = PlatformTextStyle(includeFontPadding = false),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .heightIn(min = 220.dp),
                value = value,
                onValueChange = onValueChange,
                textStyle = editorTextStyle,
                singleLine = false,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                controls()
            }
        }
    }
}

@Composable
private fun ListenControls(
    state: MainUiState,
    hasRecordPermission: Boolean,
    onStartRecording: () -> Unit,
    onRequestPermission: () -> Unit,
    onStopRecording: () -> Unit,
    onToggleRecording: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(
        isPressed,
        state.settings.listenMode,
        hasRecordPermission,
        state.isTranscribing,
    ) {
        if (state.settings.listenMode == ListenMode.HOLD && hasRecordPermission && !state.isTranscribing) {
            if (isPressed) {
                onStartRecording()
            } else {
                onStopRecording()
            }
        }
    }

    Button(
        onClick = {
            when {
                state.settings.listenMode == ListenMode.TOGGLE -> onToggleRecording()
                !hasRecordPermission -> onRequestPermission()
            }
        },
        enabled = !state.isTranscribing,
        interactionSource = interactionSource,
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.size(44.dp),
    ) {
        when {
            state.isTranscribing -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                )
            }

            state.isRecording -> Icon(
                Icons.Filled.Stop,
                contentDescription = "Stop recording",
            )
            else -> Icon(
                Icons.Filled.Mic,
                contentDescription = "Listen",
            )
        }
    }
}

@Composable
private fun ActionIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    isBusy: Boolean = false,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled && !isBusy,
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.size(40.dp),
    ) {
        if (isBusy) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
            )
        }
    }
}

@Composable
private fun ImportedAudioCard(
    importedAudio: ImportedAudio,
    expanded: Boolean,
    isBusy: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onTranscribe: () -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandedChange(!expanded) },
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse imported audio" else "Expand imported audio",
                )
                Text(
                    text = "Imported",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = importedAudio.displayName,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                }
                ActionIconButton(
                    icon = Icons.Filled.Delete,
                    contentDescription = "Clear imported audio",
                    enabled = !isBusy,
                    onClick = onClear,
                )
            }

            if (expanded) {
                Text(
                    text = importedAudio.sourceLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ActionIconButton(
                        icon = Icons.Filled.PlayArrow,
                        contentDescription = "Transcribe imported audio",
                        isBusy = isBusy,
                        onClick = onTranscribe,
                    )
                    ActionIconButton(
                        icon = Icons.Filled.Save,
                        contentDescription = "Save imported audio",
                        onClick = onSave,
                    )
                }
            }
        }
    }
}
