package fr.atelier.einklauncher

import android.app.ActivityManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import fr.atelier.koreaderhistory.data.AppIconLoader
import fr.atelier.koreaderhistory.data.LauncherApp
import fr.atelier.koreaderhistory.data.LauncherPrefs

object RecentAppsHelper {

    fun pinnedAndRunning(context: Context, limit: Int = 16): List<LauncherApp> {
        val prefs = LauncherPrefs(context)
        val result = linkedMapOf<String, LauncherApp>()

        prefs.pinnedPackages.forEach { pkg ->
            resolve(context, pkg)?.let { result[it.packageName] = it }
        }

        usageRecent(context, limit * 2).forEach { pkg ->
            if (pkg == context.packageName) return@forEach
            if (pkg in prefs.altTabExclude) return@forEach
            resolve(context, pkg)?.let { result.putIfAbsent(it.packageName, it) }
        }

        if (result.size < 4) {
            @Suppress("DEPRECATION")
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            am.getRunningTasks(limit).forEach { task ->
                val pkg = task.topActivity?.packageName ?: return@forEach
                if (pkg == context.packageName) return@forEach
                resolve(context, pkg)?.let { result.putIfAbsent(it.packageName, it) }
            }
        }

        return result.values.take(limit)
    }

    fun recentForAltTab(context: Context, limit: Int = 12): List<LauncherApp> {
        val prefs = LauncherPrefs(context)
        val exclude = prefs.altTabExclude
        return usageRecent(context, limit * 3)
            .filter { it != context.packageName && it !in exclude }
            .mapNotNull { resolve(context, it) }
            .distinctBy { it.packageName }
            .take(limit)
    }

    private fun usageRecent(context: Context, limit: Int): List<String> {
        return runCatching {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val end = System.currentTimeMillis()
            val start = end - 1000L * 60 * 60 * 24
            usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)
                .orEmpty()
                .sortedByDescending { it.lastTimeUsed }
                .map { it.packageName }
                .filter { pkg ->
                    context.packageManager.getLaunchIntentForPackage(pkg) != null
                }
                .take(limit)
        }.getOrDefault(emptyList())
    }

    private fun resolve(context: Context, packageName: String): LauncherApp? {
        return AppIconLoader.resolveLauncherApp(context, packageName)
            ?: labelFor(context.packageManager, packageName)?.let {
                LauncherApp(packageName, "", it)
            }
    }

    private fun labelFor(pm: PackageManager, packageName: String): String? {
        return runCatching {
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        }.getOrNull()
    }

    fun launch(context: Context, packageName: String, activityName: String = "") {
        AppIconLoader.ensureEnabled(context, packageName)
        if (activityName.isNotBlank()) {
            val intent = Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setClassName(packageName, activityName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            runCatching { context.startActivity(intent) }
            return
        }
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: AppIconLoader.resolveLauncherApp(context, packageName)?.launchIntent(context)
            ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        context.startActivity(intent)
    }
}
