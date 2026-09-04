package com.example.util

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CrashReport(
    val timestamp: Long,
    val formattedDate: String,
    val exceptionName: String,
    val message: String,
    val stackTrace: String,
    val deviceInfo: String,
    val appVersion: String,
    val extraContext: String = ""
)

object CrashReportManager {

    private const val TAG = "CrashReportManager"
    private const val PREFS_NAME = "crash_reporter_prefs"
    private const val KEY_HAS_CRASH = "key_has_pending_crash"
    private const val KEY_TIMESTAMP = "key_timestamp"
    private const val KEY_DATE = "key_date"
    private const val KEY_EXCEPTION = "key_exception"
    private const val KEY_MESSAGE = "key_message"
    private const val KEY_STACKTRACE = "key_stacktrace"
    private const val KEY_DEVICE_INFO = "key_device_info"
    private const val KEY_APP_VERSION = "key_app_version"
    private const val KEY_EXTRA_CONTEXT = "key_extra_context"

    private var appContext: Context? = null
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null
    private var lastRawSnippet: String = ""
    private var lastViewMode: String = ""

    fun init(application: Application) {
        appContext = application.applicationContext
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                Log.e(TAG, "Uncaught exception intercepted in thread ${thread.name}", throwable)
                saveCrash(throwable, thread.name)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist crash report", e)
            } finally {
                // Forward to system default handler to ensure clean process termination
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    fun updateAppContext(rawTextSnippet: String, viewMode: String) {
        lastRawSnippet = rawTextSnippet.take(500)
        lastViewMode = viewMode
    }

    private fun getPrefs(): SharedPreferences? {
        return appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun saveCrash(throwable: Throwable, threadName: String) {
        val prefs = getPrefs() ?: return
        val timestamp = System.currentTimeMillis()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
        val formattedDate = sdf.format(Date(timestamp))

        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        val fullStackTrace = sw.toString()

        val deviceInfo = buildDeviceInfo(threadName)
        val appVersion = "1.0.0 (API ${Build.VERSION.SDK_INT})"
        val extraContext = "ViewMode: $lastViewMode\nRaw Snippet (First 500 chars):\n$lastRawSnippet"

        prefs.edit()
            .putBoolean(KEY_HAS_CRASH, true)
            .putLong(KEY_TIMESTAMP, timestamp)
            .putString(KEY_DATE, formattedDate)
            .putString(KEY_EXCEPTION, throwable.javaClass.name)
            .putString(KEY_MESSAGE, throwable.message ?: "No message provided")
            .putString(KEY_STACKTRACE, fullStackTrace)
            .putString(KEY_DEVICE_INFO, deviceInfo)
            .putString(KEY_APP_VERSION, appVersion)
            .putString(KEY_EXTRA_CONTEXT, extraContext)
            .commit() // Synchronous commit to ensure disk write before process kill
    }

    fun recordException(throwable: Throwable, sourceTag: String = "ManualCatch") {
        Log.e(TAG, "Non-fatal or caught error recorded from $sourceTag", throwable)
        saveCrash(throwable, "Background/Caught:$sourceTag")
    }

    fun hasPendingCrash(): Boolean {
        val prefs = getPrefs() ?: return false
        return prefs.getBoolean(KEY_HAS_CRASH, false)
    }

    fun getPendingCrashReport(): CrashReport? {
        val prefs = getPrefs() ?: return null
        if (!prefs.getBoolean(KEY_HAS_CRASH, false)) return null

        return CrashReport(
            timestamp = prefs.getLong(KEY_TIMESTAMP, System.currentTimeMillis()),
            formattedDate = prefs.getString(KEY_DATE, "Unknown date") ?: "Unknown date",
            exceptionName = prefs.getString(KEY_EXCEPTION, "UnknownException") ?: "UnknownException",
            message = prefs.getString(KEY_MESSAGE, "No details") ?: "No details",
            stackTrace = prefs.getString(KEY_STACKTRACE, "") ?: "",
            deviceInfo = prefs.getString(KEY_DEVICE_INFO, "") ?: "",
            appVersion = prefs.getString(KEY_APP_VERSION, "1.0.0") ?: "1.0.0",
            extraContext = prefs.getString(KEY_EXTRA_CONTEXT, "") ?: ""
        )
    }

    fun clearPendingCrash() {
        val prefs = getPrefs() ?: return
        prefs.edit().putBoolean(KEY_HAS_CRASH, false).apply()
    }

    fun getFormattedFullLog(report: CrashReport): String {
        return buildString {
            appendLine("=== APP CRASH REPORT & DIAGNOSTICS ===")
            appendLine("Timestamp: ${report.formattedDate} (Epoch: ${report.timestamp})")
            appendLine("App Version: ${report.appVersion}")
            appendLine("Exception: ${report.exceptionName}")
            appendLine("Message: ${report.message}")
            appendLine()
            appendLine("--- Device & Environment Info ---")
            appendLine(report.deviceInfo)
            if (report.extraContext.isNotBlank()) {
                appendLine()
                appendLine("--- App State Context ---")
                appendLine(report.extraContext)
            }
            appendLine()
            appendLine("--- Stack Trace ---")
            appendLine(report.stackTrace)
            appendLine("=== END CRASH REPORT ===")
        }
    }

    fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Crash Report", text)
        clipboard.setPrimaryClip(clip)
    }

    private fun buildDeviceInfo(threadName: String): String {
        return buildString {
            appendLine("Thread: $threadName")
            appendLine("Manufacturer: ${Build.MANUFACTURER}")
            appendLine("Brand: ${Build.BRAND}")
            appendLine("Model: ${Build.MODEL}")
            appendLine("Device: ${Build.DEVICE}")
            appendLine("Product: ${Build.PRODUCT}")
            appendLine("Android OS: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
        }
    }
}
