package ru.solrudev.ackpine.impl.session

import androidx.annotation.RestrictTo
import ru.solrudev.ackpine.session.Failure
import ru.solrudev.ackpine.session.Session

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal interface CompletableSession<F : Failure> : Session<F> {
	fun complete(state: Session.State.Completed<F>)
	fun completeExceptionally(exception: Exception)
	fun notifyCommitted()
}