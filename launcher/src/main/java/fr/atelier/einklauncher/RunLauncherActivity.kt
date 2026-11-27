package fr.atelier.einklauncher

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import fr.atelier.koreaderhistory.data.AppIconLoader
import fr.atelier.koreaderhistory.data.LauncherApp
import fr.atelier.einklauncher.databinding.ActivityRunLauncherBinding
import fr.atelier.einklauncher.databinding.ItemRunResultBinding

class RunLauncherActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRunLauncherBinding
    private lateinit var allApps: List<LauncherApp>
    private lateinit var adapter: Adapter
    private var filtered: List<LauncherApp> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRunLauncherBinding.inflate(layoutInflater)
        setContentView(binding.root)
        allApps = AppIconLoader.listLauncherApps(this)
        adapter = Adapter { app ->
            RecentAppsHelper.launch(this, app.packageName)
            finish()
        }
        binding.results.layoutManager = LinearLayoutManager(this)
        binding.results.adapter = adapter
        binding.inputQuery.requestFocus()
        binding.inputQuery.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) = applyFilter()
        })
        binding.inputQuery.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                filtered.firstOrNull()?.let {
                    RecentAppsHelper.launch(this, it.packageName)
                    finish()
                }
                true
            } else false
        }
        applyFilter()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_ESCAPE) {
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun applyFilter() {
        val q = binding.inputQuery.text?.toString()?.trim()?.lowercase().orEmpty()
        filtered = if (q.isBlank()) allApps.take(30)
        else allApps.filter {
            it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q)
        }.take(40)
        adapter.submit(filtered)
    }

    private class Adapter(
        private val onClick: (LauncherApp) -> Unit,
    ) : RecyclerView.Adapter<Adapter.VH>() {
        private var items: List<LauncherApp> = emptyList()

        fun submit(list: List<LauncherApp>) {
            items = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val b = ItemRunResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return VH(b)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val app = items[position]
            holder.binding.appLabel.text = app.label
            holder.binding.appIcon.setImageDrawable(
                AppIconLoader.loadDrawable(holder.binding.root.context, app.packageName)
            )
            holder.binding.root.setOnClickListener { onClick(app) }
        }

        class VH(val binding: ItemRunResultBinding) : RecyclerView.ViewHolder(binding.root)
    }
}

