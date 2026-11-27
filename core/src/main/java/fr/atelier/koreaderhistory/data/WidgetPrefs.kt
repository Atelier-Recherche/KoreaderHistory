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

    var useFilenameAsTitle: Boolean
        get() = prefs.getBoolean(KEY_USE_FILENAME, false)
        set(value) = prefs.edit().putBoolean(KEY_USE_FILENAME, value).apply()

    var showCoverLetter: Boolean
        get() = prefs.getBoolean(KEY_SHOW_COVER, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_COVER, value).apply()

    var showPages: Boolean
        get() = prefs.getBoolean(KEY_SHOW_PAGES, false)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_PAGES, value).apply()

    var darkTheme: Boolean
        get() = prefs.getBoolean(KEY_DARK, false)
        set(value) = prefs.edit().putBoolean(KEY_DARK, value).apply()

    /** Afficher le dock du widget (indépendant du dock launcher). */
    var dockEnabled: Boolean
        get() = prefs.getBoolean(KEY_DOCK_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_DOCK_ENABLED, value).apply()

    var dockOnTop: Boolean
        get() = prefs.getBoolean(KEY_DOCK_TOP, false)
        set(value) = prefs.edit().putBoolean(KEY_DOCK_TOP, value).apply()

    var koreaderPackageMode: String
        get() = prefs.getString(KEY_PKG_MODE, "auto") ?: "auto"
        set(value) = prefs.edit().putString(KEY_PKG_MODE, value).apply()

    /** Dock icon size in dp: 32 (S), 44 (M), 56 (L), 72 (XL), 96 (XXL) */
    var dockIconSizeDp: Int
        get() = prefs.getInt(KEY_DOCK_SIZE, 44).coerceIn(28, 120)
        set(value) = prefs.edit().putInt(KEY_DOCK_SIZE, value.coerceIn(28, 120)).apply()

    var dockIconBorder: Boolean
        get() = prefs.getBoolean(KEY_DOCK_BORDER, true)
        set(value) = prefs.edit().putBoolean(KEY_DOCK_BORDER, value).apply()

    /** "center" or "space_between" */
    var dockJustify: String
        get() = prefs.getString(KEY_DOCK_JUSTIFY, JUSTIFY_CENTER) ?: JUSTIFY_CENTER
        set(value) = prefs.edit().putString(KEY_DOCK_JUSTIFY, value).apply()

    /**
     * Garantit des slots persistés avec IDs stables.
     * Absents ou liste vide → écrit [defaultDock] une fois.
     */
    fun ensureDockSlots(): List<DockSlot> {
        val raw = prefs.getString(KEY_DOCK, null)
        if (!raw.isNullOrBlank()) {
            val parsed = parseDockSlots(raw)
            if (parsed != null && parsed.isNotEmpty()) return parsed
        }
        val defaults = defaultDock()
        setDockSlots(defaults)
        return defaults
    }

    fun getDockSlots(): List<DockSlot> = ensureDockSlots()

    fun restoreDefaultDock(): List<DockSlot> {
        val defaults = defaultDock()
        setDockSlots(defaults)
        return defaults
    }

    fun setDockSlots(slots: List<DockSlot>) {
        val arr = JSONArray()
        slots.take(MAX_DOCK_SLOTS).forEach { slot ->
            arr.put(
                JSONObject()
                    .put("id", slot.id)
                    .put("label", slot.label)
                    .put("iconKey", slot.iconKey)
                    .put("actionType", slot.actionType.id)
                    .put("param", slot.param)
                    .put("param2", slot.param2)
                    .put("iconPackage", slot.iconPackage)
                    .put("iconPackPackage", slot.iconPackPackage)
                    .put("iconDrawableName", slot.iconDrawableName)
            )
        }
        prefs.edit().putString(KEY_DOCK, arr.toString()).apply()
    }

    fun upsertDockSlot(slot: DockSlot) {
        val list = getDockSlots().toMutableList()
        val idx = list.indexOfFirst { it.id == slot.id }
        if (idx >= 0) list[idx] = slot else if (list.size < MAX_DOCK_SLOTS) list.add(slot)
        setDockSlots(list)
    }

    fun removeDockSlot(id: String) {
        val remaining = getDockSlots().filterNot { it.id == id }
        // Liste vide → reseed (dock optionnel mais utile ; dockEnabled contrôle la visibilité)
        if (remaining.isEmpty()) {
            restoreDefaultDock()
        } else {
            setDockSlots(remaining)
        }
    }

    private fun parseDockSlots(raw: String): List<DockSlot>? {
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
                            iconPackage = o.optString("iconPackage", ""),
                            iconPackPackage = o.optString("iconPackPackage", ""),
                            iconDrawableName = o.optString("iconDrawableName", ""),
                        )
                    )
                }
            }
        }.getOrNull()
    }

    companion object {
        const val PREFS = "widget_prefs"
        const val DEFAULT_PATH = "/storage/emulated/0/koreader"
        const val MAX_DOCK_SLOTS = 12
        const val JUSTIFY_CENTER = "center"
        const val JUSTIFY_SPACE_BETWEEN = "space_between"

        private const val KEY_PATH = "koreader_path"
        private const val KEY_COUNT = "item_count"
        private const val KEY_EXCLUDE_FINISHED = "exclude_finished"
        private const val KEY_EXCLUDE_NON_BOOKS = "exclude_non_books"
        private const val KEY_SHOW_AUTHOR = "show_author"
        private const val KEY_SHOW_PERCENT = "show_percent"
        private const val KEY_USE_FILENAME = "use_filename_title"
        private const val KEY_SHOW_COVER = "show_cover_letter"
        private const val KEY_SHOW_PAGES = "show_pages"
        private const val KEY_DARK = "dark_theme"
        private const val KEY_DOCK_ENABLED = "dock_enabled"
        private const val KEY_DOCK_TOP = "dock_top"
        private const val KEY_PKG_MODE = "pkg_mode"
        private const val KEY_DOCK_SIZE = "dock_icon_size"
        private const val KEY_DOCK_BORDER = "dock_icon_border"
        private const val KEY_DOCK_JUSTIFY = "dock_justify"
        private const val KEY_DOCK = "dock_slots"

        fun defaultDock(): List<DockSlot> = listOf(
            DockSlot(UUID.randomUUID().toString(), "Continuer", "continue", DockActionType.CONTINUE),
            DockSlot(UUID.randomUUID().toString(), "Historique", "history", DockActionType.HISTORY),
            DockSlot(UUID.randomUUID().toString(), "KOReader", "koreader", DockActionType.OPEN_KOREADER),
        )

        fun newSlotId(): String = UUID.randomUUID().toString()
    }
}
