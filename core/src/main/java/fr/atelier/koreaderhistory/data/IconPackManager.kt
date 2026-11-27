package fr.atelier.koreaderhistory.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.TypedValue
import org.xmlpull.v1.XmlPullParser

data class IconPackInfo(
    val packageName: String,
    val label: String,
)

data class PackIconEntry(
    val drawableName: String,
    val label: String,
)

object IconPackManager {

    private val THEME_ACTIONS = listOf(
        "org.adw.launcher.THEMES",
        "com.novalauncher.THEME",
        "com.gau.go.launcherex.theme",
        "com.dlto.atom.launcher.THEME",
        "com.anddoes.launcher.THEME",
        "com.teslacoilsw.launcher.THEME",
        "org.adw.launcher.icons.ACTION_PICK_ICON",
    )

    fun listInstalledPacks(context: Context): List<IconPackInfo> {
        val pm = context.packageManager
        val found = linkedMapOf<String, IconPackInfo>()
        for (action in THEME_ACTIONS) {
            val intent = Intent(action)
            pm.queryIntentActivities(intent, PackageManager.MATCH_ALL).forEach { info ->
                val pkg = info.activityInfo?.packageName ?: return@forEach
                if (pkg in found) return@forEach
                val label = info.loadLabel(pm)?.toString()?.ifBlank { pkg } ?: pkg
                found[pkg] = IconPackInfo(pkg, label)
            }
        }
        // Also scan for appfilter.xml in installed packages that declare themes metadata
        pm.getInstalledApplications(0).forEach { app ->
            if (app.packageName in found) return@forEach
            runCatching {
                val res = pm.getResourcesForApplication(app.packageName)
                val id = res.getIdentifier("appfilter", "xml", app.packageName)
                if (id != 0) {
                    val label = pm.getApplicationLabel(app).toString()
                    found[app.packageName] = IconPackInfo(app.packageName, label)
                }
            }
        }
        return found.values.sortedBy { it.label.lowercase() }
    }

    fun listPackIcons(context: Context, packPackage: String, limit: Int = 400): List<PackIconEntry> {
        if (packPackage.isBlank()) return emptyList()
        return runCatching {
            val pm = context.packageManager
            val res = pm.getResourcesForApplication(packPackage)
            val names = linkedSetOf<String>()
            val appfilterId = res.getIdentifier("appfilter", "xml", packPackage)
            if (appfilterId != 0) {
                val parser = res.getXml(appfilterId)
                var event = parser.eventType
                while (event != XmlPullParser.END_DOCUMENT && names.size < limit) {
                    if (event == XmlPullParser.START_TAG) {
                        val drawable = parser.getAttributeValue(null, "drawable")
                            ?: parser.getAttributeValue(null, "icon")
                        if (!drawable.isNullOrBlank()) names.add(drawable.removePrefix("@drawable/"))
                    }
                    event = parser.next()
                }
            }
            if (names.isEmpty()) {
                // Fallback: enumerate drawable field names is not possible without reflection;
                // try common drawable list resource
                val listId = res.getIdentifier("icon_pack", "array", packPackage)
                if (listId != 0) {
                    res.getStringArray(listId).forEach { names.add(it.removePrefix("@drawable/")) }
                }
            }
            names.take(limit).map { PackIconEntry(it, it) }
        }.getOrDefault(emptyList())
    }

    fun loadPackDrawable(context: Context, packPackage: String, drawableName: String): Drawable? {
        if (packPackage.isBlank() || drawableName.isBlank()) return null
        return runCatching {
            val pm = context.packageManager
            val res = pm.getResourcesForApplication(packPackage)
            val id = res.getIdentifier(drawableName, "drawable", packPackage)
            if (id == 0) null else res.getDrawable(id, null)
        }.getOrNull()
    }

    fun loadPackBitmap(
        context: Context,
        packPackage: String,
        drawableName: String,
        sizeDp: Int = 48,
    ): Bitmap? {
        val drawable = loadPackDrawable(context, packPackage, drawableName) ?: return null
        val sizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            sizeDp.toFloat(),
            context.resources.displayMetrics,
        ).toInt().coerceAtLeast(1)
        return drawableToBitmap(drawable, sizePx)
    }

    private fun drawableToBitmap(drawable: Drawable, sizePx: Int): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            val src = drawable.bitmap
            if (src.width == sizePx && src.height == sizePx) return src
            return Bitmap.createScaledBitmap(src, sizePx, sizePx, true)
        }
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
