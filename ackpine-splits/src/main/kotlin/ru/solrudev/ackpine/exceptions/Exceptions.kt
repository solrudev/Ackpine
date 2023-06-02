package ru.solrudev.ackpine.exceptions

public open class SplitPackageException(message: String) : Exception(message)

public class ConflictingBaseApkException : SplitPackageException("More than one base APK found")

public class ConflictingPackageNameException(
	public val expected: String,
	public val actual: String,
	public val name: String
) : SplitPackageException("Conflicting package name. Expected: $expected, found: $actual, name: $name")

public class ConflictingVersionCodeException(
	public val expected: Long,
	public val actual: Long,
	public val name: String
) : SplitPackageException("Conflicting version code. Expected: $expected, found: $actual, name: $name")