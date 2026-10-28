package com.cancer666.ffdpi

import android.os.ParcelFileDescriptor
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

object ShizukuShell {
    fun ready(): Boolean = try {
        Shizuku.pingBinder() && Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
    } catch (_: Throwable) { false }

    fun exec(cmd: String): String {
        val pfd = Shizuku.newProcess(arrayOf("sh", "-c", cmd), null, null)
        val reader = BufferedReader(InputStreamReader(ParcelFileDescriptor.AutoCloseInputStream(pfd)))
        val out = StringBuilder()
        reader.forEachLine { out.appendLine(it) }
        return out.toString()
    }

    fun setDensity(dpi: Int): Boolean = try {
        exec("wm density $dpi"); true
    } catch (_: Throwable) { false }

    fun resetDensity(): Boolean = try {
        exec("wm density reset"); true
    } catch (_: Throwable) { false }
}
