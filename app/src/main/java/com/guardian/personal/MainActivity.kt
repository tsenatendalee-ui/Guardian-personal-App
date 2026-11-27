package com.guardian.personal

import android.app.Activity
import android.app.AlertDialog
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.os.UserManager
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.graphics.Typeface
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import android.util.Base64

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("guardian_admin", Context.MODE_PRIVATE) }
    private val dpm by lazy { getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager }
    private val admin by lazy { ComponentName(this, GuardianAdminReceiver::class.java) }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private lateinit var body: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = 0xFF102A43.toInt()
        window.navigationBarColor = 0xFF102A43.toInt()
        val scroll = android.widget.ScrollView(this)
        body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(28), dp(22), dp(24))
            setBackgroundColor(0xFFF4F7FB.toInt())
        }
        scroll.addView(body)
        setContentView(scroll)
        render()
    }

    private fun label(text: String, size: Float, color: Int, bold: Boolean = false) {
        body.addView(TextView(this).apply {
            this.text = text; textSize = size; setTextColor(color)
            if (bold) setTypeface(null, Typeface.BOLD)
            setPadding(0, dp(8), 0, dp(8))
        })
    }

    private fun pinInput(hint: String): EditText = EditText(this).apply {
        this.hint = hint
        inputType =  android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        filters = arrayOf(android.text.InputFilter.LengthFilter(20))
        body.addView(this, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    private fun button(title: String, action: () -> Unit) {
        body.addView(Button(this).apply { text = title; setOnClickListener { action() } },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    private fun render() {
        body.removeAllViews()
        label("GUARDIAN", 30f, 0xFF102A43.toInt(), true)
        label("Personal device protection", 16f, 0xFF486581.toInt())
        val owner = dpm.isDeviceOwnerApp(packageName)
        label(if (owner) "Device Owner: ACTIVE" else "Device Owner: NOT ENROLLED", 18f,
            if (owner) 0xFF16803C.toInt() else 0xFFB45309.toInt(), true)
        if (!owner) {
            label("This build includes the Device Policy Controller needed for managed-device enrollment, but Android has not granted it Device Owner status. Installation restrictions cannot be enforced until the phone is enrolled during initial setup.", 14f, 0xFF334E68.toInt())
            label("Do not factory-reset until you have a verified backup and a tested enrollment QR code.", 14f, 0xFFB45309.toInt(), true)
        } else {
            val active = prefs.getBoolean("protection_active", false)
            label(if (active) "Installation protection: ON" else "Installation protection: OFF", 18f,
                if (active) 0xFF16803C.toInt() else 0xFFB45309.toInt(), true)
            label("When enabled, Android blocks app installation and uninstallation for this device. This can also affect legitimate app updates and should only be enabled on a device you administer.", 14f, 0xFF334E68.toInt())
        }

        if (!prefs.contains("pin_hash")) {
            label("Create administrator PIN", 18f, 0xFF102A43.toInt(), true)
            val p1 = pinInput("Create PIN (6–12 digits)")
            val p2 = pinInput("Confirm PIN")
            button("Save administrator PIN") {
                val a = p1.text.toString(); val b = p2.text.toString()
                if (a.length !in 6..12 || a.any { !it.isDigit() } || a != b) {
                    Toast.makeText(this, "Enter matching PINs of 6–12 digits.", Toast.LENGTH_LONG).show()
                } else {
                    val recoveryCode = savePin(a)
                    AlertDialog.Builder(this)
                        .setTitle("Save your recovery code")
                        .setMessage("Write this 16-digit recovery code down and keep it somewhere secure and separate from your phone. It is shown only once. If you lose your administrator PIN, this code can turn protection off and let you create a new PIN.\n\n$recoveryCode")
                        .setPositiveButton("I saved it") { _, _ ->
                            Toast.makeText(this, "Administrator PIN saved.", Toast.LENGTH_SHORT).show()
                            render()
                        }
                        .setCancelable(false)
                        .show()
                }
            }
        } else if (owner) {
            val pin = pinInput("Administrator PIN")
            button(if (prefs.getBoolean("protection_active", false)) "Turn protection OFF" else "Turn protection ON") {
                val entered = pin.text.toString()
                if (verifyPin(entered)) {
                    if (setProtection(!prefs.getBoolean("protection_active", false))) render()
                } else if (verifyRecoveryCode(entered)) {
                    if (setProtection(false)) {
                        prefs.edit()
                            .remove("pin_salt").remove("pin_hash")
                            .remove("recovery_salt").remove("recovery_hash")
                            .apply()
                        Toast.makeText(this, "Recovery accepted. Create a new administrator PIN.", Toast.LENGTH_LONG).show()
                        render()
                    }
                } else {
                    Toast.makeText(this, "Incorrect PIN or recovery code.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            label("Administrator PIN is configured. Enrol this app as Device Owner before enabling device-wide policies.", 14f, 0xFF334E68.toInt())
        }
        label("Guardian • DPC build 2.0", 12f, 0xFF829AB1.toInt())
    }

    private fun setProtection(enabled: Boolean): Boolean {
        if (!dpm.isDeviceOwnerApp(packageName)) {
            Toast.makeText(this, "Guardian is not Device Owner.", Toast.LENGTH_LONG).show()
            return false
        }
        try {
            if (enabled) {
                dpm.addUserRestriction(admin, UserManager.DISALLOW_INSTALL_APPS)
                dpm.addUserRestriction(admin, UserManager.DISALLOW_UNINSTALL_APPS)
                dpm.setUninstallBlocked(admin, packageName, true)
            } else {
                dpm.setUninstallBlocked(admin, packageName, false)
                dpm.clearUserRestriction(admin, UserManager.DISALLOW_INSTALL_APPS)
                dpm.clearUserRestriction(admin, UserManager.DISALLOW_UNINSTALL_APPS)
            }
            prefs.edit().putBoolean("protection_active", enabled).apply()
            Toast.makeText(this, if (enabled) "Installation restrictions enabled." else "Installation restrictions disabled.", Toast.LENGTH_LONG).show()
            return true
        } catch (e: Exception) {
            Toast.makeText(this, "Could not apply policy: ${e.message}", Toast.LENGTH_LONG).show()
            return false
        }
    }

    private fun savePin(pin: String): String {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val recoveryCode = buildString { repeat(16) { append(SecureRandom().nextInt(10)) } }
        val recoverySalt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        prefs.edit()
            .putString("pin_salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString("pin_hash", hash(pin, salt))
            .putString("recovery_salt", Base64.encodeToString(recoverySalt, Base64.NO_WRAP))
            .putString("recovery_hash", hash(recoveryCode, recoverySalt))
            .apply()
        return recoveryCode
    }

    private fun verifyRecoveryCode(code: String): Boolean {
        if (code.length != 16 || code.any { !it.isDigit() }) return false
        val saltText = prefs.getString("recovery_salt", null) ?: return false
        val expected = prefs.getString("recovery_hash", null) ?: return false
        val salt = Base64.decode(saltText, Base64.NO_WRAP)
        return MessageDigest.isEqual(hash(code, salt).toByteArray(), expected.toByteArray())
    }
    private fun verifyPin(pin: String): Boolean {
        val saltText = prefs.getString("pin_salt", null) ?: return false
        val expected = prefs.getString("pin_hash", null) ?: return false
        val salt = Base64.decode(saltText, Base64.NO_WRAP)
        return MessageDigest.isEqual(hash(pin, salt).toByteArray(), expected.toByteArray())
    }
    private fun hash(pin: String, salt: ByteArray): String {
        val spec = PBEKeySpec(pin.toCharArray(), salt, 120000, 256)
        return Base64.encodeToString(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded, Base64.NO_WRAP)
    }
}
