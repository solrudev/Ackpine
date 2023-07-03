package ru.solrudev.ackpine.plugin

import androidx.annotation.RestrictTo
import java.util.concurrent.Executor

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface AckpinePlugin {
	public fun setExecutor(executor: Executor)
}