package fr.atelier.einklauncher

import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import fr.atelier.einklauncher.R
import fr.atelier.koreaderhistory.data.AppIconLoader
import fr.atelier.einklauncher.databinding.ActivityTaskbarBinding

class TaskBarActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityTaskbarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.taskbarRoot.setOnClickListener { finish() }
        binding.btnNotifs.setOnClickListener {
            startActivity(android.content.Intent(this, NotificationPanelActivity::class.java))
            finish()
        }

        val apps = RecentAppsHelper.pinnedAndRunning(this)
        if (apps.isEmpty()) {
            Toast.makeText(this, R.string.catalog_empty, Toast.LENGTH_SHORT).show()
        }
        val size = (56 * resources.displayMetrics.density).toInt()
        apps.forEach { app ->
            val iv = ImageView(this)
            val lp = LinearLayout.LayoutParams(size, size).apply { setMargins(6, 0, 6, 0) }
            iv.layoutParams = lp
            iv.setBackgroundResource(R.drawable.dock_button_bg)
            iv.setPadding(6, 6, 6, 6)
            iv.scaleType = ImageView.ScaleType.FIT_CENTER
            iv.setImageDrawable(AppIconLoader.loadDrawable(this, app.packageName))
            iv.contentDescription = app.label
            iv.setOnClickListener {
                RecentAppsHelper.launch(this, app.packageName)
                finish()
            }
            binding.taskbarRow.addView(iv)
        }
    }
}

