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
import android.bluetooth.BluetoothAdapter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.text.format.DateFormat
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import fr.atelier.einklauncher.databinding.ActivityHomeBinding
import fr.atelier.einklauncher.databinding.ItemDesktopAppBinding
import fr.atelier.einklauncher.databinding.ItemHistoryRowBinding
import fr.atelier.koreaderhistory.core.R as CoreR
import fr.atelier.koreaderhistory.data.AppIconLoader
import fr.atelier.koreaderhistory.data.DesktopItem
import fr.atelier.koreaderhistory.data.DockActionType
import fr.atelier.koreaderhistory.data.DockExecutor
import fr.atelier.koreaderhistory.data.DockIconResolver
import fr.atelier.koreaderhistory.data.HistoryReader
import fr.atelier.koreaderhistory.data.KoreaderCatalog
import fr.atelier.koreaderhistory.data.KoreaderLauncher
import fr.atelier.koreaderhistory.data.LauncherPrefs
import fr.atelier.koreaderhistory.data.WidgetPrefs
import java.util.Date
import java.util.Locale

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var launcherPrefs: LauncherPrefs
    private lateinit var widgetPrefs: WidgetPrefs
    private lateinit var adapter: DesktopAdapter
    private lateinit var appWidgetHost: AppWidgetHost
    private lateinit var appWidgetManager: AppWidgetManager

    private var pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var pendingProvider: ComponentName? = null
    private var pendingIconItemId: String? = null
    private var pendingIconAppKey: String? = null
    private var moveMode = false

    private val notifReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshDock()
        }
    }

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshStatusBar()
        }
    }

    private val pickDesktopIcon = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            pendingIconItemId = null
            pendingIconAppKey = null
            return@registerForActivityResult
        }
        val data = result.data ?: return@registerForActivityResult
        val override = fr.atelier.koreaderhistory.data.AppIconOverride(
            iconKey = data.getStringExtra(IconPickerActivity.EXTRA_ICON_KEY) ?: "app",
            iconPackage = data.getStringExtra(IconPickerActivity.EXTRA_ICON_PACKAGE).orEmpty(),
            iconPackPackage = data.getStringExtra(IconPickerActivity.EXTRA_ICON_PACK).orEmpty(),
            iconDrawableName = data.getStringExtra(IconPickerActivity.EXTRA_ICON_DRAWABLE).orEmpty(),
        )
        val appKey = pendingIconAppKey
        pendingIconAppKey = null
        if (appKey != null) {
            launcherPrefs.setAppIconOverride(appKey, override)
            refreshDesktop()
            return@registerForActivityResult
        }
        val id = pendingIconItemId ?: return@registerForActivityResult
        pendingIconItemId = null
        val items = launcherPrefs.getDesktopItems().toMutableList()
        val idx = items.indexOfFirst { it.id == id }
        if (idx < 0) return@registerForActivityResult
        items[idx] = items[idx].copy(
            iconKey = override.iconKey,
            iconPackage = override.iconPackage,
            iconPackPackage = override.iconPackPackage,
            iconDrawableName = override.iconDrawableName,
        )
        launcherPrefs.setDesktopItems(items)
        refreshDesktop()
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
        applyFullscreen()
        launcherPrefs = LauncherPrefs(this)
        widgetPrefs = WidgetPrefs(this)
        appWidgetManager = AppWidgetManager.getInstance(this)
        appWidgetHost = EinkAppWidgetHost(this, HOST_ID)
        WidgetLog.file(this, "HomeActivity onCreate host=$HOST_ID")

        adapter = DesktopAdapter(
            iconSizeDp = { launcherPrefs.desktopIconSizeDp },
            iconBorder = { launcherPrefs.desktopIconBorder },
            labelSp = { launcherPrefs.desktopLabelSp },
            moveMode = { moveMode },
            resolveOverride = { item ->
                if (item.type != "app") null
                else {
                    val key = if (item.activityName.isNotBlank()) {
                        "${item.packageName}/${item.activityName}"
                    } else item.packageName
                    launcherPrefs.getAppIconOverride(key)
                        ?: launcherPrefs.getAppIconOverride("${item.packageName}/")
                }
            },
            onClick = { item -> onDesktopClick(item) },
            onLongClick = { item -> onDesktopLongClick(item); true },
            onMove = { item, dCol, dRow -> moveGridItem(item, dCol, dRow) },
        )
        binding.desktopGrid.layoutManager = GridLayoutManager(this, launcherPrefs.desktopColumns)
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
        binding.btnRemoveHistory.setOnClickListener { confirmRemoveHistory() }
        binding.btnExitMoveMode.setOnClickListener { setMoveMode(false) }
    }

    private fun applyFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.statusBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    private fun setMoveMode(enabled: Boolean) {
        moveMode = enabled
        binding.moveModeBar.visibility = if (enabled) View.VISIBLE else View.GONE
        refreshDesktop()
        Toast.makeText(
            this,
            if (enabled) getString(R.string.move_mode_hint) else getString(R.string.move_mode_done),
            Toast.LENGTH_SHORT,
        ).show()
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
        registerReceivers()
        refreshDesktop()
        refreshDock()
        refreshHistoryPanel()
        refreshStatusBar()
        rebuildWidgetViews()
    }

    override fun onPause() {
        runCatching { unregisterReceiver(notifReceiver) }
        runCatching { unregisterReceiver(statusReceiver) }
        super.onPause()
    }

    private fun registerReceivers() {
        val notifFilter = IntentFilter(EinkNotificationListener.ACTION_CHANGED)
        val statusFilter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        }
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(notifReceiver, notifFilter, Context.RECEIVER_NOT_EXPORTED)
            registerReceiver(statusReceiver, statusFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(notifReceiver, notifFilter)
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(statusReceiver, statusFilter)
        }
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
        val cols = launcherPrefs.desktopColumns
        (binding.desktopGrid.layoutManager as? GridLayoutManager)?.spanCount = cols
        val gridItems = launcherPrefs.getDesktopItems().filter {
            it.type == "app" || it.type == "folder" || it.type == "shortcut"
        }
        val byCell = gridItems.associateBy { it.cellIndex.coerceAtLeast(0) }
        val maxIdx = (byCell.keys.maxOrNull() ?: -1).coerceAtLeast(0)
        val cellCount = ((maxIdx / cols) + 1) * cols
        val cells = (0 until cellCount).map { idx ->
            byCell[idx] ?: DesktopItem(id = "empty-$idx", type = "empty", label = "", cellIndex = idx)
        }
        adapter.submit(cells)
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
        val historyItem = launcherPrefs.getDesktopItems().firstOrNull { it.type == "history" }
        val show = historyItem != null
        binding.historyPanel.visibility = if (show) View.VISIBLE else View.GONE
        if (!show) return
        binding.btnRemoveHistory.setOnClickListener { confirmRemoveHistory() }
        binding.historyList.removeAllViews()
        val titleSp = launcherPrefs.historyTitleSp.toFloat()
        val books = runCatching { HistoryReader(widgetPrefs).loadBooks() }.getOrDefault(emptyList())
        books.take(6).forEach { book ->
            val row = ItemHistoryRowBinding.inflate(layoutInflater, binding.historyList, false)
            row.bookTitle.text = book.displayTitle(widgetPrefs.useFilenameAsTitle)
            row.bookTitle.textSize = titleSp
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
        if (books.isEmpty()) {
            binding.historyList.addView(
                TextView(this).apply {
                    text = "Historique vide"
                    setTextColor(0xFF444444.toInt())
                    setPadding(8, 16, 8, 16)
                },
            )
        }
    }

    private fun confirmRemoveHistory() {
        val historyItem = launcherPrefs.getDesktopItems().firstOrNull { it.type == "history" } ?: return
        AlertDialog.Builder(this)
            .setTitle(R.string.remove_history_panel)
            .setMessage(R.string.remove_history_confirm)
            .setPositiveButton(R.string.widget_remove) { _, _ -> removeDesktopItem(historyItem.id) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun refreshStatusBar() {
        applyFullscreen()
        val enabled = launcherPrefs.statusBarEnabled
        binding.statusBar.visibility = if (enabled) View.VISIBLE else View.GONE
        // Plein écran : pas de padding pour la barre système (elle est masquée)
        binding.homeRoot.setPadding(0, 0, 0, binding.homeRoot.paddingBottom)
        if (!enabled) return

        val sp = launcherPrefs.statusBarTextSp.toFloat()
        fun show(tv: TextView, on: Boolean, text: String) {
            tv.visibility = if (on) View.VISIBLE else View.GONE
            if (on) {
                tv.text = text
                tv.textSize = sp
            }
        }

        val now = Date()
        val locale = Locale.getDefault()
        show(
            binding.statusTime,
            launcherPrefs.statusBarShowTime,
            DateFormat.getTimeFormat(this).format(now),
        )
        show(
            binding.statusDate,
            launcherPrefs.statusBarShowDate,
            formatStatusDate(now, locale),
        )

        if (launcherPrefs.statusBarShowWifi) {
            val wifiOn = runCatching {
                val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val net = cm.activeNetwork
                val caps = cm.getNetworkCapabilities(net)
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            }.getOrDefault(false)
            show(binding.statusWifi, true, if (wifiOn) getString(R.string.wifi_on) else getString(R.string.wifi_off))
        } else {
            binding.statusWifi.visibility = View.GONE
        }

        if (launcherPrefs.statusBarShowBluetooth) {
            val btOn = runCatching {
                BluetoothAdapter.getDefaultAdapter()?.isEnabled == true
            }.getOrDefault(false)
            show(binding.statusBluetooth, true, if (btOn) getString(R.string.bt_on) else getString(R.string.bt_off))
        } else {
            binding.statusBluetooth.visibility = View.GONE
        }

        if (launcherPrefs.statusBarShowBattery) {
            val sticky = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = sticky?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = sticky?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else -1
            show(binding.statusBattery, true, if (pct >= 0) "$pct%" else "—")
        } else {
            binding.statusBattery.visibility = View.GONE
        }
    }

    private fun formatStatusDate(now: Date, locale: Locale): String {
        return when (launcherPrefs.statusBarDateFormat) {
            LauncherPrefs.DATE_FMT_WEEKDAY ->
                SimpleDateFormat("EEEE", locale).format(now)
            LauncherPrefs.DATE_FMT_FULL ->
                SimpleDateFormat("EEEE d MMMM yyyy", locale).format(now)
            LauncherPrefs.DATE_FMT_MEDIUM ->
                DateFormat.getMediumDateFormat(this).format(now)
            else ->
                SimpleDateFormat("EEE d MMM", locale).format(now)
        }
    }

    private fun refreshDock() {
        WidgetLog.file(this, "refreshDock ${launcherPrefs.readDockVisualDebug()}")
        if (!launcherPrefs.dockEnabled) {
            binding.launcherDock.visibility = View.GONE
            binding.launcherDock.removeAllViews()
            return
        }
        binding.launcherDock.visibility = View.VISIBLE
        binding.launcherDock.removeAllViews()
        val size = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            launcherPrefs.dockIconSizeDp.toFloat(),
            resources.displayMetrics,
        ).toInt()
        val border = launcherPrefs.dockIconBorder

        fun addBuiltin(
            override: fr.atelier.koreaderhistory.data.AppIconOverride,
            fallbackRes: Int,
            desc: String,
            badgeCount: Int = 0,
            click: () -> Unit,
        ) {
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
            val custom = DockIconResolver.loadDrawable(this, override.toDockSlot())
            if (custom != null) iv.setImageDrawable(custom) else iv.setImageResource(fallbackRes)
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

        addBuiltin(
            launcherPrefs.getDockAppsIcon(),
            CoreR.drawable.ic_app,
            getString(R.string.launcher_drawer),
        ) {
            startActivity(Intent(this, AppDrawerActivity::class.java))
        }
        val notifCount = EinkNotificationListener.instance?.audibleCount() ?: 0
        addBuiltin(
            launcherPrefs.getDockNotifsIcon(),
            CoreR.drawable.ic_dispatcher,
            getString(R.string.notifications_title),
            notifCount,
        ) {
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
            "shortcut" -> DockExecutor.execute(this, widgetPrefs, item.toDockSlot())
        }
    }

    private fun onDesktopLongClick(item: DesktopItem) {
        if (item.type == "empty") return
        val moveOptions = listOf(
            getString(R.string.move_left),
            getString(R.string.move_right),
            getString(R.string.move_up),
            getString(R.string.move_down),
        )
        when (item.type) {
            "shortcut", "folder" -> {
                val options = (moveOptions + listOf(
                    getString(R.string.rename),
                    getString(R.string.choose_icon),
                    getString(R.string.remove_from_desktop),
                )).toTypedArray()
                AlertDialog.Builder(this)
                    .setTitle(item.label)
                    .setItems(options) { _, which ->
                        when (which) {
                            0 -> moveGridItem(item, -1, 0)
                            1 -> moveGridItem(item, 1, 0)
                            2 -> moveGridItem(item, 0, -1)
                            3 -> moveGridItem(item, 0, 1)
                            4 -> renameDesktopItem(item)
                            5 -> {
                                pendingIconItemId = item.id
                                pickDesktopIcon.launch(Intent(this, IconPickerActivity::class.java))
                            }
                            6 -> removeDesktopItem(item.id)
                        }
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
            "app" -> {
                val options = (moveOptions + listOf(
                    getString(R.string.choose_icon),
                    getString(R.string.remove_from_desktop),
                )).toTypedArray()
                AlertDialog.Builder(this)
                    .setTitle(item.label)
                    .setItems(options) { _, which ->
                        when (which) {
                            0 -> moveGridItem(item, -1, 0)
                            1 -> moveGridItem(item, 1, 0)
                            2 -> moveGridItem(item, 0, -1)
                            3 -> moveGridItem(item, 0, 1)
                            4 -> {
                                pendingIconAppKey = if (item.activityName.isNotBlank()) {
                                    "${item.packageName}/${item.activityName}"
                                } else item.packageName
                                pickDesktopIcon.launch(Intent(this, IconPickerActivity::class.java))
                            }
                            5 -> removeDesktopItem(item.id)
                        }
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
            else -> {
                AlertDialog.Builder(this)
                    .setTitle(item.label)
                    .setItems(arrayOf(getString(R.string.remove_from_desktop))) { _, _ ->
                        removeDesktopItem(item.id)
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
        }
    }

    private fun moveGridItem(item: DesktopItem, dCol: Int, dRow: Int) {
        if (launcherPrefs.moveDesktopItemOnGrid(item.id, dCol, dRow)) {
            refreshDesktop()
        }
    }

    private fun renameDesktopItem(item: DesktopItem) {
        val input = EditText(this).apply {
            setText(item.label)
            setTextColor(0xFF000000.toInt())
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.rename)
            .setView(input)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = input.text?.toString().orEmpty().ifBlank { item.label }
                launcherPrefs.updateDesktopItem(item.copy(label = name))
                refreshDesktop()
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
        val hasHistory = launcherPrefs.getDesktopItems().any { it.type == "history" }
        val options = buildList {
            add(if (moveMode) getString(R.string.move_mode_done) else getString(R.string.move_mode))
            add(if (hasHistory) getString(R.string.remove_history_panel) else getString(R.string.launcher_add_history))
            add(getString(R.string.launcher_add_widget))
            add(getString(R.string.launcher_folder))
            add(getString(R.string.launcher_add_shortcut))
            add(getString(R.string.edit_launcher_dock))
            add(getString(R.string.open_settings))
            add(getString(R.string.enable_accessibility))
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.launcher_home)
            .setItems(options.toTypedArray()) { _, which ->
                when (which) {
                    0 -> setMoveMode(!moveMode)
                    1 -> if (hasHistory) confirmRemoveHistory() else addHistoryPanel()
                    2 -> requestAddWidget()
                    3 -> createFolder()
                    4 -> addDesktopShortcut()
                    5 -> startActivity(Intent(this, LauncherDockEditorActivity::class.java))
                    6 -> startActivity(Intent(this, LauncherSettingsActivity::class.java))
                    7 -> startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
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
                cellIndex = launcherPrefs.nextFreeCellIndex(),
            ),
        )
        launcherPrefs.setDesktopItems(items)
        refreshDesktop()
    }

    private fun addDesktopShortcut() {
        val choices = listOf(
            DockActionType.OPEN_KOREADER,
            DockActionType.CONTINUE,
            DockActionType.HISTORY,
            DockActionType.OPEN_FOLDER,
            DockActionType.OPEN_COLLECTION,
            DockActionType.OPEN_PLUGIN,
            DockActionType.SYSTEM_ACTION,
            DockActionType.OPEN_BOOK,
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.launcher_add_shortcut)
            .setItems(choices.map { it.label }.toTypedArray()) { _, which ->
                val type = choices[which]
                val iconKey = when (type) {
                    DockActionType.OPEN_KOREADER -> "koreader"
                    DockActionType.CONTINUE -> "continue"
                    DockActionType.HISTORY -> "history"
                    DockActionType.OPEN_FOLDER -> "folder"
                    DockActionType.OPEN_COLLECTION -> "library"
                    DockActionType.OPEN_PLUGIN -> "plugin"
                    DockActionType.SYSTEM_ACTION -> "dispatcher"
                    DockActionType.OPEN_BOOK -> "book"
                    else -> "app"
                }
                when (type) {
                    DockActionType.OPEN_FOLDER,
                    DockActionType.OPEN_COLLECTION,
                    DockActionType.OPEN_PLUGIN,
                    DockActionType.SYSTEM_ACTION -> pickShortcutParam(type, iconKey)
                    else -> {
                        commitDesktopShortcut(
                            DesktopItem(
                                id = LauncherPrefs.newId(),
                                type = "shortcut",
                                label = type.label,
                                actionType = type.id,
                                iconKey = iconKey,
                            ),
                        )
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun pickShortcutParam(type: DockActionType, iconKey: String) {
        val path = widgetPrefs.koreaderPath
        val catalog = when (type) {
            DockActionType.OPEN_FOLDER -> KoreaderCatalog.suggestFolders(path)
            DockActionType.OPEN_COLLECTION -> KoreaderCatalog.listCollections(path)
            DockActionType.OPEN_PLUGIN -> KoreaderCatalog.listPlugins(path)
            DockActionType.SYSTEM_ACTION -> KoreaderCatalog.listSystemActions()
            else -> emptyList()
        }
        if (catalog.isEmpty()) {
            Toast.makeText(this, R.string.catalog_empty, Toast.LENGTH_SHORT).show()
            commitDesktopShortcut(
                DesktopItem(
                    id = LauncherPrefs.newId(),
                    type = "shortcut",
                    label = type.label,
                    actionType = type.id,
                    iconKey = iconKey,
                ),
            )
            return
        }
        AlertDialog.Builder(this)
            .setTitle(type.label)
            .setItems(catalog.map { it.title }.toTypedArray()) { _, which ->
                val item = catalog[which]
                commitDesktopShortcut(
                    DesktopItem(
                        id = LauncherPrefs.newId(),
                        type = "shortcut",
                        label = item.title,
                        actionType = type.id,
                        param = item.param,
                        param2 = item.param2,
                        iconKey = item.suggestedIcon ?: iconKey,
                    ),
                )
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun commitDesktopShortcut(item: DesktopItem) {
        val items = launcherPrefs.getDesktopItems().toMutableList()
        val withCell = if (item.cellIndex >= 0) item
        else item.copy(cellIndex = launcherPrefs.nextFreeCellIndex())
        items.add(withCell)
        launcherPrefs.setDesktopItems(items)
        refreshDesktop()
        Toast.makeText(this, R.string.shortcut_added, Toast.LENGTH_SHORT).show()
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
        private val iconSizeDp: () -> Int,
        private val iconBorder: () -> Boolean,
        private val labelSp: () -> Int,
        private val moveMode: () -> Boolean,
        private val resolveOverride: (DesktopItem) -> fr.atelier.koreaderhistory.data.AppIconOverride?,
        private val onClick: (DesktopItem) -> Unit,
        private val onLongClick: (DesktopItem) -> Boolean,
        private val onMove: (DesktopItem, Int, Int) -> Unit,
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
            holder.bind(
                items[position],
                iconSizeDp(),
                iconBorder(),
                labelSp(),
                moveMode(),
                resolveOverride,
                onClick,
                onLongClick,
                onMove,
            )
        }

        class VH(private val binding: ItemDesktopAppBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(
                item: DesktopItem,
                sizeDp: Int,
                border: Boolean,
                labelSp: Int,
                moveMode: Boolean,
                resolveOverride: (DesktopItem) -> fr.atelier.koreaderhistory.data.AppIconOverride?,
                onClick: (DesktopItem) -> Unit,
                onLongClick: (DesktopItem) -> Boolean,
                onMove: (DesktopItem, Int, Int) -> Unit,
            ) {
                fun showMoves(show: Boolean) {
                    val v = if (show) View.VISIBLE else View.GONE
                    binding.btnMoveUp.visibility = v
                    binding.btnMoveDown.visibility = v
                    binding.btnMoveLeft.visibility = v
                    binding.btnMoveRight.visibility = v
                }

                if (item.type == "empty") {
                    binding.appLabel.text = ""
                    binding.appIcon.setImageDrawable(null)
                    binding.appIcon.background = null
                    binding.root.setOnClickListener(null)
                    binding.root.setOnLongClickListener(null)
                    binding.root.isClickable = false
                    binding.root.alpha = 0f
                    showMoves(false)
                    return
                }
                binding.root.alpha = 1f
                binding.root.isClickable = true
                showMoves(moveMode)

                val sizePx = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    sizeDp.toFloat(),
                    binding.root.resources.displayMetrics,
                ).toInt()
                binding.appIcon.layoutParams = binding.appIcon.layoutParams.apply {
                    width = sizePx
                    height = sizePx
                }
                if (border) binding.appIcon.setBackgroundResource(R.drawable.dock_button_bg)
                else binding.appIcon.background = null

                binding.appLabel.text = item.label
                binding.appLabel.textSize = labelSp.toFloat()
                when (item.type) {
                    "folder" -> {
                        val custom = (item.iconKey.isNotBlank() && item.iconKey != "app") ||
                            item.iconPackage.isNotBlank() || item.iconPackPackage.isNotBlank()
                        if (custom) {
                            binding.appIcon.setImageDrawable(
                                DockIconResolver.loadDrawable(binding.root.context, item.toDockSlot()),
                            )
                        } else {
                            binding.appIcon.setImageResource(CoreR.drawable.ic_folder)
                        }
                    }
                    "history" -> binding.appIcon.setImageResource(CoreR.drawable.ic_history)
                    "shortcut" -> binding.appIcon.setImageDrawable(
                        DockIconResolver.loadDrawable(binding.root.context, item.toDockSlot()),
                    )
                    else -> {
                        val ov = resolveOverride(item)
                        if (ov != null) {
                            binding.appIcon.setImageDrawable(
                                DockIconResolver.loadDrawable(
                                    binding.root.context,
                                    ov.toDockSlot(label = item.label),
                                ),
                            )
                        } else {
                            binding.appIcon.setImageDrawable(
                                AppIconLoader.loadActivityDrawable(
                                    binding.root.context,
                                    item.packageName,
                                    item.activityName,
                                ) ?: AppIconLoader.loadDrawable(binding.root.context, item.packageName),
                            )
                        }
                    }
                }
                if (moveMode) {
                    binding.btnMoveLeft.setOnClickListener { onMove(item, -1, 0) }
                    binding.btnMoveRight.setOnClickListener { onMove(item, 1, 0) }
                    binding.btnMoveUp.setOnClickListener { onMove(item, 0, -1) }
                    binding.btnMoveDown.setOnClickListener { onMove(item, 0, 1) }
                    binding.root.setOnClickListener(null)
                    binding.root.setOnLongClickListener(null)
                } else {
                    binding.root.setOnClickListener { onClick(item) }
                    binding.root.setOnLongClickListener { onLongClick(item) }
                }
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
