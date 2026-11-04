package io.github.lucafregona.unscroll.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.lucafregona.unscroll.R
import io.github.lucafregona.unscroll.UnscrollApp
import io.github.lucafregona.unscroll.rules.ImportResult
import io.github.lucafregona.unscroll.rules.importStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The offline flavor has no network, so rules come from a file the user picks. */
@Composable
fun RulesSourceSection(app: UnscrollApp, onRulesChanged: () -> Unit) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }
    val resetDone = stringResource(R.string.rules_reset_done)

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.let { app.rules.importStream(it) }
                } catch (e: Exception) {
                    null // a revoked grant, a cloud file that is gone: shown as "could not be opened"
                }
            }
            message = when (result) {
                is ImportResult.Stored -> resources.getString(R.string.import_stored, result.id, result.version)
                is ImportResult.Rejected -> resources.getString(R.string.import_rejected, result.reason)
                null -> resources.getString(R.string.import_unreadable)
            }
            onRulesChanged()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = { picker.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.rules_import_file))
        }
        OutlinedButton(
            onClick = {
                app.rules.resetStored()
                message = resetDone
                onRulesChanged()
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.rules_reset_bundled)) }
        message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
    }
}
