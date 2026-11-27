package fr.atelier.koreaderhistory.data

enum class DockActionType(
    val id: String,
    val label: String,
    val needsParam: Boolean,
    val pluginSide: Boolean,
) {
    OPEN_KOREADER("open_koreader", "Ouvrir KOReader", false, false),
    OPEN_BOOK("open_book", "Ouvrir un livre", true, false),
    LAUNCH_APP("launch_app", "Lancer une app Android", true, false),
    OPEN_URI("open_uri", "Ouvrir URI / intent", true, false),
    CONTINUE("continue", "Continuer la lecture", false, true),
    HISTORY("history", "Historique KOReader", false, true),
    OPEN_FOLDER("open_folder", "Dossier", true, true),
    OPEN_COLLECTION("open_collection", "Collection", true, true),
    OPEN_PLUGIN("open_plugin", "Plugin", true, true),
    SYSTEM_ACTION("system_action", "Action système", true, true),
    DISPATCHER("dispatcher", "Dispatcher (ID libre)", true, true);

    companion object {
        fun fromId(id: String): DockActionType =
            entries.find { it.id == id } ?: OPEN_KOREADER
    }
}
