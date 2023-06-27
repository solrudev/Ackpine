package ru.solrudev.ackpine.impl.installer

import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.installer.parameters.InstallParameters
import ru.solrudev.ackpine.session.Progress
import ru.solrudev.ackpine.session.ProgressSession
import ru.solrudev.ackpine.session.Session
import java.util.UUID

internal interface InstallSessionFactory {

	fun create(
		parameters: InstallParameters,
		id: UUID,
		initialState: Session.State<InstallFailure>,
		initialProgress: Progress
	): ProgressSession<InstallFailure>
}