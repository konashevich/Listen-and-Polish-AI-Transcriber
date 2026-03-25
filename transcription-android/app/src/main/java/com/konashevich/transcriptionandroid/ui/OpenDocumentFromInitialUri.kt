package com.konashevich.pressscribe.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.result.contract.ActivityResultContract

class OpenDocumentFromInitialUri(
    private val initialUri: Uri?,
) : ActivityResultContract<Array<String>, Uri?>() {

    override fun createIntent(context: Context, input: Array<String>): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = input.singleOrNull() ?: "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, input)
            initialUri?.let { putExtra(DocumentsContract.EXTRA_INITIAL_URI, it) }
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        if (resultCode != Activity.RESULT_OK) {
            return null
        }
        return intent?.data
    }
}

data class CreateDocumentRequest(
    val mimeType: String,
    val displayName: String,
)

class CreateDocumentFromInitialUri(
    private val initialUri: Uri?,
) : ActivityResultContract<CreateDocumentRequest, Uri?>() {

    override fun createIntent(context: Context, input: CreateDocumentRequest): Intent {
        return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = input.mimeType.ifBlank { "*/*" }
            putExtra(Intent.EXTRA_TITLE, input.displayName)
            initialUri?.let { putExtra(DocumentsContract.EXTRA_INITIAL_URI, it) }
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        if (resultCode != Activity.RESULT_OK) {
            return null
        }
        return intent?.data
    }
}

fun downloadsInitialUri(): Uri? {
    return runCatching {
        DocumentsContract.buildDocumentUri(
            "com.android.externalstorage.documents",
            "primary:Download",
        )
    }.getOrElse {
        runCatching {
            DocumentsContract.buildRootUri(
                "com.android.providers.downloads.documents",
                "downloads",
            )
        }.getOrNull()
    }
}
