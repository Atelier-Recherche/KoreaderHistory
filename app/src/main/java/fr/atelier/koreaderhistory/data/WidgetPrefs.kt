package fr.atelier.koreaderhistory.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class WidgetPrefs(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var koreaderPath: String
        get() = prefs.getString(KEY_PATH, DEFAULT_PATH) ?: DEFAULT_PATH
        set(value) = prefs.edit().putString(KEY_PATH, value.trimEnd('/')).apply()

    var itemCount: Int
        get() = prefs.getInt(KEY_COUNT, 8).coerceIn(1, 20)
        set(value) = prefs.edit().putInt(KEY_COUNT, value.coerceIn(1, 20)).apply()

    var excludeFinished: Boolean
        get() = prefs.getBoolean(KEY_EXCLUDE_FINISHED, true)
        set(value) = prefs.edit().putBoolean(KEY_EXCLUDE_FINISHED, value).apply()

    var excludeNonBooks: Boolean
        get() = prefs.getBoolean(KEY_EXCLUDE_NON_BOOKS, true)
        set(value) = prefs.edit().putBoolean(KEY_EXCLUDE_NON_BOOKS, value).apply()

    var showAuthor: Boolean
        get() = prefs.getBoolean(KEY_SHOW_AUTHOR, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_AUTHOR, value).apply()

    var showPercent: Boolean
        get() = prefs.getBoolean(KEY_SHOW_PERCENT, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_PERCENT, value).apply()

    var darkTheme: Boolean
        get() = prefs.getBoolean(KEY_DARK, false)
        set(value) = prefs.edit().putBoolean(KEY_DARK, value).apply()

    var dockOnTop: Boolean
        get() = prefs.getBoolean(KEY_DOCK_TOP, false)
        set(value) = prefs.edit().putBoolean(KEY_DOCK_TOP, value).apply()

    var koreaderPackageMode: String
        get() = prefs.getString(KEY_PKG_MODE, "auto") ?: "auto"
        set(value) = prefs.edit().putString(KEY_PKG_MODE, value).apply()

    /** Dock icon size in dp: 32 (S), 44 (M), 56 (L) */
    var dockIconSizeDp: Int
        get() = prefs.getInt(KEY_DOCK_SIZE, 44).coerceIn(28, 72)
        set(value) = prefs.edit().putInt(KEY_DOCK_SIZE, value.coerceIn(28, 72)).apply()

    fun getDockSlots(): List<DockSlot> {
        val raw = prefs.getString(KEY_DOCK, null)
        if (raw.isNullOrBlank()) return defaultDock()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        DockSlot(
                            id = o.getString("id"),
                            label = o.optString("label", ""),
                            iconKey = o.optString("iconKey", "history"),
                            actionType = DockActionType.fromId(o.optString("actionType", "history")),
                            param = o.optString("param", ""),
                            param2 = o.optString("param2", ""),
                        )
                    )
                }
            }
        }.getOrElse { defaultDock() }
    }

    fun setDockSlots(slots: List<DockSlot>) {
        val arr = JSONArray()
        slots.take(6).forEach { slot ->
            arr.put(
                JSONObject()
                    .put("id", slot.id)
                    .put("label", slot.label)
                    .put("iconKey", slot.iconKey)
                    .put("actionType", slot.actionType.id)
                    .put("param", slot.param)
                    .put("param2", slot.param2)
            )
        }
        prefs.edit().putString(KEY_DOCK, arr.toString()).apply()
    }

    fun upsertDockSlot(slot: DockSlot) {
        val list = getDockSlots().toMutableList()
        val idx = list.indexOfFirst { it.id == slot.id }
        if (idx >= 0) list[idx] = slot else if (list.size < 6) list.add(slot)
        setDockSlots(list)
    }

    fun removeDockSlot(id: String) {
        setDockSlots(getDockSlots().filterNot { it.id == id })
    }

    companion object {
        const val PREFS = "widget_prefs"
        const val DEFAULT_PATH = "/storage/emulated/0/koreader"

        private const val KEY_PATH = "koreader_path"
        private const val KEY_COUNT = "item_count"
        private const val KEY_EXCLUDE_FINISHED = "exclude_finished"
        private const val KEY_EXCLUDE_NON_BOOKS = "exclude_non_books"
        private const val KEY_SHOW_AUTHOR = "show_author"
        private const val KEY_SHOW_PERCENT = "show_percent"
        private const val KEY_DARK = "dark_theme"
        private const val KEY_DOCK_TOP = "dock_top"
        private const val KEY_PKG_MODE = "pkg_mode"
        private const val KEY_DOCK_SIZE = "dock_icon_size"
        private const val KEY_DOCK = "dock_slots"

        fun defaultDock(): List<DockSlot> = listOf(
            DockSlot(UUID.randomUUID().toString(), "Continuer", "continue", DockActionType.CONTINUE),
            DockSlot(UUID.randomUUID().toString(), "Historique", "history", DockActionType.HISTORY),
            DockSlot(UUID.randomUUID().toString(), "KOReader", "koreader", DockActionType.OPEN_KOREADER),
        )

        fun newSlotId(): String = UUID.randomUUID().toString()
    }
}
