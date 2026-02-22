---
hide:
  - navigation
---

FAQ
===

### My session is stuck — what's wrong?

[`Session`](/api/ackpine-api/api-main/ru.solrudev.ackpine.session/-session/index.html) is passive by design — it won't advance its [state](architecture.md#session-state-machine) unless your code drives it:

- A session in **Pending** means the session was never launched.
- A session in **Active** means preparation is still in progress, or was interrupted and needs to be launched again.
- A session in **Awaiting** means preparation is complete but the session was never committed.
- A session in **Committed** means that the session waits for confirmation from user.

Use [`Session.TerminalStateListener.bind()`](/api/ackpine-api/api-main/ru.solrudev.ackpine.session/-session/-terminal-state-listener/-companion/bind.html) (Java) or [`Session.await()`](/api/ackpine-ktx/ru.solrudev.ackpine.session/await.html) (Kotlin), which handle the session lifecycle. See [Getting Started](guide/getting_started.md#simple-session-launch).

In case of a session stuck in **Committed** state, the reason is most probably the lack of confirmation from user. By default, Ackpine will post a high-priority notification to request confirmation from user. If an app doesn't have a permission to display notifications (for example, `POST_NOTIFICATIONS` permission is not granted, or user disabled notifications for the app), session will stay in `Committed` state without progressing.

To fix this, either ensure that your app can post notifications, or change confirmation behavior to `Confirmation.IMMEDIATE` when creating a session. `Confirmation.IMMEDIATE` requires the app to be in foreground. See [confirmation mode details](guide/configuration.md#users-confirmation).

### Which artifacts do I need?

- **Always**: `ackpine-core` — runtime implementation (pulls in `ackpine-api` transitively).
- **Kotlin coroutines**: add `ackpine-ktx` for `Session.await()` and DSL parameter builders.
- **Split APKs**: add `ackpine-splits` (and `ackpine-splits-ktx` for Kotlin extensions) for reading and manipulating split APK packages. See [Split APKs](guide/split_apks.md).
- **APKs from assets**: add `ackpine-assets` for a `ContentProvider` that reads APKs bundled in your app's assets.
- **Privileged installs**: add `ackpine-shizuku` (and `ackpine-shizuku-ktx` for Kotlin DSL) for Shizuku-backed installs. See [Shizuku](guide/shizuku.md).
- **Testing**: add `ackpine-test` as a test dependency for in-memory test doubles. See [Testing](guide/testing.md).

See the [module dependency graph](architecture.md#module-dependency-graph) for a visual overview.

### Does Ackpine support silent installs?

Only in specific scenarios:

- **Via Shizuku plugin**: when your app has root or ADB shell access through [Shizuku](guide/shizuku.md), installs and uninstalls can bypass user confirmation entirely.
- **Via `requireUserAction = false` (delicate API)**: on API level 31+, if certain conditions are met (e.g. the app is updating itself, or the installer has the appropriate permissions), the system may skip user confirmation. See [User's confirmation](guide/configuration.md#users-confirmation) for details and caveats.
- **Via being a device owner / affiliated profile owner / system-signed app**: Ackpine uses Android's `PackageInstaller` which will install silently if an app has such privileges.

In all other cases, Android requires user confirmation for package installation.

### When should I use `SESSION_BASED` vs `INTENT_BASED` installer?

`SESSION_BASED` (default on API 21+) uses Android's `PackageInstaller` API. It supports split APKs, plugins, preapproval, constraints and other options.

`INTENT_BASED` uses `ACTION_INSTALL_PACKAGE` intent. It is more limited — no splits, no advanced features. It is the only option on API level < 21. This option may be more stable on certain Android distributions, especially from Chinese vendors.

If your `minSdk` is 21 or higher, and you don't have a specific reason to use intents, stick with `SESSION_BASED`. See [Installer type](guide/configuration.md#installer-type) for invariants enforced by the install parameters builder.

### How do I uninstall an app?

The API mirrors the install flow. Use `PackageUninstaller` instead of `PackageInstaller`:

=== "Kotlin"

    ```kotlin
    val packageUninstaller = PackageUninstaller.getInstance(context)
    val result = packageUninstaller.createSession("com.example.app").await()
    when (result) {
        Session.State.Succeeded -> println("Uninstalled")
        is Session.State.Failed -> println(result.failure.message)
    }
    ```

=== "Java"

    ```java
    var packageUninstaller = PackageUninstaller.getInstance(context);
    var parameters = new UninstallParameters.Builder("com.example.app").build();
    var session = packageUninstaller.createSession(parameters);
    var subscriptions = new DisposableSubscriptionContainer();
    Session.TerminalStateListener.bind(session, subscriptions)
            .addOnSuccessListener(sessionId -> System.out.println("Uninstalled"))
            .addOnFailureListener((sessionId, failure) -> System.out.println(failure.getMessage()));
    ```

See [Getting Started](guide/getting_started.md) for more detailed examples. Configuration options for uninstall sessions are described in the [Configuration](guide/configuration.md) page.

### What happens if my app is killed during installation?

Session state is persisted to a database, so sessions survive process death. After your app restarts, re-retrieve the session by its saved ID from [`PackageInstaller`](/api/ackpine-api/api-main/ru.solrudev.ackpine.installer/-package-installer/index.html) or [`PackageUninstaller`](/api/ackpine-api/api-main/ru.solrudev.ackpine.uninstaller/-package-uninstaller/index.html) and re-attach your listeners (via [`Session.TerminalStateListener.bind()`](/api/ackpine-api/api-main/ru.solrudev.ackpine.session/-session/-terminal-state-listener/-companion/bind.html) / [`Session.await()`](/api/ackpine-ktx/ru.solrudev.ackpine.session/await.html)). The session will resume from where it left off — if preparations were interrupted, attaching these listeners again re-starts preparation; if confirmation was interrupted, attaching these listeners again re-requests confirmation.

See [Handling process death](guide/getting_started.md#handling-process-death) for code examples and [Architecture](architecture.md#re-entrancy-for-process-death-resilience) for the design behind this.