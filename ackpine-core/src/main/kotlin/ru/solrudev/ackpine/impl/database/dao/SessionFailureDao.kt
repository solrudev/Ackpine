package ru.solrudev.ackpine.impl.database.dao

import ru.solrudev.ackpine.session.Failure

internal interface SessionFailureDao<F : Failure> {
	fun getFailure(id: String): F?
	fun setFailure(id: String, failure: F)
}