package io.github.lucafregona.unscroll.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import io.github.lucafregona.unscroll.UnscrollApp
import io.github.lucafregona.unscroll.engine.CooldownGate
import io.github.lucafregona.unscroll.engine.Decision
import io.github.lucafregona.unscroll.engine.Engine
import io.github.lucafregona.unscroll.engine.RuleMatcher
import io.github.lucafregona.unscroll.rules.RuleAction
import io.github.lucafregona.unscroll.rules.TargetPackages

/**
 * Receives events from Instagram and YouTube only (see `accessibility_service_config.xml`), looks at the
 * window that is in front, and presses Back or Home, or covers the screen, when a rule says that window is a
 * Reels or Shorts screen.
 */
class UnscrollService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var evaluationScheduled = false
    private var pendingPackage: String? = null
    private lateinit var engine: Engine
    private val overlay: OverlayController by lazy { OverlayController(this, ::goBackFromOverlay) }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val app = application as UnscrollApp
        engine = Engine({ app.rules.current() }, RuleMatcher(), CooldownGate(clock = SystemClock::elapsedRealtime))
        instance = this
        app.refreshRulesInBackground()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        if (packageName !in TargetPackages.ALL) return
        // A playing video fires content-changed events all the time: evaluate once per burst, not per event.
        pendingPackage = packageName
        if (!evaluationScheduled) {
            evaluationScheduled = true
            handler.postDelayed(::evaluate, EVALUATION_DELAY_MS)
        }
    }

    override fun onInterrupt() = stopWorking()

    override fun onUnbind(intent: Intent?): Boolean {
        stopWorking()
        instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        stopWorking()
        instance = null
        super.onDestroy()
    }

    /** Cancels pending work and takes the overlay down: a service that is off must leave nothing on screen. */
    private fun stopWorking() {
        handler.removeCallbacksAndMessages(null)
        evaluationScheduled = false
        overlay.hide()
    }

    private fun evaluate() {
        evaluationScheduled = false
        val packageName = pendingPackage ?: return
        val root = rootInActiveWindow ?: return
        val tree = NodeTree(root)
        try {
            val foreground = root.packageName?.toString()
            // The window in front may belong to another app by now, and then it is not ours to judge.
            if (foreground != packageName) {
                overlay.hide()
                return
            }
            val decision = engine.evaluate(packageName, tree.root)
            val wantsOverlay = decision is Decision.Match && decision.rule.action == RuleAction.OVERLAY
            if (!OverlayPolicy.shouldStay(wantsOverlay, foreground, TargetPackages.ALL)) overlay.hide()
            if (decision is Decision.Match && decision.allowed) perform(decision)
        } finally {
            tree.recycle()
        }
    }

    private fun perform(match: Decision.Match) {
        Log.i(TAG, "rule ${match.rule.id}: ${match.rule.action}")
        when (match.rule.action) {
            RuleAction.BACK -> performGlobalAction(GLOBAL_ACTION_BACK)
            RuleAction.HOME -> performGlobalAction(GLOBAL_ACTION_HOME)
            RuleAction.OVERLAY -> {
                overlay.show()
                handler.removeCallbacks(watchdog)
                handler.postDelayed(watchdog, WATCHDOG_MS)
            }
        }
    }

    private fun goBackFromOverlay() {
        overlay.hide()
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    /** Takes the overlay down once the user has left the target app, which no event announces. */
    private val watchdog = object : Runnable {
        override fun run() {
            if (!overlay.isShowing) return
            if (OverlayPolicy.shouldStay(matchPresent = true, foregroundPackage = foregroundPackage(), targets = TargetPackages.ALL)) {
                handler.postDelayed(this, WATCHDOG_MS)
            } else {
                overlay.hide()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun foregroundPackage(): String? {
        val root = rootInActiveWindow ?: return null
        val name = root.packageName?.toString()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) root.recycle()
        return name
    }

    companion object {
        private const val TAG = "unscroll"
        private const val EVALUATION_DELAY_MS = 150L
        private const val WATCHDOG_MS = 1_000L

        /** The connected service, so the app can turn it off. Set between connect and unbind. */
        @Volatile
        var instance: UnscrollService? = null
            private set
    }
}
