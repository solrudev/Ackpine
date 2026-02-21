Split APKs
==========

`ackpine-splits` artifact contains utilities for working with split APK files.

Add it to your dependencies:

=== "Gradle version catalog"

    ```toml
    [libraries]
    ackpine-splits = { module = "ru.solrudev.ackpine:ackpine-splits", version.ref = "ackpine" }
    
    # Kotlin extensions
    ackpine-splits-ktx = { module = "ru.solrudev.ackpine:ackpine-splits-ktx", version.ref = "ackpine" }
    ```

=== "build.gradle.kts"

    ```kotlin
    dependencies {
        implementation("ru.solrudev.ackpine:ackpine-splits:$ackpineVersion")
    
        // Kotlin extensions
        implementation("ru.solrudev.ackpine:ackpine-splits-ktx:$ackpineVersion")
    }
    ```

Reading zipped splits
---------------------

[`ZippedApkSplits`](/api/ackpine-splits/splits-main/ru.solrudev.ackpine.splits/-zipped-apk-splits/index.html) class contains factory methods for lazy sequences of APK splits which are contained inside of a zipped file such as ZIP, APKS, APKM and XAPK.

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

As you can see, these sequences have a type of [`CloseableSequence`](/api/ackpine-splits/splits-main/ru.solrudev.ackpine.splits/-closeable-sequence/index.html) which implements `AutoCloseable`. That means they can close all held resources, and effectively be cancelled externally at any moment with a `close()` call.

[`Apk`](/api/ackpine-splits/splits-main/ru.solrudev.ackpine.splits/-apk/index.html) has the following properties:

```kotlin
val uri: Uri
val name: String
val size: Long
val packageName: String
val versionCode: Long
val description: String
```

`Apk` has the following types: [`Base`](/api/ackpine-splits/splits-main/ru.solrudev.ackpine.splits/-apk/-base/index.html) for base APK, [`Feature`](/api/ackpine-splits/splits-main/ru.solrudev.ackpine.splits/-apk/-feature/index.html) for a feature split, [`Libs`](/api/ackpine-splits/splits-main/ru.solrudev.ackpine.splits/-apk/-libs/index.html) for an APK split containing native libraries, [`ScreenDensity`](/api/ackpine-splits/splits-main/ru.solrudev.ackpine.splits/-apk/-screen-density/index.html) for an APK split containing graphic resources tailored to specific screen density, [`Localization`](/api/ackpine-splits/splits-main/ru.solrudev.ackpine.splits/-apk/-localization/index.html) for an APK split containing localized resources and [`Other`](/api/ackpine-splits/splits-main/ru.solrudev.ackpine.splits/-apk/-other/index.html) for an unknown APK split. They also have their specific properties. Refer to [API documentation](/api/ackpine-splits/splits-main/ru.solrudev.ackpine.splits/-apk/index.html) for details.

Working with splits
-------------------

### Sequences transformations

[`ApkSplits`](/api/ackpine-splits/splits-main/ru.solrudev.ackpine.splits/-apk-splits/index.html) class contains utilities for transforming `Apk` sequences. In Kotlin they appear as extensions of `Sequence<Apk>`.

For sequences of APKs, the following operations are available:

- `validate()` operation validates a split package and throws [`SplitPackageException`](/api/ackpine-splits/splits-main/ru.solrudev.ackpine.exceptions/-split-package-exception/index.html) if it's not valid when the sequence is iterated, while also closing all opened I/O resources. This operation cooperates with cancellation if upstream sequence supports it.

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

For manipulating split packages, you can use [`SplitPackage`](/api/ackpine-splits/splits-main/ru.solrudev.ackpine.splits/-split-package/index.html) API.

`ackpine-splits-ktx` module contains Kotlin-idiomatic extensions which are used in the examples below.

First, you create a [`SplitPackage.Provider`](/api/ackpine-splits/splits-main/ru.solrudev.ackpine.splits/-split-package/-provider/index.html) from a sequence of APKs, e.g. obtained from `ZippedApkSplits`:

=== "Kotlin"

    ```kotlin
    val splits = sequence.toSplitPackage()
    ```

=== "Java"

    ```java
    var splits = SplitPackage.from(sequence);
    ```

Then you can apply different operations to it, and when you're done, materialize it into a [`SplitPackage`](/api/ackpine-splits/splits-main/ru.solrudev.ackpine.splits/-split-package/index.html) object:

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

Each [entry](/api/ackpine-splits/splits-main/ru.solrudev.ackpine.splits/-split-package/-entry/index.html) in APK lists inside of `SplitPackage` (such as `libs`, `localization` etc.) has `isPreferred` and `apk` properties:

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

List of available [`SplitPackage.Provider`](/api/ackpine-splits/splits-main/ru.solrudev.ackpine.splits/-split-package/-provider/index.html) operations:

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

You can parse an APK file from a `File` or `Uri` using static [`Apk`](/api/ackpine-splits/splits-main/ru.solrudev.ackpine.splits/-apk/index.html) factories:

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