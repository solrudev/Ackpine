---
icon: lucide/hash
---

libsu
=====

`ackpine-libsu` artifact provides a plugin for Ackpine which, when applied, uses [libsu](https://github.com/topjohnwu/libsu) to perform package installer operations under root user. This enables possibility of using such flags as bypassing low target SDK of the installed app on Android 14+, requesting version downgrade, keeping app data when uninstalling and others. Also it can bypass requirement for user's confirmation for uninstalls and fresh installs.

Kotlin examples below use APIs from `ackpine-libsu-ktx` artifact. For general plugin usage in session parameters, see [Configuration](configuration.md#plugins).

!!! Note
    Ensure that root access is available to successfully use this plugin. On first usage, root access prompt from the root manager app (such as Magisk) will be shown to the user if not already granted for your app.

Dependencies
------------

`ackpine-libsu` depends on `libsu`. Its artifacts are published on JitPack, so make sure you have the JitPack repository in your `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        maven {
            url = uri("https://jitpack.io")
            content {
                includeGroup("com.github.topjohnwu.libsu")
            }
        }
    }
}
```

Then add an `ackpine-libsu` dependency:

=== "Gradle version catalog"

    ```toml
    [libraries]
    ackpine-libsu = { module = "ru.solrudev.ackpine:ackpine-libsu", version.ref = "ackpine" }
    
    # Kotlin extensions
    ackpine-libsu-ktx = { module = "ru.solrudev.ackpine:ackpine-libsu-ktx", version.ref = "ackpine" }
    ```

=== "build.gradle.kts"

    ```kotlin
    dependencies {
        implementation("ru.solrudev.ackpine:ackpine-libsu:$ackpineVersion")
    
        // Kotlin extensions
        implementation("ru.solrudev.ackpine:ackpine-libsu-ktx:$ackpineVersion")
    }
    ```

Using the plugin
----------------

To apply the plugin to an install session, just add this to your install parameters configuration:

=== "Kotlin"

    ```kotlin
    val session = packageInstaller.createSession(uri) {
        // ...some session configuration...
        libsu()
    
        // Or, if you want to configure some parameters for the plugin
        libsu {
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
            .registerPlugin(LibsuPlugin.class, LibsuPlugin.InstallParameters.DEFAULT)
            .build();
    
    // Or, if you want to configure some parameters for the plugin
    var libsuParameters = new LibsuPlugin.InstallParameters.Builder()
            .setBypassLowTargetSdkBlock(true)
            .setAllowTest(true)
            .setReplaceExisting(true)
            .setRequestDowngrade(true)
            .setGrantAllRequestedPermissions(true)
            .setAllUsers(true)
            .setInstallerPackageName("com.android.vending")
            .build();
    var parameters = new InstallParameters.Builder(uri)
            .registerPlugin(LibsuPlugin.class, libsuParameters)
            .build();
    ```

Also, you can use libsu for uninstall sessions:

=== "Kotlin"

    ```kotlin
    val session = packageUninstaller.createSession(packageName) {
        // ...some session configuration...
        libsu()
    
        // Or, if you want to configure some parameters for the plugin
        libsu {
            keepData = true
            allUsers = true
        }
    }
    ```

=== "Java"

    ```java
    var parameters = new UninstallParameters.Builder(packageName)
            // ...some session configuration...
            .registerPlugin(LibsuPlugin.class, LibsuPlugin.UninstallParameters.DEFAULT)
            .build();
    
    // Or, if you want to configure some parameters for the plugin
    var libsuParameters = new LibsuPlugin.UninstallParameters.Builder()
            .setKeepData(true)
            .setAllUsers(true)
            .build();
    var parameters = new UninstallParameters.Builder(packageName)
            .registerPlugin(LibsuPlugin.class, libsuParameters)
            .build();
    ```

If root access is not available, session will fail.

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

`LibsuPlugin` implements [`InstallCapabilityProvider`](../api/ackpine-api/api-main/ru.solrudev.ackpine.capabilities/-install-capability-provider/index.html) and [`UninstallCapabilityProvider`](../api/ackpine-api/api-main/ru.solrudev.ackpine.capabilities/-uninstall-capability-provider/index.html), so you can query whether individual libsu install/uninstall parameters are supported for a given configuration via `getCapabilities()`. See [Querying capabilities](configuration.md#querying-capabilities) for an overview of the capabilities API.

Each field in [`LibsuInstallCapabilities`](../api/ackpine-plugins/libsu/ru.solrudev.ackpine.libsu/-libsu-install-capabilities/index.html) mirrors the corresponding `LibsuPlugin.InstallParameters` flag and reports whether it is supported for the resolved configuration. Similarly, [`LibsuUninstallCapabilities`](../api/ackpine-plugins/libsu/ru.solrudev.ackpine.libsu/-libsu-uninstall-capabilities/index.html) mirrors `LibsuPlugin.UninstallParameters`.

!!! Note
    libsu capability support is determined from Android API level and the effective installer/uninstaller type. Whether root access is available at runtime is not taken into account.

=== "Kotlin"

    ```kotlin
    val capabilities = PackageInstaller.getCapabilities(InstallerType.SESSION_BASED, LibsuPlugin::class)
    val libsuCaps = capabilities.plugin(LibsuPlugin::class) ?: return
    if (libsuCaps.bypassLowTargetSdkBlock.isSupported) {
        // bypassLowTargetSdkBlock is effective on this device
    }
    ```

=== "Java"

    ```java
    var capabilities = PackageInstaller.getCapabilities(InstallerType.SESSION_BASED, LibsuPlugin.class);
    var libsuCaps = capabilities.plugin(LibsuPlugin.class);
    if (libsuCaps != null && libsuCaps.getBypassLowTargetSdkBlock().isSupported()) {
        // bypassLowTargetSdkBlock is effective on this device
    }
    ```

For uninstall:

=== "Kotlin"

    ```kotlin
    val capabilities = PackageUninstaller.getCapabilities(UninstallerType.PACKAGE_INSTALLER_BASED, LibsuPlugin::class)
    val libsuCaps = capabilities.plugin(LibsuPlugin::class) ?: return
    if (libsuCaps.keepData.isSupported) {
        // keepData is effective on this device
    }
    ```

=== "Java"

    ```java
    var capabilities = PackageUninstaller.getCapabilities(UninstallerType.PACKAGE_INSTALLER_BASED, LibsuPlugin.class);
    var libsuCaps = capabilities.plugin(LibsuPlugin.class);
    if (libsuCaps != null && libsuCaps.getKeepData().isSupported()) {
        // keepData is effective on this device
    }
    ```