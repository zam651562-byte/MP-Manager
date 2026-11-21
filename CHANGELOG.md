# MP Manager Changelog

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

### Verification
- Focused overlay Smali unit tests pass.
- Full debug Java compilation passes.
- Lite debug APK build succeeds.

### Commit
- `a9d5e644` - Add advanced overlay code editor and backup support
