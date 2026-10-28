package com.cancer666.ffdpi

import android.app.*
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlin.concurrent.thread

class DpiService : Service() {
    companion object {
        const val CHANNEL_ID = "ffdpi"
        const val NOTIF_ID = 6661
        const val ACTION_START = "start"
        const val ACTION_STOP = "stop"
        const val FF_PKGS = "com.dts.freefireth,com.dts.freefiremax"
        const val PREFS = "ffdpi_prefs"
        const val KEY_DPI = "dpi"
        const val KEY_ENABLED = "enabled"
    }

    @Volatile private var running = false
    private lateinit var prefs: SharedPreferences
    private var worker: Thread? = null

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL_ID, "FF DPI", NotificationManager.IMPORTANCE_LOW))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopSelf(); return START_NOT_STICKY }
        startForeground(NOTIF_ID, buildNotification("Monitoring Free Fire…"))
        if (!running) { running = true; startWorker() }
        return START_STICKY
    }

    private fun startWorker() {
        worker = thread(isDaemon = true) {
            val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val ffSet = FF_PKGS.split(",").toSet()
            var currentState = "normal"
            while (running) {
                try {
                    if (!ShizukuShell.ready()) { Thread.sleep(1500); continue }
                    val now = System.currentTimeMillis()
                    val events = usm.queryEvents(now - 10_000, now)
                    var foreground: String? = null
                    val ev = UsageEvents.Event()
                    while (events.hasNextEvent()) {
                        events.getNextEvent(ev)
                        if (ev.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) foreground = ev.packageName
                    }
                    val target = if (foreground in ffSet) "ff" else "normal"
                    if (target != currentState) {
                        if (target == "ff") {
                            if (ShizukuShell.setDensity(prefs.getInt(KEY_DPI, 650))) {
                                currentState = "ff"; updateNotif("FF aktif — DPI berubah")
                            }
                        } else {
                            if (ShizukuShell.resetDensity()) {
                                currentState = "normal"; updateNotif("Normal — DPI reset")
                            }
                        }
                    }
                    Thread.sleep(800)
                } catch (_: Throwable) { try { Thread.sleep(1500) } catch (_: Throwable) {} }
            }
        }
    }

    private fun buildNotification(text: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("DvXseT Engine").setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_manage).setOngoing(true).build()

    private fun updateNotif(text: String) {
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIF_ID, buildNotification(text))
    }

    override fun onDestroy() {
        running = false; worker?.interrupt()
        if (ShizukuShell.ready()) ShizukuShell.resetDensity()
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
