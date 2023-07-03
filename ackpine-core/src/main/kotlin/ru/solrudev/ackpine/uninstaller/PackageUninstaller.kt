package ru.solrudev.ackpine.uninstaller

import android.content.Context
import android.os.Handler
import androidx.annotation.RestrictTo
import com.google.common.util.concurrent.ListenableFuture
import ru.solrudev.ackpine.impl.database.AckpineDatabase
import ru.solrudev.ackpine.impl.uninstaller.PackageUninstallerImpl
import ru.solrudev.ackpine.impl.uninstaller.UninstallSessionFactoryImpl
import ru.solrudev.ackpine.plugin.AckpinePlugin
import ru.solrudev.ackpine.plugin.AckpinePluginRegistry
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.uninstaller.parameters.UninstallParameters
import java.util.UUID
import java.util.concurrent.Executor

public interface PackageUninstaller {

	public fun createSession(parameters: UninstallParameters): Session<UninstallFailure>
	public fun getSessionAsync(sessionId: UUID): ListenableFuture<Session<UninstallFailure>?>

	public companion object : AckpinePlugin {

		private lateinit var executor: Executor
		private val lock = Any()
		private var packageUninstaller: PackageUninstaller? = null

		@JvmStatic
		public fun getInstance(context: Context): PackageUninstaller {
			if (packageUninstaller == null) {
				synchronized(lock) {
					if (packageUninstaller == null) {
						initialize(context)
					}
				}
			}
			return packageUninstaller!!
		}

		private fun initialize(context: Context) {
			AckpinePluginRegistry.register(this)
			val database = AckpineDatabase.getInstance(context.applicationContext, executor)
			packageUninstaller = PackageUninstallerImpl(
				database.uninstallSessionDao(),
				executor,
				UninstallSessionFactoryImpl(
					context.applicationContext,
					database.sessionDao(),
					database.uninstallSessionDao(),
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