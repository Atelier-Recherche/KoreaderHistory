package fr.atelier.koreaderhistory.data

import fr.atelier.koreaderhistory.R

object IconCatalog {
    data class IconDef(val key: String, val label: String, val resId: Int)

    val all = listOf(
        IconDef("history", "Historique", R.drawable.ic_history),
        IconDef("continue", "Continuer", R.drawable.ic_continue),
        IconDef("folder", "Dossier", R.drawable.ic_folder),
        IconDef("library", "Bibliothèque", R.drawable.ic_library),
        IconDef("journal", "Journal", R.drawable.ic_journal),
        IconDef("rss", "Flux RSS", R.drawable.ic_rss),
        IconDef("bluesky", "Bluesky", R.drawable.ic_bluesky),
        IconDef("koreader", "KOReader", R.drawable.ic_koreader),
        IconDef("book", "Livre", R.drawable.ic_book),
        IconDef("app", "App", R.drawable.ic_app),
        IconDef("link", "Lien", R.drawable.ic_link),
        IconDef("plugin", "Plugin", R.drawable.ic_plugin),
        IconDef("dispatcher", "Dispatcher", R.drawable.ic_dispatcher),
        IconDef("refresh", "Refresh", R.drawable.ic_refresh),
    )

    fun resId(key: String): Int =
        all.find { it.key == key }?.resId ?: R.drawable.ic_app

    fun keys(): List<String> = all.map { it.key }

    fun labels(): List<String> = all.map { it.label }
}
