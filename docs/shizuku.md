Shizuku
=======

`ackpine-shizuku` artifact provides plugins for Ackpine which, when applied, use Shizuku to obtain package installer service instead of plain `context.getPackageManager().getPackageInstaller()`, so that Ackpine can manage sessions on behalf of root user or ADB shell even if your app doesn't have such privileges. This enables possibility of using such flags as bypassing low target SDK of the installed app on Android 14+, requesting version downgrade, keeping app data when uninstalling and others. Also it can bypass requirement for user's confirmation for uninstalls and fresh installs.

Kotlin examples below use APIs from `ackpine-shizuku-ktx` artifact.

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
        useShizuku()
    
        // Or, if you want to configure some parameters for the plugin
        useShizuku {
            bypassLowTargetSdkBlock = true
            allowTest = true
            replaceExisting = true
            requestDowngrade = true
            grantAllRequestedPermissions = true
            allUsers = true
        }
    }
    ```

=== "Java"

    ```java
    var parameters = new InstallParameters.Builder(uri)
            // ...some session configuration...
            .usePlugin(ShizukuPlugin.class, ShizukuPlugin.Parameters.DEFAULT)
            .build();
    
    // Or, if you want to configure some parameters for the plugin
    var shizukuParameters = new ShizukuPlugin.Parameters.Builder()
            .setBypassLowTargetSdkBlock(true)
            .setAllowTest(true)
            .setReplaceExisting(true)
            .setRequestDowngrade(true)
            .setGrantAllRequestedPermissions(true)
            .setAllUsers(true)
            .build()
    var parameters = new InstallParameters.Builder(uri)
            .usePlugin(ShizukuPlugin.class, shizukuParameters)
            .build();
    ```

Also, you can use Shizuku for uninstall sessions:

=== "Kotlin"

    ```kotlin
    val session = packageUninstaller.createSession(packageName) {
        // ...some session configuration...
        useShizuku()
    
        // Or, if you want to configure some parameters for the plugin
        useShizuku {
            keepData = true
            allUsers = true
        }
    }
    ```

=== "Java"

    ```java
    var parameters = new UninstallParameters.Builder(packageName)
            // ...some session configuration...
            .usePlugin(ShizukuUninstallPlugin.class, ShizukuUninstallPlugin.Parameters.DEFAULT)
            .build();
    
    // Or, if you want to configure some parameters for the plugin
    var shizukuParameters = new ShizukuUninstallPlugin.Parameters.Builder()
            .setKeepData(true)
            .setAllUsers(true)
            .build()
    var parameters = new UninstallParameters.Builder(packageName)
            .usePlugin(ShizukuUninstallPlugin.class, shizukuParameters)
            .build();
    ```

!!! Note
    Shizuku versions below 11 are not supported, and with these versions installations will fall back to normal system's `PackageInstaller`, or `INTENT_BASED` installer (if was set).

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

### Uninstall flags

#### `keepData`

Flag parameter to indicate that you don't want to delete the package's data directory.

#### `allUsers`

Flag parameter to indicate that you want the package deleted for all users.