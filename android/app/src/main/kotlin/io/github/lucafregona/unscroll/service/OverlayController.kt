package io.github.lucafregona.unscroll.service

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import io.github.lucafregona.unscroll.R

/**
 * A full-screen "hidden by unscroll" screen drawn above the app. It is opaque and has one button, which
 * hides it and runs [onGoBack]. Whoever shows it must also make sure it goes away (see [OverlayPolicy]).
 */
class OverlayController(private val service: AccessibilityService, private val onGoBack: () -> Unit) {
    private val windowManager = service.getSystemService(WindowManager::class.java)
    private var view: View? = null

    val isShowing: Boolean get() = view != null

    fun show() {
        if (view != null) return
        val content = build()
        val params = WindowManager.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.OPAQUE,
        )
        windowManager.addView(content, params)
        view = content
    }

    fun hide() {
        val current = view ?: return
        view = null
        runCatching { windowManager.removeView(current) }
    }

    private fun build(): View {
        val context = ContextThemeWrapper(service, android.R.style.Theme_DeviceDefault)
        val padding = (24 * context.resources.displayMetrics.density).toInt()
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#1B1B1F"))
            setPadding(padding, padding, padding, padding)
            addView(TextView(context).apply {
                setText(R.string.overlay_title)
                textSize = 24f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
            })
            addView(TextView(context).apply {
                setText(R.string.overlay_body)
                textSize = 16f
                setTextColor(Color.parseColor("#C7C7CC"))
                gravity = Gravity.CENTER
                setPadding(0, padding / 2, 0, padding)
            })
            addView(Button(context).apply {
                setText(R.string.overlay_back)
                setOnClickListener { onGoBack() }
            })
        }
    }
}
