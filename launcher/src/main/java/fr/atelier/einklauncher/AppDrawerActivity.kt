package fr.atelier.einklauncher

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import fr.atelier.koreaderhistory.data.BooxFreezeHelper
import fr.atelier.koreaderhistory.data.DesktopItem
import fr.atelier.koreaderhistory.data.KoreaderLauncher
import fr.atelier.koreaderhistory.data.LauncherApp
import fr.atelier.koreaderhistory.data.LauncherPrefs

class AppDrawerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAppDrawerBinding
    private lateinit var prefs: LauncherPrefs
    private lateinit var adapter: AppAdapter
    private var allApps: List<LauncherApp> = emptyList()
    private var pendingIconApp: LauncherApp? = null

    private val pickIcon = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val app = pendingIconApp ?: return@registerForActivityResult
        pendingIconApp = null
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        val data = result.data ?: return@registerForActivityResult
        val ok = prefs.pinAppToDock(
            app,
            iconPackage = data.getStringExtra(IconPickerActivity.EXTRA_ICON_PACKAGE).orEmpty().ifBlank { app.packageName },
            iconKey = data.getStringExtra(IconPickerActivity.EXTRA_ICON_KEY) ?: "app",
            iconPackPackage = data.getStringExtra(IconPickerActivity.EXTRA_ICON_PACK).orEmpty(),
            iconDrawableName = data.getStringExtra(IconPickerActivity.EXTRA_ICON_DRAWABLE).orEmpty(),
        )
        Toast.makeText(
            this,
            if (ok) getString(R.string.pinned_to_dock) else getString(R.string.dock_max_slots, LauncherPrefs.MAX_DOCK_SLOTS),
            Toast.LENGTH_SHORT,
        ).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppDrawerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = LauncherPrefs(this)

        adapter = AppAdapter(
            onClick = { app -> launchApp(app) },
            onLongClick = { app -> showAppMenu(app); true },
        )
        binding.appGrid.layoutManager = GridLayoutManager(this, 4)
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
        adapter.submit(
            if (q.isBlank()) allApps
            else allApps.filter {
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
                        pickIcon.launch(Intent(this, IconPickerActivity::class.java))
                    }
                    2 -> addToDesktop(app)
                    3 -> addToFolder(app)
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
            holder.bind(items[position], onClick, onLongClick)
        }

        class VH(private val binding: ItemDesktopAppBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(
                app: LauncherApp,
                onClick: (LauncherApp) -> Unit,
                onLongClick: (LauncherApp) -> Boolean,
            ) {
                val frozen = BooxFreezeHelper.isFrozen(binding.root.context, app.packageName)
                binding.appLabel.text = if (frozen) "❄ ${app.label}" else app.label
                binding.root.alpha = if (frozen) 0.55f else 1f
                binding.appIcon.setImageDrawable(
                    AppIconLoader.loadActivityDrawable(binding.root.context, app.packageName, app.activityName)
                        ?: AppIconLoader.loadDrawable(binding.root.context, app.packageName),
                )
                binding.root.setOnClickListener { onClick(app) }
                binding.root.setOnLongClickListener { onLongClick(app) }
            }
        }
    }
}
