Change Log
==========

Version 0.9.3 (2024-12-25)
--------------------------

### Dependencies

- Updated KSP to 2.0.21-1.0.28.
- Updated Android Gradle Plugin to 8.7.3.
- Updated `apksig` to 8.7.3.
- Updated `binary-compatibility-validator` to 0.17.0.
- Updated `androidx.navigation` to 2.8.5 (sample apps dependency).
- Updated Guava to 33.4.0-android (sample apps dependency).

### Bug fixes and improvements

- Raise `compileSdk` to 35.
- Use random access when parsing APK on API level 26+ in `Apk.fromUri()`. This greatly improves performance for large APKs.
- Add `ZippedFileProvider.getUriForZipEntry()` overloads for `File` and `Uri`.
- Raise `targetSdk` for sample apps to 35.
- Proper support for edge-to-edge display in sample apps.

### Public API changes

- Added `getUriForZipEntry(File, String)` and `getUriForZipEntry(Uri, String)` to `ZippedFileProvider.Companion` and as static `ZippedFileProvider` methods.

Version 0.9.2 (2024-12-19)
--------------------------

### Bug fixes and improvements

- Fix `SESSION_BASED` installer session completing with "Install permission denied" failure when performing a self-update if install permission was not granted because it was unnecessary.

Version 0.9.1 (2024-12-14)
--------------------------

### Bug fixes and improvements

- Proper support of dynamic features for split APKs (#95).
- Add documentation for `Apk` properties.

### Public API changes

- Added `Apk.ConfigSplit` sealed interface in `ackpine-splits` module.
- Added `configForSplit` property to `Apk.Libs`, `Apk.ScreenDensity` and `Apk.Localization` in `ackpine-splits` module.

Version 0.9.0 (2024-12-07)
--------------------------

### Dependencies

- Updated Dokka to 2.0.0-Beta.
- Updated Gradle wrapper to 8.11.1.

### Bug fixes and improvements

- Introduce `InstallPreapproval` API. See documentation on usage.
- Introduce `InstallConstraints` API. See documentation on usage.
- Add `requestUpdateOwnership` option for install sessions. Permission `ENFORCE_UPDATE_OWNERSHIP` was added to `AndroidManifest.xml`.
- Add `packageSource` option for install sessions.
- Add `dontKillApp` option for install sessions with `InheritExisting` install mode.

- Source-incompatible: deprecate `SessionResult` and return `Session.State.Completed` from `Session.await()`.

    `SessionResult` was an early design leftover which was mistakenly overlooked. Now it's finally been dealt with.

    To migrate, change `SessionResult.Success` to `Session.State.Succeeded`, and `SessionResult.Error` to `Session.State.Failed`. `cause` property of `SessionResult.Error` is replaced with `failure` property of `Session.State.Failed`.

    ```kotlin
    // Old
    when (val result = session.await()) {
        is SessionResult.Success -> println("Success")
        is SessionResult.Error -> println(result.cause.message)
    }

    // New
    when (val result = session.await()) {
        Session.State.Succeeded -> println("Success")
        is Session.State.Failed -> println(result.failure.message)
    }
    ```

- Source-incompatible: `when` matches on `InstallFailure` and `UninstallFailure` type are no longer exhaustive.

    This change was made to guard against possible additions of failure types in future Android versions, like `Timeout` in Android 14.

- Fix session not launching after process restart if it was in the midst of preparations.
- Show notification for `SESSION_BASED` install sessions when `DEFERRED` confirmation is set only if user action is actually required.
- Fix possible races when `ListenableFutures` returned from `getSessionsAsync()` and `getActiveSessionsAsync()` might not get completed.
- Lower API level required for `READ_EXTERNAL_STORAGE` permission in sample apps.
- Add `sample-api34` sample project.
- Add "Building" section to documentation.

### Public API changes

- Source-incompatible: `Session.await()` now returns `Session.State.Completed`. Overload returning `SessionResult` is left for binary compatibility, but will be removed in the next minor version.
- Source-incompatible: `when` matches on `InstallFailure` and `UninstallFailure` type are no longer exhaustive.
- Deprecated: `SessionResult` is deprecated and will be removed in the next minor version.
- Added `InstallPreapproval` class and related APIs to `InstallParameters`, `InstallParameters.Builder` and `InstallParametersDsl`.
- Added `InstallConstraints` class and related APIs to `InstallParameters`, `InstallParameters.Builder` and `InstallParametersDsl`.
- Added `dontKillApp` boolean property to `InstallMode.InheritExisting`.
- Added `requestUpdateOwnership` property to `InstallParameters`, `InstallParameters.Builder` and `InstallParametersDsl`.
- Added `PackageSource` class and related APIs to `InstallParameters`, `InstallParameters.Builder` and `InstallParametersDsl`.

Version 0.8.3 (2024-11-07)
--------------------------

### Bug fixes and improvements

- Return `Aborted` failure when `INTENT_BASED` install session is cancelled.
- Return `Aborted` failure when uninstall via `ACTION_UNINSTALL_PACKAGE` is cancelled.
- Request permissions if they're not granted when sample apps are launched via `ACTION_VIEW` intent.

Version 0.8.2 (2024-11-01)
--------------------------

### Bug fixes and improvements

- Fix introduced in 0.8.1 repeated install confirmation after granting install permission if confirmation was dismissed by clicking outside of confirmation dialog on some OS versions.
- Don't use `requireUserAction` option in samples as it's unstable with different vendors and OS versions.

Version 0.8.1 (2024-10-31)
--------------------------

### Dependencies

- Updated Kotlin to 2.0.21.
- Updated `androidx.annotation` to 1.9.1.
- Updated `androidx.activity` to 1.9.3 (sample apps dependency).
- Updated `androidx.constraintlayout` to 2.2.0 (sample apps dependency).
- Updated `androidx.lifecycle` to 2.8.7 (sample apps dependency).
- Updated `androidx.navigation` to 2.8.3 (sample apps dependency).
- Updated Guava to 33.3.1-android (sample apps dependency).
- Migrated from `gradle-nexus/publish-plugin` to `vanniktech/gradle-maven-publish-plugin` for publishing artifacts to Maven Central.

### Bug fixes and improvements

- Fix install confirmation from OS not displaying after granting install permission on some devices (particularly Android TV) using `SESSION_BASED` installer. This is a fix for #84.
- Fix various issues with dismissing install confirmation from OS via clicking outside of confirmation dialog using `SESSION_BASED` installer.

Version 0.8.0 (2024-10-25)
--------------------------

### Dependencies

- Extracted `ackpine-resources` artifact, which is now depended upon by `ackpine-core`.

### Bug fixes and improvements

- `NotificationString` is superseded by `ResolvableString` to accommodate stable string resources resolution. `ResolvableString` is now located in `ackpine-resources` artifact and can also be used separately for general app needs. `NotificationString` is deprecated and will be removed in next minor release.

    To migrate `NotificationString.resource()` usages to `ResolvableString`, create classes inheriting from `ResolvableString.Resource` like this:
    ```kotlin
    // Old
    NotificationString.resource(R.string.install_message, fileName)
    
    // New
    class InstallMessage(fileName: String) : ResolvableString.Resource(fileName) {
        override fun stringId() = R.string.install_message
        private companion object {
            private const val serialVersionUID = 4749568844072243110L
        }
    }
    
    InstallMessage(fileName)
    ```

    Note that this requires to purge internal database because of incompatible changes, so all previous sessions will be cleared when Ackpine is updated to 0.8.0.

- `NotificationData` now requires an instance of `DrawableId` class instead of integer drawable resource ID for icon to accommodate stable drawable resources resolution.
- Don't hardcode a condition in implementation of `SESSION_BASED` sessions when Android's `PackageInstaller.Session` fails without report. It should possibly improve reliability on different devices.
- Fix progress bars on install screen not using latest value in sample apps.
- Disable cancel button when session's state is Committed in sample apps.

### Public API changes

- Breaking: `NotificationData`, `NotificationData.Builder` and `NotificationDataDsl` now require `ResolvableString` instead of `NotificationString` as `title` and `contentText` type. `NotificationString` is deprecated with an error deprecation level and will be removed in next minor release.
- Breaking: `NotificationData`, `NotificationData.Builder` and `NotificationDataDsl` now require `DrawableId` instead of integer as `icon` type.
- Added `ResolvableString` sealed interface in `ackpine-resources` module.
- Added `DrawableId` interface in `ackpine-core` module.

Version 0.7.6 (2024-10-12)
--------------------------

### Dependencies

- Reverted `androidx.startup` to 1.1.1.

### Bug fixes and improvements

- `INTENT_BASED` package installer sessions are no longer stuck in `COMMITTED` state if they have performed a successful app self-update (not in all scenarios). This is an (almost) fix for issue #33.
- Use `ZipFile` API for reading `AndroidManifest.xml` when possible while parsing single APK with `Apk.fromFile()` and `Apk.fromUri()`.

Version 0.7.5 (2024-10-06)
--------------------------

### Bug fixes and improvements

- Improve thread-safety of `Session`, `PackageInstaller` and `PackageUninstaller` implementations.
- Fix progress changing without animation when session is committed in sample apps.

Version 0.7.4 (2024-10-04)
--------------------------

### Dependencies

- Added direct dependency on `androidx.coordinatorlayout:coordinatorlayout:1.2.0` to sample projects.

### Bug fixes and improvements

- Improve error handling in `ackpine-splits`. This also allowed to avoid duplicate `Apk` objects in sequences returned from `ZippedApkSplits` factories in some possible cases of errors.
- Fix incorrect sessions' progress when there are list items beyond visible area in sample apps.
- Hide floating Install button when scrolling down in sample apps.

Version 0.7.3 (2024-10-01)
--------------------------

### Bug fixes and improvements

- Fix resources not closing when throwing if `throwOnInvalidSplitPackage()` is applied and `ZippedApkSplits.getApksForUri()` delegates to `ZippedApkSplits.getApksForFile()`.

Version 0.7.2 (2024-09-30)
--------------------------

### Dependencies

- Updated Kotlin to 2.0.20.
- Updated Gradle wrapper to 8.9.
- Updated Android Gradle Plugin to 8.6.1.
- Updated Apache Commons Compress to 1.27.1.
- Updated `apksig` to 8.6.1.
- Updated `binary-compatibility-validator` to 0.16.3.
- Updated `foojay-resolver-convention` to 0.8.0.
- Updated `kotlinx.coroutines` to 1.9.0.
- Updated `androidx.startup` to 1.2.0.
- Updated `androidx.annotation` to 1.8.2.
- Updated `androidx.activity` to 1.9.2 (sample apps dependency).
- Updated `androidx.lifecycle` to 2.8.6 (sample apps dependency).
- Updated `androidx.navigation` to 2.8.1 (sample apps dependency).
- Updated Guava to 33.3.0-android (sample apps dependency).

### Bug fixes and improvements

- Make split APKs sequences created with `ZippedApkSplits` factories cooperate with `throwOnInvalidSplitPackage()` operation. They will now close held resources when split package validation fails if not consumed in whole. Now the recommended order of applying `throwOnInvalidSplitPackage()` is immediately after creating a sequence.
- Improve documentation about split APKs.
- Fix some visual bugs with sessions' progress in sample apps.
- Disable cancel button when install session is committed in sample apps.
- Cancel sessions when ViewModel is cleared in Java sample app.

Version 0.7.1 (2024-07-03)
--------------------------

### Bug fixes and improvements

- Fix `ConcurrentModificationException` in `Session` in very rare cases (#70).

Version 0.7.0 (2024-07-02)
--------------------------

### Dependencies

- Updated Android Gradle Plugin to 8.5.0.
- Updated `apksig` to 8.5.0.
- Updated `androidx.concurrent` to 1.2.0.
- Updated `androidx.lifecycle` to 2.8.3 (sample apps dependency).
- Updated Guava to 33.2.1-android (sample apps dependency).

### Bug fixes and improvements

- Fix race conditions leading to `SQLiteConstraintException: FOREIGN KEY constraint failed` (#68).
- Fix sessions getting stuck after launching when there's a lot of them created concurrently.
- Fix `FileNotFoundException` and `ZipException` when reading zipped files in some cases.
- Add support for external storage `Uri`s when trying to directly access files.
- Log APK parsing exceptions in sample apps.
- Open file picker immediately after granting all permissions in sample apps.
- Remove `MANAGE_EXTERNAL_STORAGE` permission from sample apps.

### Public API changes

- `InstallMode.Companion` is made private.

Version 0.6.1 (2024-06-11)
--------------------------

### Dependencies

- Updated Kotlin to 2.0.0.
- Updated Gradle wrapper to 8.8.
- Updated Android Gradle Plugin to 8.4.2.
- Updated Apache Commons Compress to 1.26.2.
- Updated `apksig` to 8.4.2.
- Updated `kotlinx.coroutines` to 1.8.1.
- Updated `androidx.lifecycle` to 2.8.1 (sample apps dependency).
- Updated `androidx.appcompat` to 1.7.0 (sample apps dependency).

### Bug fixes and improvements

- Introduce an API for deleting previously created Ackpine's notification channel.
- Add info about notification channel initialization to documentation.

### Public API changes

- Added `Ackpine.deleteNotificationChannel()` static method in `ackpine-core` module.

Version 0.6.0 (2024-05-20)
--------------------------

### Dependencies

- Updated Kotlin to 1.9.24.
- Updated `androidx.annotation` to 1.8.0.
- Updated `androidx.activity` to 1.9.0 (sample apps dependency).
- Updated Guava to 33.2.0-android (sample apps dependency).
- Updated Material Components to 1.12.0 (sample apps dependency).

### Bug fixes and improvements

- Introduce an install mode option for `InstallParameters`.
- More consistent behavior of `COMMITTED` session state notifications when installer app is privileged for silent installs.

### Public API changes

- Added `InstallMode` sealed interface with two children (`Full` and `InheritExisting`) in `ackpine-core` module.
- Added `installMode` property to `InstallParameters` and its builder in `ackpine-core` module.
- Added `installMode` property to `InstallParametersDsl` in `ackpine-ktx` module.

Version 0.5.5 (2024-04-29)
--------------------------

### Bug fixes and improvements

- Fix some internal state of `SESSION_BASED` package installer session when it's in terminal state and is being initialized on retrieval from `PackageInstaller`.

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