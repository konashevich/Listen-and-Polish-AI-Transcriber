package com.konashevich.pressscribe

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.konashevich.pressscribe.ui.TranscriptionApp
import com.konashevich.pressscribe.ui.theme.PressScribeTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val state = viewModel.uiState.collectAsStateWithLifecycle()
            PressScribeTheme(themeMode = state.value.settings.themeMode) {
                TranscriptionApp(
                    state = state.value,
                    viewModel = viewModel,
                )
            }
        }

        handleIncomingIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val uris = extractAudioUris(intent)
        if (uris.isNotEmpty()) {
            viewModel.handleSharedAudioUris(uris)
            consumeHandledShareIntent(intent)
        }
    }

    private fun consumeHandledShareIntent(intent: Intent?) {
        if (intent == null) {
            return
        }

        setIntent(
            Intent(intent).apply {
                action = null
                data = null
                type = null
                clipData = null
                replaceExtras(Bundle())
            },
        )
    }

    @Suppress("DEPRECATION")
    private fun extractAudioUris(intent: Intent?): List<Uri> {
        return when (intent?.action) {
            Intent.ACTION_SEND -> {
                listOfNotNull(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    } else {
                        intent.getParcelableExtra(Intent.EXTRA_STREAM)
                    },
                )
            }

            Intent.ACTION_SEND_MULTIPLE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
                } else {
                    intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM).orEmpty()
                }
            }

            else -> emptyList()
        }
    }
}
