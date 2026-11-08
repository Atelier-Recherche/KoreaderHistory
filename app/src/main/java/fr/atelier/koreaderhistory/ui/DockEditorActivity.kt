package fr.atelier.koreaderhistory.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import fr.atelier.koreaderhistory.data.DockActionType
import fr.atelier.koreaderhistory.data.DockSlot
import fr.atelier.koreaderhistory.data.IconCatalog
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

        adapter = DockAdapter { slot ->
            startActivity(
                Intent(this, DockSlotEditActivity::class.java)
                    .putExtra(DockSlotEditActivity.EXTRA_SLOT_ID, slot.id)
            )
        }
        binding.dockList.layoutManager = LinearLayoutManager(this)
        binding.dockList.adapter = adapter

        binding.btnAdd.setOnClickListener {
            if (prefs.getDockSlots().size >= 6) {
                Toast.makeText(this, "Maximum 6 icônes", Toast.LENGTH_SHORT).show()
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
            holder.bind(items[position], onClick)
        }

        class VH(private val binding: ItemDockSlotBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(slot: DockSlot, onClick: (DockSlot) -> Unit) {
                binding.slotLabel.text = slot.label
                binding.slotAction.text = buildString {
                    append(slot.actionType.label)
                    if (slot.param.isNotBlank()) append(" — ").append(slot.param)
                }
                binding.slotIcon.setImageResource(IconCatalog.resId(slot.iconKey))
                binding.root.setOnClickListener { onClick(slot) }
            }
        }
    }
}
