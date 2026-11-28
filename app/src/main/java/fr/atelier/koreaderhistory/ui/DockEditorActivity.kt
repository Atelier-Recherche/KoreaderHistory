package fr.atelier.koreaderhistory.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import fr.atelier.koreaderhistory.R
import fr.atelier.koreaderhistory.data.DockActionType
import fr.atelier.koreaderhistory.data.DockIconResolver
import fr.atelier.koreaderhistory.data.DockSlot
import fr.atelier.koreaderhistory.data.WidgetPrefs
import fr.atelier.koreaderhistory.databinding.ActivityDockEditorBinding
import fr.atelier.koreaderhistory.databinding.ItemDockSlotBinding
import fr.atelier.koreaderhistory.widget.HistoryWidgetProvider

class DockEditorActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDockEditorBinding
    private lateinit var prefs: WidgetPrefs
    private lateinit var adapter: DockAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDockEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = WidgetPrefs(this)
        prefs.ensureDockSlots()

        adapter = DockAdapter(
            onClick = { slot ->
                startActivity(
                    Intent(this, DockSlotEditActivity::class.java)
                        .putExtra(DockSlotEditActivity.EXTRA_SLOT_ID, slot.id)
                )
            },
            onMove = { slot, delta ->
                if (prefs.moveDockSlot(slot.id, delta)) {
                    adapter.submit(prefs.getDockSlots())
                    HistoryWidgetProvider.refreshAll(this)
                }
            },
        )
        binding.dockList.layoutManager = LinearLayoutManager(this)
        binding.dockList.adapter = adapter

        binding.btnRestoreDefaults.setOnClickListener {
            prefs.restoreDefaultDock()
            adapter.submit(prefs.getDockSlots())
            HistoryWidgetProvider.refreshAll(this)
            Toast.makeText(this, R.string.restore_default_dock, Toast.LENGTH_SHORT).show()
        }

        binding.btnAdd.setOnClickListener {
            if (prefs.getDockSlots().size >= WidgetPrefs.MAX_DOCK_SLOTS) {
                Toast.makeText(
                    this,
                    getString(R.string.dock_max_slots, WidgetPrefs.MAX_DOCK_SLOTS),
                    Toast.LENGTH_SHORT,
                ).show()
                return@setOnClickListener
            }
            val slot = DockSlot(
                id = WidgetPrefs.newSlotId(),
                label = "Action",
                iconKey = "app",
                actionType = DockActionType.OPEN_KOREADER,
            )
            prefs.upsertDockSlot(slot)
            startActivity(
                Intent(this, DockSlotEditActivity::class.java)
                    .putExtra(DockSlotEditActivity.EXTRA_SLOT_ID, slot.id)
            )
        }
    }

    override fun onResume() {
        super.onResume()
        adapter.submit(prefs.getDockSlots())
        HistoryWidgetProvider.refreshAll(this)
    }

    private class DockAdapter(
        private val onClick: (DockSlot) -> Unit,
        private val onMove: (DockSlot, Int) -> Unit,
    ) : RecyclerView.Adapter<DockAdapter.VH>() {
        private var items: List<DockSlot> = emptyList()

        fun submit(list: List<DockSlot>) {
            items = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val binding = ItemDockSlotBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return VH(binding)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.bind(items[position], position, items.size, onClick, onMove)
        }

        class VH(private val binding: ItemDockSlotBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(
                slot: DockSlot,
                position: Int,
                count: Int,
                onClick: (DockSlot) -> Unit,
                onMove: (DockSlot, Int) -> Unit,
            ) {
                binding.slotLabel.text = slot.label
                binding.slotAction.text = buildString {
                    append(slot.actionType.label)
                    if (slot.param.isNotBlank()) append(" — ").append(slot.param)
                }
                binding.slotIcon.setImageDrawable(
                    DockIconResolver.loadDrawable(binding.root.context, slot)
                )
                binding.root.setOnClickListener { onClick(slot) }
                binding.btnMoveUp.isEnabled = position > 0
                binding.btnMoveDown.isEnabled = position < count - 1
                binding.btnMoveUp.alpha = if (position > 0) 1f else 0.3f
                binding.btnMoveDown.alpha = if (position < count - 1) 1f else 0.3f
                binding.btnMoveUp.setOnClickListener { onMove(slot, -1) }
                binding.btnMoveDown.setOnClickListener { onMove(slot, 1) }
            }
        }
    }
}
