package ru.solrudev.ackpine.impl.uninstaller.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.annotation.RestrictTo
import ru.solrudev.ackpine.impl.activity.LauncherActivity
import ru.solrudev.ackpine.impl.uninstaller.activity.contract.UninstallPackageContract
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.uninstaller.PackageUninstaller
import ru.solrudev.ackpine.uninstaller.UninstallFailure

private const val TAG = "UninstallActivity"
private const val REQUEST_CODE = 984120586
private val uninstallPackageContract = UninstallPackageContract()

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal abstract class UninstallActivity : LauncherActivity<Session<UninstallFailure>, UninstallFailure>(
	TAG, REQUEST_CODE,
	UninstallFailure::Aborted
) {

	override val ackpineSessionFuture by lazy {
		ackpinePackageUninstaller.getSessionAsync(ackpineSessionId!!)
	}

	private val packageName by lazy {
		intent.extras?.getString(PACKAGE_NAME_KEY)
	}

	private lateinit var ackpinePackageUninstaller: PackageUninstaller

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		ackpinePackageUninstaller = PackageUninstaller.getInstance(this)
		if (savedInstanceState == null) {
			launchUninstallActivity()
		}
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)
		if (requestCode != REQUEST_CODE) {
			return
		}
		val success = uninstallPackageContract.parseResult(this, resultCode)
		val result = if (success) Session.State.Succeeded else Session.State.Failed(UninstallFailure.Generic)
		withCompletableSession { session ->
			session?.complete(result)
		}
	}

	@SuppressLint("RestrictedApi")
	private fun launchUninstallActivity() {
		if (packageName == null) {
			withCompletableSession { session ->
				session?.completeExceptionally(
					IllegalStateException("$TAG: packageName was null.")
				)
			}
			return
		}
		val intent = uninstallPackageContract.createIntent(this, packageName!!)
		startActivityForResult(intent, REQUEST_CODE)
		withCompletableSession { session ->
			session?.notifyCommitted()
		}
	}

	internal companion object {

		@get:JvmSynthetic
		internal const val PACKAGE_NAME_KEY = "ACKPINE_UNINSTALLER_PACKAGE_NAME"
	}
}