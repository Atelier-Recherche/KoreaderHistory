package fr.atelier.einklauncher

import android.app.NotificationManager
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class EinkNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        instance = this
        broadcastChanged()
    }

    override fun onListenerDisconnected() {
        if (instance === this) instance = null
        broadcastChanged()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        broadcastChanged()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        broadcastChanged()
    }

    fun currentNotifications(): List<StatusBarNotification> {
        return runCatching { activeNotifications?.toList().orEmpty() }.getOrDefault(emptyList())
    }

    /** Inclut ongoing ; exclut résumés de groupe vides. Silencieuses en bas. */
    fun visibleNotifications(): List<StatusBarNotification> {
        return currentNotifications()
            .filter { sbn ->
                val n = sbn.notification ?: return@filter false
                val isGroupSummary =
                    (n.flags and android.app.Notification.FLAG_GROUP_SUMMARY) != 0
                if (isGroupSummary) {
                    val title = n.extras?.getCharSequence(android.app.Notification.EXTRA_TITLE)
                    val text = n.extras?.getCharSequence(android.app.Notification.EXTRA_TEXT)
                    !title.isNullOrBlank() || !text.isNullOrBlank()
                } else {
                    true
                }
            }
            .sortedWith(
                compareBy<StatusBarNotification> { isSilent(it) }
                    .thenByDescending { it.postTime },
            )
    }

    fun isSilent(sbn: StatusBarNotification): Boolean {
        val ranking = android.service.notification.NotificationListenerService.Ranking()
        val map = currentRanking
        if (map != null && map.getRanking(sbn.key, ranking)) {
            return ranking.importance <= NotificationManager.IMPORTANCE_LOW
        }
        val n = sbn.notification ?: return false
        @Suppress("DEPRECATION")
        return n.priority < android.app.Notification.PRIORITY_DEFAULT
    }

    /** Badge : uniquement les non-silencieuses. */
    fun audibleCount(): Int = visibleNotifications().count { !isSilent(it) }

    @Deprecated("Use audibleCount for badge", ReplaceWith("audibleCount()"))
    fun visibleCount(): Int = audibleCount()

    private fun broadcastChanged() {
        sendBroadcast(
            Intent(ACTION_CHANGED).setPackage(packageName),
        )
    }

    companion object {
        const val ACTION_CHANGED = "fr.atelier.einklauncher.NOTIFICATIONS_CHANGED"

        @Volatile
        var instance: EinkNotificationListener? = null
            private set
    }
}
