/*
 * Copyright (C) 2026 Ilya Fomichev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.solrudev.ackpine.impl.installer.session

import androidx.annotation.RestrictTo
import androidx.annotation.WorkerThread
import ru.solrudev.ackpine.impl.database.dao.InstallPreapprovalDao
import ru.solrudev.ackpine.impl.helpers.concurrent.BinarySemaphore
import ru.solrudev.ackpine.impl.helpers.concurrent.withPermit
import java.util.concurrent.atomic.AtomicReference

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class PreapprovalLifecycle internal constructor(
	initialState: State,
	private val sessionId: String,
	private val installPreapprovalDao: InstallPreapprovalDao,
	private val dbWriteSemaphore: BinarySemaphore
) {

	private val state = AtomicReference(initialState.toInMemoryState())

	@JvmSynthetic
	fun isPreapproved() = state.get() == InMemoryState.PREAPPROVED

	@JvmSynthetic
	fun isActive(): Boolean {
		val currentState = state.get()
		return currentState == InMemoryState.ACTIVE
				|| currentState == InMemoryState.ACTIVATING_CLAIMABLE
				|| currentState == InMemoryState.ACTIVATING_OWNED
	}

	@WorkerThread
	@JvmSynthetic
	fun runPreapprovalRequest(block: () -> Unit) {
		val requestAcquireResult = beginRequest()
		if (requestAcquireResult == RequestAcquireResult.NOT_ACQUIRED) {
			return
		}
		try {
			block()
		} catch (exception: IllegalStateException) {
			if (requestAcquireResult == RequestAcquireResult.ACQUIRED_FRESH) {
				abortRequest()
				throw exception
			}
			/** re-request, see [android.content.pm.PackageInstaller.Session.requestUserPreapproval] */
		} catch (throwable: Throwable) {
			abortRequest()
			throw throwable
		}
		activateRequest()
	}

	@WorkerThread
	@JvmSynthetic
	fun consumeActive(isPreapproved: Boolean): Boolean {
		var previousState: InMemoryState
		while (true) {
			val currentState = state.get()
			if (
				currentState != InMemoryState.ACTIVE
				&& currentState != InMemoryState.ACTIVATING_CLAIMABLE
				&& currentState != InMemoryState.ACTIVATING_OWNED
			) {
				return false
			}
			if (state.compareAndSet(currentState, InMemoryState.CONSUMING)) {
				previousState = currentState
				break
			}
		}
		val consumed = try {
			dbWriteSemaphore.withPermit {
				installPreapprovalDao.consumeActive(sessionId, isPreapproved)
			}
		} catch (throwable: Throwable) {
			state.compareAndSet(InMemoryState.CONSUMING, previousState)
			throw throwable
		}
		if (consumed == 0) {
			state.compareAndSet(InMemoryState.CONSUMING, InMemoryState.IDLE)
			return false
		}
		return state.compareAndSet(
			InMemoryState.CONSUMING,
			if (isPreapproved) InMemoryState.PREAPPROVED else InMemoryState.IDLE
		)
	}

	@WorkerThread
	@JvmSynthetic
	fun reset() {
		while (true) {
			val currentState = state.get()
			if (currentState == InMemoryState.RESETTING) {
				return
			}
			if (!state.compareAndSet(currentState, InMemoryState.RESETTING)) {
				continue
			}
			try {
				dbWriteSemaphore.withPermit {
					installPreapprovalDao.reset(sessionId)
				}
			} catch (throwable: Throwable) {
				state.compareAndSet(InMemoryState.RESETTING, currentState)
				throw throwable
			}
			state.set(InMemoryState.IDLE)
			return
		}
	}

	private fun beginRequest(): RequestAcquireResult {
		while (true) {
			when (state.get()) {
				InMemoryState.IDLE -> {
					if (!state.compareAndSet(InMemoryState.IDLE, InMemoryState.ACTIVATING_OWNED)) {
						continue
					}
					try {
						val updatedRows = dbWriteSemaphore.withPermit {
							installPreapprovalDao.setActivating(sessionId)
						}
						if (updatedRows == 0) {
							state.compareAndSet(InMemoryState.ACTIVATING_OWNED, InMemoryState.IDLE)
							return RequestAcquireResult.NOT_ACQUIRED
						}
						return RequestAcquireResult.ACQUIRED_FRESH
					} catch (throwable: Throwable) {
						state.compareAndSet(InMemoryState.ACTIVATING_OWNED, InMemoryState.IDLE)
						throw throwable
					}
				}

				InMemoryState.ACTIVATING_CLAIMABLE -> {
					if (!state.compareAndSet(InMemoryState.ACTIVATING_CLAIMABLE, InMemoryState.ACTIVATING_OWNED)) {
						continue
					}
					return RequestAcquireResult.ACQUIRED_RESTORED
				}

				else -> return RequestAcquireResult.NOT_ACQUIRED
			}
		}
	}

	private fun activateRequest() {
		val updatedRows = dbWriteSemaphore.withPermit {
			installPreapprovalDao.setActive(sessionId)
		}
		if (updatedRows == 0) {
			state.compareAndSet(InMemoryState.ACTIVATING_OWNED, InMemoryState.IDLE)
			return
		}
		state.compareAndSet(InMemoryState.ACTIVATING_OWNED, InMemoryState.ACTIVE)
	}

	private fun abortRequest() {
		if (!consumeActive(isPreapproved = false)) {
			state.compareAndSet(InMemoryState.ACTIVATING_OWNED, InMemoryState.IDLE)
		}
	}

	private fun State.toInMemoryState() = when (this) {
		State.IDLE -> InMemoryState.IDLE
		State.ACTIVATING -> InMemoryState.ACTIVATING_CLAIMABLE
		State.ACTIVE -> InMemoryState.ACTIVE
		State.PREAPPROVED -> InMemoryState.PREAPPROVED
	}

	@RestrictTo(RestrictTo.Scope.LIBRARY)
	enum class State {
		IDLE,
		ACTIVATING,
		ACTIVE,
		PREAPPROVED
	}

	private enum class InMemoryState {
		IDLE,
		ACTIVATING_CLAIMABLE,
		ACTIVATING_OWNED,
		ACTIVE,
		CONSUMING,
		PREAPPROVED,
		RESETTING
	}

	private enum class RequestAcquireResult {
		NOT_ACQUIRED,
		ACQUIRED_FRESH,
		ACQUIRED_RESTORED
	}
}