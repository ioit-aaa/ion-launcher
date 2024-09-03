package one.zagura.IonLauncher.util

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.view.ViewGroup.LayoutParams
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import one.zagura.IonLauncher.BuildConfig
import one.zagura.IonLauncher.R
import one.zagura.IonLauncher.ui.ionApplication
import one.zagura.IonLauncher.util.drawable.FillDrawable
import java.lang.reflect.InvocationTargetException
import kotlin.system.exitProcess

class CrashActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val t = intent.extras!!["throwable"] as Throwable

            val stackTrace = TextView(this).apply {
                setTextColor(resources.getColor(R.color.color_text))
                typeface = Typeface.MONOSPACE
                val dp = resources.displayMetrics.density
                val p = (12 * dp).toInt()
                setPadding(p, p, p, p)
                setTextIsSelectable(true)

                text = buildString {
                    appendLine(t.toString())
                    appendLine()
                    appendLine("Device info:")
                    appendLine("    api: " + Build.VERSION.SDK_INT)
                    appendLine("    brand: " + Build.BRAND)
                    appendLine("    model: " + Build.MODEL)
                    appendLine("    ram: " + run {
                        val memInfo = ActivityManager.MemoryInfo()
                        (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(memInfo)
                        formatByteAmount(memInfo.totalMem)
                    })
                    appendLine("Version: " + BuildConfig.VERSION_NAME + " (code: " + BuildConfig.VERSION_CODE + ')')
                    appendLine()
                    for (tr in t.stackTrace)
                        appendLine().append(format(tr)).appendLine()
                    for (throwable in t.suppressed)
                        for (tr in throwable.stackTrace)
                            appendLine().append(format(tr)).appendLine()
                    var cause = t.cause
                    while (cause != null) {
                        appendLine("Caused by: $cause\n")
                        for (tr in cause.stackTrace)
                            appendLine().append(format(tr)).appendLine()
                        cause = cause.cause
                    }
                }
            }

            setContentView(NestedScrollView(this).apply {
                addView(stackTrace, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
            })
            window.decorView.background = FillDrawable(resources.getColor(R.color.color_bg))
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun formatByteAmount(bytes: Long): String {
        return when {
            bytes < 1000 -> "$bytes B"
            bytes < 1000_000 -> "${bytes / 1000} KB"
            bytes < 1000_000_000 -> "${bytes / 1000_000} MB"
            else /* bytes < 1000_000_000_000 */ -> "${bytes / 1000_000_000} GB"
        }
    }

    private fun format(e: StackTraceElement) = buildString {
        if (e.isNativeMethod) {
            append("(Native Method)")
        } else if (e.fileName != null) {
            if (e.lineNumber >= 0) {
                append("at ")
                append(e.fileName)
                append(":")
                append(e.lineNumber)
            } else {
                append("at ")
                append(e.fileName)
            }
        } else {
            if (e.lineNumber >= 0) {
                append("(Unknown Source:")
                append(e.lineNumber)
                append(")")
            } else {
                append("(Unknown Source)")
            }
        }
        appendLine()
        append(e.className)
        append(".")
        append(e.methodName)
    }

    companion object {
        fun init(context: Context) {
            Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
                try {
                    context.ionApplication.settings.saveNow(context)
                    context.startActivity(Intent(context, CrashActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra("throwable", throwable))
                    Process.killProcess(Process.myPid())
                    exitProcess(0)
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }
    }
}