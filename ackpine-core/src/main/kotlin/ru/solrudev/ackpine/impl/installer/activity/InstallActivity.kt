package ru.solrudev.ackpine.impl.installer.activity

import android.app.Activity
import android.os.Build
import android.os.Bundle
import androidx.annotation.RestrictTo
import ru.solrudev.ackpine.DisposableSubscriptionContainer
import ru.solrudev.ackpine.R
import ru.solrudev.ackpine.helpers.clearTurnScreenOnSettings
import ru.solrudev.ackpine.helpers.handleResult
import ru.solrudev.ackpine.helpers.turnScreenOnWhenLocked
import ru.solrudev.ackpine.impl.installer.activity.helpers.getSerializableCompat
import ru.solrudev.ackpine.impl.session.CompletableSession
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.installer.PackageInstaller
import ru.solrudev.ackpine.session.Session
import java.util.UUID

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal abstract class InstallActivity(
	private val tag: String,
	private val requestCode: Int = -1
) : Activity() {

	private lateinit var ackpinePackageInstaller: PackageInstaller

	protected val ackpineSessionId by lazy {
		intent.extras?.getSerializableCompat<UUID>(SESSION_ID_KEY)
	}

	private val ackpineSessionFuture by lazy {
		ackpinePackageInstaller.getSessionAsync(ackpineSessionId!!)
	}

	private val subscriptions = DisposableSubscriptionContainer()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.ackpine_activity_launcher)
		turnScreenOnWhenLocked()
		registerOnBackInvokedCallback()
		finishActivityOnTerminalSessionState()
		ackpinePackageInstaller = PackageInstaller.getInstance(this)
	}

	override fun onDestroy() {
		super.onDestroy()
		subscriptions.clear()
		clearTurnScreenOnSettings()
	}

	@Deprecated("Deprecated in Java")
	@Suppress("DEPRECATION")
	override fun onBackPressed() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
			abortSession()
		}
		super.onBackPressed()
	}

	@Suppress("DEPRECATION")
	private fun registerOnBackInvokedCallback() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			onBackInvokedDispatcher.registerOnBackInvokedCallback(1000) {
				abortSession()
				super.onBackPressed()
			}
		}
	}

	private fun abortSession() = withCompletableSession { session ->
		session?.complete(
			Session.State.Failed(
				InstallFailure.Aborted(message = "$tag was finished by user")
			)
		)
	}

	private fun finishActivityOnTerminalSessionState() = ackpineSessionFuture.handleResult { session ->
		val subscription = session?.addStateListener { _, state ->
			if (state.isTerminal) {
				finishWithLaunchedActivity()
			}
		}
		subscription?.let(subscriptions::add)
	}

	private fun finishWithLaunchedActivity() {
		if (requestCode != -1) {
			finishActivity(requestCode)
		}
		finish()
	}

	@Suppress("UNCHECKED_CAST")
	@JvmSynthetic
	internal inline fun withCompletableSession(crossinline block: (CompletableSession<InstallFailure>?) -> Unit) {
		ackpineSessionFuture.handleResult { session ->
			val completableSession = session as? CompletableSession<InstallFailure>
			block(completableSession)
		}
	}

	internal companion object {

		@get:JvmSynthetic
		internal const val SESSION_ID_KEY = "ACKPINE_SESSION_ID"
	}
}