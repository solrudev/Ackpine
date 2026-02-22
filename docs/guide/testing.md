Testing
=======

Ackpine provides test doubles that work without Android framework dependencies and depend only on `ackpine-api`. They are suitable for JVM unit tests and Android instrumented tests.

Dependencies
------------

=== "Gradle version catalog"

    ```toml
    [libraries]
    ackpine-test = { module = "ru.solrudev.ackpine:ackpine-test", version.ref = "ackpine" }
    ```

=== "build.gradle.kts"

    ```kotlin
    dependencies {
        testImplementation("ru.solrudev.ackpine:ackpine-test:$ackpineVersion")
    }
    ```

Usage
-----

`ackpine-test` exposes in-memory repositories and controllable sessions:

- [`TestPackageInstaller`](/api/ackpine-test/ru.solrudev.ackpine.test/-test-package-installer/index.html) and [`TestPackageUninstaller`](/api/ackpine-test/ru.solrudev.ackpine.test/-test-package-uninstaller/index.html) keep sessions in memory and return [`ImmediateFuture`](/api/ackpine-test/ru.solrudev.ackpine.test.futures/-immediate-future/index.html) from async accessors;
- [`TestSession`](/api/ackpine-test/ru.solrudev.ackpine.test/-test-session/index.html) and [`TestProgressSession`](/api/ackpine-test/ru.solrudev.ackpine.test/-test-progress-session/index.html) expose a [`TestSessionController`](/api/ackpine-test/ru.solrudev.ackpine.test/-test-session-controller/index.html) for [state](../architecture.md#session-state-machine) and progress control, and support scripted transitions via [`TestSessionScript`](/api/ackpine-test/ru.solrudev.ackpine.test/-test-session-script/index.html).

State and progress listeners are invoked on the calling thread, and the current state/progress is delivered immediately when a listener is added.

In Kotlin, [`TestInstallSession`](/api/ackpine-test/ru.solrudev.ackpine.test/-test-install-session/index.html) is a typealias for `TestProgressSession<InstallFailure>`, and [`TestUninstallSession`](/api/ackpine-test/ru.solrudev.ackpine.test/-test-uninstall-session/index.html) is a typealias for `TestSession<UninstallFailure>`.

### `TestSdkInt`

If your tests depend on SDK-dependent behavior, configure [`TestSdkInt`](/api/ackpine-test/ru.solrudev.ackpine.test/-test-sdk-int/index.html) from the test artifact:

=== "Kotlin"

    ```kotlin
    TestSdkInt.set(30)
    ```

=== "Java"

    ```java
    TestSdkInt.set(30);
    ```

For the most part, `TestSdkInt` is used only when constructing session parameters to determine whether Android `PackageInstaller` API is available. Use `TestSdkInt.reset()` to restore the default value.

If you use instrumentation/Robolectric, you can set the value to `Build.VERSION.SDK_INT` for each test.

### `Uri` shim

If you run tests on pure JVM without Android framework (without instrumentation/Robolectric), provide a shim for `android.net.Uri` and place it under `android.net` package. Test doubles don't call any methods on the `Uri` objects, so their implementation can be simple stubs. Shim example:

=== "Kotlin"

    ```kotlin
    package android.net

    import java.io.File

    class Uri {
        companion object {
            @JvmField val EMPTY: Uri = Uri()
            @JvmStatic fun parse(uri: String): Uri = Uri()
            @JvmStatic fun fromFile(file: File): Uri = Uri()
        }
    }
    ```

=== "Java"

    ```java
    package android.net;

    import java.io.File;

    public class Uri {
        public static final Uri EMPTY = new Uri();
        public static Uri parse(String uri) { return new Uri(); }
        public static Uri fromFile(File file) { return new Uri(); }
    }
    ```

You can add any `Uri` methods you use in your code.

### Static `getInstance()` methods

To accommodate testing, it's recommended not to use static `getInstance()` methods to get [`PackageInstaller`](/api/ackpine-api/api-main/ru.solrudev.ackpine.installer/-package-installer/index.html) and [`PackageUninstaller`](/api/ackpine-api/api-main/ru.solrudev.ackpine.uninstaller/-package-uninstaller/index.html) instances inside of your logic. Instead, inject them into constructors.

Even if you don't inject `PackageInstaller` and `PackageUninstaller` interfaces into your SUT (system under test) properly, their static `getInstance()` methods will return singleton test doubles when used in tests.

To get these singletons, call `PackageInstaller.getInstance()` or `PackageUninstaller.getInstance()` and cast them to [`TestPackageInstaller`](/api/ackpine-test/ru.solrudev.ackpine.test/-test-package-installer/index.html) / [`TestPackageUninstaller`](/api/ackpine-test/ru.solrudev.ackpine.test/-test-package-uninstaller/index.html), however, these methods require you to provide `Context`. You can create a shim for `Context` like for `Uri` if you run pure JVM tests.

!!! Note
    If in your tests you rely on `getInstance()` returning a test double, make sure to clear its sessions before every test by calling `TestPackageInstaller.clearSessions()` / `TestPackageUninstaller.clearSessions()`. However, it's highly recommended just to inject a dependency into SUT to immensely simplify test setup.

### Session scripting and factories

`ackpine-test` provides [`TestSessionScript`](/api/ackpine-test/ru.solrudev.ackpine.test/-test-session-script/index.html) to drive scripted transitions on session's `launch`, `commit`, and `cancel` calls.

Use [`TestSessionScript.auto(terminalState)`](/api/ackpine-test/ru.solrudev.ackpine.test/-test-session-script/-companion/auto.html) to auto-advance through the standard states until the provided terminal state is reached, or [`TestSessionScript.empty()`](/api/ackpine-test/ru.solrudev.ackpine.test/-test-session-script/-companion/empty.html) when you want to script state transitions manually or drive transitions directly via [`TestSessionController`](/api/ackpine-test/ru.solrudev.ackpine.test/-test-session-controller/index.html).

Then create a `TestPackageInstaller` or `TestPackageUninstaller` with the script which will be applied to all created sessions:

=== "Kotlin"

    ```kotlin
    val failure = Session.State.Failed(InstallFailure.Generic("Failure"))
    val script: TestSessionScript<InstallFailure> = TestSessionScript.auto(failure)
    val installer = TestPackageInstaller(script)

    // Manual scripting
    val script: TestSessionScript<InstallFailure> = TestSessionScript.empty()
        .onLaunch(Session.State.Awaiting)
        .onCommit(Session.State.Succeeded)
    val installer = TestPackageInstaller(script)

    // State transitions through TestSessionController
    val installer = TestPackageInstaller(TestSessionScript.empty())
    sut.createSession()
    val session = installer.sessions.last()
    session.controller.setState(Session.State.Awaiting)
    session.controller.succeed()
    ```

=== "Java"

    ```java
    var failure = new Session.State.Failed<>(new InstallFailure.Generic("Failure"));
    TestSessionScript<InstallFailure> script = TestSessionScript.auto(failure);
    var installer = new TestPackageInstaller(script);

    // Manual scripting
    TestSessionScript<InstallFailure> script = TestSessionScript.empty()
        .onLaunch(Session.State.Awaiting.INSTANCE)
        .onCommit(Session.State.Succeeded.INSTANCE);
    var installer = new TestPackageInstaller(script);

    // State transitions through TestSessionController
    var installer = new TestPackageInstaller(TestSessionScript.empty());
    sut.createSession();
    var session = installer.getSessions().getLast();
    session.getController().setState(Session.State.Awaiting.INSTANCE);
    session.getController().succeed();
    ```

If you don't provide any script, by default all sessions will auto-advance their state and complete successfully.

You can also provide a session factory to the repository to intercept created sessions, or configure them depending on session's parameters or with different scripts. Simple example:

=== "Kotlin"

    ```kotlin
    val sessions = mutableListOf<TestInstallSession>()
    val installer = TestPackageInstaller { sessionId, params ->
        TestInstallSession(TestSessionScript.empty(), sessionId).also(sessions::add)
    }

    // when your SUT creates a session, it becomes available here
    val session = sessions.last()
    session.controller.succeed()
    ```

=== "Java"

    ```java
    var sessions = new ArrayList<TestProgressSession<InstallFailure>>();
    var installer = new TestPackageInstaller((sessionId, params) -> {
        var session = new TestProgressSession<InstallFailure>(TestSessionScript.empty(), sessionId);
        sessions.add(session);
        return session;
    });

    // when your SUT creates a session, it becomes available here
    var session = sessions.getLast();
    session.getController().succeed();
    ```

### Conceptual example

=== "Kotlin"

    ```kotlin
    val script = TestSessionScript.empty<InstallFailure>()
    	.onLaunch(Session.State.Awaiting)
    	.onCommit(Session.State.Succeeded)
    val installer = TestPackageInstaller(script)
    // or with a custom factory
    val installerWithFactory = TestPackageInstaller { id, params ->
        TestInstallSession(script, id)
    }
    val uri = Uri.EMPTY // shim
    installer.createSession(uri)
    val session = installer.sessions.last()
    val progress = Progress(progress = 80, max = 100)
    session.controller.setProgress(progress)
    val result = session.await()
    assertIs<Session.State.Succeeded>(result)
    assertEquals(progress, session.progress)
    ```

=== "Java"

    ```java
    TestSessionScript<InstallFailure> script = TestSessionScript.empty()
            .onLaunch(Session.State.Awaiting.INSTANCE)
            .onCommit(Session.State.Succeeded.INSTANCE);
    var installer = new TestPackageInstaller(script);
    // or with a custom factory
    var installerWithFactory = new TestPackageInstaller((id, params) ->
            new TestProgressSession<>(script, id));
    var uri = Uri.EMPTY; // shim
    installer.createSession(new InstallParameters.Builder(uri).build());
    var session = installer.getSessions().getLast();
    var progress = new Progress(80, 100);
    session.getController().setProgress(progress);
    // using SettableFuture from Guava
    var future = SettableFuture.<Session.State.Completed<InstallFailure>>create();
    var subscriptions = new DisposableSubscriptionContainer();
    Session.TerminalStateListener.bind(session, subscriptions)
            .addOnSuccessListener(sessionId -> future.set(Session.State.Succeeded.INSTANCE))
            .addOnFailureListener((sessionId, failure) -> future.set(new Session.State.Failed<>(failure)));
    var result = future.get();
    assertTrue(result instanceof Session.State.Succeeded);
    assertEquals(progress, session.getProgress());
    ```

Use [`ImmediateFuture`](/api/ackpine-test/ru.solrudev.ackpine.test.futures/-immediate-future/index.html) when you need a completed `ListenableFuture` for tests. For example, when using [`SplitPackage`](/api/ackpine-splits/splits-main/ru.solrudev.ackpine.splits/-split-package/index.html) API from `ackpine-splits`:

=== "Kotlin"

    ```kotlin
    val provider = SplitPackage.Provider { ImmediateFuture.success(splitPackage) }
    val failingProvider = SplitPackage.Provider { ImmediateFuture.failure(Exception("Failure")) }
    ```

=== "Java"

    ```java
    SplitPackage.Provider provider = () -> ImmediateFuture.success(splitPackage);
    SplitPackage.Provider failingProvider = () -> ImmediateFuture.failure(new Exception("Failure"));
    ```

If you need deterministic session IDs or want to seed sessions into the repositories:

=== "Kotlin"

    ```kotlin
    val uninstaller = TestPackageUninstaller()
    val sessionId = UUID.fromString("00000000-0000-4000-8000-000000000000")
    val session = TestUninstallSession(id = sessionId)
    uninstaller.seedSession(session)
    ```

=== "Java"

    ```java
    var uninstaller = new TestPackageUninstaller();
    var sessionId = UUID.fromString("00000000-0000-4000-8000-000000000000");
    var session = new TestSession<UninstallFailure>(
            TestSessionScript.auto(Session.State.Succeeded.INSTANCE),
            sessionId
    );
    uninstaller.seedSession(session);
    ```

For real and more thorough examples, see [Samples](samples.md#testing-patterns).