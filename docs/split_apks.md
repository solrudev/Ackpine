Split APKs
==========

`ackpine-splits` artifact contains utilities for working with split APK files.

Reading zipped splits
---------------------

`ZippedApkSplits` class contains factory methods for sequences of APK splits which are contained inside of a zipped file such as ZIP, APKS, APKM and XAPK.

=== "Kotlin"

    ```kotlin
    val splits: Sequence<Apk> = ZippedApkSplits.getApksForUri(zippedFileUri, context)
    val splitsList = splits.toList()
    ```

=== "Java"

    ```java
    Sequence<Apk> splits = ZippedApkSplits.getApksForUri(zippedFileUri, context);
    List<Apk> splitsList = new ArrayList<>();
	for (var iterator = splits.iterator(); iterator.hasNext(); ) {
        var apk = iterator.next();
        splitsList.add(apk);
    }
    ```

!!! warning "Attention"
    Iteration of these sequences is blocking due to I/O operations. Don't iterate them on UI thread!

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

`ApkSplits` class contains utilities for transforming `Apk` sequences. In Kotlin they appear as extensions of `Sequence<Apk>` and `Sequence<ApkCompatibility>`.

=== "Kotlin"

    ```kotlin
    val sortedSplitsList = mutableListOf<ApkCompatibility>()
    // Building a one-time pipeline
    val splits = ZippedApkSplits.getApksForUri(zippedFileUri, context)
        .throwOnInvalidSplitPackage()
        .sortedByCompatibility(context)
        .addAllTo(sortedSplitsList)
        .filterCompatible()
    val splitsList = try {
        splits.toList()
    } catch (exception: SplitPackageException) {
        println(exception)
        emptyList()
    }
    sortedSplitsList.filterNot { it.isPreferred }
        .map { it.apk }
        .forEach(::println) // prints incompatible APKs
    ```

=== "Java"

    ```java
    List<ApkCompatibility> sortedSplitsList = new ArrayList<>();
    // Building a one-time pipeline
    Sequence<Apk> zippedApkSplits = ZippedApkSplits.getApksForUri(uri, context);
    Sequence<Apk> verifiedSplits = ApkSplits.throwOnInvalidSplitPackage(zippedApkSplits);
    Sequence<ApkCompatibility> sortedSplits = ApkSplits.sortedByCompatibility(verifiedSplits, context);
    Sequence<ApkCompatibility> addingToListSplits = ApkSplits.addAllTo(sortedSplits, sortedSplitsList);
    Sequence<Apk> filteredSplits = ApkSplits.filterCompatible(addingToListSplits);
    List<Apk> splitsList = new ArrayList<>();
	try {
	    for (var iterator = filteredSplits.iterator(); iterator.hasNext(); ) {
            var apk = iterator.next();
            splitsList.add(apk);
        }
    } catch (SplitPackageException exception) {
        System.out.println(exception);
        splitsList = Collections.emptyList();
    }
    for (var apkCompatibility : sortedSplitsList) {
        if (!apkCompatibility.isPreferred()) {
            System.out.println(apkCompatibility.getApk()); // prints incompatible APKs
        }
    }
    
    ```

- `throwOnInvalidSplitPackage()` operation validates a split package and throws `SplitPackageException` if it's not valid when the sequence is iterated.

- `sortedByCompatibility(Context)` operation returns a sequence that contains APK splits sorted according to their compatibility with the device. The most preferred APK splits will appear first. If exact device's screen density, ABI or locale doesn't appear in the splits, nearest matching split is chosen as a preferred one. The returned sequence contains `ApkCompatibility` objects, which are pairs of an `isPreferred` flag and an `Apk` object being checked.

- `filterCompatible()` operation filters out the splits which are not the most preferred for the device. A `filterCompatible(Context)` overload is the same as writing `sortedByCompatibility(context).filterCompatible()`.

!!! Note
    To correctly close I/O resources and skip unnecessary I/O operations, it's best to apply `throwOnInvalidSplitPackage()` immediately after creating the sequence with `ZippedApkSplits` factories.

Creating APK splits from separate files
---------------------------------------

You can parse an APK file from a `File` or `Uri` using static `Apk` factories:

=== "Kotlin"

    ```kotlin
    val apkFromFile: Apk? = Apk.fromFile(file, context)
    val apkFromUri: Apk? = Apk.fromUri(uri, context)
    ```

=== "Java"

    ```java
    Apk apkFromFile = Apk.fromFile(file, context);
    Apk apkFromUri = Apk.fromUri(uri, context);
    ```