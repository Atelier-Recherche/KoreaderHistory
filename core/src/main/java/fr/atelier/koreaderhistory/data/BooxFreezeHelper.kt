package fr.atelier.koreaderhistory.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast

/**
 * Sur Boox, App Freeze désactive les packages (enabled=DISABLED_USER).
 * Une app tierce ne peut en général pas les réactiver sans Shizuku/root :
 * on tente quand même, puis on oriente vers l’écran Freeze Boox.
 */
object BooxFreezeHelper {

    const val ACTION_APP_FREEZE = "onyx.settings.action.APP_FREEZE_MANAGEMENT"

    fun isFrozen(context: Context, packageName: String): Boolean {
        if (packageName.isBlank()) return false
        val pm = context.packageManager
        return runCatching {
            val state = pm.getApplicationEnabledSetting(packageName)
            if (state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED ||
                state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER ||
                state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED
            ) {
                return@runCatching true
            }
            val flags = PackageManager.MATCH_DISABLED_COMPONENTS or
                PackageManager.MATCH_DISABLED_UNTIL_USED_COMPONENTS
            val info = if (android.os.Build.VERSION.SDK_INT >= 33) {
                pm.getApplicationInfo(
                    packageName,
                    PackageManager.ApplicationInfoFlags.of(flags.toLong()),
                )
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, flags)
            }
            !info.enabled
        }.getOrDefault(false)
    }

    fun tryUnfreeze(context: Context, packageName: String): Boolean {
        AppIconLoader.ensureEnabled(context, packageName)
        return !isFrozen(context, packageName)
    }

    fun openFreezeSettings(context: Context): Boolean {
        val freeze = Intent(ACTION_APP_FREEZE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (freeze.resolveActivity(context.packageManager) != null) {
            return runCatching {
                context.startActivity(freeze)
                true
            }.getOrDefault(false)
        }
        return runCatching {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            true
        }.getOrDefault(false)
    }

    fun openAppDetails(context: Context, packageName: String) {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.parse("package:$packageName"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    fun toastFrozen(context: Context, label: String) {
        Toast.makeText(
            context,
            "$label est gelée (Boox App Freeze). Dégèle-la puis réessaie.",
            Toast.LENGTH_LONG,
        ).show()
        openFreezeSettings(context)
    }
}
