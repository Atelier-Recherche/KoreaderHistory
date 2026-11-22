package fr.atelier.koreaderhistory.data

data class BookEntry(
    val filePath: String,
    val time: Long,
    val title: String,
    val fileNameWithoutExt: String,
    val author: String?,
    val percentFinished: Float?,
    val status: String?,
    val pageCount: Int? = null,
) {
    val displayPercent: Int?
        get() = percentFinished?.let { (it * 100f).toInt().coerceIn(0, 100) }

    fun displayTitle(useFilename: Boolean): String =
        if (useFilename) fileNameWithoutExt
        else title.ifBlank { fileNameWithoutExt }

    fun coverLetter(useFilename: Boolean): String =
        displayTitle(useFilename).trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
}
