package ru.solrudev.ackpine.impl.session

import ru.solrudev.ackpine.impl.database.dao.SessionFailureDao
import ru.solrudev.ackpine.impl.database.model.SessionEntity
import ru.solrudev.ackpine.session.Failure
import ru.solrudev.ackpine.session.Session

@JvmSynthetic
internal fun <F : Failure> SessionEntity.State.toSessionState(
	id: String,
	sessionFailureDao: SessionFailureDao<F>
): Session.State<F> = when (this) {
	SessionEntity.State.CREATING -> Session.State.Creating
	SessionEntity.State.PENDING -> Session.State.Pending
	SessionEntity.State.ACTIVE -> Session.State.Active
	SessionEntity.State.AWAITING -> Session.State.Awaiting
	SessionEntity.State.COMMITTED -> Session.State.Committed
	SessionEntity.State.CANCELLED -> Session.State.Cancelled
	SessionEntity.State.SUCCEEDED -> Session.State.Succeeded
	SessionEntity.State.FAILED -> {
		val failure = sessionFailureDao.getFailure(id)
		Session.State.Failed(failure!!)
	}
}