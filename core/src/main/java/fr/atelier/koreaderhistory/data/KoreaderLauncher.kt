package fr.atelier.koreaderhistory.data

import android.content.ComponentName
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
        return runCatching {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        }.getOrDefault(false)
    }

    fun openKoreader(context: Context, prefs: WidgetPrefs): Boolean {
        val pkg = resolvePackage(context, prefs.koreaderPackageMode) ?: return false
        return launchApp(context, pkg)
    }

    fun openBook(context: Context, prefs: WidgetPrefs, filePath: String): Boolean {
        val file = File(filePath)
        if (!file.isFile) return false
        val pkg = resolvePackage(context, prefs.koreaderPackageMode) ?: return false
        val mime = mimeFor(file) ?: "*/*"
        val uri = runCatching {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }.getOrNull()
        if (uri != null) {
            val ok = runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, mime)
                        setPackage(pkg)
                        addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED,
                        )
                    },
                )
                true
            }.getOrDefault(false)
            if (ok) return true
        }
        // Fallback e-ink / KOReader : chemin fichier brut
        return runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.parse("file://$filePath"), mime)
                    setPackage(pkg)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                },
            )
            true
        }.getOrDefault(false)
    }

    fun launchApp(context: Context, packageName: String, activity: String? = null): Boolean {
        if (packageName.isBlank()) return false
        BooxFreezeHelper.tryUnfreeze(context, packageName)

        if (BooxFreezeHelper.isFrozen(context, packageName)) {
            return false
        }

        val intent = buildLaunchIntent(context, packageName, activity) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    private fun buildLaunchIntent(
        context: Context,
        packageName: String,
        activity: String?,
    ): Intent? {
        if (!activity.isNullOrBlank()) {
            return Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(ComponentName(packageName, activity))
        }
        context.packageManager.getLaunchIntentForPackage(packageName)?.let { return it }
        val listed = AppIconLoader.listLauncherApps(context)
            .firstOrNull { it.packageName == packageName }
        if (listed != null) {
            return Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(ComponentName(listed.packageName, listed.activityName))
        }
        // Dernier recours : première activité MAIN même sans LAUNCHER (apps OEM bizarres)
        val main = Intent(Intent.ACTION_MAIN).setPackage(packageName)
        val ri = if (android.os.Build.VERSION.SDK_INT >= 33) {
            context.packageManager.queryIntentActivities(
                main,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.queryIntentActivities(main, PackageManager.MATCH_ALL)
        }
        val ai = ri.firstOrNull()?.activityInfo ?: return null
        return Intent(Intent.ACTION_MAIN).setComponent(ComponentName(ai.packageName, ai.name))
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
