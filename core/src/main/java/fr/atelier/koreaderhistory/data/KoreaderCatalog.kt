package fr.atelier.koreaderhistory.data

import java.io.File

data class CatalogItem(
    val title: String,
    val param: String,
    val param2: String = "",
    val suggestedIcon: String? = null,
)

/**
 * Scans the KOReader install for folders, collections, plugins and system actions
 * so the dock editor can offer SimpleUI-like pickers.
 */
object KoreaderCatalog {

    private val SKIP_PLUGINS = setOf("athistory")

    fun listPlugins(rootPath: String): List<CatalogItem> {
        val pluginsDir = File(rootPath, "plugins")
        if (!pluginsDir.isDirectory) return knownBuiltinPlugins()

        val scanned = pluginsDir.listFiles()
            ?.filter { it.isDirectory && it.name.endsWith(".koplugin") }
            ?.mapNotNull { dir ->
                val folderId = dir.name.removeSuffix(".koplugin")
                if (folderId in SKIP_PLUGINS) return@mapNotNull null

                val meta = File(dir, "_meta.lua")
                val text = if (meta.isFile) {
                    runCatching { meta.readText() }.getOrNull().orEmpty()
                        .removePrefix("\uFEFF")
                } else {
                    ""
                }
                // Prefer folder id (canonical KOReader plugin id). Meta "name" is only
                // used when it looks like an identifier — never fullname (regex pitfall).
                val metaName = extractLuaIdentifier(text, "name")
                val name = metaName?.takeIf { it.equals(folderId, ignoreCase = true) || looksLikePluginId(it) }
                    ?: folderId
                val fullname = extractLuaDisplayString(text, "fullname") ?: humanize(name)
                val hint = pluginMethodHint(name)
                CatalogItem(
                    title = fullname,
                    param = name,
                    param2 = hint.method,
                    suggestedIcon = hint.icon,
                )
            }
            .orEmpty()

        if (scanned.isEmpty()) return knownBuiltinPlugins()

        return scanned
            .distinctBy { it.param.lowercase() }
            .sortedBy { it.title.lowercase() }
    }

    fun listCollections(rootPath: String): List<CatalogItem> {
        val candidates = listOf(
            File(rootPath, "settings/collection.lua"),
            File(rootPath, "settings/collections.lua"),
        )
        for (file in candidates) {
            if (!file.isFile) continue
            val text = runCatching { file.readText() }.getOrNull() ?: continue
            val names = extractCollectionNames(text)
            if (names.isNotEmpty()) {
                return names.sortedBy { it.lowercase() }.map {
                    CatalogItem(title = it, param = it, suggestedIcon = "library")
                }
            }
        }
        return emptyList()
    }

    fun listSystemActions(): List<CatalogItem> = listOf(
        CatalogItem("Historique", "history", "onShowHist", "history"),
        CatalogItem("Collections", "collections", "onShowCollList", "library"),
        CatalogItem("Favoris", "collections", "onShowColl", "library"),
        CatalogItem("Infos livre", "bookinfo", "onShowBookInfo", "book"),
        CatalogItem("Recherche fichiers", "filesearcher", "onShowFileSearch", "folder"),
        CatalogItem("Raccourcis dossiers", "folder_shortcuts", "onShowFolderShortcutsDialog", "folder"),
        CatalogItem("Dictionnaire", "dictionary", "onShowDictionaryLookup", "book"),
        CatalogItem("Wikipédia", "wikipedia", "onShowWikipediaLookup", "book"),
        CatalogItem("Continuer la lecture", "continue", "", "continue"),
        CatalogItem("Écran d’accueil (Dispatcher)", "home", "", "koreader"),
        CatalogItem("FileManager (Dispatcher)", "filemanager", "", "folder"),
        CatalogItem("Stats calendrier", "statistics_calendar_view", "", "journal"),
        CatalogItem("Wifi on", "toggle_wifi", "", "link"),
        CatalogItem("Frontlight", "show_frontlight_dialog", "", "app"),
        CatalogItem("Quitter KOReader", "exit", "", "app"),
        CatalogItem("Redémarrer KOReader", "restart", "", "refresh"),
    )

    fun suggestFolders(rootPath: String): List<CatalogItem> {
        val home = readHomeDir(rootPath) ?: "/storage/emulated/0"
        val roots = listOf(
            home,
            "/storage/emulated/0/Documents",
            "/storage/emulated/0/Download",
            rootPath,
        ).distinct()
        val out = mutableListOf<CatalogItem>()
        for (r in roots) {
            val dir = File(r)
            if (!dir.isDirectory) continue
            out += CatalogItem(dir.name.ifBlank { r }, r, suggestedIcon = "folder")
            dir.listFiles()
                ?.filter { it.isDirectory }
                ?.sortedBy { it.name.lowercase() }
                ?.take(30)
                ?.forEach { child ->
                    out += CatalogItem(child.name, child.absolutePath, suggestedIcon = "folder")
                }
        }
        return out.distinctBy { it.param }
    }

    private fun readHomeDir(rootPath: String): String? {
        val settings = File(rootPath, "settings.reader.lua")
        if (!settings.isFile) return null
        val text = runCatching { settings.readText() }.getOrNull() ?: return null
        val table = LuaTableParser.parseRootTable(text)
        return table["home_dir"]?.toString()
    }

    private fun knownBuiltinPlugins(): List<CatalogItem> = listOf(
        CatalogItem("RSS Reader", "rssreader", "openAccountList", "rss"),
        CatalogItem("Bluesky", "bluesky", "openRoot", "bluesky"),
        CatalogItem("Statistics", "statistics", "onShowCalendarView", "journal"),
        CatalogItem("SimpleUI", "simpleui", "", "koreader"),
        CatalogItem("Wallabag", "wallabag", "", "journal"),
        CatalogItem("CoverBrowser", "coverbrowser", "", "library"),
        CatalogItem("Zotero Atelier", "zoteroatelier", "", "library"),
        CatalogItem("App Store", "appstore", "", "plugin"),
        CatalogItem("Multibook", "multibook", "", "book"),
    )

    private data class MethodHint(val method: String, val icon: String)

    private fun pluginMethodHint(name: String): MethodHint = when {
        name.contains("rss", true) -> MethodHint("openAccountList", "rss")
        name.contains("bluesky", true) -> MethodHint("openRoot", "bluesky")
        name.contains("statistics", true) -> MethodHint("onShowCalendarView", "journal")
        name.contains("wallabag", true) -> MethodHint("", "journal")
        name.contains("zotero", true) -> MethodHint("", "library")
        name.contains("simpleui", true) -> MethodHint("", "koreader")
        name.contains("appstore", true) -> MethodHint("", "plugin")
        name.contains("multibook", true) -> MethodHint("", "book")
        else -> MethodHint("", "plugin")
    }

    private fun looksLikePluginId(value: String): Boolean =
        value.matches(Regex("^[A-Za-z][A-Za-z0-9_]*$")) && !value.contains(' ')

    private fun humanize(raw: String): String =
        raw.replace('_', ' ').replaceFirstChar { it.uppercase() }

    /** Extract a bare identifier assignment: name = "foo" (not fullname). */
    private fun extractLuaIdentifier(source: String, key: String): String? {
        val patterns = listOf(
            Regex("""\["$key"\]\s*=\s*"([A-Za-z][A-Za-z0-9_]*)""""),
            // Word-boundary so "name" does not match inside "fullname"
            Regex("""(?:^|[\s,{])$key\s*=\s*"([A-Za-z][A-Za-z0-9_]*)""""),
        )
        for (p in patterns) {
            val m = p.find(source)
            if (m != null) return m.groupValues[1]
        }
        return null
    }

    /** Extract display string: fullname = _("…") or "…" */
    private fun extractLuaDisplayString(source: String, key: String): String? {
        val patterns = listOf(
            Regex("""\["$key"\]\s*=\s*_\(\s*"([^"]+)"\s*\)"""),
            Regex("""(?:^|[\s,{])$key\s*=\s*_\(\s*"([^"]+)"\s*\)"""),
            Regex("""\["$key"\]\s*=\s*"([^"]+)""""),
            Regex("""(?:^|[\s,{])$key\s*=\s*"([^"]+)""""),
        )
        for (p in patterns) {
            val m = p.find(source)
            if (m != null) return m.groupValues[1]
        }
        return null
    }

    private fun extractCollectionNames(source: String): List<String> {
        val root = LuaTableParser.parseRootTable(source)
        if (root.isEmpty()) return emptyList()
        return root.keys.filter { key ->
            key.toIntOrNull() == null && key != "settings" && !key.startsWith("_")
        }
    }
}
