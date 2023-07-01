package ru.solrudev.ackpine.impl.installer.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ru.solrudev.ackpine.DisposableSubscriptionContainer
import ru.solrudev.ackpine.R
import ru.solrudev.ackpine.helpers.clearTurnScreenOnSettings
import ru.solrudev.ackpine.helpers.handleResult
import ru.solrudev.ackpine.helpers.onBackPressed
import ru.solrudev.ackpine.helpers.turnScreenOnWhenLocked
import ru.solrudev.ackpine.impl.installer.activity.helpers.getParcelableCompat
import ru.solrudev.ackpine.impl.installer.activity.helpers.getSerializableCompat
import ru.solrudev.ackpine.impl.session.CompletableSession
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.installer.PackageInstaller
import ru.solrudev.ackpine.session.Session
import java.util.UUID

private const val REQUEST_CODE = 1654101745

internal class IntentBasedInstallLauncherActivity : AppCompatActivity() {

	lateinit var packageInstaller: PackageInstaller

	private val apkUri by lazy {
		intent.extras?.getParcelableCompat<Uri>(APK_URI_KEY)
	}

	private val sessionId by lazy {
		intent.extras?.getSerializableCompat<UUID>(SESSION_ID_KEY)
	}

	private val sessionFuture by lazy {
		packageInstaller.getSessionAsync(sessionId!!)
	}

	private val subscriptions = DisposableSubscriptionContainer()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.ackpine_activity_launcher)
		turnScreenOnWhenLocked()
		abortSessionOnBackPressed()
		finishActivityOnTerminalSessionState()
		if (savedInstanceState == null) {
			launchInstallActivity()
		}
	}

	override fun onDestroy() {
		super.onDestroy()
		subscriptions.clear()
		clearTurnScreenOnSettings()
	}

	@Deprecated("Deprecated in Java")
	@Suppress("DEPRECATION")
	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)
		if (requestCode != REQUEST_CODE) {
			return
		}
		val result = if (resultCode == Activity.RESULT_OK) {
			Session.State.Succeeded
		} else {
			Session.State.Failed(InstallFailure.Generic())
		}
		getCompletableSession { session ->
			session?.complete(result)
		}
	}

	private fun abortSessionOnBackPressed() = onBackPressed {
		getCompletableSession { session ->
			session?.complete(
				Session.State.Failed(
					InstallFailure.Aborted(message = "IntentBasedInstallLauncherActivity was finished by user")
				)
			)
		}
	}

	private fun finishActivityOnTerminalSessionState() {
		sessionFuture.handleResult { session ->
			val subscription = session?.addStateListener { _, state ->
				if (state.isTerminal) {
					finishActivity(REQUEST_CODE)
					finish()
				}
			}
			subscription?.let(subscriptions::add)
		}
	}

	@Suppress("DEPRECATION")
	@SuppressLint("RestrictedApi")
	private fun launchInstallActivity() {
		if (apkUri == null) {
			getCompletableSession { session ->
				session?.completeExceptionally(
					IllegalStateException("IntentBasedInstallLauncherActivity: apkUri was null.")
				)
			}
			return
		}
		val intent = Intent().apply {
			action = Intent.ACTION_INSTALL_PACKAGE
			setDataAndType(apkUri, "application/vnd.android.package-archive")
			flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
			putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
			putExtra(Intent.EXTRA_RETURN_RESULT, true)
			putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, packageName)
		}
		startActivityForResult(intent, REQUEST_CODE)
	}

	@Suppress("UNCHECKED_CAST")
	private inline fun getCompletableSession(crossinline block: (CompletableSession<InstallFailure>?) -> Unit) {
		sessionFuture.handleResult { session ->
			val completableSession = session as? CompletableSession<InstallFailure>
			block(completableSession)
		}
	}

	internal companion object {

		@get:JvmSynthetic
		internal const val APK_URI_KEY = "ACKPINE_INSTALLER_APK_URI"

		@get:JvmSynthetic
		internal const val SESSION_ID_KEY = "ACKPINE_SESSION_ID"
	}
}