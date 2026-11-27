package fr.atelier.koreaderhistory.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class DesktopItem(
    val id: String,
    val type: String, // app | folder | history | widget
    val label: String,
    val packageName: String = "",
    val activityName: String = "",
    val children: List<String> = emptyList(),
    val appWidgetId: Int = -1,
    val spanX: Int = 1,
    val spanY: Int = 1,
    /** Hauteur widget en dp (indépendante du provider, utile si resizeMode=0). */
    val heightDp: Int = 160,
)

class LauncherPrefs(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var pinnedPackages: List<String>
        get() = prefs.getString(KEY_PINNED, "")?.split(',')?.map { it.trim() }?.filter { it.isNotBlank() }
            ?: emptyList()
        set(value) = prefs.edit().putString(KEY_PINNED, value.joinToString(",")).apply()

    var altTabIconsOnly: Boolean
        get() = prefs.getBoolean(KEY_ALT_TAB_ICONS, false)
        set(value) = prefs.edit().putBoolean(KEY_ALT_TAB_ICONS, value).apply()

    var altTabExclude: Set<String>
        get() = prefs.getStringSet(KEY_ALT_TAB_EXCLUDE, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_ALT_TAB_EXCLUDE, value).apply()

    var dockIconSizeDp: Int
        get() = prefs.getInt(KEY_DOCK_SIZE, 48).coerceIn(28, 120)
        set(value) {
            prefs.edit().putInt(KEY_DOCK_SIZE, value.coerceIn(28, 120)).commit()
        }

    var dockIconBorder: Boolean
        get() = prefs.getBoolean(KEY_DOCK_BORDER, true)
        set(value) {
            prefs.edit().putBoolean(KEY_DOCK_BORDER, value).commit()
        }

    var dockJustify: String
        get() = prefs.getString(KEY_DOCK_JUSTIFY, JUSTIFY_CENTER) ?: JUSTIFY_CENTER
        set(value) {
            prefs.edit().putString(KEY_DOCK_JUSTIFY, value).commit()
        }

    fun getDockSlots(): List<DockSlot> {
        val raw = prefs.getString(KEY_DOCK, null)
        if (raw.isNullOrBlank()) return emptyList() // dock launcher vide par défaut (≠ widget)
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        DockSlot(
                            id = o.getString("id"),
                            label = o.optString("label", ""),
                            iconKey = o.optString("iconKey", "app"),
                            actionType = DockActionType.fromId(o.optString("actionType", "launch_app")),
                            param = o.optString("param", ""),
                            param2 = o.optString("param2", ""),
                            iconPackage = o.optString("iconPackage", ""),
                            iconPackPackage = o.optString("iconPackPackage", ""),
                            iconDrawableName = o.optString("iconDrawableName", ""),
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
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
        prefs.edit().putString(KEY_DOCK, arr.toString()).commit()
    }

    fun upsertDockSlot(slot: DockSlot) {
        val list = getDockSlots().toMutableList()
        val idx = list.indexOfFirst { it.id == slot.id }
        if (idx >= 0) list[idx] = slot else if (list.size < MAX_DOCK_SLOTS) list.add(slot)
        setDockSlots(list)
    }

    fun removeDockSlot(id: String) {
        setDockSlots(getDockSlots().filterNot { it.id == id })
    }

    fun getDesktopItems(): List<DesktopItem> {
        val raw = prefs.getString(KEY_DESKTOP, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val childrenArr = o.optJSONArray("children")
                    val children = buildList {
                        if (childrenArr != null) {
                            for (c in 0 until childrenArr.length()) add(childrenArr.getString(c))
                        }
                    }
                    add(
                        DesktopItem(
                            id = o.getString("id"),
                            type = o.optString("type", "app"),
                            label = o.optString("label", ""),
                            packageName = o.optString("packageName", ""),
                            activityName = o.optString("activityName", ""),
                            children = children,
                            appWidgetId = o.optInt("appWidgetId", -1),
                            spanX = o.optInt("spanX", 1),
                            spanY = o.optInt("spanY", 1),
                            heightDp = o.optInt("heightDp", 160).coerceIn(80, 800),
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun setDesktopItems(items: List<DesktopItem>) {
        val arr = JSONArray()
        items.forEach { item ->
            val children = JSONArray()
            item.children.forEach { children.put(it) }
            arr.put(
                JSONObject()
                    .put("id", item.id)
                    .put("type", item.type)
                    .put("label", item.label)
                    .put("packageName", item.packageName)
                    .put("activityName", item.activityName)
                    .put("children", children)
                    .put("appWidgetId", item.appWidgetId)
                    .put("spanX", item.spanX)
                    .put("spanY", item.spanY)
                    .put("heightDp", item.heightDp)
            )
        }
        prefs.edit().putString(KEY_DESKTOP, arr.toString()).apply()
    }

    fun updateDesktopItem(item: DesktopItem) {
        val items = getDesktopItems().toMutableList()
        val idx = items.indexOfFirst { it.id == item.id }
        if (idx >= 0) {
            items[idx] = item
            setDesktopItems(items)
        }
    }

    /**
     * Échange un item avec le prochain du même type dans la direction [delta]
     * (ex. widgets entre eux, sans dépendre des apps intercalées).
     */
    fun moveDesktopItem(id: String, delta: Int): Boolean {
        val items = getDesktopItems().toMutableList()
        val idx = items.indexOfFirst { it.id == id }
        if (idx < 0) return false
        val type = items[idx].type
        val step = if (delta >= 0) 1 else -1
        var target = idx + step
        while (target in items.indices) {
            if (items[target].type == type) {
                val tmp = items[idx]
                items[idx] = items[target]
                items[target] = tmp
                setDesktopItems(items)
                return true
            }
            target += step
        }
        return false
    }

    fun isDocked(packageName: String, activityName: String = ""): Boolean {
        return getDockSlots().any {
            it.actionType == DockActionType.LAUNCH_APP &&
                it.param == packageName &&
                (activityName.isBlank() || it.param2.isBlank() || it.param2 == activityName)
        }
    }

    fun pinAppToDock(app: LauncherApp, iconPackage: String = app.packageName, iconKey: String = "app", iconPackPackage: String = "", iconDrawableName: String = ""): Boolean {
        if (getDockSlots().size >= MAX_DOCK_SLOTS) return false
        if (isDocked(app.packageName, app.activityName)) return true
        upsertDockSlot(
            DockSlot(
                id = newSlotId(),
                label = app.label,
                iconKey = iconKey,
                actionType = DockActionType.LAUNCH_APP,
                param = app.packageName,
                param2 = app.activityName,
                iconPackage = iconPackage,
                iconPackPackage = iconPackPackage,
                iconDrawableName = iconDrawableName,
            ),
        )
        // aussi pour la barre Win / pinned
        if (app.packageName !in pinnedPackages) {
            pinnedPackages = pinnedPackages + app.packageName
        }
        return true
    }

    fun unpinAppFromDock(packageName: String, activityName: String = "") {
        setDockSlots(
            getDockSlots().filterNot {
                it.actionType == DockActionType.LAUNCH_APP &&
                    it.param == packageName &&
                    (activityName.isBlank() || it.param2.isBlank() || it.param2 == activityName)
            },
        )
        pinnedPackages = pinnedPackages.filterNot { it == packageName }
    }

    fun togglePinned(packageName: String) {
        val list = pinnedPackages.toMutableList()
        if (packageName in list) list.remove(packageName) else list.add(packageName)
        pinnedPackages = list
    }

    companion object {
        const val PREFS = "launcher_prefs"
        const val MAX_DOCK_SLOTS = 12
        const val JUSTIFY_CENTER = "center"
        const val JUSTIFY_SPACE_BETWEEN = "space_between"

        private const val KEY_PINNED = "pinned_packages"
        private const val KEY_DESKTOP = "desktop_items"
        private const val KEY_ALT_TAB_ICONS = "alt_tab_icons_only"
        private const val KEY_ALT_TAB_EXCLUDE = "alt_tab_exclude"
        private const val KEY_DOCK = "launcher_dock_slots"
        private const val KEY_DOCK_SIZE = "launcher_dock_icon_size"
        private const val KEY_DOCK_BORDER = "launcher_dock_icon_border"
        private const val KEY_DOCK_JUSTIFY = "launcher_dock_justify"

        fun newId(): String = java.util.UUID.randomUUID().toString()
        fun newSlotId(): String = java.util.UUID.randomUUID().toString()
    }
}
