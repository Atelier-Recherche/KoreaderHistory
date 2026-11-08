package fr.atelier.koreaderhistory.data

data class BookEntry(
    val filePath: String,
    val time: Long,
    val title: String,
    val author: String?,
    val percentFinished: Float?,
    val status: String?,
) {
    val displayPercent: Int?
        get() = percentFinished?.let { (it * 100f).toInt().coerceIn(0, 100) }

    val coverLetter: String
        get() = title.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
}
