# MP Manager Changelog

## 2026-11-21 - Advanced runtime protection

### Added
- Independent advanced-protection controls inside the dialog injection editor.
- Optional signing-certificate verification pinned to the target APK signer.
- Optional protected-component verification for injected activities.
- Optional PIN/password lock that blocks the original activity `onCreate` until unlock succeeds.
- Optional device credential / fingerprint prompt integration through Android Keyguard.
- Protection settings included in exported/imported overlay JSON configurations.
- Runtime protection handling for both dialog and advanced-dialog injection modes.

### Improved
- Protection runs in the generated DEX/target activity wrapper instead of as a manager-only UI.
- Signature mismatches, missing signature material, and component failures stop protected launch.
- Protection credentials are only treated as active when explicitly enabled and validated.
- Original activity startup remains unchanged when protection checks pass.

### Verification
- Source compilation for Lite Release completed before R8 resource processing.
- Release build was attempted in the constrained workspace; Gradle daemon exhausted available memory during R8 and was not reported as an APK success.
- Final APK publication will use GitHub Actions for a clean build and release verification.

## 2026-11-21 - Advanced overlay and dialog editor

### Added
- Manual JSON code editor inside the original Add Toast / Dialog section.
- Visual-designer and JSON bidirectional synchronization.
- JSON formatting and syntax validation with inline status messages.
- Copy, paste, import, and export actions for overlay configurations.
- Configuration round-trip support for toast and dialog profiles.
- Automatic backup creation before APK injection.
- Programmatic backup restoration support.
- Injection completion reports with backup and output filenames.
- Dedicated validation for toast, dialog, and advanced widget options.

### Improved
- Injection safety through earlier option validation.
- Original APK preservation during injection.
- Overlay/profile workflows remain in the existing MP Manager section.
- The existing overlay editor dialog now uses a scoped beige palette, rounded panels, and a rounded code-editing surface in light and dark modes.

### Verification
- Focused overlay Smali unit tests pass.
- Full debug Java compilation passes.
- Lite debug APK build succeeds.
- Full debug APK build succeeds after the visual update; it has not been installed or runtime-tested on a device.

### Commit
- `a9d5e644` - Add advanced overlay code editor and backup support
