package fr.atelier.koreaderhistory.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import fr.atelier.koreaderhistory.databinding.ActivityMainBinding
import fr.atelier.koreaderhistory.data.BridgeInstaller
import fr.atelier.koreaderhistory.data.WidgetPrefs
import fr.atelier.koreaderhistory.widget.HistoryWidgetProvider

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: WidgetPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = WidgetPrefs(this)

        binding.btnPermission.setOnClickListener { requestStorageAccess() }
        binding.btnConfigure.setOnClickListener {
            startActivity(Intent(this, WidgetConfigureActivity::class.java))
        }
        binding.btnDock.setOnClickListener {
            startActivity(Intent(this, DockEditorActivity::class.java))
        }
        binding.btnBridge.setOnClickListener { installBridge() }
        binding.btnRefreshWidgets.setOnClickListener {
            HistoryWidgetProvider.refreshAll(this)
            Toast.makeText(this, getString(fr.atelier.koreaderhistory.R.string.refresh), Toast.LENGTH_SHORT).show()
        }
        refreshPermissionUi()
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionUi()
    }

    private fun refreshPermissionUi() {
        val ok = hasAllFilesAccess()
        binding.permissionStatus.text = if (ok) {
            "Accès stockage OK — dossier : ${prefs.koreaderPath}"
        } else {
            getString(fr.atelier.koreaderhistory.R.string.need_permission)
        }
        binding.btnPermission.isEnabled = !ok
    }

    private fun hasAllFilesAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    private fun requestStorageAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }
    }

    private fun installBridge() {
        BridgeInstaller.install(this, prefs).fold(
            onSuccess = { path ->
                Toast.makeText(
                    this,
                    getString(fr.atelier.koreaderhistory.R.string.bridge_installed, path),
                    Toast.LENGTH_LONG
                ).show()
            },
            onFailure = { e ->
                Toast.makeText(
                    this,
                    getString(fr.atelier.koreaderhistory.R.string.bridge_failed, e.message ?: "?"),
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }
}
