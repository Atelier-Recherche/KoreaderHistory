package fr.atelier.koreaderhistory.data

import java.io.File

class HistoryReader(private val prefs: WidgetPrefs) {

    fun loadBooks(): List<BookEntry> {
        val root = File(prefs.koreaderPath)
        val historyFile = File(root, "history.lua")
        if (!historyFile.isFile) return emptyList()

        val metaMode = readMetadataMode(root)
        val raw = runCatching { historyFile.readText() }.getOrNull() ?: return emptyList()
        val entries = LuaTableParser.parseArrayOfTables(raw)

        val bookExt = BOOK_EXTENSIONS
        val result = ArrayList<BookEntry>()

        for (entry in entries) {
            if (result.size >= prefs.itemCount) break
            val path = entry["file"]?.toString() ?: continue
            val time = when (val t = entry["time"]) {
                is Number -> t.toLong()
                is String -> t.toLongOrNull() ?: 0L
                else -> 0L
            }

            val file = File(path)
            val name = file.name
            val ext = file.extension.lowercase()

            if (prefs.excludeNonBooks) {
                if (ext !in bookExt) continue
                if (ext in NON_BOOK_WHEN_FILTERED) continue
                if (name.equals("error.txt", true)) continue
            }

            val meta = readSidecar(root, path, ext, metaMode)
            val status = meta.status
            val percent = meta.percent
            if (prefs.excludeFinished) {
                if (status.equals("complete", ignoreCase = true) || (percent != null && percent >= 0.98f)) continue
            }

            val title = meta.title?.takeIf { it.isNotBlank() } ?: file.nameWithoutExtension

            result.add(
                BookEntry(
                    filePath = path,
                    time = time,
                    title = title,
                    author = meta.author,
                    percentFinished = percent,
                    status = status,
                )
            )
        }
        return result
    }

    private data class SidecarMeta(
        val title: String?,
        val author: String?,
        val percent: Float?,
        val status: String?,
    )

    private fun readMetadataMode(root: File): String {
        val settings = File(root, "settings.reader.lua")
        if (!settings.isFile) return "dir"
        val text = runCatching { settings.readText() }.getOrNull() ?: return "dir"
        val table = LuaTableParser.parseRootTable(text)
        return table["document_metadata_folder"]?.toString() ?: "dir"
    }

    private fun readSidecar(
        root: File,
        bookPath: String,
        ext: String,
        mode: String,
    ): SidecarMeta {
        val empty = SidecarMeta(null, null, null, null)
        val sidecarFile = when (mode) {
            "doc" -> File("${bookPath.substringBeforeLast('.')}.sdr", "metadata.$ext.lua")
            else -> {
                // dir mode: /koreader/docsettings + absolute path without leading slash + .sdr
                val relative = bookPath.removePrefix("/").substringBeforeLast('.')
                File(File(root, "docsettings"), "$relative.sdr/metadata.$ext.lua")
            }
        }

        if (!sidecarFile.isFile) return empty

        val text = runCatching { sidecarFile.readText() }.getOrNull() ?: return empty
        val table = LuaTableParser.parseRootTable(text)
        val props = table["doc_props"] as? Map<*, *>
        val summary = table["summary"] as? Map<*, *>
        val title = props?.get("title")?.toString()
        val author = props?.get("authors")?.toString()?.substringBefore('\\')?.substringBefore('\n')
        val percent = when (val p = table["percent_finished"]) {
            is Number -> p.toFloat()
            is String -> p.toFloatOrNull()
            else -> null
        }
        val status = summary?.get("status")?.toString()
        return SidecarMeta(title, author, percent, status)
    }

    companion object {
        private val BOOK_EXTENSIONS = setOf(
            "epub", "pdf", "djvu", "fb2", "fb3", "mobi", "azw", "azw3",
            "doc", "docx", "rtf", "cbz", "cbr", "cb7", "cbt", "xps", "oxps",
            "txt", "md", "html", "htm", "xhtml",
        )
        private val NON_BOOK_WHEN_FILTERED = setOf("html", "htm", "xhtml", "txt", "md", "log")
    }
}
