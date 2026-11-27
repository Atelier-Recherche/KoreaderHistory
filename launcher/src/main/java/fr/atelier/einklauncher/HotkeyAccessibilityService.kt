package fr.atelier.einklauncher

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

class HotkeyAccessibilityService : AccessibilityService() {

    private var altDown = false

    override fun onServiceConnected() {
        serviceInfo = serviceInfo.apply {
            eventTypes = 0
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
            notificationTimeout = 0
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onKeyEvent(event: KeyEvent): Boolean {
        when (event.keyCode) {
            KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT -> {
                altDown = event.action == KeyEvent.ACTION_DOWN
                return false
            }
            KeyEvent.KEYCODE_META_LEFT, KeyEvent.KEYCODE_META_RIGHT, KeyEvent.KEYCODE_WINDOW -> {
                if (event.action == KeyEvent.ACTION_UP) {
                    open(TaskBarActivity::class.java)
                    return true
                }
                return true
            }
            KeyEvent.KEYCODE_TAB -> {
                if (altDown && event.action == KeyEvent.ACTION_DOWN) {
                    open(AltTabActivity::class.java)
                    return true
                }
            }
            KeyEvent.KEYCODE_SPACE -> {
                if (altDown && event.action == KeyEvent.ACTION_DOWN) {
                    open(RunLauncherActivity::class.java)
                    return true
                }
            }
        }
        return false
    }

    private fun open(cls: Class<*>) {
        val intent = Intent(this, cls).addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS,
        )
        startActivity(intent)
    }
}

