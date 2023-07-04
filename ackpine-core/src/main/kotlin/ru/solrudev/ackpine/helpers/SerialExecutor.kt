package ru.solrudev.ackpine.helpers

import androidx.annotation.RestrictTo
import java.util.concurrent.Executor

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class SerialExecutor(private val executor: Executor) : Executor {

	private val lock = Any()
	private val tasks = ArrayDeque<Task>()
	private var activeCommand: Runnable? = null

	override fun execute(command: Runnable) = synchronized(lock) {
		tasks += Task(this, command)
		if (activeCommand == null) {
			scheduleNext()
		}
	}

	internal fun scheduleNext() {
		if (tasks.removeFirstOrNull().also { activeCommand = it } != null) {
			executor.execute(activeCommand)
		}
	}

	private class Task(private val serialExecutor: SerialExecutor, private val runnable: Runnable) : Runnable {

		override fun run() = try {
			runnable.run()
		} finally {
			synchronized(serialExecutor.lock) {
				serialExecutor.scheduleNext()
			}
		}
	}
}