---
icon: lucide/toy-brick
---

Shizuku
=======

`ackpine-shizuku` artifact provides plugins for Ackpine which, when applied, use Shizuku to obtain package installer service instead of plain `context.getPackageManager().getPackageInstaller()`, so that Ackpine can manage sessions on behalf of root user or ADB shell even if your app doesn't have such privileges. This enables possibility of using such flags as bypassing low target SDK of the installed app on Android 14+, requesting version downgrade, keeping app data when uninstalling and others. Also it can bypass requirement for user's confirmation for uninstalls and fresh installs.

Kotlin examples below use APIs from `ackpine-shizuku-ktx` artifact. For general plugin usage in session parameters, see [Configuration](configuration.md#plugins).

Setting up Shizuku
------------------

You can see a [full guide](https://github.com/RikkaApps/Shizuku-API#guide) in the Shizuku-API repository.

Latest Shizuku-API version: ![Shizuku-API version](https://img.shields.io/maven-central/v/dev.rikka.shizuku/api)

First of all, if you don't already have Shizuku support in your app, you need to explicitly add Shizuku-API dependencies to your build:

=== "Gradle version catalog"

    ```toml
    [versions]
    shizuku = "see above"
    
    [libraries]
    shizuku-api = { module = "dev.rikka.shizuku:api", version.ref = "shizuku" }
    
    # Add this if you want to support Shizuku
    shizuku-provider = { module = "dev.rikka.shizuku:provider", version.ref = "shizuku" }
    ```

=== "build.gradle.kts"

    ```kotlin
    dependencies {
        val shizukuVersion = "see above"
        implementation("dev.rikka.shizuku:api:$shizukuVersion")
    
        // Add this if you want to support Shizuku
        implementation("dev.rikka.shizuku:provider:$shizukuVersion")
    }
    ```

If you want to support Shizuku in addition to Sui, add `ShizukuProvider` (which comes from `dev.rikka.shizuku:provider` artifact) to your `AndroidManifest.xml`:

```xml
<provider
    android:name="rikka.shizuku.ShizukuProvider"
    android:authorities="${applicationId}.shizuku"
    android:multiprocess="false"
    android:enabled="true"
    android:exported="true"
    android:permission="android.permission.INTERACT_ACROSS_USERS_FULL" />
```

Then add an `ackpine-shizuku` dependency:

=== "Gradle version catalog"

    ```toml
    [libraries]
    ackpine-shizuku = { module = "ru.solrudev.ackpine:ackpine-shizuku", version.ref = "ackpine" }
    
    # Kotlin extensions
    ackpine-shizuku-ktx = { module = "ru.solrudev.ackpine:ackpine-shizuku-ktx", version.ref = "ackpine" }
    ```

=== "build.gradle.kts"

    ```kotlin
    dependencies {
        implementation("ru.solrudev.ackpine:ackpine-shizuku:$ackpineVersion")
    
        // Kotlin extensions
        implementation("ru.solrudev.ackpine:ackpine-shizuku-ktx:$ackpineVersion")
    }
    ```

!!! warning "Attention"
    Shizuku plugin for Ackpine doesn't manage Shizuku permission and binder lifecycle. You must handle these in your app to successfully use Shizuku. See the [Shizuku-API guide](https://github.com/RikkaApps/Shizuku-API#guide) and their [demo project](https://github.com/RikkaApps/Shizuku-API/tree/master/demo).

Using the plugin
----------------

To apply the plugin to an install session, just add this to your install parameters configuration:

=== "Kotlin"

    ```kotlin
    val session = packageInstaller.createSession(uri) {
        // ...some session configuration...
        shizuku()
    
        // Or, if you want to configure some parameters for the plugin
        shizuku {
            bypassLowTargetSdkBlock = true
            allowTest = true
            replaceExisting = true
            requestDowngrade = true
            grantAllRequestedPermissions = true
            allUsers = true
            installerPackageName = "com.android.vending"
        }
    }
    ```

=== "Java"

    ```java
    var parameters = new InstallParameters.Builder(uri)
            // ...some session configuration...
            .registerPlugin(ShizukuPlugin.class, ShizukuPlugin.InstallParameters.DEFAULT)
            .build();
    
    // Or, if you want to configure some parameters for the plugin
    var shizukuParameters = new ShizukuPlugin.InstallParameters.Builder()
            .setBypassLowTargetSdkBlock(true)
            .setAllowTest(true)
            .setReplaceExisting(true)
            .setRequestDowngrade(true)
            .setGrantAllRequestedPermissions(true)
            .setAllUsers(true)
            .setInstallerPackageName("com.android.vending")
            .build();
    var parameters = new InstallParameters.Builder(uri)
            .registerPlugin(ShizukuPlugin.class, shizukuParameters)
            .build();
    ```

Also, you can use Shizuku for uninstall sessions:

=== "Kotlin"

    ```kotlin
    val session = packageUninstaller.createSession(packageName) {
        // ...some session configuration...
        shizuku()
    
        // Or, if you want to configure some parameters for the plugin
        shizuku {
            keepData = true
            allUsers = true
        }
    }
    ```

=== "Java"

    ```java
    var parameters = new UninstallParameters.Builder(packageName)
            // ...some session configuration...
            .registerPlugin(ShizukuPlugin.class, ShizukuPlugin.UninstallParameters.DEFAULT)
            .build();
    
    // Or, if you want to configure some parameters for the plugin
    var shizukuParameters = new ShizukuPlugin.UninstallParameters.Builder()
            .setKeepData(true)
            .setAllUsers(true)
            .build();
    var parameters = new UninstallParameters.Builder(packageName)
            .registerPlugin(ShizukuPlugin.class, shizukuParameters)
            .build();
    ```

!!! Note
    Shizuku versions below 11 are not supported, and with these versions operations will fall back to normal system's `PackageInstaller`, or `INTENT_BASED` installer/uninstaller (if was set).

If Shizuku service is not running, or if Shizuku permission is not granted for your app, session will fail.

Plugin parameters
-----------------

By default, all flags are disabled.

### Install flags

#### `bypassLowTargetSdkBlock`

Flag to bypass the low target SDK version block for this install.

#### `allowTest`

Flag to indicate that you want to allow test packages (those that have set android:testOnly in their manifest) to be installed.

#### `replaceExisting`

Flag to indicate that you want to replace an already installed package, if one exists.

#### `requestDowngrade`

Flag to indicate that an upgrade to a lower version of a package than currently installed has been requested.

#### `grantAllRequestedPermissions`

Flag parameter for package install to indicate that all requested permissions should be granted to the package. If `allUsers` is set the runtime permissions will be granted to all users, otherwise only to the owner.

#### `allUsers`

Flag to indicate that this install should immediately be visible to all users.

#### `installerPackageName`

Installer package for the app. Empty by default, so the calling app package name will be used. Takes effect only on Android 9+.

### Uninstall flags

Take effect only on Android 8.1+.

#### `keepData`

Flag parameter to indicate that you don't want to delete the package's data directory.

#### `allUsers`

Flag parameter to indicate that you want the package deleted for all users.

Capabilities
------------

`ShizukuPlugin` implements [`InstallCapabilityProvider`](../api/ackpine-api/api-main/ru.solrudev.ackpine.capabilities/-install-capability-provider/index.html) and [`UninstallCapabilityProvider`](../api/ackpine-api/api-main/ru.solrudev.ackpine.capabilities/-uninstall-capability-provider/index.html), so you can query whether individual Shizuku install/uninstall parameters are supported for a given configuration via `getCapabilities()`. See [Querying capabilities](configuration.md#querying-capabilities) for an overview of the capabilities API.

Each field in [`ShizukuInstallCapabilities`](../api/ackpine-shizuku/ru.solrudev.ackpine.shizuku/-shizuku-install-capabilities/index.html) mirrors the corresponding `ShizukuPlugin.InstallParameters` flag and reports whether it is supported for the resolved configuration. Similarly, [`ShizukuUninstallCapabilities`](../api/ackpine-shizuku/ru.solrudev.ackpine.shizuku/-shizuku-uninstall-capabilities/index.html) mirrors `ShizukuPlugin.UninstallParameters`.

!!! Note
    Shizuku capability support is determined from Android API level and the effective installer/uninstaller type. Whether Shizuku version is >= 11 and whether it is available for usage by the app at runtime is not taken into account.

=== "Kotlin"

    ```kotlin
    val capabilities = PackageInstaller.getCapabilities(InstallerType.SESSION_BASED, ShizukuPlugin::class)
    val shizukuCaps = capabilities.plugin(ShizukuPlugin::class) ?: return
    if (shizukuCaps.bypassLowTargetSdkBlock.isSupported) {
        // bypassLowTargetSdkBlock is effective on this device
    }
    ```

=== "Java"

    ```java
    var capabilities = PackageInstaller.getCapabilities(InstallerType.SESSION_BASED, ShizukuPlugin.class);
    var shizukuCaps = capabilities.plugin(ShizukuPlugin.class);
    if (shizukuCaps != null && shizukuCaps.getBypassLowTargetSdkBlock().isSupported()) {
        // bypassLowTargetSdkBlock is effective on this device
    }
    ```

For uninstall:

=== "Kotlin"

    ```kotlin
    val capabilities = PackageUninstaller.getCapabilities(UninstallerType.PACKAGE_INSTALLER_BASED, ShizukuPlugin::class)
    val shizukuCaps = capabilities.plugin(ShizukuPlugin::class) ?: return
    if (shizukuCaps.keepData.isSupported) {
        // keepData is effective on this device
    }
    ```

=== "Java"

    ```java
    var capabilities = PackageUninstaller.getCapabilities(UninstallerType.PACKAGE_INSTALLER_BASED, ShizukuPlugin.class);
    var shizukuCaps = capabilities.plugin(ShizukuPlugin.class);
    if (shizukuCaps != null && shizukuCaps.getKeepData().isSupported()) {
        // keepData is effective on this device
    }
    ```