package ru.solrudev.ackpine.impl.session

import ru.solrudev.ackpine.session.Failure
import ru.solrudev.ackpine.session.Session

internal interface CompletableSession<F : Failure> : Session<F> {
	fun complete(state: Session.State.Completed<F>)
	fun completeExceptionally(exception: Exception)
	fun notifyCommitted()
}