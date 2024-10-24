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
        installMode = InstallMode.InheritExisting("com.example.package")
        name = fileName
        requireUserAction = false
        notification {
            title = InstallMessageTitle
            contentText = InstallMessage(fileName)
            icon = R.drawable.ic_install
        }
    }
    
    object InstallMessageTitle : ResolvableString.Resource(R.string.install_message_title) {
        private const val serialVersionUID = -1310602635578779088L
    }
    
    class InstallMessage(fileName: String) : ResolvableString.Resource(R.string.install_message, fileName) {
        private companion object {
            private const val serialVersionUID = 4749568844072243110L
        }
    }
    ```

=== "Java"

    ```java
    var session = packageInstaller.createSession(new InstallParameters.Builder(baseApkUri)
            .addApks(apkSplitsUris)
            .setConfirmation(Confirmation.DEFERRED)
            .setInstallerType(InstallerType.SESSION_BASED)
            .setInstallMode(new InstallMode.InheritExisting("com.example.package"))
            .setName(fileName)
            .setRequireUserAction(false)
            .setNotificationData(new NotificationData.Builder()
                    .setTitle(Resources.INSTALL_MESSAGE_TITLE)
                    .setContentText(new Resources.InstallMessage(fileName))
                    .setIcon(R.drawable.ic_install)
                    .build())
            .build());
    
    public class Resources {
    
        public static final ResolvableString INSTALL_MESSAGE_TITLE = new InstallMessageTitle();
    
        private static class InstallMessageTitle extends ResolvableString.Resource {
        
            @Serial
            private static final long serialVersionUID = -1310602635578779088L;
        
            public InstallMessageTitle() {
                super(R.string.install_message_title);
            }
        }
    
        public static class InstallMessage extends ResolvableString.Resource {
        
            @Serial
            private static final long serialVersionUID = 4749568844072243110L;
        
            public InstallMessage(String fileName) {
                super(R.string.install_message, fileName);
            }
        }
        
        private Resources() {
        }
    }
    ```

User's confirmation
-------------------

A strategy for handling user's confirmation of installation or uninstallation. Can be `DEFERRED` (used by default) or `IMMEDIATE`.

- `DEFERRED` (default) — user will be shown a high-priority notification which will launch confirmation activity.

- `IMMEDIATE` — user will be prompted to confirm installation or uninstallation right away. Suitable for launching session directly from the UI when app is in foreground.

It's also possible to configure `requireUserAction` option for install sessions. It will have effect only on API level >= 31. If set to `false`, user's confirmation from system won't be triggered if some conditions are met. See the details [here](https://developer.android.com/reference/android/content/pm/PackageInstaller.SessionParams#setRequireUserAction(int)).

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

By default, the value of installer type on API level < 21 is `INTENT_BASED`, and on API level >= 21 is `SESSION_BASED`.

Install mode
------------

Available for install sessions. Takes effect only when using `SESSION_BASED` installer.

- `Full` (default) — mode for an install session whose staged APKs should fully replace any existing APKs for the target app.

- `InheritExisting` — mode for an install session that should inherit any existing APKs for the target app, unless they have been explicitly overridden (based on split name) by the session.

    If there are no existing APKs for the target app, this behaves like `Full`.

    Requires package name of the app being installed. If the APKs staged in the session aren't consistent with the set package name, the install will fail.