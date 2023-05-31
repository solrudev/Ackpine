package ru.solrudev.ackpine.exceptions

public class ConflictingBaseApkException : Exception("More than one base APK found")

public class ConflictingPackageNameException(public val expected: String, public val actual: String) : Exception(
	"Conflicting package name. Expected: $expected, found: $actual"
)

public class ConflictingVersionCodeException(public val expected: Long, public val actual: Long) : Exception(
	"Conflicting version code. Expected: $expected, found: $actual"
)