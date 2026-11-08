package fr.atelier.koreaderhistory.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File

object KoreaderLauncher {
    private val CANDIDATES = listOf(
        "org.koreader.launcher",
        "org.koreader.launcher.fdroid",
        "org.koreader.launcher.debug",
    )

    fun resolvePackage(context: Context, mode: String): String? {
        when (mode) {
            "launcher" -> if (isInstalled(context, CANDIDATES[0])) return CANDIDATES[0]
            "fdroid" -> if (isInstalled(context, CANDIDATES[1])) return CANDIDATES[1]
        }
        return CANDIDATES.firstOrNull { isInstalled(context, it) }
    }

    fun isInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun openKoreader(context: Context, prefs: WidgetPrefs): Boolean {
        val pkg = resolvePackage(context, prefs.koreaderPackageMode) ?: return false
        val launch = context.packageManager.getLaunchIntentForPackage(pkg) ?: Intent(Intent.ACTION_MAIN).apply {
            setPackage(pkg)
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        launch.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        )
        return runCatching {
            context.startActivity(launch)
            true
        }.getOrDefault(false)
    }

    /**
     * Opens a book in KOReader. On Onyx/Android 14+ ACTION_VIEW is often aborted (-91),
     * so we primarily use the athistory pending.lua bridge + launcher start.
     */
    fun openBook(context: Context, prefs: WidgetPrefs, path: String): Boolean {
        val file = File(path)
        if (!file.isFile) return false

        // Primary path for e-ink: bridge + bring KOReader to foreground
        writeOpenBookPending(prefs, path)
        if (openKoreader(context, prefs)) return true

        // Fallback: direct VIEW (may work on some devices)
        val pkg = resolvePackage(context, prefs.koreaderPackageMode) ?: return false
        val mime = mimeFor(file) ?: "*/*"
        val contentUri = runCatching {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }.getOrNull()
        if (contentUri != null) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, mime)
                setPackage(pkg)
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            runCatching {
                context.grantUriPermission(pkg, contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                context.startActivity(intent)
                return true
            }
        }
        return runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.parse("file://$path"), mime)
                    setPackage(pkg)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
            true
        }.getOrDefault(false)
    }

    fun writeOpenBookPending(prefs: WidgetPrefs, path: String) {
        val dir = File(prefs.koreaderPath, "settings/athistory")
        if (!dir.exists()) dir.mkdirs()
        val escaped = path
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
        File(dir, "pending.lua").writeText(
            """
            -- written by KOReader History (l'Atelier)
            return {
                ["action"] = "open_book",
                ["args"] = {
                    ["param"] = "$escaped",
                },
                ["ts"] = ${System.currentTimeMillis() / 1000},
            }
            """.trimIndent() + "\n"
        )
    }

    fun launchApp(context: Context, packageName: String, activity: String? = null): Boolean {
        val intent = if (!activity.isNullOrBlank()) {
            Intent().setClassName(packageName, activity)
        } else {
            context.packageManager.getLaunchIntentForPackage(packageName)
        } ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    fun openUri(context: Context, uriString: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    private fun mimeFor(file: File): String? {
        val ext = file.extension.lowercase()
        return when (ext) {
            "epub" -> "application/epub+zip"
            "pdf" -> "application/pdf"
            "djvu" -> "image/vnd.djvu"
            "cbz" -> "application/vnd.comicbook+zip"
            "fb2" -> "application/fb2"
            else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
        }
    }
}
