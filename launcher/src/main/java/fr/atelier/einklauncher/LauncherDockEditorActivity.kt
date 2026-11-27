package fr.atelier.einklauncher

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import fr.atelier.einklauncher.databinding.ItemDesktopAppBinding
import fr.atelier.koreaderhistory.data.AppIconLoader
import fr.atelier.koreaderhistory.data.AppIconOverride
import fr.atelier.koreaderhistory.data.DockActionType
import fr.atelier.koreaderhistory.data.DockIconResolver
import fr.atelier.koreaderhistory.data.DockSlot
import fr.atelier.koreaderhistory.data.LauncherPrefs

class LauncherDockEditorActivity : AppCompatActivity() {
    private lateinit var prefs: LauncherPrefs
    private lateinit var listAdapter: Adapter
    private lateinit var sizeSpinner: Spinner
    private lateinit var enabledCheck: CheckBox
    private lateinit var borderCheck: CheckBox
    private lateinit var justifySpinner: Spinner
    private lateinit var statusView: TextView
    private var suppressAutoSave = true
    private var pendingBuiltin: BuiltinIconTarget = BuiltinIconTarget.APPS
    private val sizes = listOf(32, 44, 48, 56, 72)
    private val justifyModes = listOf(LauncherPrefs.JUSTIFY_CENTER, LauncherPrefs.JUSTIFY_SPACE_BETWEEN)

    private enum class BuiltinIconTarget { APPS, NOTIFS }

    private val pickBuiltinIcon = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        val data = result.data ?: return@registerForActivityResult
        val override = AppIconOverride(
            iconKey = data.getStringExtra(IconPickerActivity.EXTRA_ICON_KEY) ?: "app",
            iconPackage = data.getStringExtra(IconPickerActivity.EXTRA_ICON_PACKAGE).orEmpty(),
            iconPackPackage = data.getStringExtra(IconPickerActivity.EXTRA_ICON_PACK).orEmpty(),
            iconDrawableName = data.getStringExtra(IconPickerActivity.EXTRA_ICON_DRAWABLE).orEmpty(),
        )
        when (pendingBuiltin) {
            BuiltinIconTarget.APPS -> prefs.setDockAppsIcon(override)
            BuiltinIconTarget.NOTIFS -> prefs.setDockNotifsIcon(override)
        }
        Toast.makeText(this, R.string.choose_icon, Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = LauncherPrefs(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFFFFFFF.toInt())
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(48), dp(16), dp(8))
        }
        header.addView(TextView(this).apply {
            text = getString(R.string.edit_launcher_dock)
            setTextColor(0xFF000000.toInt())
            textSize = 20f
            setPadding(0, 0, 0, dp(12))
        })

        enabledCheck = CheckBox(this).apply {
            text = getString(R.string.dock_enabled_launcher)
            setTextColor(0xFF000000.toInt())
        }
        header.addView(enabledCheck)

        sizeSpinner = Spinner(this).apply {
            this.adapter = ArrayAdapter(
                this@LauncherDockEditorActivity,
                android.R.layout.simple_spinner_dropdown_item,
                sizes.map { "${it}dp" },
            )
        }
        header.addView(label(getString(R.string.dock_icon_size)))
        header.addView(sizeSpinner)

        borderCheck = CheckBox(this).apply {
            text = getString(R.string.dock_icon_border)
            setTextColor(0xFF000000.toInt())
        }
        header.addView(borderCheck)

        justifySpinner = Spinner(this).apply {
            this.adapter = ArrayAdapter(
                this@LauncherDockEditorActivity,
                android.R.layout.simple_spinner_dropdown_item,
                listOf(getString(R.string.dock_justify_center), getString(R.string.dock_justify_space)),
            )
        }
        header.addView(label(getString(R.string.dock_justify)))
        header.addView(justifySpinner)

        statusView = TextView(this).apply {
            setTextColor(0xFF444444.toInt())
            textSize = 12f
            setPadding(0, dp(8), 0, dp(4))
        }
        header.addView(statusView)

        header.addView(Button(this).apply {
            text = getString(R.string.dock_apps_icon)
            setOnClickListener {
                pendingBuiltin = BuiltinIconTarget.APPS
                pickBuiltinIcon.launch(Intent(this@LauncherDockEditorActivity, IconPickerActivity::class.java))
            }
        })
        header.addView(Button(this).apply {
            text = getString(R.string.dock_notifs_icon)
            setOnClickListener {
                pendingBuiltin = BuiltinIconTarget.NOTIFS
                pickBuiltinIcon.launch(Intent(this@LauncherDockEditorActivity, IconPickerActivity::class.java))
            }
        })

        header.addView(Button(this).apply {
            text = getString(R.string.save_dock_options)
            setOnClickListener { persistOptions(showToast = true, finishAfter = true) }
        })
        header.addView(Button(this).apply {
            text = getString(android.R.string.cancel)
            setOnClickListener { finish() }
        })
        root.addView(header)

        val list = RecyclerView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            layoutManager = LinearLayoutManager(this@LauncherDockEditorActivity)
            setPadding(dp(8), 0, dp(8), 0)
        }
        listAdapter = Adapter(
            onClick = { slot ->
                startActivity(
                    Intent(this, LauncherDockSlotEditActivity::class.java)
                        .putExtra(LauncherDockSlotEditActivity.EXTRA_SLOT_ID, slot.id),
                )
            },
            onDelete = { slot ->
                prefs.removeDockSlot(slot.id)
                reloadSlots()
            },
        )
        list.adapter = listAdapter
        root.addView(list)

        root.addView(Button(this).apply {
            text = getString(R.string.add_dock_item)
            setOnClickListener { showAddMenu() }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).also { it.setMargins(dp(16), dp(8), dp(16), dp(16)) }
        })

        setContentView(root)
        bindFromPrefs()
        wireAutoSave()
        reloadSlots()
        suppressAutoSave = false
    }

    override fun onResume() {
        super.onResume()
        reloadSlots()
        statusView.text = "Actuel : ${prefs.readDockVisualDebug()}"
    }

    private fun bindFromPrefs() {
        suppressAutoSave = true
        enabledCheck.isChecked = prefs.dockEnabled
        sizeSpinner.setSelection(sizes.indexOf(prefs.dockIconSizeDp).let { if (it >= 0) it else 2 })
        borderCheck.isChecked = prefs.dockIconBorder
        justifySpinner.setSelection(justifyModes.indexOf(prefs.dockJustify).coerceAtLeast(0))
        statusView.text = "Actuel : ${prefs.readDockVisualDebug()}"
        suppressAutoSave = false
    }

    private fun wireAutoSave() {
        val listener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!suppressAutoSave) persistOptions(showToast = false, finishAfter = false)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        sizeSpinner.onItemSelectedListener = listener
        justifySpinner.onItemSelectedListener = listener
        val checkListener = { _: android.widget.CompoundButton, _: Boolean ->
            if (!suppressAutoSave) persistOptions(showToast = false, finishAfter = false)
        }
        enabledCheck.setOnCheckedChangeListener(checkListener)
        borderCheck.setOnCheckedChangeListener(checkListener)
    }

    private fun persistOptions(showToast: Boolean, finishAfter: Boolean) {
        val size = sizes.getOrElse(sizeSpinner.selectedItemPosition) { 48 }
        val border = borderCheck.isChecked
        val enabled = enabledCheck.isChecked
        val justify = justifyModes.getOrElse(justifySpinner.selectedItemPosition) {
            LauncherPrefs.JUSTIFY_CENTER
        }
        val ok = prefs.saveDockVisualOptions(size, border, justify, enabled)
        WidgetLog.file(this, "dock visual save ok=$ok → ${prefs.readDockVisualDebug()}")
        statusView.text = if (ok) {
            "Enregistré : ${prefs.readDockVisualDebug()}"
        } else {
            "Échec sauvegarde prefs"
        }
        if (showToast) {
            Toast.makeText(
                this,
                if (ok) getString(R.string.dock_options_saved) + " (${prefs.dockIconSizeDp}dp)"
                else "Échec sauvegarde",
                Toast.LENGTH_SHORT,
            ).show()
        }
        if (finishAfter && ok) {
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun reloadSlots() {
        listAdapter.submit(prefs.getDockSlots())
    }

    private fun showAddMenu() {
        if (prefs.getDockSlots().size >= LauncherPrefs.MAX_DOCK_SLOTS) {
            Toast.makeText(
                this,
                getString(R.string.dock_max_slots, LauncherPrefs.MAX_DOCK_SLOTS),
                Toast.LENGTH_SHORT,
            ).show()
            return
        }
        val choices = arrayOf(
            getString(R.string.add_dock_app),
            DockActionType.OPEN_KOREADER.label,
            DockActionType.CONTINUE.label,
            DockActionType.HISTORY.label,
            DockActionType.OPEN_FOLDER.label,
            DockActionType.OPEN_COLLECTION.label,
            DockActionType.OPEN_PLUGIN.label,
            DockActionType.SYSTEM_ACTION.label,
            DockActionType.OPEN_BOOK.label,
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.add_dock_item)
            .setItems(choices) { _, which ->
                when (which) {
                    0 -> pickAppToAdd()
                    1 -> addActionSlot(DockActionType.OPEN_KOREADER, "koreader")
                    2 -> addActionSlot(DockActionType.CONTINUE, "continue")
                    3 -> addActionSlot(DockActionType.HISTORY, "history")
                    4 -> addActionSlot(DockActionType.OPEN_FOLDER, "folder")
                    5 -> addActionSlot(DockActionType.OPEN_COLLECTION, "library")
                    6 -> addActionSlot(DockActionType.OPEN_PLUGIN, "plugin")
                    7 -> addActionSlot(DockActionType.SYSTEM_ACTION, "dispatcher")
                    8 -> addActionSlot(DockActionType.OPEN_BOOK, "book")
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun addActionSlot(type: DockActionType, iconKey: String) {
        val slot = DockSlot(
            id = LauncherPrefs.newSlotId(),
            label = type.label,
            iconKey = iconKey,
            actionType = type,
        )
        prefs.upsertDockSlot(slot)
        startActivity(
            Intent(this, LauncherDockSlotEditActivity::class.java)
                .putExtra(LauncherDockSlotEditActivity.EXTRA_SLOT_ID, slot.id),
        )
    }

    private fun pickAppToAdd() {
        val apps = AppIconLoader.listLauncherApps(this)
        AlertDialog.Builder(this)
            .setTitle(R.string.add_dock_app)
            .setItems(apps.map { it.label }.toTypedArray()) { _, which ->
                prefs.pinAppToDock(apps[which])
                reloadSlots()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun label(text: String): TextView = TextView(this).apply {
        this.text = text
        setTextColor(0xFF000000.toInt())
    }

    private fun dp(v: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        v.toFloat(),
        resources.displayMetrics,
    ).toInt()

    private class Adapter(
        private val onClick: (DockSlot) -> Unit,
        private val onDelete: (DockSlot) -> Unit,
    ) : RecyclerView.Adapter<Adapter.VH>() {
        private var items: List<DockSlot> = emptyList()

        fun submit(list: List<DockSlot>) {
            items = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val b = ItemDesktopAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return VH(b)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val slot = items[position]
            holder.b.appLabel.text = "${slot.label}\n${slot.actionType.label}"
            holder.b.appIcon.setImageDrawable(DockIconResolver.loadDrawable(holder.b.root.context, slot))
            holder.b.root.setOnClickListener { onClick(slot) }
            holder.b.root.setOnLongClickListener { onDelete(slot); true }
        }

        class VH(val b: ItemDesktopAppBinding) : RecyclerView.ViewHolder(b.root)
    }
}
