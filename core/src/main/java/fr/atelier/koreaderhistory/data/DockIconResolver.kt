package fr.atelier.koreaderhistory.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable

object DockIconResolver {

    fun loadDrawable(context: Context, slot: DockSlot): Drawable? {
        IconPackManager.loadPackDrawable(context, slot.iconPackPackage, slot.iconDrawableName)?.let { return it }
        AppIconLoader.loadDrawable(context, slot.iconPackage)?.let { return it }
        return context.getDrawable(IconCatalog.resId(slot.iconKey))
    }

    fun loadBitmap(context: Context, slot: DockSlot, sizeDp: Int): Bitmap? {
        IconPackManager.loadPackBitmap(context, slot.iconPackPackage, slot.iconDrawableName, sizeDp)
            ?.let { return it }
        AppIconLoader.loadBitmap(context, slot.iconPackage, sizeDp)?.let { return it }
        val drawable = context.getDrawable(IconCatalog.resId(slot.iconKey)) ?: return null
        return AppIconLoader.drawableToBitmapPublic(drawable, sizeDp, context)
    }
}
