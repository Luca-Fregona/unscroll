package io.github.lucafregona.unscroll

import android.app.Application
import android.os.Handler
import android.os.Looper
import io.github.lucafregona.unscroll.rules.RulesRefresher
import io.github.lucafregona.unscroll.rules.RulesRepository
import io.github.lucafregona.unscroll.rules.createRulesFetcher
import io.github.lucafregona.unscroll.ui.SettingsStore
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/** Owns what the service and the screens share: the rules. The service runs in this same process. */
class UnscrollApp : Application() {
    val rules: RulesRepository by lazy {
        val bundled = assets.list(RULES_ASSET_DIR).orEmpty()
            .filter { it.endsWith(".json") }
            .sorted()
            .map { name -> assets.open("$RULES_ASSET_DIR/$name").bufferedReader().use { it.readText() } }
        RulesRepository(bundled, File(filesDir, "rules"))
    }

    val settings: SettingsStore by lazy { SettingsStore(this) }

    val refresher: RulesRefresher by lazy {
        RulesRefresher(settings, rules, createRulesFetcher(), BuildConfig.DEFAULT_RULES_BASE_URL, BuildConfig.DEBUG, System::currentTimeMillis)
    }

    private val refreshing = AtomicBoolean(false)

    /**
     * Refreshes the rules if that is due, on a background thread and never two at once. A no-op offline.
     * [onFinished] runs on the main thread once the rules may have changed.
     */
    fun refreshRulesInBackground(onFinished: () -> Unit = {}) {
        if (!refreshing.compareAndSet(false, true)) return
        thread(name = "unscroll-rules-refresh") {
            try {
                refresher.refreshIfDue()
            } finally {
                refreshing.set(false)
                Handler(Looper.getMainLooper()).post(onFinished)
            }
        }
    }

    private companion object {
        const val RULES_ASSET_DIR = "rules"
    }
}
