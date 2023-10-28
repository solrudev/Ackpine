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
        name = fileName
        requireUserAction = false
        notification {
            title = NotificationString.resource(R.string.install_message_title)
            contentText = NotificationString.resource(R.string.install_message, fileName)
            icon = R.drawable.ic_install
        }
    }
    ```

=== "Java"

    ```java
    var session = packageInstaller.createSession(new InstallParameters.Builder(baseApkUri)
            .addApks(apkSplitsUris)
            .setConfirmation(Confirmation.DEFERRED)
            .setInstallerType(InstallerType.SESSION_BASED)
            .setName(fileName)
            .setRequireUserAction(false)
            .setNotificationData(new NotificationData.Builder()
                    .setTitle(NotificationString.resource(R.string.install_message_title))
                    .setContentText(NotificationString.resource(R.string.install_message, fileName))
                    .setIcon(R.drawable.ic_install)
                    .build())
            .build());
    ```

User's confirmation
-------------------

A strategy for handling user's confirmation of installation or uninstallation. Can be `DEFERRED` (used by default) or `IMMEDIATE`.

`DEFERRED` (default) — user will be shown a high-priority notification which will launch confirmation activity.

`IMMEDIATE` — user will be prompted to confirm installation or uninstallation right away. Suitable for launching session directly from the UI when app is in foreground.

Notification
------------

It is possible to provide notification title, text and icon.

!!! Note
    Any configuration for notification will be ignored if `Confirmation` is set to `IMMEDIATE`, because the notification will not be shown.

`NotificationString` is a type used for `NotificationData` text values. It allows to incapsulate an Android string resource (with arguments) which will be resolved only when notification will be shown, a hardcoded string value or a default value from Ackpine library.

`android.R.drawable.ic_dialog_alert` is used as a default icon.

Session name
------------

You can provide an optional session `name` parameter to be used in default notification content text. It may be a name of the app being installed or a file name. It will be ignored if you specify custom notification content text or set `Confirmation` to `IMMEDIATE`.

Installer type
--------------

Ackpine supports two different package installer implementations: Android's `PackageInstaller` and an intent with `ACTION_INSTALL_PACKAGE` action. They're configured with `InstallerType` enum with entries `SESSION_BASED` and `INTENT_BASED` respectively.

`InstallParameters` builder will maintain the following invariants when configuring the installer type:

- When on API level < 21, `INTENT_BASED` is always set regardless of the provided value;
- When on API level >= 21 and `InstallParameters.Builder.apks` contain more than one entry, `SESSION_BASED` is always set regardless of the provided value.

By default, the value of installer type on API level < 21 is `INTENT_BASED`, and on API level >= 21 is `SESSION_BASED`.