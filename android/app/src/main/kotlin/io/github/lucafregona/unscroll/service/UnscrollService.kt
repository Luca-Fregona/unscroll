package io.github.lucafregona.unscroll.service

import android.accessibilityservice.AccessibilityService
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
 * window that is in front, and presses Back or Home when a rule says that window is a Reels or Shorts screen.
 */
class UnscrollService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var evaluationScheduled = false
    private var pendingPackage: String? = null
    private lateinit var engine: Engine

    override fun onServiceConnected() {
        super.onServiceConnected()
        val app = application as UnscrollApp
        engine = Engine({ app.rules.current() }, RuleMatcher(), CooldownGate(clock = SystemClock::elapsedRealtime))
        instance = this
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

    override fun onInterrupt() = cancelPendingEvaluation()

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        cancelPendingEvaluation()
        instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        cancelPendingEvaluation()
        instance = null
        super.onDestroy()
    }

    private fun cancelPendingEvaluation() {
        handler.removeCallbacksAndMessages(null)
        evaluationScheduled = false
    }

    private fun evaluate() {
        evaluationScheduled = false
        val packageName = pendingPackage ?: return
        val root = rootInActiveWindow ?: return
        val tree = NodeTree(root)
        try {
            // The window in front may belong to another app by now, and then it is not ours to judge.
            if (root.packageName?.toString() != packageName) return
            val decision = engine.evaluate(packageName, tree.root)
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
            RuleAction.OVERLAY -> Unit // handled with the overlay controller
        }
    }

    companion object {
        private const val TAG = "unscroll"
        private const val EVALUATION_DELAY_MS = 150L

        /** The connected service, so the app can turn it off. Set between connect and unbind. */
        @Volatile
        var instance: UnscrollService? = null
            private set
    }
}
