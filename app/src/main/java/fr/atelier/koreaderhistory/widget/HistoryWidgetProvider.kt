package fr.atelier.koreaderhistory.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import fr.atelier.koreaderhistory.R
import fr.atelier.koreaderhistory.data.DockIconResolver
import fr.atelier.koreaderhistory.data.HistoryReader
import fr.atelier.koreaderhistory.data.WidgetPrefs
import fr.atelier.koreaderhistory.ui.WidgetConfigureActivity

class HistoryWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { updateAppWidget(context, appWidgetManager, it) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH || intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                ?: mgr.getAppWidgetIds(ComponentName(context, HistoryWidgetProvider::class.java))
            ids.forEach { updateAppWidget(context, mgr, it) }
        }
    }

    companion object {
        const val ACTION_REFRESH = "fr.atelier.koreaderhistory.ACTION_REFRESH"

        fun refreshAll(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, HistoryWidgetProvider::class.java))
            val intent = Intent(context, HistoryWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
            ids.forEach { updateAppWidget(context, mgr, it) }
        }

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            try {
                val prefs = WidgetPrefs(context)
                val views = RemoteViews(context.packageName, R.layout.widget_history)

                val fg = if (prefs.darkTheme) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
                val muted = if (prefs.darkTheme) 0xFFBBBBBB.toInt() else 0xFF444444.toInt()
                val bg = if (prefs.darkTheme) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()

                views.setInt(R.id.widget_root, "setBackgroundColor", bg)
                views.setViewVisibility(R.id.widget_title, View.GONE)
                views.setTextColor(R.id.empty_view, muted)

                val density = context.resources.displayMetrics.density
                val sidePad = (4 * density).toInt()
                val topPad = (prefs.widgetMarginTopDp * density).toInt()
                val bottomPad = (prefs.widgetMarginBottomDp * density).toInt()
                views.setViewPadding(R.id.widget_root, sidePad, topPad, sidePad, bottomPad)

                val titleSp = prefs.historyFontSp.toFloat()
                views.setTextViewTextSize(R.id.empty_view, TypedValue.COMPLEX_UNIT_SP, titleSp)

                bindBooks(context, views, prefs, fg, muted, appWidgetId)
                bindDock(context, views, prefs, appWidgetId, fg)

                val refreshPi = activityPi(
                    context,
                    1000 + appWidgetId,
                    WidgetClickActivity.refreshIntent(context),
                )
                views.setOnClickPendingIntent(R.id.btn_refresh_empty, refreshPi)

                val cfgIntent = Intent(context, WidgetConfigureActivity::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                views.setOnClickPendingIntent(
                    R.id.empty_view,
                    activityPi(context, 2000 + appWidgetId, cfgIntent),
                )

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (t: Throwable) {
                Log.e("HistoryWidget", "updateAppWidget failed for $appWidgetId", t)
                runCatching {
                    val fallback = RemoteViews(context.packageName, R.layout.widget_history)
                    fallback.setViewVisibility(R.id.widget_title, View.GONE)
                    fallback.setViewVisibility(R.id.empty_view, View.VISIBLE)
                    fallback.setTextViewText(R.id.empty_view, "Erreur widget — rouvrir l’app")
                    appWidgetManager.updateAppWidget(appWidgetId, fallback)
                }
            }
        }

        private fun bindBooks(
            context: Context,
            views: RemoteViews,
            prefs: WidgetPrefs,
            fg: Int,
            muted: Int,
            appWidgetId: Int,
        ) {
            views.removeAllViews(R.id.book_container)
            val books = runCatching { HistoryReader(prefs).loadBooks() }.getOrDefault(emptyList())
            val titleSp = prefs.historyFontSp.toFloat()
            val mutedSp = (prefs.historyFontSp - 2).coerceAtLeast(8).toFloat()
            val coverSp = (prefs.historyFontSp + 3).coerceAtMost(22).toFloat()
            val pctSp = (prefs.historyFontSp - 1).coerceAtLeast(9).toFloat()

            if (books.isEmpty()) {
                views.setViewVisibility(R.id.empty_view, View.VISIBLE)
                views.setViewVisibility(R.id.book_container, View.GONE)
                views.setViewVisibility(R.id.btn_refresh_empty, View.VISIBLE)
                return
            }
            views.setViewVisibility(R.id.empty_view, View.GONE)
            views.setViewVisibility(R.id.book_container, View.VISIBLE)
            views.setViewVisibility(R.id.btn_refresh_empty, View.GONE)

            val refreshPi = activityPi(
                context,
                1000 + appWidgetId,
                WidgetClickActivity.refreshIntent(context),
            )

            books.forEachIndexed { index, book ->
                val row = RemoteViews(context.packageName, R.layout.widget_book_item)
                val displayTitle = book.displayTitle(prefs.useFilenameAsTitle)
                val isLast = index == books.lastIndex

                if (prefs.showCoverLetter) {
                    row.setViewVisibility(R.id.cover_letter, View.VISIBLE)
                    row.setTextViewText(R.id.cover_letter, book.coverLetter(prefs.useFilenameAsTitle))
                    row.setTextColor(R.id.cover_letter, fg)
                    row.setTextViewTextSize(R.id.cover_letter, TypedValue.COMPLEX_UNIT_SP, coverSp)
                } else {
                    row.setViewVisibility(R.id.cover_letter, View.GONE)
                }

                row.setTextViewText(R.id.book_title, displayTitle)
                row.setTextColor(R.id.book_title, fg)
                row.setTextViewTextSize(R.id.book_title, TypedValue.COMPLEX_UNIT_SP, titleSp)

                if (prefs.showAuthor && !book.author.isNullOrBlank()) {
                    row.setViewVisibility(R.id.book_author, View.VISIBLE)
                    row.setTextViewText(R.id.book_author, book.author)
                    row.setTextColor(R.id.book_author, muted)
                    row.setTextViewTextSize(R.id.book_author, TypedValue.COMPLEX_UNIT_SP, mutedSp)
                } else {
                    row.setViewVisibility(R.id.book_author, View.GONE)
                }

                if (prefs.showPages && book.pageCount != null) {
                    row.setViewVisibility(R.id.book_pages, View.VISIBLE)
                    row.setTextViewText(R.id.book_pages, "${book.pageCount} p.")
                    row.setTextColor(R.id.book_pages, muted)
                    row.setTextViewTextSize(R.id.book_pages, TypedValue.COMPLEX_UNIT_SP, mutedSp)
                } else {
                    row.setViewVisibility(R.id.book_pages, View.GONE)
                }

                val pct = book.displayPercent
                if (prefs.showPercent && pct != null) {
                    row.setViewVisibility(R.id.book_progress, View.VISIBLE)
                    row.setViewVisibility(R.id.book_percent, View.VISIBLE)
                    row.setProgressBar(R.id.book_progress, 100, pct, false)
                    row.setTextViewText(R.id.book_percent, "$pct%")
                    row.setTextColor(R.id.book_percent, fg)
                    row.setTextViewTextSize(R.id.book_percent, TypedValue.COMPLEX_UNIT_SP, pctSp)
                } else {
                    row.setViewVisibility(R.id.book_progress, View.GONE)
                    row.setViewVisibility(R.id.book_percent, View.GONE)
                }

                if (isLast) {
                    row.setViewVisibility(R.id.btn_refresh, View.VISIBLE)
                    row.setOnClickPendingIntent(R.id.btn_refresh, refreshPi)
                } else {
                    row.setViewVisibility(R.id.btn_refresh, View.GONE)
                }

                val openPi = activityPi(
                    context,
                    4000 + index + book.filePath.hashCode(),
                    WidgetClickActivity.openBookIntent(context, book.filePath),
                )
                row.setOnClickPendingIntent(R.id.book_item_root, openPi)
                views.addView(R.id.book_container, row)
            }
        }

        private fun activityPi(context: Context, requestCode: Int, intent: Intent): PendingIntent {
            return PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private fun bindDock(
            context: Context,
            views: RemoteViews,
            prefs: WidgetPrefs,
            appWidgetId: Int,
            fg: Int,
        ) {
            views.removeAllViews(R.id.dock_top)
            views.removeAllViews(R.id.dock_bottom)
            if (!prefs.dockEnabled) {
                views.setViewVisibility(R.id.dock_top, View.GONE)
                views.setViewVisibility(R.id.dock_bottom, View.GONE)
                return
            }

            val slots = prefs.ensureDockSlots()
            val top = prefs.dockOnTop
            val sizeDp = prefs.dockIconSizeDp
            val justify = prefs.dockJustify
            val border = prefs.dockIconBorder
            val showLabels = prefs.dockShowLabels
            val dockId = if (top) R.id.dock_top else R.id.dock_bottom
            val otherId = if (top) R.id.dock_bottom else R.id.dock_top

            if (slots.isEmpty()) {
                views.setViewVisibility(R.id.dock_top, View.GONE)
                views.setViewVisibility(R.id.dock_bottom, View.GONE)
                return
            }

            views.setViewVisibility(dockId, View.VISIBLE)
            views.setViewVisibility(otherId, View.GONE)

            val density = context.resources.displayMetrics.density
            val dockPadTop = (prefs.dockMarginTopDp * density).toInt()
            val dockPadBottom = (prefs.dockMarginBottomDp * density).toInt()
            views.setViewPadding(dockId, 0, dockPadTop, 0, dockPadBottom)
            views.setViewPadding(otherId, 0, 0, 0, 0)

            val spaceBetween = justify == WidgetPrefs.JUSTIFY_SPACE_BETWEEN && slots.size > 1
            slots.forEachIndexed { i, slot ->
                if (spaceBetween && i > 0) {
                    views.addView(dockId, RemoteViews(context.packageName, R.layout.widget_dock_spacer))
                }
                val slotRv = RemoteViews(context.packageName, R.layout.widget_dock_slot)
                val iconId = R.id.dock_slot_icon
                slotRv.setViewVisibility(iconId, View.VISIBLE)
                val bmp = DockIconResolver.loadBitmap(context, slot, sizeDp)
                if (bmp != null) {
                    slotRv.setImageViewBitmap(iconId, bmp)
                } else {
                    slotRv.setImageViewResource(
                        iconId,
                        fr.atelier.koreaderhistory.data.IconCatalog.resId(slot.iconKey),
                    )
                }
                if (border) {
                    slotRv.setInt(iconId, "setBackgroundResource", R.drawable.dock_button_bg)
                } else {
                    slotRv.setInt(iconId, "setBackgroundResource", 0)
                }
                slotRv.setContentDescription(iconId, slot.label)
                applyDockSize(slotRv, iconId, sizeDp)

                if (showLabels && slot.label.isNotBlank()) {
                    slotRv.setViewVisibility(R.id.dock_slot_label, View.VISIBLE)
                    slotRv.setTextViewText(R.id.dock_slot_label, slot.label)
                    slotRv.setTextColor(R.id.dock_slot_label, fg)
                } else {
                    slotRv.setViewVisibility(R.id.dock_slot_label, View.GONE)
                }

                val dockPi = activityPi(
                    context,
                    3000 + appWidgetId * 20 + i,
                    WidgetClickActivity.dockIntent(context, slot.id, appWidgetId * 20 + i),
                )
                slotRv.setOnClickPendingIntent(R.id.dock_slot_root, dockPi)
                slotRv.setOnClickPendingIntent(iconId, dockPi)
                views.addView(dockId, slotRv)
            }
        }

        private fun applyDockSize(views: RemoteViews, viewId: Int, sizeDp: Int) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                views.setViewLayoutWidth(viewId, sizeDp.toFloat(), TypedValue.COMPLEX_UNIT_DIP)
                views.setViewLayoutHeight(viewId, sizeDp.toFloat(), TypedValue.COMPLEX_UNIT_DIP)
            }
            val pad = when {
                sizeDp <= 36 -> 4
                sizeDp <= 48 -> 6
                sizeDp <= 64 -> 8
                sizeDp <= 80 -> 10
                else -> 12
            }
            views.setViewPadding(viewId, pad, pad, pad, pad)
        }
    }
}
