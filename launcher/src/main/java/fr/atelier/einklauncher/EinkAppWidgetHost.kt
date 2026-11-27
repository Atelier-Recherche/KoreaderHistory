package fr.atelier.einklauncher

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.view.ContextThemeWrapper
import android.widget.RemoteViews

/**
 * Host qui évite le LayoutInflater AppCompat de l'Activity.
 * Sinon ImageButton → AppCompatImageButton et RemoteViews.setImageResource casse
 * (cas Plan Recorder : "AppCompatImageButton can't use method with RemoteViews: setImageResource").
 */
class EinkAppWidgetHost(context: Context, hostId: Int) : AppWidgetHost(context, hostId) {

    override fun onCreateView(
        context: Context,
        appWidgetId: Int,
        appWidget: AppWidgetProviderInfo?,
    ): AppWidgetHostView {
        // android.view.ContextThemeWrapper (pas AppCompat) + applicationContext
        val safe = ContextThemeWrapper(
            context.applicationContext,
            android.R.style.Theme_DeviceDefault_Light_NoActionBar,
        )
        return EinkAppWidgetHostView(safe)
    }
}

class EinkAppWidgetHostView(context: Context) : AppWidgetHostView(context) {
    override fun updateAppWidget(remoteViews: RemoteViews?) {
        try {
            super.updateAppWidget(remoteViews)
        } catch (t: Throwable) {
            WidgetLog.e("updateAppWidget failed id=$appWidgetId", t)
            // Laisse le framework afficher l'état d'erreur plutôt que de crasher
            runCatching { super.updateAppWidget(null) }
        }
    }
}
