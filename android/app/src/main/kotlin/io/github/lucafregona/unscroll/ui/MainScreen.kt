package io.github.lucafregona.unscroll.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.lucafregona.unscroll.R
import io.github.lucafregona.unscroll.rules.AndroidRuleSet
import io.github.lucafregona.unscroll.rules.TargetPackages

@Composable
fun UnscrollTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background, content = content)
    }
}

@Composable
fun MainScreen(
    step: OnboardingStep,
    showRestrictedHelp: Boolean,
    ruleSets: List<AndroidRuleSet>,
    onAcceptDisclosure: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onTurnOff: () -> Unit,
    rulesSection: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        when (step) {
            OnboardingStep.DISCLOSURE -> Disclosure(onAcceptDisclosure)
            OnboardingStep.ENABLE_SERVICE -> EnableService(showRestrictedHelp, onOpenAccessibilitySettings, onOpenAppInfo)
            OnboardingStep.ACTIVE -> Active(ruleSets, onTurnOff, rulesSection)
        }
    }
}

@Composable
private fun Title(text: String) = Text(text, style = MaterialTheme.typography.headlineMedium)

@Composable
private fun Disclosure(onAccept: () -> Unit) {
    Title(stringResource(R.string.disclosure_title))
    Text(stringResource(R.string.disclosure_intro), style = MaterialTheme.typography.bodyLarge)
    listOf(
        R.string.disclosure_scope,
        R.string.disclosure_reads,
        R.string.disclosure_acts,
        R.string.disclosure_privacy,
        R.string.disclosure_network,
        R.string.disclosure_off,
    ).forEach { Bullet(stringResource(it)) }
    Spacer(Modifier.height(8.dp))
    Button(onClick = onAccept, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.disclosure_accept)) }
}

@Composable
private fun Bullet(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("•", style = MaterialTheme.typography.bodyLarge)
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun EnableService(showRestrictedHelp: Boolean, onOpenSettings: () -> Unit, onOpenAppInfo: () -> Unit) {
    Title(stringResource(R.string.enable_title))
    Text(stringResource(R.string.enable_body), style = MaterialTheme.typography.bodyLarge)
    Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.enable_open_settings)) }
    if (showRestrictedHelp) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.restricted_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.restricted_body), style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(onClick = onOpenAppInfo, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.restricted_open_app_info))
                }
            }
        }
    }
}

@Composable
private fun Active(ruleSets: List<AndroidRuleSet>, onTurnOff: () -> Unit, rulesSection: @Composable () -> Unit) {
    Title(stringResource(R.string.active_title))
    val apps = coveredAppNames(ruleSets)
    Text(
        if (apps.isEmpty()) stringResource(R.string.active_body_none) else stringResource(R.string.active_body, apps.joinToString(", ")),
        style = MaterialTheme.typography.bodyLarge,
    )
    Text(stringResource(R.string.rules_in_use), style = MaterialTheme.typography.titleMedium)
    ruleSets.forEach { ruleSet ->
        Text(
            stringResource(R.string.rule_set_line, TargetPackages.displayName(ruleSet.packageName), ruleSet.version, ruleSet.testedVersion),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    rulesSection()
    Spacer(Modifier.height(8.dp))
    OutlinedButton(onClick = onTurnOff, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.turn_off)) }
}
