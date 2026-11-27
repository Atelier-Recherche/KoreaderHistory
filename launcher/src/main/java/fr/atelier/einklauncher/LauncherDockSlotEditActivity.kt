package fr.atelier.einklauncher

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import fr.atelier.einklauncher.databinding.ActivityDockSlotEditBinding
import fr.atelier.koreaderhistory.data.CatalogItem
import fr.atelier.koreaderhistory.data.DockActionType
import fr.atelier.koreaderhistory.data.DockIconResolver
import fr.atelier.koreaderhistory.data.DockSlot
import fr.atelier.koreaderhistory.data.KoreaderCatalog
import fr.atelier.koreaderhistory.data.LauncherPrefs
import fr.atelier.koreaderhistory.data.WidgetPrefs

class LauncherDockSlotEditActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDockSlotEditBinding
    private lateinit var prefs: LauncherPrefs
    private lateinit var widgetPrefs: WidgetPrefs
    private var slotId: String = ""
    private var actionType: DockActionType = DockActionType.OPEN_KOREADER
    private var iconKey: String = "app"
    private var iconPackage: String = ""
    private var iconPackPackage: String = ""
    private var iconDrawableName: String = ""

    private val pickIcon = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        val data = result.data ?: return@registerForActivityResult
        iconKey = data.getStringExtra(IconPickerActivity.EXTRA_ICON_KEY) ?: iconKey
        iconPackage = data.getStringExtra(IconPickerActivity.EXTRA_ICON_PACKAGE).orEmpty()
        iconPackPackage = data.getStringExtra(IconPickerActivity.EXTRA_ICON_PACK).orEmpty()
        iconDrawableName = data.getStringExtra(IconPickerActivity.EXTRA_ICON_DRAWABLE).orEmpty()
        refreshIconSummary()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDockSlotEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = LauncherPrefs(this)
        widgetPrefs = WidgetPrefs(this)
        slotId = intent.getStringExtra(EXTRA_SLOT_ID) ?: run {
            finish()
            return
        }

        val slot = prefs.getDockSlots().find { it.id == slotId } ?: run {
            finish()
            return
        }

        actionType = slot.actionType
        iconKey = slot.iconKey
        iconPackage = slot.iconPackage
        iconPackPackage = slot.iconPackPackage
        iconDrawableName = slot.iconDrawableName
        binding.inputLabel.setText(slot.label)
        binding.inputParam.setText(slot.param)
        binding.inputParam2.setText(slot.param2)

        binding.spinnerIcon.visibility = View.GONE
        binding.btnPickAppIcon.text = getString(R.string.pick_icon_visual)
        binding.btnPickAppIcon.setOnClickListener {
            pickIcon.launch(Intent(this, IconPickerActivity::class.java))
        }
        binding.btnResetAppIcon.setOnClickListener {
            iconPackage = ""
            iconPackPackage = ""
            iconDrawableName = ""
            iconKey = "app"
            refreshIconSummary()
        }
        binding.btnPickAction.setOnClickListener { showActionTypePicker() }
        binding.btnBrowseParam.setOnClickListener { showParamBrowser() }
        binding.btnSave.setOnClickListener { save() }
        binding.btnDelete.setOnClickListener {
            prefs.removeDockSlot(slotId)
            finish()
        }
        refreshActionUi()
        refreshIconSummary()
    }

    private fun refreshIconSummary() {
        val preview = DockSlot(
            id = slotId,
            label = "",
            iconKey = iconKey,
            actionType = actionType,
            iconPackage = iconPackage,
            iconPackPackage = iconPackPackage,
            iconDrawableName = iconDrawableName,
        )
        binding.iconAppSummary.text = when {
            iconPackPackage.isNotBlank() -> "Pack : $iconPackPackage / $iconDrawableName"
            iconPackage.isNotBlank() -> getString(R.string.app_icon_summary, iconPackage)
            else -> getString(R.string.app_icon_catalog) + " ($iconKey)"
        }
        binding.iconAppSummary.setCompoundDrawablesWithIntrinsicBounds(
            DockIconResolver.loadDrawable(this, preview),
            null,
            null,
            null,
        )
        binding.iconAppSummary.compoundDrawablePadding = 16
    }

    private fun refreshActionUi() {
        binding.actionSummary.text = buildString {
            append(actionType.label)
            val p = binding.inputParam.text?.toString().orEmpty()
            val p2 = binding.inputParam2.text?.toString().orEmpty()
            if (p.isNotBlank()) append("\n").append(p)
            if (p2.isNotBlank()) append(" / ").append(p2)
        }
        val needs = actionType.needsParam
        binding.paramLabel.visibility = if (needs) View.VISIBLE else View.GONE
        binding.inputParam.visibility = if (needs) View.VISIBLE else View.GONE
        val needSecond = actionType == DockActionType.LAUNCH_APP ||
            actionType == DockActionType.OPEN_PLUGIN ||
            actionType == DockActionType.SYSTEM_ACTION
        binding.inputParam2.visibility = if (needSecond) View.VISIBLE else View.GONE
        binding.btnBrowseParam.visibility = when (actionType) {
            DockActionType.OPEN_FOLDER,
            DockActionType.OPEN_COLLECTION,
            DockActionType.OPEN_PLUGIN,
            DockActionType.SYSTEM_ACTION -> View.VISIBLE
            else -> View.GONE
        }
        binding.paramLabel.text = when (actionType) {
            DockActionType.OPEN_BOOK -> "Chemin du livre"
            DockActionType.LAUNCH_APP -> "Package de l’app"
            DockActionType.OPEN_URI -> "URI"
            DockActionType.OPEN_FOLDER -> "Chemin du dossier"
            DockActionType.OPEN_COLLECTION -> "Nom de la collection"
            DockActionType.OPEN_PLUGIN -> "Plugin (clé)"
            DockActionType.SYSTEM_ACTION -> "Module / action"
            DockActionType.DISPATCHER -> "ID Dispatcher"
            else -> getString(R.string.action_param)
        }
    }

    private fun showActionTypePicker() {
        val choices = listOf(
            DockActionType.OPEN_FOLDER,
            DockActionType.OPEN_COLLECTION,
            DockActionType.OPEN_PLUGIN,
            DockActionType.SYSTEM_ACTION,
            DockActionType.CONTINUE,
            DockActionType.HISTORY,
            DockActionType.OPEN_KOREADER,
            DockActionType.OPEN_BOOK,
            DockActionType.LAUNCH_APP,
            DockActionType.OPEN_URI,
            DockActionType.DISPATCHER,
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.pick_action_type)
            .setItems(choices.map { it.label }.toTypedArray()) { _, which ->
                actionType = choices[which]
                when (actionType) {
                    DockActionType.CONTINUE,
                    DockActionType.HISTORY,
                    DockActionType.OPEN_KOREADER -> {
                        binding.inputParam.setText("")
                        binding.inputParam2.setText("")
                        if (binding.inputLabel.text.isNullOrBlank()) {
                            binding.inputLabel.setText(actionType.label)
                        }
                        refreshActionUi()
                    }
                    DockActionType.OPEN_FOLDER,
                    DockActionType.OPEN_COLLECTION,
                    DockActionType.OPEN_PLUGIN,
                    DockActionType.SYSTEM_ACTION -> {
                        refreshActionUi()
                        showParamBrowser()
                    }
                    else -> refreshActionUi()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showParamBrowser() {
        val path = widgetPrefs.koreaderPath
        val items: List<CatalogItem> = when (actionType) {
            DockActionType.OPEN_FOLDER -> KoreaderCatalog.suggestFolders(path)
            DockActionType.OPEN_COLLECTION -> KoreaderCatalog.listCollections(path)
            DockActionType.OPEN_PLUGIN -> KoreaderCatalog.listPlugins(path)
            DockActionType.SYSTEM_ACTION -> KoreaderCatalog.listSystemActions()
            else -> emptyList()
        }
        if (items.isEmpty()) {
            Toast.makeText(this, R.string.catalog_empty, Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle(actionType.label)
            .setItems(items.map { it.title }.toTypedArray()) { _, which ->
                val item = items[which]
                binding.inputParam.setText(item.param)
                binding.inputParam2.setText(item.param2)
                if (binding.inputLabel.text.isNullOrBlank() ||
                    binding.inputLabel.text.toString() == actionType.label
                ) {
                    binding.inputLabel.setText(item.title)
                }
                item.suggestedIcon?.let { key -> iconKey = key }
                if (item.param == "continue" && item.param2.isBlank() &&
                    actionType == DockActionType.SYSTEM_ACTION
                ) {
                    actionType = DockActionType.CONTINUE
                }
                refreshActionUi()
                refreshIconSummary()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun save() {
        val updated = DockSlot(
            id = slotId,
            label = binding.inputLabel.text?.toString().orEmpty().ifBlank { actionType.label },
            iconKey = iconKey.ifBlank { "app" },
            actionType = actionType,
            param = binding.inputParam.text?.toString().orEmpty(),
            param2 = binding.inputParam2.text?.toString().orEmpty(),
            iconPackage = iconPackage,
            iconPackPackage = iconPackPackage,
            iconDrawableName = iconDrawableName,
        )
        prefs.upsertDockSlot(updated)
        Toast.makeText(this, R.string.save, Toast.LENGTH_SHORT).show()
        finish()
    }

    companion object {
        const val EXTRA_SLOT_ID = "slot_id"
    }
}
