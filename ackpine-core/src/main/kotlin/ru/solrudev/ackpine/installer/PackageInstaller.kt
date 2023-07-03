package ru.solrudev.ackpine.installer

import android.content.Context
import android.os.Handler
import androidx.annotation.RestrictTo
import com.google.common.util.concurrent.ListenableFuture
import ru.solrudev.ackpine.impl.database.AckpineDatabase
import ru.solrudev.ackpine.impl.installer.InstallSessionFactoryImpl
import ru.solrudev.ackpine.impl.installer.PackageInstallerImpl
import ru.solrudev.ackpine.installer.parameters.InstallParameters
import ru.solrudev.ackpine.plugin.AckpinePlugin
import ru.solrudev.ackpine.plugin.AckpinePluginRegistry
import ru.solrudev.ackpine.session.ProgressSession
import java.util.UUID
import java.util.concurrent.Executor

public interface PackageInstaller {

	public fun createSession(parameters: InstallParameters): ProgressSession<InstallFailure>
	public fun getSessionAsync(sessionId: UUID): ListenableFuture<ProgressSession<InstallFailure>?>

	public companion object : AckpinePlugin {

		private lateinit var executor: Executor
		private val lock = Any()
		private var packageInstaller: PackageInstaller? = null

		@JvmStatic
		public fun getInstance(context: Context): PackageInstaller {
			if (packageInstaller == null) {
				synchronized(lock) {
					if (packageInstaller == null) {
						initialize(context)
					}
				}
			}
			return packageInstaller!!
		}

		private fun initialize(context: Context) {
			AckpinePluginRegistry.register(this)
			val database = AckpineDatabase.getInstance(context.applicationContext, executor)
			packageInstaller = PackageInstallerImpl(
				database.installSessionDao(),
				database.sessionProgressDao(),
				executor,
				InstallSessionFactoryImpl(
					context.applicationContext,
					database.sessionDao(),
					database.installSessionDao(),
					database.sessionProgressDao(),
					database.nativeSessionIdDao(),
					executor,
					Handler(context.mainLooper)
				)
			)
		}

		@RestrictTo(RestrictTo.Scope.LIBRARY)
		@JvmSynthetic
		override fun setExecutor(executor: Executor) {
			this.executor = executor
		}
	}
}