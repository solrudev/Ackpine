package ru.solrudev.ackpine.sample.install

import ru.solrudev.ackpine.session.Progress
import java.io.Serializable
import java.util.UUID

data class SessionProgress(
	val id: UUID,
	val currentProgress: Int,
	val progressMax: Int
) : Serializable {

	constructor(id: UUID, progress: Progress) : this(id, progress.progress, progress.max)

	val progress: Progress
		get() = Progress(currentProgress, progressMax)
}