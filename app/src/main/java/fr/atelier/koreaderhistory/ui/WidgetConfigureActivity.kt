package fr.atelier.koreaderhistory.ui

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import fr.atelier.koreaderhistory.R
import fr.atelier.koreaderhistory.data.BridgeInstaller
import fr.atelier.koreaderhistory.data.WidgetPrefs
import fr.atelier.koreaderhistory.databinding.ActivityWidgetConfigureBinding
import fr.atelier.koreaderhistory.widget.HistoryWidgetProvider

class WidgetConfigureActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWidgetConfigureBinding
    private lateinit var prefs: WidgetPrefs
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    private val packageModes = listOf("auto", "launcher", "fdroid")
    private val packageLabels by lazy {
        listOf(
            getString(R.string.pkg_auto),
            getString(R.string.pkg_launcher),
            getString(R.string.pkg_fdroid),
        )
    }
    private val dockSizes = listOf(32, 44, 56)
    private val dockSizeLabels by lazy {
        listOf(
            getString(R.string.dock_size_s),
            getString(R.string.dock_size_m),
            getString(R.string.dock_size_l),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)
        binding = ActivityWidgetConfigureBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = WidgetPrefs(this)

        appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        binding.inputPath.setText(prefs.koreaderPath)
        binding.inputCount.setText(prefs.itemCount.toString())
        binding.checkExcludeFinished.isChecked = prefs.excludeFinished
        binding.checkExcludeNonBooks.isChecked = prefs.excludeNonBooks
        binding.checkShowAuthor.isChecked = prefs.showAuthor
        binding.checkShowPercent.isChecked = prefs.showPercent
        binding.checkDark.isChecked = prefs.darkTheme
        binding.checkDockTop.isChecked = prefs.dockOnTop

        binding.spinnerPackage.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            packageLabels
        )
        binding.spinnerPackage.setSelection(packageModes.indexOf(prefs.koreaderPackageMode).coerceAtLeast(0))

        binding.spinnerDockSize.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            dockSizeLabels
        )
        binding.spinnerDockSize.setSelection(
            dockSizes.indexOf(prefs.dockIconSizeDp).let { if (it >= 0) it else 1 }
        )

        binding.btnEditDock.setOnClickListener {
            startActivity(Intent(this, DockEditorActivity::class.java))
        }
        binding.btnInstallBridge.setOnClickListener {
            BridgeInstaller.install(this, prefs).fold(
                onSuccess = {
                    Toast.makeText(this, getString(R.string.bridge_installed, it), Toast.LENGTH_LONG).show()
                },
                onFailure = {
                    Toast.makeText(this, getString(R.string.bridge_failed, it.message ?: "?"), Toast.LENGTH_LONG).show()
                }
            )
        }
        binding.btnSave.setOnClickListener { saveAndFinish() }
    }

    private fun saveAndFinish() {
        prefs.koreaderPath = binding.inputPath.text?.toString().orEmpty().ifBlank { WidgetPrefs.DEFAULT_PATH }
        prefs.itemCount = binding.inputCount.text?.toString()?.toIntOrNull() ?: 8
        prefs.excludeFinished = binding.checkExcludeFinished.isChecked
        prefs.excludeNonBooks = binding.checkExcludeNonBooks.isChecked
        prefs.showAuthor = binding.checkShowAuthor.isChecked
        prefs.showPercent = binding.checkShowPercent.isChecked
        prefs.darkTheme = binding.checkDark.isChecked
        prefs.dockOnTop = binding.checkDockTop.isChecked
        prefs.koreaderPackageMode = packageModes.getOrElse(binding.spinnerPackage.selectedItemPosition) { "auto" }
        prefs.dockIconSizeDp = dockSizes.getOrElse(binding.spinnerDockSize.selectedItemPosition) { 44 }

        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            val mgr = AppWidgetManager.getInstance(this)
            HistoryWidgetProvider.updateAppWidget(this, mgr, appWidgetId)
            val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            setResult(Activity.RESULT_OK, result)
        } else {
            HistoryWidgetProvider.refreshAll(this)
        }
        finish()
    }
}
