package fr.atelier.koreaderhistory.data

import android.content.Context
import android.widget.Toast
import fr.atelier.koreaderhistory.R
import java.io.File

object DockExecutor {

    fun execute(context: Context, prefs: WidgetPrefs, slot: DockSlot) {
        when (slot.actionType) {
            DockActionType.OPEN_KOREADER -> {
                if (!KoreaderLauncher.openKoreader(context, prefs)) {
                    toast(context, R.string.no_koreader)
                }
            }
            DockActionType.OPEN_BOOK -> {
                if (slot.param.isBlank() || !KoreaderLauncher.openBook(context, prefs, slot.param)) {
                    toast(context, "Livre introuvable")
                }
            }
            DockActionType.LAUNCH_APP -> {
                if (slot.param.isBlank() ||
                    !KoreaderLauncher.launchApp(context, slot.param, slot.param2.ifBlank { null })
                ) {
                    toast(context, "Application introuvable")
                }
            }
            DockActionType.OPEN_URI -> {
                if (slot.param.isBlank() || !KoreaderLauncher.openUri(context, slot.param)) {
                    toast(context, "URI invalide")
                }
            }
            DockActionType.CONTINUE,
            DockActionType.HISTORY,
            DockActionType.OPEN_FOLDER,
            DockActionType.OPEN_COLLECTION,
            DockActionType.OPEN_PLUGIN,
            DockActionType.SYSTEM_ACTION,
            DockActionType.DISPATCHER -> {
                writePending(prefs, slot)
                if (!KoreaderLauncher.openKoreader(context, prefs)) {
                    toast(context, R.string.no_koreader)
                }
            }
        }
    }

    fun writePending(prefs: WidgetPrefs, slot: DockSlot) {
        val dir = File(prefs.koreaderPath, "settings/athistory")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "pending.lua")
        val actionId = when (slot.actionType) {
            DockActionType.SYSTEM_ACTION -> {
                // system_action uses plugin module or dispatcher id
                if (slot.param2.isNotBlank()) "open_plugin" else "dispatcher"
            }
            else -> slot.actionType.id
        }
        val param = slot.param
        val param2 = when (slot.actionType) {
            DockActionType.SYSTEM_ACTION -> slot.param2
            else -> slot.param2
        }
        val args = buildString {
            append("{\n")
            if (param.isNotBlank()) {
                append("        [\"param\"] = ").append(luaString(param)).append(",\n")
            }
            if (param2.isNotBlank()) {
                append("        [\"param2\"] = ").append(luaString(param2)).append(",\n")
            }
            // For system_action mapped to dispatcher when no method
            if (slot.actionType == DockActionType.SYSTEM_ACTION && slot.param2.isBlank()) {
                // param is already the dispatcher id
            }
            if (slot.actionType == DockActionType.OPEN_COLLECTION) {
                append("        [\"collection\"] = ").append(luaString(param)).append(",\n")
            }
            append("    }")
        }
        val content = """
            -- written by KOReader History (l'Atelier)
            return {
                ["action"] = ${luaString(actionId)},
                ["args"] = $args,
                ["ts"] = ${System.currentTimeMillis() / 1000},
            }
        """.trimIndent() + "\n"
        file.writeText(content)
    }

    private fun luaString(value: String): String {
        val escaped = value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
        return "\"$escaped\""
    }

    private fun toast(context: Context, msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    private fun toast(context: Context, resId: Int) {
        Toast.makeText(context, context.getString(resId), Toast.LENGTH_SHORT).show()
    }
}
