package ru.solrudev.ackpine.exceptions

import ru.solrudev.ackpine.splits.Apk

/**
 * Thrown when split package is invalid.
 */
public sealed class SplitPackageException(message: String) : RuntimeException(message)

/**
 * Thrown when no [base APK][Apk.Base] was found.
 */
public class NoBaseApkException : SplitPackageException("No base APK found")

/**
 * Thrown when more than one [base APK][Apk.Base] was found.
 */
public class ConflictingBaseApkException : SplitPackageException("More than one base APK found")

/**
 * Thrown when some [APK splits][Apk] are conflicting by their split name.
 */
public class ConflictingSplitNameException(public val name: String) :
	SplitPackageException("Conflicting split name: $name")

/**
 * Thrown when some [APK splits][Apk] are conflicting by their package name.
 */
public class ConflictingPackageNameException(
	public val expected: String,
	public val actual: String,
	public val name: String
) : SplitPackageException("Conflicting package name. Expected: $expected, found: $actual, name: $name")

/**
 * Thrown when some [APK splits][Apk] are conflicting by their version code.
 */
public class ConflictingVersionCodeException(
	public val expected: Long,
	public val actual: Long,
	public val name: String
) : SplitPackageException("Conflicting version code. Expected: $expected, found: $actual, name: $name")