package ru.solrudev.ackpine.session

import ru.solrudev.ackpine.DisposableSubscription
import java.util.UUID

public interface Session<out F : Failure> {

	public val id: UUID
	public val isActive: Boolean
	public fun launch()
	public fun commit()
	public fun cancel()
	public fun addStateListener(listener: StateListener<F>): DisposableSubscription
	public fun removeStateListener(listener: StateListener<F>)

	public sealed interface State<out F : Failure> {

		public sealed interface Terminal
		public sealed interface Completed<out F : Failure> : State<F>

		public val isTerminal: Boolean
			get() = this is Terminal

		public val isCompleted: Boolean
			get() = this is Completed

		public data object Creating : State<Nothing>
		public data object Pending : State<Nothing>
		public data object Active : State<Nothing>
		public data object Awaiting : State<Nothing>
		public data object Committed : State<Nothing>
		public data object Cancelled : State<Nothing>, Terminal
		public data object Succeeded : State<Nothing>, Terminal, Completed<Nothing>
		public data class Failed<out F : Failure>(public val failure: F) : State<F>, Terminal, Completed<F>
	}

	public fun interface StateListener<in F : Failure> {
		public fun onStateChanged(sessionId: UUID, state: State<F>)
	}

	public abstract class DefaultStateListener<in F : Failure>(private val session: Session<F>) : StateListener<F> {

		public open fun onSuccess() {}
		public open fun onFailure(failure: F) {}
		public open fun onCancelled() {}

		final override fun onStateChanged(sessionId: UUID, state: State<F>) {
			if (state.isTerminal) {
				session.removeStateListener(this)
			}
			when (state) {
				State.Creating -> {}
				State.Pending -> session.launch()
				State.Active -> {}
				State.Awaiting -> session.commit()
				State.Committed -> {}
				State.Cancelled -> onCancelled()
				State.Succeeded -> onSuccess()
				is State.Failed -> onFailure(state.failure)
			}
		}
	}
}