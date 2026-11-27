package fr.atelier.einklauncher

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import fr.atelier.einklauncher.databinding.ActivityHomeBinding
import fr.atelier.einklauncher.databinding.ItemDesktopAppBinding
import fr.atelier.einklauncher.databinding.ItemHistoryRowBinding
import fr.atelier.koreaderhistory.core.R as CoreR
import fr.atelier.koreaderhistory.data.AppIconLoader
import fr.atelier.koreaderhistory.data.DesktopItem
import fr.atelier.koreaderhistory.data.DockExecutor
import fr.atelier.koreaderhistory.data.DockIconResolver
import fr.atelier.koreaderhistory.data.HistoryReader
import fr.atelier.koreaderhistory.data.KoreaderLauncher
import fr.atelier.koreaderhistory.data.LauncherPrefs
import fr.atelier.koreaderhistory.data.WidgetPrefs

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var launcherPrefs: LauncherPrefs
    private lateinit var widgetPrefs: WidgetPrefs
    private lateinit var adapter: DesktopAdapter
    private lateinit var appWidgetHost: AppWidgetHost
    private lateinit var appWidgetManager: AppWidgetManager

    private var pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var pendingProvider: ComponentName? = null

    private val notifReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshDock()
        }
    }

    private val pickProvider = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val id = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingWidgetId) ?: pendingWidgetId
        val provider = result.data.componentProvider()
        WidgetLog.file(this, "pick resultCode=${result.resultCode} id=$id provider=$provider")
        if (result.resultCode != Activity.RESULT_OK || id == AppWidgetManager.INVALID_APPWIDGET_ID || provider == null) {
            if (id != AppWidgetManager.INVALID_APPWIDGET_ID) appWidgetHost.deleteAppWidgetId(id)
            clearPending()
            if (result.resultCode != Activity.RESULT_CANCELED) {
                WidgetLog.toast(this, "Picker: provider manquant (code ${result.resultCode})")
            }
            return@registerForActivityResult
        }
        pendingWidgetId = id
        pendingProvider = provider
        bindPendingWidget()
    }

    private val requestBind = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val id = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingWidgetId) ?: pendingWidgetId
        WidgetLog.file(this, "bind-permission resultCode=${result.resultCode} id=$id")
        if (result.resultCode != Activity.RESULT_OK || id == AppWidgetManager.INVALID_APPWIDGET_ID) {
            if (id != AppWidgetManager.INVALID_APPWIDGET_ID) appWidgetHost.deleteAppWidgetId(id)
            clearPending()
            WidgetLog.toast(this, "Bind refusé/annulé (code ${result.resultCode})")
            return@registerForActivityResult
        }
        continueAfterBind(id)
    }

    private val configureWidget = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val id = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingWidgetId) ?: pendingWidgetId
        WidgetLog.file(this, "configure resultCode=${result.resultCode} id=$id")
        if (result.resultCode != Activity.RESULT_OK || id == AppWidgetManager.INVALID_APPWIDGET_ID) {
            // Certains widgets (Plan Recorder) renvoient CANCELED même après config OK —
            // si le provider est bien lié, on finalise quand même.
            val info = if (id != AppWidgetManager.INVALID_APPWIDGET_ID) {
                appWidgetManager.getAppWidgetInfo(id)
            } else null
            if (info != null) {
                WidgetLog.file(this, "configure canceled but provider bound → finalize anyway: ${info.provider}")
                finalizeWidget(id)
                return@registerForActivityResult
            }
            if (id != AppWidgetManager.INVALID_APPWIDGET_ID) appWidgetHost.deleteAppWidgetId(id)
            clearPending()
            WidgetLog.toast(this, "Config widget annulée (pas de provider lié)")
            return@registerForActivityResult
        }
        finalizeWidget(id)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        launcherPrefs = LauncherPrefs(this)
        widgetPrefs = WidgetPrefs(this)
        appWidgetManager = AppWidgetManager.getInstance(this)
        appWidgetHost = EinkAppWidgetHost(this, HOST_ID)
        WidgetLog.file(this, "HomeActivity onCreate host=$HOST_ID")

        adapter = DesktopAdapter(
            onClick = { item -> onDesktopClick(item) },
            onLongClick = { item -> onDesktopLongClick(item); true },
        )
        binding.desktopGrid.layoutManager = GridLayoutManager(this, 4)
        binding.desktopGrid.adapter = adapter

        val openMenu = View.OnLongClickListener {
            showDesktopMenu()
            true
        }
        binding.desktopEmpty.setOnLongClickListener(openMenu)
        binding.desktopContent.setOnLongClickListener(openMenu)
        binding.desktopScroll.setOnLongClickListener(openMenu)
        binding.launcherDock.setOnLongClickListener {
            startActivity(Intent(this, LauncherDockEditorActivity::class.java))
            true
        }
    }

    override fun onStart() {
        super.onStart()
        appWidgetHost.startListening()
    }

    override fun onStop() {
        super.onStop()
        runCatching { appWidgetHost.stopListening() }
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(EinkNotificationListener.ACTION_CHANGED)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(notifReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(notifReceiver, filter)
        }
        refreshDesktop()
        refreshDock()
        refreshHistoryPanel()
        rebuildWidgetViews()
    }

    override fun onPause() {
        runCatching { unregisterReceiver(notifReceiver) }
        super.onPause()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when {
                event.keyCode == KeyEvent.KEYCODE_META_LEFT ||
                    event.keyCode == KeyEvent.KEYCODE_META_RIGHT ||
                    event.keyCode == KeyEvent.KEYCODE_WINDOW -> {
                    startActivity(Intent(this, TaskBarActivity::class.java))
                    return true
                }
                event.isAltPressed && event.keyCode == KeyEvent.KEYCODE_TAB -> {
                    startActivity(Intent(this, AltTabActivity::class.java))
                    return true
                }
                event.isAltPressed && event.keyCode == KeyEvent.KEYCODE_SPACE -> {
                    startActivity(Intent(this, RunLauncherActivity::class.java))
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun refreshDesktop() {
        adapter.submit(launcherPrefs.getDesktopItems().filter { it.type != "widget" })
    }

    private fun rebuildWidgetViews() {
        binding.widgetContainer.removeAllViews()
        val density = resources.displayMetrics.density
        val widthDp = ((resources.displayMetrics.widthPixels / density) - 16f).toInt().coerceAtLeast(100)
        val kept = mutableListOf<DesktopItem>()

        launcherPrefs.getDesktopItems().forEach { item ->
            if (item.type != "widget" || item.appWidgetId < 0) {
                kept.add(item)
                return@forEach
            }
            val info = appWidgetManager.getAppWidgetInfo(item.appWidgetId)
            if (info == null) {
                runCatching { appWidgetHost.deleteAppWidgetId(item.appWidgetId) }
                return@forEach
            }
            kept.add(item)

            val heightDp = item.heightDp.coerceIn(80, 800)
            val heightPx = (heightDp * density).toInt()
            applyWidgetOptions(item.appWidgetId, widthDp, heightDp)

            val wrap = LongPressFrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    heightPx,
                ).also { it.bottomMargin = (8 * density).toInt() }
                onLongPressAction = { showWidgetMenu(item) }
            }
            val hostView = appWidgetHost.createView(this, item.appWidgetId, info)
            applyHostViewSize(hostView, widthDp, heightDp)
            wrap.addView(
                hostView,
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
            )
            binding.widgetContainer.addView(wrap)
        }

        if (kept.size != launcherPrefs.getDesktopItems().size) {
            launcherPrefs.setDesktopItems(kept)
        }
    }

    private fun applyWidgetOptions(appWidgetId: Int, widthDp: Int, heightDp: Int) {
        val options = bundleOf(
            AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH to widthDp,
            AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH to widthDp,
            AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT to heightDp,
            AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT to heightDp,
        )
        runCatching { appWidgetManager.updateAppWidgetOptions(appWidgetId, options) }
    }

    private fun applyHostViewSize(hostView: android.appwidget.AppWidgetHostView, widthDp: Int, heightDp: Int) {
        runCatching {
            if (Build.VERSION.SDK_INT >= 31) {
                hostView.updateAppWidgetSize(Bundle(), listOf(SizeF(widthDp.toFloat(), heightDp.toFloat())))
            } else {
                @Suppress("DEPRECATION")
                hostView.updateAppWidgetSize(null, widthDp, heightDp, widthDp, heightDp)
            }
        }
    }

    private fun showWidgetMenu(item: DesktopItem) {
        val options = arrayOf(
            getString(R.string.widget_move_up),
            getString(R.string.widget_move_down),
            getString(R.string.widget_larger),
            getString(R.string.widget_smaller),
            getString(R.string.widget_remove),
        )
        AlertDialog.Builder(this)
            .setTitle(item.label)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        if (launcherPrefs.moveDesktopItem(item.id, -1)) {
                            refreshDesktop()
                            rebuildWidgetViews()
                        }
                    }
                    1 -> {
                        if (launcherPrefs.moveDesktopItem(item.id, 1)) {
                            refreshDesktop()
                            rebuildWidgetViews()
                        }
                    }
                    2 -> resizeWidget(item, item.heightDp + 48)
                    3 -> resizeWidget(item, item.heightDp - 48)
                    4 -> removeDesktopItem(item.id)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun resizeWidget(item: DesktopItem, newHeightDp: Int) {
        launcherPrefs.updateDesktopItem(item.copy(heightDp = newHeightDp.coerceIn(80, 800)))
        rebuildWidgetViews()
    }

    private fun refreshHistoryPanel() {
        val show = launcherPrefs.getDesktopItems().any { it.type == "history" }
        binding.historyPanel.visibility = if (show) View.VISIBLE else View.GONE
        if (!show) return
        binding.historyList.removeAllViews()
        val books = runCatching { HistoryReader(widgetPrefs).loadBooks() }.getOrDefault(emptyList())
        books.take(6).forEach { book ->
            val row = ItemHistoryRowBinding.inflate(layoutInflater, binding.historyList, false)
            row.bookTitle.text = book.displayTitle(widgetPrefs.useFilenameAsTitle)
            row.bookMeta.text = buildString {
                if (widgetPrefs.showAuthor && !book.author.isNullOrBlank()) append(book.author)
                val pct = book.displayPercent
                if (widgetPrefs.showPercent && pct != null) {
                    if (isNotEmpty()) append(" · ")
                    append("$pct%")
                }
            }
            row.root.setOnClickListener {
                KoreaderLauncher.openBook(this, widgetPrefs, book.filePath)
            }
            binding.historyList.addView(row.root)
        }
    }

    private fun refreshDock() {
        binding.launcherDock.removeAllViews()
        val size = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            launcherPrefs.dockIconSizeDp.toFloat(),
            resources.displayMetrics,
        ).toInt()
        val border = launcherPrefs.dockIconBorder

        fun addBuiltin(resId: Int, desc: String, badgeCount: Int = 0, click: () -> Unit) {
            val frame = FrameLayout(this)
            frame.layoutParams = LinearLayout.LayoutParams(size, size).apply { setMargins(4, 0, 4, 0) }
            val iv = ImageView(this)
            iv.layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            iv.setPadding(6, 6, 6, 6)
            iv.scaleType = ImageView.ScaleType.FIT_CENTER
            if (border) iv.setBackgroundResource(R.drawable.dock_button_bg)
            iv.setImageResource(resId)
            iv.contentDescription = desc
            frame.addView(iv)
            if (badgeCount > 0) {
                val badge = TextView(this)
                val badgeSize = (size * 0.42f).toInt().coerceAtLeast(18)
                badge.layoutParams = FrameLayout.LayoutParams(badgeSize, badgeSize, Gravity.TOP or Gravity.END)
                badge.setBackgroundResource(R.drawable.notif_badge_bg)
                badge.setTextColor(0xFFFFFFFF.toInt())
                badge.textSize = 10f
                badge.gravity = Gravity.CENTER
                badge.text = if (badgeCount > 99) "99+" else badgeCount.toString()
                frame.addView(badge)
            }
            frame.setOnClickListener { click() }
            binding.launcherDock.addView(frame)
        }

        addBuiltin(CoreR.drawable.ic_app, getString(R.string.launcher_drawer)) {
            startActivity(Intent(this, AppDrawerActivity::class.java))
        }
        val notifCount = EinkNotificationListener.instance?.audibleCount() ?: 0
        addBuiltin(CoreR.drawable.ic_dispatcher, getString(R.string.notifications_title), notifCount) {
            startActivity(Intent(this, NotificationPanelActivity::class.java))
        }

        val slots = launcherPrefs.getDockSlots()
        val spaceBetween = launcherPrefs.dockJustify == LauncherPrefs.JUSTIFY_SPACE_BETWEEN && slots.size > 1
        slots.forEachIndexed { index, slot ->
            if (spaceBetween && index > 0) {
                binding.launcherDock.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
            }
            val iv = ImageView(this)
            iv.layoutParams = LinearLayout.LayoutParams(size, size).apply { setMargins(4, 0, 4, 0) }
            iv.setPadding(6, 6, 6, 6)
            iv.scaleType = ImageView.ScaleType.FIT_CENTER
            if (border) iv.setBackgroundResource(R.drawable.dock_button_bg)
            iv.setImageDrawable(DockIconResolver.loadDrawable(this, slot))
            iv.contentDescription = slot.label
            iv.setOnClickListener { DockExecutor.execute(this, widgetPrefs, slot) }
            iv.setOnLongClickListener {
                startActivity(Intent(this, LauncherDockEditorActivity::class.java))
                true
            }
            binding.launcherDock.addView(iv)
        }
    }

    private fun onDesktopClick(item: DesktopItem) {
        when (item.type) {
            "app" -> launchPackage(item.packageName, item.activityName)
            "folder" -> startActivity(
                Intent(this, FolderActivity::class.java)
                    .putExtra(FolderActivity.EXTRA_FOLDER_ID, item.id),
            )
            "history" -> binding.historyPanel.visibility = View.VISIBLE
        }
    }

    private fun onDesktopLongClick(item: DesktopItem) {
        AlertDialog.Builder(this)
            .setTitle(item.label)
            .setItems(arrayOf(getString(R.string.remove_from_desktop))) { _, _ ->
                removeDesktopItem(item.id)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun removeDesktopItem(id: String) {
        val items = launcherPrefs.getDesktopItems().toMutableList()
        val removed = items.firstOrNull { it.id == id }
        if (removed?.type == "widget" && removed.appWidgetId >= 0) {
            runCatching { appWidgetHost.deleteAppWidgetId(removed.appWidgetId) }
        }
        launcherPrefs.setDesktopItems(items.filterNot { it.id == id })
        refreshDesktop()
        refreshHistoryPanel()
        rebuildWidgetViews()
    }

    private fun showDesktopMenu() {
        val options = arrayOf(
            getString(R.string.launcher_add_history),
            getString(R.string.launcher_add_widget),
            getString(R.string.launcher_folder),
            getString(R.string.edit_launcher_dock),
            getString(R.string.open_settings),
            getString(R.string.enable_accessibility),
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.launcher_home)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> addHistoryPanel()
                    1 -> requestAddWidget()
                    2 -> createFolder()
                    3 -> startActivity(Intent(this, LauncherDockEditorActivity::class.java))
                    4 -> startActivity(Intent(this, LauncherSettingsActivity::class.java))
                    5 -> startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            }
            .show()
    }

    private fun addHistoryPanel() {
        val items = launcherPrefs.getDesktopItems().toMutableList()
        if (items.any { it.type == "history" }) {
            Toast.makeText(this, "Déjà présent", Toast.LENGTH_SHORT).show()
            return
        }
        items.add(
            DesktopItem(
                id = LauncherPrefs.newId(),
                type = "history",
                label = getString(R.string.widget_name),
            ),
        )
        launcherPrefs.setDesktopItems(items)
        refreshHistoryPanel()
        refreshDesktop()
    }

    private fun createFolder() {
        val items = launcherPrefs.getDesktopItems().toMutableList()
        items.add(
            DesktopItem(
                id = LauncherPrefs.newId(),
                type = "folder",
                label = getString(R.string.launcher_folder),
            ),
        )
        launcherPrefs.setDesktopItems(items)
        refreshDesktop()
    }

    private fun requestAddWidget() {
        val id = appWidgetHost.allocateAppWidgetId()
        pendingWidgetId = id
        WidgetLog.file(this, "requestAddWidget allocate id=$id")
        pickProvider.launch(
            Intent(this, WidgetPickerActivity::class.java)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id),
        )
    }

    private fun bindPendingWidget() {
        val id = pendingWidgetId
        val provider = pendingProvider
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID || provider == null) {
            clearPending()
            return
        }
        val widthDp = ((resources.displayMetrics.widthPixels / resources.displayMetrics.density) - 16f).toInt()
        val options = bundleOf(
            AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH to widthDp,
            AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH to widthDp,
            AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT to 180,
            AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT to 180,
        )
        WidgetLog.file(this, "bindIfAllowed id=$id provider=$provider options=$options")
        val allowed = runCatching {
            appWidgetManager.bindAppWidgetIdIfAllowed(id, provider, options)
        }.onFailure { WidgetLog.e("bindIfAllowed threw", it) }.getOrDefault(false)
        WidgetLog.file(this, "bindIfAllowed → $allowed")
        if (allowed) {
            continueAfterBind(id)
        } else {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS, options)
            }
            WidgetLog.file(this, "launch ACTION_APPWIDGET_BIND")
            requestBind.launch(intent)
        }
    }

    private fun continueAfterBind(id: Int) {
        val info = appWidgetManager.getAppWidgetInfo(id)
        WidgetLog.file(this, "continueAfterBind id=$id info=${info?.provider} configure=${info?.configure}")
        if (info == null) {
            appWidgetHost.deleteAppWidgetId(id)
            clearPending()
            WidgetLog.toast(this, "Bind OK mais getAppWidgetInfo=null (id=$id)")
            return
        }
        if (info.configure != null) {
            pendingWidgetId = id
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).setComponent(info.configure)
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            WidgetLog.file(this, "launch configure ${info.configure}")
            configureWidget.launch(intent)
        } else {
            finalizeWidget(id)
        }
    }

    private fun finalizeWidget(id: Int) {
        val info = appWidgetManager.getAppWidgetInfo(id)
        WidgetLog.file(this, "finalizeWidget id=$id info=${info?.provider}")
        if (info == null) {
            appWidgetHost.deleteAppWidgetId(id)
            clearPending()
            WidgetLog.toast(this, "Finalize: getAppWidgetInfo=null (id=$id)")
            return
        }
        // Ignore absurd provider min sizes (Plan Recorder déclare parfois 64001x10241).
        val defaultHeight = 180
        val items = launcherPrefs.getDesktopItems().toMutableList()
        if (items.none { it.appWidgetId == id }) {
            items.add(
                DesktopItem(
                    id = LauncherPrefs.newId(),
                    type = "widget",
                    label = info.loadLabel(packageManager)?.toString() ?: "Widget",
                    appWidgetId = id,
                    spanX = 4,
                    spanY = 1,
                    heightDp = defaultHeight,
                ),
            )
            launcherPrefs.setDesktopItems(items)
            WidgetLog.file(this, "desktop item added for id=$id label=${info.loadLabel(packageManager)}")
        } else {
            WidgetLog.file(this, "desktop item already present for id=$id")
        }
        clearPending()
        rebuildWidgetViews()
    }

    private fun clearPending() {
        pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
        pendingProvider = null
    }

    private fun launchPackage(packageName: String, activityName: String = "") {
        if (packageName.isBlank()) return
        if (KoreaderLauncher.launchApp(this, packageName, activityName.ifBlank { null })) return
        if (fr.atelier.koreaderhistory.data.BooxFreezeHelper.isFrozen(this, packageName)) {
            fr.atelier.koreaderhistory.data.BooxFreezeHelper.toastFrozen(this, packageName)
        } else {
            Toast.makeText(this, R.string.catalog_empty, Toast.LENGTH_SHORT).show()
        }
    }

    private class DesktopAdapter(
        private val onClick: (DesktopItem) -> Unit,
        private val onLongClick: (DesktopItem) -> Boolean,
    ) : RecyclerView.Adapter<DesktopAdapter.VH>() {
        private var items: List<DesktopItem> = emptyList()

        fun submit(list: List<DesktopItem>) {
            items = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val binding = ItemDesktopAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return VH(binding)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.bind(items[position], onClick, onLongClick)
        }

        class VH(private val binding: ItemDesktopAppBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(
                item: DesktopItem,
                onClick: (DesktopItem) -> Unit,
                onLongClick: (DesktopItem) -> Boolean,
            ) {
                binding.appLabel.text = item.label
                when (item.type) {
                    "folder" -> binding.appIcon.setImageResource(CoreR.drawable.ic_folder)
                    "history" -> binding.appIcon.setImageResource(CoreR.drawable.ic_history)
                    else -> binding.appIcon.setImageDrawable(
                        AppIconLoader.loadActivityDrawable(
                            binding.root.context,
                            item.packageName,
                            item.activityName,
                        ) ?: AppIconLoader.loadDrawable(binding.root.context, item.packageName),
                    )
                }
                binding.root.setOnClickListener { onClick(item) }
                binding.root.setOnLongClickListener { onLongClick(item) }
            }
        }
    }

    companion object {
        private const val HOST_ID = 0xA7E1
    }
}

private fun Intent?.componentProvider(): ComponentName? {
    if (this == null) return null
    return if (Build.VERSION.SDK_INT >= 33) {
        getParcelableExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, ComponentName::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER)
    }
}
