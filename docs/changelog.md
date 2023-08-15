Change Log
==========

Version 0.0.8 (2023-08-15)
--------------------------

### Bug fixes and improvements

- Fix `No such file or directory` error when using `INTENT_BASED` installer on API < 24 and APK is not readable directly.
- Improve progress reports for `INTENT_BASED` installer.
- Add documentation for `AssetFileProvider`.

Version 0.0.7 (2023-08-14)
--------------------------

### Bug fixes and improvements

- Don't expose `file:` URIs on API level >= 24 when using intent-based installer. Now `ackpine-core` artifact declares `FileProvider` in its manifest to support this.

Version 0.0.6 (2023-08-12)
--------------------------

### Bug fixes and improvements

- Fix incorrect handling of `file:` URIs in `ZippedFileProvider`.
- Fix NPE when iterating APKs sequence in Java sample.

Version 0.0.5 (2023-08-08)
--------------------------

### Bug fixes and improvements

- Allow to configure `requireUserAction` option when creating `InstallParameters`.
- Revert to not showing a full screen loading indicator after session is committed to avoid unnecessary UI interruption when user's action is not required.
- Fix session not completing with failure when navigating back from `SessionCommitActivity` on API >= 33 if predictive back gesture is not enabled.

Version 0.0.4 (2023-08-04)
--------------------------

### Bug fixes and improvements

- Finish activity with loading indicator after install session was approved by user (using `SESSION_BASED` installer).
- Purge sessions in terminal state from internal database one day after last launch.

Version 0.0.3 (2023-08-03)
--------------------------

### Bug fixes and improvements

- Fix `IllegalStateException` in `Session.await()` when session has failed with an exception.

### Public API changes

- Now all Ackpine artifacts have different Android namespaces. Generated `R` classes coming from the library need to be re-imported if they were used in client code.

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