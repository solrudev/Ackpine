Split APKs
==========

`ackpine-splits` artifact contains utilities for working with split APK files.

Reading zipped splits
---------------------

`ZippedApkSplits` class contains factory methods for lazy sequences of APK splits which are contained inside of a zipped file such as ZIP, APKS, APKM and XAPK.

=== "Kotlin"

    ```kotlin
    val splits: CloseableSequence<Apk> = ZippedApkSplits.getApksForUri(zippedFileUri, context)
    val splitsList = splits.toList()
    ```

=== "Java"

    ```java
    CloseableSequence<Apk> splits = ZippedApkSplits.getApksForUri(zippedFileUri, context);
    List<Apk> splitsList = new ArrayList<>();
	for (var iterator = splits.iterator(); iterator.hasNext(); ) {
        var apk = iterator.next();
        splitsList.add(apk);
    }
    ```

!!! warning "Attention"
    Iteration of these sequences is blocking due to I/O operations. Don't iterate them on UI thread!

As you can see, these sequences have a type of `CloseableSequence` which implements `AutoCloseable`. That means they can close all held resources, and effectively be cancelled externally at any moment with a `close()` call.

`Apk` has the following properties:

```kotlin
val uri: Uri
val name: String
val size: Long
val packageName: String
val versionCode: Long
val description: String
```

!!! Note
    If your application doesn't have direct access to files (via `READ_EXTERNAL_STORAGE` permission), parsing and iteration of the sequences may be much slower or even fail on Android versions lower than 8.0 Oreo, because Ackpine may fall back to using `ZipInputStream` for these operations.

`Apk` has the following types: `Base` for base APK, `Feature` for a feature split, `Libs` for an APK split containing native libraries, `ScreenDensity` for an APK split containing graphic resources tailored to specific screen density, `Localization` for an APK split containing localized resources and `Other` for an unknown APK split. They also have their specific properties. Refer to [API documentation](api/ackpine-splits/index.html) for details.

Working with splits
-------------------

### Sequences transformations

`ApkSplits` class contains utilities for transforming `Apk` sequences. In Kotlin they appear as extensions of `Sequence<Apk>`.

For sequences of APKs, the following operations are available:

- `validate()` operation validates a split package and throws `SplitPackageException` if it's not valid when the sequence is iterated, while also closing all opened I/O resources. This operation cooperates with cancellation if upstream sequence supports it.

Example:

=== "Kotlin"

    ```kotlin
    val splits: CloseableSequence<Apk> = ZippedApkSplits
        .getApksForUri(zippedFileUri, context)
        .validate()
    val splitsList = try {
        splits.toList()
    } catch (exception: SplitPackageException) {
        println(exception)
        emptyList()
    } 
    ```

=== "Java"

    ```java
    CloseableSequence<Apk> splits = ZippedApkSplits.getApksForUri(zippedFileUri, context);
    CloseableSequence<Apk> validatedSplits = ApkSplits.validate(splits);
    List<Apk> splitsList = new ArrayList<>();
	try {
        for (var iterator = validatedSplits.iterator(); iterator.hasNext(); ) {
            var apk = iterator.next();
            splitsList.add(apk);
        }
    } catch (SplitPackageException exception) {
        System.out.println(exception);
        splitsList = Collections.emptyList();
    }
    ```

### `SplitPackage` API

For manipulating split packages, you can use `SplitPackage` API.

`ackpine-splits-ktx` module contains Kotlin-idiomatic extensions which are used in the examples below.

First, you create a `SplitPackage.Provider` from a sequence of APKs, e.g. obtained from `ZippedApkSplits`:

=== "Kotlin"

    ```kotlin
    val splits = sequence.toSplitPackage()
    ```

=== "Java"

    ```java
    var splits = SplitPackage.from(sequence);
    ```

Then you can apply different operations to it, and when you're done, materialize it into a `SplitPackage` object:

=== "Kotlin"

    ```kotlin
    val sortedSplits = splits.sortedByCompatibility(context)
    val splitPackage = sortedSplits.get() // <- suspending, cancellable
    // print SplitPackage entries for libs APKs
    println(splitPackage.libs)
    ```

=== "Java"

    ```java
    var sortedSplits = splits.sortedByCompatibility(context);
    var splitPackageFuture = sortedSplits.getAsync(); // cancellable
    // using Guava
    Futures.addCallback(splitPackageFuture, new FutureCallback<>() {
        @Override
        public void onSuccess(@NonNull SplitPackage splitPackage) {
            // print SplitPackage entries for libs APKs
            System.out.println(splitPackage.getLibs());
        }
        
        @Override
        public void onFailure(@NonNull Throwable t) {
        }
    }, MoreExecutors.directExecutor());
    ```

The `get()` is cancellable if split package source supports cancellation (such as `CloseableSequence`).

`libs` in the previous example is a `List<SplitPackage.Entry<Apk.Libs>>`.

Each entry in APK lists inside of `SplitPackage` (such as `libs`, `localization` etc.) has `isPreferred` and `apk` properties:

- `isPreferred` — indicates whether the APK is the most preferred for the device among all splits of the same type. By default it is `true`. When an operation which checks compatibility is applied, this flag is updated accordingly;
- `apk` — `Apk` object.

`SplitPackage` can be flattened to a plain list of entries by calling `toList()`. Also you can filter out all entries where `isPreferred=false` with `filterPreferred()`:

=== "Kotlin"

    ```kotlin
    val splitsList = splitPackage.toList()
    val compatibleSplits = splitPackage.filterPreferred()
    ```

=== "Java"

    ```java
    var splitsList = splitPackage.toList();
    var compatibleSplits = splitPackage.filterPreferred();
    ```

List of available `SplitPackage.Provider` operations:

- `sortedByCompatibility(Context)` operation returns a provider that gives out APK splits sorted according to their compatibility with the device. The most preferred APK splits will appear first. If exact device's screen density, ABI or locale doesn't appear in the splits, nearest matching split is chosen as a preferred one.

- `filterCompatible(Context)` operation filters out the splits which are not the most preferred for the device. It acts the same as applying `sortedByCompatibility(context)` to the provider and calling `filterPreferred()` for the resulting `SplitPackage`.

Full example of a pipeline:

=== "Kotlin"

    ```kotlin
    val splits = ZippedApkSplits.getApksForUri(zippedFileUri, context)
        .validate()
        .toSplitPackage()
        .sortedByCompatibility(context)
    val sortedSplits = try {
        splits.get()             // <- suspending, cancellable
    } catch (exception: SplitPackageException) {
        println(exception)
        SplitPackage.empty().get()
    }
    println(sortedSplits.libs)   // prints SplitPackage entries for libs APKs,
                                 // ordered by their compatibility with the device
    val splitsToInstall = sortedSplits.filterPreferred()
    sortedSplits
        .toList()
        .filterNot { entry -> entry.isPreferred }
        .map { entry -> entry.apk }
        .forEach(::println)      // prints incompatible APKs
    ```

=== "Java"

    ```java
    Sequence<Apk> zippedApkSplits = ZippedApkSplits.getApksForUri(uri, context);
    Sequence<Apk> validatedSplits = ApkSplits.validate(zippedApkSplits);
    SplitPackage.Provider splits = SplitPackage
            .from(validatedSplits)
            .sortedByCompatibility(context);
    // using Guava
    Futures.addCallback(splits.getAsync(), new FutureCallback<>() {
        @Override
        public void onSuccess(@NonNull SplitPackage sortedSplits) {
            // prints SplitPackage entries for libs APKs,
            // ordered by their compatibility with the device
            System.out.println(sortedSplits.getLibs());
            var splitsToInstall = sortedSplits.filterPreferred();
            for (var entry : sortedSplits.toList()) {
                if (!entry.isPreferred()) {
                    System.out.println(entry.getApk()); // prints incompatible APKs
                }
            }
        }
        
        @Override
        public void onFailure(@NonNull Throwable exception) {
            if (exception instanceof SplitPackageException) {
                System.out.println(exception);
            }
        }
    }, MoreExecutors.directExecutor());
    ```

Creating APK splits from separate files
---------------------------------------

You can parse an APK file from a `File` or `Uri` using static `Apk` factories:

=== "Kotlin"

    ```kotlin
    val apkFromFile: Apk? = Apk.fromFile(file, context)
    val apkFromUri: Apk? = Apk.fromUri(uri, context, cancellationSignal)
    ```

=== "Java"

    ```java
    Apk apkFromFile = Apk.fromFile(file, context);
    Apk apkFromUri = Apk.fromUri(uri, context, cancellationSignal);
    ```