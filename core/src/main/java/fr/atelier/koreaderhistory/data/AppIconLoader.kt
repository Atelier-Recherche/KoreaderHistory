package fr.atelier.koreaderhistory.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.TypedValue

data class LauncherApp(
    val packageName: String,
    val activityName: String,
    val label: String,
) {
    val componentKey: String get() = "$packageName/$activityName"

    fun launchIntent(context: Context? = null): Intent {
        context?.let { AppIconLoader.ensureEnabled(it, packageName) }
        return Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(ComponentName(packageName, activityName))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

object AppIconLoader {

    /** Inclut apps gelées / disabled-user (souvent le cas sur Boox). */
    private val launcherMatchFlags: Int
        get() = PackageManager.MATCH_ALL or
            PackageManager.MATCH_DISABLED_COMPONENTS or
            PackageManager.MATCH_DISABLED_UNTIL_USED_COMPONENTS

    /**
     * Une entrée par package (tiroir lisible), y compris apps désactivées/gelées.
     * Si plusieurs activités LAUNCHER existent, on garde l’activité principale.
     */
    fun listLauncherApps(context: Context): List<LauncherApp> {
        val pm = context.packageManager
        val byPackage = linkedMapOf<String, MutableList<LauncherApp>>()

        fun ingest(resolveInfos: List<android.content.pm.ResolveInfo>) {
            resolveInfos.forEach { info ->
                val ai = info.activityInfo ?: return@forEach
                // Ignore les activity-aliases secondaires si le target est déjà listé :
                // on garde le component déclaré (alias ou activité), le choix final est plus bas.
                val pkg = ai.packageName
                val activity = ai.name
                val label = info.loadLabel(pm)?.toString()?.ifBlank { pkg } ?: pkg
                byPackage.getOrPut(pkg) { mutableListOf() }.add(LauncherApp(pkg, activity, label))
            }
        }

        val mainIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        ingest(queryActivities(pm, mainIntent))

        // Complète les packages disabled que la query globale rate parfois sur OEM.
        installedApplications(pm).forEach { appInfo ->
            if (byPackage.containsKey(appInfo.packageName)) return@forEach
            val pkgIntent = Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setPackage(appInfo.packageName)
            ingest(queryActivities(pm, pkgIntent))
        }

        return byPackage.mapNotNull { (pkg, candidates) ->
            pickPrimary(pm, pkg, candidates)
        }.sortedBy { it.label.lowercase() }
    }

    private fun pickPrimary(
        pm: PackageManager,
        packageName: String,
        candidates: List<LauncherApp>,
    ): LauncherApp? {
        if (candidates.isEmpty()) return null
        val unique = candidates.distinctBy { it.activityName }
        // Préfère l’activité que le système considère comme launch intent (si l’app est enabled).
        val preferred = runCatching {
            pm.getLaunchIntentForPackage(packageName)?.component?.className
        }.getOrNull()
        if (!preferred.isNullOrBlank()) {
            unique.firstOrNull { it.activityName == preferred }?.let { return it }
        }
        // Sinon : activité dont le label = label application, sinon la première.
        val appLabel = runCatching {
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        }.getOrNull()
        if (!appLabel.isNullOrBlank()) {
            unique.firstOrNull { it.label.equals(appLabel, ignoreCase = true) }?.let { return it }
        }
        return unique.minByOrNull { it.activityName }
    }

    fun ensureEnabled(context: Context, packageName: String) {
        if (packageName.isBlank()) return
        val pm = context.packageManager
        runCatching {
            val state = pm.getApplicationEnabledSetting(packageName)
            if (state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED ||
                state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER ||
                state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED
            ) {
                pm.setApplicationEnabledSetting(
                    packageName,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    0,
                )
            }
        }
    }

    fun resolveLauncherApp(context: Context, keyOrPackage: String): LauncherApp? {
        if (keyOrPackage.isBlank()) return null
        val slash = keyOrPackage.indexOf('/')
        if (slash > 0) {
            val pkg = keyOrPackage.substring(0, slash)
            val activity = keyOrPackage.substring(slash + 1)
            val label = runCatching {
                context.packageManager.getActivityInfo(ComponentName(pkg, activity), 0)
                    .loadLabel(context.packageManager).toString()
            }.getOrElse {
                runCatching {
                    context.packageManager.getApplicationLabel(
                        context.packageManager.getApplicationInfo(pkg, 0),
                    ).toString()
                }.getOrDefault(pkg)
            }
            return LauncherApp(pkg, activity, label)
        }
        return listLauncherApps(context).firstOrNull { it.packageName == keyOrPackage }
            ?: run {
                val launch = context.packageManager.getLaunchIntentForPackage(keyOrPackage) ?: return null
                val cn = launch.component ?: return null
                val label = runCatching {
                    context.packageManager.getApplicationLabel(
                        context.packageManager.getApplicationInfo(keyOrPackage, 0),
                    ).toString()
                }.getOrDefault(keyOrPackage)
                LauncherApp(cn.packageName, cn.className, label)
            }
    }

    private fun queryActivities(pm: PackageManager, intent: Intent): List<android.content.pm.ResolveInfo> {
        return if (Build.VERSION.SDK_INT >= 33) {
            pm.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(launcherMatchFlags.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, launcherMatchFlags)
        }
    }

    private fun installedApplications(pm: PackageManager): List<ApplicationInfo> {
        val flags = PackageManager.MATCH_ALL or
            PackageManager.MATCH_DISABLED_COMPONENTS or
            PackageManager.MATCH_DISABLED_UNTIL_USED_COMPONENTS
        return if (Build.VERSION.SDK_INT >= 33) {
            pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledApplications(flags)
        }
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

    fun loadActivityDrawable(context: Context, packageName: String, activityName: String): Drawable? {
        if (packageName.isBlank()) return null
        return runCatching {
            if (activityName.isNotBlank()) {
                context.packageManager.getActivityIcon(ComponentName(packageName, activityName))
            } else {
                context.packageManager.getApplicationIcon(packageName)
            }
        }.getOrNull()
    }

    fun loadDrawable(context: Context, packageName: String): Drawable? {
        if (packageName.isBlank()) return null
        return runCatching { context.packageManager.getApplicationIcon(packageName) }.getOrNull()
    }

    fun drawableToBitmapPublic(drawable: Drawable, sizeDp: Int, context: Context): Bitmap {
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
