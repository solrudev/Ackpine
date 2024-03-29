Getting Started
===============

To obtain an instance of `PackageInstaller` or `PackageUninstaller`, use the `getInstance(Context)` method:

=== "Kotlin"

    ```kotlin
    val packageInstaller = PackageInstaller.getInstance(context)
    val packageUninstaller = PackageUninstaller.getInstance(context)
    ```

=== "Java"

    ```java
    var packageInstaller = PackageInstaller.getInstance(context);
    var packageUninstaller = PackageUninstaller.getInstance(context);
    ```

Simple session launch
---------------------

Launching an install or uninstall session with default parameters and getting its result back is as easy as writing this:

=== "Kotlin"

    ```kotlin
    try {
        when (val result = packageInstaller.createSession(apkUri).await()) {
            is SessionResult.Success -> println("Success")
            is SessionResult.Error -> println(result.cause.message)
        }
    } catch (_: CancellationException) {
        println("Cancelled")
    } catch (exception: Exception) {
        println(exception)
    }
    ```

    Session launches when `await()` is called.

=== "Java"

    ```java
    var subscriptions = new DisposableSubscriptionContainer();
    var session = packageInstaller.createSession(new InstallParameters.Builder(apkUri).build());
    session.addStateListener(subscriptions, new Session.TerminalStateListener<>(session) {
        @Override
        public void onSuccess(@NonNull UUID sessionId) {
            System.out.println("Success");
        }
        
        @Override
        public void onFailure(@NonNull UUID sessionId, @NonNull InstallFailure failure) {
    	    if (failure instanceof Failure.Exceptional f) {
    	        System.out.println(f.getException());
    	    } else {
                System.out.println(failure.getCause().getMessage());
    	    }
        }
        
        @Override
        public void onCancelled(@NonNull UUID sessionId) {
            System.out.println("Cancelled");
        }
    });
    ```

    Session launches when `TerminalStateListener` is added to it.

It works as long as you don't care about UI lifecycle and unpredictable situations such as process death.

Handling UI lifecycle
---------------------

If you're launching a session inside of a long-living service which is not expected to be killed (such as a foreground service), the previous example is good to go. However, when you are dealing with UI components such as Activities or Fragments, it's good practice to remove attached state listeners when appropriate:

=== "Kotlin"

    When using `ackpine-ktx` artifact and calling `Session.await()`, the listener will be automatically detached when parent coroutine scope is cancelled. So if you're calling `await()` inside of a `viewModelScope` or `lifecycleScope`, it should be fine. Note that cancelling `await()` also cancels the session, this is done to respect coroutines' structured concurrency.

=== "Java"

    ```java
    var subscriptions = new DisposableSubscriptionContainer();
    var session = packageInstaller.createSession(...);
    session.addStateListener(subscriptions, ...);
    
    // when lifecycle is destroyed
    subscriptions.clear();
	
    ```

Handling process death
----------------------

Handling process death is not any different with Ackpine as with any other persisted state handling. You can save a session's ID and then re-retrieve the session from `PackageInstaller`:

=== "Kotlin"

    ```kotlin
    savedStateHandle[SESSION_ID_KEY] = session.id
    
    // after process restart
    val id: UUID? = savedStateHandle[SESSION_ID_KEY]
    if (id != null) {
        val result = packageInstaller.getSession(id)?.await()
		// or anything else you want to do with the session
    }
    ```

=== "Java"

    ```java
    savedStateHandle.set(SESSION_ID_KEY, session.getId());
    
    // after process restart
    UUID id = savedStateHandle.get(SESSION_ID_KEY);
    if (id != null) {
        // using Guava
        Futures.addCallback(packageInstaller.getSessionAsync(id), new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable ProgressSession<InstallFailure> session) {
                if (session != null) {
                    session.addStateListener(subscriptions, ...);
                    // or anything else you want to do with the session
                }
            }
            
            @Override
            public void onFailure(@NonNull Throwable t) {
            }
        }, MoreExecutors.directExecutor());
    }
    
    ```

Observing progress
------------------

Install sessions provide progress updates:

=== "Kotlin"

    ```kotlin
    session.progress // Flow<Progress>
        .onEach { progress ->
            updateProgress(progress.progress, progress.max)
        }
        .launchIn(coroutineScope)
    ```

=== "Java"

    ```java
    session.addProgressListener(subscriptions, (sessionId, progress) -> {
        updateProgress(progress.getProgress(), progress.getMax());
    });
    ```

Every example on this page is using `PackageInstaller`, but APIs for `PackageUninstaller` are absolutely the same except for progress updates.