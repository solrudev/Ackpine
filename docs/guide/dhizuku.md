---
icon: lucide/shield-check
---

Dhizuku
=======

`ackpine-dhizuku` artifact provides a plugin for Ackpine which, when applied, uses Dhizuku to obtain package installer service instead of plain `context.getPackageManager().getPackageInstaller()`, so that Ackpine can manage sessions on behalf of the active device owner even if your app doesn't have such privileges. This can bypass the requirement for user confirmation for uninstalls and fresh installs, request version downgrades, and use selected uninstall flags.

Dhizuku calls run as the device owner application's UID. They do not run as root, ADB shell, or the Android system. Ackpine therefore exposes only the package installer options applicable to that identity.

Kotlin examples below use APIs from `ackpine-dhizuku-ktx` artifact. For general plugin usage in session parameters, see [Configuration](configuration.md#plugins).

!!! warning "Attention"
    Dhizuku plugin for Ackpine doesn't activate Dhizuku or manage Dhizuku permission. You must handle these in your app to successfully use Dhizuku. See the [Dhizuku](https://github.com/iamr0s/Dhizuku) and [Dhizuku API documentation](https://github.com/iamr0s/Dhizuku-API).

Setting up Dhizuku
------------------

Latest Dhizuku API version: ![Dhizuku API version](https://img.shields.io/maven-central/v/io.github.iamr0s/Dhizuku-API)

First of all, if you don't already have Dhizuku support in your app, you need to explicitly add a Dhizuku-API dependency to your build:

=== "Gradle version catalog"

    ```toml
    [versions]
    dhizuku = "see above"

    [libraries]
    dhizuku-api = { module = "io.github.iamr0s:Dhizuku-API", version.ref = "dhizuku" }
    ```

=== "build.gradle.kts"

    ```kotlin
    dependencies {
        val dhizukuVersion = "see above"
        implementation("io.github.iamr0s:Dhizuku-API:$dhizukuVersion")
    }
    ```

Then add an `ackpine-dhizuku` dependency:

=== "Gradle version catalog"

    ```toml
    [libraries]
    ackpine-dhizuku = { module = "ru.solrudev.ackpine:ackpine-dhizuku", version.ref = "ackpine" }

    # Kotlin extensions
    ackpine-dhizuku-ktx = { module = "ru.solrudev.ackpine:ackpine-dhizuku-ktx", version.ref = "ackpine" }
    ```

=== "build.gradle.kts"

    ```kotlin
    dependencies {
        implementation("ru.solrudev.ackpine:ackpine-dhizuku:$ackpineVersion")

        // Kotlin extensions
        implementation("ru.solrudev.ackpine:ackpine-dhizuku-ktx:$ackpineVersion")
    }
    ```


Initialize Dhizuku and obtain permission before launching an Ackpine session that uses the plugin:

```kotlin
if (!Dhizuku.init(context)) {
    return
}
if (!Dhizuku.isPermissionGranted()) {
    Dhizuku.requestPermission(permissionListener)
    return
}
```

The plugin supports Android 8.0 (API 26) and newer.

Using the plugin
----------------

!!! warning "Google Play"
    This plugin uses hidden Android APIs. This may cause your app to fail app review on Google Play. Disable reporting information about SDK dependencies in `build.gradle.kts`:
    ```
    android {
        dependenciesInfo {
            includeInApk = false
            includeInBundle = false
        }
    }
    ```

To apply the plugin to an install session, just add this to your install parameters configuration:

=== "Kotlin"

    ```kotlin
    val session = packageInstaller.createSession(uri) {
        // ...some session configuration...
        dhizuku()

        // Or, if you want to configure some parameters for the plugin
        dhizuku {
            requestDowngrade = true
        }
    }
    ```

=== "Java"

    ```java
    var parameters = new InstallParameters.Builder(uri)
            // ...some session configuration...
            .registerPlugin(DhizukuPlugin.class, DhizukuPlugin.InstallParameters.DEFAULT)
            .build();

    // Or, if you want to configure some parameters for the plugin
    var dhizukuParameters = new DhizukuPlugin.InstallParameters.Builder()
            .setRequestDowngrade(true)
            .build();
    var parameters = new InstallParameters.Builder(uri)
            .registerPlugin(DhizukuPlugin.class, dhizukuParameters)
            .build();
    ```

Also, you can use Dhizuku for uninstall sessions:

=== "Kotlin"

    ```kotlin
    val session = packageUninstaller.createSession(packageName) {
        // ...some session configuration...
        dhizuku()

        // Or, if you want to configure some parameters for the plugin
        dhizuku {
            keepData = true
            allUsers = true
            systemApp = true
        }
    }
    ```

=== "Java"

    ```java
    var parameters = new UninstallParameters.Builder(packageName)
            // ...some session configuration...
            .registerPlugin(DhizukuPlugin.class, DhizukuPlugin.UninstallParameters.DEFAULT)
            .build();

    // Or, if you want to configure some parameters for the plugin
    var dhizukuParameters = new DhizukuPlugin.UninstallParameters.Builder()
            .setKeepData(true)
            .setAllUsers(true)
            .setSystemApp(true)
            .build();
    var parameters = new UninstallParameters.Builder(packageName)
            .registerPlugin(DhizukuPlugin.class, dhizukuParameters)
            .build();
    ```

If Dhizuku is not active, or if Dhizuku permission is not granted for your app, session will fail.

Hidden APIs
-----------

This plugin initializes hidden Android API exemptions through AndroidX Startup using [LSPosed's HiddenApiBypass](https://github.com/LSPosed/AndroidHiddenApiBypass). In case you have your own additional exemptions:

1. Disable the initializer in `AndroidManifest.xml`:

    ```xml
    <provider
        android:name="androidx.startup.InitializationProvider"
        android:authorities="${applicationId}.androidx-startup"
        android:exported="false"
        tools:node="merge">
        <meta-data
            android:name="ru.solrudev.ackpine.privileged.HiddenApiExemptionsInitializer"
            tools:node="remove" />
    </provider>
    ```

2. Add this list to your `HiddenApiBypass.setHiddenApiExemptions` call:

    ```kotlin
    HiddenApiBypass.setHiddenApiExemptions(
        "Landroid/content/pm/IPackageManager",
        "Landroid/content/pm/IPackageInstaller",
        "Landroid/content/pm/IPackageInstallerSession",
        "Landroid/content/pm/PackageInstaller",
        "Landroid/os/UserHandle",
        "Landroid/os/ServiceManager"
    )
    ```

Plugin parameters
-----------------

By default, all flags are disabled.

Dhizuku operations only target the current Android user (unlike Shizuku and libsu plugins), because device owner identity doesn't give access to cross-user operations on its own without `android.permission.INTERACT_ACROSS_USERS_FULL` permission, which has `signature|installer|role` protection levels.

### Install flags

#### `requestDowngrade`

Indicates that an upgrade to a lower version of a package than currently installed has been requested.

Ackpine sets downgrade-request flag which is honored for debuggable installed apps or debuggable OS builds, but does not add the unrestricted downgrade permission flag used by root, shell, or system identities. The device owner must still be authorized by Android to perform the downgrade, so setting this option does not guarantee that a downgrade will succeed.

This is a **delicate** API.

### Uninstall flags

#### `keepData`

Flag parameter to indicate that you don't want to delete the package's data directory.

#### `allUsers`

Flag parameter to indicate that you want the package deleted for all users.

#### `systemApp`

Flag parameter to indicate that a system app should be marked as uninstalled for the current user.

This does not remove the app from the system partition. For an updated system app, it prevents the update from being rolled back globally when uninstalling it for the current user.

Capabilities
------------

`DhizukuPlugin` implements [`InstallCapabilityProvider`](../api/ackpine-api/api-main/ru.solrudev.ackpine.capabilities/-install-capability-provider/index.html) and [`UninstallCapabilityProvider`](../api/ackpine-api/api-main/ru.solrudev.ackpine.capabilities/-uninstall-capability-provider/index.html), so you can query whether individual Dhizuku install/uninstall parameters are supported for a given configuration via `getCapabilities()`. See [Querying capabilities](configuration.md#querying-capabilities) for an overview of the capabilities API.

Each field in [`DhizukuInstallCapabilities`](../api/ackpine-plugins/dhizuku/ru.solrudev.ackpine.dhizuku/-dhizuku-install-capabilities/index.html) mirrors the corresponding `DhizukuPlugin.InstallParameters` flag and reports whether it is supported for the resolved configuration. Similarly, [`DhizukuUninstallCapabilities`](../api/ackpine-plugins/dhizuku/ru.solrudev.ackpine.dhizuku/-dhizuku-uninstall-capabilities/index.html) mirrors `DhizukuPlugin.UninstallParameters`.

!!! Note
    Dhizuku capability support is determined from the effective installer/uninstaller type. Whether Dhizuku is active, whether permission is granted to the app at runtime, and whether Android authorizes a specific downgrade are not taken into account.

=== "Kotlin"

    ```kotlin
    val capabilities = PackageInstaller.getCapabilities(InstallerType.SESSION_BASED, DhizukuPlugin::class)
    val dhizukuCaps = capabilities.plugin(DhizukuPlugin::class) ?: return
    if (dhizukuCaps.requestDowngrade.isAvailable) {
        // The plugin can request a downgrade for this configuration
    }
    ```

=== "Java"

    ```java
    var capabilities = PackageInstaller.getCapabilities(InstallerType.SESSION_BASED, DhizukuPlugin.class);
    var dhizukuCaps = capabilities.plugin(DhizukuPlugin.class);
    if (dhizukuCaps != null && dhizukuCaps.getRequestDowngrade().isAvailable()) {
        // The plugin can request a downgrade for this configuration
    }
    ```

For uninstall:

=== "Kotlin"

    ```kotlin
    val capabilities = PackageUninstaller.getCapabilities(UninstallerType.PACKAGE_INSTALLER_BASED, DhizukuPlugin::class)
    val dhizukuCaps = capabilities.plugin(DhizukuPlugin::class) ?: return
    if (dhizukuCaps.keepData.isSupported) {
        // keepData is effective on this device
    }
    ```

=== "Java"

    ```java
    var capabilities = PackageUninstaller.getCapabilities(UninstallerType.PACKAGE_INSTALLER_BASED, DhizukuPlugin.class);
    var dhizukuCaps = capabilities.plugin(DhizukuPlugin.class);
    if (dhizukuCaps != null && dhizukuCaps.getKeepData().isSupported()) {
        // keepData is effective on this device
    }
    ```