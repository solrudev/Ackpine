Shizuku
=======

`ackpine-shizuku` artifact provides a plugin for Ackpine which, when applied, uses Shizuku to obtain package installer service instead of plain `context.getPackageManager().getPackageInstaller()`, so that Ackpine can manage sessions on behalf of root user or ADB shell. This enables possibility of using such flags as bypassing low target SDK of the installed app on Android 14+, requesting version downgrade, and others. Also it can bypass requirement for user's confirmation for fresh installs.

Kotlin examples below use APIs from `ackpine-shizuku-ktx` artifact.

Setting up Shizuku
------------------

You can see a [full guide](https://github.com/RikkaApps/Shizuku-API#guide) in the Shizuku-API repository.

First of all, if you don't already have Shizuku support in your app, you need to explicitly add Shizuku-API dependencies to your build:

=== "Gradle version catalog"

    ```toml
    [versions]
    shizuku = "see in Shizuku-API repository"
    
    [libraries]
    shizuku-api = { module = "dev.rikka.shizuku:api", version.ref = "shizuku" }
    
    # Add this if you want to support Shizuku
    shizuku-provider = { module = "dev.rikka.shizuku:provider", version.ref = "shizuku" }
    ```

=== "build.gradle.kts"

    ```kotlin
    dependencies {
        val shizukuVersion = "see in Shizuku-API repository"
        implementation("dev.rikka.shizuku:api:shizukuVersion")
    
        // Add this if you want to support Shizuku
        implementation("dev.rikka.shizuku:provider:shizukuVersion")
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

!!! Note
    Shizuku versions below 11 are not supported, and with these versions installations will fall back to normal system's `PackageInstaller`, or `INTENT_BASED` installer (if was set).

If Shizuku service is not running, or if Shizuku permission is not granted for your app, install session will fail.

Plugin parameters
-----------------

### `bypassLowTargetSdkBlock`

Flag to bypass the low target SDK version block for this install.

### `allowTest`

Flag to indicate that you want to allow test packages (those that have set android:testOnly in their manifest) to be installed.

### `replaceExisting`

Flag to indicate that you want to replace an already installed package, if one exists.

### `requestDowngrade`

Flag to indicate that an upgrade to a lower version of a package than currently installed has been requested.

### `grantAllRequestedPermissions`

Flag parameter for package install to indicate that all requested permissions should be granted to the package. If `allUsers` is set the runtime permissions will be granted to all users, otherwise only to the owner.

### `allUsers`

Flag to indicate that this install should immediately be visible to all users.