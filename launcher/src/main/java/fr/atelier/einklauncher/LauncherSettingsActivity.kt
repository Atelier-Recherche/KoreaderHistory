package fr.atelier.einklauncher

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import fr.atelier.koreaderhistory.data.LauncherPrefs
import fr.atelier.koreaderhistory.data.WidgetPrefs

class LauncherSettingsActivity : AppCompatActivity() {
    private lateinit var launcherPrefs: LauncherPrefs
    private lateinit var widgetPrefs: WidgetPrefs
    private val sizes = listOf(32, 44, 48, 56, 72, 96)
    private val columns = listOf(3, 4, 5, 6)
    private val fontSizes = listOf(10, 11, 12, 13, 14, 15, 16, 18)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launcherPrefs = LauncherPrefs(this)
        widgetPrefs = WidgetPrefs(this)

        val scroll = ScrollView(this).apply { setBackgroundColor(0xFFFFFFFF.toInt()) }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(48), dp(16), dp(24))
        }
        scroll.addView(root)

        root.addView(header("KOReader"))
        val path = EditText(this).apply {
            setText(widgetPrefs.koreaderPath)
            hint = getString(R.string.koreader_path)
            setTextColor(0xFF000000.toInt())
        }
        root.addView(path)

        root.addView(header(getString(R.string.edit_launcher_dock)))
        val dockEnabled = check(getString(R.string.dock_enabled_launcher), launcherPrefs.dockEnabled)
        root.addView(dockEnabled)
        val dockSize = sizeSpinner(launcherPrefs.dockIconSizeDp)
        root.addView(label(getString(R.string.dock_icon_size)))
        root.addView(dockSize)
        val dockBorder = check(getString(R.string.dock_icon_border), launcherPrefs.dockIconBorder)
        root.addView(dockBorder)

        root.addView(header("Bureau"))
        val desktopCols = columnSpinner(launcherPrefs.desktopColumns)
        root.addView(label(getString(R.string.desktop_columns)))
        root.addView(desktopCols)
        val desktopSize = sizeSpinner(launcherPrefs.desktopIconSizeDp)
        root.addView(label(getString(R.string.desktop_icon_size)))
        root.addView(desktopSize)
        val desktopBorder = check(getString(R.string.desktop_icon_border), launcherPrefs.desktopIconBorder)
        root.addView(desktopBorder)
        val desktopLabel = fontSpinner(launcherPrefs.desktopLabelSp)
        root.addView(label(getString(R.string.desktop_label_size)))
        root.addView(desktopLabel)

        root.addView(header("Tiroir d’apps"))
        val drawerCols = columnSpinner(launcherPrefs.drawerColumns)
        root.addView(label(getString(R.string.drawer_columns)))
        root.addView(drawerCols)
        val drawerSize = sizeSpinner(launcherPrefs.drawerIconSizeDp)
        root.addView(label(getString(R.string.drawer_icon_size)))
        root.addView(drawerSize)
        val drawerBorder = check(getString(R.string.drawer_icon_border), launcherPrefs.drawerIconBorder)
        root.addView(drawerBorder)
        val drawerLabel = fontSpinner(launcherPrefs.drawerLabelSp)
        root.addView(label(getString(R.string.drawer_label_size)))
        root.addView(drawerLabel)

        root.addView(Button(this).apply {
            text = getString(R.string.manage_hidden_apps)
            setOnClickListener { showHiddenAppsDialog() }
        })
        root.addView(Button(this).apply {
            text = getString(R.string.manage_icon_overrides)
            setOnClickListener { showIconOverridesDialog() }
        })

        root.addView(header(getString(R.string.status_bar_section)))
        val statusEnabled = check(getString(R.string.status_bar_enabled), launcherPrefs.statusBarEnabled)
        root.addView(statusEnabled)
        val statusFont = fontSpinner(launcherPrefs.statusBarTextSp)
        root.addView(label(getString(R.string.status_bar_font_size)))
        root.addView(statusFont)
        val statusTime = check(getString(R.string.status_bar_time), launcherPrefs.statusBarShowTime)
        root.addView(statusTime)
        val statusDate = check(getString(R.string.status_bar_date), launcherPrefs.statusBarShowDate)
        root.addView(statusDate)
        val dateFormats = listOf(
            LauncherPrefs.DATE_FMT_WEEKDAY_DATE,
            LauncherPrefs.DATE_FMT_WEEKDAY,
            LauncherPrefs.DATE_FMT_MEDIUM,
            LauncherPrefs.DATE_FMT_FULL,
        )
        val dateFormatLabels = listOf(
            getString(R.string.date_fmt_weekday_date),
            getString(R.string.date_fmt_weekday),
            getString(R.string.date_fmt_medium),
            getString(R.string.date_fmt_full),
        )
        val statusDateFmt = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@LauncherSettingsActivity,
                android.R.layout.simple_spinner_dropdown_item,
                dateFormatLabels,
            )
            setSelection(dateFormats.indexOf(launcherPrefs.statusBarDateFormat).coerceAtLeast(0))
        }
        root.addView(label(getString(R.string.status_bar_date_format)))
        root.addView(statusDateFmt)
        val statusWifi = check(getString(R.string.status_bar_wifi), launcherPrefs.statusBarShowWifi)
        root.addView(statusWifi)
        val statusBt = check(getString(R.string.status_bar_bluetooth), launcherPrefs.statusBarShowBluetooth)
        root.addView(statusBt)
        val statusBat = check(getString(R.string.status_bar_battery), launcherPrefs.statusBarShowBattery)
        root.addView(statusBat)

        root.addView(header(getString(R.string.history_panel_label)))
        val historyFont = fontSpinner(launcherPrefs.historyTitleSp)
        root.addView(label(getString(R.string.history_title_size)))
        root.addView(historyFont)

        root.addView(Button(this).apply {
            text = getString(R.string.save)
            setOnClickListener {
                widgetPrefs.koreaderPath =
                    path.text?.toString().orEmpty().ifBlank { WidgetPrefs.DEFAULT_PATH }
                launcherPrefs.dockEnabled = dockEnabled.isChecked
                launcherPrefs.dockIconSizeDp = sizes.getOrElse(dockSize.selectedItemPosition) { 48 }
                launcherPrefs.dockIconBorder = dockBorder.isChecked
                launcherPrefs.desktopColumns = columns.getOrElse(desktopCols.selectedItemPosition) { 4 }
                launcherPrefs.desktopIconSizeDp = sizes.getOrElse(desktopSize.selectedItemPosition) { 56 }
                launcherPrefs.desktopIconBorder = desktopBorder.isChecked
                launcherPrefs.desktopLabelSp = fontSizes.getOrElse(desktopLabel.selectedItemPosition) { 12 }
                launcherPrefs.drawerColumns = columns.getOrElse(drawerCols.selectedItemPosition) { 4 }
                launcherPrefs.drawerIconSizeDp = sizes.getOrElse(drawerSize.selectedItemPosition) { 56 }
                launcherPrefs.drawerIconBorder = drawerBorder.isChecked
                launcherPrefs.drawerLabelSp = fontSizes.getOrElse(drawerLabel.selectedItemPosition) { 12 }
                launcherPrefs.statusBarEnabled = statusEnabled.isChecked
                launcherPrefs.statusBarTextSp = fontSizes.getOrElse(statusFont.selectedItemPosition) { 13 }
                launcherPrefs.statusBarShowTime = statusTime.isChecked
                launcherPrefs.statusBarShowDate = statusDate.isChecked
                launcherPrefs.statusBarDateFormat =
                    dateFormats.getOrElse(statusDateFmt.selectedItemPosition) {
                        LauncherPrefs.DATE_FMT_WEEKDAY_DATE
                    }
                launcherPrefs.statusBarShowWifi = statusWifi.isChecked
                launcherPrefs.statusBarShowBluetooth = statusBt.isChecked
                launcherPrefs.statusBarShowBattery = statusBat.isChecked
                launcherPrefs.historyTitleSp = fontSizes.getOrElse(historyFont.selectedItemPosition) { 14 }
                Toast.makeText(this@LauncherSettingsActivity, R.string.save, Toast.LENGTH_SHORT).show()
                finish()
            }
        })

        root.addView(Button(this).apply {
            text = getString(R.string.edit_launcher_dock)
            setOnClickListener {
                startActivity(Intent(this@LauncherSettingsActivity, LauncherDockEditorActivity::class.java))
            }
        })
        root.addView(Button(this).apply {
            text = getString(R.string.grant_storage)
            setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    startActivity(
                        Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                            data = Uri.parse("package:$packageName")
                        },
                    )
                }
            }
        })
        root.addView(Button(this).apply {
            text = getString(R.string.set_as_home)
            setOnClickListener { startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
        })
        root.addView(Button(this).apply {
            text = getString(R.string.enable_accessibility)
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        })
        root.addView(Button(this).apply {
            text = getString(R.string.enable_notification_access)
            setOnClickListener { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        })
        root.addView(Button(this).apply {
            text = getString(R.string.enable_usage_access)
            setOnClickListener { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
        })

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            Toast.makeText(this, R.string.need_permission, Toast.LENGTH_LONG).show()
        }

        setContentView(scroll)
    }

    private fun showHiddenAppsDialog() {
        val hidden = launcherPrefs.hiddenApps.toList().sorted()
        if (hidden.isEmpty()) {
            Toast.makeText(this, R.string.no_hidden_apps, Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.manage_hidden_apps)
            .setItems(hidden.toTypedArray()) { _, which ->
                val key = hidden[which]
                AlertDialog.Builder(this)
                    .setTitle(key)
                    .setItems(arrayOf(getString(R.string.unhide_app))) { _, _ ->
                        launcherPrefs.unhideApp(key)
                        Toast.makeText(this, R.string.unhide_app, Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showIconOverridesDialog() {
        val keys = launcherPrefs.listAppIconOverrideKeys()
        if (keys.isEmpty()) {
            Toast.makeText(this, R.string.no_icon_overrides, Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.manage_icon_overrides)
            .setItems(keys.toTypedArray()) { _, which ->
                val key = keys[which]
                AlertDialog.Builder(this)
                    .setTitle(key)
                    .setItems(arrayOf(getString(R.string.reset_app_icon_override))) { _, _ ->
                        launcherPrefs.clearAppIconOverride(key)
                        Toast.makeText(this, R.string.reset_app_icon_override, Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun header(text: String): TextView = TextView(this).apply {
        this.text = text
        setTextColor(0xFF000000.toInt())
        textSize = 18f
        setPadding(0, dp(16), 0, dp(8))
        setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun label(text: String): TextView = TextView(this).apply {
        this.text = text
        setTextColor(0xFF000000.toInt())
        setPadding(0, dp(8), 0, dp(2))
    }

    private fun check(text: String, checked: Boolean): CheckBox = CheckBox(this).apply {
        this.text = text
        setTextColor(0xFF000000.toInt())
        isChecked = checked
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
    }

    private fun sizeSpinner(current: Int): Spinner = Spinner(this).apply {
        adapter = ArrayAdapter(
            this@LauncherSettingsActivity,
            android.R.layout.simple_spinner_dropdown_item,
            sizes.map { "${it}dp" },
        )
        setSelection(sizes.indexOf(current).let { if (it >= 0) it else 2 })
    }

    private fun columnSpinner(current: Int): Spinner = Spinner(this).apply {
        adapter = ArrayAdapter(
            this@LauncherSettingsActivity,
            android.R.layout.simple_spinner_dropdown_item,
            columns.map { it.toString() },
        )
        setSelection(columns.indexOf(current).let { if (it >= 0) it else 1 })
    }

    private fun fontSpinner(current: Int): Spinner = Spinner(this).apply {
        adapter = ArrayAdapter(
            this@LauncherSettingsActivity,
            android.R.layout.simple_spinner_dropdown_item,
            fontSizes.map { "${it}sp" },
        )
        setSelection(fontSizes.indexOf(current).let { if (it >= 0) it else 2 })
    }

    private fun dp(v: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        v.toFloat(),
        resources.displayMetrics,
    ).toInt()
}
