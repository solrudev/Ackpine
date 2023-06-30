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
}