package io.github.lucafregona.unscroll.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.lucafregona.unscroll.BuildConfig
import io.github.lucafregona.unscroll.R
import io.github.lucafregona.unscroll.UnscrollApp
import io.github.lucafregona.unscroll.rules.BaseUrl
import io.github.lucafregona.unscroll.rules.RefreshResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The online flavor reads rules from a web address the user can change. The default is this project's own. */
@Composable
fun RulesSourceSection(app: UnscrollApp, onRulesChanged: () -> Unit) {
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    var address by remember { mutableStateOf(app.settings.rulesBaseUrl ?: BuildConfig.DEFAULT_RULES_BASE_URL) }
    var status by remember { mutableStateOf(emptyList<String>()) }
    var busy by remember { mutableStateOf(false) }
    val addressOk = BaseUrl.parse(address, allowLocalCleartext = BuildConfig.DEBUG) != null

    fun describe(result: RefreshResult): String = when (result) {
        is RefreshResult.Updated -> resources.getString(R.string.refresh_updated, result.id, result.version)
        is RefreshResult.UpToDate -> resources.getString(R.string.refresh_up_to_date, result.id)
        is RefreshResult.Failed -> resources.getString(R.string.refresh_failed, result.id, result.reason)
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text(stringResource(R.string.rules_address_label)) },
            isError = !addressOk,
            supportingText = { Text(stringResource(if (addressOk) R.string.rules_address_help else R.string.rules_address_error)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            enabled = addressOk && !busy,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val typed = BaseUrl.parse(address, allowLocalCleartext = BuildConfig.DEBUG)!!.value
                app.settings.rulesBaseUrl = typed.takeUnless { it == BuildConfig.DEFAULT_RULES_BASE_URL }
                busy = true
                scope.launch {
                    val results = withContext(Dispatchers.IO) { app.refresher.refreshNow() }
                    status = results.map(::describe)
                    busy = false
                    onRulesChanged()
                }
            },
        ) { Text(stringResource(R.string.rules_update_now)) }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                app.settings.rulesBaseUrl = null
                app.rules.resetStored()
                address = BuildConfig.DEFAULT_RULES_BASE_URL
                status = listOf(resources.getString(R.string.rules_reset_done))
                onRulesChanged()
            },
        ) { Text(stringResource(R.string.rules_reset_default)) }
        status.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
    }
}
