package fr.atelier.koreaderhistory.data

import android.content.Context
import java.io.File

object BridgeInstaller {
    private val PLUGIN_FILES = listOf("_meta.lua", "main.lua")

    fun install(context: Context, prefs: WidgetPrefs): Result<String> {
        return runCatching {
            val destDir = File(prefs.koreaderPath, "plugins/athistory.koplugin")
            if (!destDir.exists() && !destDir.mkdirs()) {
                error("Impossible de créer ${destDir.absolutePath}")
            }
            for (name in PLUGIN_FILES) {
                context.assets.open("athistory.koplugin/$name").use { input ->
                    File(destDir, name).outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
            // Ensure pending dir exists
            File(prefs.koreaderPath, "settings/athistory").mkdirs()
            destDir.absolutePath
        }
    }
}
