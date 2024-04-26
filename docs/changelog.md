Change Log
==========

Version 0.5.4 (2024-04-26)
--------------------------

### Bug fixes and improvements

- Fix `SESSION_BASED` package installer session not notifying about transitioning into `Committed` state when installation is performed without user's action via setting `requireUserAction` to `false`.
- Fix `SESSION_BASED` package installer session not updating its progress if it's already prepared and app process is restarted.
- Don't allow to commit `SESSION_BASED` package installer session after app process restart when an actual installation process in the system is ongoing.

Version 0.5.3 (2024-04-25)
--------------------------

### Dependencies

- Updated Android Gradle Plugin to 8.3.2.
- Updated `apksig` to 8.3.2.
- Added Apache Commons Compress dependency to `ackpine-splits` module.

### Bug fixes and improvements

- `ackpine-splits`: use `FileChannel` to read zipped APKs on Android Oreo+ if possible. This drastically improves performance when direct access through `java.io` APIs is not available and allows to process problematic ZIP files (such as XAPK files).
- Don't crash if exception occurs while iterating APK sequence in sample apps, and display the exception message instead.

Version 0.5.2 (2024-03-30)
--------------------------

### Dependencies

- Updated Kotlin to 1.9.23.
- Updated Gradle wrapper to 8.7.
- Updated Android Gradle Plugin to 8.3.1.
- Updated `apksig` to 8.3.1.
- Updated Dokka to 1.9.20.
- Updated Guava to 33.1.0-android (sample apps dependency).

### Bug fixes and improvements

- Allow to commit `SESSION_BASED` package installer sessions from background when using `requireUserAction = false` and `Confirmation.IMMEDIATE`.

Version 0.5.1 (2024-02-23)
--------------------------

### Dependencies

- Updated Gradle wrapper to 8.6.
- Updated Android Gradle Plugin to 8.2.2.
- Updated `apksig` to 8.2.2.
- Updated `kotlinx.coroutines` to 1.8.0.
- Updated `binary-compatibility-validator` to 0.14.0 .
- Updated `androidx.navigation` to 2.7.7 (sample apps dependency).
- Updated Guava to 33.0.0-android (sample apps dependency).

### Bug fixes and improvements

- Fix possibility of sessions' notification ID inconsistencies.

Version 0.5.0 (2024-02-02)
--------------------------

### Bug fixes and improvements

- Add support for timeout install failure introduced in API level 34.
- Add new `sortedByCompatibility()` and related APIs in `ackpine-splits` module.
- Fix documentation for `NotificationData` and related APIs.
- Don't create session object if it already exists when initializing all sessions.

### Public API changes

- Source-incompatible, possibly throwing in runtime: added `Timeout` child to sealed `InstallFailure` class in `ackpine-core` module.
- Added `ApkCompatibility` class in `ackpine-splits` module.
- Added extension functions for `Sequence<Apk>`, `Sequence<ApkCompatibility>`, `Iterable<Apk>`, `Iterable<ApkCompatibility>` to `ApkSplits` in `ackpine-splits` module: `sortedByCompatibility()`, `filterCompatible()`, `addAllTo()`.

Version 0.4.4 (2024-01-22)
--------------------------

### Bug fixes and improvements

- Prevent clients from committing a `SESSION_BASED` package installer session while it's initializing if it's already completed.
- Fix an exception when app process is restarted after returning from package uninstall confirmation.

Version 0.4.3 (2024-01-19)
--------------------------

### Dependencies

- Updated Kotlin to 1.9.22.
- Updated Android Gradle Plugin to 8.2.1.
- Updated `apksig` to 8.2.1.
- Updated `androidx.annotation` to 1.7.1.
- Updated `androidx.activity` to 1.8.2 (sample apps dependency).
- Updated `androidx.lifecycle` to 2.7.0 (sample apps dependency).
- Updated `androidx.navigation` to 2.7.6 (sample apps dependency).
- Updated Material Components to 1.11.0 (sample apps dependency).

### Bug fixes and improvements

- Fix incorrect `Succeeded` state of `SESSION_BASED` package installer session in some cases if app is killed while installing but system installer Activity remains visible.
- Scale Ackpine thread pool size depending on available CPU cores.
- Associate APK, APKS, APKM and XAPK files with Ackpine sample app.

Version 0.4.2 (2024-01-15)
--------------------------

### Bug fixes and improvements

- `PackageInstaller` and `PackageUninstaller` no longer throw exceptions when trying to get a session of wrong type (install session from `PackageUninstaller` and vice versa).
- `PackageInstaller` and `PackageUninstaller` no longer throw exceptions when getting a list of sessions if there were both install and uninstall sessions in internal database.
- Add ProGuard rules and `serialVersionUID` for classes which are `Serializable` for more stable serialization. Note that this requires to purge internal database because of incompatible changes, so all previous sessions will be cleared when Ackpine is updated to 0.4.2.

Version 0.4.1 (2024-01-06)
--------------------------

### Bug fixes and improvements

- `SESSION_BASED` package installer sessions are no longer stuck in `COMMITTED` state if they have performed a successful app self-update. This is a partial fix for issue #33.

Version 0.4.0 (2023-12-11)
--------------------------

### Bug fixes and improvements

- Improve documentation for `Session.launch()` and `Session.commit()` and make them return a boolean to indicate whether their invocation took effect.
- Add `isCompleted` and `isCancelled` boolean properties to `Session`.

### Public API changes

- Breaking: `Session.DefaultStateListener` is renamed to `Session.TerminalStateListener`.
- Breaking: `Session.State.isCompleted` is removed.
- Possibly breaking: `isCompleted` and `isCancelled` boolean properties are added to `Session`.
- Possibly breaking: `Session.launch()` and `Session.commit()` now return `Boolean`.

Version 0.3.2 (2023-12-01)
--------------------------

### Dependencies

- Updated Kotlin to 1.9.21.
- Updated Gradle wrapper to 8.5.
- Updated Android Gradle Plugin to 8.2.0.
- Updated `apksig` to 8.2.0.
- Updated `androidx.room` to 2.6.1.
- Updated `androidx.activity` to 1.8.1 (sample apps dependency).
- Updated Guava to 32.1.3-android (sample apps dependency).

### Bug fixes and improvements

- Add Afrikaans translations for South Africa. Thanks to @MJJacobs01!

Version 0.3.1 (2023-11-10)
--------------------------

### Bug fixes and improvements

- Don't throw from install parameters builder constructor which accepts `Iterable`, from `add()` and `addAll()` methods on API levels < 21 if only a single APK ends up in `ApkList`. This fixes a long-lasting issue of throwing when getting a session from `PackageInstaller` after process restart on API levels < 21.

Version 0.3.0 (2023-11-09)
--------------------------

### Dependencies

- Extracted `ackpine-runtime` artifact, which is now depended upon by `ackpine-core` and `ackpine-splits`. `ackpine-splits` now doesn't declare a transitive dependency on `ackpine-core`.
- `ackpine-ktx` now depends on `androidx.concurrent:concurrent-futures-ktx` instead of `androidx.concurrent:concurrent-futures`.
- Updated Kotlin to 1.9.20.
- Updated Android Gradle Plugin to 8.1.3.
- Updated `apksig` to 8.1.3.
- Updated `androidx.navigation` to 2.7.5 (sample apps dependency).

### Bug fixes and improvements

- Return dummy `DisposableSubscription` object when attempting to add an already registered listener to a session instead of a new one each time.
- Don't notify an already registered listener with snapshot of current session's state or progress when attempting to add it again.
- Require a `DisposableSubscriptionContainer` when adding a listener to a session to avoid an error-prone practice of adding the subscription to a subscriptions bag manually. Documentation and Java sample app were updated accordingly.
- Make confirmation Activity appear less jarring on finish.

### Public API changes

- Breaking: `Session.addStateListener()` and `ProgressSession.addProgressListener()` now require a `DisposableSubscriptionContainer` to be provided.
- Extension functions `PackageInstaller.getSession()`, `PackageInstaller.getSessions()`, `PackageInstaller.getActiveSessions()` and their respective counterparts for `PackageUninstaller` are not inline functions anymore.

Version 0.2.2 (2023-11-03)
--------------------------

### Bug fixes and improvements

- Fix duplicate session's state change notifications in some cases (e.g. after process restart and re-attaching a listener when session's been completed right before). This also fixes `IllegalStateException` in `Session.await()` in these cases.
- Make confirmation's background fully transparent.
- Don't display loading indicator during confirmation from system.

Version 0.2.1 (2023-10-30)
--------------------------

### Bug fixes and improvements

- Fix issues with incorrect sessions behavior when multiple session confirmations are active simultaneously and are stacked up on each other.
- Remove unnecessary Activity flags from `AndroidManifest.xml`.

Version 0.2.0 (2023-10-28)
--------------------------

### Dependencies

- Updated AndroidX Room to 2.6.0.
- Updated RecyclerView to 1.3.2 (sample apps dependency).

### Bug fixes and improvements

- Remove intrusive behavior of `DEFERRED` confirmation: dismissing keyguard, using full-screen intent. `USE_FULL_SCREEN_INTENT` and `DISABLE_KEYGUARD` permissions were removed. Also it allowed to make `DEFERRED` confirmation's behavior consistent on old and new Android versions, as on old versions full-screen intent behaved like `IMMEDIATE` confirmation.
- Enable vibration and lights for library's notification channel.
- Make confirmation's background semi-transparent.
- Fix incorrect handling of `file:` URIs in sample apps.
- Add documentation about permissions and library's architecture.

Version 0.1.6 (2023-10-17)
--------------------------

### Dependencies

- Updated Dokka to 1.9.10 Beta.

### Bug fixes and improvements

- Extend from `FileProvider` and declare it in `AndroidManifest.xml` instead to avoid possible issues and providers conflicts.

Version 0.1.5 (2023-10-11)
--------------------------

### Bug fixes and improvements

- Fix an issue with sessions' `commit()` implementation when it allowed to commit while session is already in the process of being committed.

Version 0.1.4 (2023-10-10)
--------------------------

This release bumps `compileSdk` to 34.

### Dependencies

- Updated Gradle wrapper to 8.4.
- Updated Android Gradle Plugin to 8.1.2.
- Updated `apksig` to 8.1.2.
- Updated `androidx.annotation` to 1.7.0.
- Updated `androidx.core` to 1.12.0.
- Updated `androidx.activity` to 1.8.0 (sample apps dependency).
- Updated `androidx.lifecycle` to 2.6.2 (sample apps dependency).
- Updated `androidx.navigation` to 2.7.4 (sample apps dependency).
- Updated Material Components to 1.10.0 (sample apps dependency).

### Bug fixes and improvements

- Make it possible to commit an arbitrary session more than once (if its confirmation was interrupted, e.g. by completely exiting the app).
- Update `compileSdk` and sample apps' `targetSdk` to 34.
- Change primary color of color scheme in sample apps to match the logo.

Version 0.1.3 (2023-09-23)
--------------------------

Added a logo for the library! Sample apps icon now uses the logo too.

### Bug fixes and improvements

- Small optimization in `SESSION_BASED` package installer.

Version 0.1.2 (2023-09-19)
--------------------------

### Bug fixes and improvements

- Fix deadlock in `SESSION_BASED` package installer.

Version 0.1.1 (2023-08-30)
--------------------------

### Bug fixes and improvements

- Add index for `lastLaunchTimestamp` internal database column.

Version 0.1.0 (2023-08-24)
--------------------------

### Dependencies

- Updated Kotlin to 1.9.10.
- Updated Android Gradle Plugin to 8.1.1.
- Updated `apksig` to 8.1.1.

### Bug fixes and improvements

- Don't add `DisposableSubscription` instance to `DisposableSubscriptionContainer` if it's already added.

### Public API changes

- Breaking: `Sequence<Apk>.filterIncompatible()` in `ApkSplits` is renamed to `Sequence<Apk>.filterCompatible()`.

Version 0.0.11 (2023-08-19)
--------------------------

### Dependencies

- Replaced transitive `kotlinx-coroutines-android` dependency with `kotlinx-coroutines-core`.

### Bug fixes and improvements

- Handle sessions' cancellation synchronously.
- Fix falling back to using `ContentResolver` instead of Java File API when file path has `mnt/media_rw` in the middle.

Version 0.0.10 (2023-08-18)
--------------------------

### Dependencies

- Updated Kotlin to 1.9.0.
- Updated Android Gradle Plugin to 8.1.0.
- Updated `apksig` to 8.1.0.
- Updated Gradle wrapper to 8.3.

### Bug fixes and improvements

- Reduce count of threads in shared Ackpine thread pool.

### Public API changes

- Due to updating to Kotlin 1.9, `entries` property was added to `Confirmation`, `InstallerType`, `Abi` and `Dpi` enums.

Version 0.0.9 (2023-08-16)
--------------------------

### Bug fixes and improvements

- Detach state listener when session reaches terminal state in `Session.await()`.
- Don't store strong references in `DisposableSubscription` implementations.
- Don't add listener instance to session if it's already registered.

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