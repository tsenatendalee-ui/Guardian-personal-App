package com.guardian.personal

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.graphics.Typeface
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = 0xFF102A43.toInt()
        window.navigationBarColor = 0xFF102A43.toInt()
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(28), dp(22), dp(24))
            setBackgroundColor(0xFFF4F7FB.toInt())
        }
        fun label(text: String, size: Float, color: Int, bold: Boolean = false) {
            content.addView(TextView(this).apply {
                this.text = text; textSize = size; setTextColor(color)
                if (bold) setTypeface(null, Typeface.BOLD)
                setPadding(0, dp(8), 0, dp(8))
            })
        }
        label("GUARDIAN", 30f, 0xFF102A43.toInt(), true)
        label("Personal device protection", 16f, 0xFF486581.toInt())
        label("Your security dashboard", 20f, 0xFF102A43.toInt(), true)
        label("Use these Android settings to review your device protection. Guardian is a helper app; Android does not allow an ordinary app to make itself impossible to uninstall or universally block other app installations.", 15f, 0xFF334E68.toInt())
        fun action(title: String, description: String, click: () -> Unit) {
            val box = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(12), dp(16), dp(12))
                setBackgroundColor(0xFFFFFFFF.toInt())
                elevation = dp(2).toFloat()
            }
            box.addView(TextView(this).apply {
                text = title; textSize = 17f; setTypeface(null, Typeface.BOLD)
                setTextColor(0xFF102A43.toInt())
            })
            box.addView(TextView(this).apply {
                text = description; textSize = 13f; setTextColor(0xFF627D98.toInt())
            })
            box.setOnClickListener { click() }
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.setMargins(0, dp(9), 0, dp(4))
            content.addView(box, lp)
        }
        action("Security & privacy", "Open Android security settings") {
            runCatching { startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS)) }
        }
        action("App installation permissions", "Review which apps can install unknown apps") {
            runCatching { startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName"))) }
                .onFailure { startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS)) }
        }
        action("Guardian app settings", "View permissions and app information") {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }
        label("Important", 17f, 0xFFB45309.toInt(), true)
        label("Stronger device lockdown requires Android Enterprise/device-owner management configured during device provisioning. This APK alone cannot enforce that level of control.", 14f, 0xFF334E68.toInt())
        label("Guardian • Version 1.0", 12f, 0xFF829AB1.toInt())
        scroll.addView(content)
        setContentView(scroll)
    }
}
