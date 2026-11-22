package fr.atelier.koreaderhistory.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.TypedValue

data class LauncherApp(
    val packageName: String,
    val label: String,
)

object AppIconLoader {

    fun listLauncherApps(context: Context): List<LauncherApp> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        return resolveInfos
            .mapNotNull { info ->
                val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                val label = info.loadLabel(pm)?.toString()?.ifBlank { pkg } ?: pkg
                LauncherApp(pkg, label)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    fun loadBitmap(context: Context, packageName: String, sizeDp: Int = 48): Bitmap? {
        if (packageName.isBlank()) return null
        return runCatching {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            val sizePx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                sizeDp.toFloat(),
                context.resources.displayMetrics,
            ).toInt().coerceAtLeast(1)
            drawableToBitmap(drawable, sizePx)
        }.getOrNull()
    }

    fun loadDrawable(context: Context, packageName: String): Drawable? {
        if (packageName.isBlank()) return null
        return runCatching { context.packageManager.getApplicationIcon(packageName) }.getOrNull()
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
