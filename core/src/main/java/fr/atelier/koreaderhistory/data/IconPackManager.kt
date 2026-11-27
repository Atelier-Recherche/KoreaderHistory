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
import org.xmlpull.v1.XmlPullParserFactory
import java.util.concurrent.ConcurrentHashMap

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

    private val ARRAY_NAMES = listOf(
        "icon_pack",
        "icons",
        "iconpack",
        "drawable",
        "drawables",
        "icon_names",
    )

    /** Cache liste complète par package (noms seulement). */
    private val iconCache = ConcurrentHashMap<String, List<PackIconEntry>>()

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

    fun listPackIcons(context: Context, packPackage: String): List<PackIconEntry> {
        if (packPackage.isBlank()) return emptyList()
        iconCache[packPackage]?.let { return it }
        val listed = loadPackIconsUncached(context, packPackage)
        iconCache[packPackage] = listed
        return listed
    }

    fun clearCache(packPackage: String? = null) {
        if (packPackage == null) iconCache.clear() else iconCache.remove(packPackage)
    }

    private fun loadPackIconsUncached(context: Context, packPackage: String): List<PackIconEntry> {
        return runCatching {
            val pm = context.packageManager
            val res = pm.getResourcesForApplication(packPackage)
            val names = linkedSetOf<String>()

            // 1) res/xml/appfilter.xml (complet, sans limite)
            val appfilterId = res.getIdentifier("appfilter", "xml", packPackage)
            if (appfilterId != 0) {
                collectDrawablesFromXml(res.getXml(appfilterId), names)
            }

            // 2) assets/appfilter.xml (certains packs)
            runCatching {
                val pkgCtx = context.createPackageContext(
                    packPackage,
                    Context.CONTEXT_IGNORE_SECURITY,
                )
                pkgCtx.assets.open("appfilter.xml").use { input ->
                    val parser = XmlPullParserFactory.newInstance().newPullParser().apply {
                        setInput(input, "UTF-8")
                    }
                    collectDrawablesFromXml(parser, names)
                }
            }

            // 3) arrays classiques (icon_pack, drawable, …)
            for (arrayName in ARRAY_NAMES) {
                val listId = res.getIdentifier(arrayName, "array", packPackage)
                if (listId == 0) continue
                runCatching {
                    res.getStringArray(listId).forEach { raw ->
                        val name = normalizeDrawableName(raw)
                        if (name.isNotBlank()) names.add(name)
                    }
                }
            }

            // 4) drawable.xml (Nova / ADW)
            val drawableXmlId = res.getIdentifier("drawable", "xml", packPackage)
            if (drawableXmlId != 0) {
                collectDrawablesFromXml(res.getXml(drawableXmlId), names)
            }

            // 5) Si encore peu d’entrées : énumérer R.drawable via réflexion
            if (names.size < 50) {
                collectDrawablesFromReflection(packPackage, names)
            }

            names
                .filter { isLikelyIconName(it) }
                .sortedBy { it.lowercase() }
                .map { PackIconEntry(it, it) }
        }.getOrDefault(emptyList())
    }

    private fun collectDrawablesFromXml(parser: XmlPullParser, names: MutableSet<String>) {
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                val drawable = parser.getAttributeValue(null, "drawable")
                    ?: parser.getAttributeValue(null, "icon")
                    ?: parser.getAttributeValue(null, "name")
                    ?: attributeEndsWith(parser, "drawable")
                if (!drawable.isNullOrBlank()) {
                    val name = normalizeDrawableName(drawable)
                    if (name.isNotBlank()) names.add(name)
                }
            }
            event = parser.next()
        }
    }

    private fun attributeEndsWith(parser: XmlPullParser, suffix: String): String? {
        for (i in 0 until parser.attributeCount) {
            val attrName = parser.getAttributeName(i) ?: continue
            if (attrName.equals(suffix, ignoreCase = true) || attrName.endsWith(":$suffix")) {
                return parser.getAttributeValue(i)
            }
        }
        return null
    }

    private fun collectDrawablesFromReflection(packPackage: String, names: MutableSet<String>) {
        runCatching {
            val cls = Class.forName("$packPackage.R\$drawable")
            cls.fields.forEach { field ->
                runCatching {
                    if (java.lang.reflect.Modifier.isStatic(field.modifiers)) {
                        val name = field.name
                        if (isLikelyIconName(name)) names.add(name)
                    }
                }
            }
        }
    }

    private fun normalizeDrawableName(raw: String): String {
        var s = raw.trim()
        if (s.startsWith("@drawable/", ignoreCase = true)) {
            s = s.substringAfter('/')
        } else if (s.startsWith("drawable/", ignoreCase = true)) {
            s = s.substringAfter('/')
        }
        // Ignore resource ids numériques
        if (s.toIntOrNull() != null) return ""
        return s
    }

    private fun isLikelyIconName(name: String): Boolean {
        if (name.isBlank()) return false
        if (name.startsWith("abc_")) return false
        if (name.startsWith("notification_")) return false
        if (name.startsWith("ic_mtrl_")) return false
        if (name == "icon_background" || name == "adaptive_icon_foreground") return false
        return true
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
