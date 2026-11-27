package com.guardian.personal

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.os.Bundle

/**
 * Android 12+ admin-integrated provisioning entry point.
 * Only requests fully-managed mode when Android explicitly permits it.
 */
class GetProvisioningModeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val allowedModes = intent.getIntArrayExtra(
            DevicePolicyManager.EXTRA_PROVISIONING_ALLOWED_PROVISIONING_MODES
        )

        if (allowedModes == null ||
            !allowedModes.contains(DevicePolicyManager.PROVISIONING_MODE_FULLY_MANAGED_DEVICE)
        ) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        val result = Intent().putExtra(
            DevicePolicyManager.EXTRA_PROVISIONING_MODE,
            DevicePolicyManager.PROVISIONING_MODE_FULLY_MANAGED_DEVICE
        )
        setResult(RESULT_OK, result)
        finish()
    }
}
