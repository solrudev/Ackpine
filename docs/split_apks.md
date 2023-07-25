Split APKs
==========

`ackpine-splits` artifact contains utilities for working with split APK files.

Working with zipped splits
--------------------------

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

!!! Attention
    Iteration of these sequences is blocking due to I/O operations. Don't iterate them on UI thread!

`Apk` has the following properties:

```kotlin
uri: Uri
name: String
size: Long
packageName: String
versionCode: Long
description: String
```

!!! Note
    If your application doesn't have direct access to files (via `MANAGE_EXTERNAL_STORAGE` or `READ_EXTERNAL_STORAGE` permissions), parsing and iteration of the sequences will be much slower, because Ackpine will fall back to using `ZipInputStream` for these operations.

`Apk` has the following types: `Base` for base APK, `Feature` for a feature split, `Libs` for an APK split containing native libraries, `ScreenDensity` for an APK split containing graphic resources tailored to specific screen density, `Localization` for an APK split containing localized resources and `Other` for an unknown APK split. They also have their specific properties. Refer to [API documentation](api/ackpine-splits/index.html) for details.

`ApkSplits` class contains utilities for transforming `Apk` sequences.

=== "Kotlin"

    ```kotlin
    val splits = ZippedApkSplits.getApksForUri(zippedFileUri, context)
        .filterIncompatible(context)
        .throwOnInvalidSplitPackage()
    val splitsList = try {
        splits.toList()
    } catch (exception: SplitPackageException) {
        println(exception)
        emptyList()
    }
    ```

=== "Java"

    ```java
    Sequence<Apk> zippedApkSplits = ZippedApkSplits.getApksForUri(uri, context);
    Sequence<Apk> filteredSplits = ApkSplits.filterIncompatible(zippedApkSplits, context);
    Sequence<Apk> splits = ApkSplits.throwOnInvalidSplitPackage(filteredSplits);
    List<Apk> splitsList = new ArrayList<>();
	try {
	    for (var iterator = splits.iterator(); iterator.hasNext(); ) {
            var apk = iterator.next();
            splitsList.add(apk);
        }
    } catch (SplitPackageException exception) {
        System.out.println(exception);
        splitsList = Collections.emptyList();
    }
    ```

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