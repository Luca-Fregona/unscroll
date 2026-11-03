package io.github.lucafregona.unscroll

import android.app.Application
import io.github.lucafregona.unscroll.rules.RulesRepository
import java.io.File

/** Owns what the service and the screens share: the rules. The service runs in this same process. */
class UnscrollApp : Application() {
    val rules: RulesRepository by lazy {
        val bundled = assets.list(RULES_ASSET_DIR).orEmpty()
            .filter { it.endsWith(".json") }
            .sorted()
            .map { name -> assets.open("$RULES_ASSET_DIR/$name").bufferedReader().use { it.readText() } }
        RulesRepository(bundled, File(filesDir, "rules"))
    }

    private companion object {
        const val RULES_ASSET_DIR = "rules"
    }
}
