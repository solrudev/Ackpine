package ru.solrudev.ackpine.helpers

private const val CONFIG_PART = "config."

@JvmSynthetic
internal fun splitTypePart(name: String): String? {
	if (!name.contains(CONFIG_PART, ignoreCase = true) && !name.contains(".$CONFIG_PART", ignoreCase = true)) {
		return null
	}
	return name.substringAfter(CONFIG_PART).lowercase()
}