package fr.atelier.einklauncher

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class WidgetPickerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        val mgr = AppWidgetManager.getInstance(this)
        val providers = mgr.installedProviders
            .sortedBy { it.loadLabel(packageManager)?.toString()?.lowercase().orEmpty() }
        WidgetLog.file(this, "WidgetPicker ${providers.size} providers (id=$appWidgetId)")
        providers.forEach {
            WidgetLog.d("provider ${it.provider} cat=${it.widgetCategory} min=${it.minWidth}x${it.minHeight}")
        }

        val list = RecyclerView(this).apply {
            setPadding(dp(12), dp(48), dp(12), dp(12))
            setBackgroundColor(0xFFFFFFFF.toInt())
            layoutManager = LinearLayoutManager(this@WidgetPickerActivity)
            adapter = object : RecyclerView.Adapter<VH>() {
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
                    val tv = TextView(parent.context).apply {
                        setPadding(dp(12), dp(16), dp(12), dp(16))
                        setTextColor(0xFF000000.toInt())
                        textSize = 16f
                    }
                    return VH(tv)
                }

                override fun getItemCount(): Int = providers.size

                override fun onBindViewHolder(holder: VH, position: Int) {
                    val info = providers[position]
                    val label = info.loadLabel(packageManager)?.toString() ?: info.provider.className
                    val pkgLabel = runCatching {
                        packageManager.getApplicationLabel(
                            packageManager.getApplicationInfo(info.provider.packageName, 0),
                        ).toString()
                    }.getOrDefault(info.provider.packageName)
                    holder.tv.text = "$label\n$pkgLabel"
                    holder.tv.setOnClickListener {
                        val data = Intent()
                            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
                        setResult(Activity.RESULT_OK, data)
                        finish()
                    }
                }
            }
        }
        setContentView(list)
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private class VH(val tv: TextView) : RecyclerView.ViewHolder(tv)
}
