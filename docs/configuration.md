Configuration
=============

Sessions can be configured via `InstallParameters` and `UninstallParameters`.

Session parameters
------------------

An instance of session parameters is created with a builder. `ackpine-ktx` artifact contains DSL APIs for configuring sessions.

An example of creating a session with custom parameters:

=== "Kotlin"

    ```kotlin
    val session = packageInstaller.createSession(baseApkUri) {
        apks += apkSplitsUris
        confirmation = Confirmation.DEFERRED
        installerType = InstallerType.SESSION_BASED
        installMode = InstallMode.InheritExisting(
            packageName = "com.example.package",
            dontKillApp = true
        )
        name = fileName
        requireUserAction = true
        requestUpdateOwnership = true
        packageSource = PackageSource.Store
        notification {
            title = InstallMessageTitle
            contentText = InstallMessage(fileName)
            icon = InstallIcon
        }
        preapproval(
            packageName = "com.example.package",
            label = "Sample App",
            locale = ULocale.US
        ) {
            icon = iconUri
            fallbackToOnDemandApproval = true
        }
        constraints(timeout = 1.minutes) {
            timeoutStrategy = TimeoutStrategy.CommitEagerly
            isAppNotForegroundRequired = true
            isAppNotInteractingRequired = true
        }
    }
    
    object InstallMessageTitle : ResolvableString.Resource() {
        private const val serialVersionUID = -1310602635578779088L
        override fun stringId() = R.string.install_message_title
        private fun readResolve(): Any = InstallMessageTitle
    }
    
    class InstallMessage(fileName: String) : ResolvableString.Resource(fileName) {
        override fun stringId() = R.string.install_message
        private companion object {
            private const val serialVersionUID = 4749568844072243110L
        }
    }
    
    object InstallIcon : DrawableId {
        private const val serialVersionUID = 3692803605642002954L
        override fun drawableId() = R.drawable.ic_install
        private fun readResolve(): Any = InstallIcon
    }
    ```

=== "Java"

    ```java
    var installMode = new InstallMode.InheritExisting("com.example.package", true);
    var notificationData = new NotificationData.Builder()
            .setTitle(Resources.INSTALL_MESSAGE_TITLE)
            .setContentText(new Resources.InstallMessage(fileName))
            .setIcon(Resources.INSTALL_ICON)
            .build();
    var preapproval = new InstallPreapproval.Builder("com.example.package", "Sample App", ULocale.US)
            .setIcon(iconUri)
            .setFallbackToOnDemandApproval(true)
            .build();
    var timeout = Duration.ofMinutes(1);
    // Or use raw millis value on older Android versions, e.g. 60000L
    var constraints = new InstallConstraints.Builder(timeout)
            .setTimeoutStrategy(TimeoutStrategy.COMMIT_EAGERLY)
            .setAppNotForegroundRequired(true)
            .setAppNotInteractingRequired(true)
            .build();
    var parameters = new InstallParameters.Builder(baseApkUri)
            .addApks(apkSplitsUris)
            .setConfirmation(Confirmation.DEFERRED)
            .setInstallerType(InstallerType.SESSION_BASED)
            .setInstallMode(installMode)
            .setName(fileName)
            .setRequireUserAction(true)
            .setRequestUpdateOwnership(true)
            .setPackageSource(PackageSource.STORE)
            .setNotificationData(notificationData)
            .setPreapproval(preapproval)
            .setConstraints(constraints)
            .build();
    var session = packageInstaller.createSession(parameters);
    
    public abstract class Resources {
    
        public static final ResolvableString INSTALL_MESSAGE_TITLE = new InstallMessageTitle();
        public static final DrawableId INSTALL_ICON = new InstallIcon();
    
        private static class InstallMessageTitle extends ResolvableString.Resource {
    
            @Serial
            private static final long serialVersionUID = -1310602635578779088L;
    
            @Override
            protected int stringId() {
                return R.string.install_message_title;
            }
    
            @Serial
            private Object readResolve() {
                return Resources.INSTALL_MESSAGE_TITLE;
            }
        }
    
        public static class InstallMessage extends ResolvableString.Resource {
    
            @Serial
            private static final long serialVersionUID = 4749568844072243110L;
    
            public InstallMessage(String fileName) {
                super(fileName);
            }
    
            @Override
            protected int stringId() {
                return R.string.install_message;
            }
        }
    
        private static class InstallIcon implements DrawableId {
    
            @Serial
            private static final long serialVersionUID = 3692803605642002954L;
    
            @Override
            public int drawableId() {
                return R.drawable.ic_install;
            }
    
            @Serial
            private Object readResolve() {
                return Resources.INSTALL_ICON;
            }
        }
    }
    ```

User's confirmation
-------------------

A strategy for handling user's confirmation of installation or uninstallation. Can be `DEFERRED` (used by default) or `IMMEDIATE`.

- `DEFERRED` (default) — user will be shown a high-priority notification which will launch confirmation activity.

- `IMMEDIATE` — user will be prompted to confirm installation or uninstallation right away. Suitable for launching session directly from the UI when app is in foreground.

It's also possible to configure `requireUserAction` option for install sessions. It will have effect only on API level >= 31. If set to `false`, user's confirmation from system won't be triggered if some conditions are met. See the details [here](https://developer.android.com/reference/android/content/pm/PackageInstaller.SessionParams#setRequireUserAction(int)).

`requireUserAction` is a **delicate** API. This option is unstable for use on different Android versions from different vendors. It's recommended to avoid using it on API level < 33 and on devices with modified OS package installer, most notably from Chinese vendors, unless your app is privileged for silent installs.

If `DEFERRED` confirmation is never used in the app, it's possible to remove Ackpine's notification channel from the app's notification settings, which is used for posting confirmation notifications and is set up automatically. For this, disable automatic Ackpine initialization by adding the following lines to the app's `AndroidManifest.xml`:
```xml
<provider
    android:name="androidx.startup.InitializationProvider"
    android:authorities="${applicationId}.androidx-startup"
    android:exported="false"
    tools:node="merge">
    <meta-data
        android:name="ru.solrudev.ackpine.AckpineInitializer"
        tools:node="remove" />
</provider>
```
Then, if the notification channel was already created previously, call `Ackpine.deleteNotificationChannel()` when initializing the app.

Notification
------------

It is possible to provide notification title, text and icon.

!!! Note
    Any configuration for notification will be ignored if `Confirmation` is set to `IMMEDIATE`, because the notification will not be shown.

`ResolvableString` is a type used for `NotificationData` text values. It allows to incapsulate an Android string resource (with arguments) which will be resolved only when notification will be shown, a hardcoded string value or a default value from Ackpine library if nothing was set.

`android.R.drawable.ic_dialog_alert` is used as a default icon.

Session name
------------

Available for install sessions. You can provide an optional session `name` parameter to be used in default notification content text. It may be a name of the app being installed or a file name. It will be ignored if you specify custom notification content text or set `Confirmation` to `IMMEDIATE`.

Installer type
--------------

Available for install sessions. Ackpine supports two different package installer implementations: Android's `PackageInstaller` and an intent with `ACTION_INSTALL_PACKAGE` action. They're configured with `InstallerType` enum with entries `SESSION_BASED` and `INTENT_BASED` respectively.

`InstallParameters` builder will maintain the following invariants when configuring the installer type:

- When on API level < 21, `INTENT_BASED` is always set regardless of the provided value;
- When on API level >= 21 and `InstallParameters.Builder.apks` contains more than one entry, `SESSION_BASED` is always set regardless of the provided value.

By default, the value of installer type on API level < 21 is `INTENT_BASED`, and on API level >= 21 it is `SESSION_BASED`.

Uninstaller type
--------------

Available for uninstall sessions. Ackpine supports two different package uninstaller implementations: Android's `PackageInstaller` and an intent with `ACTION_UNINSTALL_PACKAGE` or `ACTION_DELETE` action. They're configured with `UninstallerType` enum with entries `PACKAGE_INSTALLER_BASED` and `INTENT_BASED` respectively.

By default, the value of uninstaller type on API level < 21 is `INTENT_BASED`, and on API level >= 21 it is `PACKAGE_INSTALLER_BASED`.

Install mode
------------

Available for install sessions. Takes effect only when using `SESSION_BASED` installer.

- `Full` (default) — mode for an install session whose staged APKs should fully replace any existing APKs for the target app.

- `InheritExisting` — mode for an install session that should inherit any existing APKs for the target app, unless they have been explicitly overridden (based on split name) by the session.

    If there are no existing APKs for the target app, this behaves like `Full`.

    Requires package name of the app being installed. If the APKs staged in the session aren't consistent with the set package name, the install will fail.

    Optionally, it's possible to request the system to not kill any of the package's running processes as part of a session in which splits are being added by setting `dontKillApp` to `true`. This option takes effect only on API level >= 34.

Preapproval
-----------

Available for install sessions on API level >= 34. Attempts to request the approval before committing this session. See the details [here](https://developer.android.com/reference/android/content/pm/PackageInstaller.Session#requestUserPreapproval(android.content.pm.PackageInstaller.PreapprovalDetails,%20android.content.IntentSender)).

Preapproval requires package name of the app being installed, label representing it and locale used to get the label to be provided. Optionally, it's possible to also provide the app's icon via `Uri`.

If preapproval is not available on the device, session will fail. If you want to instead fall back to on-demand user approval, set the `fallbackToOnDemandApproval` property to `true` when configuring `InstallPreapproval`.

Constraints
-----------

Available for install sessions on API level >= 34. Constraints specify the conditions to check against for the installed packages. This can be used by app stores to deliver auto updates without disrupting the user experience (referred as gentle update) - for example, an app store might hold off updates when it find out the app to update is interacting with the user. See the details [here](https://developer.android.com/reference/android/content/pm/PackageInstaller.InstallConstraints).

Installer waits for constraints to be satisfied, so to configure them, timeout duration is required to be provided after which installer will act based on set `TimeoutStrategy`.

`TimeoutStrategy` may be one of the following:

- `Fail` (default) - installer reports failure on timeout if constraints are not met.
- `CommitEagerly` - installer commits session immediately after timeout even if constraints are not met.
- `Retry` - installer retries waiting for constraints to be satisfied with the same timeout if constraints were not met after the first attempt. Requires `retries` parameter to be provided when created.

There's a preset for gentle updates which can be used like this:

=== "Kotlin"

    ```kotlin
    val session = packageInstaller.createSession(apkUri) {
        constraints = InstallConstraints.gentleUpdate(
            timeout = 1.minutes,
            timeoutStrategy = TimeoutStrategy.CommitEagerly // optional
        )
    }
    ```

=== "Java"

    ```java
    var constraints = InstallConstraints.gentleUpdate(60000L,
            /* optional */ TimeoutStrategy.COMMIT_EAGERLY);
    var session = packageInstaller.createSession(new InstallParameters.Builder(apkUri)
            .setConstraints(constraints)
            .build());
    ```

Update ownership
----------------

Available for install sessions on API level >= 34.

Optionally indicate whether the package being installed needs the update ownership enforcement. Once the update ownership enforcement is enabled, the other installers will need the user action to update the package even if the installers have been granted the `INSTALL_PACKAGES` permission. Default to `false`. The update ownership enforcement can only be enabled on initial installation. Setting this to `true` on package update is a no-op.

Package source
--------------

Available for install sessions.

Optionally indicates the package source of the app being installed. This is informational and may be used as a signal by the system. Default value is `PackageSource.Unspecified`.

Setting this value to `PackageSource.LocalFile` or `PackageSource.DownloadedFile` will disable restricted settings for the app being installed on API level >= 33.

Plugins
-------

Ackpine supports plugins. They are available for sessions that use Android's `PackageInstaller` API.

At the moment, there are two Ackpine plugins: [ShizukuPlugin and ShizukuUninstallPlugin](shizuku.md).