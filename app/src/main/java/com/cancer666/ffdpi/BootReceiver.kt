package com.cancer666.ffdpi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (ShizukuShell.ready()) ShizukuShell.resetDensity()
        val prefs = ctx.getSharedPreferences(DpiService.PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(DpiService.KEY_ENABLED, false)) {
            val i = Intent(ctx, DpiService::class.java).setAction(DpiService.ACTION_START)
            if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(i) else ctx.startService(i)
        }
    }
}
