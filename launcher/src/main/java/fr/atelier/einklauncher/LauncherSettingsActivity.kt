package fr.atelier.einklauncher

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import fr.atelier.koreaderhistory.data.WidgetPrefs

class LauncherSettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = WidgetPrefs(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
            setBackgroundColor(0xFFFFFFFF.toInt())
        }

        val path = EditText(this).apply {
            setText(prefs.koreaderPath)
            hint = getString(R.string.koreader_path)
            setTextColor(0xFF000000.toInt())
        }
        root.addView(path)

        fun addBtn(label: Int, click: () -> Unit) {
            root.addView(
                MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                    text = getString(label)
                    setOnClickListener { click() }
                },
            )
        }

        addBtn(R.string.save) {
            prefs.koreaderPath = path.text?.toString().orEmpty().ifBlank { WidgetPrefs.DEFAULT_PATH }
            Toast.makeText(this, R.string.save, Toast.LENGTH_SHORT).show()
        }
        addBtn(R.string.grant_storage) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                startActivity(
                    Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:$packageName")
                    },
                )
            }
        }
        addBtn(R.string.edit_launcher_dock) {
            startActivity(Intent(this, LauncherDockEditorActivity::class.java))
        }
        addBtn(R.string.set_as_home) { startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
        addBtn(R.string.enable_accessibility) { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        addBtn(R.string.enable_notification_access) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        addBtn(R.string.enable_usage_access) { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            Toast.makeText(this, R.string.need_permission, Toast.LENGTH_LONG).show()
        }

        setContentView(root)
    }
}
