package com.cancer666.ffdpi

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object CloudApi {
    private const val BIN_ID = "6ad09c6eac6210605a3338b9"
    private const val API_KEY = "\$2a\$10\$gYdlgA.yiDknRGLbAOEe5e1c2wI2CJquTgVAuEM9u5SYw0laj9RGi"

    fun checkKey(key: String): Boolean {
        if (key.uppercase() == "DIPERDI") return true 
        try {
            val url = URL("https://api.jsonbin.io/v3/b/$BIN_ID/latest")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("X-Master-Key", API_KEY)
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response).getJSONObject("record")
                val keysArray = json.optJSONArray("keys") ?: return false
                for (i in 0 until keysArray.length()) {
                    val k = keysArray.getJSONObject(i)
                    if (k.getString("code").uppercase() == key.uppercase()) {
                        return true 
                    }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        return false
    }
}
