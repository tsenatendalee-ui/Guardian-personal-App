# Guardian Personal App

Android Device Policy Controller (DPC) prototype for a personally owned device.

## Current status

- Debug APK builds through GitHub Actions.
- Device Owner provisioning entry points are present, but QR enrollment has not been tested on a physical Samsung device.
- Installation/uninstallation restrictions remain OFF until the user explicitly enables them in Guardian.
- This is a prototype, not a hardened commercial device-management product.

## Signed release setup (maintainer only)

Never send a keystore, signing password, or recovery credential in a chat or issue.

1. Create and back up a private Android release keystore locally. Keep at least two secure offline backups.
2. In GitHub repository Settings → Secrets and variables → Actions, add these repository secrets:
   - `GUARDIAN_KEYSTORE_BASE64`: base64 encoding of the keystore file.
   - `GUARDIAN_KEYSTORE_PASSWORD`
   - `GUARDIAN_KEY_ALIAS`
   - `GUARDIAN_KEY_PASSWORD`
3. Push a version tag such as `v1.0.0`. The signed-release workflow will build the APK, publish a GitHub Release, and attach:
   - `Guardian.apk`
   - `Guardian.apk.sha256`
   - `guardian-provisioning.json`
   - `guardian-enrollment-qr.png`

The signing key must be retained permanently to publish compatible app updates. Losing it may prevent updates over the installed app.

## Device Owner and recovery safety

**Do not factory-reset or scan an enrollment QR until all of the following are true:**

- Important phone data has a verified backup and account sign-in/recovery details are available.
- The signed APK has been installed and tested on a spare/test device, including enrollment and recovery.
- A documented recovery procedure has been tested by someone with physical access to the device.
- The owner understands that Device Owner is device-wide management, not an ordinary app lock.

Device Owner provisioning is normally performed during initial setup on a new or factory-reset device. QR provisioning requires a public APK download URL and a matching SHA-256 checksum. A checksum mismatch can cause provisioning to fail and may require resetting the device.

The current PIN is stored as a salted PBKDF2 hash in app preferences. This prototype does not yet provide a separately tested recovery credential or administrator recovery channel. If the PIN is lost while restrictions are enabled, the user may be unable to turn them off in the app. Therefore, **do not enable the global installation/uninstallation restrictions on a primary phone yet**.

## Policies used

When explicitly enabled by the Device Owner, Guardian applies:
- `DISALLOW_INSTALL_APPS`
- `DISALLOW_UNINSTALL_APPS`
- Blocks uninstalling Guardian itself.

These restrictions can affect legitimate app installs, app updates, troubleshooting, and device use. The app does not claim to prevent every possible bypass or recovery path.
