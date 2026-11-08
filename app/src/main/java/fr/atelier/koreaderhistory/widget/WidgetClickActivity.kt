package fr.atelier.koreaderhistory.widget

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import fr.atelier.koreaderhistory.data.DockExecutor
import fr.atelier.koreaderhistory.data.KoreaderLauncher
import fr.atelier.koreaderhistory.data.WidgetPrefs

/**
 * Transparent trampoline: widget clicks start an Activity (reliable on e-ink
 * launchers) instead of relying on Broadcast receivers.
 */
class WidgetClickActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = WidgetPrefs(this)
        when (intent?.action) {
            ACTION_OPEN_BOOK -> {
                val path = intent.getStringExtra(EXTRA_PATH)
                if (!path.isNullOrBlank()) {
                    KoreaderLauncher.openBook(this, prefs, path)
                }
            }
            ACTION_DOCK -> {
                val slotId = intent.getStringExtra(EXTRA_SLOT_ID)
                val slot = slotId?.let { id -> prefs.getDockSlots().find { it.id == id } }
                if (slot != null) {
                    DockExecutor.execute(this, prefs, slot)
                }
            }
            ACTION_REFRESH -> {
                HistoryWidgetProvider.refreshAll(this)
            }
        }
        // Delay finish so the outgoing startActivity is not aborted as BAL (-91 on Onyx/Android 15)
        Handler(Looper.getMainLooper()).postDelayed({
            if (!isFinishing) finish()
        }, 800)
    }

    companion object {
        const val ACTION_OPEN_BOOK = "fr.atelier.koreaderhistory.CLICK_OPEN_BOOK"
        const val ACTION_DOCK = "fr.atelier.koreaderhistory.CLICK_DOCK"
        const val ACTION_REFRESH = "fr.atelier.koreaderhistory.CLICK_REFRESH"
        const val EXTRA_PATH = "path"
        const val EXTRA_SLOT_ID = "slot_id"

        fun openBookIntent(context: Context, path: String): Intent =
            Intent(context, WidgetClickActivity::class.java).apply {
                action = ACTION_OPEN_BOOK
                putExtra(EXTRA_PATH, path)
                data = android.net.Uri.parse("athistory://book/${path.hashCode()}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

        fun dockIntent(context: Context, slotId: String, requestKey: Int): Intent =
            Intent(context, WidgetClickActivity::class.java).apply {
                action = ACTION_DOCK
                putExtra(EXTRA_SLOT_ID, slotId)
                data = android.net.Uri.parse("athistory://dock/$requestKey/$slotId")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

        fun refreshIntent(context: Context): Intent =
            Intent(context, WidgetClickActivity::class.java).apply {
                action = ACTION_REFRESH
                data = android.net.Uri.parse("athistory://refresh")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
    }
}
