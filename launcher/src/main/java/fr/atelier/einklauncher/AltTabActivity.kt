package fr.atelier.einklauncher

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import fr.atelier.koreaderhistory.data.AppIconLoader
import fr.atelier.koreaderhistory.data.LauncherApp
import fr.atelier.koreaderhistory.data.LauncherPrefs
import fr.atelier.einklauncher.databinding.ActivityAltTabBinding
import fr.atelier.einklauncher.databinding.ItemAltTabBinding

class AltTabActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAltTabBinding
    private lateinit var apps: List<LauncherApp>
    private var selected = 0
    private lateinit var adapter: Adapter
    private var iconsOnly = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAltTabBinding.inflate(layoutInflater)
        setContentView(binding.root)
        iconsOnly = LauncherPrefs(this).altTabIconsOnly
        apps = RecentAppsHelper.recentForAltTab(this)
        if (apps.isEmpty()) {
            finish()
            return
        }
        selected = 0
        adapter = Adapter(apps, iconsOnly) { idx ->
            selected = idx
            adapter.selected = selected
            adapter.notifyDataSetChanged()
            launchSelected()
        }
        binding.altTabList.layoutManager = LinearLayoutManager(this)
        binding.altTabList.adapter = adapter
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_TAB) {
            if (apps.isNotEmpty()) {
                selected = (selected + 1) % apps.size
                adapter.selected = selected
                adapter.notifyDataSetChanged()
                binding.altTabList.scrollToPosition(selected)
            }
            return true
        }
        if (event.action == KeyEvent.ACTION_UP &&
            (event.keyCode == KeyEvent.KEYCODE_ALT_LEFT || event.keyCode == KeyEvent.KEYCODE_ALT_RIGHT)
        ) {
            launchSelected()
            return true
        }
        if (event.keyCode == KeyEvent.KEYCODE_ESCAPE) {
            finish()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun launchSelected() {
        apps.getOrNull(selected)?.let { RecentAppsHelper.launch(this, it.packageName) }
        finish()
    }

    private class Adapter(
        private val apps: List<LauncherApp>,
        private val iconsOnly: Boolean,
        private val onClick: (Int) -> Unit,
    ) : RecyclerView.Adapter<Adapter.VH>() {
        var selected = 0

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val b = ItemAltTabBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return VH(b)
        }

        override fun getItemCount(): Int = apps.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val app = apps[position]
            holder.binding.appLabel.visibility = if (iconsOnly) View.GONE else View.VISIBLE
            holder.binding.appLabel.text = app.label
            holder.binding.appIcon.setImageDrawable(
                AppIconLoader.loadDrawable(holder.binding.root.context, app.packageName)
            )
            holder.binding.root.setBackgroundColor(
                if (position == selected) 0x22000000 else 0x00000000
            )
            holder.binding.root.setOnClickListener { onClick(position) }
        }

        class VH(val binding: ItemAltTabBinding) : RecyclerView.ViewHolder(binding.root)
    }
}

