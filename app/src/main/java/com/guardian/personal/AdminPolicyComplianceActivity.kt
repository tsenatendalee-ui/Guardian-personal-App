package com.guardian.personal

import android.app.Activity
import android.os.Bundle

/**
 * Called by Android during the final stage of managed-device provisioning.
 * Policies remain OFF until the owner opens Guardian and explicitly enables them.
 */
class AdminPolicyComplianceActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_OK)
        finish()
    }
}
