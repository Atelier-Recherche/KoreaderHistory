package fr.atelier.einklauncher

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import fr.atelier.einklauncher.databinding.ActivityAppDrawerBinding
import fr.atelier.einklauncher.databinding.ItemDesktopAppBinding
import fr.atelier.koreaderhistory.data.AppIconLoader
import fr.atelier.koreaderhistory.data.AppIconOverride
import fr.atelier.koreaderhistory.data.BooxFreezeHelper
import fr.atelier.koreaderhistory.data.DesktopItem
import fr.atelier.koreaderhistory.data.DockIconResolver
import fr.atelier.koreaderhistory.data.KoreaderLauncher
import fr.atelier.koreaderhistory.data.LauncherApp
import fr.atelier.koreaderhistory.data.LauncherPrefs

class AppDrawerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAppDrawerBinding
    private lateinit var prefs: LauncherPrefs
    private lateinit var adapter: AppAdapter
    private var allApps: List<LauncherApp> = emptyList()
    private var pendingIconApp: LauncherApp? = null
    private var pendingIconMode: IconMode = IconMode.OVERRIDE

    private enum class IconMode { OVERRIDE, PIN_DOCK }

    private val pickIcon = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val app = pendingIconApp ?: return@registerForActivityResult
        pendingIconApp = null
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        val data = result.data ?: return@registerForActivityResult
        val override = AppIconOverride(
            iconKey = data.getStringExtra(IconPickerActivity.EXTRA_ICON_KEY) ?: "app",
            iconPackage = data.getStringExtra(IconPickerActivity.EXTRA_ICON_PACKAGE).orEmpty()
                .ifBlank { app.packageName },
            iconPackPackage = data.getStringExtra(IconPickerActivity.EXTRA_ICON_PACK).orEmpty(),
            iconDrawableName = data.getStringExtra(IconPickerActivity.EXTRA_ICON_DRAWABLE).orEmpty(),
        )
        when (pendingIconMode) {
            IconMode.OVERRIDE -> {
                prefs.setAppIconOverride(app.componentKey, override)
                applyFilter()
                Toast.makeText(this, R.string.choose_icon, Toast.LENGTH_SHORT).show()
            }
            IconMode.PIN_DOCK -> {
                val ok = prefs.pinAppToDock(
                    app,
                    iconPackage = override.iconPackage.ifBlank { app.packageName },
                    iconKey = override.iconKey,
                    iconPackPackage = override.iconPackPackage,
                    iconDrawableName = override.iconDrawableName,
                )
                Toast.makeText(
                    this,
                    if (ok) getString(R.string.pinned_to_dock)
                    else getString(R.string.dock_max_slots, LauncherPrefs.MAX_DOCK_SLOTS),
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppDrawerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = LauncherPrefs(this)

        adapter = AppAdapter(
            iconSizeDp = { prefs.drawerIconSizeDp },
            iconBorder = { prefs.drawerIconBorder },
            labelSp = { prefs.drawerLabelSp },
            resolveOverride = { app -> prefs.getAppIconOverride(app.componentKey) },
            onClick = { app -> launchApp(app) },
            onLongClick = { app -> showAppMenu(app); true },
        )
        binding.appGrid.layoutManager = GridLayoutManager(this, prefs.drawerColumns)
        binding.appGrid.adapter = adapter
        binding.inputSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) = applyFilter()
        })

        allApps = AppIconLoader.listLauncherApps(this)
        val frozen = allApps.count { BooxFreezeHelper.isFrozen(this, it.packageName) }
        WidgetLog.file(this, "AppDrawer apps=${allApps.size} frozen=$frozen")
        applyFilter()
    }

    override fun onResume() {
        super.onResume()
        (binding.appGrid.layoutManager as? GridLayoutManager)?.spanCount = prefs.drawerColumns
        applyFilter()
    }

    private fun launchApp(app: LauncherApp) {
        if (KoreaderLauncher.launchApp(this, app.packageName, app.activityName)) return
        if (BooxFreezeHelper.isFrozen(this, app.packageName)) {
            AlertDialog.Builder(this)
                .setTitle(app.label)
                .setMessage(getString(R.string.app_frozen_message))
                .setPositiveButton(R.string.open_app_freeze) { _, _ ->
                    BooxFreezeHelper.openFreezeSettings(this)
                }
                .setNeutralButton(R.string.app_details) { _, _ ->
                    BooxFreezeHelper.openAppDetails(this, app.packageName)
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        } else {
            Toast.makeText(this, R.string.catalog_empty, Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyFilter() {
        val q = binding.inputSearch.text?.toString()?.trim()?.lowercase().orEmpty()
        val visible = allApps.filter { !prefs.isAppHidden(it.componentKey) }
        adapter.submit(
            if (q.isBlank()) visible
            else visible.filter {
                it.label.lowercase().contains(q) ||
                    it.packageName.lowercase().contains(q) ||
                    it.activityName.lowercase().contains(q)
            },
        )
    }

    private fun showAppMenu(app: LauncherApp) {
        val docked = prefs.isDocked(app.packageName, app.activityName)
        val options = arrayOf(
            if (docked) getString(R.string.launcher_unpin_dock) else getString(R.string.launcher_pin_dock),
            getString(R.string.pin_dock_with_icon),
            getString(R.string.choose_icon),
            getString(R.string.hide_app),
            getString(R.string.add_to_desktop),
            getString(R.string.add_to_folder),
        )
        AlertDialog.Builder(this)
            .setTitle(app.label)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        if (docked) {
                            prefs.unpinAppFromDock(app.packageName, app.activityName)
                            Toast.makeText(this, R.string.unpinned_from_dock, Toast.LENGTH_SHORT).show()
                        } else {
                            val ok = prefs.pinAppToDock(app)
                            Toast.makeText(
                                this,
                                if (ok) getString(R.string.pinned_to_dock)
                                else getString(R.string.dock_max_slots, LauncherPrefs.MAX_DOCK_SLOTS),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
                    1 -> {
                        pendingIconApp = app
                        pendingIconMode = IconMode.PIN_DOCK
                        pickIcon.launch(Intent(this, IconPickerActivity::class.java))
                    }
                    2 -> {
                        pendingIconApp = app
                        pendingIconMode = IconMode.OVERRIDE
                        pickIcon.launch(Intent(this, IconPickerActivity::class.java))
                    }
                    3 -> {
                        prefs.hideApp(app.componentKey)
                        applyFilter()
                        Toast.makeText(this, R.string.hide_app, Toast.LENGTH_SHORT).show()
                    }
                    4 -> addToDesktop(app)
                    5 -> addToFolder(app)
                }
            }
            .show()
    }

    private fun addToDesktop(app: LauncherApp) {
        val items = prefs.getDesktopItems().toMutableList()
        if (items.any { it.type == "app" && it.packageName == app.packageName && it.activityName == app.activityName }) {
            Toast.makeText(this, "Déjà sur le bureau", Toast.LENGTH_SHORT).show()
            return
        }
        items.add(
            DesktopItem(
                id = LauncherPrefs.newId(),
                type = "app",
                label = app.label,
                packageName = app.packageName,
                activityName = app.activityName,
                cellIndex = prefs.nextFreeCellIndex(),
            ),
        )
        prefs.setDesktopItems(items)
        Toast.makeText(this, "Ajouté", Toast.LENGTH_SHORT).show()
    }

    private fun addToFolder(app: LauncherApp) {
        val folders = prefs.getDesktopItems().filter { it.type == "folder" }
        if (folders.isEmpty()) {
            Toast.makeText(this, "Aucun dossier — créez-en un depuis le bureau", Toast.LENGTH_LONG).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.launcher_folder)
            .setItems(folders.map { it.label }.toTypedArray()) { _, which ->
                val folder = folders[which]
                val items = prefs.getDesktopItems().toMutableList()
                val idx = items.indexOfFirst { it.id == folder.id }
                if (idx < 0) return@setItems
                val key = app.componentKey
                val children = folder.children.toMutableList()
                if (key !in children && app.packageName !in children) children.add(key)
                items[idx] = folder.copy(children = children)
                prefs.setDesktopItems(items)
                Toast.makeText(this, "Ajouté au dossier", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private class AppAdapter(
        private val iconSizeDp: () -> Int,
        private val iconBorder: () -> Boolean,
        private val labelSp: () -> Int,
        private val resolveOverride: (LauncherApp) -> AppIconOverride?,
        private val onClick: (LauncherApp) -> Unit,
        private val onLongClick: (LauncherApp) -> Boolean,
    ) : RecyclerView.Adapter<AppAdapter.VH>() {
        private var items: List<LauncherApp> = emptyList()

        fun submit(list: List<LauncherApp>) {
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
                resolveOverride,
                onClick,
                onLongClick,
            )
        }

        class VH(private val binding: ItemDesktopAppBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(
                app: LauncherApp,
                sizeDp: Int,
                border: Boolean,
                labelSp: Int,
                resolveOverride: (LauncherApp) -> AppIconOverride?,
                onClick: (LauncherApp) -> Unit,
                onLongClick: (LauncherApp) -> Boolean,
            ) {
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

                val frozen = BooxFreezeHelper.isFrozen(binding.root.context, app.packageName)
                binding.appLabel.text = if (frozen) "❄ ${app.label}" else app.label
                binding.appLabel.textSize = labelSp.toFloat()
                binding.root.alpha = if (frozen) 0.55f else 1f
                val ov = resolveOverride(app)
                if (ov != null) {
                    binding.appIcon.setImageDrawable(
                        DockIconResolver.loadDrawable(
                            binding.root.context,
                            ov.toDockSlot(label = app.label),
                        ),
                    )
                } else {
                    binding.appIcon.setImageDrawable(
                        AppIconLoader.loadActivityDrawable(binding.root.context, app.packageName, app.activityName)
                            ?: AppIconLoader.loadDrawable(binding.root.context, app.packageName),
                    )
                }
                binding.root.setOnClickListener { onClick(app) }
                binding.root.setOnLongClickListener { onLongClick(app) }
            }
        }
    }
}
