package ru.solrudev.ackpine.uninstaller.parameters

/**
 * Constructs a new instance of [UninstallParameters].
 */
public inline fun UninstallParameters(
	packageName: String,
	initializer: UninstallParameters.Builder.() -> Unit
): UninstallParameters {
	return UninstallParameters.Builder(packageName).apply(initializer).build()
}