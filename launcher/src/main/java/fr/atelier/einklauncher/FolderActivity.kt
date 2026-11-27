package fr.atelier.einklauncher

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import fr.atelier.einklauncher.databinding.ActivityFolderBinding
import fr.atelier.einklauncher.databinding.ItemDesktopAppBinding
import fr.atelier.koreaderhistory.data.AppIconLoader
import fr.atelier.koreaderhistory.data.LauncherApp
import fr.atelier.koreaderhistory.data.LauncherPrefs

class FolderActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityFolderBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val prefs = LauncherPrefs(this)
        val folderId = intent.getStringExtra(EXTRA_FOLDER_ID) ?: run { finish(); return }
        val folder = prefs.getDesktopItems().find { it.id == folderId && it.type == "folder" }
            ?: run { finish(); return }

        binding.folderTitle.text = folder.label
        val apps = folder.children.mapNotNull { AppIconLoader.resolveLauncherApp(this, it) }
        val adapter = object : RecyclerView.Adapter<VH>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
                val b = ItemDesktopAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                return VH(b)
            }
            override fun getItemCount(): Int = apps.size
            override fun onBindViewHolder(holder: VH, position: Int) {
                val app = apps[position]
                holder.binding.appLabel.text = app.label
                holder.binding.appIcon.setImageDrawable(
                    AppIconLoader.loadActivityDrawable(this@FolderActivity, app.packageName, app.activityName)
                        ?: AppIconLoader.loadDrawable(this@FolderActivity, app.packageName),
                )
                holder.binding.root.setOnClickListener {
                    startActivity(app.launchIntent(this@FolderActivity))
                }
            }
        }
        binding.folderGrid.layoutManager = GridLayoutManager(this, 4)
        binding.folderGrid.adapter = adapter
    }

    private class VH(val binding: ItemDesktopAppBinding) : RecyclerView.ViewHolder(binding.root)

    companion object {
        const val EXTRA_FOLDER_ID = "folder_id"
    }
}
