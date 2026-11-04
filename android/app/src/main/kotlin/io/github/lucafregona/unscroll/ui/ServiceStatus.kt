package io.github.lucafregona.unscroll.ui

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import io.github.lucafregona.unscroll.service.UnscrollService

/**
 * Whether [flatComponentName] is in the `enabled_accessibility_services` setting, a colon-separated list.
 * Entries may use the short class name (`pkg/.Cls`) or the full one (`pkg/pkg.Cls`).
 */
fun isServiceEnabled(settingValue: String?, flatComponentName: String): Boolean {
    if (settingValue.isNullOrEmpty()) return false
    val wanted = ComponentRef.parse(flatComponentName) ?: return false
    return settingValue.split(':').any { ComponentRef.parse(it) == wanted }
}

fun isUnscrollServiceEnabled(context: Context): Boolean {
    val setting = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
    return isServiceEnabled(setting, ComponentName(context, UnscrollService::class.java).flattenToString())
}

/** A component with its class name expanded, so both spellings of a component compare equal. */
private data class ComponentRef(val packageName: String, val className: String) {
    companion object {
        fun parse(flat: String): ComponentRef? {
            val slash = flat.indexOf('/')
            if (slash <= 0 || slash == flat.length - 1) return null
            val packageName = flat.substring(0, slash)
            val className = flat.substring(slash + 1).let { if (it.startsWith(".")) packageName + it else it }
            return ComponentRef(packageName, className)
        }
    }
}
