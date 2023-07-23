package ru.solrudev.ackpine.sample.install

import ru.solrudev.ackpine.session.parameters.NotificationString
import java.io.Serializable
import java.util.UUID

data class SessionData(
	val id: UUID,
	val name: String,
	val error: NotificationString = NotificationString.empty()
) : Serializable