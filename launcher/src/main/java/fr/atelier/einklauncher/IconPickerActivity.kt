package fr.atelier.einklauncher

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import fr.atelier.einklauncher.databinding.ActivityIconPickerBinding
import fr.atelier.einklauncher.databinding.ItemIconPickBinding
import fr.atelier.koreaderhistory.data.AppIconLoader
import fr.atelier.koreaderhistory.data.IconCatalog
import fr.atelier.koreaderhistory.data.IconPackInfo
import fr.atelier.koreaderhistory.data.IconPackManager

class IconPickerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityIconPickerBinding
    private lateinit var adapter: IconAdapter
    private var source = SOURCE_CATALOG
    private var packs: List<IconPackInfo> = emptyList()
    private var selectedPack: String = ""
    private var allItems: List<IconItem> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIconPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = IconAdapter { item ->
            val data = Intent()
                .putExtra(EXTRA_ICON_KEY, item.iconKey)
                .putExtra(EXTRA_ICON_PACKAGE, item.iconPackage)
                .putExtra(EXTRA_ICON_PACK, item.iconPackPackage)
                .putExtra(EXTRA_ICON_DRAWABLE, item.iconDrawableName)
            setResult(Activity.RESULT_OK, data)
            finish()
        }
        binding.iconGrid.layoutManager = GridLayoutManager(this, 4)
        binding.iconGrid.adapter = adapter

        binding.btnCatalog.setOnClickListener { loadSource(SOURCE_CATALOG) }
        binding.btnApps.setOnClickListener { loadSource(SOURCE_APPS) }
        binding.btnPacks.setOnClickListener { loadSource(SOURCE_PACKS) }
        binding.inputSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) = applyFilter()
        })

        packs = IconPackManager.listInstalledPacks(this)
        binding.spinnerPack.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            packs.map { it.label }.ifEmpty { listOf("(aucun pack)") },
        )
        binding.spinnerPack.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedPack = packs.getOrNull(position)?.packageName.orEmpty()
                if (source == SOURCE_PACKS) reloadItems()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        loadSource(SOURCE_CATALOG)
    }

    private fun loadSource(src: Int) {
        source = src
        binding.spinnerPack.visibility = if (src == SOURCE_PACKS) View.VISIBLE else View.GONE
        reloadItems()
    }

    private fun reloadItems() {
        allItems = when (source) {
            SOURCE_APPS -> AppIconLoader.listLauncherApps(this).map {
                IconItem(
                    label = it.label,
                    iconKey = "app",
                    iconPackage = it.packageName,
                    previewPackage = it.packageName,
                    previewActivity = it.activityName,
                )
            }
            SOURCE_PACKS -> {
                if (selectedPack.isBlank() && packs.isNotEmpty()) {
                    selectedPack = packs.first().packageName
                }
                IconPackManager.listPackIcons(this, selectedPack).map {
                    IconItem(
                        label = it.label,
                        iconKey = "app",
                        iconPackPackage = selectedPack,
                        iconDrawableName = it.drawableName,
                    )
                }
            }
            else -> IconCatalog.all.map {
                IconItem(label = it.label, iconKey = it.key, catalogRes = it.resId)
            }
        }
        applyFilter()
    }

    private fun applyFilter() {
        val q = binding.inputSearch.text?.toString()?.trim()?.lowercase().orEmpty()
        adapter.submit(
            if (q.isBlank()) allItems
            else allItems.filter {
                it.label.lowercase().contains(q) ||
                    it.iconPackage.lowercase().contains(q) ||
                    it.iconDrawableName.lowercase().contains(q) ||
                    it.iconPackPackage.lowercase().contains(q)
            },
        )
    }

    data class IconItem(
        val label: String,
        val iconKey: String = "app",
        val iconPackage: String = "",
        val iconPackPackage: String = "",
        val iconDrawableName: String = "",
        val catalogRes: Int = 0,
        val previewPackage: String = "",
        val previewActivity: String = "",
    )

    private class IconAdapter(
        private val onClick: (IconItem) -> Unit,
    ) : RecyclerView.Adapter<IconAdapter.VH>() {
        private var items: List<IconItem> = emptyList()

        fun submit(list: List<IconItem>) {
            items = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val binding = ItemIconPickBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return VH(binding)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.bind(items[position], onClick)
        }

        class VH(private val binding: ItemIconPickBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(item: IconItem, onClick: (IconItem) -> Unit) {
                binding.iconLabel.text = item.label
                when {
                    item.catalogRes != 0 -> binding.iconImage.setImageResource(item.catalogRes)
                    item.iconPackPackage.isNotBlank() -> {
                        binding.iconImage.setImageDrawable(
                            IconPackManager.loadPackDrawable(
                                binding.root.context,
                                item.iconPackPackage,
                                item.iconDrawableName,
                            ),
                        )
                    }
                    else -> binding.iconImage.setImageDrawable(
                        AppIconLoader.loadActivityDrawable(
                            binding.root.context,
                            item.previewPackage.ifBlank { item.iconPackage },
                            item.previewActivity,
                        ) ?: AppIconLoader.loadDrawable(
                            binding.root.context,
                            item.previewPackage.ifBlank { item.iconPackage },
                        ),
                    )
                }
                binding.root.setOnClickListener { onClick(item) }
            }
        }
    }

    companion object {
        const val EXTRA_ICON_KEY = "icon_key"
        const val EXTRA_ICON_PACKAGE = "icon_package"
        const val EXTRA_ICON_PACK = "icon_pack"
        const val EXTRA_ICON_DRAWABLE = "icon_drawable"
        private const val SOURCE_CATALOG = 0
        private const val SOURCE_APPS = 1
        private const val SOURCE_PACKS = 2
    }
}
