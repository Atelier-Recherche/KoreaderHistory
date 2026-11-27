package fr.atelier.einklauncher

import android.content.Context
import android.util.Log
import android.widget.Toast
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object WidgetLog {
    const val TAG = "EinkLauncher"

    fun d(msg: String) {
        Log.d(TAG, msg)
    }

    fun e(msg: String, t: Throwable? = null) {
        if (t != null) Log.e(TAG, msg, t) else Log.e(TAG, msg)
    }

    fun toast(context: Context, msg: String) {
        d("TOAST: $msg")
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
    }

    fun file(context: Context, msg: String) {
        d(msg)
        runCatching {
            val dir = context.getExternalFilesDir(null) ?: context.filesDir
            val f = File(dir, "widget_debug.log")
            val ts = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
            f.appendText("$ts  $msg\n")
        }
    }
}
