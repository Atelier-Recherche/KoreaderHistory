package fr.atelier.koreaderhistory.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class AppIconOverride(
    val iconKey: String = "app",
    val iconPackage: String = "",
    val iconPackPackage: String = "",
    val iconDrawableName: String = "",
) {
    fun toDockSlot(id: String = "override", label: String = ""): DockSlot = DockSlot(
        id = id,
        label = label,
        iconKey = iconKey.ifBlank { "app" },
        actionType = DockActionType.LAUNCH_APP,
        iconPackage = iconPackage,
        iconPackPackage = iconPackPackage,
        iconDrawableName = iconDrawableName,
    )
}

data class DesktopItem(
    val id: String,
    /** app | folder | history | widget | shortcut (action KOReader / dock-like) */
    val type: String,
    val label: String,
    val packageName: String = "",
    val activityName: String = "",
    val children: List<String> = emptyList(),
    val appWidgetId: Int = -1,
    val spanX: Int = 1,
    val spanY: Int = 1,
    /** Hauteur widget en dp (indépendante du provider, utile si resizeMode=0). */
    val heightDp: Int = 160,
    /** Pour type=shortcut : action dock KOReader / plugin / dossier… */
    val actionType: String = "",
    val param: String = "",
    val param2: String = "",
    val iconKey: String = "app",
    val iconPackage: String = "",
    val iconPackPackage: String = "",
    val iconDrawableName: String = "",
    /** Position dans la grille bureau (cases vides autorisées). -1 = non assigné. */
    val cellIndex: Int = -1,
) {
    fun toDockSlot(): DockSlot = DockSlot(
        id = id,
        label = label,
        iconKey = iconKey.ifBlank { "app" },
        actionType = DockActionType.fromId(actionType.ifBlank { DockActionType.OPEN_KOREADER.id }),
        param = param,
        param2 = param2,
        iconPackage = iconPackage,
        iconPackPackage = iconPackPackage,
        iconDrawableName = iconDrawableName,
    )
}

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

    /** Afficher le dock launcher (indépendant du dock widget). */
    var dockEnabled: Boolean
        get() = prefs.getBoolean(KEY_DOCK_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_DOCK_ENABLED, value).commit()
        }

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

    var desktopIconSizeDp: Int
        get() = prefs.getInt(KEY_DESKTOP_SIZE, 56).coerceIn(28, 120)
        set(value) {
            prefs.edit().putInt(KEY_DESKTOP_SIZE, value.coerceIn(28, 120)).commit()
        }

    var desktopIconBorder: Boolean
        get() = prefs.getBoolean(KEY_DESKTOP_BORDER, false)
        set(value) {
            prefs.edit().putBoolean(KEY_DESKTOP_BORDER, value).commit()
        }

    var drawerIconSizeDp: Int
        get() = prefs.getInt(KEY_DRAWER_SIZE, 56).coerceIn(28, 120)
        set(value) {
            prefs.edit().putInt(KEY_DRAWER_SIZE, value.coerceIn(28, 120)).commit()
        }

    var drawerIconBorder: Boolean
        get() = prefs.getBoolean(KEY_DRAWER_BORDER, false)
        set(value) {
            prefs.edit().putBoolean(KEY_DRAWER_BORDER, value).commit()
        }

    var statusBarEnabled: Boolean
        get() = prefs.getBoolean(KEY_STATUS_ENABLED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_STATUS_ENABLED, value).commit()
        }

    var statusBarShowTime: Boolean
        get() = prefs.getBoolean(KEY_STATUS_TIME, true)
        set(value) {
            prefs.edit().putBoolean(KEY_STATUS_TIME, value).commit()
        }

    var statusBarShowDate: Boolean
        get() = prefs.getBoolean(KEY_STATUS_DATE, true)
        set(value) {
            prefs.edit().putBoolean(KEY_STATUS_DATE, value).commit()
        }

    var statusBarShowWifi: Boolean
        get() = prefs.getBoolean(KEY_STATUS_WIFI, true)
        set(value) {
            prefs.edit().putBoolean(KEY_STATUS_WIFI, value).commit()
        }

    var statusBarShowBluetooth: Boolean
        get() = prefs.getBoolean(KEY_STATUS_BT, true)
        set(value) {
            prefs.edit().putBoolean(KEY_STATUS_BT, value).commit()
        }

    var statusBarShowBattery: Boolean
        get() = prefs.getBoolean(KEY_STATUS_BATTERY, true)
        set(value) {
            prefs.edit().putBoolean(KEY_STATUS_BATTERY, value).commit()
        }

    /**
     * Format date barre d’état :
     * medium | weekday | weekday_date | full
     */
    var statusBarDateFormat: String
        get() = prefs.getString(KEY_STATUS_DATE_FMT, DATE_FMT_WEEKDAY_DATE) ?: DATE_FMT_WEEKDAY_DATE
        set(value) {
            prefs.edit().putString(KEY_STATUS_DATE_FMT, value).commit()
        }

    var desktopColumns: Int
        get() = prefs.getInt(KEY_DESKTOP_COLS, 4).coerceIn(3, 6)
        set(value) {
            prefs.edit().putInt(KEY_DESKTOP_COLS, value.coerceIn(3, 6)).commit()
        }

    var drawerColumns: Int
        get() = prefs.getInt(KEY_DRAWER_COLS, 4).coerceIn(3, 6)
        set(value) {
            prefs.edit().putInt(KEY_DRAWER_COLS, value.coerceIn(3, 6)).commit()
        }

    var desktopLabelSp: Int
        get() = prefs.getInt(KEY_DESKTOP_LABEL_SP, 12).coerceIn(10, 18)
        set(value) {
            prefs.edit().putInt(KEY_DESKTOP_LABEL_SP, value.coerceIn(10, 18)).commit()
        }

    var drawerLabelSp: Int
        get() = prefs.getInt(KEY_DRAWER_LABEL_SP, 12).coerceIn(10, 18)
        set(value) {
            prefs.edit().putInt(KEY_DRAWER_LABEL_SP, value.coerceIn(10, 18)).commit()
        }

    var statusBarTextSp: Int
        get() = prefs.getInt(KEY_STATUS_TEXT_SP, 13).coerceIn(10, 18)
        set(value) {
            prefs.edit().putInt(KEY_STATUS_TEXT_SP, value.coerceIn(10, 18)).commit()
        }

    var historyTitleSp: Int
        get() = prefs.getInt(KEY_HISTORY_TITLE_SP, 14).coerceIn(10, 18)
        set(value) {
            prefs.edit().putInt(KEY_HISTORY_TITLE_SP, value.coerceIn(10, 18)).commit()
        }

    var hiddenApps: Set<String>
        get() = prefs.getStringSet(KEY_HIDDEN_APPS, emptySet()) ?: emptySet()
        set(value) {
            prefs.edit().putStringSet(KEY_HIDDEN_APPS, value).commit()
        }

    fun isAppHidden(componentKey: String): Boolean = componentKey in hiddenApps

    fun hideApp(componentKey: String) {
        hiddenApps = hiddenApps + componentKey
    }

    fun unhideApp(componentKey: String) {
        hiddenApps = hiddenApps - componentKey
    }

    fun getAppIconOverride(componentKey: String): AppIconOverride? {
        val raw = prefs.getString(KEY_ICON_OVERRIDES, null) ?: return null
        return runCatching {
            val o = JSONObject(raw).optJSONObject(componentKey) ?: return null
            AppIconOverride(
                iconKey = o.optString("iconKey", "app"),
                iconPackage = o.optString("iconPackage", ""),
                iconPackPackage = o.optString("iconPackPackage", ""),
                iconDrawableName = o.optString("iconDrawableName", ""),
            )
        }.getOrNull()
    }

    fun setAppIconOverride(componentKey: String, override: AppIconOverride) {
        val root = runCatching {
            JSONObject(prefs.getString(KEY_ICON_OVERRIDES, null) ?: "{}")
        }.getOrElse { JSONObject() }
        root.put(
            componentKey,
            JSONObject()
                .put("iconKey", override.iconKey)
                .put("iconPackage", override.iconPackage)
                .put("iconPackPackage", override.iconPackPackage)
                .put("iconDrawableName", override.iconDrawableName),
        )
        prefs.edit().putString(KEY_ICON_OVERRIDES, root.toString()).commit()
    }

    fun clearAppIconOverride(componentKey: String) {
        val raw = prefs.getString(KEY_ICON_OVERRIDES, null) ?: return
        val root = runCatching { JSONObject(raw) }.getOrNull() ?: return
        root.remove(componentKey)
        prefs.edit().putString(KEY_ICON_OVERRIDES, root.toString()).commit()
    }

    fun listAppIconOverrideKeys(): List<String> {
        val raw = prefs.getString(KEY_ICON_OVERRIDES, null) ?: return emptyList()
        val root = runCatching { JSONObject(raw) }.getOrNull() ?: return emptyList()
        return root.keys().asSequence().toList().sorted()
    }

    /** Icône builtin dock « Applications » (défaut catalogue app). */
    fun getDockAppsIcon(): AppIconOverride = readBuiltinIcon(KEY_DOCK_APPS_ICON, "app")

    fun setDockAppsIcon(override: AppIconOverride) = writeBuiltinIcon(KEY_DOCK_APPS_ICON, override)

    /** Icône builtin dock « Notifications » (défaut catalogue dispatcher). */
    fun getDockNotifsIcon(): AppIconOverride = readBuiltinIcon(KEY_DOCK_NOTIFS_ICON, "dispatcher")

    fun setDockNotifsIcon(override: AppIconOverride) = writeBuiltinIcon(KEY_DOCK_NOTIFS_ICON, override)

    private fun readBuiltinIcon(key: String, defaultKey: String): AppIconOverride {
        val raw = prefs.getString(key, null) ?: return AppIconOverride(iconKey = defaultKey)
        return runCatching {
            val o = JSONObject(raw)
            AppIconOverride(
                iconKey = o.optString("iconKey", defaultKey),
                iconPackage = o.optString("iconPackage", ""),
                iconPackPackage = o.optString("iconPackPackage", ""),
                iconDrawableName = o.optString("iconDrawableName", ""),
            )
        }.getOrElse { AppIconOverride(iconKey = defaultKey) }
    }

    private fun writeBuiltinIcon(key: String, override: AppIconOverride) {
        prefs.edit().putString(
            key,
            JSONObject()
                .put("iconKey", override.iconKey)
                .put("iconPackage", override.iconPackage)
                .put("iconPackPackage", override.iconPackPackage)
                .put("iconDrawableName", override.iconDrawableName)
                .toString(),
        ).commit()
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
        val items = runCatching {
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
                            actionType = o.optString("actionType", ""),
                            param = o.optString("param", ""),
                            param2 = o.optString("param2", ""),
                            iconKey = o.optString("iconKey", "app"),
                            iconPackage = o.optString("iconPackage", ""),
                            iconPackPackage = o.optString("iconPackPackage", ""),
                            iconDrawableName = o.optString("iconDrawableName", ""),
                            cellIndex = o.optInt("cellIndex", -1),
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
        return migrateDesktopCellIndices(items)
    }

    /** Assigne cellIndex 0..n-1 aux items de grille sans position, puis persiste si besoin. */
    private fun migrateDesktopCellIndices(items: List<DesktopItem>): List<DesktopItem> {
        val gridTypes = setOf("app", "folder", "shortcut")
        val needs = items.any { it.type in gridTypes && it.cellIndex < 0 }
        if (!needs) return items
        var next = 0
        val used = items.mapNotNull { if (it.type in gridTypes && it.cellIndex >= 0) it.cellIndex else null }.toMutableSet()
        val migrated = items.map { item ->
            if (item.type !in gridTypes || item.cellIndex >= 0) return@map item
            while (next in used) next++
            used.add(next)
            item.copy(cellIndex = next++)
        }
        setDesktopItems(migrated)
        return migrated
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
                    .put("actionType", item.actionType)
                    .put("param", item.param)
                    .put("param2", item.param2)
                    .put("iconKey", item.iconKey)
                    .put("iconPackage", item.iconPackage)
                    .put("iconPackPackage", item.iconPackPackage)
                    .put("iconDrawableName", item.iconDrawableName)
                    .put("cellIndex", item.cellIndex)
            )
        }
        prefs.edit().putString(KEY_DESKTOP, arr.toString()).commit()
    }

    /** Prochain cellIndex libre pour un nouvel item de grille. */
    fun nextFreeCellIndex(): Int {
        val used = getDesktopItems()
            .filter { it.type == "app" || it.type == "folder" || it.type == "shortcut" }
            .map { it.cellIndex }
            .filter { it >= 0 }
            .toSet()
        var i = 0
        while (i in used) i++
        return i
    }

    /**
     * Déplace un item de grille d’une case (dCol, dRow). Swap si la cible est occupée.
     */
    fun moveDesktopItemOnGrid(id: String, dCol: Int, dRow: Int): Boolean {
        val cols = desktopColumns
        val items = getDesktopItems().toMutableList()
        val idx = items.indexOfFirst { it.id == id }
        if (idx < 0) return false
        val item = items[idx]
        if (item.type !in setOf("app", "folder", "shortcut")) return false
        val from = item.cellIndex.coerceAtLeast(0)
        val fromCol = from % cols
        val fromRow = from / cols
        val toCol = fromCol + dCol
        val toRow = fromRow + dRow
        if (toCol !in 0 until cols || toRow < 0) return false
        val to = toRow * cols + toCol
        val otherIdx = items.indexOfFirst {
            it.id != id &&
                (it.type == "app" || it.type == "folder" || it.type == "shortcut") &&
                it.cellIndex == to
        }
        items[idx] = item.copy(cellIndex = to)
        if (otherIdx >= 0) {
            items[otherIdx] = items[otherIdx].copy(cellIndex = from)
        }
        setDesktopItems(items)
        return true
    }

    fun saveDockVisualOptions(sizeDp: Int, border: Boolean, justify: String, enabled: Boolean = dockEnabled): Boolean {
        return prefs.edit()
            .putBoolean(KEY_DOCK_ENABLED, enabled)
            .putInt(KEY_DOCK_SIZE, sizeDp.coerceIn(28, 120))
            .putBoolean(KEY_DOCK_BORDER, border)
            .putString(KEY_DOCK_JUSTIFY, justify)
            .commit()
    }

    fun readDockVisualDebug(): String =
        "enabled=$dockEnabled size=${dockIconSizeDp} border=$dockIconBorder justify=$dockJustify"

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
        private const val KEY_DOCK_ENABLED = "launcher_dock_enabled"
        private const val KEY_DOCK_SIZE = "launcher_dock_icon_size"
        private const val KEY_DOCK_BORDER = "launcher_dock_icon_border"
        private const val KEY_DOCK_JUSTIFY = "launcher_dock_justify"
        private const val KEY_DESKTOP_SIZE = "desktop_icon_size"
        private const val KEY_DESKTOP_BORDER = "desktop_icon_border"
        private const val KEY_DRAWER_SIZE = "drawer_icon_size"
        private const val KEY_DRAWER_BORDER = "drawer_icon_border"
        private const val KEY_DESKTOP_COLS = "desktop_columns"
        private const val KEY_DRAWER_COLS = "drawer_columns"
        private const val KEY_DESKTOP_LABEL_SP = "desktop_label_sp"
        private const val KEY_DRAWER_LABEL_SP = "drawer_label_sp"
        private const val KEY_STATUS_TEXT_SP = "status_bar_text_sp"
        private const val KEY_HISTORY_TITLE_SP = "history_title_sp"
        private const val KEY_HIDDEN_APPS = "hidden_apps"
        private const val KEY_ICON_OVERRIDES = "app_icon_overrides"
        private const val KEY_DOCK_APPS_ICON = "dock_apps_icon"
        private const val KEY_DOCK_NOTIFS_ICON = "dock_notifs_icon"
        private const val KEY_STATUS_ENABLED = "status_bar_enabled"
        private const val KEY_STATUS_TIME = "status_bar_time"
        private const val KEY_STATUS_DATE = "status_bar_date"
        private const val KEY_STATUS_DATE_FMT = "status_bar_date_format"
        private const val KEY_STATUS_WIFI = "status_bar_wifi"
        private const val KEY_STATUS_BT = "status_bar_bluetooth"
        private const val KEY_STATUS_BATTERY = "status_bar_battery"

        const val DATE_FMT_MEDIUM = "medium"
        const val DATE_FMT_WEEKDAY = "weekday"
        const val DATE_FMT_WEEKDAY_DATE = "weekday_date"
        const val DATE_FMT_FULL = "full"

        fun newId(): String = java.util.UUID.randomUUID().toString()
        fun newSlotId(): String = java.util.UUID.randomUUID().toString()
    }
}
