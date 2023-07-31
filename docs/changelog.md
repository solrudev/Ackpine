Change Log
==========

Version 0.0.2 (2023-07-31)
--------------------------

### Dependencies

- Updated `kotlinx.coroutines` to 1.7.3.
- Updated AndroidX Room to 2.5.2.

### Bug fixes and improvements

- Show a full screen loading indicator after session is committed if it takes long to launch system confirmation activity.
- Fix race when session is failing with `Session $id is dead` instead of other reasons.
- Create session first and only then persist it in `PackageInstaller` and `PackageUninstaller` implementations to avoid persisting if session factory threw an exception.
- Fix night theme in sample app.

### Public API changes

- Added constructors with default values to `InstallFailure` descendants (via `@JvmOverloads`).

Version 0.0.1 (2023-07-26)
--------------------------

Initial release.