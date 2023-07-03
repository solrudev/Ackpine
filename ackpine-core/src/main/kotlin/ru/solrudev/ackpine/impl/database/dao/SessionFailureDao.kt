package ru.solrudev.ackpine.impl.database.dao

import androidx.annotation.RestrictTo
import ru.solrudev.ackpine.session.Failure

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal interface SessionFailureDao<F : Failure> {
	fun getFailure(id: String): F?
	fun setFailure(id: String, failure: F)
}