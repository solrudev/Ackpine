package ru.solrudev.ackpine.installer

/**
 * Represents progress data.
 */
public data class Progress(
	public val progress: Int = 0,
	public val max: Int = 100
)