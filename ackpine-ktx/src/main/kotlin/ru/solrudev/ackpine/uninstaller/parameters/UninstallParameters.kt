package ru.solrudev.ackpine.uninstaller.parameters

/**
 * Constructs a new instance of [UninstallParameters].
 */
public inline fun UninstallParameters(
	packageName: String,
	configure: UninstallParametersDsl.() -> Unit
): UninstallParameters {
	return UninstallParametersDslBuilder(packageName).apply(configure).build()
}