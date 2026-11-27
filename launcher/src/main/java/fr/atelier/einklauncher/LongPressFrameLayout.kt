package fr.atelier.einklauncher

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.FrameLayout

/**
 * Laisse passer les clics au widget enfant, mais détecte l'appui long
 * (les AppWidgetHostView mangent souvent le long-click).
 */
class LongPressFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    var onLongPressAction: (() -> Unit)? = null

    private val detector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true
            override fun onLongPress(e: MotionEvent) {
                onLongPressAction?.invoke()
            }
        },
    )

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        detector.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }
}
