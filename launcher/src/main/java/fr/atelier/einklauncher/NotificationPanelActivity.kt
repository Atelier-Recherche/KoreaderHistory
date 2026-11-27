package fr.atelier.einklauncher

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import fr.atelier.einklauncher.databinding.ActivityNotificationsBinding
import fr.atelier.einklauncher.databinding.ItemNotificationBinding

class NotificationPanelActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNotificationsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.notifList.layoutManager = LinearLayoutManager(this)
        binding.btnClose.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        val listener = EinkNotificationListener.instance
        if (listener == null) {
            binding.emptyView.visibility = View.VISIBLE
            binding.emptyView.text = getString(R.string.enable_notification_access)
            binding.emptyView.setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
            binding.notifList.adapter = null
            return
        }
        val items = listener.visibleNotifications().map { sbn ->
            val appLabel = runCatching {
                packageManager.getApplicationLabel(
                    packageManager.getApplicationInfo(sbn.packageName, 0),
                ).toString()
            }.getOrDefault(sbn.packageName)
            NotifUi(
                key = sbn.key,
                packageName = sbn.packageName,
                appLabel = appLabel,
                silent = listener.isSilent(sbn),
                notification = sbn.notification,
            )
        }
        binding.emptyView.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        binding.emptyView.text = getString(R.string.notifications_empty)
        binding.emptyView.setOnClickListener(null)
        binding.notifList.adapter = Adapter(items) { action, item ->
            when (action) {
                Action.OPEN -> {
                    item.notification.contentIntent?.send()
                    finish()
                }
                Action.DISMISS -> {
                    EinkNotificationListener.instance?.cancelNotification(item.key)
                    onResume()
                }
            }
        }
    }

    private data class NotifUi(
        val key: String,
        val packageName: String,
        val appLabel: String,
        val silent: Boolean,
        val notification: android.app.Notification,
    )

    private enum class Action { OPEN, DISMISS }

    private class Adapter(
        private val items: List<NotifUi>,
        private val onAction: (Action, NotifUi) -> Unit,
    ) : RecyclerView.Adapter<Adapter.VH>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val b = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return VH(b)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            val extras = item.notification.extras
            val ongoing = (item.notification.flags and android.app.Notification.FLAG_ONGOING_EVENT) != 0
            val title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString()
                ?: item.appLabel
            holder.binding.notifApp.text = buildString {
                append(item.appLabel)
                if (item.silent) append(" · silencieuse")
                if (ongoing) append(" · permanente")
            }
            holder.binding.notifTitle.text = title
            holder.binding.notifText.text =
                extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString().orEmpty()
            holder.binding.root.alpha = if (item.silent) 0.55f else 1f
            holder.binding.btnOpen.setOnClickListener {
                runCatching { onAction(Action.OPEN, item) }
                    .onFailure {
                        Toast.makeText(holder.binding.root.context, "Impossible d’ouvrir", Toast.LENGTH_SHORT).show()
                    }
            }
            holder.binding.btnDismiss.setOnClickListener { onAction(Action.DISMISS, item) }
        }

        class VH(val binding: ItemNotificationBinding) : RecyclerView.ViewHolder(binding.root)
    }
}
