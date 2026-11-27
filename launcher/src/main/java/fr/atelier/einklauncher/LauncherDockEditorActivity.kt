package fr.atelier.einklauncher

import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import fr.atelier.einklauncher.databinding.ItemDesktopAppBinding
import fr.atelier.koreaderhistory.data.AppIconLoader
import fr.atelier.koreaderhistory.data.DockActionType
import fr.atelier.koreaderhistory.data.DockIconResolver
import fr.atelier.koreaderhistory.data.DockSlot
import fr.atelier.koreaderhistory.data.LauncherPrefs

class LauncherDockEditorActivity : AppCompatActivity() {
    private lateinit var prefs: LauncherPrefs
    private lateinit var adapter: Adapter
    private lateinit var sizeSpinner: Spinner
    private lateinit var borderCheck: CheckBox
    private lateinit var justifySpinner: Spinner
    private val sizes = listOf(32, 44, 48, 56, 72)
    private val justifyModes = listOf(LauncherPrefs.JUSTIFY_CENTER, LauncherPrefs.JUSTIFY_SPACE_BETWEEN)

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

        sizeSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@LauncherDockEditorActivity,
                android.R.layout.simple_spinner_dropdown_item,
                sizes.map { "${it}dp" },
            )
            setSelection(sizes.indexOf(prefs.dockIconSizeDp).let { if (it >= 0) it else 2 })
        }
        header.addView(TextView(this).apply {
            text = getString(R.string.dock_icon_size)
            setTextColor(0xFF000000.toInt())
        })
        header.addView(sizeSpinner)

        borderCheck = CheckBox(this).apply {
            text = getString(R.string.dock_icon_border)
            setTextColor(0xFF000000.toInt())
            isChecked = prefs.dockIconBorder
        }
        header.addView(borderCheck)

        justifySpinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@LauncherDockEditorActivity,
                android.R.layout.simple_spinner_dropdown_item,
                listOf(getString(R.string.dock_justify_center), getString(R.string.dock_justify_space)),
            )
            setSelection(justifyModes.indexOf(prefs.dockJustify).coerceAtLeast(0))
        }
        header.addView(TextView(this).apply {
            text = getString(R.string.dock_justify)
            setTextColor(0xFF000000.toInt())
        })
        header.addView(justifySpinner)

        header.addView(
            MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                text = getString(R.string.save_dock_options)
                setOnClickListener { saveOptions(finishAfter = true) }
            },
        )
        root.addView(header)

        val list = RecyclerView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            layoutManager = LinearLayoutManager(this@LauncherDockEditorActivity)
            setPadding(dp(8), 0, dp(8), 0)
        }
        adapter = Adapter(
            onClick = { slot ->
                startActivity(
                    Intent(this, LauncherDockSlotEditActivity::class.java)
                        .putExtra(LauncherDockSlotEditActivity.EXTRA_SLOT_ID, slot.id),
                )
            },
            onDelete = { slot ->
                prefs.removeDockSlot(slot.id)
                reload()
            },
        )
        list.adapter = adapter
        root.addView(list)

        root.addView(
            MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                text = getString(R.string.add_dock_item)
                setOnClickListener { showAddMenu() }
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).also { it.setMargins(dp(16), dp(8), dp(16), dp(16)) }
            },
        )

        setContentView(root)
        reload()
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    private fun saveOptions(finishAfter: Boolean) {
        prefs.dockIconSizeDp = sizes.getOrElse(sizeSpinner.selectedItemPosition) { 48 }
        prefs.dockIconBorder = borderCheck.isChecked
        prefs.dockJustify = justifyModes.getOrElse(justifySpinner.selectedItemPosition) {
            LauncherPrefs.JUSTIFY_CENTER
        }
        // commit synchrone pour éviter une perte si on revient trop vite
        Toast.makeText(this, R.string.dock_options_saved, Toast.LENGTH_SHORT).show()
        if (finishAfter) finish()
    }

    private fun reload() {
        adapter.submit(prefs.getDockSlots())
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
                val app = apps[which]
                prefs.pinAppToDock(app)
                reload()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
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
