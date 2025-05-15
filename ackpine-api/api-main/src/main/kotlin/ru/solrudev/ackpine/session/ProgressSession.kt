/*
 * Copyright (C) 2023 Ilya Fomichev
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

package ru.solrudev.ackpine.session

import ru.solrudev.ackpine.DisposableSubscription
import ru.solrudev.ackpine.DisposableSubscriptionContainer
import java.util.UUID

/**
 * A [Session] with a progress.
 */
public interface ProgressSession<out F : Failure> : Session<F> {

	/**
	 * Adds a [ProgressListener] to this session if it's not registered yet and appends the subscription to the
	 * [subscriptions bag][subscriptionContainer]. The listener will be notified with current progress immediately
	 * upon registering.
	 *
	 * Listeners are notified on main thread.
	 *
	 * @return [DisposableSubscription] &mdash; a handle to the subscription, dummy object if listener is already
	 * registered.
	 */
	public fun addProgressListener(
		subscriptionContainer: DisposableSubscriptionContainer,
		listener: ProgressListener
	): DisposableSubscription

	/**
	 * Removes the provided [ProgressListener] from this session.
	 */
	public fun removeProgressListener(listener: ProgressListener)

	/**
	 * Callback interface for listening to [ProgressSession] progress updates.
	 */
	public fun interface ProgressListener {

		/**
		 * Notifies about progress update.
		 * @param sessionId ID of the session which had its progress updated.
		 * @param progress progress of the session.
		 */
		public fun onProgressChanged(sessionId: UUID, progress: Progress)
	}
}