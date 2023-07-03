package ru.solrudev.ackpine.plugin

import androidx.annotation.RestrictTo
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object AckpinePluginRegistry {

	private val executor = Executors.newFixedThreadPool(
		16,
		object : ThreadFactory {
			private val threadCount = AtomicInteger(0)

			override fun newThread(runnable: Runnable?): Thread {
				return Thread(runnable, "ackpine.pool-${threadCount.incrementAndGet()}")
			}
		}
	)

	public fun register(plugin: AckpinePlugin) {
		plugin.setExecutor(executor)
	}
}