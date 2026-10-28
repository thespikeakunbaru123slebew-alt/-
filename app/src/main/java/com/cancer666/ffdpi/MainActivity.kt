package com.cancer666.ffdpi

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlin.concurrent.thread
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {
    private val REQ_SHIZUKU = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // --- LAYAR LOGIN (PAJANGAN + CEK CLOUD) ---
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        if (!prefs.getBoolean("is_logged_in", false)) {
            showLoginScreen(prefs)
        } else {
            showMainMenu()
        }
    }

    private fun showLoginScreen(prefs: android.content.SharedPreferences) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 150, 50, 50)
            setBackgroundColor(android.graphics.Color.parseColor("#F0F8FF"))
        }
        
        val title = TextView(this).apply {
            text = "DvXseT PRO\n(Secure Access)"
            textSize = 24f
            setTextColor(android.graphics.Color.parseColor("#00D2FF"))
            gravity = android.view.Gravity.CENTER
        }
        
        val input = EditText(this).apply {
            hint = "Masukkan Key Anda"
            gravity = android.view.Gravity.CENTER
        }
        
        val btn = Button(this).apply {
            text = "CONNECT"
            setBackgroundColor(android.graphics.Color.parseColor("#00D2FF"))
            setTextColor(android.graphics.Color.WHITE)
        }

        layout.addView(title)
        layout.addView(input)
        layout.addView(btn)
        setContentView(layout)

        btn.setOnClickListener {
            val keyInput = input.text.toString().trim()
            if (keyInput.isEmpty()) {
                Toast.makeText(this, "Key kosong!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            btn.text = "CHECKING..."
            btn.isEnabled = false
            
            // Cek ke JSONBin di background
            thread {
                val isValid = CloudApi.checkKey(keyInput)
                runOnUiThread {
                    if (isValid) {
                        prefs.edit().putBoolean("is_logged_in", true).apply()
                        Toast.makeText(this, "Login Berhasil!", Toast.LENGTH_SHORT).show()
                        showMainMenu()
                    } else {
                        Toast.makeText(this, "Key Tidak Valid / Expired!", Toast.LENGTH_SHORT).show()
                        btn.text = "CONNECT"
                        btn.isEnabled = true
                    }
                }
            }
        }
    }

    // --- LAYAR UTAMA (DPI & FITUR PAJANGAN) ---
    private fun showMainMenu() {
        setContentView(R.layout.activity_main)

        Shizuku.addRequestPermissionResultListener(permissionListener)

        val dpiInput = findViewById<EditText>(R.id.dpiInput)
        val status = findViewById<TextView>(R.id.status)
        val btnShizuku = findViewById<Button>(R.id.btnShizuku)
        val btnUsage = findViewById<Button>(R.id.btnUsage)
        val btnStart = findViewById<Button>(R.id.btnStart)
        val btnStop = findViewById<Button>(R.id.btnStop)
        val btnReset = findViewById<Button>(R.id.btnReset)

        val btnHook = findViewById<Button>(R.id.btnHook)
        val btnAimbot = findViewById<Button>(R.id.btnAimbot)
        val btnEsp = findViewById<Button>(R.id.btnEsp)

        val prefsDpi = getSharedPreferences(DpiService.PREFS, MODE_PRIVATE)
        dpiInput.setText(prefsDpi.getInt(DpiService.KEY_DPI, 650).toString())

        btnShizuku.setOnClickListener {
            if (!Shizuku.pingBinder()) { toast("Install & start Shizuku dulu"); return@setOnClickListener }
            if (Shizuku.isPreV11() || Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                toast("Shizuku OK")
            } else {
                Shizuku.requestPermission(REQ_SHIZUKU)
            }
            refreshStatus(status)
        }

        btnUsage.setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

        btnStart.setOnClickListener {
            val dpi = dpiInput.text.toString().toIntOrNull()
            if (dpi == null || dpi !in 100..1000) { toast("DPI 100-1000"); return@setOnClickListener }
            if (!usageGranted()) { toast("Grant Usage Access dulu"); return@setOnClickListener }
            if (!ShizukuShell.ready()) { toast("Shizuku belum ready"); return@setOnClickListener }
            
            prefsDpi.edit().putInt(DpiService.KEY_DPI, dpi).putBoolean(DpiService.KEY_ENABLED, true).apply()
            val i = Intent(this, DpiService::class.java).setAction(DpiService.ACTION_START)
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i) else startService(i)
            toast("Engine Jalan, DPI $dpi")
            refreshStatus(status)
        }

        btnStop.setOnClickListener {
            startService(Intent(this, DpiService::class.java).setAction(DpiService.ACTION_STOP))
            prefsDpi.edit().putBoolean(DpiService.KEY_ENABLED, false).apply()
            if (ShizukuShell.ready()) ShizukuShell.resetDensity()
            toast("Stop + reset")
            refreshStatus(status)
        }

        btnReset.setOnClickListener {
            if (ShizukuShell.ready()) ShizukuShell.resetDensity()
            toast("Density reset")
        }

        // Tombol Pajangan Visual
        btnHook.setOnClickListener { toast("Hook FF bypassed (Visual Only)") }
        btnAimbot.setOnClickListener { toast("Aimbot aktif (Visual Only)") }
        btnEsp.setOnClickListener { toast("ESP dinyalain (Visual Only)") }

        refreshStatus(status)
    }

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { req, res ->
        if (req == REQ_SHIZUKU) {
            toast(if (res == android.content.pm.PackageManager.PERMISSION_GRANTED) "Shizuku granted" else "Shizuku denied")
        }
    }

    private fun refreshStatus(textView: TextView) {
        val sh = if (ShizukuShell.ready()) "OK" else "belum"
        val us = if (usageGranted()) "OK" else "belum"
        textView.text = "System Status:\nShizuku: $sh\nUsage Access: $us"
    }

    private fun usageGranted(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= 29)
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
        else
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()

    override fun onDestroy() {
        try { Shizuku.removeRequestPermissionResultListener(permissionListener) } catch (_: Exception) {}
        super.onDestroy()
    }
}
